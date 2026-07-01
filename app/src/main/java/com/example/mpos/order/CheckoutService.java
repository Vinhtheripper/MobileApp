package com.example.mpos.order;

import android.content.ContentValues;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

import com.example.mpos.cart.CartManager;
import com.example.mpos.constants.InventoryConstants;
import com.example.mpos.constants.OrderConstants;
import com.example.mpos.constants.PaymentConstants;
import com.example.mpos.constants.SyncConstants;
import com.example.mpos.database.DatabaseHelper;
import com.example.mpos.model.CartItem;
import com.example.mpos.service.NotificationServiceMock;
import com.example.mpos.service.PaymentServiceMock;
import com.example.mpos.sync.AuditLogger;

/** Writes every sale atomically: order, items, payment, stock log, receipt and outbox. */
public class CheckoutService {
    private final DatabaseHelper helper;
    private final PaymentServiceMock paymentService = new PaymentServiceMock();
    private final NotificationServiceMock notificationService = new NotificationServiceMock();
    public CheckoutService(DatabaseHelper helper) { this.helper = helper; }

    public long checkout(long userId, long shiftId, String customerPhone, String paymentMethod, long receivedCash) {
        CartManager cart = CartManager.get();
        if (cart.isEmpty()) throw new IllegalStateException("Giỏ hàng đang trống");
        SQLiteDatabase db = helper.getWritableDatabase();
        db.beginTransaction();
        try {
            long customerId = findOrCreateCustomer(db, customerPhone);
            long subtotal = cart.subtotal();
            ContentValues order = new ContentValues();
            long now = System.currentTimeMillis();
            order.put("order_code", "ORD-" + now); if (customerId > 0) order.put("customer_id", customerId); order.put("user_id", userId); order.put("shift_id", shiftId); order.put("channel", OrderConstants.SOURCE_WALK_IN); order.put("status", OrderConstants.STATUS_CONFIRMED); order.put("subtotal", subtotal); order.put("total_amount", subtotal); order.put("sync_status", SyncConstants.STATUS_PENDING); order.put("created_at", now); order.put("updated_at", now);
            long orderId = db.insertOrThrow("orders", null, order);
            enqueueSync(db, SyncConstants.EVENT_ORDER_CREATE, "ORDER", orderId, "CREATE", "{\"orderId\":" + orderId + "}");
            enqueueSync(db, SyncConstants.EVENT_ORDER_CONFIRM, "ORDER", orderId, "CONFIRM", "{\"orderId\":" + orderId + "}");
            for (CartItem item : cart.getItems()) saveItemAndReduceStock(db, orderId, userId, item);
            updateOrderStatus(db, orderId, OrderConstants.STATUS_INVENTORY_LOCKED);
            updateOrderStatus(db, orderId, OrderConstants.STATUS_PAYMENT_PENDING);
            PaymentServiceMock.PaymentResult result = paymentService.pay(paymentMethod, subtotal, receivedCash);
            if (!result.success) {
                restoreStock(db, orderId, userId);
                ContentValues failedPayment = new ContentValues(); failedPayment.put("order_id", orderId); failedPayment.put("method", paymentMethod); failedPayment.put("amount", subtotal); failedPayment.put("status", PaymentConstants.STATUS_FAILED); failedPayment.put("note", result.message); db.insertOrThrow("payments", null, failedPayment);
                updateOrderStatus(db, orderId, OrderConstants.STATUS_PAYMENT_FAILED);
                db.setTransactionSuccessful();
                throw new IllegalStateException(result.message);
            }
            ContentValues payment = new ContentValues(); payment.put("order_id", orderId); payment.put("method", paymentMethod); payment.put("amount", subtotal); payment.put("status", PaymentConstants.STATUS_SUCCESS); payment.put("transaction_code", result.transactionRef); payment.put("paid_at", now); payment.put("note", PaymentConstants.METHOD_CASH.equals(paymentMethod) ? "Khách đưa: " + receivedCash : "Thanh toán mock"); db.insertOrThrow("payments", null, payment);
            ContentValues receipt = new ContentValues(); receipt.put("order_id", orderId); receipt.put("receipt_number", "RCP-" + now); receipt.put("content", "mPOS Pro - Tổng thanh toán: " + subtotal); receipt.put("created_at", now); db.insertOrThrow("receipts", null, receipt);
            updateOrderStatus(db, orderId, OrderConstants.STATUS_COMPLETED);
            enqueueSync(db, SyncConstants.EVENT_PAYMENT_COMPLETE, "PAYMENT", orderId, "COMPLETE", "{\"orderId\":" + orderId + "}");
            AuditLogger.log(db, userId, "CREATE_SALE", "ORDER", orderId, "Thanh toán " + paymentMethod);
            db.setTransactionSuccessful(); cart.clear(); return orderId;
        } finally { db.endTransaction(); }
    }

    private void saveItemAndReduceStock(SQLiteDatabase db, long orderId, long userId, CartItem item) {
        Cursor c = db.rawQuery("SELECT stock_quantity FROM products WHERE id=?", new String[]{String.valueOf(item.product.id)});
        int before; try { if (!c.moveToFirst()) throw new IllegalStateException("Sản phẩm không tồn tại"); before=c.getInt(0); } finally { c.close(); }
        if (before < item.quantity) throw new IllegalStateException("Tồn kho không đủ: " + item.product.name);
        ContentValues line = new ContentValues(); line.put("order_id", orderId); line.put("product_id", item.product.id); line.put("product_name", item.product.name); line.put("unit_price", item.product.salePrice); line.put("quantity", item.quantity); line.put("line_total", item.getLineTotal()); db.insertOrThrow("order_items", null, line);
        ContentValues product = new ContentValues(); product.put("stock_quantity", before-item.quantity); product.put("updated_at", System.currentTimeMillis()); db.update("products", product, "id=?", new String[]{String.valueOf(item.product.id)});
        ContentValues movement = new ContentValues(); movement.put("product_id", item.product.id); movement.put("user_id", userId); movement.put("order_id", orderId); movement.put("transaction_type", InventoryConstants.TYPE_SALE_LOCK); movement.put("quantity_change", -item.quantity); movement.put("quantity_before", before); movement.put("quantity_after", before-item.quantity); movement.put("created_at", System.currentTimeMillis()); db.insertOrThrow("inventory_transactions", null, movement);
        enqueueSync(db, SyncConstants.EVENT_INVENTORY_LOCK, "PRODUCT", item.product.id, "LOCK", "{\"orderId\":" + orderId + ",\"productId\":" + item.product.id + "}");
        if (before - item.quantity == 0) notificationService.notifyManagers("Sản phẩm hết hàng: " + item.product.name);
    }

    private long findOrCreateCustomer(SQLiteDatabase db, String phone) {
        if (phone == null || phone.trim().isEmpty()) return -1;
        Cursor c = db.rawQuery("SELECT id FROM customers WHERE phone=?", new String[]{phone.trim()});
        try { if (c.moveToFirst()) return c.getLong(0); } finally { c.close(); }
        ContentValues customer = new ContentValues(); customer.put("phone", phone.trim()); customer.put("created_at", System.currentTimeMillis()); return db.insertOrThrow("customers", null, customer);
    }

    private void restoreStock(SQLiteDatabase db, long orderId, long userId) {
        Cursor lines = db.rawQuery("SELECT product_id, quantity FROM order_items WHERE order_id=?", new String[]{String.valueOf(orderId)});
        try {
            while (lines.moveToNext()) {
                long productId = lines.getLong(0);
                int quantity = lines.getInt(1);
                Cursor p = db.rawQuery("SELECT stock_quantity FROM products WHERE id=?", new String[]{String.valueOf(productId)});
                int before;
                try { if (!p.moveToFirst()) continue; before = p.getInt(0); } finally { p.close(); }
                ContentValues product = new ContentValues(); product.put("stock_quantity", before + quantity); product.put("updated_at", System.currentTimeMillis()); db.update("products", product, "id=?", new String[]{String.valueOf(productId)});
                ContentValues movement = new ContentValues(); movement.put("product_id", productId); movement.put("user_id", userId); movement.put("order_id", orderId); movement.put("transaction_type", InventoryConstants.TYPE_SALE_RESTORE); movement.put("quantity_change", quantity); movement.put("quantity_before", before); movement.put("quantity_after", before + quantity); movement.put("created_at", System.currentTimeMillis()); db.insertOrThrow("inventory_transactions", null, movement);
            }
        } finally { lines.close(); }
    }

    private void updateOrderStatus(SQLiteDatabase db, long orderId, String status) {
        ContentValues values = new ContentValues(); values.put("status", status); values.put("updated_at", System.currentTimeMillis()); db.update("orders", values, "id=?", new String[]{String.valueOf(orderId)});
    }

    private void enqueueSync(SQLiteDatabase db, String eventType, String entity, long id, String action, String payload) {
        ContentValues queue = new ContentValues(); queue.put("event_type", eventType); queue.put("entity_type", entity); queue.put("entity_id", id); queue.put("action_type", action); queue.put("payload", payload); queue.put("status", SyncConstants.STATUS_PENDING); queue.put("created_at", System.currentTimeMillis()); db.insertOrThrow("sync_queue", null, queue);
    }
}

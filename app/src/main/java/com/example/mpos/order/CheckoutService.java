package com.example.mpos.order;

import android.content.ContentValues;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

import com.example.mpos.cart.CartManager;
import com.example.mpos.database.DatabaseHelper;
import com.example.mpos.model.CartItem;
import com.example.mpos.sync.AuditLogger;

/** Writes every sale atomically: order, items, payment, stock log, receipt and outbox. */
public class CheckoutService {
    private final DatabaseHelper helper;
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
            order.put("order_code", "ORD-" + now); if (customerId > 0) order.put("customer_id", customerId); order.put("user_id", userId); order.put("shift_id", shiftId); order.put("channel", "WALK_IN"); order.put("status", "PAID"); order.put("subtotal", subtotal); order.put("total_amount", subtotal); order.put("created_at", now); order.put("updated_at", now);
            long orderId = db.insertOrThrow("orders", null, order);
            for (CartItem item : cart.getItems()) saveItemAndReduceStock(db, orderId, userId, item);
            ContentValues payment = new ContentValues(); payment.put("order_id", orderId); payment.put("method", paymentMethod); payment.put("amount", subtotal); payment.put("status", "PAID"); payment.put("paid_at", now); payment.put("note", paymentMethod.equals("CASH") ? "Khách đưa: " + receivedCash : "Thanh toán mock"); db.insertOrThrow("payments", null, payment);
            ContentValues receipt = new ContentValues(); receipt.put("order_id", orderId); receipt.put("receipt_number", "RCP-" + now); receipt.put("content", "mPOS Pro - Tổng thanh toán: " + subtotal); receipt.put("created_at", now); db.insertOrThrow("receipts", null, receipt);
            enqueueSync(db, "ORDER", orderId, "CREATE", "{\"orderId\":" + orderId + "}");
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
        ContentValues movement = new ContentValues(); movement.put("product_id", item.product.id); movement.put("user_id", userId); movement.put("order_id", orderId); movement.put("transaction_type", "SALE"); movement.put("quantity_change", -item.quantity); movement.put("quantity_before", before); movement.put("quantity_after", before-item.quantity); movement.put("created_at", System.currentTimeMillis()); db.insertOrThrow("inventory_transactions", null, movement);
    }

    private long findOrCreateCustomer(SQLiteDatabase db, String phone) {
        if (phone == null || phone.trim().isEmpty()) return -1;
        Cursor c = db.rawQuery("SELECT id FROM customers WHERE phone=?", new String[]{phone.trim()});
        try { if (c.moveToFirst()) return c.getLong(0); } finally { c.close(); }
        ContentValues customer = new ContentValues(); customer.put("phone", phone.trim()); customer.put("created_at", System.currentTimeMillis()); return db.insertOrThrow("customers", null, customer);
    }

    private void enqueueSync(SQLiteDatabase db, String entity, long id, String action, String payload) {
        ContentValues queue = new ContentValues(); queue.put("entity_type", entity); queue.put("entity_id", id); queue.put("action_type", action); queue.put("payload", payload); queue.put("status", "PENDING"); queue.put("created_at", System.currentTimeMillis()); db.insertOrThrow("sync_queue", null, queue);
    }
}

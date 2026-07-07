package com.example.mpos.order;

import android.content.ContentValues;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

import android.content.Context;

import com.example.mpos.cart.CartManager;
import com.example.mpos.database.DatabaseHelper;
import com.example.mpos.model.CartItem;
import com.example.mpos.sync.AuditLogger;
import com.example.mpos.sync.RtdbSync;

/** Writes every sale atomically: order, items, payment, stock log, receipt and outbox. */
public class CheckoutService {
    private final DatabaseHelper helper;
    private final long shopId;
    private final Context context;

    public CheckoutService(DatabaseHelper helper, long shopId) {
        this.helper  = helper;
        this.shopId  = shopId;
        this.context = null;
    }

    public CheckoutService(Context context, DatabaseHelper helper, long shopId) {
        this.helper  = helper;
        this.shopId  = shopId;
        this.context = context;
    }

    public long checkoutWithDiscount(long userId, long shiftId, String customerPhone,
                                      String paymentMethod, long receivedCash, long discountAmount) {
        return checkout(userId, shiftId, customerPhone, null, null, "WALK_IN", paymentMethod, receivedCash, discountAmount);
    }

    public long checkoutWithDiscount(long userId, long shiftId, String customerPhone,
                                      String customerName, String customerAddress, String channel,
                                      String paymentMethod, long receivedCash, long discountAmount) {
        return checkout(userId, shiftId, customerPhone, customerName, customerAddress, channel, paymentMethod, receivedCash, discountAmount);
    }

    public long checkout(long userId, long shiftId, String customerPhone, String paymentMethod, long receivedCash) {
        return checkout(userId, shiftId, customerPhone, null, null, "WALK_IN", paymentMethod, receivedCash, 0);
    }

    private long checkout(long userId, long shiftId, String customerPhone,
                          String customerName, String customerAddress, String channel,
                          String paymentMethod, long receivedCash, long discountAmount) {
        CartManager cart = CartManager.get();
        if (cart.isEmpty()) throw new IllegalStateException("Giỏ hàng đang trống");
        SQLiteDatabase db = helper.getWritableDatabase();
        db.beginTransaction();
        try {
            long customerId = findOrCreateCustomer(db, customerPhone, customerName, customerAddress);
            long subtotal = cart.subtotal();
            long taxAmt   = cart.tax();
            long total    = Math.max(cart.total() - discountAmount, 0);
            long now = System.currentTimeMillis();

            ContentValues order = new ContentValues();
            order.put("order_code", "ORD-" + now);
            if (customerId > 0) order.put("customer_id", customerId);
            order.put("user_id", userId);
            order.put("shift_id", shiftId);
            order.put("shop_id", shopId);
            order.put("channel", channel != null ? channel : "WALK_IN");
            order.put("status", "PAID");
            order.put("subtotal", subtotal);
            order.put("vat_percent", 10);
            order.put("vat_amount", taxAmt);
            order.put("discount_amount", discountAmount);
            order.put("total_amount", total);
            order.put("created_at", now);
            order.put("updated_at", now);
            long orderId = db.insertOrThrow("orders", null, order);

            for (CartItem item : cart.getItems()) saveItemAndReduceStock(db, orderId, userId, item);

            ContentValues payment = new ContentValues();
            payment.put("order_id", orderId);
            payment.put("method", paymentMethod);
            payment.put("amount", total);
            payment.put("status", "PAID");
            payment.put("paid_at", now);
            payment.put("note", paymentMethod.equals("CASH") ? "Khách đưa: " + receivedCash + ", Thừa: " + (receivedCash - total) : "Thanh toán " + paymentMethod);
            db.insertOrThrow("payments", null, payment);

            ContentValues receipt = new ContentValues();
            receipt.put("order_id", orderId);
            receipt.put("receipt_number", "RCP-" + now);
            receipt.put("content", buildReceiptContent(orderId, subtotal, taxAmt, total, paymentMethod, receivedCash));
            receipt.put("created_at", now);
            db.insertOrThrow("receipts", null, receipt);

            enqueueSync(db, "ORDER", orderId, "CREATE", "{\"orderId\":" + orderId + "}");
            AuditLogger.log(db, userId, "CREATE_SALE", "ORDER", orderId, "Thanh toán " + paymentMethod);
            db.setTransactionSuccessful();
            cart.clear();

            // Push to Firebase Realtime Database (fire-and-forget)
            if (context != null) {
                final long finalOrderId = orderId;
                new Thread(() -> {
                    try {
                        new RtdbSync(context, shopId).pushOrder(finalOrderId);
                    } catch (Exception ignored) {}
                }).start();
            }

            return orderId;
        } finally { db.endTransaction(); }
    }

    private String buildReceiptContent(long orderId, long subtotal, long tax, long total, String method, long received) {
        StringBuilder sb = new StringBuilder();
        sb.append("mPOS Pro\n");
        sb.append("================================\n");
        sb.append("Tạm tính: ").append(subtotal).append(" ₫\n");
        sb.append("Thuế (10%): ").append(tax).append(" ₫\n");
        sb.append("Tổng cộng: ").append(total).append(" ₫\n");
        sb.append("Thanh toán: ").append(method).append("\n");
        if ("CASH".equals(method)) {
            sb.append("Khách đưa: ").append(received).append(" ₫\n");
            sb.append("Tiền thừa: ").append(received - total).append(" ₫\n");
        }
        sb.append("================================\n");
        sb.append("Cảm ơn quý khách!");
        return sb.toString();
    }

    private void saveItemAndReduceStock(SQLiteDatabase db, long orderId, long userId, CartItem item) {
        Cursor c = db.rawQuery("SELECT stock_quantity FROM products WHERE id=?", new String[]{String.valueOf(item.product.id)});
        int before;
        try { if (!c.moveToFirst()) throw new IllegalStateException("Sản phẩm không tồn tại"); before = c.getInt(0); } finally { c.close(); }
        if (before < item.quantity) throw new IllegalStateException("Tồn kho không đủ: " + item.product.name);

        ContentValues line = new ContentValues();
        line.put("order_id", orderId); line.put("product_id", item.product.id);
        line.put("product_name", item.product.name); line.put("unit_price", item.product.salePrice);
        line.put("quantity", item.quantity); line.put("line_total", item.getLineTotal());
        db.insertOrThrow("order_items", null, line);

        ContentValues product = new ContentValues();
        product.put("stock_quantity", before - item.quantity);
        product.put("updated_at", System.currentTimeMillis());
        db.update("products", product, "id=?", new String[]{String.valueOf(item.product.id)});

        ContentValues movement = new ContentValues();
        movement.put("product_id", item.product.id); movement.put("user_id", userId);
        movement.put("order_id", orderId); movement.put("transaction_type", "SALE");
        movement.put("quantity_change", -item.quantity); movement.put("quantity_before", before);
        movement.put("quantity_after", before - item.quantity); movement.put("created_at", System.currentTimeMillis());
        db.insertOrThrow("inventory_transactions", null, movement);
    }

    private long findOrCreateCustomer(SQLiteDatabase db, String phone, String name, String address) {
        if (phone == null || phone.trim().isEmpty()) return -1;
        String p = phone.trim();
        Cursor c = db.rawQuery("SELECT id FROM customers WHERE phone=? AND shop_id=?",
            new String[]{p, String.valueOf(shopId)});
        long existingId = -1;
        try { if (c.moveToFirst()) existingId = c.getLong(0); } finally { c.close(); }

        long now = System.currentTimeMillis();
        if (existingId > 0) {
            // Update name/address if provided
            if ((name != null && !name.isEmpty()) || (address != null && !address.isEmpty())) {
                ContentValues upd = new ContentValues();
                if (name != null && !name.isEmpty()) upd.put("full_name", name);
                if (address != null && !address.isEmpty()) upd.put("address", address);
                upd.put("updated_at", now);
                db.update("customers", upd, "id=?", new String[]{String.valueOf(existingId)});
            }
            return existingId;
        }

        ContentValues customer = new ContentValues();
        customer.put("phone", p);
        customer.put("shop_id", shopId);
        if (name != null && !name.isEmpty()) customer.put("full_name", name);
        if (address != null && !address.isEmpty()) customer.put("address", address);
        customer.put("created_at", now);
        customer.put("updated_at", now);
        return db.insertOrThrow("customers", null, customer);
    }

    private void enqueueSync(SQLiteDatabase db, String entity, long id, String action, String payload) {
        ContentValues queue = new ContentValues();
        queue.put("entity_type", entity); queue.put("entity_id", id);
        queue.put("action_type", action); queue.put("payload", payload);
        queue.put("status", "PENDING"); queue.put("created_at", System.currentTimeMillis());
        db.insertOrThrow("sync_queue", null, queue);
    }
}

package com.example.mpos.sync;

import android.content.Context;
import android.database.Cursor;
import android.util.Log;

import com.example.mpos.auth.FirebaseAuthHelper;
import com.example.mpos.database.DatabaseHelper;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import java.util.HashMap;
import java.util.Map;

/**
 * Syncs local SQLite data to Firebase Realtime Database.
 *
 * DB structure (per-user isolation):
 *   users/{uid}/shops/{shopId}/orders/{orderId}
 *   users/{uid}/shops/{shopId}/products/{prodId}
 *   users/{uid}/shops/{shopId}/stats/today
 *
 * Security Rules enforce auth.uid === $uid so each user only sees their own data.
 */
public class RtdbSync {

    private static final String TAG = "RtdbSync";
    private static final String DB_URL =
        "https://mobile-app-19c4a-default-rtdb.asia-southeast1.firebasedatabase.app";

    private final DatabaseReference shopRef;
    private final DatabaseHelper     local;

    public RtdbSync(Context ctx, long shopId) {
        this.local = new DatabaseHelper(ctx);

        String uid = FirebaseAuthHelper.currentUid();
        DatabaseReference root = FirebaseDatabase.getInstance(DB_URL).getReference();

        if (uid != null) {
            this.shopRef = root.child("users").child(uid)
                               .child("shops").child(String.valueOf(shopId));
        } else {
            // Not yet signed in to Firebase — fallback path (rules will block writes)
            this.shopRef = root.child("shops").child(String.valueOf(shopId));
        }
    }

    /** Push one order after checkout. */
    public void pushOrder(long orderId) {
        try (Cursor c = local.getReadableDatabase().rawQuery(
                "SELECT o.id, o.order_code, o.total_amount, o.status, " +
                "o.created_at, o.shop_id, o.channel, o.discount_amount " +
                "FROM orders o WHERE o.id=?",
                new String[]{String.valueOf(orderId)})) {
            if (c == null || !c.moveToFirst()) return;

            Map<String, Object> data = new HashMap<>();
            data.put("id",           c.getLong(0));
            data.put("orderCode",    c.getString(1));
            data.put("totalAmount",  c.getLong(2));
            data.put("status",       c.getString(3));
            data.put("createdAt",    c.getLong(4));
            data.put("shopId",       c.getLong(5));
            data.put("channel",      c.getString(6));
            data.put("discount",     c.getLong(7));
            data.put("syncedAt",     System.currentTimeMillis());

            shopRef.child("orders").child(String.valueOf(orderId))
                   .setValue(data)
                   .addOnSuccessListener(v -> Log.d(TAG, "Order synced: " + orderId))
                   .addOnFailureListener(e -> Log.w(TAG, "Order sync fail: " + e.getMessage()));
        } catch (Exception e) {
            Log.e(TAG, "pushOrder: " + e.getMessage());
        }
    }

    /** Push live today-stats. */
    public void pushTodayStats(long revenue, int orderCount, long profit) {
        Map<String, Object> stats = new HashMap<>();
        stats.put("revenue",    revenue);
        stats.put("orderCount", orderCount);
        stats.put("profit",     profit);
        stats.put("updatedAt",  System.currentTimeMillis());

        shopRef.child("stats").child("today").setValue(stats)
               .addOnFailureListener(e -> Log.w(TAG, "Stats sync fail: " + e.getMessage()));
    }

    /** Push product stock level. */
    public void pushProductStock(long productId) {
        try (Cursor c = local.getReadableDatabase().rawQuery(
                "SELECT id, name, stock_quantity, min_stock_quantity, sell_price " +
                "FROM products WHERE id=?",
                new String[]{String.valueOf(productId)})) {
            if (c == null || !c.moveToFirst()) return;

            Map<String, Object> data = new HashMap<>();
            data.put("id",       c.getLong(0));
            data.put("name",     c.getString(1));
            data.put("stock",    c.getInt(2));
            data.put("minStock", c.getInt(3));
            data.put("price",    c.getLong(4));
            data.put("syncedAt", System.currentTimeMillis());

            shopRef.child("products").child(String.valueOf(productId)).setValue(data)
                   .addOnFailureListener(e -> Log.w(TAG, "Product sync fail: " + e.getMessage()));
        } catch (Exception e) {
            Log.e(TAG, "pushProductStock: " + e.getMessage());
        }
    }

    /** Sync all pending orders (called from SyncWorker). */
    public void syncPendingOrders() {
        try (Cursor c = local.getReadableDatabase().rawQuery(
                "SELECT id FROM orders WHERE sync_status=0 LIMIT 50", null)) {
            if (c == null) return;
            while (c.moveToNext()) pushOrder(c.getLong(0));
        } catch (Exception e) {
            Log.e(TAG, "syncPendingOrders: " + e.getMessage());
        }
    }
}

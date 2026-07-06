package com.example.mpos.dao;

import android.database.Cursor;

import com.example.mpos.database.DatabaseHelper;

import java.util.Calendar;

public class OrderDao {

    private final DatabaseHelper db;
    private final long shopId;

    public OrderDao(DatabaseHelper db, long shopId) {
        this.db = db;
        this.shopId = shopId;
    }

    public DailyStats getTodayStats() {
        long todayStart = getTodayStartMs();
        try (Cursor c = db.getReadableDatabase().rawQuery(
                "SELECT COALESCE(SUM(o.total_amount),0), COUNT(*), " +
                "COALESCE(SUM(c.cost),0) " +
                "FROM orders o " +
                "LEFT JOIN (SELECT oi.order_id, SUM(oi.quantity * COALESCE(p.cost_price,0)) AS cost " +
                "           FROM order_items oi JOIN products p ON p.id=oi.product_id " +
                "           GROUP BY oi.order_id) c ON c.order_id=o.id " +
                "WHERE o.shop_id=? AND o.created_at >= ? AND o.status != 'CANCELLED'",
                new String[]{String.valueOf(shopId), String.valueOf(todayStart)})) {
            if (c != null && c.moveToFirst()) {
                return new DailyStats(c.getLong(0), c.getInt(1), c.getLong(2));
            }
        } catch (Exception e) {
            android.util.Log.e("OrderDao", "getTodayStats error: " + e.getMessage(), e);
        }
        return new DailyStats(0, 0, 0);
    }

    public int getLowStockCount() {
        try (Cursor c = db.getReadableDatabase().rawQuery(
                "SELECT COUNT(*) FROM products WHERE is_active=1 AND shop_id=? AND stock_quantity <= min_stock_quantity",
                new String[]{String.valueOf(shopId)})) {
            if (c != null && c.moveToFirst()) return c.getInt(0);
        } catch (Exception ignored) {}
        return 0;
    }

    public int getPendingSyncCount() {
        try (Cursor c = db.getReadableDatabase().rawQuery(
                "SELECT COUNT(*) FROM orders WHERE sync_status = 0 AND shop_id=?",
                new String[]{String.valueOf(shopId)})) {
            if (c != null && c.moveToFirst()) return c.getInt(0);
        } catch (Exception ignored) {}
        return 0;
    }

    private long getTodayStartMs() {
        Calendar cal = Calendar.getInstance();
        cal.set(Calendar.HOUR_OF_DAY, 0);
        cal.set(Calendar.MINUTE, 0);
        cal.set(Calendar.SECOND, 0);
        cal.set(Calendar.MILLISECOND, 0);
        return cal.getTimeInMillis();
    }

    public static class DailyStats {
        public final long totalRevenue;
        public final int  orderCount;
        public final long totalCost;
        public DailyStats(long totalRevenue, int orderCount, long totalCost) {
            this.totalRevenue = totalRevenue;
            this.totalCost    = totalCost;
            this.orderCount   = orderCount;
        }
    }
}
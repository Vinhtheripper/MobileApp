package com.example.mpos.database;

import android.content.ContentValues;
import android.database.sqlite.SQLiteDatabase;
import android.database.Cursor;

import com.example.mpos.auth.PasswordUtils;

/** Seed is deliberately small so a fresh install is usable without a backend. */
public final class DatabaseSeeder {
    private DatabaseSeeder() { }

    public static void seed(SQLiteDatabase db) {
        Cursor existing = db.rawQuery("SELECT COUNT(*) FROM users", null);
        try { if (existing.moveToFirst() && existing.getInt(0) > 0) return; } finally { existing.close(); }
        long now = System.currentTimeMillis();
        ContentValues employee = new ContentValues();
        employee.put("employee_code", "EMP-001");
        employee.put("full_name", "Administrator");
        employee.put("position", "Owner");
        long employeeId = db.insert(DatabaseContract.EMPLOYEES, null, employee);

        ContentValues user = new ContentValues();
        user.put("username", "admin");
        user.put("password_hash", PasswordUtils.hash("admin123"));
        user.put("employee_id", employeeId);
        user.put("role", "ADMIN");
        user.put("created_at", now);
        db.insert(DatabaseContract.USERS, null, user);

        ContentValues category = new ContentValues();
        category.put("name", "Hàng mẫu");
        long categoryId = db.insert(DatabaseContract.CATEGORIES, null, category);
        insertProduct(db, "SP001", "893000000001", "Nước suối 500ml", categoryId, 5000, 12000, 50, now);
        insertProduct(db, "SP002", "893000000002", "Bánh quy", categoryId, 18000, 30000, 25, now);
    }

    private static void insertProduct(SQLiteDatabase db, String sku, String barcode, String name,
                                      long categoryId, int cost, int price, int stock, long now) {
        ContentValues values = new ContentValues();
        values.put("sku", sku); values.put("barcode", barcode); values.put("name", name);
        values.put("category_id", categoryId); values.put("cost_price", cost); values.put("sale_price", price);
        values.put("stock_quantity", stock); values.put("min_stock_quantity", 5); values.put("created_at", now);
        db.insert(DatabaseContract.PRODUCTS, null, values);
    }
}

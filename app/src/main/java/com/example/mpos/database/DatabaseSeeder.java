package com.example.mpos.database;

import android.content.ContentValues;
import android.database.sqlite.SQLiteDatabase;
import android.database.Cursor;

import com.example.mpos.auth.PasswordUtils;
import com.example.mpos.constants.RoleConstants;

/** Seed is deliberately small so a fresh install is usable without a backend. */
public final class DatabaseSeeder {
    private DatabaseSeeder() { }

    public static void seed(SQLiteDatabase db) {
        long now = System.currentTimeMillis();
        seedUser(db, "EMP-001", "Administrator", "Owner", "admin", "admin123", RoleConstants.ADMIN, now);
        seedUser(db, "EMP-002", "Sales Staff", "Staff", "staff", "staff123", RoleConstants.STAFF, now);

        long categoryId = findCategory(db, "Hàng mẫu");
        if (categoryId < 0) {
            ContentValues category = new ContentValues();
            category.put("name", "Hàng mẫu");
            categoryId = db.insert(DatabaseContract.CATEGORIES, null, category);
        }
        insertProduct(db, "SP001", "893000000001", "Nước suối 500ml", categoryId, 5000, 12000, 50, now);
        insertProduct(db, "SP002", "893000000002", "Bánh quy", categoryId, 18000, 30000, 25, now);
    }

    private static long findCategory(SQLiteDatabase db, String name) {
        Cursor c = db.rawQuery("SELECT id FROM categories WHERE name=? ORDER BY id LIMIT 1", new String[]{name});
        try { return c.moveToFirst() ? c.getLong(0) : -1; } finally { c.close(); }
    }

    private static void seedUser(SQLiteDatabase db, String employeeCode, String fullName, String position,
                                 String username, String password, String role, long now) {
        Cursor existing = db.rawQuery("SELECT id FROM users WHERE username=?", new String[]{username});
        try { if (existing.moveToFirst()) return; } finally { existing.close(); }
        ContentValues employee = new ContentValues();
        employee.put("employee_code", employeeCode);
        employee.put("full_name", fullName);
        employee.put("position", position);
        employee.put("created_at", now);
        long employeeId = db.insertWithOnConflict(DatabaseContract.EMPLOYEES, null, employee, SQLiteDatabase.CONFLICT_IGNORE);
        if (employeeId < 0) {
            Cursor c = db.rawQuery("SELECT id FROM employees WHERE employee_code=?", new String[]{employeeCode});
            try { if (c.moveToFirst()) employeeId = c.getLong(0); } finally { c.close(); }
        }
        ContentValues user = new ContentValues();
        user.put("username", username);
        user.put("password_hash", PasswordUtils.hash(password));
        user.put("employee_id", employeeId);
        user.put("role", role);
        user.put("created_at", now);
        db.insertWithOnConflict(DatabaseContract.USERS, null, user, SQLiteDatabase.CONFLICT_IGNORE);
    }

    private static void insertProduct(SQLiteDatabase db, String sku, String barcode, String name,
                                      long categoryId, int cost, int price, int stock, long now) {
        ContentValues values = new ContentValues();
        values.put("sku", sku); values.put("barcode", barcode); values.put("name", name);
        values.put("category_id", categoryId); values.put("cost_price", cost); values.put("sale_price", price);
        values.put("stock_quantity", stock); values.put("min_stock_quantity", 5); values.put("created_at", now);
        db.insertWithOnConflict(DatabaseContract.PRODUCTS, null, values, SQLiteDatabase.CONFLICT_IGNORE);
    }
}

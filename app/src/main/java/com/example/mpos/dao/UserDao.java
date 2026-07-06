package com.example.mpos.dao;

import android.content.ContentValues;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

import com.example.mpos.auth.PasswordUtils;
import com.example.mpos.database.DatabaseHelper;
import com.example.mpos.model.User;

public class UserDao {
    private final DatabaseHelper helper;
    public UserDao(DatabaseHelper helper) { this.helper = helper; }

    public User login(String username, String password) {
        Cursor cursor = helper.getReadableDatabase().rawQuery("SELECT id, employee_id, username, role FROM users WHERE username=? AND password_hash=? AND is_active=1", new String[]{username.trim(), PasswordUtils.hash(password)});
        try {
            if (!cursor.moveToFirst()) return null;
            User user = new User();
            user.id = cursor.getLong(0);
            user.employeeId = cursor.isNull(1) ? -1 : cursor.getLong(1);
            user.username = cursor.getString(2);
            user.role = cursor.getString(3);
            return user;
        } finally { cursor.close(); }
    }

    public User findByEmail(String email) {
        Cursor c = helper.getReadableDatabase().rawQuery(
            "SELECT id, employee_id, username, role FROM users WHERE username=? AND is_active=1",
            new String[]{email.trim()});
        try {
            if (!c.moveToFirst()) return null;
            User user = new User();
            user.id = c.getLong(0);
            user.employeeId = c.isNull(1) ? -1 : c.getLong(1);
            user.username = c.getString(2);
            user.role = c.getString(3);
            return user;
        } finally { c.close(); }
    }

    public User registerWithGoogle(String email, String displayName) {
        String randomPass = java.util.UUID.randomUUID().toString();
        String name = (displayName != null && !displayName.isEmpty()) ? displayName : email;
        boolean ok = register(email, name, "", randomPass, "ADMIN");
        if (!ok) return null;
        return findByEmail(email);
    }

    public boolean isEmailTaken(String email) {
        Cursor c = helper.getReadableDatabase().rawQuery(
            "SELECT 1 FROM users WHERE username=?", new String[]{email.trim()});
        try { return c.moveToFirst(); }
        finally { c.close(); }
    }

    public boolean register(String email, String fullName, String phone, String password, String role) {
        SQLiteDatabase db = helper.getWritableDatabase();
        long now = System.currentTimeMillis();
        db.beginTransaction();
        try {
            ContentValues emp = new ContentValues();
            emp.put("full_name", fullName.trim());
            emp.put("phone", phone.trim());
            emp.put("email", email.trim());
            emp.put("is_active", 1);
            emp.put("created_at", now);
            emp.put("updated_at", now);
            long employeeId = db.insert("employees", null, emp);
            if (employeeId < 0) return false;

            ContentValues user = new ContentValues();
            user.put("username", email.trim());
            user.put("password_hash", PasswordUtils.hash(password));
            user.put("employee_id", employeeId);
            user.put("role", role);
            user.put("is_active", 1);
            user.put("created_at", now);
            user.put("updated_at", now);
            long userId = db.insert("users", null, user);
            if (userId < 0) return false;

            db.setTransactionSuccessful();
            return true;
        } catch (Exception e) {
            return false;
        } finally {
            db.endTransaction();
        }
    }
}
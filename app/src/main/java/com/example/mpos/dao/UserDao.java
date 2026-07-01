package com.example.mpos.dao;

import android.content.ContentValues;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

import com.example.mpos.auth.PasswordUtils;
import com.example.mpos.database.DatabaseHelper;
import com.example.mpos.model.User;

import java.util.ArrayList;
import java.util.List;

public class UserDao {
    private final DatabaseHelper helper;
    public UserDao(DatabaseHelper helper) { this.helper = helper; }
    public User login(String username, String password) {
        Cursor cursor = helper.getReadableDatabase().rawQuery("SELECT id, employee_id, username, role FROM users WHERE username=? AND password_hash=? AND is_active=1", new String[]{username.trim(), PasswordUtils.hash(password)});
        try {
            if (!cursor.moveToFirst()) return null;
            User user = new User(); user.id = cursor.getLong(0); user.employeeId = cursor.isNull(1) ? -1 : cursor.getLong(1); user.username = cursor.getString(2); user.role = cursor.getString(3); return user;
        } finally { cursor.close(); }
    }

    public List<User> listAll() {
        Cursor cursor = helper.getReadableDatabase().rawQuery(
                "SELECT u.id,u.employee_id,u.username,u.role,u.is_active,e.full_name,e.phone,e.email,e.position FROM users u LEFT JOIN employees e ON e.id=u.employee_id ORDER BY u.is_active DESC,u.username",
                null);
        List<User> users = new ArrayList<>();
        try { while (cursor.moveToNext()) users.add(map(cursor)); } finally { cursor.close(); }
        return users;
    }

    public User findById(long id) {
        Cursor cursor = helper.getReadableDatabase().rawQuery(
                "SELECT u.id,u.employee_id,u.username,u.role,u.is_active,e.full_name,e.phone,e.email,e.position FROM users u LEFT JOIN employees e ON e.id=u.employee_id WHERE u.id=?",
                new String[]{String.valueOf(id)});
        try { return cursor.moveToFirst() ? map(cursor) : null; } finally { cursor.close(); }
    }

    public long saveAccount(User user, String password) {
        if (user.username == null || user.username.trim().isEmpty()) throw new IllegalArgumentException("Tên đăng nhập là bắt buộc");
        if (user.role == null || user.role.trim().isEmpty()) throw new IllegalArgumentException("Vai trò là bắt buộc");
        SQLiteDatabase db = helper.getWritableDatabase();
        db.beginTransaction();
        try {
            long employeeId = user.employeeId;
            ContentValues employee = new ContentValues();
            employee.put("full_name", user.fullName == null || user.fullName.trim().isEmpty() ? user.username.trim() : user.fullName.trim());
            employee.put("phone", user.phone);
            employee.put("email", user.email);
            employee.put("position", user.position);
            employee.put("updated_at", System.currentTimeMillis());
            if (employeeId <= 0) {
                employee.put("employee_code", "EMP-" + System.currentTimeMillis());
                employee.put("created_at", System.currentTimeMillis());
                employeeId = db.insertOrThrow("employees", null, employee);
            } else {
                db.update("employees", employee, "id=?", new String[]{String.valueOf(employeeId)});
            }
            ContentValues account = new ContentValues();
            account.put("username", user.username.trim());
            account.put("employee_id", employeeId);
            account.put("role", user.role);
            account.put("is_active", user.active ? 1 : 0);
            account.put("updated_at", System.currentTimeMillis());
            if (password != null && !password.trim().isEmpty()) account.put("password_hash", PasswordUtils.hash(password));
            long id;
            if (user.id <= 0) {
                if (password == null || password.trim().isEmpty()) throw new IllegalArgumentException("Mật khẩu là bắt buộc khi tạo tài khoản");
                account.put("created_at", System.currentTimeMillis());
                id = db.insertOrThrow("users", null, account);
            } else {
                db.update("users", account, "id=?", new String[]{String.valueOf(user.id)});
                id = user.id;
            }
            db.setTransactionSuccessful();
            return id;
        } finally {
            db.endTransaction();
        }
    }

    public void setActive(long userId, boolean active) {
        ContentValues values = new ContentValues();
        values.put("is_active", active ? 1 : 0);
        values.put("updated_at", System.currentTimeMillis());
        helper.getWritableDatabase().update("users", values, "id=?", new String[]{String.valueOf(userId)});
    }

    private User map(Cursor cursor) {
        User user = new User();
        user.id = cursor.getLong(0);
        user.employeeId = cursor.isNull(1) ? -1 : cursor.getLong(1);
        user.username = cursor.getString(2);
        user.role = cursor.getString(3);
        user.active = cursor.getInt(4) == 1;
        user.fullName = cursor.getString(5);
        user.phone = cursor.getString(6);
        user.email = cursor.getString(7);
        user.position = cursor.getString(8);
        return user;
    }
}

package com.example.mpos.dao;

import android.database.Cursor;

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
            User user = new User(); user.id = cursor.getLong(0); user.employeeId = cursor.isNull(1) ? -1 : cursor.getLong(1); user.username = cursor.getString(2); user.role = cursor.getString(3); return user;
        } finally { cursor.close(); }
    }
}

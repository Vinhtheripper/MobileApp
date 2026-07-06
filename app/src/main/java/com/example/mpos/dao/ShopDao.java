package com.example.mpos.dao;

import android.content.ContentValues;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

import com.example.mpos.database.DatabaseHelper;
import com.example.mpos.model.Shop;

import java.util.ArrayList;
import java.util.List;

public class ShopDao {
    private final DatabaseHelper helper;
    public ShopDao(DatabaseHelper helper) { this.helper = helper; }

    public List<Shop> getShopsForUser(long userId) {
        List<Shop> result = new ArrayList<>();
        Cursor c = helper.getReadableDatabase().rawQuery(
            "SELECT s.id, s.name, s.address, s.phone, s.logo_uri, s.owner_user_id, s.created_at, sm.role " +
            "FROM shops s INNER JOIN shop_members sm ON sm.shop_id=s.id " +
            "WHERE sm.user_id=? ORDER BY s.created_at ASC",
            new String[]{String.valueOf(userId)});
        try {
            while (c.moveToNext()) {
                Shop shop = new Shop();
                shop.id          = c.getLong(0);
                shop.name        = c.getString(1);
                shop.address     = c.isNull(2) ? "" : c.getString(2);
                shop.phone       = c.isNull(3) ? "" : c.getString(3);
                shop.logoUri     = c.isNull(4) ? null : c.getString(4);
                shop.ownerUserId = c.getLong(5);
                shop.createdAt   = c.getLong(6);
                shop.memberRole  = c.getString(7);
                result.add(shop);
            }
        } finally { c.close(); }
        return result;
    }

    public Shop getById(long shopId) {
        Cursor c = helper.getReadableDatabase().rawQuery(
            "SELECT id, name, address, phone, logo_uri, owner_user_id, created_at FROM shops WHERE id=?",
            new String[]{String.valueOf(shopId)});
        try {
            if (!c.moveToFirst()) return null;
            Shop shop = new Shop();
            shop.id          = c.getLong(0);
            shop.name        = c.getString(1);
            shop.address     = c.isNull(2) ? "" : c.getString(2);
            shop.phone       = c.isNull(3) ? "" : c.getString(3);
            shop.logoUri     = c.isNull(4) ? null : c.getString(4);
            shop.ownerUserId = c.getLong(5);
            shop.createdAt   = c.getLong(6);
            return shop;
        } finally { c.close(); }
    }

    public long create(String name, String address, String phone, String logoUri, long ownerUserId) {
        SQLiteDatabase db = helper.getWritableDatabase();
        db.beginTransaction();
        try {
            ContentValues v = new ContentValues();
            v.put("name", name.trim());
            if (address != null && !address.trim().isEmpty()) v.put("address", address.trim());
            if (phone != null && !phone.trim().isEmpty()) v.put("phone", phone.trim());
            if (logoUri != null && !logoUri.isEmpty()) v.put("logo_uri", logoUri);
            v.put("owner_user_id", ownerUserId);
            v.put("created_at", System.currentTimeMillis());
            long shopId = db.insertOrThrow("shops", null, v);

            addMember(db, shopId, ownerUserId, "OWNER");
            db.setTransactionSuccessful();
            return shopId;
        } finally { db.endTransaction(); }
    }

    public void addMember(long shopId, long userId, String role) {
        addMember(helper.getWritableDatabase(), shopId, userId, role);
    }

    private void addMember(SQLiteDatabase db, long shopId, long userId, String role) {
        ContentValues v = new ContentValues();
        v.put("shop_id", shopId);
        v.put("user_id", userId);
        v.put("role", role);
        v.put("created_at", System.currentTimeMillis());
        try { db.insertOrThrow("shop_members", null, v); } catch (Exception ignored) {}
    }

    public String getMemberRole(long shopId, long userId) {
        Cursor c = helper.getReadableDatabase().rawQuery(
            "SELECT role FROM shop_members WHERE shop_id=? AND user_id=?",
            new String[]{String.valueOf(shopId), String.valueOf(userId)});
        try { return c.moveToFirst() ? c.getString(0) : null; } finally { c.close(); }
    }

    public boolean isMember(long shopId, long userId) {
        return getMemberRole(shopId, userId) != null;
    }

    public List<Shop.Member> getMembers(long shopId) {
        List<Shop.Member> result = new ArrayList<>();
        Cursor c = helper.getReadableDatabase().rawQuery(
            "SELECT u.id, e.full_name, u.username, sm.role " +
            "FROM shop_members sm " +
            "INNER JOIN users u ON u.id=sm.user_id " +
            "LEFT JOIN employees e ON e.id=u.employee_id " +
            "WHERE sm.shop_id=? ORDER BY sm.role",
            new String[]{String.valueOf(shopId)});
        try {
            while (c.moveToNext()) {
                Shop.Member m = new Shop.Member();
                m.userId   = c.getLong(0);
                m.fullName = c.isNull(1) ? c.getString(2) : c.getString(1);
                m.email    = c.getString(2);
                m.role     = c.getString(3);
                result.add(m);
            }
        } finally { c.close(); }
        return result;
    }

    public void updateName(long shopId, String name) {
        ContentValues v = new ContentValues();
        v.put("name", name.trim());
        helper.getWritableDatabase().update("shops", v, "id=?", new String[]{String.valueOf(shopId)});
    }
}
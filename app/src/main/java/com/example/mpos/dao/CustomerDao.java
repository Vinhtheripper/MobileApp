package com.example.mpos.dao;

import android.content.ContentValues;
import android.database.Cursor;

import com.example.mpos.database.DatabaseHelper;
import com.example.mpos.model.Customer;

import java.util.ArrayList;
import java.util.List;

public class CustomerDao {
    private final DatabaseHelper helper;
    private final long shopId;

    public CustomerDao(DatabaseHelper helper, long shopId) {
        this.helper = helper;
        this.shopId = shopId;
    }

    public List<Customer> search(String term) {
        Cursor c;
        if (term == null || term.trim().isEmpty()) {
            c = helper.getReadableDatabase().rawQuery(
                "SELECT id,full_name,phone,email,address,loyalty_points FROM customers " +
                "WHERE shop_id=? ORDER BY COALESCE(updated_at, created_at) DESC LIMIT 200",
                new String[]{String.valueOf(shopId)});
        } else {
            String p = "%" + term.trim() + "%";
            c = helper.getReadableDatabase().rawQuery(
                "SELECT id,full_name,phone,email,address,loyalty_points FROM customers " +
                "WHERE shop_id=? AND (COALESCE(full_name,'') LIKE ? OR COALESCE(phone,'') LIKE ?) " +
                "ORDER BY COALESCE(updated_at, created_at) DESC LIMIT 100",
                new String[]{String.valueOf(shopId), p, p});
        }
        List<Customer> result = new ArrayList<>();
        try { while (c.moveToNext()) result.add(map(c)); } finally { c.close(); }
        return result;
    }

    public Customer findById(long id) {
        Cursor c = helper.getReadableDatabase().rawQuery(
            "SELECT id,full_name,phone,email,address,loyalty_points FROM customers WHERE id=?",
            new String[]{String.valueOf(id)});
        try { return c.moveToFirst() ? map(c) : null; } finally { c.close(); }
    }

    public Customer findByPhone(String phone) {
        Cursor c = helper.getReadableDatabase().rawQuery(
            "SELECT id,full_name,phone,email,address,loyalty_points FROM customers " +
            "WHERE phone=? AND shop_id=?",
            new String[]{phone.trim(), String.valueOf(shopId)});
        try { return c.moveToFirst() ? map(c) : null; } finally { c.close(); }
    }

    public long save(Customer customer) {
        ContentValues v = new ContentValues();
        v.put("full_name", customer.fullName);
        v.put("phone", customer.phone);
        v.put("email", customer.email);
        v.put("address", customer.address);
        v.put("shop_id", shopId);
        v.put("updated_at", System.currentTimeMillis());
        if (customer.id <= 0) {
            v.put("created_at", System.currentTimeMillis());
            return helper.getWritableDatabase().insertOrThrow("customers", null, v);
        }
        helper.getWritableDatabase().update("customers", v, "id=?", new String[]{String.valueOf(customer.id)});
        return customer.id;
    }

    public void delete(long id) {
        helper.getWritableDatabase().delete("customers", "id=?", new String[]{String.valueOf(id)});
    }

    public long findOrCreate(String phone) {
        if (phone == null || phone.trim().isEmpty()) return -1;
        Customer existing = findByPhone(phone);
        if (existing != null) return existing.id;
        long now = System.currentTimeMillis();
        ContentValues v = new ContentValues();
        v.put("phone", phone.trim());
        v.put("shop_id", shopId);
        v.put("created_at", now);
        v.put("updated_at", now);
        return helper.getWritableDatabase().insertOrThrow("customers", null, v);
    }

    private Customer map(Cursor c) {
        Customer x = new Customer();
        x.id           = c.getLong(0);
        x.fullName     = c.getString(1);
        x.phone        = c.getString(2);
        x.email        = c.getString(3);
        x.address      = c.getString(4);
        x.loyaltyPoints = c.getInt(5);
        return x;
    }
}
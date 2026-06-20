package com.example.mpos.dao;

import android.content.ContentValues;
import android.database.Cursor;

import com.example.mpos.database.DatabaseHelper;
import com.example.mpos.model.Product;

import java.util.ArrayList;
import java.util.List;

public class ProductDao {
    private final DatabaseHelper helper;
    public ProductDao(DatabaseHelper helper) { this.helper = helper; }
    public List<Product> search(String query) {
        String pattern = "%" + query.trim() + "%";
        Cursor cursor = helper.getReadableDatabase().rawQuery("SELECT id, barcode, sku, name, sale_price, stock_quantity, min_stock_quantity FROM products WHERE is_active=1 AND (name LIKE ? OR sku LIKE ? OR barcode LIKE ?) ORDER BY name LIMIT 100", new String[]{pattern, pattern, pattern});
        List<Product> result = new ArrayList<>();
        try { while (cursor.moveToNext()) result.add(map(cursor)); } finally { cursor.close(); }
        return result;
    }
    public Product findByBarcodeOrSku(String value) {
        Cursor cursor = helper.getReadableDatabase().rawQuery("SELECT id, barcode, sku, name, sale_price, stock_quantity, min_stock_quantity FROM products WHERE is_active=1 AND (barcode=? OR sku=?)", new String[]{value, value});
        try { return cursor.moveToFirst() ? map(cursor) : null; } finally { cursor.close(); }
    }
    public long save(Product product) {
        ContentValues values = new ContentValues(); values.put("barcode", product.barcode); values.put("sku", product.sku); values.put("name", product.name); values.put("sale_price", product.salePrice); values.put("stock_quantity", product.stockQuantity); values.put("min_stock_quantity", product.minStockQuantity); values.put("updated_at", System.currentTimeMillis());
        if (product.id <= 0) { values.put("created_at", System.currentTimeMillis()); return helper.getWritableDatabase().insertOrThrow("products", null, values); }
        helper.getWritableDatabase().update("products", values, "id=?", new String[]{String.valueOf(product.id)}); return product.id;
    }
    public void softDelete(long id) { ContentValues values = new ContentValues(); values.put("is_active", 0); values.put("updated_at", System.currentTimeMillis()); helper.getWritableDatabase().update("products", values, "id=?", new String[]{String.valueOf(id)}); }
    private Product map(Cursor c) { Product p = new Product(); p.id=c.getLong(0); p.barcode=c.getString(1); p.sku=c.getString(2); p.name=c.getString(3); p.salePrice=c.getLong(4); p.stockQuantity=c.getInt(5); p.minStockQuantity=c.getInt(6); return p; }
}

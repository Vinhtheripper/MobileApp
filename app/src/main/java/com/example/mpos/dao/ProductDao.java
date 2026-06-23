package com.example.mpos.dao;

import android.content.ContentValues;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

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
    public Product findById(long id) {
        Cursor cursor = helper.getReadableDatabase().rawQuery("SELECT id, barcode, sku, name, sale_price, stock_quantity, min_stock_quantity FROM products WHERE id=?", new String[]{String.valueOf(id)});
        try { return cursor.moveToFirst() ? map(cursor) : null; } finally { cursor.close(); }
    }
    public long save(Product product) {
        ContentValues values = new ContentValues(); values.put("barcode", product.barcode); values.put("sku", product.sku); values.put("name", product.name); values.put("sale_price", product.salePrice); values.put("stock_quantity", product.stockQuantity); values.put("min_stock_quantity", product.minStockQuantity); values.put("updated_at", System.currentTimeMillis());
        if (product.id <= 0) { values.put("created_at", System.currentTimeMillis()); return helper.getWritableDatabase().insertOrThrow("products", null, values); }
        helper.getWritableDatabase().update("products", values, "id=?", new String[]{String.valueOf(product.id)}); return product.id;
    }
    public void softDelete(long id) { ContentValues values = new ContentValues(); values.put("is_active", 0); values.put("updated_at", System.currentTimeMillis()); helper.getWritableDatabase().update("products", values, "id=?", new String[]{String.valueOf(id)}); }
    public void adjustStock(long productId, int newQuantity, long userId, String note) {
        if (newQuantity < 0) throw new IllegalArgumentException("Tồn kho không thể âm");
        SQLiteDatabase db = helper.getWritableDatabase(); db.beginTransaction();
        try {
            Cursor cursor = db.rawQuery("SELECT stock_quantity FROM products WHERE id=?", new String[]{String.valueOf(productId)});
            int before; try { if (!cursor.moveToFirst()) throw new IllegalArgumentException("Sản phẩm không tồn tại"); before = cursor.getInt(0); } finally { cursor.close(); }
            ContentValues product = new ContentValues(); product.put("stock_quantity", newQuantity); product.put("updated_at", System.currentTimeMillis()); db.update("products", product, "id=?", new String[]{String.valueOf(productId)});
            ContentValues movement = new ContentValues(); movement.put("product_id", productId); movement.put("user_id", userId); movement.put("transaction_type", "ADJUSTMENT"); movement.put("quantity_change", newQuantity - before); movement.put("quantity_before", before); movement.put("quantity_after", newQuantity); movement.put("note", note); movement.put("created_at", System.currentTimeMillis()); db.insertOrThrow("inventory_transactions", null, movement);
            db.setTransactionSuccessful();
        } finally { db.endTransaction(); }
    }
    private Product map(Cursor c) { Product p = new Product(); p.id=c.getLong(0); p.barcode=c.getString(1); p.sku=c.getString(2); p.name=c.getString(3); p.salePrice=c.getLong(4); p.stockQuantity=c.getInt(5); p.minStockQuantity=c.getInt(6); return p; }
}

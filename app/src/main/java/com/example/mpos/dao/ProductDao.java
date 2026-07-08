package com.example.mpos.dao;

import android.content.ContentValues;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

import com.example.mpos.database.DatabaseHelper;
import com.example.mpos.model.Category;
import com.example.mpos.model.Product;

import java.util.ArrayList;
import java.util.List;

public class ProductDao {
    private static final String COLS = "id,barcode,sku,name,sale_price,stock_quantity,min_stock_quantity,category_id,image_uri,cost_price,description";
    private final DatabaseHelper helper;
    private final long shopId;

    public ProductDao(DatabaseHelper helper, long shopId) {
        this.helper = helper;
        this.shopId = shopId;
    }

    public List<Product> search(String query) {
        String p = "%" + query.trim() + "%";
        Cursor c = helper.getReadableDatabase().rawQuery(
            "SELECT " + COLS + " FROM products WHERE is_active=1 AND shop_id=? AND (name LIKE ? OR sku LIKE ? OR barcode LIKE ?) ORDER BY name LIMIT 100",
            new String[]{String.valueOf(shopId), p, p, p});
        return mapAll(c);
    }

    public List<Product> searchByCategory(long categoryId, String query) {
        if (categoryId == Category.ALL_ID) return search(query);
        String p = "%" + query.trim() + "%";
        Cursor c = helper.getReadableDatabase().rawQuery(
            "SELECT " + COLS + " FROM products WHERE is_active=1 AND shop_id=? AND category_id=? AND (name LIKE ? OR sku LIKE ? OR barcode LIKE ?) ORDER BY name LIMIT 100",
            new String[]{String.valueOf(shopId), String.valueOf(categoryId), p, p, p});
        return mapAll(c);
    }

    public List<Product> getAll() { return search(""); }

    public Product findByBarcodeOrSku(String value) {
        Cursor c = helper.getReadableDatabase().rawQuery(
            "SELECT " + COLS + " FROM products WHERE is_active=1 AND shop_id=? AND (barcode=? OR sku=?)",
            new String[]{String.valueOf(shopId), value, value});
        try { return c.moveToFirst() ? map(c) : null; } finally { c.close(); }
    }

    public Product findBySku(String sku) {
        Cursor c = helper.getReadableDatabase().rawQuery(
            "SELECT " + COLS + " FROM products WHERE shop_id=? AND sku=? LIMIT 1",
            new String[]{String.valueOf(shopId), sku});
        try { return c.moveToFirst() ? map(c) : null; } finally { c.close(); }
    }

    public Product findById(long id) {
        Cursor c = helper.getReadableDatabase().rawQuery(
            "SELECT " + COLS + " FROM products WHERE id=?", new String[]{String.valueOf(id)});
        try { return c.moveToFirst() ? map(c) : null; } finally { c.close(); }
    }

    public long save(Product product) {
        ContentValues v = new ContentValues();
        // NULL không vi phạm UNIQUE, empty string thì bị — chuyển empty → null
        String sku     = (product.sku     != null && !product.sku.isEmpty())     ? product.sku     : null;
        String barcode = (product.barcode != null && !product.barcode.isEmpty()) ? product.barcode : null;
        v.put("barcode", barcode); v.put("sku", sku); v.put("name", product.name);
        v.put("sale_price", product.salePrice); v.put("stock_quantity", product.stockQuantity);
        v.put("min_stock_quantity", product.minStockQuantity);
        if (product.categoryId > 0) v.put("category_id", product.categoryId);
        if (product.costPrice > 0)  v.put("cost_price", product.costPrice);
        if (product.description != null) v.put("description", product.description);
        v.put("image_uri", product.imageUri);
        v.put("shop_id", shopId);
        v.put("updated_at", System.currentTimeMillis());
        if (product.id <= 0) {
            v.put("created_at", System.currentTimeMillis());
            return helper.getWritableDatabase().insertOrThrow("products", null, v);
        }
        helper.getWritableDatabase().update("products", v, "id=?", new String[]{String.valueOf(product.id)});
        return product.id;
    }

    public void softDelete(long id) {
        ContentValues v = new ContentValues();
        v.put("is_active", 0);
        v.put("updated_at", System.currentTimeMillis());
        helper.getWritableDatabase().update("products", v, "id=?", new String[]{String.valueOf(id)});
    }

    public void adjustStock(long productId, int newQuantity, long userId, String note) {
        if (newQuantity < 0) throw new IllegalArgumentException("Tồn kho không thể âm");
        SQLiteDatabase db = helper.getWritableDatabase();
        db.beginTransaction();
        try {
            Cursor c = db.rawQuery("SELECT stock_quantity FROM products WHERE id=?", new String[]{String.valueOf(productId)});
            int before;
            try {
                if (!c.moveToFirst()) throw new IllegalArgumentException("Sản phẩm không tồn tại");
                before = c.getInt(0);
            } finally { c.close(); }
            ContentValues pv = new ContentValues();
            pv.put("stock_quantity", newQuantity);
            pv.put("updated_at", System.currentTimeMillis());
            db.update("products", pv, "id=?", new String[]{String.valueOf(productId)});
            ContentValues mv = new ContentValues();
            mv.put("product_id", productId); mv.put("user_id", userId);
            mv.put("transaction_type", "ADJUSTMENT"); mv.put("quantity_change", newQuantity - before);
            mv.put("quantity_before", before); mv.put("quantity_after", newQuantity);
            mv.put("note", note); mv.put("created_at", System.currentTimeMillis());
            db.insertOrThrow("inventory_transactions", null, mv);
            db.setTransactionSuccessful();
        } finally { db.endTransaction(); }
    }

    private List<Product> mapAll(Cursor c) {
        List<Product> result = new ArrayList<>();
        try { while (c.moveToNext()) result.add(map(c)); } finally { c.close(); }
        return result;
    }

    private Product map(Cursor c) {
        Product p = new Product();
        p.id               = c.getLong(0);
        p.barcode          = c.getString(1);
        p.sku              = c.getString(2);
        p.name             = c.getString(3);
        p.salePrice        = c.getLong(4);
        p.stockQuantity    = c.getInt(5);
        p.minStockQuantity = c.getInt(6);
        p.categoryId       = c.getLong(7);
        p.imageUri         = c.isNull(8) ? null : c.getString(8);
        p.costPrice        = c.isNull(9)  ? 0    : c.getLong(9);
        p.description      = c.isNull(10) ? null : c.getString(10);
        return p;
    }

    public void delete(long id) { softDelete(id); }
}
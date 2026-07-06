package com.example.mpos.dao;

import android.content.ContentValues;
import android.database.Cursor;
import com.example.mpos.database.DatabaseHelper;
import com.example.mpos.model.Category;
import java.util.ArrayList;
import java.util.List;

public class CategoryDao {
    private final DatabaseHelper helper;
    private final long shopId;

    public CategoryDao(DatabaseHelper helper, long shopId) {
        this.helper = helper;
        this.shopId = shopId;
    }

    public List<Category> getAll() {
        List<Category> result = new ArrayList<>();
        result.add(new Category(Category.ALL_ID, "Tất cả"));
        Cursor c = helper.getReadableDatabase().rawQuery(
            "SELECT id, name FROM categories WHERE is_active=1 AND shop_id=? ORDER BY name",
            new String[]{String.valueOf(shopId)});
        try { while (c.moveToNext()) result.add(new Category(c.getLong(0), c.getString(1))); }
        finally { c.close(); }
        return result;
    }

    public long save(String name) {
        ContentValues v = new ContentValues();
        v.put("name", name.trim());
        v.put("shop_id", shopId);
        v.put("is_active", 1);
        v.put("created_at", System.currentTimeMillis());
        try { return helper.getWritableDatabase().insertOrThrow("categories", null, v); }
        catch (Exception e) { return -1; }
    }
}
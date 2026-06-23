package com.example.mpos.dao;

import android.content.ContentValues;
import android.database.Cursor;

import com.example.mpos.database.DatabaseHelper;

public class SettingsDao {
    private final DatabaseHelper helper;
    public SettingsDao(DatabaseHelper helper) { this.helper = helper; }
    public String get(String key, String fallback) { Cursor c=helper.getReadableDatabase().rawQuery("SELECT setting_value FROM settings WHERE setting_key=?",new String[]{key});try{return c.moveToFirst()?c.getString(0):fallback;}finally{c.close();} }
    public void put(String key, String value) { ContentValues v=new ContentValues();v.put("setting_key",key);v.put("setting_value",value);v.put("updated_at",System.currentTimeMillis());helper.getWritableDatabase().insertWithOnConflict("settings",null,v,android.database.sqlite.SQLiteDatabase.CONFLICT_REPLACE); }
}

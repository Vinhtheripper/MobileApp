package com.example.mpos.dao;

import android.content.ContentValues;
import android.database.Cursor;

import com.example.mpos.database.DatabaseHelper;

public class ShiftDao {
    private final DatabaseHelper helper;
    public ShiftDao(DatabaseHelper helper) { this.helper = helper; }
    public long getOpenShiftId(long userId) {
        Cursor c = helper.getReadableDatabase().rawQuery("SELECT id FROM shifts WHERE user_id=? AND status='OPEN' ORDER BY id DESC LIMIT 1", new String[]{String.valueOf(userId)});
        try { return c.moveToFirst() ? c.getLong(0) : -1; } finally { c.close(); }
    }
    public long open(long userId, long openingCash) {
        ContentValues v = new ContentValues(); long now=System.currentTimeMillis(); v.put("shift_code", "SHIFT-"+now); v.put("user_id", userId); v.put("opened_at", now); v.put("opening_cash", openingCash); v.put("status", "OPEN"); return helper.getWritableDatabase().insertOrThrow("shifts", null, v);
    }
    public void close(long shiftId, long actualCash, String note) {
        ContentValues v = new ContentValues(); v.put("closed_at", System.currentTimeMillis()); v.put("actual_cash", actualCash); v.put("handover_note", note); v.put("status", "CLOSED"); helper.getWritableDatabase().update("shifts", v, "id=?", new String[]{String.valueOf(shiftId)});
    }
}

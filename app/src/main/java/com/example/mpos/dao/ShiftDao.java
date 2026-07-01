package com.example.mpos.dao;

import android.content.ContentValues;
import android.database.Cursor;

import com.example.mpos.constants.PaymentConstants;
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
        Cursor cursor = helper.getReadableDatabase().rawQuery("SELECT opening_cash, COALESCE(SUM(CASE WHEN p.method='CASH' AND p.status=? THEN p.amount ELSE 0 END),0) FROM shifts s LEFT JOIN orders o ON o.shift_id=s.id LEFT JOIN payments p ON p.order_id=o.id WHERE s.id=?", new String[]{PaymentConstants.STATUS_SUCCESS, String.valueOf(shiftId)});
        long expected = 0; try { if (cursor.moveToFirst()) expected = cursor.getLong(0) + cursor.getLong(1); } finally { cursor.close(); }
        ContentValues v = new ContentValues(); v.put("closed_at", System.currentTimeMillis()); v.put("expected_cash", expected); v.put("actual_cash", actualCash); v.put("difference_amount", actualCash - expected); v.put("handover_note", note); v.put("status", "CLOSED"); helper.getWritableDatabase().update("shifts", v, "id=?", new String[]{String.valueOf(shiftId)});
    }
}

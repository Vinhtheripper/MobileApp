package com.example.mpos.sync;

import android.content.ContentValues;
import android.database.sqlite.SQLiteDatabase;

import com.example.mpos.database.DatabaseHelper;

/** Local mock transport. Replace markSuccessful with Retrofit/Firebase later. */
public class SyncQueueProcessor {
    private final DatabaseHelper helper;
    public SyncQueueProcessor(DatabaseHelper helper) { this.helper = helper; }
    public int processPending() {
        SQLiteDatabase db = helper.getWritableDatabase();
        ContentValues v = new ContentValues(); v.put("status", "SYNCED"); v.put("updated_at", System.currentTimeMillis());
        return db.update("sync_queue", v, "status='PENDING'", null);
    }
}

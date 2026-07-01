package com.example.mpos.sync;

import android.content.ContentValues;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

import com.example.mpos.constants.SyncConstants;
import com.example.mpos.database.DatabaseHelper;
import com.example.mpos.service.ChannelSyncServiceMock;

/** Local mock transport. Replace markSuccessful with Retrofit/Firebase later. */
public class SyncQueueProcessor {
    private final DatabaseHelper helper;
    private final ChannelSyncServiceMock channelSync = new ChannelSyncServiceMock();
    public SyncQueueProcessor(DatabaseHelper helper) { this.helper = helper; }
    public int processPending() {
        SQLiteDatabase db = helper.getWritableDatabase();
        Cursor cursor = db.rawQuery("SELECT id,event_type,payload FROM sync_queue WHERE status IN (?,?) ORDER BY created_at", new String[]{SyncConstants.STATUS_PENDING, SyncConstants.STATUS_FAILED});
        int processed = 0;
        try {
            while (cursor.moveToNext()) {
                long id = cursor.getLong(0);
                String eventType = cursor.getString(1);
                String payload = cursor.getString(2);
                boolean ok = channelSync.push(eventType, payload);
                ContentValues values = new ContentValues();
                values.put("status", ok ? SyncConstants.STATUS_SYNCED : SyncConstants.STATUS_FAILED);
                values.put("updated_at", System.currentTimeMillis());
                if (!ok) {
                    values.put("last_error", "Mock network unavailable");
                    db.execSQL("UPDATE sync_queue SET retry_count=retry_count+1 WHERE id=?", new Object[]{id});
                }
                db.update("sync_queue", values, "id=?", new String[]{String.valueOf(id)});
                processed++;
            }
        } finally {
            cursor.close();
        }
        return processed;
    }
}

package com.example.mpos.sync;

import android.content.ContentValues;
import android.database.sqlite.SQLiteDatabase;

public final class AuditLogger {
    private AuditLogger() { }
    public static void log(SQLiteDatabase db, long userId, String action, String entity, long entityId, String detail) {
        ContentValues v = new ContentValues(); v.put("user_id", userId); v.put("action", action); v.put("entity_type", entity); v.put("entity_id", entityId); v.put("detail", detail); v.put("created_at", System.currentTimeMillis()); db.insert("audit_logs", null, v);
    }
}

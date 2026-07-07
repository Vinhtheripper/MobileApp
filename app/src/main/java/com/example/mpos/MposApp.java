package com.example.mpos;

import android.app.Application;

import com.example.mpos.sync.SyncWorker;

public class MposApp extends Application {
    @Override
    public void onCreate() {
        super.onCreate();

        // Pre-warm DB connection on background thread
        new Thread(() -> {
            try {
                com.example.mpos.database.DatabaseHelper helper =
                    new com.example.mpos.database.DatabaseHelper(this);
                helper.getReadableDatabase().close();
            } catch (Exception ignored) { }
        }, "db-prewarm").start();

        // Schedule periodic Firestore sync (runs every 15 min when online)
        SyncWorker.schedule(this);
    }
}

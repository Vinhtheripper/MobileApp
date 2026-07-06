package com.example.mpos;

import android.app.Application;
import android.os.StrictMode;

public class MposApp extends Application {
    @Override
    public void onCreate() {
        super.onCreate();
        // Pre-warm database connection in background to avoid I/O on main thread
        // when user taps Login button for the first time
        new Thread(() -> {
            try {
                com.example.mpos.database.DatabaseHelper helper =
                    new com.example.mpos.database.DatabaseHelper(this);
                // Open DB now (triggers onCreate/onUpgrade) rather than on first button tap
                helper.getReadableDatabase().close();
            } catch (Exception ignored) { }
        }, "db-prewarm").start();
    }
}

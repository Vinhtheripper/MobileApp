package com.example.mpos.service;

import android.util.Log;

public class NotificationServiceMock {
    public void notifyManagers(String message) {
        Log.d("mPOS Notification", message == null ? "" : message);
    }
}

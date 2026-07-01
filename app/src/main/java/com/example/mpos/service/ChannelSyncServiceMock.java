package com.example.mpos.service;

public class ChannelSyncServiceMock {
    private boolean networkAvailable = true;

    public void setNetworkAvailable(boolean networkAvailable) {
        this.networkAvailable = networkAvailable;
    }

    public boolean push(String eventType, String payload) {
        return networkAvailable && eventType != null && !eventType.contains("FAIL");
    }
}

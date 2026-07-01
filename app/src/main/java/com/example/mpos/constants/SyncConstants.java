package com.example.mpos.constants;

public final class SyncConstants {
    private SyncConstants() { }

    public static final String STATUS_PENDING = "PENDING";
    public static final String STATUS_SYNCED = "SYNCED";
    public static final String STATUS_FAILED = "FAILED";

    public static final String EVENT_ORDER_CREATE = "ORDER_CREATE";
    public static final String EVENT_ORDER_CONFIRM = "ORDER_CONFIRM";
    public static final String EVENT_PAYMENT_COMPLETE = "PAYMENT_COMPLETE";
    public static final String EVENT_INVENTORY_LOCK = "INVENTORY_LOCK";
    public static final String EVENT_CUSTOMER_CREATE = "CUSTOMER_CREATE";
    public static final String EVENT_CUSTOMER_UPDATE = "CUSTOMER_UPDATE";
    public static final String EVENT_SHIPMENT_CREATE = "SHIPMENT_CREATE";
    public static final String EVENT_SHIFT_OPEN = "SHIFT_OPEN";
    public static final String EVENT_SHIFT_CLOSE = "SHIFT_CLOSE";
}

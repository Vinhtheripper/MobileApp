package com.example.mpos.database;

/** Central table/column names. Keep SQL out of Activities. */
public final class DatabaseContract {
    private DatabaseContract() { }

    public static final String DATABASE_NAME = "mpos.db";
    public static final int DATABASE_VERSION = 2;

    public static final String USERS = "users";
    public static final String EMPLOYEES = "employees";
    public static final String CATEGORIES = "categories";
    public static final String PRODUCTS = "products";
    public static final String CUSTOMERS = "customers";
    public static final String ORDERS = "orders";
    public static final String ORDER_ITEMS = "order_items";
    public static final String PAYMENTS = "payments";
    public static final String INVENTORY_TRANSACTIONS = "inventory_transactions";
    public static final String SHIFTS = "shifts";
    public static final String RECEIPTS = "receipts";
    public static final String SETTINGS = "settings";
    public static final String SYNC_QUEUE = "sync_queue";
    public static final String AUDIT_LOGS = "audit_logs";
}

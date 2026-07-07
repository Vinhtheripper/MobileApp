package com.example.mpos.database;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

/** SQLite entry point. DAOs obtain readable/writable databases from this class. */
public class DatabaseHelper extends SQLiteOpenHelper {
    public DatabaseHelper(Context context) {
        super(context, DatabaseContract.DATABASE_NAME, null, DatabaseContract.DATABASE_VERSION);
    }

    @Override
    public void onConfigure(SQLiteDatabase db) {
        super.onConfigure(db);
        db.setForeignKeyConstraintsEnabled(true);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL("CREATE TABLE employees (id INTEGER PRIMARY KEY AUTOINCREMENT, employee_code TEXT UNIQUE, full_name TEXT NOT NULL, phone TEXT, email TEXT, position TEXT, is_active INTEGER NOT NULL DEFAULT 1, created_at INTEGER, updated_at INTEGER)");
        db.execSQL("CREATE TABLE users (id INTEGER PRIMARY KEY AUTOINCREMENT, username TEXT UNIQUE NOT NULL, password_hash TEXT NOT NULL, employee_id INTEGER, role TEXT NOT NULL, is_active INTEGER NOT NULL DEFAULT 1, created_at INTEGER, updated_at INTEGER, FOREIGN KEY(employee_id) REFERENCES employees(id))");
        db.execSQL("CREATE TABLE shops (id INTEGER PRIMARY KEY AUTOINCREMENT, name TEXT NOT NULL, address TEXT, phone TEXT, logo_uri TEXT, owner_user_id INTEGER NOT NULL, created_at INTEGER)");
        db.execSQL("CREATE TABLE shop_members (id INTEGER PRIMARY KEY AUTOINCREMENT, shop_id INTEGER NOT NULL, user_id INTEGER NOT NULL, role TEXT NOT NULL DEFAULT 'STAFF', created_at INTEGER, UNIQUE(shop_id, user_id), FOREIGN KEY(shop_id) REFERENCES shops(id), FOREIGN KEY(user_id) REFERENCES users(id))");
        db.execSQL("CREATE TABLE categories (id INTEGER PRIMARY KEY AUTOINCREMENT, name TEXT NOT NULL, description TEXT, is_active INTEGER NOT NULL DEFAULT 1, shop_id INTEGER NOT NULL DEFAULT 1, created_at INTEGER, updated_at INTEGER)");
        db.execSQL("CREATE TABLE products (id INTEGER PRIMARY KEY AUTOINCREMENT, barcode TEXT UNIQUE, sku TEXT UNIQUE, name TEXT NOT NULL, category_id INTEGER, cost_price INTEGER NOT NULL DEFAULT 0, sale_price INTEGER NOT NULL, stock_quantity INTEGER NOT NULL DEFAULT 0, min_stock_quantity INTEGER NOT NULL DEFAULT 0, image_uri TEXT, description TEXT, is_active INTEGER NOT NULL DEFAULT 1, shop_id INTEGER NOT NULL DEFAULT 1, created_at INTEGER, updated_at INTEGER, FOREIGN KEY(category_id) REFERENCES categories(id))");
        db.execSQL("CREATE TABLE customers (id INTEGER PRIMARY KEY AUTOINCREMENT, phone TEXT NOT NULL, full_name TEXT, email TEXT, address TEXT, loyalty_points INTEGER NOT NULL DEFAULT 0, shop_id INTEGER NOT NULL DEFAULT 1, created_at INTEGER, updated_at INTEGER, UNIQUE(phone, shop_id))");
        db.execSQL("CREATE TABLE shifts (id INTEGER PRIMARY KEY AUTOINCREMENT, shift_code TEXT UNIQUE NOT NULL, user_id INTEGER NOT NULL, shop_id INTEGER NOT NULL DEFAULT 1, opened_at INTEGER NOT NULL, closed_at INTEGER, opening_cash INTEGER NOT NULL, expected_cash INTEGER DEFAULT 0, actual_cash INTEGER, difference_amount INTEGER, status TEXT NOT NULL, handover_note TEXT, FOREIGN KEY(user_id) REFERENCES users(id))");
        db.execSQL("CREATE TABLE orders (id INTEGER PRIMARY KEY AUTOINCREMENT, order_code TEXT UNIQUE NOT NULL, customer_id INTEGER, user_id INTEGER NOT NULL, shift_id INTEGER, shop_id INTEGER NOT NULL DEFAULT 1, channel TEXT NOT NULL DEFAULT 'WALK_IN', status TEXT NOT NULL, subtotal INTEGER NOT NULL, discount_amount INTEGER NOT NULL DEFAULT 0, vat_percent INTEGER NOT NULL DEFAULT 0, vat_amount INTEGER NOT NULL DEFAULT 0, total_amount INTEGER NOT NULL, note TEXT, created_at INTEGER, updated_at INTEGER, FOREIGN KEY(customer_id) REFERENCES customers(id), FOREIGN KEY(user_id) REFERENCES users(id), FOREIGN KEY(shift_id) REFERENCES shifts(id))");

        db.execSQL("CREATE TABLE order_items (id INTEGER PRIMARY KEY AUTOINCREMENT, order_id INTEGER NOT NULL, product_id INTEGER NOT NULL, product_name TEXT NOT NULL, unit_price INTEGER NOT NULL, quantity INTEGER NOT NULL, discount_amount INTEGER NOT NULL DEFAULT 0, line_total INTEGER NOT NULL, FOREIGN KEY(order_id) REFERENCES orders(id), FOREIGN KEY(product_id) REFERENCES products(id))");
        db.execSQL("CREATE TABLE payments (id INTEGER PRIMARY KEY AUTOINCREMENT, order_id INTEGER NOT NULL, method TEXT NOT NULL, amount INTEGER NOT NULL, status TEXT NOT NULL, transaction_code TEXT, paid_at INTEGER, note TEXT, FOREIGN KEY(order_id) REFERENCES orders(id))");
        db.execSQL("CREATE TABLE inventory_transactions (id INTEGER PRIMARY KEY AUTOINCREMENT, product_id INTEGER NOT NULL, user_id INTEGER, order_id INTEGER, transaction_type TEXT NOT NULL, quantity_change INTEGER NOT NULL, quantity_before INTEGER NOT NULL, quantity_after INTEGER NOT NULL, note TEXT, created_at INTEGER, FOREIGN KEY(product_id) REFERENCES products(id), FOREIGN KEY(user_id) REFERENCES users(id), FOREIGN KEY(order_id) REFERENCES orders(id))");
        db.execSQL("CREATE TABLE receipts (id INTEGER PRIMARY KEY AUTOINCREMENT, order_id INTEGER UNIQUE NOT NULL, receipt_number TEXT UNIQUE NOT NULL, content TEXT, printed_at INTEGER, shared_at INTEGER, created_at INTEGER, FOREIGN KEY(order_id) REFERENCES orders(id))");
        db.execSQL("CREATE TABLE settings (id INTEGER PRIMARY KEY AUTOINCREMENT, setting_key TEXT UNIQUE NOT NULL, setting_value TEXT, updated_at INTEGER)");
        db.execSQL("CREATE TABLE sync_queue (id INTEGER PRIMARY KEY AUTOINCREMENT, entity_type TEXT NOT NULL, entity_id INTEGER NOT NULL, action_type TEXT NOT NULL, payload TEXT, status TEXT NOT NULL DEFAULT 'PENDING', retry_count INTEGER NOT NULL DEFAULT 0, last_error TEXT, created_at INTEGER, updated_at INTEGER)");
        db.execSQL("CREATE TABLE audit_logs (id INTEGER PRIMARY KEY AUTOINCREMENT, user_id INTEGER, action TEXT NOT NULL, entity_type TEXT NOT NULL, entity_id INTEGER, detail TEXT, created_at INTEGER NOT NULL, FOREIGN KEY(user_id) REFERENCES users(id))");
        db.execSQL("CREATE TABLE shipping_orders (id INTEGER PRIMARY KEY AUTOINCREMENT, order_id INTEGER, carrier TEXT NOT NULL, tracking_code TEXT, shipping_fee INTEGER NOT NULL DEFAULT 0, from_province TEXT, to_province TEXT, weight_gram INTEGER NOT NULL DEFAULT 500, status TEXT NOT NULL DEFAULT 'PENDING', created_at INTEGER, FOREIGN KEY(order_id) REFERENCES orders(id))");
        db.execSQL("CREATE INDEX idx_products_name ON products(name)");
        db.execSQL("CREATE INDEX idx_orders_created_at ON orders(created_at)");
        db.execSQL("CREATE INDEX idx_order_items_order_id ON order_items(order_id)");
        db.execSQL("CREATE INDEX idx_sync_queue_status ON sync_queue(status)");
        DatabaseSeeder.seed(db);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        if (oldVersion < 2) {
            db.execSQL("CREATE TABLE IF NOT EXISTS audit_logs (id INTEGER PRIMARY KEY AUTOINCREMENT, user_id INTEGER, action TEXT NOT NULL, entity_type TEXT NOT NULL, entity_id INTEGER, detail TEXT, created_at INTEGER NOT NULL, FOREIGN KEY(user_id) REFERENCES users(id))");
            db.execSQL("CREATE INDEX IF NOT EXISTS idx_sync_queue_status ON sync_queue(status)");
            DatabaseSeeder.seed(db);
        }
        if (oldVersion < 3) {
            db.execSQL("UPDATE users SET username='admin@mpos.com' WHERE username='admin'");
            DatabaseSeeder.seedProductsIfNeeded(db);
        }
        if (oldVersion < 4) {
            DatabaseSeeder.migrateV4(db);
        }
        if (oldVersion < 5) {
            DatabaseSeeder.migrateV5(db);
        }
        if (oldVersion < 6) {
            DatabaseSeeder.migrateV6(db);
        }
        if (oldVersion < 7) {
            DatabaseSeeder.migrateV7(db);
        }
        if (oldVersion < 8) {
            DatabaseSeeder.migrateV8(db);
        }
        if (oldVersion < 9) {
            DatabaseSeeder.migrateV9(db);
        }
        if (oldVersion < 10) {
            DatabaseSeeder.migrateV10(db);
        }
        if (oldVersion < 11) {
            db.execSQL("ALTER TABLE shops ADD COLUMN logo_uri TEXT");
        }
        if (oldVersion < 12) {
            db.execSQL("CREATE TABLE IF NOT EXISTS salary_configs (id INTEGER PRIMARY KEY AUTOINCREMENT, user_id INTEGER UNIQUE NOT NULL, salary_type TEXT NOT NULL DEFAULT 'MONTHLY', base_amount INTEGER NOT NULL DEFAULT 0, commission_percent INTEGER NOT NULL DEFAULT 0, updated_at INTEGER, FOREIGN KEY(user_id) REFERENCES users(id))");
        }
        if (oldVersion < 13) {
            db.execSQL("ALTER TABLE products ADD COLUMN description TEXT");
        }
        if (oldVersion < 14) {
            db.execSQL("ALTER TABLE shipping_orders ADD COLUMN recipient_name TEXT");
            db.execSQL("ALTER TABLE shipping_orders ADD COLUMN recipient_phone TEXT");
            db.execSQL("ALTER TABLE shipping_orders ADD COLUMN recipient_address TEXT");
            db.execSQL("ALTER TABLE shipping_orders ADD COLUMN recipient_district TEXT");
            db.execSQL("ALTER TABLE shipping_orders ADD COLUMN cod_amount INTEGER NOT NULL DEFAULT 0");
        }
    }
}

package com.example.mpos.database;

import android.content.ContentValues;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

import com.example.mpos.auth.PasswordUtils;

public final class DatabaseSeeder {
    private DatabaseSeeder() { }

    private static final String RES = "res://";
    // loremflickr: keyword-based Flickr photos, consistent per lock number, relevant to product
    private static final String LF = "https://loremflickr.com/300/300/";

    public static void seed(SQLiteDatabase db) {
        Cursor existing = db.rawQuery("SELECT COUNT(*) FROM users", null);
        try { if (existing.moveToFirst() && existing.getInt(0) > 0) return; } finally { existing.close(); }
        long now = System.currentTimeMillis();

        long empAdmin   = insertEmployee(db, "EMP-001", "Administrator",   "admin@mpos.com",   "Owner");
        long empManager = insertEmployee(db, "EMP-002", "Nguyen Van Quan", "manager@mpos.com", "Manager");
        long empStaff1  = insertEmployee(db, "EMP-003", "Tran Thi Thu",   "staff@mpos.com",   "Cashier");
        long empStaff2  = insertEmployee(db, "EMP-004", "Le Minh Hung",   "staff2@mpos.com",  "Sales Staff");

        long adminId   = insertUser(db, "admin@mpos.com",   "admin123",   empAdmin,   "ADMIN",   now);
        long managerId = insertUser(db, "manager@mpos.com", "manager123", empManager, "MANAGER", now);
        long staffId1  = insertUser(db, "staff@mpos.com",   "staff123",   empStaff1,  "STAFF",   now);
        long staffId2  = insertUser(db, "staff2@mpos.com",  "staff123",   empStaff2,  "STAFF",   now);

        long shopId = insertShop(db, "Cửa hàng Demo", "123 Nguyễn Huệ, Q.1, TP.HCM", "0900123456", adminId, now);
        insertShopMember(db, shopId, adminId,   "OWNER");
        insertShopMember(db, shopId, managerId, "MANAGER");
        insertShopMember(db, shopId, staffId1,  "STAFF");
        insertShopMember(db, shopId, staffId2,  "STAFF");

        seedProducts(db, now, shopId);
    }

    public static void migrateV4(SQLiteDatabase db) {
        long now = System.currentTimeMillis();
        db.execSQL("UPDATE users SET username='admin@mpos.com' WHERE username='admin'");
        if (!userExists(db, "manager@mpos.com")) {
            long emp = insertEmployee(db, "EMP-002", "Nguyen Van Quan", "manager@mpos.com", "Manager");
            insertUser(db, "manager@mpos.com", "manager123", emp, "MANAGER", now);
        }
        if (!userExists(db, "staff@mpos.com")) {
            long emp = insertEmployee(db, "EMP-003", "Tran Thi Thu", "staff@mpos.com", "Cashier");
            insertUser(db, "staff@mpos.com", "staff123", emp, "STAFF", now);
        }
        if (!userExists(db, "staff2@mpos.com")) {
            long emp = insertEmployee(db, "EMP-004", "Le Minh Hung", "staff2@mpos.com", "Sales Staff");
            insertUser(db, "staff2@mpos.com", "staff123", emp, "STAFF", now);
        }
        // Update existing products with image URIs if they don't have one
        db.execSQL("UPDATE products SET image_uri='" + RES + "img_d01' WHERE sku='SKU-D01' AND (image_uri IS NULL OR image_uri='')");
        db.execSQL("UPDATE products SET image_uri='" + RES + "img_d02' WHERE sku='SKU-D02' AND (image_uri IS NULL OR image_uri='')");
        db.execSQL("UPDATE products SET image_uri='" + RES + "img_d03' WHERE sku='SKU-D03' AND (image_uri IS NULL OR image_uri='')");
        db.execSQL("UPDATE products SET image_uri='" + RES + "img_d04' WHERE sku='SKU-D04' AND (image_uri IS NULL OR image_uri='')");
        // Also seed new products if needed
        Cursor c = db.rawQuery("SELECT COUNT(*) FROM products", null);
        try {
            if (c.moveToFirst() && c.getInt(0) < 8) {
                Cursor cs = db.rawQuery("SELECT id FROM shops LIMIT 1", null);
                long sid = 1;
                try { if (cs.moveToFirst()) sid = cs.getLong(0); } finally { cs.close(); }
                seedProducts(db, now, sid);
            }
        } finally { c.close(); }
    }

    private static void seedProducts(SQLiteDatabase db, long now, long shopId) {
        long catDrink = insertCategory(db, "Do uong", shopId);
        long catFood  = insertCategory(db, "Do an", shopId);
        long catSnack = insertCategory(db, "Banh & Snack", shopId);
        long catFresh = insertCategory(db, "Thuc pham tuoi", shopId);

        // Do uong — loremflickr keyword ảnh khớp với tên sản phẩm
        insertProduct(db, "SKU-D01", "8930001001", "Cà phê sữa đá",     catDrink, 12000, 29000, 80, 5, LF+"coffee,milk,iced?lock=101", now, shopId);
        insertProduct(db, "SKU-D02", "8930001002", "Bạc xỉu",           catDrink, 10000, 25000, 60, 5, LF+"coffee,latte,white?lock=102", now, shopId);
        insertProduct(db, "SKU-D03", "8930001003", "Cà phê đen đá",     catDrink,  8000, 20000, 70, 5, LF+"espresso,black,coffee?lock=103", now, shopId);
        insertProduct(db, "SKU-D04", "8930001004", "Trà sữa trân châu", catDrink, 18000, 39000, 40, 5, LF+"bubble,tea,boba?lock=104", now, shopId);
        insertProduct(db, "SKU-D05", "8930001005", "Sinh tố bơ",        catDrink, 20000, 45000, 25, 3, LF+"avocado,smoothie,green?lock=105", now, shopId);
        insertProduct(db, "SKU-D06", "8930001006", "Nước cam ép",       catDrink, 15000, 35000, 30, 3, LF+"orange,juice,fresh?lock=106", now, shopId);
        insertProduct(db, "SKU-D07", "8930001007", "Trà đào cam sả",    catDrink, 14000, 32000, 35, 3, LF+"peach,tea,iced?lock=107", now, shopId);
        insertProduct(db, "SKU-D08", "8930001008", "Matcha latte",      catDrink, 22000, 49000, 20, 3, LF+"matcha,green,latte?lock=108", now, shopId);

        // Do an
        insertProduct(db, "SKU-F01", "8930002001", "Bánh mì thịt",      catFood, 18000, 35000, 30, 5, LF+"banh,mi,sandwich?lock=201", now, shopId);
        insertProduct(db, "SKU-F02", "8930002002", "Bánh mì pate",      catFood, 12000, 25000, 25, 5, LF+"baguette,pate,bread?lock=202", now, shopId);
        insertProduct(db, "SKU-F03", "8930002003", "Gỏi cuốn tôm",      catFood,  8000, 15000, 20, 3, LF+"spring,rolls,shrimp?lock=203", now, shopId);
        insertProduct(db, "SKU-F04", "8930002004", "Hủ tiếu Nam Vang",  catFood, 25000, 55000, 15, 3, LF+"noodle,soup,asian?lock=204", now, shopId);
        insertProduct(db, "SKU-F05", "8930002005", "Phở bò tái",        catFood, 30000, 65000, 10, 2, LF+"pho,vietnamese,noodle?lock=205", now, shopId);
        insertProduct(db, "SKU-F06", "8930002006", "Cơm sườn bì chả",   catFood, 28000, 60000, 12, 2, LF+"rice,pork,vietnamese?lock=206", now, shopId);

        // Banh & Snack
        insertProduct(db, "SKU-S01", "8930003001", "Bánh tiramisu",     catSnack, 20000, 45000, 15, 3, LF+"tiramisu,dessert,cake?lock=301", now, shopId);
        insertProduct(db, "SKU-S02", "8930003002", "Croissant bơ",      catSnack, 15000, 32000, 20, 3, LF+"croissant,pastry,butter?lock=302", now, shopId);
        insertProduct(db, "SKU-S03", "8930003003", "Mochi đậu đỏ",      catSnack, 10000, 22000, 30, 5, LF+"mochi,japanese,dessert?lock=303", now, shopId);
        insertProduct(db, "SKU-S04", "8930003004", "Bánh flan caramel", catSnack,  8000, 18000, 25, 5, LF+"flan,caramel,pudding?lock=304", now, shopId);
        insertProduct(db, "SKU-S05", "8930003005", "Khoai tây chiên",   catSnack, 10000, 25000,  0, 3, LF+"french,fries,potato?lock=305", now, shopId);

        // Thuc pham tuoi
        insertProduct(db, "SKU-P01", "8930004001", "Sữa tươi Vinamilk 1L",    catFresh, 22000, 32000, 50, 10, LF+"milk,bottle,dairy?lock=401", now, shopId);
        insertProduct(db, "SKU-P02", "8930004002", "Trứng gà vỉ 10",           catFresh, 28000, 38000, 30,  5, LF+"eggs,chicken,fresh?lock=402", now, shopId);
        insertProduct(db, "SKU-P03", "8930004003", "Bánh mì sandwich Kinh Đô", catFresh, 12000, 18000, 40,  8, LF+"bread,loaf,bakery?lock=403", now, shopId);
    }

    // ── helpers ──────────────────────────────────────────────────────────────

    private static long insertEmployee(SQLiteDatabase db, String code, String name, String email, String position) {
        Cursor c = db.rawQuery("SELECT id FROM employees WHERE employee_code=?", new String[]{code});
        try { if (c.moveToFirst()) return c.getLong(0); } finally { c.close(); }
        ContentValues v = new ContentValues();
        v.put("employee_code", code); v.put("full_name", name);
        v.put("email", email); v.put("position", position);
        return db.insert(DatabaseContract.EMPLOYEES, null, v);
    }

    private static long insertUser(SQLiteDatabase db, String email, String password,
                                   long employeeId, String role, long now) {
        ContentValues v = new ContentValues();
        v.put("username", email);
        v.put("password_hash", PasswordUtils.hash(password));
        v.put("employee_id", employeeId);
        v.put("role", role);
        v.put("created_at", now);
        try { return db.insertOrThrow(DatabaseContract.USERS, null, v); } catch (Exception ignored) { return -1; }
    }

    private static long insertShop(SQLiteDatabase db, String name, String address, String phone,
                                   long ownerUserId, long now) {
        Cursor c = db.rawQuery("SELECT id FROM shops WHERE name=? AND owner_user_id=?",
            new String[]{name, String.valueOf(ownerUserId)});
        try { if (c.moveToFirst()) return c.getLong(0); } finally { c.close(); }
        ContentValues v = new ContentValues();
        v.put("name", name); v.put("address", address); v.put("phone", phone);
        v.put("owner_user_id", ownerUserId); v.put("created_at", now);
        try { return db.insertOrThrow(DatabaseContract.SHOPS, null, v); } catch (Exception e) { return 1; }
    }

    private static void insertShopMember(SQLiteDatabase db, long shopId, long userId, String role) {
        ContentValues v = new ContentValues();
        v.put("shop_id", shopId); v.put("user_id", userId);
        v.put("role", role); v.put("created_at", System.currentTimeMillis());
        try { db.insertOrThrow(DatabaseContract.SHOP_MEMBERS, null, v); } catch (Exception ignored) { }
    }

    private static boolean userExists(SQLiteDatabase db, String email) {
        Cursor c = db.rawQuery("SELECT COUNT(*) FROM users WHERE username=?", new String[]{email});
        try { return c.moveToFirst() && c.getInt(0) > 0; } finally { c.close(); }
    }

    private static long insertCategory(SQLiteDatabase db, String name, long shopId) {
        Cursor c = db.rawQuery("SELECT id FROM categories WHERE name=? AND shop_id=?",
            new String[]{name, String.valueOf(shopId)});
        try { if (c.moveToFirst()) return c.getLong(0); } finally { c.close(); }
        ContentValues cat = new ContentValues();
        cat.put("name", name);
        cat.put("shop_id", shopId);
        return db.insert(DatabaseContract.CATEGORIES, null, cat);
    }

    private static void insertProduct(SQLiteDatabase db, String sku, String barcode, String name,
                                      long categoryId, int cost, int price, int stock, int minStock,
                                      String imageUri, long now, long shopId) {
        ContentValues v = new ContentValues();
        v.put("sku", sku); v.put("barcode", barcode); v.put("name", name);
        v.put("category_id", categoryId); v.put("cost_price", cost); v.put("sale_price", price);
        v.put("stock_quantity", stock); v.put("min_stock_quantity", minStock);
        v.put("image_uri", imageUri);
        v.put("shop_id", shopId);
        v.put("created_at", now);
        try { db.insertOrThrow(DatabaseContract.PRODUCTS, null, v); } catch (Exception ignored) { }
    }

    /** V6: cập nhật ảnh sản phẩm từ vector cục bộ sang URL ảnh thật. */
    public static void migrateV6(SQLiteDatabase db) {
        // V7 will overwrite these with picsum.photos URLs, so V6 just needs to run
        String[][] updates = {
            {"SKU-D01", "https://loremflickr.com/300/300/vietnamese,coffee?lock=11"},
            {"SKU-D02", "https://loremflickr.com/300/300/latte,coffee?lock=22"},
            {"SKU-D03", "https://loremflickr.com/300/300/espresso,coffee?lock=33"},
            {"SKU-D04", "https://loremflickr.com/300/300/bubble,tea?lock=44"},
            {"SKU-D05", "https://loremflickr.com/300/300/avocado,smoothie?lock=55"},
            {"SKU-D06", "https://loremflickr.com/300/300/orange,juice?lock=66"},
            {"SKU-D07", "https://loremflickr.com/300/300/iced,tea?lock=77"},
            {"SKU-D08", "https://loremflickr.com/300/300/matcha,latte?lock=88"},
            {"SKU-F01", "https://loremflickr.com/300/300/banh,mi?lock=11"},
            {"SKU-F02", "https://loremflickr.com/300/300/baguette,sandwich?lock=22"},
            {"SKU-F03", "https://loremflickr.com/300/300/spring,rolls?lock=33"},
            {"SKU-F04", "https://loremflickr.com/300/300/noodle,soup?lock=44"},
            {"SKU-F05", "https://loremflickr.com/300/300/pho,soup?lock=55"},
            {"SKU-F06", "https://loremflickr.com/300/300/rice,pork?lock=66"},
            {"SKU-S01", "https://loremflickr.com/300/300/tiramisu,dessert?lock=11"},
            {"SKU-S02", "https://loremflickr.com/300/300/croissant,bread?lock=22"},
            {"SKU-S03", "https://loremflickr.com/300/300/mochi,japanese?lock=33"},
            {"SKU-S04", "https://loremflickr.com/300/300/flan,pudding?lock=44"},
            {"SKU-S05", "https://loremflickr.com/300/300/french,fries?lock=55"},
            {"SKU-P01", "https://loremflickr.com/300/300/milk,bottle?lock=11"},
            {"SKU-P02", "https://loremflickr.com/300/300/eggs,fresh?lock=22"},
            {"SKU-P03", "https://loremflickr.com/300/300/sandwich,bread?lock=33"},
        };
        for (String[] row : updates) {
            db.execSQL("UPDATE products SET image_uri=? WHERE sku=?", new String[]{row[1], row[0]});
        }
    }

    /** V7: picsum.photos — superseded by V8 which uses loremflickr with proper keywords. */
    public static void migrateV7(SQLiteDatabase db) {
        String base = "https://picsum.photos/seed/";
        String[][] updates = {
            {"SKU-D01", base + "d01/300/300"}, {"SKU-D02", base + "d02/300/300"},
            {"SKU-D03", base + "d03/300/300"}, {"SKU-D04", base + "d04/300/300"},
            {"SKU-D05", base + "d05/300/300"}, {"SKU-D06", base + "d06/300/300"},
            {"SKU-D07", base + "d07/300/300"}, {"SKU-D08", base + "d08/300/300"},
            {"SKU-F01", base + "f01/300/300"}, {"SKU-F02", base + "f02/300/300"},
            {"SKU-F03", base + "f03/300/300"}, {"SKU-F04", base + "f04/300/300"},
            {"SKU-F05", base + "f05/300/300"}, {"SKU-F06", base + "f06/300/300"},
            {"SKU-S01", base + "s01/300/300"}, {"SKU-S02", base + "s02/300/300"},
            {"SKU-S03", base + "s03/300/300"}, {"SKU-S04", base + "s04/300/300"},
            {"SKU-S05", base + "s05/300/300"},
            {"SKU-P01", base + "p01/300/300"}, {"SKU-P02", base + "p02/300/300"},
            {"SKU-P03", base + "p03/300/300"},
        };
        for (String[] row : updates) {
            db.execSQL("UPDATE products SET image_uri=? WHERE sku=?", new String[]{row[1], row[0]});
        }
    }

    /** V8: loremflickr keyword URLs — ảnh đúng theo tên sản phẩm. */
    public static void migrateV8(SQLiteDatabase db) {
        String[][] updates = {
            {"SKU-D01", LF + "coffee,milk,iced?lock=101"},
            {"SKU-D02", LF + "coffee,latte,white?lock=102"},
            {"SKU-D03", LF + "espresso,black,coffee?lock=103"},
            {"SKU-D04", LF + "bubble,tea,boba?lock=104"},
            {"SKU-D05", LF + "avocado,smoothie,green?lock=105"},
            {"SKU-D06", LF + "orange,juice,fresh?lock=106"},
            {"SKU-D07", LF + "peach,tea,iced?lock=107"},
            {"SKU-D08", LF + "matcha,green,latte?lock=108"},
            {"SKU-F01", LF + "banh,mi,sandwich?lock=201"},
            {"SKU-F02", LF + "baguette,pate,bread?lock=202"},
            {"SKU-F03", LF + "spring,rolls,shrimp?lock=203"},
            {"SKU-F04", LF + "noodle,soup,asian?lock=204"},
            {"SKU-F05", LF + "pho,vietnamese,noodle?lock=205"},
            {"SKU-F06", LF + "rice,pork,vietnamese?lock=206"},
            {"SKU-S01", LF + "tiramisu,dessert,cake?lock=301"},
            {"SKU-S02", LF + "croissant,pastry,butter?lock=302"},
            {"SKU-S03", LF + "mochi,japanese,dessert?lock=303"},
            {"SKU-S04", LF + "flan,caramel,pudding?lock=304"},
            {"SKU-S05", LF + "french,fries,potato?lock=305"},
            {"SKU-P01", LF + "milk,bottle,dairy?lock=401"},
            {"SKU-P02", LF + "eggs,chicken,fresh?lock=402"},
            {"SKU-P03", LF + "bread,loaf,bakery?lock=403"},
        };
        for (String[] row : updates) {
            db.execSQL("UPDATE products SET image_uri=? WHERE sku=?", new String[]{row[1], row[0]});
        }
    }

    /** V10: thêm bảng shops, shop_members; thêm cột shop_id vào các bảng dữ liệu. */
    public static void migrateV10(SQLiteDatabase db) {
        db.execSQL("CREATE TABLE IF NOT EXISTS shops " +
            "(id INTEGER PRIMARY KEY AUTOINCREMENT, name TEXT NOT NULL, address TEXT, phone TEXT, " +
            "owner_user_id INTEGER NOT NULL, created_at INTEGER)");
        db.execSQL("CREATE TABLE IF NOT EXISTS shop_members " +
            "(id INTEGER PRIMARY KEY AUTOINCREMENT, shop_id INTEGER NOT NULL, user_id INTEGER NOT NULL, " +
            "role TEXT NOT NULL DEFAULT 'STAFF', created_at INTEGER, UNIQUE(shop_id, user_id), " +
            "FOREIGN KEY(shop_id) REFERENCES shops(id), FOREIGN KEY(user_id) REFERENCES users(id))");

        // Add shop_id column to existing tables (DEFAULT 1 = demo shop)
        try { db.execSQL("ALTER TABLE products ADD COLUMN shop_id INTEGER NOT NULL DEFAULT 1"); } catch (Exception ignored) {}
        try { db.execSQL("ALTER TABLE categories ADD COLUMN shop_id INTEGER NOT NULL DEFAULT 1"); } catch (Exception ignored) {}
        try { db.execSQL("ALTER TABLE customers ADD COLUMN shop_id INTEGER NOT NULL DEFAULT 1"); } catch (Exception ignored) {}
        try { db.execSQL("ALTER TABLE orders ADD COLUMN shop_id INTEGER NOT NULL DEFAULT 1"); } catch (Exception ignored) {}
        try { db.execSQL("ALTER TABLE shifts ADD COLUMN shop_id INTEGER NOT NULL DEFAULT 1"); } catch (Exception ignored) {}

        // Seed the demo shop (id=1) if it doesn't exist
        long now = System.currentTimeMillis();
        Cursor cu = db.rawQuery("SELECT id FROM users WHERE username='admin@mpos.com' LIMIT 1", null);
        long adminId = 1;
        try { if (cu.moveToFirst()) adminId = cu.getLong(0); } finally { cu.close(); }

        Cursor cs = db.rawQuery("SELECT id FROM shops LIMIT 1", null);
        boolean shopExists;
        try { shopExists = cs.moveToFirst(); } finally { cs.close(); }

        if (!shopExists) {
            long shopId = insertShop(db, "Cửa hàng Demo", "123 Nguyễn Huệ, Q.1, TP.HCM", "0900123456", adminId, now);

            // Add all existing users as members of the demo shop
            Cursor usersCur = db.rawQuery("SELECT id, role FROM users WHERE is_active=1", null);
            try {
                while (usersCur.moveToNext()) {
                    long uid = usersCur.getLong(0);
                    String role = usersCur.getString(1);
                    String memberRole = "ADMIN".equals(role) ? "OWNER" : role;
                    insertShopMember(db, shopId, uid, memberRole);
                }
            } finally { usersCur.close(); }
        }
    }

    /** V9: thêm bảng shipping_orders đ�� theo dõi vận đơn. */
    public static void migrateV9(SQLiteDatabase db) {
        db.execSQL("CREATE TABLE IF NOT EXISTS shipping_orders " +
            "(id INTEGER PRIMARY KEY AUTOINCREMENT, " +
            "order_id INTEGER, carrier TEXT NOT NULL, tracking_code TEXT, " +
            "shipping_fee INTEGER NOT NULL DEFAULT 0, from_province TEXT, to_province TEXT, " +
            "weight_gram INTEGER NOT NULL DEFAULT 500, status TEXT NOT NULL DEFAULT 'PENDING', " +
            "created_at INTEGER, FOREIGN KEY(order_id) REFERENCES orders(id))");
    }

    public static void seedProductsIfNeeded(SQLiteDatabase db) {
        Cursor c = db.rawQuery("SELECT COUNT(*) FROM products", null);
        try { if (c.moveToFirst() && c.getInt(0) >= 4) return; } finally { c.close(); }
        Cursor cs = db.rawQuery("SELECT id FROM shops LIMIT 1", null);
        long shopId = 1;
        try { if (cs.moveToFirst()) shopId = cs.getLong(0); } finally { cs.close(); }
        seedProducts(db, System.currentTimeMillis(), shopId);
    }

    public static void migrateV5(SQLiteDatabase db) {
        Cursor c = db.rawQuery("SELECT COUNT(*) FROM customers", null);
        boolean hasCustomers;
        try { hasCustomers = c.moveToFirst() && c.getInt(0) > 0; } finally { c.close(); }
        if (!hasCustomers) {
            seedCustomers(db);
            seedOrders(db);
        }
    }

    private static void seedCustomers(SQLiteDatabase db) {
        long now = System.currentTimeMillis();
        insertCustomer(db, "0901234567", "Nguyễn Thị Lan",   "lan.nguyen@gmail.com",  "123 Nguyễn Huệ, Q.1, TP.HCM",          150, now - 30L*86400000);
        insertCustomer(db, "0912345678", "Trần Văn Minh",    "minh.tran@gmail.com",   "45 Lê Lợi, Q.3, TP.HCM",               85,  now - 20L*86400000);
        insertCustomer(db, "0923456789", "Lê Thị Hoa",       "hoa.le@yahoo.com",      "78 Nguyễn Trãi, Q.5, TP.HCM",          200, now - 45L*86400000);
        insertCustomer(db, "0934567890", "Phạm Quốc Hùng",   null,                    "22 Trần Hưng Đạo, Q.1, TP.HCM",        30,  now - 5L*86400000);
        insertCustomer(db, "0945678901", "Hoàng Thị Mai",    "mai.hoang@gmail.com",   "156 CMT8, Q.10, TP.HCM",               320, now - 60L*86400000);
        insertCustomer(db, "0956789012", "Đinh Văn Tùng",    null,                    "99 Đinh Tiên Hoàng, Q.Bình Thạnh",     0,   now - 2L*86400000);
        insertCustomer(db, "0967890123", "Nguyễn Văn Bình",  "binh.nv@gmail.com",     "33 Hoàng Diệu, Q.4, TP.HCM",          500, now - 90L*86400000);
    }

    private static void insertCustomer(SQLiteDatabase db, String phone, String name, String email, String address, int points, long createdAt) {
        ContentValues v = new ContentValues();
        v.put("phone", phone); v.put("full_name", name);
        if (email != null) v.put("email", email);
        v.put("address", address); v.put("loyalty_points", points);
        v.put("created_at", createdAt);
        try { db.insertOrThrow(DatabaseContract.CUSTOMERS, null, v); } catch (Exception ignored) { }
    }

    private static void seedOrders(SQLiteDatabase db) {
        long now = System.currentTimeMillis();
        // Get admin user id
        Cursor cu = db.rawQuery("SELECT id FROM users WHERE username='admin@mpos.com' LIMIT 1", null);
        long adminId = 1;
        try { if (cu.moveToFirst()) adminId = cu.getLong(0); } finally { cu.close(); }

        // Get customer ids
        long cust1 = getCustomerId(db, "0901234567");
        long cust2 = getCustomerId(db, "0912345678");
        long cust3 = getCustomerId(db, "0923456789");
        long cust5 = getCustomerId(db, "0945678901");

        // Get product ids by sku
        long pdCaPhe    = getProductId(db, "SKU-D01"); // 29000
        long pdBacXiu   = getProductId(db, "SKU-D02"); // 25000
        long pdTraSua   = getProductId(db, "SKU-D04"); // 39000
        long pdNuocCam  = getProductId(db, "SKU-D06"); // 35000
        long pdMatcha   = getProductId(db, "SKU-D08"); // 49000
        long pdBanhMi   = getProductId(db, "SKU-F01"); // 35000
        long pdPho      = getProductId(db, "SKU-F05"); // 65000
        long pdCom      = getProductId(db, "SKU-F06"); // 60000
        long pdCroissant= getProductId(db, "SKU-S02"); // 32000
        long pdMochi    = getProductId(db, "SKU-S03"); // 22000

        // Order 1: Cà phê sữa đá x2 + Bánh mì thịt x1 = 93000
        long o1 = insertOrder(db, "ORD-001", cust1, adminId, "WALK_IN", "COMPLETED", 93000, 0, 0, 0, 93000, now - 5L*86400000);
        insertOrderItem(db, o1, pdCaPhe,  "Cà phê sữa đá",    29000, 2, 58000);
        insertOrderItem(db, o1, pdBanhMi, "Bánh mì thịt",     35000, 1, 35000);
        insertPayment(db, o1, "CASH", 93000, now - 5L*86400000);

        // Order 2: Trà sữa trân châu x1 + Mochi đậu đỏ x2 = 83000
        long o2 = insertOrder(db, "ORD-002", cust2, adminId, "ONLINE", "COMPLETED", 83000, 0, 0, 0, 83000, now - 4L*86400000);
        insertOrderItem(db, o2, pdTraSua, "Trà sữa trân châu", 39000, 1, 39000);
        insertOrderItem(db, o2, pdMochi,  "Mochi đậu đỏ",      22000, 2, 44000);
        insertPayment(db, o2, "TRANSFER", 83000, now - 4L*86400000);

        // Order 3: Bạc xỉu x3 = 75000
        long o3 = insertOrder(db, "ORD-003", 0, adminId, "WALK_IN", "COMPLETED", 75000, 5000, 0, 0, 70000, now - 3L*86400000);
        insertOrderItem(db, o3, pdBacXiu, "Bạc xỉu", 25000, 3, 75000);
        insertPayment(db, o3, "CASH", 70000, now - 3L*86400000);

        // Order 4: Phở bò tái x1 + Nước cam ép x2 = 135000
        long o4 = insertOrder(db, "ORD-004", cust3, adminId, "WALK_IN", "COMPLETED", 135000, 0, 8, 10800, 145800, now - 2L*86400000);
        insertOrderItem(db, o4, pdPho,    "Phở bò tái",   65000, 1, 65000);
        insertOrderItem(db, o4, pdNuocCam,"Nước cam ép",  35000, 2, 70000);
        insertPayment(db, o4, "CARD", 145800, now - 2L*86400000);

        // Order 5: Cơm sườn bì chả x2 = 120000 (pending)
        long o5 = insertOrder(db, "ORD-005", cust5, adminId, "ONLINE", "PENDING", 120000, 0, 0, 0, 120000, now - 86400000);
        insertOrderItem(db, o5, pdCom, "Cơm sườn bì chả", 60000, 2, 120000);

        // Order 6: Matcha latte x2 + Croissant bơ x1 = 130000 (today)
        long o6 = insertOrder(db, "ORD-006", cust1, adminId, "WALK_IN", "COMPLETED", 130000, 0, 0, 0, 130000, now - 3600000);
        insertOrderItem(db, o6, pdMatcha,    "Matcha latte",  49000, 2, 98000);
        insertOrderItem(db, o6, pdCroissant, "Croissant bơ",  32000, 1, 32000);
        insertPayment(db, o6, "CASH", 130000, now - 3600000);
    }

    private static long getCustomerId(SQLiteDatabase db, String phone) {
        Cursor c = db.rawQuery("SELECT id FROM customers WHERE phone=?", new String[]{phone});
        try { return c.moveToFirst() ? c.getLong(0) : 0; } finally { c.close(); }
    }

    private static long getProductId(SQLiteDatabase db, String sku) {
        Cursor c = db.rawQuery("SELECT id FROM products WHERE sku=?", new String[]{sku});
        try { return c.moveToFirst() ? c.getLong(0) : 0; } finally { c.close(); }
    }

    private static long insertOrder(SQLiteDatabase db, String code, long customerId, long userId,
                                    String channel, String status, int subtotal, int discount,
                                    int vatPct, int vatAmt, int total, long createdAt) {
        ContentValues v = new ContentValues();
        v.put("order_code", code); v.put("user_id", userId);
        if (customerId > 0) v.put("customer_id", customerId);
        v.put("channel", channel); v.put("status", status);
        v.put("subtotal", subtotal); v.put("discount_amount", discount);
        v.put("vat_percent", vatPct); v.put("vat_amount", vatAmt);
        v.put("total_amount", total); v.put("created_at", createdAt);
        try { return db.insertOrThrow(DatabaseContract.ORDERS, null, v); } catch (Exception e) { return -1; }
    }

    private static void insertOrderItem(SQLiteDatabase db, long orderId, long productId,
                                        String productName, int unitPrice, int qty, int lineTotal) {
        if (orderId <= 0 || productId <= 0) return;
        ContentValues v = new ContentValues();
        v.put("order_id", orderId); v.put("product_id", productId);
        v.put("product_name", productName); v.put("unit_price", unitPrice);
        v.put("quantity", qty); v.put("discount_amount", 0); v.put("line_total", lineTotal);
        try { db.insertOrThrow(DatabaseContract.ORDER_ITEMS, null, v); } catch (Exception ignored) { }
    }

    private static void insertPayment(SQLiteDatabase db, long orderId, String method, int amount, long paidAt) {
        if (orderId <= 0) return;
        ContentValues v = new ContentValues();
        v.put("order_id", orderId); v.put("method", method);
        v.put("amount", amount); v.put("status", "PAID"); v.put("paid_at", paidAt);
        try { db.insertOrThrow(DatabaseContract.PAYMENTS, null, v); } catch (Exception ignored) { }
    }

    /** Tạo shop "Chuyen Lang Nghe" + 60 sản phẩm thủ công mỹ nghệ. Idempotent. */
    public static void seedShopLangNghe(SQLiteDatabase db) {
        Cursor cs = db.rawQuery("SELECT COUNT(*) FROM shops WHERE name='Chuyen Lang Nghe'", null);
        try { if (cs.moveToFirst() && cs.getInt(0) > 0) return; } finally { cs.close(); }

        long now = System.currentTimeMillis();

        // Tạo user chủ shop nếu chưa có
        long ownerId = -1;
        Cursor cu = db.rawQuery("SELECT id FROM users WHERE username='duongtanminh2306@gmail.com'", null);
        try { if (cu.moveToFirst()) ownerId = cu.getLong(0); } finally { cu.close(); }
        if (ownerId < 0) {
            long emp = insertEmployee(db, "EMP-LN01", "Duong Tan Minh", "duongtanminh2306@gmail.com", "Owner");
            ContentValues uv = new ContentValues();
            uv.put("username", "duongtanminh2306@gmail.com");
            uv.put("password_hash", "7d2bb7202f7c343024416b95a98ebe9050562b82974132c66109c61a09a2e063");
            uv.put("employee_id", emp);
            uv.put("role", "ADMIN");
            uv.put("created_at", now);
            try { ownerId = db.insertOrThrow(DatabaseContract.USERS, null, uv); } catch (Exception ignored) { }
            cu = db.rawQuery("SELECT id FROM users WHERE username='duongtanminh2306@gmail.com'", null);
            try { if (cu.moveToFirst()) ownerId = cu.getLong(0); } finally { cu.close(); }
        }
        if (ownerId < 0) return;

        long shopId = insertShop(db, "Chuyen Lang Nghe", "", "", ownerId, now);
        insertShopMember(db, shopId, ownerId, "OWNER");

        long catGom   = insertCategory(db, "Gom su",        shopId);
        long catMay   = insertCategory(db, "May tre dan",   shopId);
        long catLich  = insertCategory(db, "Lich go",       shopId);
        long catTranh = insertCategory(db, "Tranh & Tuong", shopId);

        insertProduct(db, "4312153686462", null, "(Bán Lẻ) Ấm trà gốm men hỏa biến – Thanh Hà", catGom, 90000, 150000, 34, 3, "https://cdn.hstatic.net/products/200001185819/amtrathanhha_b740fca9d17945209769834d36e7b8b5.png", now, shopId);
        insertProduct(db, "2720802123461", null, "(Bán Lẻ) Bộ Trà Gốm \"Hải Vân Chân Phương\" – Men Hỏa Biến Thanh Hà", catGom, 675000, 1125000, 79, 6, "https://cdn.hstatic.net/products/200001185819/haivanchanphuong_4ef1e42661894f70aa2efad8701dbfdd.png", now, shopId);
        insertProduct(db, "4360840033724", null, "(CAO) Bình gốm trang trí dáng trụ \"Vân Sóng\" – Bàu Trúc", catGom, 468000, 780000, 67, 5, "https://cdn.hstatic.net/products/200001185819/binhgomtrangtri_e377271385ea4c86b0430fb501f9843c.png", now, shopId);
        insertProduct(db, "9380428449410", null, "(MEN HỔ PHÁCH) Bình Gốm Decor \"Quả Bí\" – Gốm Biên Hòa", catGom, 210000, 350000, 36, 4, "https://cdn.hstatic.net/products/200001185819/menhophch_feab873ea93d4ce89adcf51675a622f3.png", now, shopId);
        insertProduct(db, "3362113505531", null, "(MEN NGỌC LỤC BẢO) Bình Gốm Decor \"Quả Bí\" – Gốm Biên Hòa", catGom, 210000, 350000, 118, 3, "https://cdn.hstatic.net/products/200001185819/1_369cf4577f8f4fc6b9b0a95edd574a6e.png", now, shopId);
        insertProduct(db, "2574211521799", null, "(MEN NGỌC LỤC BẢO) Bình Gốm Quả Bí Họa Tiết Mây Cổ - Làng Gốm Biên Hòa", catGom, 299400, 499000, 17, 4, "https://cdn.hstatic.net/products/200001185819/binhgomquabi_83c47961d34e4766a047c49f068c17f1.png", now, shopId);
        insertProduct(db, "283802884775", null, "(MEN THANH LƯU) Bình Gốm Quả Bí Họa Tiết Mây Cổ - Làng Gốm Biên Hòa", catGom, 299400, 499000, 65, 6, "https://cdn.hstatic.net/products/200001185819/mayco_b167f23017ca4ddaa7143706559991cf.png", now, shopId);
        insertProduct(db, "1074235442294", null, "(MEN XANH TRÀM) Bình Gốm Decor \"Quả Bí\" – Gốm Biên Hòa", catGom, 210000, 350000, 139, 3, "https://cdn.hstatic.net/products/200001185819/decorquabi_971dae949c9f44bba2383f80087cff25.png", now, shopId);
        insertProduct(db, "3000575365780", null, "(TRUNG) Bình gốm trang trí dáng trụ \"Vân Sóng\" – Bàu Trúc", catGom, 330000, 550000, 60, 9, "https://cdn.hstatic.net/products/200001185819/trung_7d603df6cf9f4a148f11423253acb05b.png", now, shopId);
        insertProduct(db, "8358526471417", null, "Bàn Chải Đánh Răng Tre", catMay, 27000, 45000, 66, 10, "https://cdn.hstatic.net/products/200001185819/_nh_ch_p_m_n_h_nh_2026-06-16_161849_b6dee74aa1fd468ca9c8f1dc3044839c.png", now, shopId);
        insertProduct(db, "2054541520299", null, "Bát Cơm Gốm Biên Hòa Men Màu Độc Đáo", catGom, 108000, 180000, 81, 3, "https://cdn.hstatic.net/products/200001185819/_nh_ch_p_m_n_h_nh_2026-06-16_164354_e5039ffe21554ef1b99a528c30388e1f.png", now, shopId);
        insertProduct(db, "1130382079633", null, "Bình gốm trang trí \"Mộc Lan Chi\" – Lái Thiêu", catGom, 600000, 1000000, 50, 9, "https://cdn.hstatic.net/products/200001185819/laithieu_9460302c2af84eee8a75d32bcceec8d0.png", now, shopId);
        insertProduct(db, "9194032116241", null, "Bình gốm trang trí họa tiết Thiên Dương – Phù Lãng", catGom, 675000, 1125000, 97, 7, "https://cdn.hstatic.net/products/200001185819/thienduong1_8e907734524d4396aaf7c3a1b3e40021.png", now, shopId);
        insertProduct(db, "4380420390126", null, "Bình gốm tứ cảnh \"Điểu Hoa Đoàn Viên\" – Gốm Chu Đậu", catGom, 1110000, 1850000, 49, 6, "https://cdn.hstatic.net/products/200001185819/doanvien1_eaf2896414624437b0bfe377944ba3cb.png", now, shopId);
        insertProduct(db, "5341271244023", null, "Bình gốm tỳ bà \"Mã Đáo Thành Công\" – Chu Đậu Kim Lan", catGom, 1590000, 2650000, 96, 4, "https://cdn.hstatic.net/products/200001185819/madaothanhcong1_481847339d414a42871cd0beb999e73f.png", now, shopId);
        insertProduct(db, "5432144343684", null, "Bình Rượu Gốm Sứ Cổ Điển", catGom, 312000, 520000, 33, 9, "https://cdn.hstatic.net/products/200001185819/_nh_ch_p_m_n_h_nh_2026-06-16_164549_89f43b8019d049a7a4bc12c0a55cae42.png", now, shopId);
        insertProduct(db, "5289204045554", null, "Bộ ấm trà gốm men hỏa biến – Thanh Hà", catGom, 534000, 890000, 34, 8, "https://cdn.hstatic.net/products/200001185819/menhoa1_b5fe14decbfe48eeb1baf94e4cc6b71b.png", now, shopId);
        insertProduct(db, "5405155345110", null, "Bộ Ấm Trà Gốm Thanh Hà 5 Món", catGom, 1008000, 1680000, 98, 7, "https://cdn.hstatic.net/products/200001185819/_nh_ch_p_m_n_h_nh_2026-06-16_164718_7d97131615f84e17bd0d6f517adf9534.png", now, shopId);
        insertProduct(db, "0300303272470", null, "Bộ Đồ Trang Trí Gốm Sứ Buncheong", catGom, 907200, 1512000, 21, 10, "https://cdn.hstatic.net/products/200001185819/_nh_ch_p_m_n_h_nh_2026-06-16_165359_674cbca3a88b432e92e583478e0df6ac.png", now, shopId);
        insertProduct(db, "3583003352830", null, "Bộ Đũa Gỗ Tay Trong Giỏ Tre", catMay, 252000, 420000, 147, 4, "https://cdn.hstatic.net/products/200001185819/_nh_ch_p_m_n_h_nh_2026-06-16_165148_2db6e2dc58594cd691c5a812b88db514.png", now, shopId);
        insertProduct(db, "5448243291184", null, "Bộ Trà Gốm \"Hải Vân Chân Phương\" – Men Hỏa Biến Thanh Hà", catGom, 675000, 1125000, 106, 4, "https://cdn.hstatic.net/products/200001185819/menhoatra_4767243e1d2b42c7ba64ba066d3c21d5.png", now, shopId);
        insertProduct(db, "602141711249", null, "BST Bình Gốm Decor \"Quả Bí\" – Gốm Biên Hòa", catGom, 570000, 950000, 85, 8, "https://cdn.hstatic.net/products/200001185819/quabibienhoa_a601f4fd4bf0457ca07bc7f21786d255.png", now, shopId);
        insertProduct(db, "5789302533106", null, "BST Bình Và Dĩa Gốm Trang Trí \"Điệp Vũ Hoa Đình\"", catGom, 1335000, 2225000, 59, 4, "https://cdn.hstatic.net/products/200001185819/diepvuhoadinh_0bbba79f6c7244faa7d0ec281250dedd.png", now, shopId);
        insertProduct(db, "4176053653246", null, "BST Chén Dĩa \"Huyền Liên\" – Phù Lãng", catGom, 690000, 1150000, 21, 6, "https://cdn.hstatic.net/products/200001185819/huyenlien_3642216f3d0a4853a301abb1d2184acd.png", now, shopId);
        insertProduct(db, "847505703533", null, "BST Ngọc Lam Phú Quý – Bình gốm men ngọc họa tiết chim xuân", catGom, 894000, 1490000, 84, 4, "https://cdn.hstatic.net/products/200001185819/ngoclamphuquy_78c0a093e11244cd8759553d695cab8c.png", now, shopId);
        insertProduct(db, "181537988102", null, "Chén Ăn Gốm Men Xanh Bát Tràng", catGom, 72000, 120000, 69, 4, "https://cdn.hstatic.net/products/200001185819/_nh_ch_p_m_n_h_nh_2026-06-16_165008_8a6dd17b630444f1aab4f2e572e82d74.png", now, shopId);
        insertProduct(db, "8525512763064", null, "Chén cơm Gốm \"Hà Liên\" – Tuyệt phẩm Men Hỏa Biến Thanh Hà", catGom, 75000, 125000, 107, 7, "https://cdn.hstatic.net/products/200001185819/halien1_7867d1f5a8b04fae8ddb2eb016f9764c.png", now, shopId);
        insertProduct(db, "5222594141431", null, "Cốc Uống Nước Gốm Bát Tràng", catGom, 57000, 95000, 126, 8, "https://cdn.hstatic.net/products/200001185819/_nh_ch_p_m_n_h_nh_2026-06-16_165548_153ba0e970424e228a658eeb6d868cde.png", now, shopId);
        insertProduct(db, "3515934207085", null, "Đĩa Trang Trí Gốm Biên Hòa Men Hổ Phách", catGom, 270000, 450000, 51, 8, "https://cdn.hstatic.net/products/200001185819/anh_chup_man_hinh_2026-06-16_161405_3fd1dd300abf4abc9de139ee8044f208.png", now, shopId);
        insertProduct(db, "4540818467400", null, "Giỏ Đan Đa Năng \"Chương Mỹ\" – Kèm lót vải hoa.", catMay, 99000, 165000, 100, 6, "https://cdn.hstatic.net/products/200001185819/chuongmy1_907c63e65c814246852adcb7a7235eb7.png", now, shopId);
        insertProduct(db, "3544095905270", null, "Giỏ Mây Bập Bênh – Eclipse Basket", catMay, 120000, 200000, 78, 4, "https://cdn.hstatic.net/products/200001185819/bapbenh_5885ddbc4d064534a7d3152d53ac882f.png", now, shopId);
        insertProduct(db, "3845431554350", null, "Giỏ Mây Đan Picnic – Rural Charm (Kèm lót vải)", catMay, 147000, 245000, 53, 6, "https://cdn.hstatic.net/products/200001185819/maydan1_22072c07a3cc4b2ea7a7421614329e12.png", now, shopId);
        insertProduct(db, "3591350224226", null, "Giỏ Mây Hai Tầng Trang Trí \"Tam Đảo\".", catMay, 143400, 239000, 51, 10, "https://cdn.hstatic.net/products/200001185819/maytang1_339e9fb0244f4ef7bfeee25f086cc54e.png", now, shopId);
        insertProduct(db, "434715860891", null, "Giỏ Tre Đan Thủ Công Làng Triêu Khúc", catMay, 168000, 280000, 107, 7, "https://cdn.hstatic.net/products/200001185819/_nh_ch_p_m_n_h_nh_2026-06-16_160834_8515d4b528254744be8e89a3217a0603.png", now, shopId);
        insertProduct(db, "7133235810902", null, "Giỏ Tre Đan Tròn Có Nắp", catMay, 210000, 350000, 66, 8, "https://cdn.hstatic.net/products/200001185819/_nh_ch_p_m_n_h_nh_2026-06-16_160424_ae08b8875a2b40fe8bfc475e4f940582.png", now, shopId);
        insertProduct(db, "4465403356592", null, "Hộp Trà Gỗ Có Ngăn Chia", catLich, 348000, 580000, 24, 6, "https://cdn.hstatic.net/products/200001185819/_nh_ch_p_m_n_h_nh_2026-06-16_160041_87ad83518c98482fa81fba38b7aa9239.png", now, shopId);
        insertProduct(db, "3636420046151", null, "Khay Mây Chữ Nhật – Minimalist Organizer", catMay, 119400, 199000, 18, 8, "https://cdn.hstatic.net/products/200001185819/khaymay1_d04ae3f0700e4962b23f9041a4a11042.png", now, shopId);
        insertProduct(db, "4401270345144", null, "Lịch Để Bàn \"Kỷ Nguyên\" – Đế gỗ thủ công", catLich, 129000, 215000, 112, 7, "https://cdn.hstatic.net/products/200001185819/kynguyen1_4d809ba97f5c4b779d2da4b1e693c45d.png", now, shopId);
        insertProduct(db, "850618113101", null, "Lịch Gỗ Lật Decor Mini 2026", catLich, 99000, 165000, 26, 6, "https://cdn.hstatic.net/products/200001185819/_nh_ch_p_m_n_h_nh_2026-06-16_155516_98e2a4b6c73649ab8b715ba37fe174f7.png", now, shopId);
        insertProduct(db, "143646213806", null, "Lịch Gỗ \"Mái Nhà Bình Yên\" – Dáng nhà gỗ thủ công", catLich, 150000, 250000, 90, 6, "https://cdn.hstatic.net/products/200001185819/mainha1_f15678714586416fa8be6d20929196c6.png", now, shopId);
        insertProduct(db, "633092692335", null, "Lịch Gỗ Đa Năng \"Ký Ức\" – Tích hợp khay bút & khung ảnh", catMay, 147000, 245000, 137, 9, "https://cdn.hstatic.net/products/200001185819/kyuc1_008e0a33666b4a06bdc47807b251fb3f.png", now, shopId);
        insertProduct(db, "2592633984434", null, "Lịch Gỗ Để Bàn Đa Năng \"Mộc Chuẩn\" – Tích hợp khay bút", catMay, 171000, 285000, 127, 5, "https://cdn.hstatic.net/products/200001185819/mocchuan1_49925c675bf440899310b0a503ee3026.png", now, shopId);
        insertProduct(db, "4009347136524", null, "Lịch Gỗ Trang Trí 2026 Phong Cách Truyền Thống", catLich, 192000, 320000, 77, 5, "https://cdn.hstatic.net/products/200001185819/_nh_ch_p_m_n_h_nh_2026-06-16_154727_5f6767f4d9e042fe8119b1f675c0c218.png", now, shopId);
        insertProduct(db, "3645980634545", null, "Lốc Lịch Gỗ Treo Tường Tượng Di Lặc 2026", catLich, 168000, 280000, 73, 7, "https://cdn.hstatic.net/products/200001185819/_nh_ch_p_m_n_h_nh_2026-06-16_154158_2eee4c9bcbc548798ed8df496c1ab19f.png", now, shopId);
        insertProduct(db, "2544824597359", null, "Lọ Cắm Hoa Gốm Phù Lãng Họa Tiết Phượng", catGom, 408000, 680000, 119, 9, "https://cdn.hstatic.net/products/200001185819/_nh_ch_p_m_n_h_nh_2026-06-16_153742_53ea0447960f4a0bb1f83bf961a6087b.png", now, shopId);
        insertProduct(db, "4547195951037", null, "Ngọc Lam Phú Quý – Bình gốm men ngọc họa tiết chim xuân", catGom, 354000, 590000, 102, 6, "https://cdn.hstatic.net/products/200001185819/1_51ab25cd71934293b66e1989357a253f.png", now, shopId);
        insertProduct(db, "3601145013371", null, "Ngọc Lam Phú Quý – Dĩa gốm men ngọc họa tiết chim xuân", catGom, 228000, 380000, 45, 10, "https://cdn.hstatic.net/products/200001185819/2_f91368866e8f43ef800780d1755e2689.png", now, shopId);
        insertProduct(db, "3040808342281", null, "Ngọc Lam Phú Quý – Lọ gốm men ngọc họa tiết chim xuân", catGom, 372000, 620000, 33, 3, "https://cdn.hstatic.net/products/200001185819/3_14689debc023444c922ebfc73e645edf.png", now, shopId);
        insertProduct(db, "5100032540701", null, "Sọt Cói Decor \"Nét Mộc\" – Có Quai Dây Thừng", catMay, 111000, 185000, 38, 5, "https://cdn.hstatic.net/products/200001185819/46_543a632696574ca3b4bbbf8ded69b42c.png", now, shopId);
        insertProduct(db, "5091306245401", null, "Tranh Gốm Phù Điêu Cảnh Làng Quê", catTranh, 660000, 1100000, 50, 9, "https://cdn.hstatic.net/products/200001185819/_nh_ch_p_m_n_h_nh_2026-06-16_152627_8a3373e9cc874549a9fac32729964518.png", now, shopId);
        insertProduct(db, "8616472276854", null, "Tranh Sơn Mài Tranh Phong Cảnh Việt Nam", catTranh, 720000, 1200000, 26, 9, "https://cdn.hstatic.net/products/200001185819/_nh_ch_p_m_n_h_nh_2026-06-16_152416_6938514fabc1431b9459abbdca64c289.png", now, shopId);
        insertProduct(db, "9090853144831", null, "Tranh Thêu Tay Phong Cảnh Hạ Long", catTranh, 870000, 1450000, 107, 10, "https://cdn.hstatic.net/products/200001185819/_nh_ch_p_m_n_h_nh_2026-06-16_152126_be686f5f2db64016b693d47d60fb0aa1.png", now, shopId);
        insertProduct(db, "502121022693", null, "Túi Xách Đan Thủ Công \"Sa Huỳnh\"", catMay, 177000, 295000, 145, 7, "https://cdn.hstatic.net/products/200001185819/63_5d6d3b8ab7be41019e0d27ec333613c0.png", now, shopId);
        insertProduct(db, "981734221129", null, "Tượng Bạch Mã \"Vinh Hiển\" – Điểm xuyết kim quý", catTranh, 1350000, 2250000, 12, 4, "https://cdn.hstatic.net/products/200001185819/81_2a88d8ad1022463e99b46b85163358a4.png", now, shopId);
        insertProduct(db, "3202093909188", null, "Tượng Linh Xà Ấn Vương \"Thanh Xuân\" – Khắc họa linh vật năm Tỵ", catTranh, 1350000, 2250000, 147, 7, "https://cdn.hstatic.net/products/200001185819/86_92b42b8b57a84e62a83b7c7c7212bf1f.png", now, shopId);
        insertProduct(db, "4724134945854", null, "Tượng Mèo Maneki-neko Gốm May Mắn", catTranh, 228000, 380000, 96, 4, "https://cdn.hstatic.net/products/200001185819/_nh_ch_p_m_n_h_nh_2026-06-16_150036_e734ac73be054cebbfeceacc32f7ad23.png", now, shopId);
        insertProduct(db, "5941537370354", null, "Tượng Phật Quan Âm Gốm Sứ Bát Tràng", catTranh, 510000, 850000, 84, 9, "https://cdn.hstatic.net/products/200001185819/_nh_ch_p_m_n_h_nh_2026-06-16_144624_194d41e0ca00431c9abec13a955bd5b8.png", now, shopId);
        insertProduct(db, "3125265836737", null, "Tượng Rồng \"Hoàng Long Tài Lộc\"", catTranh, 1800000, 3000000, 50, 10, "https://cdn.hstatic.net/products/200001185819/94_fe4e766f1e1a4add9e89a8fbcfb41d91.png", now, shopId);
        insertProduct(db, "1021792454515", null, "Tượng Rồng \"Khải Đức\"", catTranh, 1950000, 3250000, 10, 7, "https://cdn.hstatic.net/products/200001185819/85_0ddd9b88f262469cb557dcf7aa9c0806.png", now, shopId);
        insertProduct(db, "6006313579536", null, "Tượng Rồng Gốm Sứ Phong Thủy", catTranh, 552000, 920000, 138, 5, "https://cdn.hstatic.net/products/200001185819/88_621920cc558548f1b577d7423b9a13bd.png", now, shopId);
    }
}

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
}

package com.example.mpos;

import android.net.Uri;
import android.os.Bundle;
import android.content.Intent;
import android.view.Gravity;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;

import androidx.appcompat.app.AppCompatActivity;
import androidx.drawerlayout.widget.DrawerLayout;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

import com.example.mpos.ai.AiChatActivity;
import com.example.mpos.auth.LoginActivity;
import com.example.mpos.auth.SessionManager;
import com.example.mpos.shop.ShopListActivity;
import com.example.mpos.customer.CustomerListActivity;
import com.example.mpos.dao.ShopDao;
import com.example.mpos.database.DatabaseHelper;
import com.example.mpos.inventory.InventoryActivity;
import com.example.mpos.logistics.CreateShippingOrderActivity;
import com.example.mpos.logistics.ShippingActivity;
import com.example.mpos.model.Shop;
import com.example.mpos.order.OrderListActivity;
import com.example.mpos.pos.PosActivity;
import com.example.mpos.product.ProductListActivity;
import com.example.mpos.profile.ProfileActivity;
import com.example.mpos.report.ReportActivity;
import com.example.mpos.shift.ShiftActivity;
import com.example.mpos.ui.BottomNavHelper;
import com.example.mpos.notification.NotificationActivity;
import com.example.mpos.omnichannel.OmnichannelActivity;
import com.example.mpos.ui.MoreActivity;
import com.example.mpos.utils.CurrencyUtils;

import com.example.mpos.dao.OrderDao;
import com.example.mpos.dao.ShiftDao;
import com.example.mpos.notification.MposFcmService;
import android.widget.TextView;

public class MainActivity extends AppCompatActivity {
    private OrderDao orderDao;
    private DrawerLayout drawerLayout;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        SessionManager session = new SessionManager(this);
        if (!session.isLoggedIn()) { startActivity(new Intent(this, LoginActivity.class)); finish(); return; }

        // Check shop is selected
        if (!session.hasShopSelected()) {
            startActivity(new Intent(this, ShopListActivity.class));
            finish();
            return;
        }

        setContentView(R.layout.activity_main);
        orderDao = new OrderDao(new DatabaseHelper(this), session.getShopId());
        BottomNavHelper.bind(this);

        // Subscribe this device to push notifications for this shop
        MposFcmService.subscribeToShop(session.getShopId());

        com.example.mpos.model.User currentUser = session.getUser();
        String name = currentUser != null ? currentUser.username : "Admin";
        if (name.contains("@")) name = name.substring(0, name.indexOf("@"));
        String role = currentUser != null ? currentUser.role : "STAFF";
        ((TextView) findViewById(R.id.txtGreeting)).setText("Xin chào, " + capitalize(name) + " (" + roleLabel(role) + ")");

        TextView txtShopName = findViewById(R.id.txtShopName);
        if (txtShopName != null) txtShopName.setText(session.getShopName());

        TextView btnSwitchShop = findViewById(R.id.btnSwitchShop);
        if (btnSwitchShop != null) {
            btnSwitchShop.setOnClickListener(v -> switchShop(session));
        }

        // Set current date
        String today = new SimpleDateFormat("EEEE, dd/MM/yyyy", new Locale("vi", "VN")).format(new Date());
        TextView txtDate = findViewById(R.id.txtDate);
        if (txtDate != null) txtDate.setText(today);

        boolean isStaff   = "STAFF".equals(role);
        boolean isAdmin   = "ADMIN".equals(role);

        // Wire clicks — all buttons visible for all roles; restricted ones show a notice
        View btnNotificationMain = findViewById(R.id.btnNotification);
        if (btnNotificationMain != null)
            btnNotificationMain.setOnClickListener(v -> startActivity(new Intent(this, NotificationActivity.class)));
        findViewById(R.id.btnNewSale).setOnClickListener(v -> startActivity(new Intent(this, PosActivity.class)));
        findViewById(R.id.btnShift).setOnClickListener(v -> startActivity(new Intent(this, ShiftActivity.class)));
        findViewById(R.id.btnOrders).setOnClickListener(v -> startActivity(new Intent(this, OrderListActivity.class)));
        findViewById(R.id.btnProducts).setOnClickListener(v -> {
            if (isStaff) { noPermission(); return; }
            startActivity(new Intent(this, ProductListActivity.class));
        });
        findViewById(R.id.btnInventory).setOnClickListener(v -> {
            if (isStaff) { noPermission(); return; }
            startActivity(new Intent(this, InventoryActivity.class));
        });
        findViewById(R.id.btnReports).setOnClickListener(v -> {
            if (isStaff) { noPermission(); return; }
            startActivity(new Intent(this, ReportActivity.class));
        });
        findViewById(R.id.btnMore).setOnClickListener(v -> {
            if (!isAdmin) { noPermission(); return; }
            startActivity(new Intent(this, MoreActivity.class));
        });
        findViewById(R.id.btnCustomers).setOnClickListener(v -> startActivity(new Intent(this, CustomerListActivity.class)));
        findViewById(R.id.btnProfile).setOnClickListener(v -> startActivity(new Intent(this, ProfileActivity.class)));
        findViewById(R.id.btnAiChat).setOnClickListener(v -> startActivity(new Intent(this, AiChatActivity.class)));
        findViewById(R.id.btnShipping).setOnClickListener(v -> startActivity(new Intent(this, CreateShippingOrderActivity.class)));
        View btnViewChannels = findViewById(R.id.btnViewChannels);
        if (btnViewChannels != null) btnViewChannels.setOnClickListener(v -> startActivity(new Intent(this, OmnichannelActivity.class)));
        View btnChannelCard = findViewById(R.id.btnChannelCard);
        if (btnChannelCard != null) btnChannelCard.setOnClickListener(v -> startActivity(new Intent(this, OmnichannelActivity.class)));
        findViewById(R.id.btnLogout).setOnClickListener(v -> doLogout(session));
        // Load event banner async
        android.widget.ImageView imgBanner = findViewById(R.id.imgEventBanner);
        if (imgBanner != null) {
            java.util.concurrent.Executors.newSingleThreadExecutor().execute(() -> {
                android.graphics.Bitmap bm = android.graphics.BitmapFactory.decodeResource(getResources(), R.drawable.img_event_banner);
                runOnUiThread(() -> imgBanner.setImageBitmap(bm));
            });
        }

        // ── DRAWER SETUP ──
        drawerLayout = findViewById(R.id.drawerLayout);
        findViewById(R.id.btnOpenDrawer).setOnClickListener(v ->
            drawerLayout.openDrawer(Gravity.START));

        setupDrawer(session, role);
    }

    private void setupDrawer(SessionManager session, String role) {
        // Header: shop name + role
        TextView drawerShopName = findViewById(R.id.drawerShopName);
        TextView drawerRoleBadge = findViewById(R.id.drawerRoleBadge);
        TextView drawerShopInitial = findViewById(R.id.drawerShopInitial);
        ImageView drawerShopLogo = findViewById(R.id.drawerShopLogo);

        String shopName = session.getShopName();
        if (drawerShopName != null) drawerShopName.setText(shopName != null ? shopName : "Cửa hàng");
        if (drawerRoleBadge != null) drawerRoleBadge.setText(shopRoleLabel(session.getShopRole()));

        // Shop initial letter
        if (drawerShopInitial != null && shopName != null && !shopName.isEmpty()) {
            drawerShopInitial.setText(String.valueOf(shopName.charAt(0)).toUpperCase());
        }

        // Try load shop logo
        try {
            Shop shop = new ShopDao(new DatabaseHelper(this)).getById(session.getShopId());
            if (shop != null && shop.logoUri != null && !shop.logoUri.isEmpty() && drawerShopLogo != null) {
                drawerShopLogo.setImageURI(Uri.parse(shop.logoUri));
                drawerShopLogo.setVisibility(android.view.View.VISIBLE);
                if (drawerShopInitial != null) drawerShopInitial.setVisibility(android.view.View.GONE);
            }
        } catch (Exception ignored) {}

        // Menu item clicks
        LinearLayout itemShopSettings = findViewById(R.id.drawerItemShopSettings);
        LinearLayout itemStaff        = findViewById(R.id.drawerItemStaff);
        LinearLayout itemReports      = findViewById(R.id.drawerItemReports);
        LinearLayout itemSupport      = findViewById(R.id.drawerItemSupport);
        LinearLayout itemReferral     = findViewById(R.id.drawerItemReferral);
        LinearLayout itemRate         = findViewById(R.id.drawerItemRate);
        LinearLayout itemProfile      = findViewById(R.id.drawerItemProfile);
        LinearLayout itemSwitchShop   = findViewById(R.id.drawerItemSwitchShop);
        LinearLayout itemLogout       = findViewById(R.id.drawerItemLogout);

        if (itemShopSettings != null) itemShopSettings.setOnClickListener(v -> {
            drawerLayout.closeDrawers();
            startActivity(new Intent(this, MoreActivity.class));
        });
        if (itemStaff != null) itemStaff.setOnClickListener(v -> {
            drawerLayout.closeDrawers();
            startActivity(new Intent(this, ProfileActivity.class));
        });
        if (itemReports != null) itemReports.setOnClickListener(v -> {
            drawerLayout.closeDrawers();
            startActivity(new Intent(this, ReportActivity.class));
        });
        if (itemSupport != null) itemSupport.setOnClickListener(v -> {
            drawerLayout.closeDrawers();
            new android.app.AlertDialog.Builder(this)
                .setTitle("Hỗ trợ")
                .setMessage("Email: support@quay.vn\nHotline: 1800 xxxx\n\nThời gian hỗ trợ: 8h - 22h mỗi ngày")
                .setPositiveButton("Đóng", null)
                .show();
        });
        if (itemReferral != null) itemReferral.setOnClickListener(v -> {
            drawerLayout.closeDrawers();
            new android.app.AlertDialog.Builder(this)
                .setTitle("Giới thiệu & Nhận thưởng")
                .setMessage("Tính năng đang được phát triển. Sắp ra mắt!")
                .setPositiveButton("Đóng", null)
                .show();
        });
        if (itemRate != null) itemRate.setOnClickListener(v -> {
            drawerLayout.closeDrawers();
            new android.app.AlertDialog.Builder(this)
                .setTitle("Đánh giá ứng dụng")
                .setMessage("Cảm ơn bạn đã sử dụng Quầy mPOS Pro!\nTính năng đánh giá sẽ sớm được cập nhật.")
                .setPositiveButton("Đóng", null)
                .show();
        });
        if (itemProfile != null) itemProfile.setOnClickListener(v -> {
            drawerLayout.closeDrawers();
            startActivity(new Intent(this, ProfileActivity.class));
        });
        if (itemSwitchShop != null) itemSwitchShop.setOnClickListener(v -> {
            drawerLayout.closeDrawers();
            switchShop(session);
        });
        if (itemLogout != null) itemLogout.setOnClickListener(v -> {
            drawerLayout.closeDrawers();
            doLogout(session);
        });
    }

    private void noPermission() {
        android.widget.Toast.makeText(this, "Bạn không có quyền truy cập chức năng này", android.widget.Toast.LENGTH_SHORT).show();
    }

    private void switchShop(SessionManager session) {
        session.saveShop(-1, "", "");
        startActivity(new Intent(this, ShopListActivity.class));
        finish();
    }

    private void doLogout(SessionManager session) {
        com.example.mpos.auth.FirebaseAuthHelper.signOut();
        session.clear();
        startActivity(new Intent(this, LoginActivity.class));
        finish();
    }

    private String shopRoleLabel(String role) {
        if ("OWNER".equals(role))   return "Chủ cửa hàng";
        if ("MANAGER".equals(role)) return "Quản lý";
        return "Nhân viên";
    }

    @Override protected void onResume() { super.onResume(); loadStats(); }

    private void loadStats() {
        if (orderDao == null) return;
        try {
            // Shift status chip
            SessionManager sess = new SessionManager(this);
            long userId = sess.getUser().id;
            long shopId = sess.getShopId();
            boolean shiftOpen = new ShiftDao(new DatabaseHelper(this), shopId).getOpenShiftId(userId) > 0;
            TextView chipShift = findViewById(R.id.txtShiftStatusChip);
            if (chipShift != null) {
                chipShift.setTextColor(0xFF1C2333);
                if (shiftOpen) {
                    chipShift.setText("Đang mở ca");
                    chipShift.setBackgroundResource(R.drawable.badge_success);
                } else {
                    chipShift.setText("Chưa mở ca");
                    chipShift.setBackgroundResource(R.drawable.badge_warning);
                }
            }
            OrderDao.DailyStats stats = orderDao.getTodayStats();
            TextView tv;
            // Hero card
            if ((tv = findViewById(R.id.txtRevenue)) != null) tv.setText(CurrencyUtils.vnd(stats.totalRevenue));
            if ((tv = findViewById(R.id.txtRevenueSubtitle)) != null) tv.setText(stats.orderCount + " đơn hàng hôm nay");
            if ((tv = findViewById(R.id.txtPosRevenue)) != null) tv.setText(CurrencyUtils.vnd(stats.totalRevenue));
            if ((tv = findViewById(R.id.txtPosOrders)) != null) tv.setText(stats.orderCount + " đơn hàng");

            // Dashboard date header
            java.text.SimpleDateFormat sdf = new java.text.SimpleDateFormat("dd/MM/yy", java.util.Locale.getDefault());
            String dateStr = "HÔM NAY, " + sdf.format(new java.util.Date());
            if ((tv = findViewById(R.id.txtDashboardDate)) != null) tv.setText(dateStr);

            // Dashboard stats
            if ((tv = findViewById(R.id.txtTodayRevenue)) != null) tv.setText(CurrencyUtils.vnd(stats.totalRevenue));
            if ((tv = findViewById(R.id.txtOrderCount)) != null) tv.setText(String.valueOf(stats.orderCount));
            long profit = stats.totalRevenue - stats.totalCost;
            if ((tv = findViewById(R.id.txtTodayProfit)) != null) tv.setText(CurrencyUtils.vnd(profit));

            // Low stock
            int lowStock = orderDao.getLowStockCount();
            if ((tv = findViewById(R.id.txtProductLow)) != null) tv.setText(lowStock + " sản phẩm");
        } catch (Exception e) {
            android.util.Log.e("MainActivity", "loadStats error: " + e.getMessage(), e);
        }
    }

    private String capitalize(String s) { return s.isEmpty() ? s : Character.toUpperCase(s.charAt(0)) + s.substring(1); }

    private void setVisible(int id, boolean visible) {
        android.view.View v = findViewById(id);
        if (v != null) v.setVisibility(visible ? android.view.View.VISIBLE : android.view.View.GONE);
    }

    private String roleLabel(String role) {
        if ("ADMIN".equals(role))   return "Admin";
        if ("MANAGER".equals(role)) return "Quản lý";
        return "Nhân viên";
    }
}

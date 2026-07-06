package com.example.mpos;

import android.content.Intent;
import android.database.Cursor;
import android.os.Bundle;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.example.mpos.auth.LoginActivity;
import com.example.mpos.auth.SessionManager;
import com.example.mpos.customer.CustomerListActivity;
import com.example.mpos.database.DatabaseHelper;
import com.example.mpos.inventory.InventoryActivity;
import com.example.mpos.model.User;
import com.example.mpos.order.OrderListActivity;
import com.example.mpos.pos.PosActivity;
import com.example.mpos.settings.SettingsActivity;
import com.example.mpos.notification.NotificationActivity;
import com.example.mpos.utils.CurrencyUtils;

public class StaffDashboardActivity extends AppCompatActivity {

    private DatabaseHelper db;
    private User currentUser;

    private TextView txtStaffInitial, txtStaffName, txtStaffRole;
    private LinearLayout layoutAlertBanner;
    private TextView txtAlertMessage;
    private TextView txtShiftOrderCount, txtShiftRevenue, txtShiftInfo;
    private LinearLayout layoutOrder1, layoutOrder2, layoutOrder3;
    private TextView txtOrder1Code, txtOrder1Status, txtOrder1Customer, txtOrder1Amount, txtOrder1Pay;
    private TextView txtOrder2Code, txtOrder2Status, txtOrder2Customer, txtOrder2Amount, txtOrder2Pay;
    private TextView txtOrder3Code, txtOrder3Status, txtOrder3Customer, txtOrder3Amount, txtOrder3Pay;
    private TextView txtNoOrders;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        SessionManager session = new SessionManager(this);
        if (!session.isLoggedIn()) {
            startActivity(new Intent(this, LoginActivity.class));
            finish();
            return;
        }
        currentUser = session.getUser();
        db = new DatabaseHelper(this);

        setContentView(R.layout.activity_staff_dashboard);

        // AppBar
        txtStaffInitial = findViewById(R.id.txtStaffInitial);
        txtStaffName    = findViewById(R.id.txtStaffName);
        txtStaffRole    = findViewById(R.id.txtStaffRole);

        // Alert banner
        layoutAlertBanner = findViewById(R.id.layoutAlertBanner);
        txtAlertMessage   = findViewById(R.id.txtAlertMessage);
        layoutAlertBanner.setOnClickListener(v ->
            startActivity(new Intent(this, InventoryActivity.class)));

        // Summary card
        txtShiftOrderCount = findViewById(R.id.txtShiftOrderCount);
        txtShiftRevenue    = findViewById(R.id.txtShiftRevenue);
        txtShiftInfo       = findViewById(R.id.txtShiftInfo);

        // Quick actions
        findViewById(R.id.btnNewSale).setOnClickListener(v ->
            startActivity(new Intent(this, PosActivity.class)));
        findViewById(R.id.btnScanBarcode).setOnClickListener(v ->
            startActivity(new Intent(this, PosActivity.class)));
        findViewById(R.id.btnViewOrders).setOnClickListener(v ->
            startActivity(new Intent(this, OrderListActivity.class)));
        findViewById(R.id.btnCustomers).setOnClickListener(v ->
            startActivity(new Intent(this, CustomerListActivity.class)));

        // Recent orders
        findViewById(R.id.btnViewAllOrders).setOnClickListener(v ->
            startActivity(new Intent(this, OrderListActivity.class)));

        layoutOrder1 = findViewById(R.id.layoutOrder1);
        layoutOrder2 = findViewById(R.id.layoutOrder2);
        layoutOrder3 = findViewById(R.id.layoutOrder3);
        txtOrder1Code = findViewById(R.id.txtOrder1Code);   txtOrder1Status = findViewById(R.id.txtOrder1Status);
        txtOrder1Customer = findViewById(R.id.txtOrder1Customer); txtOrder1Amount = findViewById(R.id.txtOrder1Amount);
        txtOrder1Pay  = findViewById(R.id.txtOrder1Pay);
        txtOrder2Code = findViewById(R.id.txtOrder2Code);   txtOrder2Status = findViewById(R.id.txtOrder2Status);
        txtOrder2Customer = findViewById(R.id.txtOrder2Customer); txtOrder2Amount = findViewById(R.id.txtOrder2Amount);
        txtOrder2Pay  = findViewById(R.id.txtOrder2Pay);
        txtOrder3Code = findViewById(R.id.txtOrder3Code);   txtOrder3Status = findViewById(R.id.txtOrder3Status);
        txtOrder3Customer = findViewById(R.id.txtOrder3Customer); txtOrder3Amount = findViewById(R.id.txtOrder3Amount);
        txtOrder3Pay  = findViewById(R.id.txtOrder3Pay);
        txtNoOrders   = findViewById(R.id.txtNoOrders);

        layoutOrder1.setOnClickListener(v -> startActivity(new Intent(this, OrderListActivity.class)));
        layoutOrder2.setOnClickListener(v -> startActivity(new Intent(this, OrderListActivity.class)));
        layoutOrder3.setOnClickListener(v -> startActivity(new Intent(this, OrderListActivity.class)));

        // Notification bell
        View btnNotification = findViewById(R.id.btnNotification);
        if (btnNotification != null)
            btnNotification.setOnClickListener(v -> startActivity(new Intent(this, NotificationActivity.class)));

        // Bottom nav
        findViewById(R.id.navStaffHome).setOnClickListener(v -> { /* already here */ });
        findViewById(R.id.navStaffPos).setOnClickListener(v ->
            startActivity(new Intent(this, PosActivity.class).addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)));
        findViewById(R.id.navStaffOrders).setOnClickListener(v ->
            startActivity(new Intent(this, OrderListActivity.class).addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)));
        findViewById(R.id.navStaffInventory).setOnClickListener(v ->
            startActivity(new Intent(this, InventoryActivity.class).addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)));
        findViewById(R.id.navStaffSettings).setOnClickListener(v ->
            startActivity(new Intent(this, SettingsActivity.class).addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)));

        populateAppBar();
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadStats();
    }

    private void populateAppBar() {
        if (currentUser == null) return;
        String username = currentUser.username;
        String displayName = username.contains("@") ? username.substring(0, username.indexOf("@")) : username;
        String initial = displayName.isEmpty() ? "?" : String.valueOf(Character.toUpperCase(displayName.charAt(0)));
        txtStaffInitial.setText(initial);
        txtStaffName.setText("Xin chào, " + capitalize(displayName));
        txtStaffRole.setText(roleLabel(currentUser.role));
    }

    private void loadStats() {
        long todayStart = getTodayStartMs();
        long userId = currentUser != null ? currentUser.id : -1;

        // Low stock alert
        Cursor lowStock = db.getReadableDatabase().rawQuery(
            "SELECT COUNT(*) FROM products WHERE is_active=1 AND stock_quantity<=min_stock_quantity", null);
        try {
            if (lowStock.moveToFirst()) {
                int count = lowStock.getInt(0);
                if (count > 0) {
                    layoutAlertBanner.setVisibility(View.VISIBLE);
                    txtAlertMessage.setText(count + " sản phẩm sắp hết hàng");
                } else {
                    layoutAlertBanner.setVisibility(View.GONE);
                }
            }
        } finally { lowStock.close(); }

        // Today summary for this user
        Cursor summary = db.getReadableDatabase().rawQuery(
            "SELECT COUNT(*), COALESCE(SUM(total_amount),0) FROM orders WHERE created_at>=? AND user_id=? AND status!='CANCELLED'",
            new String[]{String.valueOf(todayStart), String.valueOf(userId)});
        try {
            if (summary.moveToFirst()) {
                txtShiftOrderCount.setText(String.valueOf(summary.getInt(0)));
                txtShiftRevenue.setText(CurrencyUtils.vnd(summary.getLong(1)));
            }
        } finally { summary.close(); }

        // Active shift info
        Cursor shift = db.getReadableDatabase().rawQuery(
            "SELECT shift_code, opened_at FROM shifts WHERE user_id=? AND status='OPEN' ORDER BY opened_at DESC LIMIT 1",
            new String[]{String.valueOf(userId)});
        try {
            if (shift.moveToFirst()) {
                long openedAt = shift.getLong(1);
                java.text.SimpleDateFormat sdf = new java.text.SimpleDateFormat("HH:mm", java.util.Locale.getDefault());
                String openTime = sdf.format(new java.util.Date(openedAt));
                txtShiftInfo.setText("🕐 Ca từ " + openTime + "  ·  " + shift.getString(0));
            } else {
                txtShiftInfo.setText("🕐 Chưa mở ca làm việc");
            }
        } finally { shift.close(); }

        // Recent 3 orders (all orders, not just this user's, for better UX)
        Cursor orders = db.getReadableDatabase().rawQuery(
            "SELECT o.order_code, o.status, o.total_amount, COALESCE(c.full_name,'Khách lẻ') " +
            "FROM orders o LEFT JOIN customers c ON o.customer_id=c.id " +
            "ORDER BY o.created_at DESC LIMIT 3", null);

        LinearLayout[] rows   = {layoutOrder1,     layoutOrder2,     layoutOrder3};
        TextView[] codes      = {txtOrder1Code,    txtOrder2Code,    txtOrder3Code};
        TextView[] statuses   = {txtOrder1Status,  txtOrder2Status,  txtOrder3Status};
        TextView[] customers  = {txtOrder1Customer,txtOrder2Customer,txtOrder3Customer};
        TextView[] amounts    = {txtOrder1Amount,  txtOrder2Amount,  txtOrder3Amount};
        TextView[] pays       = {txtOrder1Pay,     txtOrder2Pay,     txtOrder3Pay};

        for (LinearLayout row : rows) row.setVisibility(View.GONE);

        int idx = 0;
        try {
            while (orders.moveToNext() && idx < 3) {
                rows[idx].setVisibility(View.VISIBLE);
                codes[idx].setText(orders.getString(0));
                bindOrderStatus(statuses[idx], orders.getString(1));
                customers[idx].setText(orders.getString(3));
                amounts[idx].setText(CurrencyUtils.vnd(orders.getLong(2)));
                bindPayStatus(pays[idx], orders.getString(1));
                idx++;
            }
        } finally { orders.close(); }

        txtNoOrders.setVisibility(idx == 0 ? View.VISIBLE : View.GONE);
    }

    private void bindOrderStatus(TextView badge, String status) {
        int drawableRes;
        String label;
        switch (status == null ? "" : status) {
            case "CANCELLED": drawableRes = R.drawable.bg_badge_pill_red;    label = "HỦY";  break;
            case "PENDING":   drawableRes = R.drawable.bg_badge_pill_orange; label = "CHỜ";  break;
            default:          drawableRes = R.drawable.bg_badge_pill_green;  label = "XONG"; break;
        }
        badge.setBackground(getDrawable(drawableRes));
        badge.setText(label);
        badge.setTextColor(0xFFFFFFFF);
    }

    private void bindPayStatus(TextView chip, String orderStatus) {
        if ("COMPLETED".equals(orderStatus)) {
            chip.setBackground(getDrawable(R.drawable.bg_chip_paid));
            chip.setTextColor(0xFF15803D);
            chip.setText("Đã TT");
        } else if ("CANCELLED".equals(orderStatus)) {
            chip.setBackground(getDrawable(R.drawable.bg_chip_unpaid));
            chip.setTextColor(0xFFEF4444);
            chip.setText("Đã hủy");
        } else {
            chip.setBackground(getDrawable(R.drawable.bg_chip_unpaid));
            chip.setTextColor(0xFFA16207);
            chip.setText("Chưa TT");
        }
    }

    private long getTodayStartMs() {
        java.util.Calendar cal = java.util.Calendar.getInstance();
        cal.set(java.util.Calendar.HOUR_OF_DAY, 0);
        cal.set(java.util.Calendar.MINUTE, 0);
        cal.set(java.util.Calendar.SECOND, 0);
        cal.set(java.util.Calendar.MILLISECOND, 0);
        return cal.getTimeInMillis();
    }

    private String capitalize(String s) {
        return s.isEmpty() ? s : Character.toUpperCase(s.charAt(0)) + s.substring(1);
    }

    private String roleLabel(String role) {
        if ("ADMIN".equals(role))   return "Admin";
        if ("MANAGER".equals(role)) return "Quản lý";
        return "Nhân viên";
    }
}
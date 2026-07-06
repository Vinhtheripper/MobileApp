package com.example.mpos.order;

import android.content.Intent;
import android.database.Cursor;
import android.os.Bundle;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import com.example.mpos.R;
import com.example.mpos.auth.SessionManager;
import com.example.mpos.database.DatabaseHelper;
import com.example.mpos.MainActivity;
import com.example.mpos.inventory.InventoryActivity;
import com.example.mpos.pos.PosActivity;
import com.example.mpos.settings.SettingsActivity;
import com.example.mpos.utils.CurrencyUtils;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class OrderListActivity extends AppCompatActivity {

    private static final String[] STATUSES = {"Tất cả", "PAID", "PENDING", "CANCELLED"};
    private static final String[] STATUS_LABELS = {"Tất cả", "Đã thanh toán", "Chờ xử lý", "Đã huỷ"};
    private String selectedStatus = null; // null = all

    private OrderAdapter adapter;
    private DatabaseHelper db;
    private long shopId = 1;
    private TextView txtCountStat, txtRevenueStat;

    static class OrderRow {
        long id; String orderCode, status, date; long total; int itemCount; String channel;
    }

    @Override
    public void onCreate(Bundle state) {
        super.onCreate(state);
        setContentView(R.layout.activity_order_list);
        bindStaffNav();

        db = new DatabaseHelper(this);
        shopId = new SessionManager(this).getShopId();
        txtCountStat = findViewById(R.id.txtOrderCountStat);
        txtRevenueStat = findViewById(R.id.txtRevenueStat);

        adapter = new OrderAdapter();
        ListView list = findViewById(R.id.listOrders);
        list.setAdapter(adapter);
        list.setOnItemClickListener((p, v, pos, id) -> {
            OrderRow row = adapter.getItem(pos);
            if (row == null) return;
            Intent i = new Intent(this, OrderDetailActivity.class);
            i.putExtra("order_id", row.id);
            startActivity(i);
        });

        buildFilterChips();

        findViewById(R.id.btnBack).setOnClickListener(v -> finish());
        load();
    }

    @Override protected void onResume() { super.onResume(); load(); }

    private void buildFilterChips() {
        LinearLayout layout = findViewById(R.id.layoutStatusFilter);
        layout.removeAllViews();
        for (int i = 0; i < STATUSES.length; i++) {
            final String statusValue = i == 0 ? null : STATUSES[i];
            final String label = STATUS_LABELS[i];
            TextView chip = new TextView(this);
            chip.setText(label);
            chip.setTextSize(12f);
            chip.setGravity(Gravity.CENTER);
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT);
            lp.setMarginEnd(8);
            chip.setLayoutParams(lp);
            chip.setPadding(dp(14), dp(6), dp(14), dp(6));
            boolean isSelected = (statusValue == null && selectedStatus == null) ||
                                 (statusValue != null && statusValue.equals(selectedStatus));
            setChipStyle(chip, isSelected);
            chip.setOnClickListener(v -> {
                selectedStatus = statusValue;
                for (int j = 0; j < layout.getChildCount(); j++) {
                    setChipStyle((TextView) layout.getChildAt(j), j == indexOf(statusValue));
                }
                load();
            });
            layout.addView(chip);
        }
    }

    private int indexOf(String val) {
        if (val == null) return 0;
        for (int i = 1; i < STATUSES.length; i++) if (STATUSES[i].equals(val)) return i;
        return 0;
    }

    private void setChipStyle(TextView chip, boolean selected) {
        chip.setBackgroundResource(selected ? R.drawable.chip_selected : R.drawable.chip_default);
        chip.setTextColor(ContextCompat.getColor(this, selected ? R.color.blue_primary_dark : R.color.text_secondary));
    }

    private void load() {
        String shopFilter = " shop_id=" + shopId;
        String where = selectedStatus != null
            ? " WHERE" + shopFilter + " AND status='" + selectedStatus + "'"
            : " WHERE" + shopFilter;
        Cursor c = db.getReadableDatabase().rawQuery(
            "SELECT o.id, o.order_code, o.status, o.total_amount, o.created_at, o.channel, " +
            "COUNT(i.id) FROM orders o LEFT JOIN order_items i ON i.order_id=o.id" + where +
            " GROUP BY o.id ORDER BY o.created_at DESC", null);

        List<OrderRow> rows = new ArrayList<>();
        SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault());
        long totalRevenue = 0;
        try {
            while (c.moveToNext()) {
                OrderRow row = new OrderRow();
                row.id        = c.getLong(0);
                row.orderCode = c.getString(1);
                row.status    = c.getString(2);
                row.total     = c.getLong(3);
                row.date      = sdf.format(new Date(c.getLong(4)));
                row.channel   = c.getString(5);
                row.itemCount = c.getInt(6);
                rows.add(row);
                if ("PAID".equals(row.status)) totalRevenue += row.total;
            }
        } finally { c.close(); }

        adapter.setRows(rows);
        txtCountStat.setText(rows.size() + " đơn hàng");
        txtRevenueStat.setText(CurrencyUtils.vnd(totalRevenue));
    }

    class OrderAdapter extends BaseAdapter {
        private List<OrderRow> rows = new ArrayList<>();
        void setRows(List<OrderRow> r) { rows = r; notifyDataSetChanged(); }
        @Override public int getCount() { return rows.isEmpty() ? 1 : rows.size(); }
        @Override public OrderRow getItem(int p) { return rows.isEmpty() ? null : rows.get(p); }
        @Override public long getItemId(int p) { return p; }

        @Override
        public View getView(int pos, View convertView, ViewGroup parent) {
            if (rows.isEmpty()) {
                TextView tv = new TextView(OrderListActivity.this);
                tv.setText("Chưa có đơn hàng\nTạo đơn mới từ màn Bán hàng");
                tv.setGravity(Gravity.CENTER);
                tv.setPadding(0, 80, 0, 80);
                tv.setTextColor(getResources().getColor(R.color.text_secondary));
                return tv;
            }
            if (convertView == null || convertView instanceof TextView)
                convertView = LayoutInflater.from(OrderListActivity.this).inflate(R.layout.item_order, parent, false);
            OrderRow row = rows.get(pos);
            ((TextView) convertView.findViewById(R.id.txtOrderCode)).setText(row.orderCode);
            ((TextView) convertView.findViewById(R.id.txtOrderDate)).setText(row.date);
            ((TextView) convertView.findViewById(R.id.txtOrderTotal)).setText(CurrencyUtils.vnd(row.total));
            ((TextView) convertView.findViewById(R.id.txtOrderItems)).setText(row.itemCount + " sản phẩm");

            TextView txtStatus = convertView.findViewById(R.id.txtOrderStatus);
            txtStatus.setText(statusLabel(row.status));
            txtStatus.setBackgroundResource(statusBadge(row.status));
            txtStatus.setTextColor(ContextCompat.getColor(OrderListActivity.this, statusColor(row.status)));

            ((TextView) convertView.findViewById(R.id.txtOrderChannel)).setText(row.channel);
            return convertView;
        }
    }

    private String statusLabel(String s) {
        if (s == null) return "—";
        switch (s) {
            case "PAID":      return "Đã thanh toán";
            case "PENDING":   return "Chờ xử lý";
            case "CANCELLED": return "Đã huỷ";
            default:          return s;
        }
    }
    private int statusBadge(String s) {
        if ("CANCELLED".equals(s)) return R.drawable.badge_error;
        if ("PENDING".equals(s))   return R.drawable.badge_warning;
        return R.drawable.badge_success;
    }
    private int statusColor(String s) {
        if ("CANCELLED".equals(s)) return R.color.status_error;
        if ("PENDING".equals(s))   return R.color.status_warning;
        return R.color.status_success;
    }

    private void bindStaffNav() {
        navClick(R.id.navStaffHome,      MainActivity.class);
        navClick(R.id.navStaffPos,       PosActivity.class);
        navClick(R.id.navStaffOrders,    OrderListActivity.class);
        navClick(R.id.navStaffInventory, InventoryActivity.class);
        navClick(R.id.navStaffSettings,  SettingsActivity.class);
    }

    private void navClick(int id, Class<?> target) {
        View v = findViewById(id);
        if (v != null) v.setOnClickListener(x -> {
            if (!getClass().equals(target))
                startActivity(new Intent(this, target).addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP));
        });
    }

    private int dp(int dp) { return Math.round(dp * getResources().getDisplayMetrics().density); }
}

package com.example.mpos.report;

import android.database.Cursor;
import android.graphics.Color;
import android.graphics.Typeface;
import android.os.Bundle;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import com.example.mpos.R;
import com.example.mpos.database.DatabaseHelper;
import com.example.mpos.ui.BottomNavHelper;
import com.example.mpos.utils.CurrencyUtils;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;

public class ReportActivity extends AppCompatActivity {

    private static final int PERIOD_7     = 7;
    private static final int PERIOD_30    = 30;
    private static final int PERIOD_MONTH = -1;

    private int currentPeriod = PERIOD_7;
    private DatabaseHelper dbHelper;
    private long shopId;

    @Override
    public void onCreate(Bundle state) {
        super.onCreate(state);
        setContentView(R.layout.activity_report);
        dbHelper = new DatabaseHelper(this);
        shopId = new com.example.mpos.auth.SessionManager(this).getShopId();
        BottomNavHelper.bind(this);
        findViewById(R.id.btnBack).setOnClickListener(v -> finish());
        setupTabs();
        loadReport();
    }

    @Override protected void onResume() { super.onResume(); loadReport(); }

    // ─── Tabs ────────────────────────────────────────────────────────────────────

    private void setupTabs() {
        TextView t7   = findViewById(R.id.tab7days);
        TextView t30  = findViewById(R.id.tab30days);
        TextView tMon = findViewById(R.id.tabThisMonth);
        t7.setOnClickListener(v  -> { currentPeriod = PERIOD_7;     selectTab(t7,   t30, tMon); loadReport(); });
        t30.setOnClickListener(v -> { currentPeriod = PERIOD_30;    selectTab(t30,  t7,  tMon); loadReport(); });
        tMon.setOnClickListener(v-> { currentPeriod = PERIOD_MONTH; selectTab(tMon, t7,  t30);  loadReport(); });
    }

    private void selectTab(TextView active, TextView... rest) {
        active.setBackgroundResource(R.drawable.bg_tab_selected);
        active.setTextColor(Color.WHITE);
        active.setTypeface(null, Typeface.BOLD);
        for (TextView t : rest) {
            t.setBackgroundResource(R.drawable.bg_tab_unselected);
            t.setTextColor(Color.parseColor("#64748B"));
            t.setTypeface(null, Typeface.NORMAL);
        }
    }

    // ─── Load ────────────────────────────────────────────────────────────────────

    private void loadReport() {
        long from = getPeriodStart();
        loadSummary(from);
        loadRevenueChart(from);
        loadTopProducts(from);
        loadRecentOrders(from);
        loadLowStock();
    }

    // ─── Summary ─────────────────────────────────────────────────────────────────

    private void loadSummary(long from) {
        Cursor c = dbHelper.getReadableDatabase().rawQuery(
            "SELECT COUNT(*), COALESCE(SUM(total_amount),0), COALESCE(SUM(vat_amount),0) " +
            "FROM orders WHERE shop_id=? AND status='PAID' AND created_at>=?",
            new String[]{String.valueOf(shopId), String.valueOf(from)});
        try {
            if (c.moveToFirst()) {
                int orders  = c.getInt(0);
                long rev    = c.getLong(1);
                long vat    = c.getLong(2);
                long avg    = orders > 0 ? rev / orders : 0;
                setText(R.id.txtSummaryRevenue, CurrencyUtils.vnd(rev));
                setText(R.id.txtSummaryOrders, String.valueOf(orders));
                setText(R.id.txtSummaryAvg, CurrencyUtils.vnd(avg));
                setText(R.id.txtSummaryVat, CurrencyUtils.vnd(vat));
            }
        } finally { c.close(); }
    }

    // ─── Revenue LineChart ────────────────────────────────────────────────────────

    private void loadRevenueChart(long from) {
        int days = (currentPeriod == PERIOD_MONTH) ? daysInCurrentMonth() : currentPeriod;
        long dayMs    = 86400000L;
        long midnight = getMidnightToday();

        long[] revenues = new long[days];
        String[] labels = new String[days];
        SimpleDateFormat fmt = new SimpleDateFormat("d/M", Locale.getDefault());

        for (int i = 0; i < days; i++) {
            long dayStart = midnight - (long)(days - 1 - i) * dayMs;
            labels[i] = fmt.format(new Date(dayStart));
        }

        // day_off = 0 means 'from' day, 1 means next day, etc.
        Cursor c = dbHelper.getReadableDatabase().rawQuery(
            "SELECT CAST((created_at - ?) / 86400000 AS INTEGER) day_off, SUM(total_amount) " +
            "FROM orders WHERE shop_id=? AND status='PAID' AND created_at >= ? " +
            "GROUP BY day_off ORDER BY day_off",
            new String[]{String.valueOf(from), String.valueOf(shopId), String.valueOf(from)});
        try {
            while (c.moveToNext()) {
                int off = c.getInt(0);
                if (off >= 0 && off < days) revenues[off] = c.getLong(1);
            }
        } finally { c.close(); }

        LineChartView chart = findViewById(R.id.revenueChart);
        chart.setData(revenues, labels);
    }

    // ─── Top Products (ranked list with progress bars) ────────────────────────────

    private void loadTopProducts(long from) {
        LinearLayout layout = findViewById(R.id.layoutTopProducts);
        layout.removeAllViews();

        Cursor c = dbHelper.getReadableDatabase().rawQuery(
            "SELECT oi.product_name, SUM(oi.quantity) qty, SUM(oi.line_total) rev " +
            "FROM order_items oi JOIN orders o ON oi.order_id = o.id " +
            "WHERE o.shop_id=? AND o.status='PAID' AND o.created_at >= ? " +
            "GROUP BY oi.product_id ORDER BY qty DESC LIMIT 5",
            new String[]{String.valueOf(shopId), String.valueOf(from)});

        // Collect rows first to get max for bar scaling
        String[] names = new String[5];
        long[]   qtys  = new long[5];
        long[]   revs  = new long[5];
        int count = 0;
        try {
            while (c.moveToNext() && count < 5) {
                names[count] = c.getString(0);
                qtys[count]  = c.getLong(1);
                revs[count]  = c.getLong(2);
                count++;
            }
        } finally { c.close(); }

        if (count == 0) { addEmptyRow(layout, "Chưa có dữ liệu bán hàng"); return; }

        long maxQty = qtys[0]; // already sorted DESC
        int blue = ContextCompat.getColor(this, R.color.blue_primary);

        for (int i = 0; i < count; i++) {
            // Row container
            LinearLayout row = new LinearLayout(this);
            row.setOrientation(LinearLayout.VERTICAL);
            LinearLayout.LayoutParams rowLp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
            rowLp.setMargins(0, 0, 0, dp(10));
            row.setLayoutParams(rowLp);

            // Name + qty line
            LinearLayout nameRow = new LinearLayout(this);
            nameRow.setOrientation(LinearLayout.HORIZONTAL);
            nameRow.setGravity(android.view.Gravity.CENTER_VERTICAL);

            // Rank badge
            TextView tvRank = new TextView(this);
            tvRank.setText(String.valueOf(i + 1));
            tvRank.setTextColor(i == 0 ? Color.WHITE : Color.parseColor("#64748B"));
            tvRank.setTextSize(10f);
            tvRank.setTypeface(null, Typeface.BOLD);
            tvRank.setGravity(android.view.Gravity.CENTER);
            int badgeSize = dp(20);
            LinearLayout.LayoutParams rankLp = new LinearLayout.LayoutParams(badgeSize, badgeSize);
            rankLp.setMarginEnd(dp(8));
            tvRank.setLayoutParams(rankLp);
            tvRank.setBackgroundResource(i == 0 ? R.drawable.bg_tab_selected : R.drawable.bg_tab_unselected);

            // Product name
            TextView tvName = new TextView(this);
            tvName.setText(names[i]);
            tvName.setTextColor(Color.parseColor("#1C2333"));
            tvName.setTextSize(13f);
            tvName.setTypeface(null, Typeface.BOLD);
            LinearLayout.LayoutParams nameLp = new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f);
            tvName.setLayoutParams(nameLp);

            // Qty + revenue
            LinearLayout rightCol = new LinearLayout(this);
            rightCol.setOrientation(LinearLayout.VERTICAL);
            rightCol.setGravity(android.view.Gravity.END);

            TextView tvQty = new TextView(this);
            tvQty.setText(qtys[i] + " sp");
            tvQty.setTextColor(blue);
            tvQty.setTextSize(13f);
            tvQty.setTypeface(null, Typeface.BOLD);
            tvQty.setGravity(android.view.Gravity.END);

            TextView tvRev = new TextView(this);
            tvRev.setText(CurrencyUtils.vnd(revs[i]));
            tvRev.setTextColor(Color.parseColor("#64748B"));
            tvRev.setTextSize(11f);
            tvRev.setGravity(android.view.Gravity.END);

            rightCol.addView(tvQty);
            rightCol.addView(tvRev);

            nameRow.addView(tvRank);
            nameRow.addView(tvName);
            nameRow.addView(rightCol);

            // Progress bar
            LinearLayout barBg = new LinearLayout(this);
            barBg.setOrientation(LinearLayout.HORIZONTAL);
            LinearLayout.LayoutParams barBgLp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, dp(6));
            barBgLp.setMargins(0, dp(5), 0, 0);
            barBg.setLayoutParams(barBgLp);
            barBg.setBackgroundColor(Color.parseColor("#F1F5F9"));

            // rounded corners by clipping would need a custom drawable; use a colored view for simplicity
            View barFill = new View(this);
            int fillW = maxQty > 0 ? (int)(((float) qtys[i] / maxQty) * getResources().getDisplayMetrics().widthPixels) : 0;
            LinearLayout.LayoutParams fillLp = new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.MATCH_PARENT, (float) qtys[i] / maxQty);
            barFill.setLayoutParams(fillLp);
            barFill.setBackgroundColor(i == 0 ? blue : Color.parseColor("#93C5FD"));

            View barRest = new View(this);
            barRest.setLayoutParams(new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.MATCH_PARENT, 1f - (float) qtys[i] / maxQty));

            barBg.addView(barFill);
            barBg.addView(barRest);

            row.addView(nameRow);
            row.addView(barBg);
            layout.addView(row);
        }
    }

    // ─── Recent Orders ────────────────────────────────────────────────────────────

    private void loadRecentOrders(long from) {
        LinearLayout layout = findViewById(R.id.layoutRecentOrders);
        layout.removeAllViews();

        Cursor c = dbHelper.getReadableDatabase().rawQuery(
            "SELECT order_code, total_amount, created_at FROM orders " +
            "WHERE shop_id=? AND status='PAID' AND created_at >= ? ORDER BY created_at DESC LIMIT 15",
            new String[]{String.valueOf(shopId), String.valueOf(from)});

        int count = 0;
        SimpleDateFormat fmt = new SimpleDateFormat("HH:mm dd/MM", Locale.getDefault());
        try {
            while (c.moveToNext()) {
                addOrderRow(layout, c.getString(0), fmt.format(new Date(c.getLong(2))), c.getLong(1), count % 2 == 1);
                count++;
            }
        } finally { c.close(); }

        if (count == 0) addEmptyRow(layout, "Không có đơn hàng trong kỳ này");
        setText(R.id.txtOrderCount, count + " đơn");
    }

    private void addOrderRow(LinearLayout parent, String code, String time, long amount, boolean zebra) {
        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(android.view.Gravity.CENTER_VERTICAL);
        row.setPadding(dp(16), dp(10), dp(16), dp(10));
        if (zebra) row.setBackgroundColor(Color.parseColor("#F8FAFC"));

        TextView tvCode = new TextView(this);
        tvCode.setText(code);
        tvCode.setTextColor(Color.parseColor("#1C2333"));
        tvCode.setTextSize(12f);
        tvCode.setTypeface(null, Typeface.BOLD);
        tvCode.setLayoutParams(new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 2f));

        TextView tvTime = new TextView(this);
        tvTime.setText(time);
        tvTime.setTextColor(Color.parseColor("#64748B"));
        tvTime.setTextSize(11f);
        tvTime.setLayoutParams(new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 2f));

        TextView tvAmt = new TextView(this);
        tvAmt.setText(CurrencyUtils.vnd(amount));
        tvAmt.setTextColor(ContextCompat.getColor(this, R.color.blue_primary));
        tvAmt.setTextSize(12f);
        tvAmt.setTypeface(null, Typeface.BOLD);
        tvAmt.setGravity(android.view.Gravity.END);
        tvAmt.setLayoutParams(new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 2f));

        row.addView(tvCode);
        row.addView(tvTime);
        row.addView(tvAmt);
        parent.addView(row);
    }

    // ─── Low Stock ────────────────────────────────────────────────────────────────

    private void loadLowStock() {
        LinearLayout layout = findViewById(R.id.layoutLowStock);
        layout.removeAllViews();

        Cursor c = dbHelper.getReadableDatabase().rawQuery(
            "SELECT name, stock_quantity, min_stock_quantity FROM products " +
            "WHERE is_active=1 AND shop_id=? AND stock_quantity <= min_stock_quantity ORDER BY stock_quantity ASC LIMIT 8",
            new String[]{String.valueOf(shopId)});

        int count = 0;
        try {
            while (c.moveToNext()) {
                addStockRow(layout, c.getString(0), c.getInt(1), c.getInt(2), count % 2 == 1);
                count++;
            }
        } finally { c.close(); }

        TextView badge = findViewById(R.id.txtLowStockBadge);
        if (count > 0) {
            badge.setText(count + " sp");
            badge.setVisibility(View.VISIBLE);
        } else {
            badge.setVisibility(View.GONE);
            addEmptyRow(layout, "Tất cả sản phẩm đủ tồn kho");
        }
    }

    private void addStockRow(LinearLayout parent, String name, int stock, int min, boolean zebra) {
        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(android.view.Gravity.CENTER_VERTICAL);
        row.setPadding(dp(16), dp(11), dp(16), dp(11));
        if (zebra) row.setBackgroundColor(Color.parseColor("#F8FAFC"));

        LinearLayout left = new LinearLayout(this);
        left.setOrientation(LinearLayout.VERTICAL);
        left.setLayoutParams(new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f));

        TextView tvName = new TextView(this);
        tvName.setText(name);
        tvName.setTextColor(Color.parseColor("#1C2333"));
        tvName.setTextSize(13f);

        TextView tvMin = new TextView(this);
        tvMin.setText("Tối thiểu: " + min);
        tvMin.setTextColor(Color.parseColor("#94A3B8"));
        tvMin.setTextSize(11f);

        left.addView(tvName);
        left.addView(tvMin);

        TextView tvStock = new TextView(this);
        tvStock.setText("Còn " + stock);
        tvStock.setTextColor(stock == 0 ? Color.parseColor("#EF4444") : Color.parseColor("#F97316"));
        tvStock.setTextSize(13f);
        tvStock.setTypeface(null, Typeface.BOLD);
        tvStock.setGravity(android.view.Gravity.END);

        row.addView(left);
        row.addView(tvStock);
        parent.addView(row);
    }

    private void addEmptyRow(LinearLayout parent, String msg) {
        TextView tv = new TextView(this);
        tv.setText(msg);
        tv.setGravity(android.view.Gravity.CENTER);
        tv.setPadding(0, dp(20), 0, dp(20));
        tv.setTextColor(Color.parseColor("#94A3B8"));
        parent.addView(tv);
    }

    // ─── Helpers ─────────────────────────────────────────────────────────────────

    private long getPeriodStart() {
        Calendar c = Calendar.getInstance();
        c.set(Calendar.HOUR_OF_DAY, 0); c.set(Calendar.MINUTE, 0);
        c.set(Calendar.SECOND, 0); c.set(Calendar.MILLISECOND, 0);
        if (currentPeriod == PERIOD_MONTH) {
            c.set(Calendar.DAY_OF_MONTH, 1);
        } else {
            c.add(Calendar.DAY_OF_YEAR, -(currentPeriod - 1));
        }
        return c.getTimeInMillis();
    }

    private long getMidnightToday() {
        Calendar c = Calendar.getInstance();
        c.set(Calendar.HOUR_OF_DAY, 0); c.set(Calendar.MINUTE, 0);
        c.set(Calendar.SECOND, 0); c.set(Calendar.MILLISECOND, 0);
        return c.getTimeInMillis();
    }

    private int daysInCurrentMonth() {
        return Calendar.getInstance().getActualMaximum(Calendar.DAY_OF_MONTH);
    }

    private void setText(int id, String text) { ((TextView) findViewById(id)).setText(text); }

    private int dp(int v) { return Math.round(v * getResources().getDisplayMetrics().density); }
}

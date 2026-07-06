package com.example.mpos.employee;

import android.content.ContentValues;
import android.database.Cursor;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.example.mpos.database.DatabaseHelper;
import com.example.mpos.utils.CurrencyUtils;

import java.util.Calendar;

public class EmployeeKpiActivity extends AppCompatActivity {

    private DatabaseHelper db;
    private LinearLayout layoutList;

    @Override
    protected void onCreate(Bundle state) {
        super.onCreate(state);

        db = new DatabaseHelper(this);

        // Build layout programmatically
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setLayoutParams(new LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
        root.setBackgroundColor(0xFFF5F8FC);

        // AppBar
        LinearLayout appBar = new LinearLayout(this);
        appBar.setOrientation(LinearLayout.HORIZONTAL);
        appBar.setGravity(Gravity.CENTER_VERTICAL);
        appBar.setBackgroundColor(0xFF1C2333);
        LinearLayout.LayoutParams barLp = new LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT, dp(56));
        appBar.setLayoutParams(barLp);
        appBar.setPadding(dp(4), 0, dp(16), 0);

        TextView btnBack = new TextView(this);
        btnBack.setText("←");
        btnBack.setTextColor(0xFFFFFFFF);
        btnBack.setTextSize(20f);
        btnBack.setGravity(Gravity.CENTER);
        btnBack.setLayoutParams(new LinearLayout.LayoutParams(dp(44), dp(44)));
        btnBack.setOnClickListener(v -> finish());
        appBar.addView(btnBack);

        TextView title = new TextView(this);
        title.setText("KPI & Lương nhân viên");
        title.setTextColor(0xFFFFFFFF);
        title.setTextSize(16f);
        title.setTypeface(null, Typeface.BOLD);
        title.setLayoutParams(new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
        title.setPadding(dp(8), 0, 0, 0);
        appBar.addView(title);
        root.addView(appBar);

        // Period label
        Calendar cal = Calendar.getInstance();
        String month = cal.get(Calendar.MONTH) + 1 + "/" + cal.get(Calendar.YEAR);
        TextView tvPeriod = new TextView(this);
        tvPeriod.setText("Thống kê tháng " + month);
        tvPeriod.setTextSize(13f);
        tvPeriod.setTextColor(0xFF64748B);
        tvPeriod.setPadding(dp(16), dp(12), dp(16), dp(4));
        root.addView(tvPeriod);

        // Scrollable list
        ScrollView scroll = new ScrollView(this);
        scroll.setLayoutParams(new LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f));
        layoutList = new LinearLayout(this);
        layoutList.setOrientation(LinearLayout.VERTICAL);
        layoutList.setPadding(dp(16), dp(8), dp(16), dp(32));
        scroll.addView(layoutList);
        root.addView(scroll);

        setContentView(root);
        loadKpi();
    }

    private void loadKpi() {
        layoutList.removeAllViews();

        // This month range
        Calendar cal = Calendar.getInstance();
        cal.set(Calendar.DAY_OF_MONTH, 1);
        cal.set(Calendar.HOUR_OF_DAY, 0); cal.set(Calendar.MINUTE, 0); cal.set(Calendar.SECOND, 0);
        long monthStart = cal.getTimeInMillis();
        long monthEnd   = System.currentTimeMillis();

        Cursor c = db.getReadableDatabase().rawQuery(
            "SELECT u.id, COALESCE(e.full_name, u.username) as name, u.role, " +
            "  COUNT(o.id) as order_count, " +
            "  COALESCE(SUM(o.total_amount), 0) as revenue " +
            "FROM users u " +
            "LEFT JOIN employees e ON u.employee_id = e.id " +
            "LEFT JOIN orders o ON o.user_id = u.id " +
            "  AND o.created_at BETWEEN ? AND ? " +
            "  AND o.status = 'PAID' " +
            "WHERE u.is_active = 1 " +
            "GROUP BY u.id, name, u.role " +
            "ORDER BY revenue DESC",
            new String[]{String.valueOf(monthStart), String.valueOf(monthEnd)});

        try {
            if (!c.moveToFirst()) {
                addEmpty();
                return;
            }
            do {
                long userId      = c.getLong(0);
                String name      = c.getString(1);
                String role      = c.getString(2);
                int orderCount   = c.getInt(3);
                long revenue     = c.getLong(4);
                addEmployeeCard(userId, name, role, orderCount, revenue);
            } while (c.moveToNext());
        } finally { c.close(); }
    }

    private void addEmployeeCard(long userId, String name, String role, int orders, long revenue) {
        // Fetch salary config
        Cursor sc = db.getReadableDatabase().rawQuery(
            "SELECT salary_type, base_amount, commission_percent FROM salary_configs WHERE user_id=?",
            new String[]{String.valueOf(userId)});
        String salaryType = "MONTHLY"; long baseAmt = 0; int commPct = 0;
        try {
            if (sc.moveToFirst()) {
                salaryType = sc.getString(0);
                baseAmt    = sc.getLong(1);
                commPct    = sc.getInt(2);
            }
        } finally { sc.close(); }

        long commission  = revenue * commPct / 100;
        long totalSalary = baseAmt + commission;

        LinearLayout card = new LinearLayout(this);
        card.setOrientation(LinearLayout.VERTICAL);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        lp.setMargins(0, 0, 0, dp(12));
        card.setLayoutParams(lp);
        card.setBackground(rounded(0xFFFFFFFF, 14));
        card.setElevation(dp(1));
        card.setPadding(dp(16), dp(16), dp(16), dp(16));

        // Top row: avatar + name + role
        LinearLayout topRow = new LinearLayout(this);
        topRow.setOrientation(LinearLayout.HORIZONTAL);
        topRow.setGravity(Gravity.CENTER_VERTICAL);

        LinearLayout avatar = new LinearLayout(this);
        avatar.setGravity(Gravity.CENTER);
        LinearLayout.LayoutParams avLp = new LinearLayout.LayoutParams(dp(42), dp(42));
        avLp.setMargins(0, 0, dp(12), 0);
        avatar.setLayoutParams(avLp);
        avatar.setBackground(rounded(roleColor(role, true), 21));
        TextView tvInit = new TextView(this);
        tvInit.setText(name.length() >= 2 ? name.substring(0, 2).toUpperCase() : name.toUpperCase());
        tvInit.setTextColor(0xFFFFFFFF);
        tvInit.setTextSize(15f);
        tvInit.setTypeface(null, Typeface.BOLD);
        avatar.addView(tvInit);
        topRow.addView(avatar);

        LinearLayout info = new LinearLayout(this);
        info.setOrientation(LinearLayout.VERTICAL);
        info.setLayoutParams(new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
        tvBold(info, name, 15, 0xFF1C2333);
        tvNormal(info, roleLabel(role), 12, roleColor(role, false));
        topRow.addView(info);

        TextView btnEdit = new TextView(this);
        btnEdit.setText("Cài lương");
        btnEdit.setTextSize(12f);
        btnEdit.setTypeface(null, Typeface.BOLD);
        btnEdit.setTextColor(0xFF2875FB);
        btnEdit.setBackground(rounded(0xFFEEF3FF, 16));
        btnEdit.setPadding(dp(12), dp(6), dp(12), dp(6));
        btnEdit.setClickable(true); btnEdit.setFocusable(true);
        final String sType = salaryType; final long sBase = baseAmt; final int sComm = commPct;
        btnEdit.setOnClickListener(v -> showSalaryDialog(userId, name, sType, sBase, sComm));
        topRow.addView(btnEdit);
        card.addView(topRow);

        // Divider
        View div = new View(this);
        LinearLayout.LayoutParams divLp = new LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT, 1);
        divLp.setMargins(0, dp(12), 0, dp(12));
        div.setLayoutParams(divLp);
        div.setBackgroundColor(0xFFF1F5F9);
        card.addView(div);

        // KPI stats row
        LinearLayout stats = new LinearLayout(this);
        stats.setOrientation(LinearLayout.HORIZONTAL);
        stats.setLayoutParams(new LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        addStat(stats, "Đơn hàng", String.valueOf(orders), 0xFF2875FB);
        addStat(stats, "Doanh thu", CurrencyUtils.vnd(revenue), 0xFF10B981);
        addStat(stats, "Tổng lương TT", CurrencyUtils.vnd(totalSalary), 0xFFF59E0B);
        card.addView(stats);

        // Salary breakdown
        LinearLayout breakdown = new LinearLayout(this);
        breakdown.setOrientation(LinearLayout.VERTICAL);
        LinearLayout.LayoutParams bkLp = new LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        bkLp.setMargins(0, dp(10), 0, 0);
        breakdown.setLayoutParams(bkLp);
        breakdown.setBackground(rounded(0xFFF8FAFC, 10));
        breakdown.setPadding(dp(12), dp(10), dp(12), dp(10));
        tvNormal(breakdown, "Lương cơ bản (" + ("HOURLY".equals(salaryType) ? "theo giờ" : "cố định") + "): " + CurrencyUtils.vnd(baseAmt), 12, 0xFF475569);
        tvNormal(breakdown, "Hoa hồng " + commPct + "% doanh thu: " + CurrencyUtils.vnd(commission), 12, 0xFF475569);
        card.addView(breakdown);

        layoutList.addView(card);
    }

    private void addStat(LinearLayout parent, String label, String value, int color) {
        LinearLayout col = new LinearLayout(this);
        col.setOrientation(LinearLayout.VERTICAL);
        col.setGravity(Gravity.CENTER);
        col.setLayoutParams(new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
        TextView tvVal = new TextView(this);
        tvVal.setText(value);
        tvVal.setTextSize(14f);
        tvVal.setTypeface(null, Typeface.BOLD);
        tvVal.setTextColor(color);
        tvVal.setGravity(Gravity.CENTER);
        col.addView(tvVal);
        TextView tvLabel = new TextView(this);
        tvLabel.setText(label);
        tvLabel.setTextSize(11f);
        tvLabel.setTextColor(0xFF94A3B8);
        tvLabel.setGravity(Gravity.CENTER);
        tvLabel.setPadding(0, dp(2), 0, 0);
        col.addView(tvLabel);
        parent.addView(col);
    }

    private void showSalaryDialog(long userId, String name, String curType, long curBase, int curComm) {
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(dp(24), dp(12), dp(24), dp(4));

        tvNormal(root, "Loại: cố định theo tháng + hoa hồng %\n(VD: lương 5.000.000đ + 2% doanh thu)", 12, 0xFF64748B)
            .setPadding(0, 0, 0, dp(12));

        EditText etBase = new EditText(this);
        etBase.setHint("Lương cơ bản (VNĐ/tháng)");
        etBase.setText(curBase > 0 ? String.valueOf(curBase) : "");
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        lp.setMargins(0, 0, 0, dp(10));
        etBase.setLayoutParams(lp);
        root.addView(etBase);

        EditText etComm = new EditText(this);
        etComm.setHint("Hoa hồng (%) doanh thu");
        etComm.setText(curComm > 0 ? String.valueOf(curComm) : "");
        etComm.setLayoutParams(lp);
        root.addView(etComm);

        new AlertDialog.Builder(this)
            .setTitle("Cài lương: " + name)
            .setView(root)
            .setPositiveButton("Lưu", (d, w) -> {
                long base = 0; int comm = 0;
                try { base = Long.parseLong(etBase.getText().toString().trim().replaceAll("[^0-9]", "")); } catch (Exception ignored) {}
                try { comm = Integer.parseInt(etComm.getText().toString().trim()); } catch (Exception ignored) {}
                saveSalaryConfig(userId, "MONTHLY", base, Math.min(comm, 100));
            })
            .setNegativeButton("Hủy", null)
            .show();
    }

    private void saveSalaryConfig(long userId, String type, long base, int comm) {
        ContentValues cv = new ContentValues();
        cv.put("user_id",            userId);
        cv.put("salary_type",        type);
        cv.put("base_amount",        base);
        cv.put("commission_percent", comm);
        cv.put("updated_at",         System.currentTimeMillis());
        int rows = db.getWritableDatabase().update("salary_configs", cv, "user_id=?", new String[]{String.valueOf(userId)});
        if (rows == 0) db.getWritableDatabase().insertOrThrow("salary_configs", null, cv);
        Toast.makeText(this, "Đã lưu cấu hình lương", Toast.LENGTH_SHORT).show();
        loadKpi();
    }

    private void addEmpty() {
        TextView tv = new TextView(this);
        tv.setText("Chưa có dữ liệu nhân viên");
        tv.setTextColor(0xFF94A3B8);
        tv.setTextSize(14f);
        tv.setGravity(Gravity.CENTER);
        tv.setPadding(0, dp(48), 0, 0);
        layoutList.addView(tv);
    }

    // ─── Helpers ──────────────────────────────────────────────────────────────

    private String roleLabel(String r) {
        if (r == null) return "Nhân viên";
        switch (r) { case "ADMIN": return "Admin"; case "MANAGER": return "Quản lý"; default: return "Nhân viên"; }
    }

    private int roleColor(String r, boolean bg) {
        if ("ADMIN".equals(r))   return bg ? 0xFFEF4444 : 0xFFEF4444;
        if ("MANAGER".equals(r)) return bg ? 0xFFF59E0B : 0xFFD97706;
        return bg ? 0xFF2875FB : 0xFF2875FB;
    }

    private GradientDrawable rounded(int color, int r) {
        GradientDrawable g = new GradientDrawable();
        g.setColor(color); g.setCornerRadius(dp(r)); return g;
    }

    private TextView tvBold(LinearLayout p, String txt, int sp, int color) {
        TextView tv = new TextView(this);
        tv.setText(txt); tv.setTextSize(sp); tv.setTextColor(color);
        tv.setTypeface(null, Typeface.BOLD);
        p.addView(tv); return tv;
    }

    private TextView tvNormal(LinearLayout p, String txt, int sp, int color) {
        TextView tv = new TextView(this);
        tv.setText(txt); tv.setTextSize(sp); tv.setTextColor(color);
        p.addView(tv); return tv;
    }

    private int dp(int dp) {
        return Math.round(dp * getResources().getDisplayMetrics().density);
    }
}

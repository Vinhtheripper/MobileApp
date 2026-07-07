package com.example.mpos.order;

import android.content.Intent;
import android.database.Cursor;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import com.example.mpos.R;
import com.example.mpos.database.DatabaseHelper;
import com.example.mpos.logistics.CreateShippingOrderActivity;
import com.example.mpos.receipt.ReceiptActivity;
import com.example.mpos.utils.CurrencyUtils;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class OrderDetailActivity extends AppCompatActivity {
    private long currentOrderId = -1;

    @Override
    public void onCreate(Bundle state) {
        super.onCreate(state);
        setContentView(R.layout.activity_order_detail);
        currentOrderId = getIntent().getLongExtra("order_id", -1);
        findViewById(R.id.btnBack).setOnClickListener(v -> finish());
        if (currentOrderId >= 0) loadOrder(currentOrderId);
        findViewById(R.id.btnShipOrder).setOnClickListener(v -> openCreateShipping());
        findViewById(R.id.btnViewReceipt).setOnClickListener(v -> openReceipt());
    }

    private void openReceipt() {
        if (currentOrderId < 0) return;
        Intent intent = new Intent(this, ReceiptActivity.class);
        intent.putExtra("order_id", currentOrderId);
        intent.putExtra("from_history", true);
        startActivity(intent);
    }

    private void openCreateShipping() {
        if (currentOrderId < 0) return;
        DatabaseHelper dbHelper = new DatabaseHelper(this);
        android.database.Cursor c = dbHelper.getReadableDatabase().rawQuery(
            "SELECT o.order_code, o.total_amount, " +
            "(SELECT COUNT(*) FROM order_items WHERE order_id=o.id) AS item_count " +
            "FROM orders o WHERE o.id=?",
            new String[]{String.valueOf(currentOrderId)});
        try {
            if (!c.moveToFirst()) return;
            String orderCode = c.getString(0);
            long   codAmount = c.getLong(1);
            int    itemCount = c.getInt(2);
            Intent intent = new Intent(this, CreateShippingOrderActivity.class);
            intent.putExtra("order_id",    currentOrderId);
            intent.putExtra("cod_amount",  codAmount);
            intent.putExtra("item_count",  itemCount);
            intent.putExtra("order_code",  orderCode);
            startActivity(intent);
        } finally { c.close(); dbHelper.close(); }
    }

    private void loadOrder(long orderId) {
        DatabaseHelper db = new DatabaseHelper(this);
        SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault());

        // Order header
        Cursor o = db.getReadableDatabase().rawQuery(
            "SELECT o.order_code, o.status, o.subtotal, o.vat_amount, o.total_amount, o.created_at, o.channel, " +
            "u.username FROM orders o LEFT JOIN users u ON u.id=o.user_id WHERE o.id=?",
            new String[]{String.valueOf(orderId)});
        try {
            if (!o.moveToFirst()) return;
            String code    = o.getString(0);
            String status  = o.getString(1);
            long subtotal  = o.getLong(2);
            long vatAmt    = o.getLong(3);
            long total     = o.getLong(4);
            long createdAt = o.getLong(5);
            String channel = o.getString(6);
            String staff   = o.getString(7);

            ((TextView) findViewById(R.id.txtDetailTitle)).setText(code);
            ((TextView) findViewById(R.id.txtDetailCode)).setText(code);
            ((TextView) findViewById(R.id.txtDetailDate)).setText(sdf.format(new Date(createdAt)));
            ((TextView) findViewById(R.id.txtDetailChannel)).setText(channelLabel(channel));
            ((TextView) findViewById(R.id.txtDetailStaff)).setText(staff != null ? staff : "—");
            ((TextView) findViewById(R.id.txtDetailSubtotal)).setText(CurrencyUtils.vnd(subtotal));
            ((TextView) findViewById(R.id.txtDetailTax)).setText(CurrencyUtils.vnd(vatAmt));
            ((TextView) findViewById(R.id.txtDetailTotal)).setText(CurrencyUtils.vnd(total));

            TextView txtStatus = findViewById(R.id.txtDetailStatus);
            txtStatus.setText(statusLabel(status));
            txtStatus.setBackgroundResource(statusBadge(status));
            txtStatus.setTextColor(ContextCompat.getColor(this, statusColor(status)));
        } finally { o.close(); }

        // Payment method
        Cursor pay = db.getReadableDatabase().rawQuery(
            "SELECT method FROM payments WHERE order_id=? LIMIT 1",
            new String[]{String.valueOf(orderId)});
        try {
            if (pay.moveToFirst()) {
                ((TextView) findViewById(R.id.txtDetailPayMethod)).setText(methodLabel(pay.getString(0)));
            }
        } finally { pay.close(); }

        // Items
        LinearLayout layout = findViewById(R.id.layoutItems);
        layout.removeAllViews();
        Cursor items = db.getReadableDatabase().rawQuery(
            "SELECT product_name, quantity, unit_price, line_total FROM order_items WHERE order_id=?",
            new String[]{String.valueOf(orderId)});
        try {
            while (items.moveToNext()) {
                String name  = items.getString(0);
                int qty      = items.getInt(1);
                long price   = items.getLong(2);
                long lineTot = items.getLong(3);
                addItemRow(layout, name, qty, price, lineTot);
            }
            if (items.getCount() == 0) {
                TextView empty = new TextView(this);
                empty.setText("Không có sản phẩm");
                empty.setGravity(Gravity.CENTER);
                empty.setPadding(0, 20, 0, 20);
                empty.setTextColor(getResources().getColor(R.color.text_secondary));
                layout.addView(empty);
            }
        } finally { items.close(); }
    }

    private void addItemRow(LinearLayout parent, String name, int qty, long price, long lineTotal) {
        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(android.view.Gravity.CENTER_VERTICAL);
        int pad = dp(12);
        row.setPadding(pad, dp(10), pad, dp(10));

        LinearLayout left = new LinearLayout(this);
        left.setOrientation(LinearLayout.VERTICAL);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f);
        left.setLayoutParams(lp);

        TextView tvName = new TextView(this);
        tvName.setText(name);
        tvName.setTextColor(getResources().getColor(R.color.text_primary));
        tvName.setTextSize(13f);
        left.addView(tvName);

        TextView tvQtyPrice = new TextView(this);
        tvQtyPrice.setText(qty + " × " + CurrencyUtils.vnd(price));
        tvQtyPrice.setTextColor(getResources().getColor(R.color.text_secondary));
        tvQtyPrice.setTextSize(12f);
        left.addView(tvQtyPrice);

        row.addView(left);

        TextView tvTotal = new TextView(this);
        tvTotal.setText(CurrencyUtils.vnd(lineTotal));
        tvTotal.setTextColor(getResources().getColor(R.color.blue_primary));
        tvTotal.setTextSize(14f);
        tvTotal.setTypeface(null, android.graphics.Typeface.BOLD);
        tvTotal.setGravity(android.view.Gravity.END);
        row.addView(tvTotal);

        parent.addView(row);

        // Divider (not after last — we won't bother checking)
        View div = new View(this);
        LinearLayout.LayoutParams dlp = new LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT, 1);
        dlp.setMarginStart(pad); dlp.setMarginEnd(pad);
        div.setLayoutParams(dlp);
        div.setBackgroundColor(getResources().getColor(R.color.border));
        parent.addView(div);
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
    private String methodLabel(String m) {
        if (m == null) return "—";
        switch (m) {
            case "CASH":    return "Tiền mặt";
            case "VIETQR":  return "Chuyển khoản VietQR";
            case "CARD":    return "Thẻ ngân hàng";
            case "EWALLET": return "Ví điện tử";
            default:        return m;
        }
    }
    private String channelLabel(String ch) {
        if (ch == null) return "Tại quầy";
        switch (ch) {
            case "WALK_IN":  return "🏪 Tại quầy";
            case "ORDER":    return "📱 Đặt qua MXH";
            case "FACEBOOK": return "📘 Facebook";
            case "ZALO":     return "💬 Zalo";
            case "SHOPEE":   return "🛍 Shopee";
            default:         return ch;
        }
    }

    private int dp(int dp) { return Math.round(dp * getResources().getDisplayMetrics().density); }
}

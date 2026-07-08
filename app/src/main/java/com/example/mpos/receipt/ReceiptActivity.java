package com.example.mpos.receipt;

import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.print.PrintAttributes;
import android.print.PrintManager;
import android.view.Gravity;
import android.view.View;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.mpos.R;
import com.example.mpos.dao.SettingsDao;
import com.example.mpos.database.DatabaseHelper;
import com.example.mpos.order.OrderListActivity;
import com.example.mpos.pos.PosActivity;
import com.example.mpos.utils.CurrencyUtils;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class ReceiptActivity extends AppCompatActivity {

    private long orderId;
    private String paymentMethod;
    private long receivedCash;
    private DatabaseHelper db;
    private SettingsDao settings;

    @Override
    public void onCreate(Bundle state) {
        super.onCreate(state);
        setContentView(R.layout.activity_receipt);

        db       = new DatabaseHelper(this);
        settings = new SettingsDao(db);

        orderId       = getIntent().getLongExtra("order_id", -1);
        paymentMethod = getIntent().getStringExtra("payment_method");
        receivedCash  = getIntent().getLongExtra("received_cash", 0);
        boolean fromHistory = getIntent().getBooleanExtra("from_history", false);

        loadReceipt(orderId, paymentMethod, receivedCash);

        View btnNewSale = findViewById(R.id.btnNewSale);
        if (fromHistory) {
            btnNewSale.setVisibility(View.GONE);
        } else {
            btnNewSale.setOnClickListener(v -> goToPos());
        }
        findViewById(R.id.btnPrint).setOnClickListener(v -> printReceipt());
        findViewById(R.id.btnShare).setOnClickListener(v -> shareReceipt());

        // Share row buttons
        findViewById(R.id.btnShareZalo).setOnClickListener(v -> shareViaZalo());
        findViewById(R.id.btnShareMessenger).setOnClickListener(v -> shareViaMessenger());
        findViewById(R.id.btnShareBluetooth).setOnClickListener(v -> shareViaBluetooth());
        findViewById(R.id.btnShareEmail).setOnClickListener(v -> shareViaEmail());
    }

    @Override
    public void onBackPressed() {
        if (getIntent().getBooleanExtra("from_history", false)) {
            finish();
        } else {
            goToPos();
        }
    }

    private void goToPos() {
        Intent i = new Intent(this, PosActivity.class);
        i.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
        startActivity(i);
        finish();
    }

    // ─── Load receipt ─────────────────────────────────────────────────────────

    private void loadReceipt(long orderId, String paymentMethod, long receivedCash) {
        TextView txtOrderCode    = findViewById(R.id.txtReceiptOrderCode);
        TextView txtDate         = findViewById(R.id.txtReceiptDate);
        LinearLayout layoutItems = findViewById(R.id.layoutReceiptItems);
        TextView txtSubtotal     = findViewById(R.id.txtReceiptSubtotal);
        TextView txtVat          = findViewById(R.id.txtReceiptVat);
        TextView txtPayMethod    = findViewById(R.id.txtReceiptPayMethod);
        TextView txtTotal        = findViewById(R.id.txtReceiptTotal);
        TextView txtHeroAmount   = findViewById(R.id.txtHeroAmount);
        TextView txtCustomer     = findViewById(R.id.txtReceiptCustomer);
        TextView txtStoreName    = findViewById(R.id.txtStoreName);
        TextView txtStoreAddress = findViewById(R.id.txtStoreAddress);

        // Store info
        txtStoreName.setText(settings.get("store_name", "Quầy mPOS Pro"));
        String address = settings.get("store_address", "");
        if (!address.isEmpty()) {
            txtStoreAddress.setText(address);
            txtStoreAddress.setVisibility(View.VISIBLE);
        }

        if (orderId < 0) { txtOrderCode.setText("Không tìm thấy đơn hàng"); return; }

        Cursor order = db.getReadableDatabase().rawQuery(
            "SELECT o.order_code, o.subtotal, o.vat_amount, o.total_amount, o.created_at, c.full_name, o.discount_amount " +
            "FROM orders o LEFT JOIN customers c ON o.customer_id=c.id WHERE o.id=?",
            new String[]{String.valueOf(orderId)});
        long subtotal = 0, vatAmt = 0, total = 0, discount = 0;
        try {
            if (!order.moveToFirst()) { txtOrderCode.setText("Không tìm thấy đơn hàng"); return; }
            String orderCode    = order.getString(0);
            subtotal            = order.getLong(1);
            vatAmt              = order.getLong(2);
            total               = order.getLong(3);
            long createdAt      = order.getLong(4);
            String customerName = order.getString(5);
            discount            = order.getLong(6);

            txtOrderCode.setText("Đơn hàng #" + orderCode);
            String dateStr = new SimpleDateFormat("dd/MM/yyyy  HH:mm", Locale.getDefault())
                .format(new Date(createdAt));
            txtDate.setText(dateStr);

            if (customerName != null && !customerName.isEmpty()) {
                txtCustomer.setText("Khách hàng: " + customerName);
                txtCustomer.setVisibility(View.VISIBLE);
            }
        } finally { order.close(); }

        Cursor items = db.getReadableDatabase().rawQuery(
            "SELECT product_name, quantity, unit_price, line_total FROM order_items WHERE order_id=?",
            new String[]{String.valueOf(orderId)});
        try {
            while (items.moveToNext()) {
                LinearLayout row = new LinearLayout(this);
                row.setOrientation(LinearLayout.HORIZONTAL);
                LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
                lp.setMargins(0, 0, 0, dp(6));
                row.setLayoutParams(lp);

                TextView tvName = new TextView(this);
                tvName.setLayoutParams(new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f));
                tvName.setText(items.getString(0) + "  ×" + items.getInt(1));
                tvName.setTextSize(13f);
                tvName.setTextColor(0xFF1C2333);

                TextView tvPrice = new TextView(this);
                tvPrice.setLayoutParams(new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT));
                tvPrice.setText(CurrencyUtils.vnd(items.getLong(3)));
                tvPrice.setTextSize(13f);
                tvPrice.setTextColor(0xFF1C2333);
                tvPrice.setGravity(Gravity.END);

                row.addView(tvName);
                row.addView(tvPrice);
                layoutItems.addView(row);
            }
        } finally { items.close(); }

        if (discount > 0) {
            LinearLayout discRow = new LinearLayout(this);
            discRow.setOrientation(LinearLayout.HORIZONTAL);
            LinearLayout.LayoutParams dlp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
            dlp.setMargins(0, dp(4), 0, 0);
            discRow.setLayoutParams(dlp);
            TextView tvDiscLabel = new TextView(this);
            tvDiscLabel.setLayoutParams(new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f));
            tvDiscLabel.setText("Giảm giá điểm thưởng");
            tvDiscLabel.setTextSize(13f);
            tvDiscLabel.setTextColor(0xFF10B981);
            TextView tvDiscAmt = new TextView(this);
            tvDiscAmt.setText("−" + CurrencyUtils.vnd(discount));
            tvDiscAmt.setTextSize(13f);
            tvDiscAmt.setTextColor(0xFF10B981);
            discRow.addView(tvDiscLabel);
            discRow.addView(tvDiscAmt);
            layoutItems.addView(discRow);
        }

        txtSubtotal.setText(CurrencyUtils.vnd(subtotal));
        txtVat.setText(CurrencyUtils.vnd(vatAmt));
        txtPayMethod.setText(methodDisplayName(paymentMethod));
        txtTotal.setText(CurrencyUtils.vnd(total));
        if (txtHeroAmount != null) txtHeroAmount.setText(CurrencyUtils.vnd(total));

        if ("CASH".equals(paymentMethod)) {
            View layoutCash = findViewById(R.id.layoutCashBreakdown);
            if (layoutCash != null) layoutCash.setVisibility(View.VISIBLE);
            TextView txtReceived = findViewById(R.id.txtReceiptReceived);
            TextView txtChange   = findViewById(R.id.txtReceiptChange);
            if (txtReceived != null) txtReceived.setText(CurrencyUtils.vnd(receivedCash));
            if (txtChange   != null) txtChange.setText(CurrencyUtils.vnd(Math.max(receivedCash - total, 0)));
        }
    }

    // ─── Share actions ────────────────────────────────────────────────────────

    private void shareViaZalo() {
        String text = buildTextReceipt();
        Intent intent = new Intent(Intent.ACTION_SEND);
        intent.setType("text/plain");
        intent.putExtra(Intent.EXTRA_TEXT, text);
        intent.setPackage("com.zing.zalo");
        try {
            startActivity(intent);
        } catch (Exception e) {
            // Zalo not installed — fallback to chooser
            shareReceipt();
        }
    }

    private void shareViaMessenger() {
        String text = buildTextReceipt();
        Intent intent = new Intent(Intent.ACTION_SEND);
        intent.setType("text/plain");
        intent.putExtra(Intent.EXTRA_TEXT, text);
        intent.setPackage("com.facebook.orca");
        try {
            startActivity(intent);
        } catch (Exception e) {
            shareReceipt();
        }
    }

    private void shareViaBluetooth() {
        String text = buildTextReceipt();
        Intent intent = new Intent(Intent.ACTION_SEND);
        intent.setType("text/plain");
        intent.putExtra(Intent.EXTRA_TEXT, text);
        intent.setPackage("com.android.bluetooth");
        try {
            startActivity(intent);
        } catch (Exception e) {
            // Fallback: open chooser filtered for Bluetooth
            Intent chooser = new Intent(Intent.ACTION_SEND);
            chooser.setType("text/plain");
            chooser.putExtra(Intent.EXTRA_TEXT, text);
            startActivity(Intent.createChooser(chooser, "Gửi qua Bluetooth..."));
        }
    }

    private void shareViaEmail() {
        String text = buildTextReceipt();
        Intent intent = new Intent(Intent.ACTION_SEND);
        intent.setType("message/rfc822");
        intent.putExtra(Intent.EXTRA_SUBJECT, "Hóa đơn mPOS #" + orderId);
        intent.putExtra(Intent.EXTRA_TEXT, text);
        try {
            startActivity(Intent.createChooser(intent, "Gửi email..."));
        } catch (Exception e) {
            Toast.makeText(this, "Không tìm thấy ứng dụng email", Toast.LENGTH_SHORT).show();
        }
    }

    private void shareReceipt() {
        Intent share = new Intent(Intent.ACTION_SEND);
        share.setType("text/plain");
        share.putExtra(Intent.EXTRA_TEXT, buildTextReceipt());
        share.putExtra(Intent.EXTRA_SUBJECT, "Hóa đơn mPOS #" + orderId);
        startActivity(Intent.createChooser(share, "Gửi hóa đơn qua..."));
    }

    // ─── Print ────────────────────────────────────────────────────────────────

    private void printReceipt() {
        String html = buildHtmlReceipt();
        WebView webView = new WebView(this);
        webView.setWebViewClient(new WebViewClient() {
            @Override
            public void onPageFinished(WebView view, String url) {
                PrintManager pm = (PrintManager) getSystemService(PRINT_SERVICE);
                String jobName = "HoaDon_" + orderId;
                if (pm != null)
                    pm.print(jobName, webView.createPrintDocumentAdapter(jobName),
                        new PrintAttributes.Builder()
                            .setMediaSize(PrintAttributes.MediaSize.ISO_A4)
                            .setResolution(new PrintAttributes.Resolution("pdf", "pdf", 600, 600))
                            .setMinMargins(PrintAttributes.Margins.NO_MARGINS)
                            .build());
            }
        });
        webView.loadDataWithBaseURL(null, html, "text/html", "UTF-8", null);
    }

    // ─── Build receipt content ────────────────────────────────────────────────

    private String buildTextReceipt() {
        String storeName = settings.get("store_name", "Cửa hàng của tôi");
        String address   = settings.get("store_address", "");
        String phone     = settings.get("store_phone", "");
        String header    = settings.get("receipt_header", "Cảm ơn quý khách!");
        String footer    = settings.get("receipt_footer", "Hẹn gặp lại!");

        Cursor order = db.getReadableDatabase().rawQuery(
            "SELECT order_code, subtotal, vat_amount, total_amount, created_at, discount_amount FROM orders WHERE id=?",
            new String[]{String.valueOf(orderId)});
        StringBuilder sb = new StringBuilder();
        sb.append("=================================\n");
        sb.append(storeName).append("\n");
        if (!address.isEmpty()) sb.append(address).append("\n");
        if (!phone.isEmpty()) sb.append("ĐT: ").append(phone).append("\n");
        sb.append(header).append("\n");
        sb.append("=================================\n");

        try {
            if (order.moveToFirst()) {
                sb.append("Đơn hàng: #").append(order.getString(0)).append("\n");
                sb.append(new SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault())
                    .format(new Date(order.getLong(4)))).append("\n");
                sb.append("---------------------------------\n");
                appendItemsText(sb);
                sb.append("---------------------------------\n");
                sb.append("Tạm tính:  ").append(CurrencyUtils.vnd(order.getLong(1))).append("\n");
                sb.append("Thuế:      ").append(CurrencyUtils.vnd(order.getLong(2))).append("\n");
                long disc = order.getLong(5);
                if (disc > 0) sb.append("Giảm giá:  -").append(CurrencyUtils.vnd(disc)).append("\n");
                sb.append("TỔNG:      ").append(CurrencyUtils.vnd(order.getLong(3))).append("\n");
                sb.append("T/T:       ").append(methodDisplayName(paymentMethod)).append("\n");
                if ("CASH".equals(paymentMethod)) {
                    sb.append("Đưa:       ").append(CurrencyUtils.vnd(receivedCash)).append("\n");
                    sb.append("Thừa:      ").append(CurrencyUtils.vnd(receivedCash - order.getLong(3))).append("\n");
                }
            }
        } finally { order.close(); }
        sb.append("=================================\n");
        sb.append(footer).append("\n");
        return sb.toString();
    }

    private void appendItemsText(StringBuilder sb) {
        Cursor c = db.getReadableDatabase().rawQuery(
            "SELECT product_name, quantity, line_total FROM order_items WHERE order_id=?",
            new String[]{String.valueOf(orderId)});
        try {
            while (c.moveToNext())
                sb.append(c.getString(0)).append(" x").append(c.getInt(1))
                  .append("  ").append(CurrencyUtils.vnd(c.getLong(2))).append("\n");
        } finally { c.close(); }
    }

    private String buildHtmlReceipt() {
        String storeName = settings.get("store_name", "Cửa hàng của tôi");
        String address   = settings.get("store_address", "");
        String phone     = settings.get("store_phone", "");
        String taxCode   = settings.get("tax_code", "");
        String header    = settings.get("receipt_header", "Cảm ơn quý khách!");
        String footer    = settings.get("receipt_footer", "Hẹn gặp lại!");

        StringBuilder items = new StringBuilder();
        Cursor c = db.getReadableDatabase().rawQuery(
            "SELECT product_name, quantity, unit_price, line_total FROM order_items WHERE order_id=?",
            new String[]{String.valueOf(orderId)});
        try {
            while (c.moveToNext())
                items.append("<tr><td>").append(c.getString(0)).append(" x").append(c.getInt(1))
                     .append("</td><td align='right'>")
                     .append(CurrencyUtils.vnd(c.getLong(2))).append("</td><td align='right'>")
                     .append(CurrencyUtils.vnd(c.getLong(3))).append("</td></tr>");
        } finally { c.close(); }

        Cursor order = db.getReadableDatabase().rawQuery(
            "SELECT order_code, subtotal, vat_amount, total_amount, created_at, discount_amount FROM orders WHERE id=?",
            new String[]{String.valueOf(orderId)});
        String code = ""; long sub = 0, vat = 0, total = 0, disc = 0; String dateStr = "";
        try {
            if (order.moveToFirst()) {
                code    = order.getString(0);
                sub     = order.getLong(1);
                vat     = order.getLong(2);
                total   = order.getLong(3);
                disc    = order.getLong(5);
                dateStr = new SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault()).format(new Date(order.getLong(4)));
            }
        } finally { order.close(); }

        String discRow = disc > 0
            ? "<tr><td><b>Giảm giá</b></td><td align='right' colspan='2' style='color:green'>−" + CurrencyUtils.vnd(disc) + "</td></tr>"
            : "";
        String cashRows = "CASH".equals(paymentMethod)
            ? "<tr><td>Tiền khách đưa</td><td align='right' colspan='2'>" + CurrencyUtils.vnd(receivedCash) + "</td></tr>"
            + "<tr><td>Tiền thừa</td><td align='right' colspan='2'>" + CurrencyUtils.vnd(receivedCash - total) + "</td></tr>"
            : "";

        return "<!DOCTYPE html><html><head><meta charset='UTF-8'><style>"
            + "body{font-family:monospace;font-size:12px;padding:20px;max-width:400px;margin:auto}"
            + "h2{text-align:center;margin:0;font-size:16px}p{text-align:center;margin:4px 0;font-size:11px}"
            + "table{width:100%;border-collapse:collapse}td{padding:4px 6px}"
            + ".total{font-size:15px;font-weight:bold;background:#1C2333;color:white}"
            + "hr{border:1px dashed #CBD5E1;margin:8px 0}"
            + "th{text-align:left;border-bottom:1px solid #CBD5E1;padding-bottom:4px;font-size:11px;color:#64748B}"
            + "</style></head><body>"
            + "<h2>" + storeName + "</h2>"
            + (!address.isEmpty() ? "<p>" + address + "</p>" : "")
            + (!phone.isEmpty() ? "<p>ĐT: " + phone + "</p>" : "")
            + (!taxCode.isEmpty() ? "<p>MST: " + taxCode + "</p>" : "")
            + "<p><i>" + header + "</i></p>"
            + "<hr><p><b>#" + code + "</b> &nbsp; " + dateStr + "</p><hr>"
            + "<table>"
            + "<tr><th>Sản phẩm</th><th align='right'>Đ.giá</th><th align='right'>T.tiền</th></tr>"
            + items
            + "<tr><td colspan='3'><hr></td></tr>"
            + "<tr><td>Tạm tính</td><td align='right' colspan='2'>" + CurrencyUtils.vnd(sub) + "</td></tr>"
            + "<tr><td>Thuế VAT</td><td align='right' colspan='2'>" + CurrencyUtils.vnd(vat) + "</td></tr>"
            + discRow
            + "<tr class='total'><td colspan='3' style='padding:6px'>"
            + "<table width='100%'><tr><td><b>TỔNG CỘNG</b></td><td align='right'><b>" + CurrencyUtils.vnd(total) + "</b></td></tr></table>"
            + "</td></tr>"
            + "<tr><td>Thanh toán</td><td align='right' colspan='2'>" + methodDisplayName(paymentMethod) + "</td></tr>"
            + cashRows
            + "</table><hr>"
            + "<p style='text-align:center'>" + footer + "</p>"
            + "</body></html>";
    }

    private String methodDisplayName(String method) {
        if (method == null) return "—";
        switch (method) {
            case "CASH":         return "Tiền mặt";
            case "VIETQR":       return "Chuyển khoản VietQR";
            case "CARD":         return "Thẻ ngân hàng";
            case "MOMO":         return "Ví MoMo";
            case "ZALOPAY":      return "ZaloPay";
            case "VIETTELMONEY": return "Viettel Money";
            case "EWALLET":      return "Ví điện tử";
            default:             return method;
        }
    }

    private int dp(int dp) {
        return Math.round(dp * getResources().getDisplayMetrics().density);
    }
}

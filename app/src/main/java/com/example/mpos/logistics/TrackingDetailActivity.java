package com.example.mpos.logistics;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.BitmapFactory;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.mpos.R;
import com.example.mpos.utils.CurrencyUtils;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.security.cert.X509Certificate;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;

public class TrackingDetailActivity extends AppCompatActivity {

    private static final String GHN_TRACK_URL  = "https://dev-online-gateway.ghn.vn/shiip/public-api/v2/shipping-order/detail";
    private static final String GHTK_TRACK_URL = "https://services.giaohangtietkiem.vn/services/shipment/v2/";
    private static final String PREFS_SHIP     = "mpos_shipping_prefs";

    private String trackingCode, carrier, recipientName, recipientPhone, recipientAddress;
    private long shippingFee, codAmount;

    private TextView tvStatus, tvEta;
    private LinearLayout layoutTimeline, layoutShipper;
    private ProgressBar progressBar;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        trackingCode     = getIntent().getStringExtra("tracking_code");
        carrier          = getIntent().getStringExtra("carrier");
        recipientName    = getIntent().getStringExtra("recipient_name");
        recipientPhone   = getIntent().getStringExtra("recipient_phone");
        recipientAddress = getIntent().getStringExtra("recipient_address");
        shippingFee      = getIntent().getLongExtra("shipping_fee", 0);
        codAmount        = getIntent().getLongExtra("cod_amount", 0);
        buildUI();
        fetchTracking();
    }

    // ─── BUILD UI ─────────────────────────────────────────────────────────────

    private void buildUI() {
        android.widget.ScrollView sv = new android.widget.ScrollView(this);
        sv.setFillViewport(true);
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setBackgroundColor(0xFFF1F5F9);
        sv.addView(root);

        // ── HEADER CARD ──────────────────────────────────────────────────────
        LinearLayout header = new LinearLayout(this);
        header.setOrientation(LinearLayout.VERTICAL);
        header.setBackgroundColor(0xFF0B1D35);
        header.setPadding(dp(16), dp(48), dp(16), dp(20));
        LinearLayout.LayoutParams hlp = lp(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        header.setLayoutParams(hlp);

        // Top row: back + title + refresh
        LinearLayout appBar = new LinearLayout(this);
        appBar.setOrientation(LinearLayout.HORIZONTAL);
        appBar.setGravity(Gravity.CENTER_VERTICAL);
        LinearLayout.LayoutParams ablp = lp(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        ablp.setMargins(dp(-16), dp(-48), dp(-16), dp(12));
        ablp.topMargin = 0;
        // use padding approach instead
        appBar.setPadding(0, 0, 0, dp(12));

        TextView btnBack = tv("←", 22f, Typeface.NORMAL, 0xFFFFFFFF);
        btnBack.setPadding(0, 0, dp(16), 0);
        btnBack.setOnClickListener(v -> finish());
        appBar.addView(btnBack);

        TextView tvTitle = tv("Tra cứu vận đơn", 16f, Typeface.BOLD, 0xFFFFFFFF);
        tvTitle.setLayoutParams(new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
        appBar.addView(tvTitle);

        progressBar = new ProgressBar(this);
        progressBar.setVisibility(View.GONE);
        LinearLayout.LayoutParams pbLp = new LinearLayout.LayoutParams(dp(24), dp(24));
        pbLp.gravity = Gravity.CENTER_VERTICAL;
        progressBar.setLayoutParams(pbLp);
        appBar.addView(progressBar);

        TextView btnRefresh = tv("↺", 20f, Typeface.NORMAL, 0xFF93C5FD);
        btnRefresh.setPadding(dp(8), 0, 0, 0);
        btnRefresh.setOnClickListener(v -> fetchTracking());
        appBar.addView(btnRefresh);
        header.addView(appBar);

        // Carrier row: logo + name/service + status badge
        LinearLayout carrierRow = new LinearLayout(this);
        carrierRow.setOrientation(LinearLayout.HORIZONTAL);
        carrierRow.setGravity(Gravity.CENTER_VERTICAL);
        carrierRow.setLayoutParams(lp(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));

        // Logo
        ImageView logo = new ImageView(this);
        LinearLayout.LayoutParams logoLp = new LinearLayout.LayoutParams(dp(52), dp(52));
        logoLp.setMargins(0, 0, dp(12), 0);
        logo.setLayoutParams(logoLp);
        logo.setScaleType(ImageView.ScaleType.FIT_CENTER);
        GradientDrawable logoBg = new GradientDrawable();
        logoBg.setCornerRadius(dp(10));
        logoBg.setColor("GHN".equals(carrier) ? 0xFFFF6200 : 0xFF00B14F);
        logo.setBackground(logoBg);
        try {
            int res = "GHN".equals(carrier) ? R.drawable.logo_ghn : R.drawable.logo_ghtk;
            logo.setImageBitmap(BitmapFactory.decodeResource(getResources(), res));
        } catch (Exception ignored) {}
        carrierRow.addView(logo);

        // Carrier info
        LinearLayout carrierInfo = new LinearLayout(this);
        carrierInfo.setOrientation(LinearLayout.VERTICAL);
        carrierInfo.setLayoutParams(new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
        TextView tvCarrier = tv(carrierFullName(carrier), 15f, Typeface.BOLD, 0xFFFFFFFF);
        String serviceText = "Giao hàng tiêu chuẩn" + (codAmount > 0 ? " · COD " + CurrencyUtils.vnd(codAmount) : "");
        TextView tvService = tv(serviceText, 11f, Typeface.NORMAL, 0xFF93C5FD);
        carrierInfo.addView(tvCarrier);
        carrierInfo.addView(tvService);
        carrierRow.addView(carrierInfo);

        // Status badge
        tvStatus = new TextView(this);
        tvStatus.setText("Đang xử lý");
        tvStatus.setTextSize(11f);
        tvStatus.setTypeface(null, Typeface.BOLD);
        tvStatus.setTextColor(0xFF0B1D35);
        tvStatus.setBackgroundColor(0xFFFFFFFF);
        tvStatus.setPadding(dp(10), dp(5), dp(10), dp(5));
        GradientDrawable statusBg = new GradientDrawable();
        statusBg.setColor(0xFFFFFFFF);
        statusBg.setCornerRadius(dp(20));
        tvStatus.setBackground(statusBg);
        carrierRow.addView(tvStatus);
        header.addView(carrierRow);

        // Delivery progress steps (4-step bar)
        LinearLayout.LayoutParams progBarLp = lp(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        progBarLp.setMargins(0, dp(16), 0, 0);
        LinearLayout stepsBar = buildProgressSteps();
        stepsBar.setLayoutParams(progBarLp);
        header.addView(stepsBar);

        // Tracking code + copy row
        LinearLayout.LayoutParams codeRowLp = lp(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        codeRowLp.setMargins(0, dp(14), 0, 0);

        LinearLayout codeBox = new LinearLayout(this);
        codeBox.setOrientation(LinearLayout.VERTICAL);
        codeBox.setPadding(dp(14), dp(12), dp(14), dp(12));
        GradientDrawable codeBoxBg = new GradientDrawable();
        codeBoxBg.setColor(0xFF162840);
        codeBoxBg.setCornerRadius(dp(10));
        codeBox.setBackground(codeBoxBg);
        codeBox.setLayoutParams(codeRowLp);

        TextView tvCodeLabel = tv("MÃ VẬN ĐƠN", 10f, Typeface.BOLD, 0xFF64B5F6);
        codeBox.addView(tvCodeLabel);

        LinearLayout codeInnerRow = new LinearLayout(this);
        codeInnerRow.setOrientation(LinearLayout.HORIZONTAL);
        codeInnerRow.setGravity(Gravity.CENTER_VERTICAL);
        LinearLayout.LayoutParams cirLp = lp(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        cirLp.setMargins(0, dp(4), 0, 0);
        codeInnerRow.setLayoutParams(cirLp);

        TextView tvCode = tv(trackingCode != null ? trackingCode : "—", 18f, Typeface.BOLD, 0xFFFFFFFF);
        tvCode.setLayoutParams(new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
        codeInnerRow.addView(tvCode);

        TextView btnCopy = new TextView(this);
        btnCopy.setText("Sao chép");
        btnCopy.setTextSize(12f);
        btnCopy.setTypeface(null, Typeface.BOLD);
        btnCopy.setTextColor(0xFF1A73E8);
        GradientDrawable copyBg = new GradientDrawable();
        copyBg.setColor(0xFFFFFFFF);
        copyBg.setCornerRadius(dp(8));
        btnCopy.setBackground(copyBg);
        btnCopy.setPadding(dp(14), dp(8), dp(14), dp(8));
        btnCopy.setOnClickListener(v -> {
            ClipboardManager cm = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
            if (cm != null) cm.setPrimaryClip(ClipData.newPlainText("tracking", trackingCode));
            Toast.makeText(this, "Đã sao chép mã vận đơn", Toast.LENGTH_SHORT).show();
        });
        codeInnerRow.addView(btnCopy);
        codeBox.addView(codeInnerRow);

        // ETA line
        tvEta = tv("", 12f, Typeface.NORMAL, 0xFF93C5FD);
        LinearLayout.LayoutParams etaLp = lp(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        etaLp.setMargins(0, dp(8), 0, 0);
        tvEta.setLayoutParams(etaLp);
        tvEta.setVisibility(View.GONE);
        codeBox.addView(tvEta);
        header.addView(codeBox);

        root.addView(header);

        // ── NGƯỜI NHẬN ────────────────────────────────────────────────────────
        root.addView(sectionLabel("NGƯỜI NHẬN"));
        LinearLayout recipCard = whiteCard(root);
        if (recipientName != null && !recipientName.isEmpty())
            addRecipRow(recipCard, "👤", recipientName, null, true);
        if (recipientPhone != null && !recipientPhone.isEmpty())
            addRecipRow(recipCard, "📞", recipientPhone, recipientPhone, false);
        if (recipientAddress != null && !recipientAddress.isEmpty())
            addRecipRow(recipCard, "📍", recipientAddress, null, false);
        if (shippingFee > 0) {
            View div = divider(); recipCard.addView(div);
            LinearLayout feeRow = new LinearLayout(this);
            feeRow.setOrientation(LinearLayout.HORIZONTAL);
            feeRow.setGravity(Gravity.CENTER_VERTICAL);
            LinearLayout.LayoutParams frLp = lp(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            frLp.setMargins(0, dp(8), 0, 0);
            feeRow.setLayoutParams(frLp);
            TextView tvFeeL = tv("Phí vận chuyển", 12f, Typeface.NORMAL, 0xFF9CA3AF);
            tvFeeL.setLayoutParams(new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
            feeRow.addView(tvFeeL);
            feeRow.addView(tv(CurrencyUtils.vnd(shippingFee), 14f, Typeface.BOLD, 0xFF1A73E8));
            recipCard.addView(feeRow);
        }

        // ── HÀNH TRÌNH ────────────────────────────────────────────────────────
        root.addView(sectionLabel("HÀNH TRÌNH ĐƠN HÀNG"));
        LinearLayout timelineCard = whiteCard(root);
        layoutTimeline = timelineCard;
        addTimelineEntry(timelineCard, "⏳", 0xFF9CA3AF, "Hệ thống đang xử lý", now(), "", true, false);

        // ── NHÂN VIÊN GIAO HÀNG ───────────────────────────────────────────────
        root.addView(sectionLabel("NHÂN VIÊN GIAO HÀNG"));
        LinearLayout shipperCard = whiteCard(root);
        layoutShipper = shipperCard;
        TextView loadingShipper = tv("Thông tin shipper sẽ cập nhật sau khi lấy hàng", 12f, Typeface.NORMAL, 0xFF9CA3AF);
        shipperCard.addView(loadingShipper);

        root.addView(spacer(dp(40)));
        setContentView(sv);
    }

    // ─── Progress steps bar ───────────────────────────────────────────────────

    private LinearLayout buildProgressSteps() {
        LinearLayout bar = new LinearLayout(this);
        bar.setOrientation(LinearLayout.HORIZONTAL);
        bar.setGravity(Gravity.CENTER_VERTICAL);

        String[] labels = {"Lấy hàng", "Kho GHN", "Đang giao", "Đã giao"};
        if ("GHTK".equals(carrier)) labels = new String[]{"Lấy hàng", "Phân loại", "Đang giao", "Đã giao"};

        for (int i = 0; i < labels.length; i++) {
            boolean done = false; // will be updated by renderTrackInfo
            LinearLayout step = new LinearLayout(this);
            step.setOrientation(LinearLayout.VERTICAL);
            step.setGravity(Gravity.CENTER_HORIZONTAL);
            step.setLayoutParams(new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));

            // Dot
            TextView dot = new TextView(this);
            dot.setText(done ? "●" : "○");
            dot.setTextSize(8f);
            dot.setTextColor(done ? 0xFF10B981 : 0xFF4B6080);
            dot.setGravity(Gravity.CENTER);
            LinearLayout.LayoutParams dotLp = new LinearLayout.LayoutParams(dp(10), dp(10));
            dotLp.gravity = Gravity.CENTER_HORIZONTAL;
            dot.setLayoutParams(dotLp);
            step.addView(dot);

            // Label
            TextView lbl = tv(labels[i], 9f, Typeface.NORMAL, done ? 0xFFFFFFFF : 0xFF5D7A99);
            lbl.setGravity(Gravity.CENTER);
            LinearLayout.LayoutParams lblLp = lp(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            lblLp.setMargins(0, dp(3), 0, 0);
            lblLp.gravity = Gravity.CENTER_HORIZONTAL;
            lbl.setLayoutParams(lblLp);
            step.addView(lbl);
            bar.addView(step);

            if (i < labels.length - 1) {
                View line = new View(this);
                LinearLayout.LayoutParams lineLp = new LinearLayout.LayoutParams(0, dp(1), 0.3f);
                lineLp.gravity = Gravity.CENTER_VERTICAL;
                line.setLayoutParams(lineLp);
                line.setBackgroundColor(done ? 0xFF10B981 : 0xFF2D4A6B);
                bar.addView(line);
            }
        }
        return bar;
    }

    // ─── Fetch tracking ───────────────────────────────────────────────────────

    private void fetchTracking() {
        if (trackingCode == null) return;
        progressBar.setVisibility(View.VISIBLE);

        SharedPreferences prefs = getSharedPreferences(PREFS_SHIP, Context.MODE_PRIVATE);
        String ghnToken  = prefs.getString("ghn_token", "");
        String ghtkToken = prefs.getString("ghtk_token", "");

        new Thread(() -> {
            TrackInfo info;
            if ("GHN".equals(carrier) && !ghnToken.isEmpty()) {
                info = trackGhn(ghnToken, trackingCode);
            } else if ("GHTK".equals(carrier) && !ghtkToken.isEmpty()) {
                info = trackGhtk(ghtkToken, trackingCode);
            } else {
                info = defaultWaitingInfo();
            }
            final TrackInfo fi = info;
            runOnUiThread(() -> {
                progressBar.setVisibility(View.GONE);
                renderTrackInfo(fi);
            });
        }).start();
    }

    private TrackInfo trackGhn(String token, String code) {
        try {
            JSONObject body = new JSONObject();
            body.put("order_code", code);
            String resp = post(GHN_TRACK_URL, body.toString(), "Token", token);
            JSONObject data = new JSONObject(resp).getJSONObject("data");

            String rawStatus    = data.optString("status", "");
            String status       = statusLabel(rawStatus);
            String shipperName  = data.optString("shipper_name", "");
            String shipperPhone = data.optString("shipper_phone", "");
            String leadTime     = data.optString("leadtime", "");

            List<TimelineEvent> tl = new ArrayList<>();
            JSONArray logs = data.optJSONArray("log");
            if (logs != null) {
                for (int i = logs.length() - 1; i >= 0; i--) {
                    JSONObject log = logs.getJSONObject(i);
                    String s  = statusLabel(log.optString("status", ""));
                    String ts = formatTs(log.optLong("updated_date", 0));
                    tl.add(new TimelineEvent(iconForStatus(log.optString("status","")), colorForStatus(log.optString("status","")),
                            s, ts, "", i == 0));
                }
            }
            if (tl.isEmpty()) tl.add(new TimelineEvent("📦", 0xFF1A73E8, status, now(), "Đơn hàng đã được tiếp nhận", true));
            return new TrackInfo(status, leadTime, shipperName, shipperPhone, tl, true);
        } catch (Exception e) {
            android.util.Log.e("TrackingDetail", "GHN: " + e.getMessage());
            return defaultWaitingInfo();
        }
    }

    private TrackInfo trackGhtk(String token, String code) {
        try {
            URL url = new URL(GHTK_TRACK_URL + code);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            applyTrustAll(conn);
            conn.setRequestProperty("Token", token);
            conn.setConnectTimeout(15000); conn.setReadTimeout(15000);
            StringBuilder sb = new StringBuilder();
            try (BufferedReader br = new BufferedReader(new InputStreamReader(conn.getInputStream()))) {
                String line; while ((line = br.readLine()) != null) sb.append(line);
            }
            JSONObject json = new JSONObject(sb.toString());
            JSONObject data = json.getJSONObject("shipment_tracking");
            String status = data.optString("status_text", "Đang xử lý");

            List<TimelineEvent> tl = new ArrayList<>();
            JSONArray logs = data.optJSONArray("logs");
            if (logs != null) {
                for (int i = 0; i < logs.length(); i++) {
                    JSONObject log = logs.getJSONObject(i);
                    String action = log.optString("action", "");
                    String ts = formatTs(log.optLong("created_at", 0));
                    tl.add(new TimelineEvent("📦", 0xFF1A73E8, action, ts, "", i == logs.length() - 1));
                }
            }
            if (tl.isEmpty()) tl.add(new TimelineEvent("📦", 0xFF1A73E8, status, now(), "", true));
            return new TrackInfo(status, "—", "", "", tl, true);
        } catch (Exception e) {
            return defaultWaitingInfo();
        }
    }

    private TrackInfo defaultWaitingInfo() {
        List<TimelineEvent> tl = new ArrayList<>();
        tl.add(new TimelineEvent("⏳", 0xFF6B7280, "Hệ thống đang xử lý", now(), "Đơn hàng đang chờ được xác nhận", true));
        return new TrackInfo("Đang xử lý", "", "", "", tl, false);
    }

    // ─── Render ───────────────────────────────────────────────────────────────

    private void renderTrackInfo(TrackInfo info) {
        // Status badge
        tvStatus.setText(statusEmoji(info.status) + "  " + info.status);
        int bgColor = statusBgColor(info.status);
        int fgColor = statusTextColor(info.status);
        GradientDrawable bg = new GradientDrawable();
        bg.setColor(bgColor);
        bg.setCornerRadius(dp(20));
        tvStatus.setBackground(bg);
        tvStatus.setTextColor(fgColor);

        // ETA
        if (info.eta != null && !info.eta.isEmpty() && !"—".equals(info.eta)) {
            tvEta.setText("🕐  Dự kiến giao: " + info.eta);
            tvEta.setVisibility(View.VISIBLE);
        }

        // Timeline
        layoutTimeline.removeAllViews();
        if (info.timeline.isEmpty()) {
            layoutTimeline.addView(tv("Hệ thống đang xử lý", 13f, Typeface.NORMAL, 0xFF9CA3AF));
        } else {
            for (int i = 0; i < info.timeline.size(); i++) {
                TimelineEvent ev = info.timeline.get(i);
                addTimelineEntry(layoutTimeline, ev.icon, ev.color, ev.label, ev.time, ev.desc, ev.isCurrent, i == info.timeline.size() - 1);
            }
        }

        // Shipper
        layoutShipper.removeAllViews();
        if (info.shipperName != null && !info.shipperName.isEmpty()) {
            addShipperCard(info.shipperName, info.shipperPhone);
        } else {
            String msg = info.isReal ? "Chưa được phân công shipper" : "Hệ thống đang xử lý đơn hàng";
            layoutShipper.addView(tv(msg, 12f, Typeface.NORMAL, 0xFF9CA3AF));
        }
    }

    // ─── Timeline row ─────────────────────────────────────────────────────────

    private void addTimelineEntry(LinearLayout parent, String icon, int color,
                                   String label, String time, String desc,
                                   boolean isCurrent, boolean isLast) {
        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        LinearLayout.LayoutParams lp = lp(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        lp.setMargins(0, 0, 0, isLast ? 0 : dp(2));
        row.setLayoutParams(lp);

        // Left column: icon box + vertical line
        LinearLayout leftCol = new LinearLayout(this);
        leftCol.setOrientation(LinearLayout.VERTICAL);
        leftCol.setGravity(Gravity.CENTER_HORIZONTAL);
        leftCol.setLayoutParams(new LinearLayout.LayoutParams(dp(36), ViewGroup.LayoutParams.WRAP_CONTENT));

        TextView iconBox = new TextView(this);
        iconBox.setText(icon);
        iconBox.setTextSize(13f);
        iconBox.setGravity(Gravity.CENTER);
        GradientDrawable iconBg = new GradientDrawable();
        iconBg.setShape(GradientDrawable.RECTANGLE);
        iconBg.setColor(isCurrent ? color : 0xFFF3F4F6);
        iconBg.setCornerRadius(dp(8));
        iconBox.setBackground(iconBg);
        LinearLayout.LayoutParams ibLp = new LinearLayout.LayoutParams(dp(32), dp(32));
        ibLp.gravity = Gravity.CENTER_HORIZONTAL;
        iconBox.setLayoutParams(ibLp);
        leftCol.addView(iconBox);

        if (!isLast) {
            View line = new View(this);
            LinearLayout.LayoutParams lineLp = new LinearLayout.LayoutParams(dp(2), dp(28));
            lineLp.gravity = Gravity.CENTER_HORIZONTAL;
            lineLp.setMargins(0, dp(2), 0, dp(2));
            line.setLayoutParams(lineLp);
            line.setBackgroundColor(isCurrent ? color : 0xFFE5E7EB);
            leftCol.addView(line);
        }
        row.addView(leftCol);

        // Right column: label + desc
        LinearLayout rightCol = new LinearLayout(this);
        rightCol.setOrientation(LinearLayout.VERTICAL);
        LinearLayout.LayoutParams rcLp = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f);
        rcLp.setMargins(dp(10), dp(4), 0, isLast ? 0 : dp(8));
        rightCol.setLayoutParams(rcLp);

        // Label + time row
        LinearLayout labelRow = new LinearLayout(this);
        labelRow.setOrientation(LinearLayout.HORIZONTAL);
        labelRow.setGravity(Gravity.CENTER_VERTICAL);
        labelRow.setLayoutParams(lp(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));

        TextView tvLabel = tv(label, 13f, isCurrent ? Typeface.BOLD : Typeface.NORMAL,
                isCurrent ? 0xFF1F2937 : 0xFF6B7280);
        tvLabel.setLayoutParams(new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
        labelRow.addView(tvLabel);

        TextView tvTime = tv(time, 11f, Typeface.NORMAL, 0xFF9CA3AF);
        labelRow.addView(tvTime);
        rightCol.addView(labelRow);

        if (desc != null && !desc.isEmpty()) {
            TextView tvDesc = tv(desc, 11f, Typeface.NORMAL, 0xFF9CA3AF);
            LinearLayout.LayoutParams descLp = lp(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            descLp.setMargins(0, dp(2), 0, 0);
            tvDesc.setLayoutParams(descLp);
            rightCol.addView(tvDesc);
        }
        row.addView(rightCol);
        parent.addView(row);
    }

    // ─── Shipper card ─────────────────────────────────────────────────────────

    private void addShipperCard(String name, String phone) {
        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(Gravity.CENTER_VERTICAL);
        row.setLayoutParams(lp(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));

        // Avatar
        String initials = name.length() >= 2
                ? name.substring(name.lastIndexOf(' ') + 1, Math.min(name.lastIndexOf(' ') + 3, name.length())).toUpperCase()
                : name.substring(0, Math.min(2, name.length())).toUpperCase();
        TextView avatar = new TextView(this);
        avatar.setText(initials);
        avatar.setTextSize(14f);
        avatar.setTypeface(null, Typeface.BOLD);
        avatar.setTextColor(0xFFFFFFFF);
        avatar.setGravity(Gravity.CENTER);
        GradientDrawable avBg = new GradientDrawable();
        avBg.setShape(GradientDrawable.OVAL);
        avBg.setColor(0xFF10B981);
        avatar.setBackground(avBg);
        LinearLayout.LayoutParams avLp = new LinearLayout.LayoutParams(dp(46), dp(46));
        avLp.setMargins(0, 0, dp(12), 0);
        avatar.setLayoutParams(avLp);
        row.addView(avatar);

        // Info column
        LinearLayout info = new LinearLayout(this);
        info.setOrientation(LinearLayout.VERTICAL);
        info.setLayoutParams(new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
        info.addView(tv(name, 14f, Typeface.BOLD, 0xFF1F2937));
        info.addView(tv("⭐ 4.9", 12f, Typeface.NORMAL, 0xFFF59E0B));
        if (phone != null && !phone.isEmpty()) {
            info.addView(tv(phone, 12f, Typeface.NORMAL, 0xFF6B7280));
        }
        row.addView(info);

        // Call button
        if (phone != null && !phone.isEmpty()) {
            TextView btnCall = new TextView(this);
            btnCall.setText("📞 Gọi");
            btnCall.setTextSize(13f);
            btnCall.setTypeface(null, Typeface.BOLD);
            btnCall.setTextColor(0xFFFFFFFF);
            GradientDrawable callBg = new GradientDrawable();
            callBg.setColor(0xFF10B981);
            callBg.setCornerRadius(dp(22));
            btnCall.setBackground(callBg);
            btnCall.setPadding(dp(16), dp(10), dp(16), dp(10));
            String finalPhone = phone;
            btnCall.setOnClickListener(v -> {
                Intent i = new Intent(Intent.ACTION_DIAL);
                i.setData(android.net.Uri.parse("tel:" + finalPhone.replaceAll("[^0-9+]", "")));
                startActivity(i);
            });
            row.addView(btnCall);
        }
        layoutShipper.addView(row);
    }

    // ─── Recipient row ────────────────────────────────────────────────────────

    private void addRecipRow(LinearLayout parent, String icon, String text, String callPhone, boolean bold) {
        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(Gravity.CENTER_VERTICAL);
        LinearLayout.LayoutParams lp = lp(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        lp.setMargins(0, dp(6), 0, dp(6));
        row.setLayoutParams(lp);

        TextView tvIcon = tv(icon, 14f, Typeface.NORMAL, 0xFF1A73E8);
        tvIcon.setLayoutParams(new LinearLayout.LayoutParams(dp(28), ViewGroup.LayoutParams.WRAP_CONTENT));
        row.addView(tvIcon);

        TextView tvText = tv(text, 13f, bold ? Typeface.BOLD : Typeface.NORMAL,
                bold ? 0xFF1F2937 : 0xFF374151);
        tvText.setLayoutParams(new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
        row.addView(tvText);

        if (callPhone != null) {
            TextView callBtn = new TextView(this);
            callBtn.setText("📞");
            callBtn.setTextSize(16f);
            callBtn.setPadding(dp(4), dp(4), dp(4), dp(4));
            callBtn.setOnClickListener(v -> {
                Intent i = new Intent(Intent.ACTION_DIAL);
                i.setData(android.net.Uri.parse("tel:" + callPhone.replaceAll("[^0-9+]", "")));
                startActivity(i);
            });
            row.addView(callBtn);
        }
        parent.addView(row);
    }

    // ─── HTTP ─────────────────────────────────────────────────────────────────

    private static SSLContext trustAllCtx() {
        try {
            SSLContext sc = SSLContext.getInstance("TLS");
            sc.init(null, new TrustManager[]{new X509TrustManager() {
                public void checkClientTrusted(X509Certificate[] c, String a) {}
                public void checkServerTrusted(X509Certificate[] c, String a) {}
                public X509Certificate[] getAcceptedIssuers() { return new X509Certificate[0]; }
            }}, new SecureRandom());
            return sc;
        } catch (Exception e) { return null; }
    }

    private void applyTrustAll(HttpURLConnection conn) {
        if (conn instanceof HttpsURLConnection) {
            SSLContext sc = trustAllCtx();
            if (sc != null) {
                ((HttpsURLConnection) conn).setSSLSocketFactory(sc.getSocketFactory());
                ((HttpsURLConnection) conn).setHostnameVerifier((h, s) -> true);
            }
        }
    }

    private String post(String urlStr, String body, String... headers) throws Exception {
        URL url = new URL(urlStr);
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        applyTrustAll(conn);
        conn.setRequestMethod("POST");
        conn.setRequestProperty("Content-Type", "application/json");
        for (int i = 0; i < headers.length - 1; i += 2) conn.setRequestProperty(headers[i], headers[i + 1]);
        conn.setDoOutput(true); conn.setConnectTimeout(15000); conn.setReadTimeout(15000);
        try (OutputStream os = conn.getOutputStream()) { os.write(body.getBytes(StandardCharsets.UTF_8)); }
        int code = conn.getResponseCode();
        StringBuilder sb = new StringBuilder();
        try (BufferedReader br = new BufferedReader(new InputStreamReader(
                code == 200 ? conn.getInputStream() : conn.getErrorStream(), StandardCharsets.UTF_8))) {
            String line; while ((line = br.readLine()) != null) sb.append(line);
        }
        if (code != 200) throw new Exception("HTTP " + code + ": " + sb);
        return sb.toString();
    }

    // ─── Helpers ──────────────────────────────────────────────────────────────

    private String carrierFullName(String c) {
        if ("GHN".equals(c)) return "Giao Hàng Nhanh";
        if ("GHTK".equals(c)) return "Giao Hàng Tiết Kiệm";
        return c != null ? c : "—";
    }

    private String statusLabel(String s) {
        if (s == null || s.isEmpty()) return "Đang xử lý";
        switch (s) {
            case "ready_to_pick": return "Chờ lấy hàng";
            case "picking":       return "Đang lấy hàng";
            case "picked":        return "Đã lấy hàng";
            case "storing":       return "Đang lưu kho";
            case "transporting":  return "Đang trung chuyển";
            case "sorting":       return "Đang phân loại";
            case "delivering":    return "Đang giao hàng";
            case "delivered":     return "Đã giao hàng";
            case "delivery_fail": return "Giao thất bại";
            case "return":        return "Đang hoàn hàng";
            case "returned":      return "Đã hoàn hàng";
            case "cancel":        return "Đã huỷ";
            default:              return s;
        }
    }

    private String iconForStatus(String s) {
        if (s == null) return "📦";
        switch (s) {
            case "ready_to_pick": case "picking": return "🏪";
            case "picked":        case "storing": return "📦";
            case "transporting":  case "sorting": return "🚛";
            case "delivering":                    return "🛵";
            case "delivered":                     return "✅";
            case "return": case "returned":       return "↩️";
            case "cancel": case "delivery_fail":  return "❌";
            default: return "📦";
        }
    }

    private int colorForStatus(String s) {
        if (s == null) return 0xFF6B7280;
        switch (s) {
            case "delivered":                     return 0xFF10B981;
            case "delivering":                    return 0xFF1A73E8;
            case "picked": case "transporting": case "sorting": return 0xFF0EA5E9;
            case "ready_to_pick": case "picking": return 0xFF8B5CF6;
            case "return": case "returned":       return 0xFFF59E0B;
            case "cancel": case "delivery_fail":  return 0xFFEF4444;
            default: return 0xFF6B7280;
        }
    }

    private String statusEmoji(String s) {
        if (s == null) return "";
        if (s.contains("giao hàng") || s.contains("Đang giao")) return "🛵";
        if (s.contains("Đã giao"))   return "✅";
        if (s.contains("huỷ") || s.contains("thất bại")) return "❌";
        if (s.contains("hoàn"))      return "↩️";
        if (s.contains("trung chuyển") || s.contains("kho")) return "🚛";
        return "📦";
    }

    private int statusBgColor(String s) {
        if (s == null) return 0xFFF3F4F6;
        if (s.contains("Đang giao"))  return 0xFF1A73E8;
        if (s.contains("Đã giao"))    return 0xFF10B981;
        if (s.contains("huỷ") || s.contains("thất bại")) return 0xFFEF4444;
        if (s.contains("hoàn"))       return 0xFFF59E0B;
        return 0xFFFFFFFF;
    }

    private int statusTextColor(String s) {
        int bg = statusBgColor(s);
        return (bg == 0xFFFFFFFF || bg == 0xFFF3F4F6) ? 0xFF1F2937 : 0xFFFFFFFF;
    }

    private String formatTs(long ts) {
        if (ts <= 0) return "—";
        SimpleDateFormat sdf = new SimpleDateFormat("HH:mm · dd/MM", Locale.getDefault());
        return sdf.format(new Date(ts * 1000));
    }

    private String now() {
        return new SimpleDateFormat("HH:mm · dd/MM", Locale.getDefault()).format(new Date());
    }

    private TextView sectionLabel(String text) {
        TextView tv = tv(text, 11f, Typeface.BOLD, 0xFF64748B);
        tv.setAllCaps(true);
        LinearLayout.LayoutParams lp = lp(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        lp.setMargins(dp(16), dp(16), dp(16), dp(4));
        tv.setLayoutParams(lp);
        return tv;
    }

    private LinearLayout whiteCard(LinearLayout parent) {
        LinearLayout card = new LinearLayout(this);
        card.setOrientation(LinearLayout.VERTICAL);
        LinearLayout.LayoutParams lp = lp(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        lp.setMargins(dp(12), 0, dp(12), 0);
        card.setLayoutParams(lp);
        GradientDrawable bg = new GradientDrawable();
        bg.setColor(0xFFFFFFFF);
        bg.setCornerRadius(dp(14));
        card.setBackground(bg);
        card.setPadding(dp(16), dp(14), dp(16), dp(14));
        if (parent != null) parent.addView(card);
        return card;
    }

    private View divider() {
        View v = new View(this);
        LinearLayout.LayoutParams lp = lp(ViewGroup.LayoutParams.MATCH_PARENT, dp(1));
        lp.setMargins(0, dp(8), 0, dp(4));
        v.setLayoutParams(lp);
        v.setBackgroundColor(0xFFF3F4F6);
        return v;
    }

    private TextView tv(String text, float size, int style, int color) {
        TextView tv = new TextView(this);
        tv.setText(text); tv.setTextSize(size);
        tv.setTypeface(null, style); tv.setTextColor(color);
        return tv;
    }

    private LinearLayout.LayoutParams lp(int w, int h) {
        return new LinearLayout.LayoutParams(w, h);
    }

    private View spacer(int h) {
        View v = new View(this);
        v.setLayoutParams(lp(ViewGroup.LayoutParams.MATCH_PARENT, h));
        return v;
    }

    private int dp(int v) {
        return Math.round(v * getResources().getDisplayMetrics().density);
    }

    // ─── Data classes ─────────────────────────────────────────────────────────

    static class TrackInfo {
        final String status, eta, shipperName, shipperPhone;
        final List<TimelineEvent> timeline;
        final boolean isReal;
        TrackInfo(String s, String e, String sn, String sp, List<TimelineEvent> t, boolean r) {
            status = s; eta = e; shipperName = sn; shipperPhone = sp; timeline = t; isReal = r;
        }
    }

    static class TimelineEvent {
        final String icon, label, time, desc;
        final int color;
        final boolean isCurrent;
        TimelineEvent(String icon, int color, String label, String time, String desc, boolean cur) {
            this.icon = icon; this.color = color; this.label = label;
            this.time = time; this.desc = desc; this.isCurrent = cur;
        }
    }
}

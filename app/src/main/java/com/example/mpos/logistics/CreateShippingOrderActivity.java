package com.example.mpos.logistics;

import android.app.AlertDialog;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.graphics.BitmapFactory;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.Filter;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.mpos.R;
import com.example.mpos.database.DatabaseHelper;
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
import java.util.ArrayList;
import java.util.List;

import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;

public class CreateShippingOrderActivity extends AppCompatActivity {

    private static final String PREFS_SHIP        = "mpos_shipping_prefs";
    private static final String GHN_FEE_URL       = "https://dev-online-gateway.ghn.vn/shiip/public-api/v2/shipping-order/fee";
    private static final String GHN_CREATE_URL    = "https://dev-online-gateway.ghn.vn/shiip/public-api/v2/shipping-order/create";
    private static final String GHN_PROVINCE_URL  = "https://dev-online-gateway.ghn.vn/shiip/public-api/master-data/province";
    private static final String GHN_DISTRICT_URL  = "https://dev-online-gateway.ghn.vn/shiip/public-api/master-data/district";
    private static final String GHN_WARD_URL      = "https://dev-online-gateway.ghn.vn/shiip/public-api/master-data/ward";
    private static final String GHTK_FEE_URL      = "https://services.giaohangtietkiem.vn/services/shipment/fee";
    private static final String GHTK_CREATE_URL   = "https://services.giaohangtietkiem.vn/services/shipment/order/?ver=1.5";

    // Order info
    private long orderId;
    private long codAmount;
    private int  itemCount;
    private String orderCode;

    // Recipient fields
    private EditText etName, etPhone, etStreet, etWeight;

    // Location pickers
    private TextView tvProvince, tvDistrict, tvWard;
    private int    toProvinceId   = -1;
    private String toProvinceName = null;
    private int    toDistrictId   = -1;
    private String toDistrictName = null;
    private String toWardCode     = null;
    private String toWardName     = null;

    // Cached master data
    private List<String[]> cachedProvinces = null;
    private List<String[]> cachedDistricts = null;
    private List<String[]> cachedWards     = null;

    // Carrier UI
    private LinearLayout cardGhn, cardGhtk;
    private TextView tvGhnPrice, tvGhtkPrice, tvGhnEta, tvGhtkEta;
    private TextView tvGhnBadge, tvGhtkBadge, tvCheckGhn, tvCheckGhtk;
    private TextView tvSavings, tvSelectedFee, tvSelectedLabel;
    private LinearLayout feeRow;
    private TextView btnCreate;
    private ProgressBar progressBar;

    private DatabaseHelper db;
    private String selectedCarrier = null;
    private int feeGhn  = 0;
    private int feeGhtk = 0;
    private boolean ratesLoaded = false;

    // ── Lifecycle ─────────────────────────────────────────────────────────────

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        db        = new DatabaseHelper(this);
        orderId   = getIntent().getLongExtra("order_id", -1);
        codAmount = getIntent().getLongExtra("cod_amount", 0);
        itemCount = getIntent().getIntExtra("item_count", 0);
        orderCode = getIntent().getStringExtra("order_code");
        buildUI();
        prefillFromOrder();
    }

    // ── UI Builder ────────────────────────────────────────────────────────────

    private void buildUI() {
        android.widget.ScrollView sv = new android.widget.ScrollView(this);
        sv.setFillViewport(true);
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setBackgroundColor(0xFFF1F5F9);
        sv.addView(root);

        // Toolbar
        root.addView(buildToolbar());

        progressBar = new ProgressBar(this);
        progressBar.setVisibility(View.GONE);
        LinearLayout.LayoutParams pLp = lp(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        pLp.setMargins(dp(16), dp(6), dp(16), 0);
        progressBar.setLayoutParams(pLp);
        root.addView(progressBar);

        // ── NGƯỜI NHẬN ────────────────────────────────────────────────────────
        root.addView(sectionLabel("📍  NGƯỜI NHẬN"));
        LinearLayout recipCard = card(root);

        etName   = iconField(recipCard, "Tên người nhận *",          "Nguyễn Thị Lan",         android.text.InputType.TYPE_CLASS_TEXT | android.text.InputType.TYPE_TEXT_FLAG_CAP_WORDS);
        etPhone  = iconField(recipCard, "Số điện thoại *",           "0912 345 678",            android.text.InputType.TYPE_CLASS_PHONE);
        etStreet = iconField(recipCard, "Số nhà, tên đường *",       "48/12 Nguyễn Thị Minh Khai", android.text.InputType.TYPE_CLASS_TEXT);

        recipCard.addView(fieldLabel("Tỉnh / Thành phố *"));
        tvProvince = pickerView("Chọn tỉnh / thành phố");
        tvProvince.setOnClickListener(v -> openProvincePicker());
        recipCard.addView(tvProvince);

        recipCard.addView(fieldLabel("Quận / Huyện *"));
        tvDistrict = pickerView("— Chọn tỉnh trước —");
        tvDistrict.setAlpha(0.45f);
        tvDistrict.setOnClickListener(v -> {
            if (toProvinceId < 0) { Toast.makeText(this, "Chọn tỉnh/thành phố trước", Toast.LENGTH_SHORT).show(); return; }
            openDistrictPicker();
        });
        recipCard.addView(tvDistrict);

        recipCard.addView(fieldLabel("Phường / Xã *"));
        tvWard = pickerView("— Chọn quận/huyện trước —");
        tvWard.setAlpha(0.45f);
        tvWard.setOnClickListener(v -> {
            if (toDistrictId < 0) { Toast.makeText(this, "Chọn quận/huyện trước", Toast.LENGTH_SHORT).show(); return; }
            openWardPicker();
        });
        recipCard.addView(tvWard);

        // ── GÓI HÀNG ──────────────────────────────────────────────────────────
        root.addView(sectionLabel("📦  GÓI HÀNG"));
        LinearLayout pkgCard = card(root);
        pkgCard.addView(fieldLabel("Khối lượng (kg)"));
        etWeight = styledEdit("0.5");
        etWeight.setInputType(android.text.InputType.TYPE_CLASS_NUMBER | android.text.InputType.TYPE_NUMBER_FLAG_DECIMAL);
        pkgCard.addView(etWeight);

        // ── VẬN CHUYỂN ────────────────────────────────────────────────────────
        root.addView(sectionLabel("🚚  VẬN CHUYỂN"));
        LinearLayout carrierSection = card(root);
        buildCarrierSection(carrierSection);

        // ── THANH TOÁN ────────────────────────────────────────────────────────
        root.addView(sectionLabel("💰  THANH TOÁN"));
        LinearLayout payCard = card(root);
        buildPaySection(payCard);

        // ── CREATE BUTTON ──────────────────────────────────────────────────────
        btnCreate = new TextView(this);
        btnCreate.setText("Chọn đơn vị vận chuyển");
        btnCreate.setTextSize(15f);
        btnCreate.setTypeface(null, Typeface.BOLD);
        btnCreate.setTextColor(0xFFFFFFFF);
        btnCreate.setGravity(Gravity.CENTER);
        btnCreate.setBackground(roundRect(0xFF1A73E8, dp(14)));
        LinearLayout.LayoutParams btnLp = lp(ViewGroup.LayoutParams.MATCH_PARENT, dp(54));
        btnLp.setMargins(dp(16), dp(12), dp(16), dp(36));
        btnCreate.setLayoutParams(btnLp);
        btnCreate.setAlpha(0.45f);
        btnCreate.setOnClickListener(v -> handleCreate());
        root.addView(btnCreate);

        setContentView(sv);
    }

    private LinearLayout buildToolbar() {
        LinearLayout tb = new LinearLayout(this);
        tb.setOrientation(LinearLayout.HORIZONTAL);
        tb.setGravity(Gravity.CENTER_VERTICAL);
        tb.setBackgroundColor(0xFF0F2942);
        tb.setPadding(dp(14), dp(46), dp(16), dp(14));
        tb.setLayoutParams(lp(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));

        TextView back = tv("←", 20f, Typeface.NORMAL, 0xFFFFFFFF);
        back.setPadding(dp(4), dp(8), dp(16), dp(8));
        back.setOnClickListener(v -> finish());
        tb.addView(back);

        LinearLayout col = new LinearLayout(this);
        col.setOrientation(LinearLayout.VERTICAL);
        col.setLayoutParams(new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
        col.addView(tv("Tạo đơn giao hàng", 16f, Typeface.BOLD, 0xFFFFFFFF));
        String sub = (orderCode != null ? "Đơn " + orderCode : "Tạo đơn mới")
                + (itemCount > 0 ? " · " + itemCount + " sản phẩm" : "");
        col.addView(tv(sub, 12f, Typeface.NORMAL, 0xFF93C5FD));
        tb.addView(col);
        return tb;
    }

    private void buildCarrierSection(LinearLayout parent) {
        LinearLayout hdr = new LinearLayout(this);
        hdr.setOrientation(LinearLayout.HORIZONTAL);
        hdr.setGravity(Gravity.CENTER_VERTICAL);
        hdr.setLayoutParams(lp(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        TextView ttl = tv("So sánh vận chuyển", 13f, Typeface.BOLD, 0xFF1F2937);
        ttl.setLayoutParams(new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
        hdr.addView(ttl);
        hdr.addView(tv("Chạm để chọn →", 11f, Typeface.NORMAL, 0xFF9CA3AF));
        parent.addView(hdr);

        // Carrier cards row
        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        LinearLayout.LayoutParams rLp = lp(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        rLp.setMargins(0, dp(12), 0, 0);
        row.setLayoutParams(rLp);

        cardGhn  = buildCarrierCard("GHN",  "Giao Hàng\nNhanh",    R.drawable.logo_ghn,  0xFFFF6200);
        cardGhtk = buildCarrierCard("GHTK", "Giao Hàng\nTiết Kiệm", R.drawable.logo_ghtk, 0xFF00B14F);

        LinearLayout.LayoutParams c1 = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f);
        c1.setMargins(0, 0, dp(6), 0); cardGhn.setLayoutParams(c1);
        LinearLayout.LayoutParams c2 = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f);
        c2.setMargins(dp(6), 0, 0, 0); cardGhtk.setLayoutParams(c2);
        row.addView(cardGhn); row.addView(cardGhtk);
        parent.addView(row);

        tvGhnPrice  = (TextView) cardGhn.getTag(R.id.tag_price);
        tvGhtkPrice = (TextView) cardGhtk.getTag(R.id.tag_price);
        tvGhnBadge  = (TextView) cardGhn.getTag(R.id.tag_badge);
        tvGhtkBadge = (TextView) cardGhtk.getTag(R.id.tag_badge);
        tvCheckGhn  = (TextView) cardGhn.getTag(R.id.tag_check);
        tvCheckGhtk = (TextView) cardGhtk.getTag(R.id.tag_check);

        tvSavings = tv("", 12f, Typeface.NORMAL, 0xFF16A34A);
        tvSavings.setVisibility(View.GONE);
        LinearLayout.LayoutParams savLp = lp(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        savLp.setMargins(0, dp(10), 0, 0);
        tvSavings.setLayoutParams(savLp);
        parent.addView(tvSavings);

        // Refresh button
        TextView btnRefresh = tv("↺ Cập nhật giá", 12f, Typeface.BOLD, 0xFF1A73E8);
        LinearLayout.LayoutParams rflLp = lp(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        rflLp.setMargins(0, dp(12), 0, 0); rflLp.gravity = Gravity.END;
        btnRefresh.setLayoutParams(rflLp);
        btnRefresh.setOnClickListener(v -> fetchRates());
        parent.addView(btnRefresh);

        fetchRates();
    }

    private void buildPaySection(LinearLayout payCard) {
        LinearLayout codRow = new LinearLayout(this);
        codRow.setOrientation(LinearLayout.HORIZONTAL);
        codRow.setGravity(Gravity.CENTER_VERTICAL);
        codRow.setLayoutParams(lp(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        TextView lbl = tv("Thu hộ (COD)", 13f, Typeface.NORMAL, 0xFF6B7280);
        lbl.setLayoutParams(new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
        codRow.addView(lbl);
        codRow.addView(tv(CurrencyUtils.vnd(codAmount), 15f, Typeface.BOLD, 0xFF1A73E8));
        payCard.addView(codRow);

        feeRow = new LinearLayout(this);
        feeRow.setOrientation(LinearLayout.HORIZONTAL);
        feeRow.setGravity(Gravity.CENTER_VERTICAL);
        LinearLayout.LayoutParams frLp = lp(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        frLp.setMargins(0, dp(10), 0, 0);
        feeRow.setLayoutParams(frLp);
        feeRow.setVisibility(View.GONE);

        tvSelectedLabel = tv("PHÍ VẬN CHUYỂN", 11f, Typeface.BOLD, 0xFF9CA3AF);
        tvSelectedLabel.setAllCaps(true);
        tvSelectedLabel.setLayoutParams(new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
        feeRow.addView(tvSelectedLabel);
        tvSelectedFee = tv("—", 18f, Typeface.BOLD, 0xFF1A73E8);
        feeRow.addView(tvSelectedFee);
        payCard.addView(feeRow);
    }

    // ── Carrier card builder ──────────────────────────────────────────────────

    private LinearLayout buildCarrierCard(String carrier, String name, int logoRes, int brandColor) {
        LinearLayout card = new LinearLayout(this);
        card.setOrientation(LinearLayout.VERTICAL);
        card.setGravity(Gravity.CENTER_HORIZONTAL);
        card.setPadding(dp(10), dp(14), dp(10), dp(14));
        card.setBackground(roundRectStroke(0xFFFFFFFF, dp(14), 0xFFE5E7EB, 2));
        card.setClickable(true); card.setFocusable(true);
        card.setOnClickListener(v -> selectCarrier(carrier));

        ImageView logo = new ImageView(this);
        logo.setLayoutParams(new LinearLayout.LayoutParams(dp(52), dp(52)));
        logo.setScaleType(ImageView.ScaleType.FIT_CENTER);
        GradientDrawable logoBg = new GradientDrawable();
        logoBg.setColor(brandColor); logoBg.setCornerRadius(dp(10));
        logo.setBackground(logoBg);
        try { logo.setImageBitmap(BitmapFactory.decodeResource(getResources(), logoRes)); } catch (Exception ignored) {}
        card.addView(logo);

        TextView tvName = tv(name, 11f, Typeface.BOLD, 0xFF374151);
        tvName.setGravity(Gravity.CENTER);
        LinearLayout.LayoutParams nameLp = lp(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        nameLp.setMargins(0, dp(6), 0, 0); tvName.setLayoutParams(nameLp);
        card.addView(tvName);

        TextView badge = new TextView(this);
        badge.setTextSize(9f); badge.setTypeface(null, Typeface.BOLD);
        badge.setTextColor(0xFFFFFFFF); badge.setGravity(Gravity.CENTER);
        badge.setPadding(dp(6), dp(2), dp(6), dp(2));
        badge.setVisibility(View.INVISIBLE);
        LinearLayout.LayoutParams badgeLp = lp(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        badgeLp.setMargins(0, dp(5), 0, 0); badgeLp.gravity = Gravity.CENTER_HORIZONTAL;
        badge.setLayoutParams(badgeLp);
        card.addView(badge); card.setTag(R.id.tag_badge, badge);

        TextView price = tv("—", 16f, Typeface.BOLD, 0xFFF57C00);
        price.setGravity(Gravity.CENTER);
        LinearLayout.LayoutParams priceLp = lp(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        priceLp.setMargins(0, dp(4), 0, 0); price.setLayoutParams(priceLp);
        card.addView(price); card.setTag(R.id.tag_price, price);

        String etaText = "GHN".equals(carrier) ? "1-2 ngày" : "2-3 ngày";
        TextView etaTv = tv(etaText, 10f, Typeface.NORMAL, 0xFF9CA3AF);
        etaTv.setGravity(Gravity.CENTER); card.addView(etaTv);
        card.setTag(R.id.tag_eta, etaTv);

        TextView check = new TextView(this);
        check.setText("✓"); check.setTextSize(13f); check.setTypeface(null, Typeface.BOLD);
        check.setTextColor(0xFFFFFFFF); check.setGravity(Gravity.CENTER);
        GradientDrawable chkBg = new GradientDrawable();
        chkBg.setShape(GradientDrawable.OVAL); chkBg.setColor(0xFF1A73E8);
        check.setBackground(chkBg);
        LinearLayout.LayoutParams chkLp = new LinearLayout.LayoutParams(dp(22), dp(22));
        chkLp.setMargins(0, dp(6), 0, 0); chkLp.gravity = Gravity.CENTER_HORIZONTAL;
        check.setLayoutParams(chkLp); check.setVisibility(View.GONE);
        card.addView(check); card.setTag(R.id.tag_check, check);

        return card;
    }

    // ── Location pickers ──────────────────────────────────────────────────────

    private void openProvincePicker() {
        if (cachedProvinces != null) {
            showSearchDialog("Chọn tỉnh / thành phố", cachedProvinces, (id, name) -> {
                toProvinceId = Integer.parseInt(id);
                toProvinceName = name;
                tvProvince.setText(name); tvProvince.setTextColor(0xFF1F2937);
                // Reset downstream
                toDistrictId = -1; toDistrictName = null;
                toWardCode = null; toWardName = null;
                cachedDistricts = null; cachedWards = null;
                tvDistrict.setText("Chọn quận / huyện"); tvDistrict.setTextColor(0xFF94A3B8); tvDistrict.setAlpha(1f);
                tvWard.setText("— Chọn quận/huyện trước —"); tvWard.setTextColor(0xFF94A3B8); tvWard.setAlpha(0.45f);
                // Reset rates since to-district changed
                resetRates();
            });
            return;
        }
        progressBar.setVisibility(View.VISIBLE);
        SharedPreferences p = getSharedPreferences(PREFS_SHIP, Context.MODE_PRIVATE);
        String tok = p.getString("ghn_token", "");
        new Thread(() -> {
            try {
                String resp = get(GHN_PROVINCE_URL, "Token", tok);
                JSONArray arr = new JSONObject(resp).getJSONArray("data");
                List<String[]> list = new ArrayList<>();
                for (int i = 0; i < arr.length(); i++) {
                    JSONObject o = arr.getJSONObject(i);
                    list.add(new String[]{String.valueOf(o.getInt("ProvinceID")), o.getString("ProvinceName")});
                }
                list.sort((a, b) -> a[1].compareToIgnoreCase(b[1]));
                cachedProvinces = list;
                runOnUiThread(() -> { progressBar.setVisibility(View.GONE); openProvincePicker(); });
            } catch (Exception e) {
                runOnUiThread(() -> { progressBar.setVisibility(View.GONE);
                    Toast.makeText(this, "Không tải được danh sách tỉnh", Toast.LENGTH_SHORT).show(); });
            }
        }).start();
    }

    private void openDistrictPicker() {
        if (cachedDistricts != null) {
            showSearchDialog("Chọn quận / huyện", cachedDistricts, (id, name) -> {
                toDistrictId = Integer.parseInt(id);
                toDistrictName = name;
                tvDistrict.setText(name); tvDistrict.setTextColor(0xFF1F2937);
                toWardCode = null; toWardName = null;
                cachedWards = null;
                tvWard.setText("Chọn phường / xã"); tvWard.setTextColor(0xFF94A3B8); tvWard.setAlpha(1f);
                // Auto-refresh rates with real district
                fetchRates();
            });
            return;
        }
        progressBar.setVisibility(View.VISIBLE);
        SharedPreferences p = getSharedPreferences(PREFS_SHIP, Context.MODE_PRIVATE);
        String tok = p.getString("ghn_token", "");
        new Thread(() -> {
            try {
                String resp = get(GHN_DISTRICT_URL + "?province_id=" + toProvinceId, "Token", tok);
                JSONArray arr = new JSONObject(resp).getJSONArray("data");
                List<String[]> list = new ArrayList<>();
                for (int i = 0; i < arr.length(); i++) {
                    JSONObject o = arr.getJSONObject(i);
                    list.add(new String[]{String.valueOf(o.getInt("DistrictID")), o.getString("DistrictName")});
                }
                list.sort((a, b) -> a[1].compareToIgnoreCase(b[1]));
                cachedDistricts = list;
                runOnUiThread(() -> { progressBar.setVisibility(View.GONE); openDistrictPicker(); });
            } catch (Exception e) {
                runOnUiThread(() -> { progressBar.setVisibility(View.GONE);
                    Toast.makeText(this, "Không tải được danh sách quận", Toast.LENGTH_SHORT).show(); });
            }
        }).start();
    }

    private void openWardPicker() {
        if (cachedWards != null) {
            showSearchDialog("Chọn phường / xã", cachedWards, (id, name) -> {
                toWardCode = id;
                toWardName = name;
                tvWard.setText(name); tvWard.setTextColor(0xFF1F2937);
            });
            return;
        }
        progressBar.setVisibility(View.VISIBLE);
        SharedPreferences p = getSharedPreferences(PREFS_SHIP, Context.MODE_PRIVATE);
        String tok = p.getString("ghn_token", "");
        new Thread(() -> {
            try {
                String resp = get(GHN_WARD_URL + "?district_id=" + toDistrictId, "Token", tok);
                JSONArray arr = new JSONObject(resp).getJSONArray("data");
                List<String[]> list = new ArrayList<>();
                for (int i = 0; i < arr.length(); i++) {
                    JSONObject o = arr.getJSONObject(i);
                    list.add(new String[]{o.getString("WardCode"), o.getString("WardName")});
                }
                list.sort((a, b) -> a[1].compareToIgnoreCase(b[1]));
                cachedWards = list;
                runOnUiThread(() -> { progressBar.setVisibility(View.GONE); openWardPicker(); });
            } catch (Exception e) {
                runOnUiThread(() -> { progressBar.setVisibility(View.GONE);
                    Toast.makeText(this, "Không tải được danh sách phường", Toast.LENGTH_SHORT).show(); });
            }
        }).start();
    }

    // ── Search dialog ─────────────────────────────────────────────────────────

    interface OnPick { void onPick(String id, String name); }

    private void showSearchDialog(String title, List<String[]> items, OnPick cb) {
        // Build display names
        final List<String[]> originalItems = new ArrayList<>(items);
        final String[] names = new String[items.size()];
        for (int i = 0; i < items.size(); i++) names[i] = items.get(i)[1];

        View dialogView = getLayoutInflater().inflate(android.R.layout.list_content, null);
        // Build custom view manually
        LinearLayout dlgRoot = new LinearLayout(this);
        dlgRoot.setOrientation(LinearLayout.VERTICAL);

        EditText etSearch = new EditText(this);
        etSearch.setHint("🔍  Tìm kiếm...");
        etSearch.setTextSize(14f);
        etSearch.setBackgroundColor(0xFFF8FAFC);
        etSearch.setPadding(dp(16), dp(12), dp(16), dp(12));
        dlgRoot.addView(etSearch);

        View div = new View(this);
        div.setLayoutParams(new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 1));
        div.setBackgroundColor(0xFFE2E8F0);
        dlgRoot.addView(div);

        ListView lv = new ListView(this);
        lv.setDividerHeight(0);
        LinearLayout.LayoutParams lvLp = lp(ViewGroup.LayoutParams.MATCH_PARENT, dp(320));
        lv.setLayoutParams(lvLp);

        final ArrayList<String> nameList = new ArrayList<>();
        for (String n : names) nameList.add(n);

        ArrayAdapter<String> adapter = new ArrayAdapter<String>(this, android.R.layout.simple_list_item_1, nameList) {
            @Override public Filter getFilter() { return super.getFilter(); }
        };
        lv.setAdapter(adapter);
        dlgRoot.addView(lv);

        AlertDialog dialog = new AlertDialog.Builder(this)
                .setTitle(title)
                .setView(dlgRoot)
                .setNegativeButton("Đóng", null)
                .create();

        lv.setOnItemClickListener((parent, view, pos, id) -> {
            String picked = (String) adapter.getItem(pos);
            for (String[] item : originalItems) {
                if (item[1].equals(picked)) { cb.onPick(item[0], item[1]); break; }
            }
            dialog.dismiss();
        });

        etSearch.addTextChangedListener(new TextWatcher() {
            public void afterTextChanged(Editable s) { adapter.getFilter().filter(s.toString()); }
            public void beforeTextChanged(CharSequence s, int a, int b, int c) {}
            public void onTextChanged(CharSequence s, int a, int b, int c) {}
        });

        dialog.show();
    }

    // ── Rate fetching ─────────────────────────────────────────────────────────

    private void resetRates() {
        ratesLoaded = false; feeGhn = 0; feeGhtk = 0;
        selectedCarrier = null;
        tvGhnPrice.setText("—"); tvGhtkPrice.setText("—");
        tvGhnBadge.setVisibility(View.INVISIBLE); tvGhtkBadge.setVisibility(View.INVISIBLE);
        tvSavings.setVisibility(View.GONE); feeRow.setVisibility(View.GONE);
        applyCardSelection(cardGhn,  tvCheckGhn,  false);
        applyCardSelection(cardGhtk, tvCheckGhtk, false);
        btnCreate.setText("Chọn đơn vị vận chuyển"); btnCreate.setAlpha(0.45f);
    }

    private void fetchRates() {
        double wKg; try { wKg = Double.parseDouble(etWeight.getText().toString().trim()); } catch (Exception e) { wKg = 0.5; }
        final int wg = (int)(wKg * 1000);

        progressBar.setVisibility(View.VISIBLE);
        tvGhnPrice.setText("…"); tvGhtkPrice.setText("…");
        tvGhnBadge.setVisibility(View.INVISIBLE); tvGhtkBadge.setVisibility(View.INVISIBLE);
        tvSavings.setVisibility(View.GONE);

        SharedPreferences prefs = getSharedPreferences(PREFS_SHIP, Context.MODE_PRIVATE);
        String ghnTok  = prefs.getString("ghn_token", "");
        String ghnShop = prefs.getString("ghn_shop_id", "0");
        String ghtkTok = prefs.getString("ghtk_token", "");

        final int distId = toDistrictId > 0 ? toDistrictId : 1442;
        final String provName = toProvinceName != null ? toProvinceName : "Hà Nội";
        final String distName = toDistrictName != null ? toDistrictName : "Hà Nội";

        new Thread(() -> {
            int ghn  = fetchGhnFee(ghnTok, ghnShop, wg, distId);
            int ghtk = fetchGhtkFee(ghtkTok, wg, provName, distName);
            runOnUiThread(() -> {
                progressBar.setVisibility(View.GONE);
                feeGhn = ghn; feeGhtk = ghtk; ratesLoaded = true;
                tvGhnPrice.setText(CurrencyUtils.vnd(ghn));
                tvGhtkPrice.setText(CurrencyUtils.vnd(ghtk));
                applyBadge(tvGhnBadge,  "NHANH NHẤT", 0xFF1A73E8);
                boolean ghtkCheaper = ghtk <= ghn;
                applyBadge(tvGhtkBadge, ghtkCheaper ? "RẺ NHẤT" : "CHẬM HƠN", ghtkCheaper ? 0xFF16A34A : 0xFF9CA3AF);
                tvGhnBadge.setVisibility(View.VISIBLE); tvGhtkBadge.setVisibility(View.VISIBLE);
                if (selectedCarrier != null) { updateCarrierUI(); showSavings(); }
            });
        }).start();
    }

    private void applyBadge(TextView badge, String text, int color) {
        badge.setText(text);
        GradientDrawable bg = new GradientDrawable();
        bg.setColor(color); bg.setCornerRadius(dp(6));
        badge.setBackground(bg);
    }

    private void selectCarrier(String carrier) {
        selectedCarrier = carrier;
        updateCarrierUI(); showSavings();
        int fee = "GHN".equals(carrier) ? feeGhn : feeGhtk;
        String eta = "GHN".equals(carrier) ? "1-2 ngày" : "2-3 ngày";
        tvSelectedLabel.setText("PHÍ VẬN CHUYỂN  ·  " + carrier + "  ·  " + eta);
        tvSelectedFee.setText(CurrencyUtils.vnd(fee));
        feeRow.setVisibility(View.VISIBLE);
        btnCreate.setText("🚚  Tạo vận đơn " + carrier + "  –  " + CurrencyUtils.vnd(fee));
        btnCreate.setAlpha(1f);
        btnCreate.setBackground(roundRect(0xFF1A73E8, dp(14)));
    }

    private void updateCarrierUI() {
        applyCardSelection(cardGhn,  tvCheckGhn,  "GHN".equals(selectedCarrier));
        applyCardSelection(cardGhtk, tvCheckGhtk, "GHTK".equals(selectedCarrier));
    }

    private void applyCardSelection(LinearLayout card, TextView check, boolean selected) {
        card.setBackground(roundRectStroke(
                selected ? 0xFFEFF6FF : 0xFFFFFFFF, dp(14),
                selected ? 0xFF1A73E8 : 0xFFE5E7EB,
                selected ? 3 : 2));
        check.setVisibility(selected ? View.VISIBLE : View.GONE);
    }

    private void showSavings() {
        if (!ratesLoaded || feeGhn == 0 || feeGhtk == 0) return;
        int cheaper = Math.min(feeGhn, feeGhtk);
        int expensive = Math.max(feeGhn, feeGhtk);
        String cheapName = feeGhn <= feeGhtk ? "GHN" : "GHTK";
        int saving = expensive - cheaper;
        if (saving > 0) {
            tvSavings.setText("💡 " + cheapName + " là lựa chọn rẻ nhất – tiết kiệm "
                    + CurrencyUtils.vnd(saving) + " so với đắt nhất");
            tvSavings.setVisibility(View.VISIBLE);
        }
    }

    // ── Create order ──────────────────────────────────────────────────────────

    private void handleCreate() {
        if (selectedCarrier == null) {
            Toast.makeText(this, "Chọn đơn vị vận chuyển", Toast.LENGTH_SHORT).show(); return;
        }
        String name   = etName.getText().toString().trim();
        String phone  = etPhone.getText().toString().trim();
        String street = etStreet.getText().toString().trim();
        if (name.isEmpty() || phone.isEmpty() || street.isEmpty()) {
            Toast.makeText(this, "Vui lòng điền đủ tên, SĐT và địa chỉ", Toast.LENGTH_SHORT).show(); return;
        }
        if ("GHN".equals(selectedCarrier) && toDistrictId < 0) {
            Toast.makeText(this, "Chọn quận/huyện để tạo đơn GHN chính xác", Toast.LENGTH_SHORT).show(); return;
        }

        String fullAddress = street
                + (toWardName     != null ? ", " + toWardName     : "")
                + (toDistrictName != null ? ", " + toDistrictName : "")
                + (toProvinceName != null ? ", " + toProvinceName : "");

        int wg = 500;
        try { wg = (int)(Double.parseDouble(etWeight.getText().toString().trim()) * 1000); } catch (Exception ignored) {}

        btnCreate.setAlpha(0.6f); btnCreate.setClickable(false);
        progressBar.setVisibility(View.VISIBLE);

        SharedPreferences prefs = getSharedPreferences(PREFS_SHIP, Context.MODE_PRIVATE);
        String ghnTok  = prefs.getString("ghn_token", "");
        String ghnShop = prefs.getString("ghn_shop_id", "0");
        String ghtkTok = prefs.getString("ghtk_token", "");

        final String finalName = name, finalPhone = phone, finalAddr = fullAddress;
        final int finalWg = wg;

        new Thread(() -> {
            String trackingCode = null;
            String apiError = null;
            try {
                if ("GHN".equals(selectedCarrier) && !ghnTok.isEmpty()) {
                    trackingCode = createGhnOrder(ghnTok, ghnShop, finalName, finalPhone,
                            street, toDistrictId > 0 ? toDistrictId : 1442,
                            toWardCode != null ? toWardCode : null, finalWg, (int) codAmount);
                } else if ("GHTK".equals(selectedCarrier) && !ghtkTok.isEmpty()) {
                    trackingCode = createGhtkOrder(ghtkTok, finalName, finalPhone, finalAddr,
                            toDistrictName != null ? toDistrictName : "Hà Nội",
                            toProvinceName != null ? toProvinceName : "Hà Nội",
                            finalWg, (int) codAmount);
                }
            } catch (Exception e) {
                apiError = e.getMessage();
                android.util.Log.e("CreateShipping", "API error: " + apiError, e);
            }
            final String finalApiError = apiError;

            boolean apiOk = trackingCode != null;
            final String finalCode = apiOk ? trackingCode
                    : selectedCarrier.substring(0, 3).toUpperCase() + "-" + (System.currentTimeMillis() % 100000);
            int fee = "GHN".equals(selectedCarrier) ? feeGhn : feeGhtk;

            ContentValues cv = new ContentValues();
            cv.put("order_id",          orderId > 0 ? orderId : null);
            cv.put("carrier",           selectedCarrier);
            cv.put("tracking_code",     finalCode);
            cv.put("shipping_fee",      fee);
            cv.put("from_province",     "Hồ Chí Minh");
            cv.put("to_province",       toProvinceName != null ? toProvinceName : "Hà Nội");
            cv.put("weight_gram",       finalWg);
            cv.put("status",            apiOk ? "CONFIRMED" : "BOOKED");
            cv.put("created_at",        System.currentTimeMillis());
            cv.put("recipient_name",    finalName);
            cv.put("recipient_phone",   finalPhone);
            cv.put("recipient_address", finalAddr);
            cv.put("recipient_district", toDistrictName != null ? toDistrictName : "");
            cv.put("cod_amount",        codAmount);
            db.getWritableDatabase().insert("shipping_orders", null, cv);

            runOnUiThread(() -> {
                progressBar.setVisibility(View.GONE);
                btnCreate.setAlpha(1f); btnCreate.setClickable(true);
                if (apiOk) {
                    Toast.makeText(this, "✅ Tạo vận đơn thành công!", Toast.LENGTH_SHORT).show();
                } else {
                    String errShort = finalApiError != null
                        ? finalApiError.replaceAll("HTTP \\d+: ", "").substring(0, Math.min(finalApiError.length(), 120))
                        : "Token/ShopId không hợp lệ hoặc thiếu ward code";
                    new android.app.AlertDialog.Builder(this)
                        .setTitle("⚠️ API lỗi – đã lưu offline")
                        .setMessage(errShort)
                        .setPositiveButton("OK", null)
                        .show();
                }
                Intent intent = new Intent(this, TrackingDetailActivity.class);
                intent.putExtra("tracking_code",     finalCode);
                intent.putExtra("carrier",           selectedCarrier);
                intent.putExtra("recipient_name",    finalName);
                intent.putExtra("recipient_phone",   finalPhone);
                intent.putExtra("recipient_address", finalAddr);
                intent.putExtra("shipping_fee",      fee);
                intent.putExtra("cod_amount",        codAmount);
                startActivity(intent);
                finish();
            });
        }).start();
    }

    // ── GHN + GHTK APIs ───────────────────────────────────────────────────────

    private int fetchGhnFee(String token, String shopId, int wg, int distId) {
        try {
            JSONObject body = new JSONObject();
            body.put("service_type_id", 2);
            body.put("from_district_id", 1453);
            body.put("to_district_id",   distId);
            body.put("to_ward_code",     toWardCode != null ? toWardCode : "");
            body.put("height", 10); body.put("length", 20);
            body.put("weight", wg); body.put("width",  15);
            body.put("insurance_value", codAmount);
            String resp = post(GHN_FEE_URL, body.toString(), "Token", token, "ShopId", shopId);
            return new JSONObject(resp).getJSONObject("data").getInt("total");
        } catch (Exception e) { return mockFee("GHN", wg); }
    }

    private int fetchGhtkFee(String token, int wg, String provName, String distName) {
        try {
            String url = GHTK_FEE_URL
                    + "?pick_province=" + encode("Hồ Chí Minh")
                    + "&pick_district=" + encode("Hồ Chí Minh")
                    + "&province=" + encode(provName)
                    + "&district=" + encode(distName)
                    + "&weight=" + wg + "&value=" + codAmount + "&deliver_option=none";
            URL u = new URL(url);
            HttpURLConnection conn = (HttpURLConnection) u.openConnection();
            applyTrustAll(conn);
            conn.setRequestProperty("Token", token);
            conn.setConnectTimeout(15000); conn.setReadTimeout(15000);
            StringBuilder sb = new StringBuilder();
            try (BufferedReader br = new BufferedReader(new InputStreamReader(conn.getInputStream()))) {
                String line; while ((line = br.readLine()) != null) sb.append(line);
            }
            return (int) new JSONObject(sb.toString()).getJSONObject("fee").getDouble("fee");
        } catch (Exception e) { return mockFee("GHTK", wg); }
    }

    private String createGhnOrder(String token, String shopId, String name, String phone,
            String address, int distId, String wardCode, int wg, int cod) throws Exception {
        JSONObject body = new JSONObject();
        body.put("to_name", name); body.put("to_phone", phone);
        body.put("to_address", address);
        if (wardCode != null && !wardCode.isEmpty()) body.put("to_ward_code", wardCode);
        body.put("to_district_id", distId);
        body.put("weight", wg); body.put("length", 20); body.put("width", 15); body.put("height", 10);
        body.put("service_type_id", 2); body.put("payment_type_id", 1);
        body.put("cod_amount", cod); body.put("required_note", "CHOXEMHANGKHONGTHU");
        JSONArray items = new JSONArray();
        JSONObject item = new JSONObject(); item.put("name","Hàng hóa"); item.put("quantity",1); item.put("weight",wg);
        items.put(item); body.put("items", items);
        String resp = post(GHN_CREATE_URL, body.toString(), "Token", token, "ShopId", shopId);
        return new JSONObject(resp).getJSONObject("data").getString("order_code");
    }

    private String createGhtkOrder(String token, String name, String phone, String address,
            String district, String province, int wg, int cod) throws Exception {
        JSONObject order = new JSONObject();
        order.put("id",           "MPOS-" + System.currentTimeMillis());
        order.put("pick_name",    "Cửa hàng");
        order.put("pick_address", "Địa chỉ cửa hàng");
        order.put("pick_province","Hồ Chí Minh"); order.put("pick_district","Hồ Chí Minh");
        order.put("pick_tel",     "0900000000");
        order.put("tel", phone); order.put("name", name);
        order.put("address", address); order.put("province", province); order.put("district", district);
        order.put("weight", wg); order.put("value", cod); order.put("pick_money", cod); order.put("note", "");
        JSONArray products = new JSONArray();
        JSONObject p = new JSONObject(); p.put("name","Hàng hóa"); p.put("weight",wg); p.put("quantity",1);
        products.put(p);
        JSONObject body = new JSONObject(); body.put("order", order); body.put("products", products);
        String resp = post(GHTK_CREATE_URL, body.toString(), "Token", token);
        return new JSONObject(resp).getJSONObject("order").getString("label");
    }

    private int mockFee(String carrier, int wg) {
        int base = "GHN".equals(carrier) ? 22000 : 18000;
        return base + Math.max(0, (wg - 500) / 100 * 1000);
    }

    // ── HTTP helpers ──────────────────────────────────────────────────────────

    private String get(String urlStr, String... headers) throws Exception {
        URL url = new URL(urlStr);
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        applyTrustAll(conn);
        conn.setRequestMethod("GET");
        for (int i = 0; i < headers.length - 1; i += 2) conn.setRequestProperty(headers[i], headers[i + 1]);
        conn.setConnectTimeout(15000); conn.setReadTimeout(15000);
        int code = conn.getResponseCode();
        StringBuilder sb = new StringBuilder();
        try (BufferedReader br = new BufferedReader(new InputStreamReader(
                code == 200 ? conn.getInputStream() : conn.getErrorStream(), StandardCharsets.UTF_8))) {
            String line; while ((line = br.readLine()) != null) sb.append(line);
        }
        if (code != 200) throw new Exception("HTTP " + code + ": " + sb);
        return sb.toString();
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

    private String encode(String s) {
        try { return java.net.URLEncoder.encode(s, "UTF-8"); } catch (Exception e) { return s; }
    }

    // ── Prefill ───────────────────────────────────────────────────────────────

    private void prefillFromOrder() {
        if (orderId < 0) return;
        Cursor c = db.getReadableDatabase().rawQuery(
                "SELECT c.full_name, c.phone, c.address FROM orders o LEFT JOIN customers c ON c.id=o.customer_id WHERE o.id=?",
                new String[]{String.valueOf(orderId)});
        try {
            if (c.moveToFirst()) {
                String name = c.getString(0), phone = c.getString(1), addr = c.getString(2);
                if (name  != null && !name.isEmpty())  etName.setText(name);
                if (phone != null && !phone.isEmpty()) etPhone.setText(phone);
                if (addr  != null && !addr.isEmpty())  etStreet.setText(addr);
            }
        } finally { c.close(); }
    }

    // ── UI helpers ────────────────────────────────────────────────────────────

    private TextView sectionLabel(String text) {
        TextView tv = tv(text, 11f, Typeface.BOLD, 0xFF64748B);
        tv.setAllCaps(false);
        LinearLayout.LayoutParams lp = lp(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        lp.setMargins(dp(16), dp(16), dp(16), dp(4)); tv.setLayoutParams(lp);
        return tv;
    }

    private LinearLayout card(LinearLayout parent) {
        LinearLayout card = new LinearLayout(this);
        card.setOrientation(LinearLayout.VERTICAL);
        LinearLayout.LayoutParams lp = lp(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        lp.setMargins(dp(12), 0, dp(12), 0);
        card.setLayoutParams(lp);
        card.setBackground(roundRect(0xFFFFFFFF, dp(14)));
        card.setPadding(dp(16), dp(16), dp(16), dp(16));
        if (parent != null) parent.addView(card);
        return card;
    }

    private EditText iconField(LinearLayout parent, String label, String hint, int inputType) {
        parent.addView(fieldLabel(label));
        EditText et = styledEdit(hint);
        et.setInputType(inputType);
        LinearLayout.LayoutParams lp = lp(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        lp.setMargins(0, dp(4), 0, dp(10)); et.setLayoutParams(lp);
        parent.addView(et);
        return et;
    }

    private TextView pickerView(String hint) {
        TextView tv = new TextView(this);
        tv.setText(hint);
        tv.setTextSize(14f);
        tv.setTextColor(0xFF94A3B8);
        tv.setBackground(getResources().getDrawable(R.drawable.bg_input_field));
        tv.setPadding(dp(12), dp(12), dp(12), dp(12));
        LinearLayout.LayoutParams lp = lp(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        lp.setMargins(0, dp(4), 0, dp(10)); tv.setLayoutParams(lp);
        tv.setClickable(true); tv.setFocusable(true);
        return tv;
    }

    private EditText styledEdit(String hint) {
        EditText et = new EditText(this);
        et.setHint(hint); et.setTextSize(14f);
        et.setBackground(getResources().getDrawable(R.drawable.bg_input_field));
        et.setPadding(dp(12), dp(10), dp(12), dp(10));
        et.setLayoutParams(lp(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        return et;
    }

    private TextView fieldLabel(String text) {
        TextView tv = tv(text, 12f, Typeface.NORMAL, 0xFF6B7280);
        LinearLayout.LayoutParams lp = lp(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        lp.setMargins(0, dp(2), 0, 0); tv.setLayoutParams(lp);
        return tv;
    }

    private TextView tv(String text, float size, int style, int color) {
        TextView tv = new TextView(this);
        tv.setText(text); tv.setTextSize(size); tv.setTypeface(null, style); tv.setTextColor(color);
        return tv;
    }

    private GradientDrawable roundRect(int fill, float radius) {
        GradientDrawable gd = new GradientDrawable();
        gd.setColor(fill); gd.setCornerRadius(radius); return gd;
    }

    private GradientDrawable roundRectStroke(int fill, float radius, int strokeColor, int strokeWidth) {
        GradientDrawable gd = new GradientDrawable();
        gd.setColor(fill); gd.setCornerRadius(radius);
        if (strokeColor != 0) gd.setStroke(strokeWidth, strokeColor);
        return gd;
    }

    private LinearLayout.LayoutParams lp(int w, int h) {
        return new LinearLayout.LayoutParams(w, h);
    }

    private int dp(int dp) {
        return Math.round(dp * getResources().getDisplayMetrics().density);
    }
}

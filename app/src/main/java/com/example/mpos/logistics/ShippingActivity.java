package com.example.mpos.logistics;

import android.content.Context;
import android.content.ContentValues;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.graphics.Typeface;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.ScrollView;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.example.mpos.R;
import com.example.mpos.database.DatabaseHelper;
import com.example.mpos.utils.CurrencyUtils;

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
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;

public class ShippingActivity extends AppCompatActivity {

    private static final String PREFS_SHIP   = "mpos_shipping_prefs";
    private static final String KEY_GHN_TOKEN  = "ghn_token";
    private static final String KEY_GHN_SHOP   = "ghn_shop_id";
    private static final String KEY_GHTK_TOKEN = "ghtk_token";
    private static final String KEY_VTP_TOKEN  = "vtp_token";

    private static final String GHN_FEE_URL    = "https://dev-online-gateway.ghn.vn/shiip/public-api/v2/shipping-order/fee";
    private static final String GHN_CREATE_URL = "https://dev-online-gateway.ghn.vn/shiip/public-api/v2/shipping-order/create";
    private static final String GHN_TRACK_URL  = "https://dev-online-gateway.ghn.vn/shiip/public-api/v2/shipping-order/detail";
    private static final String GHTK_FEE_URL   = "https://services.giaohangtietkiem.vn/services/shipment/fee";
    private static final String GHTK_CREATE_URL = "https://services.giaohangtietkiem.vn/services/shipment/order/?ver=1.5";
    private static final String GHTK_TRACK_URL  = "https://services.giaohangtietkiem.vn/services/shipment/v2/";
    private static final String VTP_FEE_URL     = "https://partner.viettelpost.vn/v2/order/getPriceAll";
    private static final String VTP_CREATE_URL  = "https://partner.viettelpost.vn/v2/order/createOrder";
    private static final String VTP_TRACK_URL   = "https://partner.viettelpost.vn/v2/order/getOrderByOrderNumber?ORDER_NUMBER=";

    // GHN province_id mapping (province name → GHN province_id)
    private static final Map<String, Integer> GHN_PROVINCE_ID = new HashMap<String, Integer>() {{
        put("Hà Nội", 201);           put("Hồ Chí Minh", 202);       put("Hải Phòng", 203);
        put("Đà Nẵng", 206);          put("Cần Thơ", 221);            put("An Giang", 222);
        put("Bà Rịa – Vũng Tàu", 208); put("Bắc Giang", 209);        put("Bắc Kạn", 210);
        put("Bạc Liêu", 223);         put("Bắc Ninh", 211);           put("Bến Tre", 224);
        put("Bình Định", 212);        put("Bình Dương", 213);          put("Bình Phước", 214);
        put("Bình Thuận", 215);       put("Cà Mau", 225);             put("Cao Bằng", 216);
        put("Đắk Lắk", 217);         put("Đắk Nông", 218);           put("Điện Biên", 219);
        put("Đồng Nai", 220);         put("Đồng Tháp", 226);          put("Gia Lai", 227);
        put("Hà Giang", 228);         put("Hà Nam", 229);             put("Hà Tĩnh", 230);
        put("Hải Dương", 231);        put("Hậu Giang", 232);          put("Hòa Bình", 233);
        put("Hưng Yên", 234);         put("Khánh Hòa", 235);          put("Kiên Giang", 236);
        put("Kon Tum", 237);          put("Lai Châu", 238);            put("Lâm Đồng", 239);
        put("Lạng Sơn", 240);         put("Lào Cai", 241);            put("Long An", 242);
        put("Nam Định", 243);         put("Nghệ An", 244);            put("Ninh Bình", 245);
        put("Ninh Thuận", 246);       put("Phú Thọ", 247);            put("Phú Yên", 248);
        put("Quảng Bình", 249);       put("Quảng Nam", 250);          put("Quảng Ngãi", 251);
        put("Quảng Ninh", 252);       put("Quảng Trị", 253);          put("Sóc Trăng", 254);
        put("Sơn La", 255);           put("Tây Ninh", 256);           put("Thái Bình", 257);
        put("Thái Nguyên", 258);      put("Thanh Hóa", 259);          put("Thừa Thiên Huế", 260);
        put("Tiền Giang", 261);       put("Trà Vinh", 262);           put("Tuyên Quang", 263);
        put("Vĩnh Long", 264);        put("Vĩnh Phúc", 265);          put("Yên Bái", 266);
    }};

    // ViettelPost province_id mapping
    private static final Map<String, Integer> VTP_PROVINCE_ID = new HashMap<String, Integer>() {{
        put("Hà Nội", 1);             put("Hồ Chí Minh", 2);          put("Hải Phòng", 3);
        put("Đà Nẵng", 4);            put("Cần Thơ", 5);              put("An Giang", 6);
        put("Bà Rịa – Vũng Tàu", 7); put("Bắc Giang", 8);            put("Bắc Kạn", 9);
        put("Bạc Liêu", 10);          put("Bắc Ninh", 11);            put("Bến Tre", 12);
        put("Bình Định", 13);         put("Bình Dương", 14);           put("Bình Phước", 15);
        put("Bình Thuận", 16);        put("Cà Mau", 17);              put("Cao Bằng", 18);
        put("Đắk Lắk", 19);          put("Đắk Nông", 20);            put("Điện Biên", 21);
        put("Đồng Nai", 22);          put("Đồng Tháp", 23);           put("Gia Lai", 24);
        put("Hà Giang", 25);          put("Hà Nam", 26);              put("Hà Tĩnh", 27);
        put("Hải Dương", 28);         put("Hậu Giang", 29);           put("Hòa Bình", 30);
        put("Hưng Yên", 31);          put("Khánh Hòa", 32);           put("Kiên Giang", 33);
        put("Kon Tum", 34);           put("Lai Châu", 35);             put("Lâm Đồng", 36);
        put("Lạng Sơn", 37);          put("Lào Cai", 38);             put("Long An", 39);
        put("Nam Định", 40);          put("Nghệ An", 41);             put("Ninh Bình", 42);
        put("Ninh Thuận", 43);        put("Phú Thọ", 44);             put("Phú Yên", 45);
        put("Quảng Bình", 46);        put("Quảng Nam", 47);           put("Quảng Ngãi", 48);
        put("Quảng Ninh", 49);        put("Quảng Trị", 50);           put("Sóc Trăng", 51);
        put("Sơn La", 52);            put("Tây Ninh", 53);             put("Thái Bình", 54);
        put("Thái Nguyên", 55);       put("Thanh Hóa", 56);           put("Thừa Thiên Huế", 57);
        put("Tiền Giang", 58);        put("Trà Vinh", 59);            put("Tuyên Quang", 60);
        put("Vĩnh Long", 61);         put("Vĩnh Phúc", 62);           put("Yên Bái", 63);
    }};

    private static final String[] PROVINCES = {
        "Hà Nội","Hồ Chí Minh","Đà Nẵng","Hải Phòng","Cần Thơ",
        "An Giang","Bà Rịa – Vũng Tàu","Bắc Giang","Bắc Kạn","Bạc Liêu",
        "Bắc Ninh","Bến Tre","Bình Định","Bình Dương","Bình Phước",
        "Bình Thuận","Cà Mau","Cao Bằng","Đắk Lắk","Đắk Nông",
        "Điện Biên","Đồng Nai","Đồng Tháp","Gia Lai","Hà Giang",
        "Hà Nam","Hà Tĩnh","Hải Dương","Hậu Giang","Hòa Bình",
        "Hưng Yên","Khánh Hòa","Kiên Giang","Kon Tum","Lai Châu",
        "Lâm Đồng","Lạng Sơn","Lào Cai","Long An","Nam Định",
        "Nghệ An","Ninh Bình","Ninh Thuận","Phú Thọ","Phú Yên",
        "Quảng Bình","Quảng Nam","Quảng Ngãi","Quảng Ninh","Quảng Trị",
        "Sóc Trăng","Sơn La","Tây Ninh","Thái Bình","Thái Nguyên",
        "Thanh Hóa","Thừa Thiên Huế","Tiền Giang","Trà Vinh","Tuyên Quang",
        "Vĩnh Long","Vĩnh Phúc","Yên Bái"
    };

    private Spinner spinnerFromProvince, spinnerToProvince;
    private EditText etWeight, etCod, etTrackCode;
    private LinearLayout layoutResults;
    private ProgressBar progressCompare, progressTrack;
    private LinearLayout layoutTrackResult;
    private LinearLayout layoutPendingOrders;
    private DatabaseHelper db;

    // Passed from OrderDetailActivity / ReceiptActivity
    private long sourceOrderId = -1;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_shipping);

        db = new DatabaseHelper(this);

        spinnerFromProvince = findViewById(R.id.spinnerFromProvince);
        spinnerToProvince   = findViewById(R.id.spinnerToProvince);
        etWeight            = findViewById(R.id.etWeight);
        etCod               = findViewById(R.id.etCod);
        layoutResults       = findViewById(R.id.layoutShippingResults);
        progressCompare     = findViewById(R.id.progressShipping);
        etTrackCode         = findViewById(R.id.etTrackCode);
        progressTrack       = findViewById(R.id.progressTrack);
        layoutTrackResult   = findViewById(R.id.layoutTrackResult);
        layoutPendingOrders = findViewById(R.id.layoutPendingOrders);

        ArrayAdapter<String> adapter = new ArrayAdapter<>(this,
            android.R.layout.simple_spinner_item, PROVINCES);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerFromProvince.setAdapter(adapter);
        spinnerToProvince.setAdapter(adapter);
        spinnerToProvince.setSelection(0);
        spinnerFromProvince.setSelection(1);

        String preWeight = getIntent().getStringExtra("weight");
        String preCod    = getIntent().getStringExtra("cod_amount");
        sourceOrderId    = getIntent().getLongExtra("source_order_id", -1);
        if (preWeight != null) etWeight.setText(preWeight);
        if (preCod    != null) etCod.setText(preCod);

        findViewById(R.id.btnBack).setOnClickListener(v -> finish());
        findViewById(R.id.btnShippingSettings).setOnClickListener(v -> showSettingsDialog());
        findViewById(R.id.btnCompare).setOnClickListener(v -> compareRates());
        findViewById(R.id.btnTrack).setOnClickListener(v -> trackOrder());

        loadPendingOrders();
    }

    // ── Pending orders that need shipping ─────────────────────────────────────

    private void loadPendingOrders() {
        if (layoutPendingOrders == null) return;
        layoutPendingOrders.removeAllViews();

        Cursor c = db.getReadableDatabase().rawQuery(
            "SELECT o.id, o.order_code, o.total_amount, o.created_at, o.status " +
            "FROM orders o " +
            "WHERE o.status IN ('COMPLETED','PENDING') " +
            "AND o.id NOT IN (SELECT DISTINCT order_id FROM shipping_orders WHERE order_id IS NOT NULL) " +
            "ORDER BY o.created_at DESC LIMIT 5", null);

        try {
            if (!c.moveToFirst()) {
                layoutPendingOrders.setVisibility(View.GONE);
                return;
            }
            layoutPendingOrders.setVisibility(View.VISIBLE);
            SimpleDateFormat sdf = new SimpleDateFormat("dd/MM HH:mm", Locale.getDefault());

            do {
                long   orderId   = c.getLong(0);
                String orderCode = c.getString(1);
                long   total     = c.getLong(2);
                long   createdAt = c.getLong(3);
                String status    = c.getString(4);
                addPendingOrderRow(orderId, orderCode, total, createdAt, status, sdf);
            } while (c.moveToNext());
        } finally { c.close(); }
    }

    private void addPendingOrderRow(long orderId, String code, long total,
                                    long createdAt, String status, SimpleDateFormat sdf) {
        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(Gravity.CENTER_VERTICAL);
        LinearLayout.LayoutParams rowLp = new LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        rowLp.setMargins(0, 0, 0, dp(8));
        row.setLayoutParams(rowLp);
        row.setBackground(getResources().getDrawable(R.drawable.bg_card_white_12));
        row.setPadding(dp(12), dp(10), dp(12), dp(10));

        LinearLayout info = new LinearLayout(this);
        info.setOrientation(LinearLayout.VERTICAL);
        info.setLayoutParams(new LinearLayout.LayoutParams(0,
            ViewGroup.LayoutParams.WRAP_CONTENT, 1f));

        TextView tvCode = new TextView(this);
        tvCode.setText(code);
        tvCode.setTextSize(13f);
        tvCode.setTypeface(null, Typeface.BOLD);
        tvCode.setTextColor(0xFF1C2333);
        info.addView(tvCode);

        TextView tvMeta = new TextView(this);
        tvMeta.setText(sdf.format(new Date(createdAt)) + "  •  " + CurrencyUtils.vnd(total));
        tvMeta.setTextSize(11f);
        tvMeta.setTextColor(0xFF64748B);
        info.addView(tvMeta);

        row.addView(info);

        Button btnShip = new Button(this);
        btnShip.setText("Tạo vận đơn");
        btnShip.setTextSize(11f);
        btnShip.setTextColor(0xFF1A73E8);
        btnShip.setBackground(getResources().getDrawable(R.drawable.bg_blue_summary));
        btnShip.setPadding(dp(10), dp(6), dp(10), dp(6));
        LinearLayout.LayoutParams btnLp = new LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        btnShip.setLayoutParams(btnLp);
        btnShip.setOnClickListener(v -> confirmCreateShipping(orderId, code, total));
        row.addView(btnShip);

        layoutPendingOrders.addView(row);
    }

    private void confirmCreateShipping(long orderId, String orderCode, long cod) {
        // Count items for this order
        int itemCount = 0;
        try (android.database.Cursor ic = db.getReadableDatabase().rawQuery(
                "SELECT COUNT(*) FROM order_items WHERE order_id=?",
                new String[]{String.valueOf(orderId)})) {
            if (ic.moveToFirst()) itemCount = ic.getInt(0);
        } catch (Exception ignored) {}

        android.content.Intent intent = new android.content.Intent(this, CreateShippingOrderActivity.class);
        intent.putExtra("order_id",   orderId);
        intent.putExtra("cod_amount", cod);
        intent.putExtra("item_count", itemCount);
        intent.putExtra("order_code", orderCode);
        startActivity(intent);
    }

    // ── Rate comparison ───────────────────────────────────────────────────────

    private void compareRates() {
        String weightStr = etWeight.getText().toString().trim();
        if (weightStr.isEmpty()) {
            Toast.makeText(this, "Nhập cân nặng gói hàng", Toast.LENGTH_SHORT).show(); return;
        }
        double weightGram;
        try {
            weightGram = Double.parseDouble(weightStr) * 1000;
        } catch (NumberFormatException e) {
            Toast.makeText(this, "Cân nặng không hợp lệ", Toast.LENGTH_SHORT).show(); return;
        }

        int codAmount = 0;
        try { codAmount = Integer.parseInt(etCod.getText().toString().trim()); } catch (Exception ignored) {}

        String from = spinnerFromProvince.getSelectedItem().toString();
        String to   = spinnerToProvince.getSelectedItem().toString();

        layoutResults.setVisibility(View.GONE);
        progressCompare.setVisibility(View.VISIBLE);

        final int finalCod   = codAmount;
        final double finalWg = weightGram;
        final String finalFrom = from, finalTo = to;

        String ghnToken  = getPrefs().getString(KEY_GHN_TOKEN, "");
        String ghtkToken = getPrefs().getString(KEY_GHTK_TOKEN, "");
        String vtpToken  = getPrefs().getString(KEY_VTP_TOKEN, "");

        new Thread(() -> {
            ShippingResult ghn  = !ghnToken.isEmpty()
                ? fetchGhnRate(ghnToken, from, to, (int) finalWg, finalCod)
                : mockRate("GHN", from, to, finalWg / 1000.0, finalCod, 1.0);
            ShippingResult ghtk = !ghtkToken.isEmpty()
                ? fetchGhtkRate(ghtkToken, from, to, finalWg / 1000.0, finalCod)
                : mockRate("GHTK", from, to, finalWg / 1000.0, finalCod, 0.95);
            ShippingResult vtp  = !vtpToken.isEmpty()
                ? fetchVtpRate(vtpToken, from, to, (int) finalWg, finalCod)
                : mockRate("ViettelPost", from, to, finalWg / 1000.0, finalCod, 0.9);

            ShippingResult cheapest = cheapest(ghn, ghtk, vtp);

            runOnUiThread(() -> {
                progressCompare.setVisibility(View.GONE);
                layoutResults.setVisibility(View.VISIBLE);
                layoutResults.removeAllViews();
                addResultCard(ghn,  cheapest, from, to);
                addResultCard(ghtk, cheapest, from, to);
                addResultCard(vtp,  cheapest, from, to);
            });
        }).start();
    }

    // ── GHN API — uses province_id from mapping ────────────────────────────────
    private ShippingResult fetchGhnRate(String token, String from, String to,
                                         int weightGram, int codAmount) {
        try {
            int fromProvId = GHN_PROVINCE_ID.getOrDefault(from, 202);
            int toProvId   = GHN_PROVINCE_ID.getOrDefault(to,   201);
            // Use first district of the province as default (urban centre)
            int fromDist   = provinceToGhnDistrict(fromProvId);
            int toDist     = provinceToGhnDistrict(toProvId);

            JSONObject body = new JSONObject();
            body.put("service_type_id",   2);
            body.put("from_district_id",  fromDist);
            body.put("to_district_id",    toDist);
            body.put("to_ward_code",      "");
            body.put("height", 10);  body.put("length", 20);
            body.put("weight", weightGram); body.put("width", 15);
            body.put("insurance_value", codAmount);

            String resp = post(GHN_FEE_URL, body.toString(),
                "Token", token, "ShopId", getPrefs().getString(KEY_GHN_SHOP, "0"));
            JSONObject json = new JSONObject(resp);
            int fee = json.getJSONObject("data").getInt("total");
            return new ShippingResult("GHN", fee, "1-2 ngày", "ghn.vn", true);
        } catch (Exception e) {
            android.util.Log.e("GHN_API", "fetchGhnRate failed: " + e.getClass().getSimpleName() + ": " + e.getMessage(), e);
            return mockRate("GHN", from, to, weightGram / 1000.0, codAmount, 1.0);
        }
    }

    private int provinceToGhnDistrict(int provinceId) {
        // Return the main urban district ID for each province (simplified mapping)
        switch (provinceId) {
            case 201: return 1442; // Hà Nội → Hoàn Kiếm
            case 202: return 1453; // HCM → Quận 1
            case 203: return 1570; // Hải Phòng → Hồng Bàng
            case 206: return 1572; // Đà Nẵng → Hải Châu
            case 221: return 1601; // Cần Thơ → Ninh Kiều
            default:  return 1453;
        }
    }

    // ── GHTK API — province name works directly ────────────────────────────────
    private ShippingResult fetchGhtkRate(String token, String from, String to,
                                          double weightKg, int codAmount) {
        try {
            String urlStr = GHTK_FEE_URL
                + "?pick_province=" + encode(from)
                + "&pick_district=" + encode(from)
                + "&province=" + encode(to)
                + "&district=" + encode(to)
                + "&weight=" + (int)(weightKg * 1000)
                + "&value=" + codAmount
                + "&deliver_option=none";
            URL url = new URL(urlStr);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            if (conn instanceof HttpsURLConnection) {
                SSLContext sc = trustAllSslContext();
                if (sc != null) { ((HttpsURLConnection)conn).setSSLSocketFactory(sc.getSocketFactory()); ((HttpsURLConnection)conn).setHostnameVerifier((h,s)->true); }
            }
            conn.setRequestProperty("Token", token);
            conn.setConnectTimeout(15000); conn.setReadTimeout(15000);
            StringBuilder sb = new StringBuilder();
            try (BufferedReader br = new BufferedReader(new InputStreamReader(conn.getInputStream()))) {
                String line; while ((line = br.readLine()) != null) sb.append(line);
            }
            JSONObject json = new JSONObject(sb.toString());
            int fee = (int) json.getJSONObject("fee").getDouble("fee");
            return new ShippingResult("GHTK", fee, "1-2 ngày", "giaohangtietkiem.vn", true);
        } catch (Exception e) {
            return mockRate("GHTK", from, to, weightKg, codAmount, 0.95);
        }
    }

    // ── ViettelPost API — uses province_id from mapping ───────────────────────
    private ShippingResult fetchVtpRate(String token, String from, String to,
                                         int weightGram, int codAmount) {
        try {
            int fromId = VTP_PROVINCE_ID.getOrDefault(from, 2);
            int toId   = VTP_PROVINCE_ID.getOrDefault(to,   1);

            JSONObject body = new JSONObject();
            body.put("PRODUCT_WEIGHT",   weightGram);
            body.put("PRODUCT_PRICE",    codAmount);
            body.put("MONEY_COLLECTION", codAmount);
            body.put("SENDER_PROVINCE",   fromId);
            body.put("SENDER_DISTRICT",   0);
            body.put("RECEIVER_PROVINCE", toId);
            body.put("RECEIVER_DISTRICT", 0);
            body.put("PRODUCT_TYPE",  "HH");
            body.put("NATIONAL_TYPE", 1);

            String resp = post(VTP_FEE_URL, body.toString(), "Authorization", "Bearer " + token);
            JSONObject json = new JSONObject(resp);
            int fee = json.getJSONArray("data").getJSONObject(0).getInt("GIA_CUOC");
            return new ShippingResult("ViettelPost", fee, "1-3 ngày", "viettelpost.vn", true);
        } catch (Exception e) {
            return mockRate("ViettelPost", from, to, weightGram / 1000.0, codAmount, 0.9);
        }
    }

    // ── Mock rates (formula) ──────────────────────────────────────────────────
    private ShippingResult mockRate(String carrier, String from, String to,
                                    double weightKg, int cod, double factor) {
        int baseRate  = calcBaseRate(from, to);
        int weightFee = (int) Math.max(0, (weightKg - 0.5) * 5000);
        int codFee    = (int) (cod * 0.01);
        int total     = (int) ((baseRate + weightFee + codFee) * factor);
        String eta    = calcEta(from, to);
        String domain = carrier.equals("GHN") ? "ghn.vn"
                      : carrier.equals("GHTK") ? "giaohangtietkiem.vn"
                      : "viettelpost.vn";
        return new ShippingResult(carrier, total, eta, domain, false);
    }

    private int calcBaseRate(String from, String to) {
        boolean fN = isNorth(from), fS = isSouth(from);
        boolean tN = isNorth(to),   tS = isSouth(to);
        if (from.equals(to)) return 20000;
        if (fN && tN || fS && tS) return 27000;
        if ((fN && tS) || (fS && tN)) return 52000;
        return 35000;
    }

    private String calcEta(String from, String to) {
        if (from.equals(to)) return "Trong ngày";
        boolean cross = (isNorth(from) && isSouth(to)) || (isSouth(from) && isNorth(to));
        return cross ? "2-4 ngày" : "1-2 ngày";
    }

    private boolean isNorth(String p) {
        return p.equals("Hà Nội") || p.equals("Hải Phòng") || p.equals("Bắc Giang")
            || p.equals("Bắc Ninh") || p.equals("Hà Nam") || p.equals("Hải Dương")
            || p.equals("Hưng Yên") || p.equals("Nam Định") || p.equals("Ninh Bình")
            || p.equals("Quảng Ninh") || p.equals("Thái Bình") || p.equals("Vĩnh Phúc");
    }
    private boolean isSouth(String p) {
        return p.equals("Hồ Chí Minh") || p.equals("Cần Thơ") || p.equals("Đồng Nai")
            || p.equals("Bình Dương") || p.equals("Bà Rịa – Vũng Tàu") || p.equals("Long An")
            || p.equals("Tiền Giang") || p.equals("Vĩnh Long") || p.equals("Đồng Tháp")
            || p.equals("An Giang") || p.equals("Kiên Giang") || p.equals("Hậu Giang")
            || p.equals("Trà Vinh") || p.equals("Sóc Trăng") || p.equals("Bạc Liêu")
            || p.equals("Cà Mau") || p.equals("Bến Tre");
    }

    private ShippingResult cheapest(ShippingResult... results) {
        ShippingResult best = null;
        for (ShippingResult r : results) {
            if (r.fee < 0) continue;
            if (best == null || r.fee < best.fee) best = r;
        }
        return best;
    }

    // ── Result card UI ────────────────────────────────────────────────────────

    private void addResultCard(ShippingResult result, ShippingResult cheapest,
                               String from, String to) {
        boolean isBest = cheapest != null && result.carrier.equals(cheapest.carrier);

        LinearLayout card = new LinearLayout(this);
        card.setOrientation(LinearLayout.VERTICAL);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        lp.setMargins(0, 0, 0, dp(10));
        card.setLayoutParams(lp);
        card.setBackground(getResources().getDrawable(
            isBest ? R.drawable.bg_card_orange_12 : R.drawable.bg_card_white_12));
        card.setElevation(isBest ? dp(4) : dp(1));
        card.setPadding(dp(16), dp(14), dp(16), dp(14));

        // Header
        LinearLayout header = new LinearLayout(this);
        header.setOrientation(LinearLayout.HORIZONTAL);
        header.setGravity(Gravity.CENTER_VERTICAL);
        header.setLayoutParams(new LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));

        TextView tvCarrier = new TextView(this);
        tvCarrier.setText(result.carrier);
        tvCarrier.setTextSize(15f);
        tvCarrier.setTypeface(null, Typeface.BOLD);
        tvCarrier.setTextColor(isBest ? 0xFFFFFFFF : 0xFF1F2937);
        tvCarrier.setLayoutParams(new LinearLayout.LayoutParams(0,
            ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
        header.addView(tvCarrier);

        if (isBest) {
            TextView badge = new TextView(this);
            badge.setText("RẺ NHẤT");
            badge.setTextSize(10f);
            badge.setTypeface(null, Typeface.BOLD);
            badge.setTextColor(0xFFF57C00);
            badge.setBackground(getResources().getDrawable(R.drawable.bg_card_white_12));
            badge.setPadding(dp(8), dp(4), dp(8), dp(4));
            header.addView(badge);
        }

        if (!result.isRealApi) {
            TextView mock = new TextView(this);
            mock.setText("*ước tính");
            mock.setTextSize(10f);
            mock.setTextColor(isBest ? 0xFFFFE0B2 : 0xFF9CA3AF);
            mock.setLayoutParams(new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT));
            mock.setGravity(Gravity.END);
            header.addView(mock);
        }
        card.addView(header);

        // Price row
        LinearLayout priceRow = new LinearLayout(this);
        priceRow.setOrientation(LinearLayout.HORIZONTAL);
        priceRow.setGravity(Gravity.CENTER_VERTICAL);
        LinearLayout.LayoutParams priceRowLp = new LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        priceRowLp.setMargins(0, dp(8), 0, 0);
        priceRow.setLayoutParams(priceRowLp);

        TextView tvPrice = new TextView(this);
        tvPrice.setText(result.fee >= 0 ? CurrencyUtils.vnd(result.fee) : "Không khả dụng");
        tvPrice.setTextSize(22f);
        tvPrice.setTypeface(null, Typeface.BOLD);
        tvPrice.setTextColor(isBest ? 0xFFFFFFFF : 0xFFF57C00);
        tvPrice.setLayoutParams(new LinearLayout.LayoutParams(0,
            ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
        priceRow.addView(tvPrice);

        LinearLayout etaBox = new LinearLayout(this);
        etaBox.setOrientation(LinearLayout.VERTICAL);
        etaBox.setGravity(Gravity.END);

        TextView tvEta = new TextView(this);
        tvEta.setText(result.eta);
        tvEta.setTextSize(13f);
        tvEta.setTextColor(isBest ? 0xFFFFE0B2 : 0xFF6B7280);
        etaBox.addView(tvEta);

        TextView tvDomain = new TextView(this);
        tvDomain.setText(result.domain);
        tvDomain.setTextSize(11f);
        tvDomain.setTextColor(isBest ? 0xFFFFE0B2 : 0xFF9CA3AF);
        etaBox.addView(tvDomain);
        priceRow.addView(etaBox);
        card.addView(priceRow);

        // Route
        TextView tvRoute = new TextView(this);
        tvRoute.setText(from + " → " + to);
        tvRoute.setTextSize(12f);
        tvRoute.setTextColor(isBest ? 0xFFFFE0B2 : 0xFF6B7280);
        LinearLayout.LayoutParams routeLp = new LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        routeLp.setMargins(0, dp(6), 0, 0);
        tvRoute.setLayoutParams(routeLp);
        card.addView(tvRoute);

        // "Chọn" button if a source order is pending
        if (sourceOrderId > 0 && result.fee > 0) {
            final ShippingResult finalResult = result;
            Button btnSelect = new Button(this);
            btnSelect.setText("Chọn " + result.carrier);
            btnSelect.setTextSize(12f);
            btnSelect.setTextColor(isBest ? 0xFFF57C00 : 0xFFFFFFFF);
            btnSelect.setBackgroundResource(isBest ? R.drawable.bg_card_white_12
                : R.drawable.bg_pos_cat_active);
            LinearLayout.LayoutParams selLp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, dp(40));
            selLp.setMargins(0, dp(10), 0, 0);
            btnSelect.setLayoutParams(selLp);
            btnSelect.setOnClickListener(v -> recordShippingOrder(finalResult));
            card.addView(btnSelect);
        }

        layoutResults.addView(card);
    }

    private void recordShippingOrder(ShippingResult r) {
        // Show dialog to collect recipient info before creating the real order
        android.view.View form = getLayoutInflater().inflate(
            android.R.layout.simple_list_item_2, null);

        // Build input form programmatically
        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setPadding(dp(20), dp(16), dp(20), dp(8));

        EditText etName    = makeInput(layout, "Tên người nhận *", "Nguyễn Văn A");
        EditText etPhone   = makeInput(layout, "Số điện thoại *", "0901234567");
        EditText etAddress = makeInput(layout, "Địa chỉ giao hàng *", "123 Đường ABC, Phường XYZ");
        EditText etDistrict = makeInput(layout, "Quận/Huyện *", "Quận 1");

        String codStr = etCod.getText().toString().trim();

        new AlertDialog.Builder(this)
            .setTitle("Tạo vận đơn " + r.carrier)
            .setView(layout)
            .setPositiveButton("Tạo đơn", (d, w) -> {
                String name     = etName.getText().toString().trim();
                String phone    = etPhone.getText().toString().trim();
                String address  = etAddress.getText().toString().trim();
                String district = etDistrict.getText().toString().trim();

                if (name.isEmpty() || phone.isEmpty() || address.isEmpty()) {
                    Toast.makeText(this, "Vui lòng điền đầy đủ thông tin", Toast.LENGTH_SHORT).show();
                    return;
                }

                int cod = 0;
                try { cod = Integer.parseInt(codStr); } catch (Exception ignored) {}
                int wg = 500;
                try { wg = (int)(Double.parseDouble(etWeight.getText().toString()) * 1000); }
                catch (Exception ignored2) {}

                String from = spinnerFromProvince.getSelectedItem().toString();
                String to   = spinnerToProvince.getSelectedItem().toString();
                final int finalWg = wg, finalCod = cod;
                final long finalOrderId = sourceOrderId;

                progressCompare.setVisibility(View.VISIBLE);
                new Thread(() -> {
                    String trackingCode = null;
                    String ghnToken  = getPrefs().getString(KEY_GHN_TOKEN, "");
                    String ghtkToken = getPrefs().getString(KEY_GHTK_TOKEN, "");

                    try {
                        if ("GHN".equals(r.carrier) && !ghnToken.isEmpty()) {
                            trackingCode = createGhnOrder(ghnToken,
                                getPrefs().getString(KEY_GHN_SHOP, "0"),
                                name, phone, address, district, to, finalWg, finalCod);
                        } else if ("GHTK".equals(r.carrier) && !ghtkToken.isEmpty()) {
                            trackingCode = createGhtkOrder(ghtkToken,
                                name, phone, address, district, from, to, finalWg, finalCod);
                        }
                    } catch (Exception e) {
                        android.util.Log.e("ShippingActivity", "Create order error: " + e.getMessage());
                    }

                    final boolean apiSuccess = trackingCode != null;
                    final String finalTrackCode = apiSuccess ? trackingCode
                        : (r.carrier.substring(0, Math.min(3, r.carrier.length())).toUpperCase()
                           + "-" + System.currentTimeMillis() % 100000);

                    ContentValues cv = new ContentValues();
                    cv.put("order_id",      finalOrderId);
                    cv.put("carrier",       r.carrier);
                    cv.put("tracking_code", finalTrackCode);
                    cv.put("shipping_fee",  r.fee);
                    cv.put("from_province", from);
                    cv.put("to_province",   to);
                    cv.put("weight_gram",   finalWg);
                    cv.put("status",        apiSuccess ? "CONFIRMED" : "BOOKED");
                    cv.put("created_at",    System.currentTimeMillis());
                    db.getWritableDatabase().insert("shipping_orders", null, cv);

                    runOnUiThread(() -> {
                        progressCompare.setVisibility(View.GONE);
                        String msg = apiSuccess
                            ? "Tạo vận đơn thành công!\nMã: " + finalTrackCode
                            : "Đã lưu vận đơn (chưa có API token — mã tạm: " + finalTrackCode + ")";
                        new AlertDialog.Builder(this)
                            .setTitle("Vận đơn " + r.carrier)
                            .setMessage(msg)
                            .setPositiveButton("Theo dõi đơn", (dd, ww) -> {
                                etTrackCode.setText(finalTrackCode);
                                trackOrder();
                            })
                            .setNegativeButton("Đóng", null)
                            .show();
                        sourceOrderId = -1;
                        loadPendingOrders();
                    });
                }).start();
            })
            .setNegativeButton("Hủy", null)
            .show();
    }

    private EditText makeInput(LinearLayout parent, String hint, String placeholder) {
        TextView label = new TextView(this);
        label.setText(hint);
        label.setTextSize(12f);
        label.setTextColor(0xFF6B7280);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        lp.setMargins(0, dp(8), 0, 0);
        label.setLayoutParams(lp);
        parent.addView(label);

        EditText et = new EditText(this);
        et.setHint(placeholder);
        et.setTextSize(14f);
        et.setBackground(getResources().getDrawable(R.drawable.bg_input_field));
        et.setPadding(dp(12), dp(10), dp(12), dp(10));
        et.setLayoutParams(new LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        parent.addView(et);
        return et;
    }

    // ── GHN Create Order API ───────────────────────────────────────────────────
    private String createGhnOrder(String token, String shopId,
            String recipientName, String phone, String address, String district,
            String toProvince, int weightGram, int codAmount) throws Exception {
        int toProvId = GHN_PROVINCE_ID.getOrDefault(toProvince, 202);
        int toDist   = provinceToGhnDistrict(toProvId);

        org.json.JSONObject body = new org.json.JSONObject();
        body.put("to_name",        recipientName);
        body.put("to_phone",       phone);
        body.put("to_address",     address + ", " + district);
        body.put("to_ward_code",   "");
        body.put("to_district_id", toDist);
        body.put("weight",         weightGram);
        body.put("length",         20);
        body.put("width",          15);
        body.put("height",         10);
        body.put("service_type_id", 2);
        body.put("payment_type_id", 1);
        body.put("cod_amount",      codAmount);
        body.put("required_note",   "CHOXEMHANGKHONGTHU");

        org.json.JSONArray items = new org.json.JSONArray();
        org.json.JSONObject item = new org.json.JSONObject();
        item.put("name",     "Hàng hóa");
        item.put("quantity", 1);
        item.put("weight",   weightGram);
        items.put(item);
        body.put("items", items);

        String resp = post(GHN_CREATE_URL, body.toString(),
            "Token", token, "ShopId", shopId);
        return new org.json.JSONObject(resp).getJSONObject("data").getString("order_code");
    }

    // ── GHTK Create Order API ──────────────────────────────────────────────────
    private String createGhtkOrder(String token,
            String recipientName, String phone, String address, String district,
            String fromProvince, String toProvince, int weightGram, int codAmount) throws Exception {
        org.json.JSONObject orderObj = new org.json.JSONObject();
        orderObj.put("id",            "MPOS-" + System.currentTimeMillis());
        orderObj.put("pick_name",     "Cửa hàng");
        orderObj.put("pick_address",  "Địa chỉ lấy hàng");
        orderObj.put("pick_province", fromProvince);
        orderObj.put("pick_district", fromProvince);
        orderObj.put("pick_tel",      "0900000000");
        orderObj.put("tel",           phone);
        orderObj.put("name",          recipientName);
        orderObj.put("address",       address);
        orderObj.put("province",      toProvince);
        orderObj.put("district",      district);
        orderObj.put("weight",        weightGram);
        orderObj.put("value",         codAmount);
        orderObj.put("pick_money",    codAmount);
        orderObj.put("note",          "");

        org.json.JSONArray products = new org.json.JSONArray();
        org.json.JSONObject p = new org.json.JSONObject();
        p.put("name", "Hàng hóa"); p.put("weight", weightGram); p.put("quantity", 1);
        products.put(p);

        org.json.JSONObject body = new org.json.JSONObject();
        body.put("order",    orderObj);
        body.put("products", products);

        String resp = post(GHTK_CREATE_URL, body.toString(), "Token", token);
        org.json.JSONObject json = new org.json.JSONObject(resp);
        return json.getJSONObject("order").getString("label");
    }

    // ── Order tracking ────────────────────────────────────────────────────────

    private void trackOrder() {
        String code = etTrackCode.getText().toString().trim();
        if (code.isEmpty()) {
            Toast.makeText(this, "Nhập mã vận đơn", Toast.LENGTH_SHORT).show(); return;
        }
        progressTrack.setVisibility(View.VISIBLE);
        layoutTrackResult.setVisibility(View.GONE);

        String ghnToken  = getPrefs().getString(KEY_GHN_TOKEN, "");
        String ghtkToken = getPrefs().getString(KEY_GHTK_TOKEN, "");
        String vtpToken  = getPrefs().getString(KEY_VTP_TOKEN, "");

        new Thread(() -> {
            TrackResult result;
            // Auto-detect carrier from code prefix, or try each API
            if (code.startsWith("J") && !ghnToken.isEmpty()) {
                result = trackGhn(ghnToken, code);
            } else if ((code.startsWith("S") || code.startsWith("GHTK")) && !ghtkToken.isEmpty()) {
                result = trackGhtk(ghtkToken, code);
            } else if ((code.startsWith("VTP") || code.startsWith("8")) && !vtpToken.isEmpty()) {
                result = trackVtp(vtpToken, code);
            } else if (!ghnToken.isEmpty()) {
                result = trackGhn(ghnToken, code);
            } else {
                result = mockTrack(code);
            }
            runOnUiThread(() -> {
                progressTrack.setVisibility(View.GONE);
                // Open full tracking screen
                android.content.Intent intent = new android.content.Intent(this, TrackingDetailActivity.class);
                intent.putExtra("tracking_code", result.code);
                intent.putExtra("carrier",       result.carrier);
                startActivity(intent);
            });
        }).start();
    }

    private TrackResult trackGhn(String token, String orderCode) {
        try {
            JSONObject body = new JSONObject();
            body.put("order_code", orderCode);
            String resp = post(GHN_TRACK_URL, body.toString(), "Token", token);
            JSONObject json = new JSONObject(resp);
            JSONObject data = json.getJSONObject("data");
            String status  = data.getString("status");
            String updated = data.optString("updated_date", "—");
            return new TrackResult(orderCode, "GHN", statusLabel(status), updated, true);
        } catch (Exception e) {
            return new TrackResult(orderCode, "GHN", "Không tìm thấy", "—", true);
        }
    }

    private TrackResult trackGhtk(String token, String labelId) {
        try {
            URL url = new URL(GHTK_TRACK_URL + labelId);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestProperty("Token", token);
            conn.setConnectTimeout(15000); conn.setReadTimeout(15000);
            StringBuilder sb = new StringBuilder();
            try (BufferedReader br = new BufferedReader(
                    new InputStreamReader(conn.getInputStream()))) {
                String line; while ((line = br.readLine()) != null) sb.append(line);
            }
            JSONObject json  = new JSONObject(sb.toString());
            JSONObject data  = json.getJSONObject("shipment_tracking");
            String status    = data.optString("status_text", "—");
            String updated   = data.optString("updated_date", "—");
            return new TrackResult(labelId, "GHTK", status, updated, true);
        } catch (Exception e) {
            return new TrackResult(labelId, "GHTK", "Không tìm thấy", "—", true);
        }
    }

    private TrackResult trackVtp(String token, String orderNumber) {
        try {
            URL url = new URL(VTP_TRACK_URL + orderNumber);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestProperty("Authorization", "Bearer " + token);
            conn.setConnectTimeout(15000); conn.setReadTimeout(15000);
            StringBuilder sb = new StringBuilder();
            try (BufferedReader br = new BufferedReader(
                    new InputStreamReader(conn.getInputStream()))) {
                String line; while ((line = br.readLine()) != null) sb.append(line);
            }
            JSONObject json  = new JSONObject(sb.toString());
            JSONObject data  = json.optJSONObject("data");
            if (data == null) throw new Exception("no data");
            String status    = data.optString("ORDER_STATUSDESCRIPTION", "—");
            String updated   = data.optString("LAST_UPDATE", "—");
            return new TrackResult(orderNumber, "ViettelPost", status, updated, true);
        } catch (Exception e) {
            return new TrackResult(orderNumber, "ViettelPost", "Không tìm thấy", "—", true);
        }
    }

    private TrackResult mockTrack(String code) {
        String carrier = code.startsWith("GHN")  ? "GHN"
                       : code.startsWith("GHTK") ? "GHTK"
                       : code.startsWith("VTP")  ? "ViettelPost"
                       : "Không xác định";
        String status = code.length() > 8 ? "Đang vận chuyển" : "Chờ lấy hàng";
        return new TrackResult(code, carrier, status, "Hôm nay, 09:30", false);
    }

    private void showTrackResult(TrackResult r) {
        layoutTrackResult.removeAllViews();

        TextView tvTitle = new TextView(this);
        tvTitle.setText("Vận đơn: " + r.code);
        tvTitle.setTextSize(14f);
        tvTitle.setTypeface(null, Typeface.BOLD);
        tvTitle.setTextColor(0xFF1F2937);
        layoutTrackResult.addView(tvTitle);

        addTrackRow("Đơn vị:", r.carrier);
        addTrackRow("Trạng thái:", r.status);
        addTrackRow("Cập nhật:", r.updatedAt);

        if (!r.isRealApi) {
            TextView note = new TextView(this);
            note.setText("Dữ liệu mô phỏng — nhập GHN token để tra cứu thật");
            note.setTextSize(11f);
            note.setTextColor(0xFFF59E0B);
            note.setPadding(0, dp(6), 0, 0);
            layoutTrackResult.addView(note);
        }
        layoutTrackResult.setVisibility(View.VISIBLE);
    }

    private void addTrackRow(String label, String value) {
        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        lp.setMargins(0, dp(4), 0, 0);
        row.setLayoutParams(lp);

        TextView tvLabel = new TextView(this);
        tvLabel.setText(label);
        tvLabel.setTextSize(13f);
        tvLabel.setTextColor(0xFF6B7280);
        tvLabel.setLayoutParams(new LinearLayout.LayoutParams(dp(140),
            ViewGroup.LayoutParams.WRAP_CONTENT));
        row.addView(tvLabel);

        TextView tvValue = new TextView(this);
        tvValue.setText(value);
        tvValue.setTextSize(13f);
        tvValue.setTypeface(null, Typeface.BOLD);
        tvValue.setTextColor(0xFF1F2937);
        row.addView(tvValue);

        layoutTrackResult.addView(row);
    }

    private String statusLabel(String s) {
        if (s == null) return "—";
        switch (s) {
            case "ready_to_pick":      return "Chờ lấy hàng";
            case "picking":            return "Đang lấy hàng";
            case "cancel":             return "Đã huỷ";
            case "money_collect_pick": return "Đang thu tiền";
            case "picked":             return "Đã lấy hàng";
            case "storing":            return "Đang lưu kho";
            case "transporting":       return "Đang vận chuyển";
            case "sorting":            return "Đang phân loại";
            case "delivering":         return "Đang giao hàng";
            case "delivered":          return "Đã giao hàng";
            case "delivery_fail":      return "Giao thất bại";
            case "return":             return "Đang hoàn hàng";
            case "returned":           return "Đã hoàn hàng";
            default:                   return s;
        }
    }

    // ── Settings dialog ───────────────────────────────────────────────────────

    private void showSettingsDialog() {
        SharedPreferences prefs = getPrefs();
        android.view.View view = getLayoutInflater().inflate(R.layout.dialog_shipping_settings, null);
        EditText etGhnToken  = view.findViewById(R.id.etGhnToken);
        EditText etGhnShop   = view.findViewById(R.id.etGhnShop);
        EditText etGhtkToken = view.findViewById(R.id.etGhtkToken);
        EditText etVtpToken  = view.findViewById(R.id.etVtpToken);

        etGhnToken.setText(prefs.getString(KEY_GHN_TOKEN,  ""));
        etGhnShop.setText(prefs.getString(KEY_GHN_SHOP,    ""));
        etGhtkToken.setText(prefs.getString(KEY_GHTK_TOKEN, ""));
        etVtpToken.setText(prefs.getString(KEY_VTP_TOKEN,   ""));

        new AlertDialog.Builder(this)
            .setTitle("Cấu hình API vận chuyển")
            .setView(view)
            .setPositiveButton("Lưu", (d, w) -> {
                prefs.edit()
                    .putString(KEY_GHN_TOKEN,   etGhnToken.getText().toString().trim())
                    .putString(KEY_GHN_SHOP,    etGhnShop.getText().toString().trim())
                    .putString(KEY_GHTK_TOKEN,  etGhtkToken.getText().toString().trim())
                    .putString(KEY_VTP_TOKEN,   etVtpToken.getText().toString().trim())
                    .apply();
                Toast.makeText(this, "Đã lưu cấu hình API", Toast.LENGTH_SHORT).show();
            })
            .setNegativeButton("Hủy", null)
            .show();
    }

    // ── HTTP helper ───────────────────────────────────────────────────────────

    private static SSLContext trustAllSslContext() {
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

    private String post(String urlStr, String body, String... headers) throws Exception {
        URL url = new URL(urlStr);
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        // Carrier dev endpoints use certificates not in Android's trust store — bypass for SDK.
        if (conn instanceof HttpsURLConnection) {
            SSLContext sc = trustAllSslContext();
            if (sc != null) {
                ((HttpsURLConnection) conn).setSSLSocketFactory(sc.getSocketFactory());
                ((HttpsURLConnection) conn).setHostnameVerifier((h, s) -> true);
            }
        }
        conn.setRequestMethod("POST");
        conn.setRequestProperty("Content-Type", "application/json");
        for (int i = 0; i < headers.length - 1; i += 2)
            conn.setRequestProperty(headers[i], headers[i + 1]);
        conn.setDoOutput(true);
        conn.setConnectTimeout(15000); conn.setReadTimeout(15000);
        try (OutputStream os = conn.getOutputStream()) {
            os.write(body.getBytes(StandardCharsets.UTF_8));
        }
        int code = conn.getResponseCode();
        StringBuilder sb = new StringBuilder();
        try (BufferedReader br = new BufferedReader(new InputStreamReader(
                code == 200 ? conn.getInputStream() : conn.getErrorStream(),
                StandardCharsets.UTF_8))) {
            String line; while ((line = br.readLine()) != null) sb.append(line);
        }
        if (code != 200) throw new Exception("HTTP " + code + ": " + sb);
        return sb.toString();
    }

    private String encode(String s) {
        try { return java.net.URLEncoder.encode(s, "UTF-8"); } catch (Exception e) { return s; }
    }

    private SharedPreferences getPrefs() {
        return getSharedPreferences(PREFS_SHIP, Context.MODE_PRIVATE);
    }

    private int dp(int dp) {
        return Math.round(dp * getResources().getDisplayMetrics().density);
    }

    // ── Data models ───────────────────────────────────────────────────────────

    private static class ShippingResult {
        final String carrier, eta, domain;
        final int    fee;
        final boolean isRealApi;
        ShippingResult(String carrier, int fee, String eta, String domain, boolean isReal) {
            this.carrier = carrier; this.fee = fee; this.eta = eta;
            this.domain = domain; this.isRealApi = isReal;
        }
    }

    private static class TrackResult {
        final String code, carrier, status, updatedAt;
        final boolean isRealApi;
        TrackResult(String code, String carrier, String status, String updatedAt, boolean isReal) {
            this.code = code; this.carrier = carrier; this.status = status;
            this.updatedAt = updatedAt; this.isRealApi = isReal;
        }
    }
}

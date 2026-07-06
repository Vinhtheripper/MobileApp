package com.example.mpos.omnichannel;

import android.app.ProgressDialog;
import android.content.ContentValues;
import android.content.Context;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.example.mpos.R;
import com.example.mpos.auth.SessionManager;
import com.example.mpos.database.DatabaseHelper;
import com.example.mpos.utils.CurrencyUtils;

import org.json.JSONArray;
import org.json.JSONObject;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class OmnichannelActivity extends AppCompatActivity {

    private static final String PREFS = "mpos_omnichannel";

    private static final String[] CHANNEL_KEYS  = {"shopee",    "tiktok",      "lazada"};
    private static final String[] CHANNEL_NAMES = {"Shopee",    "TikTok Shop", "Lazada"};
    private static final int[]    CHANNEL_COLOR  = {0xFFEE4D2D, 0xFF161823,    0xFF0F146D};
    private static final int[]    CHANNEL_LIGHT  = {0xFFFFF3F0, 0xFFF5F5F5,   0xFFF0F0FF};
    private static final String[] CHANNEL_INIT   = {"S",        "T",           "L"};

    private LinearLayout layoutChannels, layoutOrders, layoutFilterChips;
    private View contentChannels, contentOrders, tabIndicator;
    private TextView tabChannelsTv, tabOrdersTv;
    private String activeFilter = "all";
    private DatabaseHelper db;
    private SessionManager session;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_omnichannel);

        db      = new DatabaseHelper(this);
        session = new SessionManager(this);

        layoutChannels    = findViewById(R.id.layoutChannels);
        layoutOrders      = findViewById(R.id.layoutOrders);
        layoutFilterChips = findViewById(R.id.layoutFilterChips);
        contentChannels   = findViewById(R.id.contentChannels);
        contentOrders     = findViewById(R.id.contentOrders);
        tabIndicator      = findViewById(R.id.tabIndicator);
        tabChannelsTv     = findViewById(R.id.tabChannels);
        tabOrdersTv       = findViewById(R.id.tabOrders);

        findViewById(R.id.btnBack).setOnClickListener(v -> finish());
        tabChannelsTv.setOnClickListener(v -> switchTab(0));
        tabOrdersTv.setOnClickListener(v -> switchTab(1));

        buildChannelCards();
        buildFilterChips();
        loadOrders("all");
    }

    // ─── Tab switching ────────────────────────────────────────────────────────

    private void switchTab(int tab) {
        boolean ch = (tab == 0);
        contentChannels.setVisibility(ch ? View.VISIBLE : View.GONE);
        contentOrders.setVisibility(ch ? View.GONE : View.VISIBLE);
        tabChannelsTv.setTextColor(ch ? 0xFF2875FB : 0xFF64748B);
        tabChannelsTv.setTypeface(null, ch ? Typeface.BOLD : Typeface.NORMAL);
        tabOrdersTv.setTextColor(ch ? 0xFF64748B : 0xFF2875FB);
        tabOrdersTv.setTypeface(null, ch ? Typeface.NORMAL : Typeface.BOLD);
        tabIndicator.post(() -> {
            int w = ((ViewGroup) tabIndicator.getParent()).getWidth();
            FrameLayout.LayoutParams lp = (FrameLayout.LayoutParams) tabIndicator.getLayoutParams();
            lp.width = w / 2;
            tabIndicator.setLayoutParams(lp);
            tabIndicator.setTranslationX(ch ? 0 : w / 2f);
        });
    }

    // ─── Channel cards ────────────────────────────────────────────────────────

    private void buildChannelCards() {
        layoutChannels.removeAllViews();
        SharedPreferences p = prefs();
        int cnt = 0;
        for (String k : CHANNEL_KEYS) if (p.getBoolean(k + "_connected", false)) cnt++;

        // Banner
        LinearLayout banner = new LinearLayout(this);
        banner.setOrientation(LinearLayout.VERTICAL);
        LinearLayout.LayoutParams blp = new LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        blp.setMargins(0, 0, 0, dp(16));
        banner.setLayoutParams(blp);
        banner.setBackground(rounded(0xFFEEF3FF, 12));
        banner.setPadding(dp(16), dp(14), dp(16), dp(14));
        tv(banner, cnt > 0 ? cnt + "/" + CHANNEL_KEYS.length + " kênh đã kết nối"
            : "Chưa kết nối kênh nào", 14, cnt > 0 ? 0xFF10B981 : 0xFF2875FB, true);
        tv(banner, "Kết nối sàn TMĐT để đồng bộ đơn hàng về hệ thống", 12, 0xFF64748B, false)
            .setPadding(0, dp(4), 0, 0);
        layoutChannels.addView(banner);

        for (int i = 0; i < CHANNEL_KEYS.length; i++)
            addChannelCard(CHANNEL_KEYS[i], CHANNEL_NAMES[i],
                CHANNEL_COLOR[i], CHANNEL_LIGHT[i], CHANNEL_INIT[i]);

        tv(layoutChannels,
            "Shopee: Cần Partner ID + Partner Key + Shop ID + Access Token từ open.shopee.com\n" +
            "TikTok / Lazada: Nhập Access Token từ Seller Portal để kết nối thủ công.",
            11, 0xFF94A3B8, false).setPadding(dp(4), dp(12), dp(4), 0);
    }

    private void addChannelCard(String key, String name, int color, int light, String init) {
        SharedPreferences p  = prefs();
        boolean connected    = p.getBoolean(key + "_connected", false);
        String shopName      = p.getString(key + "_shop_name", "");
        String shopIdStr     = p.getString(key + "_shop_id", "");
        boolean isShopee     = "shopee".equals(key);

        LinearLayout card = new LinearLayout(this);
        card.setOrientation(LinearLayout.VERTICAL);
        LinearLayout.LayoutParams clp = new LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        clp.setMargins(0, 0, 0, dp(12));
        card.setLayoutParams(clp);
        card.setBackground(rounded(0xFFFFFFFF, 14));
        card.setElevation(dp(1));
        card.setPadding(dp(16), dp(16), dp(16), dp(16));

        // Top row: logo + info + action button
        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(Gravity.CENTER_VERTICAL);
        row.setLayoutParams(new LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));

        // Left accent
        View accent = new View(this);
        LinearLayout.LayoutParams alp = new LinearLayout.LayoutParams(dp(4), dp(56));
        alp.setMargins(0, 0, dp(14), 0);
        accent.setLayoutParams(alp);
        accent.setBackground(rounded(color, 2));
        row.addView(accent);

        // Circle logo
        LinearLayout logo = new LinearLayout(this);
        logo.setGravity(Gravity.CENTER);
        LinearLayout.LayoutParams llp = new LinearLayout.LayoutParams(dp(44), dp(44));
        llp.setMargins(0, 0, dp(12), 0);
        logo.setLayoutParams(llp);
        logo.setBackground(circle(light));
        TextView tvInit = new TextView(this);
        tvInit.setText(init);
        tvInit.setTextSize(20f);
        tvInit.setTypeface(null, Typeface.BOLD);
        tvInit.setTextColor(color);
        logo.addView(tvInit);
        row.addView(logo);

        // Info column
        LinearLayout info = new LinearLayout(this);
        info.setOrientation(LinearLayout.VERTICAL);
        info.setLayoutParams(new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
        tv(info, name, 15, 0xFF1C2333, true);
        if (connected && !shopName.isEmpty())
            tv(info, shopName + (shopIdStr.isEmpty() ? "" : "  #" + shopIdStr), 11, 0xFF64748B, false);
        TextView tvStatus = tv(info, connected ? "● Đã kết nối" : "○ Chưa kết nối",
            11, connected ? 0xFF10B981 : 0xFF94A3B8, false);
        tvStatus.setPadding(0, dp(3), 0, 0);
        row.addView(info);

        // Primary action button
        TextView btnPrimary = new TextView(this);
        btnPrimary.setText(connected ? "Quản lý" : "Kết nối");
        btnPrimary.setTextSize(12f);
        btnPrimary.setTypeface(null, Typeface.BOLD);
        btnPrimary.setTextColor(connected ? 0xFF64748B : 0xFFFFFFFF);
        btnPrimary.setBackground(connected ? outlined(0xFFCBD5E1, 18) : rounded(color, 18));
        btnPrimary.setPadding(dp(14), dp(8), dp(14), dp(8));
        btnPrimary.setClickable(true);
        btnPrimary.setFocusable(true);
        btnPrimary.setOnClickListener(v -> {
            if (connected) showManageDialog(key, name, shopName, shopIdStr);
            else if (isShopee) showShopeeConnectDialog();
            else showGenericConnectDialog(key, name, color);
        });
        row.addView(btnPrimary);
        card.addView(row);

        // Shopee sync button (only when connected)
        if (isShopee && connected) {
            TextView btnSync = new TextView(this);
            btnSync.setText("↻  Đồng bộ đơn hàng từ Shopee");
            btnSync.setTextSize(13f);
            btnSync.setTypeface(null, Typeface.BOLD);
            btnSync.setTextColor(0xFFEE4D2D);
            btnSync.setBackground(rounded(0xFFFFF3F0, 10));
            btnSync.setGravity(Gravity.CENTER);
            LinearLayout.LayoutParams slp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, dp(42));
            slp.setMargins(0, dp(12), 0, 0);
            btnSync.setLayoutParams(slp);
            btnSync.setClickable(true);
            btnSync.setFocusable(true);
            btnSync.setOnClickListener(v -> syncShopeeOrders());
            card.addView(btnSync);
        }

        layoutChannels.addView(card);
    }

    // ─── Shopee connect (real API verify) ────────────────────────────────────

    private void showShopeeConnectDialog() {
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(dp(24), dp(12), dp(24), dp(4));

        TextView hint = new TextView(this);
        hint.setText("Lấy thông tin tại: open.shopee.com\n→ Ứng dụng → Thông tin API");
        hint.setTextSize(12f);
        hint.setTextColor(0xFFEE4D2D);
        hint.setPadding(0, 0, 0, dp(14));
        root.addView(hint);

        SharedPreferences p = prefs();
        EditText etPid = makeInput(root, "Partner ID (số)");
        EditText etPk  = makeInput(root, "Partner Key");
        EditText etSid = makeInput(root, "Shop ID (số)");
        EditText etTok = makeInput(root, "Access Token");

        String savedPid = p.getString("shopee_partner_id", "");
        String savedPk  = p.getString("shopee_partner_key", "");
        String savedSid = p.getString("shopee_shop_id_raw", "");
        String savedTok = p.getString("shopee_token", "");
        if (!savedPid.isEmpty()) etPid.setText(savedPid);
        if (!savedPk.isEmpty())  etPk.setText(savedPk);
        if (!savedSid.isEmpty()) etSid.setText(savedSid);
        if (!savedTok.isEmpty()) etTok.setText(savedTok);

        new AlertDialog.Builder(this)
            .setTitle("Kết nối Shopee")
            .setView(root)
            .setPositiveButton("Xác thực & Kết nối", (d, w) -> {
                String pidStr = etPid.getText().toString().trim();
                String pkStr  = etPk.getText().toString().trim();
                String sidStr = etSid.getText().toString().trim();
                String tokStr = etTok.getText().toString().trim();
                if (pidStr.isEmpty() || pkStr.isEmpty() || sidStr.isEmpty() || tokStr.isEmpty()) {
                    Toast.makeText(this, "Vui lòng nhập đầy đủ thông tin", Toast.LENGTH_SHORT).show();
                    return;
                }
                try {
                    long pid = Long.parseLong(pidStr);
                    long sid = Long.parseLong(sidStr);
                    verifyShopeeAndSave(pid, pkStr, sid, sidStr, tokStr);
                } catch (NumberFormatException e) {
                    Toast.makeText(this, "Partner ID và Shop ID phải là số", Toast.LENGTH_SHORT).show();
                }
            })
            .setNegativeButton("Hủy", null)
            .show();
    }

    private void verifyShopeeAndSave(long pid, String pk, long sid, String sidStr, String tok) {
        ProgressDialog prog = new ProgressDialog(this);
        prog.setMessage("Đang xác thực với Shopee...");
        prog.setCancelable(false);
        prog.show();

        new Thread(() -> {
            try {
                ShopeeApiHelper api = new ShopeeApiHelper(pid, pk, sid, tok);
                String shopName = api.verifyAndGetShopName();

                runOnUiThread(() -> {
                    prog.dismiss();
                    prefs().edit()
                        .putBoolean("shopee_connected",    true)
                        .putString("shopee_partner_id",    String.valueOf(pid))
                        .putString("shopee_partner_key",   pk)
                        .putString("shopee_shop_id",       sidStr)
                        .putString("shopee_shop_id_raw",   sidStr)
                        .putString("shopee_token",         tok)
                        .putString("shopee_shop_name",     shopName)
                        .putLong("shopee_connected_at",    System.currentTimeMillis())
                        .apply();
                    Toast.makeText(this, "Kết nối thành công: " + shopName, Toast.LENGTH_SHORT).show();
                    buildChannelCards();
                });
            } catch (Exception e) {
                runOnUiThread(() -> {
                    prog.dismiss();
                    new AlertDialog.Builder(this)
                        .setTitle("Xác thực thất bại")
                        .setMessage("Lỗi: " + e.getMessage() +
                            "\n\nKiểm tra lại Partner ID, Partner Key, Shop ID và Access Token.")
                        .setPositiveButton("OK", null)
                        .show();
                });
            }
        }).start();
    }

    // ─── Shopee order sync ────────────────────────────────────────────────────

    private void syncShopeeOrders() {
        SharedPreferences p = prefs();
        String pidStr = p.getString("shopee_partner_id", "");
        String pk     = p.getString("shopee_partner_key", "");
        String sidStr = p.getString("shopee_shop_id_raw", "");
        String tok    = p.getString("shopee_token", "");

        if (pidStr.isEmpty() || pk.isEmpty() || sidStr.isEmpty() || tok.isEmpty()) {
            Toast.makeText(this, "Chưa có thông tin API Shopee", Toast.LENGTH_SHORT).show();
            return;
        }

        long pid, sid;
        try { pid = Long.parseLong(pidStr); sid = Long.parseLong(sidStr); }
        catch (Exception e) {
            Toast.makeText(this, "Thông tin API không hợp lệ", Toast.LENGTH_SHORT).show();
            return;
        }

        ProgressDialog prog = new ProgressDialog(this);
        prog.setMessage("Đang lấy đơn hàng từ Shopee (15 ngày gần nhất)...");
        prog.setCancelable(false);
        prog.show();

        final long finalPid = pid, finalSid = sid;
        final String finalPk = pk, finalTok = tok;

        new Thread(() -> {
            try {
                ShopeeApiHelper api = new ShopeeApiHelper(finalPid, finalPk, finalSid, finalTok);
                long now  = System.currentTimeMillis() / 1000;
                long from = now - 15L * 24 * 3600;

                // Step 1: get order SN list
                JSONObject listResp = api.getOrderList(from, now);
                JSONObject listData = listResp.optJSONObject("response");
                if (listData == null) throw new Exception("Không lấy được danh sách đơn: " +
                    listResp.optString("message", "unknown error"));

                JSONArray orderList = listData.optJSONArray("order_list");
                if (orderList == null || orderList.length() == 0) {
                    runOnUiThread(() -> { prog.dismiss();
                        Toast.makeText(this, "Không có đơn hàng trong 15 ngày qua", Toast.LENGTH_SHORT).show(); });
                    return;
                }

                // Step 2: get order details in batch (max 50 per call)
                int newCount = 0;
                int batchSize = 50;
                int total = orderList.length();

                for (int start = 0; start < total; start += batchSize) {
                    StringBuilder sns = new StringBuilder();
                    for (int j = start; j < Math.min(start + batchSize, total); j++) {
                        if (j > start) sns.append(",");
                        sns.append(orderList.getJSONObject(j).getString("order_sn"));
                    }

                    JSONObject detailResp = api.getOrderDetail(sns.toString());
                    JSONObject detailData = detailResp.optJSONObject("response");
                    if (detailData == null) continue;

                    JSONArray details = detailData.optJSONArray("order_list");
                    if (details == null) continue;

                    long shopId = session.getShopId();
                    long userId = session.getUser() != null ? session.getUser().id : 1;
                    SQLiteDatabase wdb = db.getWritableDatabase();

                    for (int i = 0; i < details.length(); i++) {
                        JSONObject o   = details.getJSONObject(i);
                        String sn      = o.optString("order_sn", "");
                        String status  = mapShopeeStatus(o.optString("order_status", ""));
                        long amount    = (long) o.optDouble("total_amount", 0);
                        long createSec = o.optLong("create_time",
                            o.optLong("pay_time", System.currentTimeMillis() / 1000));

                        ContentValues cv = new ContentValues();
                        cv.put("order_code",      "SHP-" + sn);
                        cv.put("user_id",         userId);
                        cv.put("shop_id",         shopId);
                        cv.put("channel",         "SHOPEE");
                        cv.put("status",          status);
                        cv.put("subtotal",        amount);
                        cv.put("discount_amount", 0);
                        cv.put("vat_percent",     0);
                        cv.put("vat_amount",      0);
                        cv.put("total_amount",    amount);
                        cv.put("note",            "Shopee #" + sn);
                        cv.put("created_at",      createSec * 1000L);
                        cv.put("updated_at",      System.currentTimeMillis());

                        long result = wdb.insertWithOnConflict(
                            "orders", null, cv, SQLiteDatabase.CONFLICT_IGNORE);
                        if (result > 0) newCount++;
                    }
                }

                final int finalNew = newCount;
                runOnUiThread(() -> {
                    prog.dismiss();
                    Toast.makeText(this,
                        finalNew > 0
                            ? "Đã đồng bộ " + finalNew + " đơn mới từ Shopee"
                            : "Không có đơn mới (tất cả đã được đồng bộ)",
                        Toast.LENGTH_LONG).show();
                    loadOrders("SHOPEE");
                    activeFilter = "SHOPEE";
                    buildFilterChips();
                    switchTab(1);
                });

            } catch (Exception e) {
                runOnUiThread(() -> {
                    prog.dismiss();
                    new AlertDialog.Builder(this)
                        .setTitle("Lỗi đồng bộ Shopee")
                        .setMessage(e.getMessage() +
                            "\n\nKiểm tra Access Token còn hạn không.")
                        .setPositiveButton("OK", null)
                        .show();
                });
            }
        }).start();
    }

    private String mapShopeeStatus(String s) {
        if (s == null) return "PENDING";
        switch (s.toUpperCase(Locale.ROOT)) {
            case "COMPLETED":        return "COMPLETED";
            case "CANCELLED":        return "CANCELLED";
            case "READY_TO_SHIP":
            case "PROCESSED":
            case "SHIPPED":          return "PROCESSING";
            default:                 return "PENDING";
        }
    }

    // ─── Generic connect (TikTok / Lazada) ───────────────────────────────────

    private void showGenericConnectDialog(String key, String name, int color) {
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(dp(24), dp(12), dp(24), dp(4));

        TextView hint = new TextView(this);
        hint.setText("Nhập Access Token từ " + name + " Seller Portal.\nChức năng đồng bộ đơn hàng đang phát triển.");
        hint.setTextSize(12f);
        hint.setTextColor(0xFF64748B);
        hint.setPadding(0, 0, 0, dp(14));
        root.addView(hint);

        EditText etName  = makeInput(root, "Tên gian hàng *");
        EditText etSid   = makeInput(root, "Shop ID");
        EditText etToken = makeInput(root, "Access Token");

        new AlertDialog.Builder(this)
            .setTitle("Kết nối " + name)
            .setView(root)
            .setPositiveButton("Lưu", (d, w) -> {
                String sn = etName.getText().toString().trim();
                if (sn.isEmpty()) { Toast.makeText(this, "Nhập tên gian hàng", Toast.LENGTH_SHORT).show(); return; }
                prefs().edit()
                    .putBoolean(key + "_connected",   true)
                    .putString(key + "_shop_name",    sn)
                    .putString(key + "_shop_id",      etSid.getText().toString().trim())
                    .putString(key + "_token",        etToken.getText().toString().trim())
                    .putLong(key + "_connected_at",   System.currentTimeMillis())
                    .apply();
                Toast.makeText(this, "Đã lưu thông tin " + name, Toast.LENGTH_SHORT).show();
                buildChannelCards();
            })
            .setNegativeButton("Hủy", null)
            .show();
    }

    // ─── Manage / disconnect ──────────────────────────────────────────────────

    private void showManageDialog(String key, String name, String shopName, String shopId) {
        long at = prefs().getLong(key + "_connected_at", 0);
        String dateStr = at > 0
            ? new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(new Date(at)) : "—";

        String msg = "Gian hàng: " + shopName
            + (shopId.isEmpty() ? "" : "\nShop ID: " + shopId)
            + "\nKết nối từ: " + dateStr
            + ("\nshopee".equals(key) ? "\n\nNhấn Đồng bộ trên trang kênh để kéo đơn hàng về." : "");

        new AlertDialog.Builder(this)
            .setTitle(name + " · " + shopName)
            .setMessage(msg)
            .setNeutralButton("Ngắt kết nối", (d, w) ->
                new AlertDialog.Builder(this)
                    .setTitle("Xác nhận")
                    .setMessage("Ngắt kết nối " + name + " — " + shopName + "?")
                    .setPositiveButton("Ngắt", (d2, w2) -> {
                        SharedPreferences.Editor ed = prefs().edit();
                        for (String suffix : new String[]{"_connected","_shop_name","_shop_id",
                            "_token","_connected_at","_partner_id","_partner_key","_shop_id_raw"})
                            ed.remove(key + suffix);
                        ed.apply();
                        Toast.makeText(this, "Đã ngắt " + name, Toast.LENGTH_SHORT).show();
                        buildChannelCards();
                    })
                    .setNegativeButton("Hủy", null).show())
            .setPositiveButton("Đóng", null)
            .show();
    }

    // ─── Orders tab ───────────────────────────────────────────────────────────

    private void buildFilterChips() {
        layoutFilterChips.removeAllViews();
        String[][] filters = {{"all","Tất cả"},{"POS","POS"},
            {"SHOPEE","Shopee"},{"TIKTOK","TikTok"},{"LAZADA","Lazada"}};
        for (String[] f : filters) addChip(f[0], f[1]);
    }

    private void addChip(String key, String label) {
        boolean active = key.equals(activeFilter);
        TextView chip = new TextView(this);
        chip.setText(label);
        chip.setTextSize(12f);
        chip.setTypeface(null, active ? Typeface.BOLD : Typeface.NORMAL);
        chip.setTextColor(active ? 0xFFFFFFFF : 0xFF64748B);
        chip.setBackground(active ? rounded(0xFF2875FB, 20) : outlined(0xFFE2E8F0, 20));
        chip.setPadding(dp(14), dp(6), dp(14), dp(6));
        chip.setClickable(true); chip.setFocusable(true);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        lp.setMargins(0, 0, dp(8), 0);
        chip.setLayoutParams(lp);
        chip.setOnClickListener(v -> { activeFilter = key; buildFilterChips(); loadOrders(key); });
        layoutFilterChips.addView(chip);
    }

    private void loadOrders(String filter) {
        layoutOrders.removeAllViews();
        String where = "all".equals(filter) ? ""
            : " AND UPPER(COALESCE(channel,'WALK_IN')) = '"
            + ("POS".equals(filter) ? "WALK_IN" : filter.toUpperCase(Locale.ROOT)) + "'";
        // Also include WALK_IN when filter is POS
        if ("POS".equals(filter))
            where = " AND (UPPER(COALESCE(channel,'WALK_IN')) IN ('WALK_IN','POS'))";

        Cursor c = db.getReadableDatabase().rawQuery(
            "SELECT order_code, channel, status, total_amount, created_at " +
            "FROM orders WHERE 1=1" + where + " ORDER BY created_at DESC LIMIT 80", null);
        SimpleDateFormat sdf = new SimpleDateFormat("dd/MM  HH:mm", Locale.getDefault());
        try {
            if (!c.moveToFirst()) { addEmptyState(); return; }
            do {
                addOrderRow(c.getString(0), c.getString(1),
                    c.getString(2), c.getLong(3), c.getLong(4), sdf);
            } while (c.moveToNext());
        } finally { c.close(); }
    }

    private void addOrderRow(String code, String ch, String status,
                              long amount, long createdAt, SimpleDateFormat sdf) {
        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(Gravity.CENTER_VERTICAL);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        lp.setMargins(0, 0, 0, dp(8));
        row.setLayoutParams(lp);
        row.setBackground(rounded(0xFFFFFFFF, 12));
        row.setElevation(dp(1));
        row.setPadding(dp(14), dp(12), dp(14), dp(12));

        // Badge
        TextView badge = new TextView(this);
        badge.setText(channelLabel(ch));
        badge.setTextSize(10f); badge.setTypeface(null, Typeface.BOLD);
        badge.setTextColor(0xFFFFFFFF);
        badge.setBackground(rounded(channelColor(ch), 4));
        badge.setPadding(dp(6), dp(3), dp(6), dp(3));
        LinearLayout.LayoutParams blp = new LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        blp.setMargins(0, 0, dp(12), 0);
        badge.setLayoutParams(blp);
        row.addView(badge);

        // Info
        LinearLayout info = new LinearLayout(this);
        info.setOrientation(LinearLayout.VERTICAL);
        info.setLayoutParams(new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
        tv(info, code != null ? code : "—", 13, 0xFF1C2333, true);
        tv(info, sdf.format(new Date(createdAt)), 11, 0xFF94A3B8, false);
        row.addView(info);

        // Right: amount + status
        LinearLayout right = new LinearLayout(this);
        right.setOrientation(LinearLayout.VERTICAL);
        right.setGravity(Gravity.END);
        tv(right, CurrencyUtils.vnd(amount), 13, 0xFF2875FB, true);
        tv(right, statusLabel(status), 10, statusColor(status), false);
        row.addView(right);
        layoutOrders.addView(row);
    }

    private void addEmptyState() {
        TextView tv = new TextView(this);
        tv.setText("Chưa có đơn hàng");
        tv.setTextSize(13f); tv.setTextColor(0xFF94A3B8);
        tv.setGravity(Gravity.CENTER);
        tv.setPadding(0, dp(48), 0, 0);
        tv.setLayoutParams(new LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        layoutOrders.addView(tv);
    }

    // ─── Label helpers ────────────────────────────────────────────────────────

    private String channelLabel(String ch) {
        if (ch == null) return "POS";
        switch (ch.toUpperCase(Locale.ROOT)) {
            case "SHOPEE": return "Shopee";
            case "TIKTOK": return "TikTok";
            case "LAZADA": return "Lazada";
            default:       return "POS";
        }
    }

    private int channelColor(String ch) {
        if (ch == null) return 0xFF2875FB;
        switch (ch.toUpperCase(Locale.ROOT)) {
            case "SHOPEE": return 0xFFEE4D2D;
            case "TIKTOK": return 0xFF161823;
            case "LAZADA": return 0xFF0F146D;
            default:       return 0xFF2875FB;
        }
    }

    private String statusLabel(String s) {
        if (s == null) return "—";
        switch (s.toUpperCase(Locale.ROOT)) {
            case "COMPLETED":  return "Hoàn thành";
            case "CANCELLED":  return "Đã huỷ";
            case "PROCESSING": return "Đang xử lý";
            case "PENDING":    return "Chờ xử lý";
            default:           return s;
        }
    }

    private int statusColor(String s) {
        if (s == null) return 0xFF94A3B8;
        switch (s.toUpperCase(Locale.ROOT)) {
            case "COMPLETED":  return 0xFF10B981;
            case "CANCELLED":  return 0xFFEF4444;
            case "PROCESSING": return 0xFF2875FB;
            default:           return 0xFFF59E0B;
        }
    }

    // ─── UI helpers ───────────────────────────────────────────────────────────

    private EditText makeInput(LinearLayout parent, String hint) {
        EditText et = new EditText(this);
        et.setHint(hint); et.setTextSize(14f); et.setTextColor(0xFF1C2333);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        lp.setMargins(0, 0, 0, dp(10));
        et.setLayoutParams(lp);
        parent.addView(et);
        return et;
    }

    private TextView tv(LinearLayout parent, String text, int sp, int color, boolean bold) {
        TextView tv = new TextView(this);
        tv.setText(text); tv.setTextSize(sp); tv.setTextColor(color);
        if (bold) tv.setTypeface(null, Typeface.BOLD);
        parent.addView(tv);
        return tv;
    }

    private GradientDrawable rounded(int color, int r) {
        GradientDrawable g = new GradientDrawable();
        g.setColor(color); g.setCornerRadius(dp(r)); return g;
    }

    private GradientDrawable outlined(int stroke, int r) {
        GradientDrawable g = new GradientDrawable();
        g.setColor(0x00000000); g.setStroke(dp(1), stroke);
        g.setCornerRadius(dp(r)); return g;
    }

    private GradientDrawable circle(int color) {
        GradientDrawable g = new GradientDrawable();
        g.setShape(GradientDrawable.OVAL); g.setColor(color); return g;
    }

    private SharedPreferences prefs() {
        return getSharedPreferences(PREFS, Context.MODE_PRIVATE);
    }

    private int dp(int dp) {
        return Math.round(dp * getResources().getDisplayMetrics().density);
    }
}

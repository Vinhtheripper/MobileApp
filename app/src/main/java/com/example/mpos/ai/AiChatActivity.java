package com.example.mpos.ai;

import android.animation.ObjectAnimator;
import android.animation.ValueAnimator;
import android.content.Context;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.graphics.Typeface;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.HorizontalScrollView;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.ScrollView;
import android.widget.TextView;

import androidx.appcompat.app.AlertDialog;
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
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;
import java.util.Locale;

public class AiChatActivity extends AppCompatActivity {

    private static final String PREFS_AI  = "mpos_ai_prefs";
    private static final String KEY_API_KEY = "claude_api_key";
    private static final String GROQ_URL  = "https://api.groq.com/openai/v1/chat/completions";
    private static final String MODEL     = "llama-3.1-8b-instant";
    private static final String DEFAULT_KEY = "";

    private LinearLayout layoutMessages;
    private ScrollView scrollView;
    private EditText etInput;
    private View btnSend;
    private ProgressBar progressBar;
    private final List<JSONObject> conversationHistory = new ArrayList<>();
    private String systemPrompt;

    // ──────────────────────────────────────────────────────────────────────────

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_ai_chat);

        layoutMessages = findViewById(R.id.layoutMessages);
        scrollView     = findViewById(R.id.scrollMessages);
        etInput        = findViewById(R.id.etChatInput);
        btnSend        = findViewById(R.id.btnChatSend);
        progressBar    = findViewById(R.id.progressChat);

        // Mascot header bounce animation
        View mascotHeader = findViewById(R.id.imgChatMascot);
        if (mascotHeader != null) startMascotBounce(mascotHeader);

        findViewById(R.id.btnBack).setOnClickListener(v -> finish());
        findViewById(R.id.btnApiKey).setOnClickListener(v -> showApiKeyDialog());

        buildQuickChips();

        btnSend.setOnClickListener(v -> sendMessage());
        etInput.addTextChangedListener(new TextWatcher() {
            public void beforeTextChanged(CharSequence s, int a, int b, int c) {}
            public void onTextChanged(CharSequence s, int a, int b, int c) {
                btnSend.setEnabled(s.toString().trim().length() > 0);
            }
            public void afterTextChanged(Editable s) {}
        });
        btnSend.setEnabled(false);

        systemPrompt = buildSystemPrompt();

        addBotMessage("👋 Xin chào! Tôi là **Quầy AI** - trợ lý kinh doanh của bạn!\n\nTôi có thể giúp:\n• 📊 Phân tích doanh thu & hiệu suất\n• 📦 Kiểm tra tồn kho & cảnh báo\n• 💡 Tư vấn chiến lược bán hàng\n• 🤖 Trả lời mọi câu hỏi về cửa hàng\n\nHãy hỏi tôi bất cứ điều gì!");
    }

    // ──────────────────────────────────────────────────────────────────────────
    // Mascot bounce animation in header

    private void startMascotBounce(View mascot) {
        ObjectAnimator bounce = ObjectAnimator.ofFloat(mascot, "translationY", 0f, -4f, 0f);
        bounce.setDuration(1600);
        bounce.setRepeatCount(ValueAnimator.INFINITE);
        bounce.setRepeatMode(ValueAnimator.RESTART);
        bounce.setInterpolator(new AccelerateDecelerateInterpolator());
        bounce.setStartDelay(800);
        bounce.start();
    }

    // ──────────────────────────────────────────────────────────────────────────
    // Quick chips

    private void buildQuickChips() {
        LinearLayout chipRow = findViewById(R.id.layoutChips);
        String[] chips = {
            "📊 Doanh thu hôm nay?",
            "🏆 Sản phẩm bán chạy?",
            "⚠️ Hàng sắp hết kho?",
            "📋 Đơn hàng tháng này?",
            "💡 Gợi ý tăng doanh thu?",
            "👥 Phân tích khách hàng?"
        };
        for (String chip : chips) {
            TextView tv = new TextView(this);
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            lp.setMarginEnd(dp(8));
            tv.setLayoutParams(lp);
            tv.setText(chip);
            tv.setTextSize(12f);
            tv.setTextColor(getResources().getColor(R.color.blue_primary));
            tv.setBackground(getResources().getDrawable(R.drawable.badge_info));
            tv.setPadding(dp(12), dp(7), dp(12), dp(7));
            tv.setTypeface(null, Typeface.BOLD);
            tv.setClickable(true);
            tv.setFocusable(true);
            String finalChip = chip;
            tv.setOnClickListener(v -> sendText(finalChip));
            chipRow.addView(tv);
        }
    }

    // ──────────────────────────────────────────────────────────────────────────
    // Message sending

    private void sendMessage() {
        String text = etInput.getText().toString().trim();
        if (text.isEmpty()) return;
        etInput.setText("");
        sendText(text);
    }

    private void sendText(String text) {
        if (getApiKey().isEmpty()) {
            showApiKeyDialog();
            return;
        }
        addUserMessage(text);
        setLoading(true);

        try {
            JSONObject userMsg = new JSONObject();
            userMsg.put("role", "user");
            userMsg.put("content", text);
            conversationHistory.add(userMsg);
        } catch (Exception ignored) {}

        new Thread(() -> {
            try {
                String response = callClaudeApi();
                try {
                    JSONObject assistantMsg = new JSONObject();
                    assistantMsg.put("role", "assistant");
                    assistantMsg.put("content", response);
                    conversationHistory.add(assistantMsg);
                } catch (Exception ignored) {}
                runOnUiThread(() -> {
                    setLoading(false);
                    addBotMessage(response);
                });
            } catch (Exception e) {
                runOnUiThread(() -> {
                    setLoading(false);
                    String err = e.getMessage() != null ? e.getMessage() : "Lỗi không xác định";
                    addBotMessage("❌ Lỗi API: " + err + "\n\n⚠️ Key Gemini phải lấy tại aistudio.google.com → Get API Key (bắt đầu bằng AIzaSy...)\nNhấn 🔑 để nhập lại key.");
                });
            }
        }).start();
    }

    // ──────────────────────────────────────────────────────────────────────────
    // Trust-all SSL helper (fixes CertPathValidatorException on some devices)

    private static void trustAllCerts() {
        try {
            TrustManager[] tm = new TrustManager[]{new X509TrustManager() {
                public X509Certificate[] getAcceptedIssuers() { return new X509Certificate[0]; }
                public void checkClientTrusted(X509Certificate[] c, String a) {}
                public void checkServerTrusted(X509Certificate[] c, String a) {}
            }};
            SSLContext sc = SSLContext.getInstance("TLS");
            sc.init(null, tm, new SecureRandom());
            HttpsURLConnection.setDefaultSSLSocketFactory(sc.getSocketFactory());
            HttpsURLConnection.setDefaultHostnameVerifier((h, s) -> true);
        } catch (Exception ignored) {}
    }

    // ──────────────────────────────────────────────────────────────────────────
    // Groq API call (OpenAI-compatible)

    private String callClaudeApi() throws Exception {
        trustAllCerts();

        URL url = new URL(GROQ_URL);
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestMethod("POST");
        conn.setRequestProperty("Content-Type", "application/json; charset=UTF-8");
        conn.setRequestProperty("Authorization", "Bearer " + getApiKey());
        conn.setDoOutput(true);
        conn.setConnectTimeout(30000);
        conn.setReadTimeout(60000);

        // Build messages: system + history
        JSONArray messages = new JSONArray();
        messages.put(new JSONObject().put("role", "system").put("content", systemPrompt));
        for (JSONObject m : conversationHistory) messages.put(m);

        JSONObject body = new JSONObject();
        body.put("model", MODEL);
        body.put("messages", messages);
        body.put("max_tokens", 1024);
        body.put("temperature", 0.7);

        byte[] bytes = body.toString().getBytes(StandardCharsets.UTF_8);
        try (OutputStream os = conn.getOutputStream()) { os.write(bytes); }

        int code = conn.getResponseCode();
        StringBuilder sb = new StringBuilder();
        try (BufferedReader br = new BufferedReader(new InputStreamReader(
                code == 200 ? conn.getInputStream() : conn.getErrorStream(),
                StandardCharsets.UTF_8))) {
            String line;
            while ((line = br.readLine()) != null) sb.append(line);
        }

        if (code != 200) {
            String errMsg = "HTTP " + code;
            try {
                JSONObject errJson = new JSONObject(sb.toString());
                JSONObject error = errJson.optJSONObject("error");
                if (error != null) errMsg = error.optString("message", errMsg);
            } catch (Exception ignored) {}
            throw new Exception(errMsg);
        }

        JSONObject resp = new JSONObject(sb.toString());
        return resp.getJSONArray("choices")
                   .getJSONObject(0)
                   .getJSONObject("message")
                   .getString("content");
    }

    // ──────────────────────────────────────────────────────────────────────────
    // System prompt with live business data

    private String buildSystemPrompt() {
        DatabaseHelper db = new DatabaseHelper(this);
        long now = System.currentTimeMillis();
        long todayStart = getTodayStart();
        long monthStart = getMonthStart();
        String date = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(new Date(now));

        com.example.mpos.auth.SessionManager sess = new com.example.mpos.auth.SessionManager(this);
        String shopName = sess.getShopName();
        String shopId   = String.valueOf(sess.getShopId());
        StringBuilder ctx = new StringBuilder();
        ctx.append("Bạn là Quầy AI - trợ lý AI thông minh của cửa hàng \"").append(shopName).append("\" tại Việt Nam.\n");
        ctx.append("Ngày hiện tại: ").append(date).append("\n\n");
        ctx.append("=== DỮ LIỆU KINH DOANH THỰC TẾ ===\n");

        // Today stats
        Cursor c = db.getReadableDatabase().rawQuery(
            "SELECT COUNT(*), COALESCE(SUM(total_amount),0) FROM orders WHERE shop_id=? AND status!='CANCELLED' AND created_at>=?",
            new String[]{shopId, String.valueOf(todayStart)});
        if (c.moveToFirst()) {
            ctx.append("Hôm nay: ").append(c.getInt(0)).append(" đơn / ")
               .append(CurrencyUtils.vnd(c.getLong(1))).append("\n");
        }
        c.close();

        // Month stats
        Cursor cm = db.getReadableDatabase().rawQuery(
            "SELECT COUNT(*), COALESCE(SUM(total_amount),0) FROM orders WHERE shop_id=? AND status='PAID' AND created_at>=?",
            new String[]{shopId, String.valueOf(monthStart)});
        if (cm.moveToFirst()) {
            long rev = cm.getLong(1);
            int cnt  = cm.getInt(0);
            ctx.append("Tháng này: ").append(cnt).append(" đơn / ").append(CurrencyUtils.vnd(rev)).append("\n");
            ctx.append("Trung bình/đơn: ").append(cnt > 0 ? CurrencyUtils.vnd(rev / cnt) : "0 ₫").append("\n");
        }
        cm.close();

        // All time
        Cursor ca = db.getReadableDatabase().rawQuery(
            "SELECT COUNT(*), COALESCE(SUM(total_amount),0) FROM orders WHERE shop_id=? AND status='PAID'",
            new String[]{shopId});
        if (ca.moveToFirst()) {
            ctx.append("Tổng tích lũy: ").append(ca.getInt(0)).append(" đơn / ")
               .append(CurrencyUtils.vnd(ca.getLong(1))).append("\n");
        }
        ca.close();

        // Top products (join orders để lọc đúng shop)
        ctx.append("\nTop sản phẩm bán chạy:\n");
        Cursor top = db.getReadableDatabase().rawQuery(
            "SELECT oi.product_name, SUM(oi.quantity) q, SUM(oi.line_total) rev " +
            "FROM order_items oi JOIN orders o ON oi.order_id=o.id " +
            "WHERE o.shop_id=? GROUP BY oi.product_id ORDER BY q DESC LIMIT 5",
            new String[]{shopId});
        int rank = 1;
        while (top.moveToNext()) {
            ctx.append(rank).append(". ").append(top.getString(0))
               .append(" — ").append(top.getInt(1)).append(" sp / ")
               .append(CurrencyUtils.vnd(top.getLong(2))).append("\n");
            rank++;
        }
        top.close();

        // Low stock
        ctx.append("\nSản phẩm sắp hết hàng:\n");
        Cursor low = db.getReadableDatabase().rawQuery(
            "SELECT name, stock_quantity, min_stock_quantity FROM products " +
            "WHERE shop_id=? AND is_active=1 AND stock_quantity<=min_stock_quantity ORDER BY stock_quantity LIMIT 5",
            new String[]{shopId});
        int lowCount = 0;
        while (low.moveToNext()) {
            ctx.append("• ").append(low.getString(0))
               .append(": còn ").append(low.getInt(1))
               .append(" (tối thiểu ").append(low.getInt(2)).append(")\n");
            lowCount++;
        }
        if (lowCount == 0) ctx.append("• Tất cả đủ hàng ✓\n");
        low.close();

        // Customers
        Cursor cust = db.getReadableDatabase().rawQuery(
            "SELECT COUNT(*), COALESCE(SUM(loyalty_points),0) FROM customers WHERE shop_id=?",
            new String[]{shopId});
        if (cust.moveToFirst()) {
            ctx.append("\nTổng khách hàng: ").append(cust.getInt(0))
               .append(" | Điểm thưởng tích lũy: ").append(cust.getLong(1)).append("\n");
        }
        cust.close();

        // Inventory total
        Cursor inv = db.getReadableDatabase().rawQuery(
            "SELECT COUNT(*), SUM(stock_quantity) FROM products WHERE shop_id=? AND is_active=1",
            new String[]{shopId});
        if (inv.moveToFirst()) {
            ctx.append("Tổng sản phẩm: ").append(inv.getInt(0))
               .append(" | Tổng tồn kho: ").append(inv.getInt(1)).append(" đơn vị\n");
        }
        inv.close();

        ctx.append("\n=== HƯỚNG DẪN TRẢ LỜI ===\n");
        ctx.append("• Trả lời bằng tiếng Việt, thân thiện và chuyên nghiệp\n");
        ctx.append("• Xưng hô là 'Quầy AI' khi giới thiệu bản thân\n");
        ctx.append("• Dựa trên dữ liệu thực tế ở trên để phân tích\n");
        ctx.append("• Đưa ra gợi ý hành động cụ thể khi phù hợp\n");
        ctx.append("• Giữ câu trả lời ngắn gọn (tối đa 200 từ)\n");
        ctx.append("• Dùng emoji để làm nổi bật điểm quan trọng\n");

        db.close();
        return ctx.toString();
    }

    // ──────────────────────────────────────────────────────────────────────────
    // UI helpers — bot message with mascot mini avatar

    private void addUserMessage(String text) {
        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(Gravity.END);
        LinearLayout.LayoutParams rowLp = new LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        rowLp.setMargins(dp(60), dp(6), dp(12), dp(6));
        row.setLayoutParams(rowLp);

        TextView tv = new TextView(this);
        tv.setText(text);
        tv.setTextSize(14f);
        tv.setTextColor(0xFFFFFFFF);
        tv.setBackground(getResources().getDrawable(R.drawable.bg_chat_user));
        tv.setPadding(dp(12), dp(10), dp(12), dp(10));
        tv.setMaxWidth(dp(260));
        row.addView(tv);
        layoutMessages.addView(row);
        scrollToBottom();
    }

    private void addBotMessage(String text) {
        // Row: mascot avatar + message bubble
        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(Gravity.START | Gravity.BOTTOM);
        LinearLayout.LayoutParams rowLp = new LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        rowLp.setMargins(dp(4), dp(6), dp(60), dp(6));
        row.setLayoutParams(rowLp);

        // Mascot mini avatar
        FrameLayout avatarFrame = new FrameLayout(this);
        LinearLayout.LayoutParams avatarLp = new LinearLayout.LayoutParams(dp(34), dp(34));
        avatarLp.setMarginEnd(dp(8));
        avatarFrame.setLayoutParams(avatarLp);
        avatarFrame.setBackground(getResources().getDrawable(R.drawable.bg_chat_mascot_mini));

        ImageView mascotImg = new ImageView(this);
        FrameLayout.LayoutParams imgLp = new FrameLayout.LayoutParams(dp(28), dp(28));
        imgLp.gravity = Gravity.CENTER;
        mascotImg.setLayoutParams(imgLp);
        mascotImg.setImageResource(R.drawable.ic_mascot_png);
        mascotImg.setScaleType(ImageView.ScaleType.FIT_CENTER);
        avatarFrame.addView(mascotImg);
        row.addView(avatarFrame);

        // Message bubble
        TextView tv = new TextView(this);
        tv.setText(text);
        tv.setTextSize(14f);
        tv.setTextColor(0xFF1F2937);
        tv.setBackground(getResources().getDrawable(R.drawable.bg_chat_bot));
        tv.setPadding(dp(12), dp(10), dp(12), dp(10));
        tv.setMaxWidth(dp(260));
        row.addView(tv);

        layoutMessages.addView(row);
        scrollToBottom();
    }

    private void scrollToBottom() {
        scrollView.post(() -> scrollView.fullScroll(ScrollView.FOCUS_DOWN));
    }

    private void setLoading(boolean loading) {
        progressBar.setVisibility(loading ? View.VISIBLE : View.GONE);
        btnSend.setEnabled(!loading && !etInput.getText().toString().trim().isEmpty());
        if (loading) addTypingIndicator();
        else removeTypingIndicator();
    }

    private View typingIndicator;

    private void addTypingIndicator() {
        removeTypingIndicator();

        // Row with mascot + typing dots
        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(Gravity.START | Gravity.BOTTOM);
        LinearLayout.LayoutParams rowLp = new LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        rowLp.setMargins(dp(4), dp(6), dp(60), dp(6));
        row.setLayoutParams(rowLp);

        // Mascot mini avatar
        FrameLayout avatarFrame = new FrameLayout(this);
        LinearLayout.LayoutParams avatarLp = new LinearLayout.LayoutParams(dp(34), dp(34));
        avatarLp.setMarginEnd(dp(8));
        avatarFrame.setLayoutParams(avatarLp);
        avatarFrame.setBackground(getResources().getDrawable(R.drawable.bg_chat_mascot_mini));
        ImageView mascotImg = new ImageView(this);
        FrameLayout.LayoutParams imgLp = new FrameLayout.LayoutParams(dp(28), dp(28));
        imgLp.gravity = Gravity.CENTER;
        mascotImg.setLayoutParams(imgLp);
        mascotImg.setImageResource(R.drawable.ic_mascot_png);
        mascotImg.setScaleType(ImageView.ScaleType.FIT_CENTER);
        avatarFrame.addView(mascotImg);
        row.addView(avatarFrame);

        // Typing bubble
        TextView tv = new TextView(this);
        tv.setText("● ● ●");
        tv.setTextSize(14f);
        tv.setTextColor(0xFF6B7280);
        tv.setBackground(getResources().getDrawable(R.drawable.bg_chat_bot));
        tv.setPadding(dp(12), dp(10), dp(12), dp(10));
        row.addView(tv);

        // Animate dots alpha
        ObjectAnimator pulse = ObjectAnimator.ofFloat(tv, "alpha", 1f, 0.3f, 1f);
        pulse.setDuration(900);
        pulse.setRepeatCount(ValueAnimator.INFINITE);
        pulse.start();

        typingIndicator = row;
        layoutMessages.addView(row);
        scrollToBottom();
    }

    private void removeTypingIndicator() {
        if (typingIndicator != null) {
            layoutMessages.removeView(typingIndicator);
            typingIndicator = null;
        }
    }

    // ──────────────────────────────────────────────────────────────────────────
    // API key dialog

    private void showApiKeyDialog() {
        EditText etKey = new EditText(this);
        etKey.setHint("gsk_...");
        etKey.setText(getApiKey());
        int pad = dp(16);
        etKey.setPadding(pad, pad, pad, pad);

        new AlertDialog.Builder(this)
            .setTitle("🔑 Groq API Key")
            .setMessage("Lấy key miễn phí (không cần thẻ) tại:\nconsole.groq.com → API Keys → Create API key\n(Key bắt đầu bằng gsk_...)")
            .setView(etKey)
            .setPositiveButton("Lưu", (d, w) -> {
                String key = etKey.getText().toString().trim();
                saveApiKey(key);
                if (!key.isEmpty()) {
                    conversationHistory.clear();
                    systemPrompt = buildSystemPrompt();
                    addBotMessage("✅ Gemini API Key đã được lưu! Tôi là **Quầy AI** - sẵn sàng hỗ trợ bạn 🚀");
                }
            })
            .setNegativeButton("Hủy", null)
            .show();
    }

    private String getApiKey() {
        String saved = getSharedPreferences(PREFS_AI, Context.MODE_PRIVATE)
                       .getString(KEY_API_KEY, "");
        return saved.isEmpty() ? DEFAULT_KEY : saved;
    }

    private void saveApiKey(String key) {
        getSharedPreferences(PREFS_AI, Context.MODE_PRIVATE)
            .edit().putString(KEY_API_KEY, key).apply();
    }

    // ──────────────────────────────────────────────────────────────────────────

    private long getTodayStart() {
        Calendar c = Calendar.getInstance();
        c.set(Calendar.HOUR_OF_DAY, 0); c.set(Calendar.MINUTE, 0);
        c.set(Calendar.SECOND, 0); c.set(Calendar.MILLISECOND, 0);
        return c.getTimeInMillis();
    }

    private long getMonthStart() {
        Calendar c = Calendar.getInstance();
        c.set(Calendar.DAY_OF_MONTH, 1); c.set(Calendar.HOUR_OF_DAY, 0);
        c.set(Calendar.MINUTE, 0); c.set(Calendar.SECOND, 0); c.set(Calendar.MILLISECOND, 0);
        return c.getTimeInMillis();
    }

    private int dp(int dp) {
        return Math.round(dp * getResources().getDisplayMetrics().density);
    }
}

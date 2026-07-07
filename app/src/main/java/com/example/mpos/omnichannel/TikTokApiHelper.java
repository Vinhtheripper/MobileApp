package com.example.mpos.omnichannel;

import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.security.cert.X509Certificate;
import java.util.TreeMap;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;

/** TikTok Shop Open API helper — HMAC-SHA256 signed requests. */
public class TikTokApiHelper {

    private static final String BASE = "https://open-api.tiktokglobalshop.com";

    private final String appKey;
    private final String appSecret;
    private final String accessToken;

    public TikTokApiHelper(String appKey, String appSecret, String accessToken) {
        this.appKey      = appKey;
        this.appSecret   = appSecret;
        this.accessToken = accessToken;
    }

    /** Verify credentials — returns shop name, throws if invalid. */
    public String verifyAndGetShopName() throws Exception {
        JSONObject resp = get("/api/seller/global/seller_info", new TreeMap<>());
        int code = resp.optInt("code", -1);
        if (code != 0) {
            String msg = resp.optString("message", "Xác thực thất bại");
            throw new Exception("TikTok: " + msg + " (code=" + code + ")");
        }
        JSONObject data = resp.optJSONObject("data");
        if (data == null) throw new Exception("Không lấy được thông tin shop TikTok");
        return data.optString("shop_name", data.optString("name", "TikTok Shop"));
    }

    /** Fetch orders created in the given time range (unix seconds). */
    public JSONObject getOrderList(long timeFrom, long timeTo) throws Exception {
        TreeMap<String, String> params = new TreeMap<>();
        params.put("create_time_from", String.valueOf(timeFrom));
        params.put("create_time_to",   String.valueOf(timeTo));
        params.put("page_size",        "50");
        params.put("sort_type",        "1");
        params.put("sort_by",          "create_time");
        return get("/api/orders/search", params);
    }

    // ─── SSL helper ──────────────────────────────────────────────────────────

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

    // ─── HTTP ────────────────────────────────────────────────────────────────

    private JSONObject get(String path, TreeMap<String, String> extraParams) throws Exception {
        trustAllCerts();
        long ts = System.currentTimeMillis() / 1000;
        extraParams.put("app_key",      appKey);
        extraParams.put("timestamp",    String.valueOf(ts));
        extraParams.put("access_token", accessToken);
        extraParams.put("sign",         sign(path, extraParams, ts));

        StringBuilder query = new StringBuilder();
        for (java.util.Map.Entry<String, String> e : extraParams.entrySet()) {
            if (query.length() > 0) query.append("&");
            query.append(java.net.URLEncoder.encode(e.getKey(), "UTF-8"))
                 .append("=")
                 .append(java.net.URLEncoder.encode(e.getValue(), "UTF-8"));
        }

        URL url = new URL(BASE + path + "?" + query);
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestMethod("GET");
        conn.setRequestProperty("Content-Type", "application/json");
        conn.setConnectTimeout(20000);
        conn.setReadTimeout(20000);

        int code = conn.getResponseCode();
        java.io.InputStream is = (code == 200) ? conn.getInputStream() : conn.getErrorStream();
        BufferedReader br = new BufferedReader(new InputStreamReader(is, StandardCharsets.UTF_8));
        StringBuilder sb = new StringBuilder();
        String line;
        while ((line = br.readLine()) != null) sb.append(line);
        br.close();
        return new JSONObject(sb.toString());
    }

    // ─── Signature ───────────────────────────────────────────────────────────

    private String sign(String path, TreeMap<String, String> params, long ts) throws Exception {
        // TikTok Shop v1 signature:
        // sorted_params = sorted non-special query string (exclude sign, access_token)
        // base = appSecret + path + sortedKV + appSecret
        StringBuilder paramStr = new StringBuilder();
        for (java.util.Map.Entry<String, String> e : params.entrySet()) {
            String k = e.getKey();
            if ("sign".equals(k) || "access_token".equals(k)) continue;
            paramStr.append(k).append(e.getValue());
        }
        String base = appSecret + path + paramStr + appSecret;
        Mac mac = Mac.getInstance("HmacSHA256");
        mac.init(new SecretKeySpec(appSecret.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
        byte[] hash = mac.doFinal(base.getBytes(StandardCharsets.UTF_8));
        StringBuilder hex = new StringBuilder();
        for (byte b : hash) hex.append(String.format("%02x", b));
        return hex.toString();
    }
}

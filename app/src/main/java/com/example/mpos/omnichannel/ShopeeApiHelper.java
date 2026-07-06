package com.example.mpos.omnichannel;

import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

/** Shopee Open Platform API v2 helper — HMAC-SHA256 signed requests. */
public class ShopeeApiHelper {

    private static final String BASE = "https://partner.shopeemobile.com";

    private final long   partnerId;
    private final String partnerKey;
    private final long   shopId;
    private final String accessToken;

    public ShopeeApiHelper(long partnerId, String partnerKey, long shopId, String accessToken) {
        this.partnerId   = partnerId;
        this.partnerKey  = partnerKey;
        this.shopId      = shopId;
        this.accessToken = accessToken;
    }

    /** Verify credentials — returns shop name if OK, throws if invalid. */
    public String verifyAndGetShopName() throws Exception {
        JSONObject resp = get("/api/v2/shop/get_shop_info", "");
        int error = resp.optInt("error", -1);
        if (error != 0) {
            String msg = resp.optString("message", "Xác thực thất bại");
            throw new Exception(msg);
        }
        JSONObject data = resp.optJSONObject("response");
        return data != null ? data.optString("shop_name", "Shopee Shop") : "Shopee Shop";
    }

    /** Fetch order list for a time range (unix seconds). Returns raw JSON. */
    public JSONObject getOrderList(long timeFrom, long timeTo) throws Exception {
        String extra = "&time_range_field=create_time"
            + "&time_from=" + timeFrom
            + "&time_to="   + timeTo
            + "&page_size=100";
        return get("/api/v2/order/get_order_list", extra);
    }

    /** Batch fetch order details for up to 50 comma-separated SNs. */
    public JSONObject getOrderDetail(String commaSeparatedSns) throws Exception {
        String extra = "&order_sn_list=" + URLEncoder.encode(commaSeparatedSns, "UTF-8")
            + "&response_optional_fields=pay_time,buyer_username";
        return get("/api/v2/order/get_order_detail", extra);
    }

    // ─── HTTP ────────────────────────────────────────────────────────────────

    private JSONObject get(String path, String extraParams) throws Exception {
        long ts   = System.currentTimeMillis() / 1000;
        String sign = sign(path, ts);
        String urlStr = BASE + path
            + "?partner_id="   + partnerId
            + "&timestamp="    + ts
            + "&sign="         + sign
            + "&shop_id="      + shopId
            + "&access_token=" + accessToken
            + extraParams;

        URL url = new URL(urlStr);
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

    private String sign(String path, long timestamp) throws Exception {
        // Shopee v2 signature: HMAC-SHA256(partnerKey, "{partnerId}{path}{ts}{accessToken}{shopId}")
        String base = partnerId + path + timestamp + accessToken + shopId;
        Mac mac = Mac.getInstance("HmacSHA256");
        mac.init(new SecretKeySpec(partnerKey.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
        byte[] hash = mac.doFinal(base.getBytes(StandardCharsets.UTF_8));
        StringBuilder hex = new StringBuilder();
        for (byte b : hash) hex.append(String.format("%02x", b));
        return hex.toString();
    }
}

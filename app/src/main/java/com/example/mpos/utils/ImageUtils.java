package com.example.mpos.utils;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Handler;
import android.os.Looper;
import android.util.LruCache;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.security.MessageDigest;

/**
 * Central image-loading helper (no external libraries required).
 *
 * Supported URI schemes:
 *   https://... / http://... → remote URL, loaded async with in-memory + disk cache
 *   res://img_d01            → R.drawable.img_d01 (bundled vector)
 *   file:///data/...         → file on internal storage
 *   content://...            → content URI
 */
public final class ImageUtils {
    private static final String RES_SCHEME = "res://";

    // 12 MB in-memory LRU cache
    private static final LruCache<String, Bitmap> MEM_CACHE =
        new LruCache<String, Bitmap>((int) (Runtime.getRuntime().maxMemory() / 8)) {
            @Override protected int sizeOf(String key, Bitmap v) {
                return v.getByteCount();
            }
        };

    private static final Handler MAIN = new Handler(Looper.getMainLooper());

    private ImageUtils() {}

    public static void load(Context context, String imageUri,
                            ImageView imgView, TextView txtFallback, String productName) {
        if (imageUri == null || imageUri.isEmpty()) {
            showInitials(imgView, txtFallback, productName);
            return;
        }
        try {
            if (imageUri.startsWith("http://") || imageUri.startsWith("https://")) {
                loadFromUrl(context, imageUri, imgView, txtFallback, productName);
            } else if (imageUri.startsWith(RES_SCHEME)) {
                String drawableName = imageUri.substring(RES_SCHEME.length());
                int resId = context.getResources().getIdentifier(
                        drawableName, "drawable", context.getPackageName());
                if (resId != 0) {
                    imgView.setImageResource(resId);
                    imgView.setVisibility(View.VISIBLE);
                    txtFallback.setVisibility(View.GONE);
                } else {
                    showInitials(imgView, txtFallback, productName);
                }
            } else {
                imgView.setImageURI(Uri.parse(imageUri));
                imgView.setVisibility(View.VISIBLE);
                txtFallback.setVisibility(View.GONE);
            }
        } catch (Exception e) {
            showInitials(imgView, txtFallback, productName);
        }
    }

    private static void loadFromUrl(Context ctx, String url,
                                    ImageView imgView, TextView txtFallback, String productName) {
        // Check memory cache first
        Bitmap cached = MEM_CACHE.get(url);
        if (cached != null) {
            imgView.setImageBitmap(cached);
            imgView.setVisibility(View.VISIBLE);
            txtFallback.setVisibility(View.GONE);
            return;
        }

        // Show initials while loading
        showInitials(imgView, txtFallback, productName);

        File cacheDir = new File(ctx.getCacheDir(), "img_cache");
        if (!cacheDir.exists()) cacheDir.mkdirs();

        new Thread(() -> {
            Bitmap bmp = null;
            try {
                // Check disk cache
                String key = md5(url);
                File diskFile = new File(cacheDir, key + ".jpg");
                if (diskFile.exists() && diskFile.length() > 0) {
                    bmp = BitmapFactory.decodeFile(diskFile.getAbsolutePath());
                }
                // Download if not cached
                if (bmp == null) {
                    bmp = downloadBitmap(url);
                    if (bmp != null) saveToDisk(diskFile, bmp);
                }
                if (bmp != null) {
                    Bitmap scaled = scaleBitmap(bmp, 300, 300);
                    MEM_CACHE.put(url, scaled);
                    final Bitmap finalBmp = scaled;
                    MAIN.post(() -> {
                        if (imgView.getTag() == null || imgView.getTag().equals(url)) {
                            imgView.setImageBitmap(finalBmp);
                            imgView.setVisibility(View.VISIBLE);
                            txtFallback.setVisibility(View.GONE);
                        }
                    });
                }
            } catch (Exception ignored) {}
        }).start();

        imgView.setTag(url);
    }

    private static Bitmap downloadBitmap(String urlStr) throws Exception {
        URL url = new URL(urlStr);
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setInstanceFollowRedirects(true);
        conn.setConnectTimeout(12000);
        conn.setReadTimeout(20000);
        conn.setRequestProperty("User-Agent", "Mozilla/5.0");
        int code = conn.getResponseCode();
        if (code == 200) {
            try (InputStream is = conn.getInputStream()) {
                return BitmapFactory.decodeStream(is);
            } finally { conn.disconnect(); }
        }
        conn.disconnect();
        return null;
    }

    private static Bitmap scaleBitmap(Bitmap src, int maxW, int maxH) {
        int w = src.getWidth(), h = src.getHeight();
        if (w <= maxW && h <= maxH) return src;
        float ratio = Math.min((float) maxW / w, (float) maxH / h);
        return Bitmap.createScaledBitmap(src, (int)(w * ratio), (int)(h * ratio), true);
    }

    private static void saveToDisk(File file, Bitmap bmp) {
        try (FileOutputStream fos = new FileOutputStream(file)) {
            bmp.compress(Bitmap.CompressFormat.JPEG, 85, fos);
        } catch (Exception ignored) {}
    }

    private static String md5(String s) {
        try {
            MessageDigest md = MessageDigest.getInstance("MD5");
            byte[] hash = md.digest(s.getBytes("UTF-8"));
            StringBuilder sb = new StringBuilder();
            for (byte b : hash) sb.append(String.format("%02x", b));
            return sb.toString();
        } catch (Exception e) {
            return String.valueOf(s.hashCode());
        }
    }

    private static void showInitials(ImageView img, TextView txt, String name) {
        img.setVisibility(View.GONE);
        txt.setVisibility(View.VISIBLE);
        String initials = (name != null && name.length() >= 2)
                ? name.substring(0, 2).toUpperCase()
                : (name != null && !name.isEmpty() ? name.toUpperCase() : "SP");
        txt.setText(initials);
    }
}

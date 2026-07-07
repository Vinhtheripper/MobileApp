package com.example.mpos.notification;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

import androidx.core.app.NotificationCompat;

import com.example.mpos.MainActivity;
import com.example.mpos.R;
import com.google.firebase.messaging.FirebaseMessaging;
import com.google.firebase.messaging.FirebaseMessagingService;
import com.google.firebase.messaging.RemoteMessage;

/**
 * Handles incoming FCM push notifications.
 * Supported notification types (via data payload "type"):
 *   - "new_order"     → "Đơn hàng mới"
 *   - "low_stock"     → "Sắp hết hàng"
 *   - "sync_complete" → "Đồng bộ hoàn tất"
 */
public class MposFcmService extends FirebaseMessagingService {

    private static final String TAG        = "MposFcm";
    private static final String CHANNEL_ID = "mpos_main";

    @Override
    public void onMessageReceived(RemoteMessage message) {
        String title = "Quầy mPOS";
        String body  = "";

        if (message.getNotification() != null) {
            title = message.getNotification().getTitle() != null
                    ? message.getNotification().getTitle() : title;
            body  = message.getNotification().getBody() != null
                    ? message.getNotification().getBody() : body;
        }

        // Data payload overrides notification payload
        if (message.getData().containsKey("title")) title = message.getData().get("title");
        if (message.getData().containsKey("body"))  body  = message.getData().get("body");

        showNotification(title, body);
    }

    @Override
    public void onNewToken(String token) {
        Log.d(TAG, "FCM token refreshed: " + token);
        // TODO: send token to your backend/Firestore so server can target this device
    }

    private void showNotification(String title, String body) {
        NotificationManager mgr =
                (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);

        // Create channel (required on Android 8+)
        NotificationChannel channel = new NotificationChannel(
                CHANNEL_ID, "Quầy mPOS Thông báo",
                NotificationManager.IMPORTANCE_DEFAULT);
        mgr.createNotificationChannel(channel);

        Intent intent = new Intent(this, MainActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        PendingIntent pi = PendingIntent.getActivity(this, 0, intent,
                PendingIntent.FLAG_ONE_SHOT | PendingIntent.FLAG_IMMUTABLE);

        NotificationCompat.Builder builder = new NotificationCompat.Builder(this, CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_notification)
                .setContentTitle(title)
                .setContentText(body)
                .setAutoCancel(true)
                .setContentIntent(pi);

        mgr.notify((int) System.currentTimeMillis(), builder.build());
    }

    /** Subscribe this device to a shop's push topic. */
    public static void subscribeToShop(long shopId) {
        FirebaseMessaging.getInstance()
                .subscribeToTopic("shop_" + shopId)
                .addOnCompleteListener(t -> Log.d(TAG, "Subscribed to shop_" + shopId));
    }

    /** Unsubscribe (on logout / shop switch). */
    public static void unsubscribeFromShop(long shopId) {
        FirebaseMessaging.getInstance().unsubscribeFromTopic("shop_" + shopId);
    }
}

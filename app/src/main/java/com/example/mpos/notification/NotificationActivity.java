package com.example.mpos.notification;

import android.database.Cursor;
import android.graphics.Typeface;
import android.os.Bundle;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.example.mpos.R;
import com.example.mpos.database.DatabaseHelper;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class NotificationActivity extends AppCompatActivity {

    static class NotifItem {
        String title, body, time, type;
        boolean unread = true;
    }

    private NotifAdapter adapter;
    private List<NotifItem> allItems = new ArrayList<>();

    @Override
    public void onCreate(Bundle state) {
        super.onCreate(state);
        setContentView(R.layout.activity_notification);

        adapter = new NotifAdapter();
        ((ListView) findViewById(R.id.listNotifications)).setAdapter(adapter);

        findViewById(R.id.btnBack).setOnClickListener(v -> finish());
        findViewById(R.id.btnMarkAllRead).setOnClickListener(v -> markAllRead());

        // Filter tabs
        View tabAll       = findViewById(R.id.tabAll);
        View tabOrder     = findViewById(R.id.tabOrder);
        View tabInventory = findViewById(R.id.tabInventory);
        View tabSystem    = findViewById(R.id.tabSystem);

        tabAll.setOnClickListener(v  -> filter(null));
        tabOrder.setOnClickListener(v -> filter("ORDER"));
        tabInventory.setOnClickListener(v -> filter("INVENTORY"));
        tabSystem.setOnClickListener(v -> filter("SYSTEM"));

        load();
    }

    private void load() {
        allItems.clear();
        DatabaseHelper db = new DatabaseHelper(this);

        // Low stock alerts
        Cursor low = db.getReadableDatabase().rawQuery(
            "SELECT name, stock_quantity, min_stock_quantity FROM products " +
            "WHERE is_active=1 AND stock_quantity <= min_stock_quantity AND min_stock_quantity > 0 " +
            "ORDER BY stock_quantity ASC LIMIT 10", null);
        try {
            while (low.moveToNext()) {
                NotifItem n = new NotifItem();
                n.type  = "INVENTORY";
                n.title = "Cảnh báo tồn kho thấp";
                int qty = low.getInt(1), min = low.getInt(2);
                n.body  = low.getString(0) + " — còn " + qty + " (tối thiểu " + min + ")";
                n.time  = "Hôm nay";
                n.unread = qty <= 0;
                allItems.add(n);
            }
        } finally { low.close(); }

        // Recent orders
        Cursor orders = db.getReadableDatabase().rawQuery(
            "SELECT id, total_amount, status, created_at FROM orders " +
            "ORDER BY created_at DESC LIMIT 5", null);
        SimpleDateFormat sdf = new SimpleDateFormat("HH:mm dd/MM", Locale.getDefault());
        try {
            while (orders.moveToNext()) {
                NotifItem n = new NotifItem();
                n.type  = "ORDER";
                String status = orders.getString(2);
                if ("PAID".equals(status)) {
                    n.title = "Đơn hàng đã thanh toán";
                } else {
                    n.title = "Đơn hàng mới #" + orders.getLong(0);
                }
                long amount = orders.getLong(1);
                n.body = "Tổng: " + String.format("%,.0f₫", (double) amount);
                n.time = sdf.format(new Date(orders.getLong(3)));
                n.unread = false;
                allItems.add(n);
            }
        } finally { orders.close(); }

        // System notification
        NotifItem sys = new NotifItem();
        sys.type  = "SYSTEM";
        sys.title = "Hệ thống MPOS đã kết nối";
        sys.body  = "Dữ liệu đồng bộ thành công. Sẵn sàng bán hàng.";
        sys.time  = "Hôm nay";
        sys.unread = false;
        allItems.add(sys);

        adapter.setItems(allItems);
    }

    private void filter(String type) {
        if (type == null) {
            adapter.setItems(allItems);
        } else {
            List<NotifItem> filtered = new ArrayList<>();
            for (NotifItem n : allItems) if (type.equals(n.type)) filtered.add(n);
            adapter.setItems(filtered);
        }
    }

    private void markAllRead() {
        for (NotifItem n : allItems) n.unread = false;
        adapter.notifyDataSetChanged();
    }

    class NotifAdapter extends BaseAdapter {
        private List<NotifItem> data = new ArrayList<>();
        void setItems(List<NotifItem> list) { data = list; notifyDataSetChanged(); }
        @Override public int getCount() { return data.isEmpty() ? 1 : data.size(); }
        @Override public NotifItem getItem(int p) { return data.isEmpty() ? null : data.get(p); }
        @Override public long getItemId(int p) { return p; }

        @Override
        public View getView(int pos, View cv, ViewGroup parent) {
            if (data.isEmpty()) {
                TextView tv = new TextView(NotificationActivity.this);
                tv.setText("Không có thông báo mới");
                tv.setGravity(Gravity.CENTER);
                tv.setPadding(0, 80, 0, 80);
                tv.setTextColor(getResources().getColor(R.color.text_secondary));
                return tv;
            }
            if (cv == null || cv instanceof TextView)
                cv = LayoutInflater.from(NotificationActivity.this)
                    .inflate(R.layout.item_notification, parent, false);

            NotifItem n = data.get(pos);
            ((TextView) cv.findViewById(R.id.txtNotifTitle)).setText(n.title);
            ((TextView) cv.findViewById(R.id.txtNotifTitle))
                .setTypeface(null, n.unread ? Typeface.BOLD : Typeface.NORMAL);
            ((TextView) cv.findViewById(R.id.txtNotifBody)).setText(n.body);
            ((TextView) cv.findViewById(R.id.txtNotifTime)).setText(n.time);
            cv.findViewById(R.id.viewUnread).setVisibility(n.unread ? View.VISIBLE : View.INVISIBLE);

            // Icon by type
            ImageView icon = cv.findViewById(R.id.imgNotifIcon);
            if ("INVENTORY".equals(n.type)) {
                icon.setImageResource(R.drawable.ic_inventory);
                icon.setColorFilter(getResources().getColor(R.color.status_warning));
                cv.findViewById(R.id.layoutNotifIcon)
                    .setBackgroundResource(R.drawable.badge_warning);
            } else if ("ORDER".equals(n.type)) {
                icon.setImageResource(R.drawable.ic_orders);
                icon.setColorFilter(getResources().getColor(R.color.status_success));
                cv.findViewById(R.id.layoutNotifIcon)
                    .setBackgroundResource(R.drawable.badge_success);
            } else {
                icon.setImageResource(R.drawable.ic_notification);
                icon.setColorFilter(getResources().getColor(R.color.blue_primary));
                cv.findViewById(R.id.layoutNotifIcon)
                    .setBackgroundResource(R.drawable.bg_blue_summary);
            }

            cv.setAlpha(n.unread ? 1.0f : 0.75f);
            return cv;
        }
    }
}

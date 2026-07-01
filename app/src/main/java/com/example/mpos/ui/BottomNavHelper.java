package com.example.mpos.ui;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Typeface;
import android.view.View;
import android.widget.TextView;

import com.example.mpos.MainActivity;
import com.example.mpos.R;
import com.example.mpos.order.OrderListActivity;
import com.example.mpos.pos.PosActivity;
import com.example.mpos.inventory.InventoryActivity;

public final class BottomNavHelper {
    private BottomNavHelper() {}
    public static void bind(Activity activity) {
        markActive(activity, R.id.navHome, MainActivity.class);
        markActive(activity, R.id.navPos, PosActivity.class);
        markActive(activity, R.id.navOrders, OrderListActivity.class);
        markActive(activity, R.id.navProducts, InventoryActivity.class);
        markActive(activity, R.id.navMore, MoreActivity.class);
        click(activity, R.id.navHome, MainActivity.class);
        click(activity, R.id.navPos, PosActivity.class);
        click(activity, R.id.navOrders, OrderListActivity.class);
        click(activity, R.id.navProducts, InventoryActivity.class);
        click(activity, R.id.navMore, MoreActivity.class);
    }
    private static void click(Activity activity, int id, Class<?> target) {
        View view = activity.findViewById(id);
        if (view != null) view.setOnClickListener(v -> {
            if (!activity.getClass().equals(target)) activity.startActivity(new Intent(activity, target).addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP));
        });
    }
    private static void markActive(Activity activity, int id, Class<?> target) {
        View view = activity.findViewById(id);
        if (!(view instanceof TextView)) return;
        TextView textView = (TextView) view;
        boolean active = activity.getClass().equals(target);
        textView.setTextColor(activity.getColor(active ? R.color.blue_primary_dark : R.color.text_secondary));
        textView.setTypeface(Typeface.DEFAULT, active ? Typeface.BOLD : Typeface.NORMAL);
    }
}

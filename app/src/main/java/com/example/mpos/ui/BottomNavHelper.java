package com.example.mpos.ui;

import android.app.Activity;
import android.content.Intent;
import android.view.View;

import com.example.mpos.MainActivity;
import com.example.mpos.R;
import com.example.mpos.order.OrderListActivity;
import com.example.mpos.pos.PosActivity;
import com.example.mpos.product.ProductListActivity;

public final class BottomNavHelper {
    private BottomNavHelper() {}
    public static void bind(Activity activity) {
        click(activity, R.id.navHome, MainActivity.class);
        click(activity, R.id.navPos, PosActivity.class);
        click(activity, R.id.navOrders, OrderListActivity.class);
        click(activity, R.id.navProducts, ProductListActivity.class);
        click(activity, R.id.navMore, MoreActivity.class);
    }
    private static void click(Activity activity, int id, Class<?> target) {
        View view = activity.findViewById(id);
        if (view != null) view.setOnClickListener(v -> {
            if (!activity.getClass().equals(target)) activity.startActivity(new Intent(activity, target).addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP));
        });
    }
}

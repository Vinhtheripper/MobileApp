package com.example.mpos.shop;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ListView;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.example.mpos.MainActivity;
import com.example.mpos.R;
import com.example.mpos.auth.LoginActivity;
import com.example.mpos.auth.SessionManager;
import com.example.mpos.dao.ShopDao;
import com.example.mpos.database.DatabaseHelper;
import com.example.mpos.model.Shop;

import java.util.List;

public class ShopListActivity extends AppCompatActivity {

    private SessionManager session;
    private ShopDao shopDao;
    private ShopAdapter adapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_shop_list);

        session = new SessionManager(this);
        shopDao = new ShopDao(new DatabaseHelper(this));

        adapter = new ShopAdapter();
        ListView list = findViewById(R.id.listShops);
        list.setAdapter(adapter);
        list.setOnItemClickListener((parent, view, pos, id) -> enterShop(adapter.getItem(pos)));

        findViewById(R.id.btnCreateNewShop).setOnClickListener(v ->
            startActivity(new Intent(this, CreateShopActivity.class)));

        TextView btnLogout = findViewById(R.id.btnLogoutFromShopList);
        btnLogout.setOnClickListener(v -> {
            session.clear();
            startActivity(new Intent(this, LoginActivity.class)
                .addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK | Intent.FLAG_ACTIVITY_NEW_TASK));
        });

        loadShops();
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadShops();
    }

    // Prevent back-press from going to a state without a shop
    @Override public void onBackPressed() { }

    private void loadShops() {
        long userId = session.getUser().id;
        List<Shop> shops = shopDao.getShopsForUser(userId);
        adapter.setShops(shops);

        if (shops.size() == 1) {
            enterShop(shops.get(0));
        }
    }

    private void enterShop(Shop shop) {
        if (shop == null) return;
        session.saveShop(shop.id, shop.name, shop.memberRole != null ? shop.memberRole : "STAFF");
        com.example.mpos.auth.FirebaseAuthHelper.onShopSelected(shop.id);

        startActivity(new Intent(this, MainActivity.class)
            .addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK | Intent.FLAG_ACTIVITY_NEW_TASK));
    }

    class ShopAdapter extends BaseAdapter {
        private List<Shop> shops = new java.util.ArrayList<>();

        void setShops(List<Shop> list) { shops = list; notifyDataSetChanged(); }

        @Override public int getCount() { return shops.isEmpty() ? 1 : shops.size(); }
        @Override public Shop getItem(int pos) { return shops.isEmpty() ? null : shops.get(pos); }
        @Override public long getItemId(int pos) { return pos; }

        @Override
        public View getView(int pos, View convertView, ViewGroup parent) {
            if (shops.isEmpty()) {
                TextView tv = new TextView(ShopListActivity.this);
                tv.setText("Bạn chưa có cửa hàng nào\nNhấn 'Tạo cửa hàng mới' để bắt đầu");
                tv.setGravity(android.view.Gravity.CENTER);
                tv.setPadding(0, 80, 0, 80);
                tv.setTextColor(0xFF64748B);
                tv.setTextSize(14f);
                return tv;
            }

            if (convertView == null || convertView instanceof TextView)
                convertView = LayoutInflater.from(ShopListActivity.this)
                    .inflate(R.layout.item_shop, parent, false);

            Shop shop = shops.get(pos);
            ((TextView) convertView.findViewById(R.id.txtShopInitial))
                .setText(shop.name.substring(0, 1).toUpperCase());
            ((TextView) convertView.findViewById(R.id.txtShopName)).setText(shop.name);

            TextView txtAddr = convertView.findViewById(R.id.txtShopAddress);
            txtAddr.setText(shop.address != null && !shop.address.isEmpty()
                ? shop.address : "Chưa có địa chỉ");

            TextView txtRole = convertView.findViewById(R.id.txtShopRole);
            txtRole.setText(roleLabel(shop.memberRole));

            return convertView;
        }

        private String roleLabel(String role) {
            if ("OWNER".equals(role)) return "Chủ sở hữu";
            if ("MANAGER".equals(role)) return "Quản lý";
            return "Nhân viên";
        }
    }
}

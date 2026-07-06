package com.example.mpos.pos;

import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.Gravity;
import android.widget.EditText;
import android.widget.GridView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import com.example.mpos.R;
import com.example.mpos.adapter.ProductPosAdapter;
import com.example.mpos.cart.CartActivity;
import com.example.mpos.cart.CartManager;
import com.example.mpos.dao.CategoryDao;
import com.example.mpos.dao.ProductDao;
import com.example.mpos.database.DatabaseHelper;
import com.example.mpos.inventory.InventoryActivity;
import com.example.mpos.model.Category;
import com.example.mpos.model.Product;
import com.example.mpos.order.OrderListActivity;
import com.example.mpos.settings.SettingsActivity;
import com.example.mpos.MainActivity;

import java.util.ArrayList;
import java.util.List;

public class PosActivity extends AppCompatActivity {
    private ProductPosAdapter adapter;
    private ProductDao productDao;
    private long selectedCategoryId = Category.ALL_ID;
    private long shopId = 1;
    private String currentQuery = "";
    private TextView cartBadge;
    private TextView fabBadge;
    private List<Category> allCategories = new ArrayList<>();
    private List<TextView> categoryChips = new ArrayList<>();

    @Override
    public void onCreate(Bundle state) {
        super.onCreate(state);
        setContentView(R.layout.activity_pos);

        shopId = new com.example.mpos.auth.SessionManager(this).getShopId();
        DatabaseHelper db = new DatabaseHelper(this);
        productDao = new ProductDao(db, shopId);
        cartBadge = findViewById(R.id.txtCartBadge);
        fabBadge  = findViewById(R.id.txtFabBadge);

        // AppBar cart button
        findViewById(R.id.btnCheckout).setOnClickListener(v -> openCart());

        // Floating cart FAB
        findViewById(R.id.btnCartFab).setOnClickListener(v -> openCart());

        // Barcode scan (placeholder)
        findViewById(R.id.btnScan).setOnClickListener(v ->
            Toast.makeText(this, "Quét mã vạch sắp ra mắt", Toast.LENGTH_SHORT).show());

        // Product grid
        GridView grid = findViewById(R.id.gridProducts);
        adapter = new ProductPosAdapter(this);
        grid.setAdapter(adapter);
        grid.setOnItemClickListener((parent, view, position, id) -> {
            Product p = adapter.getItem(position);
            if (!CartManager.get().add(p)) {
                Toast.makeText(this, "Hết hàng hoặc đã đạt tối đa", Toast.LENGTH_SHORT).show();
            }
            refreshBadge();
        });

        // Search
        EditText search = findViewById(R.id.inputSearch);
        search.addTextChangedListener(new TextWatcher() {
            public void afterTextChanged(Editable s) { currentQuery = s.toString(); loadProducts(); }
            public void beforeTextChanged(CharSequence s, int a, int b, int c) {}
            public void onTextChanged(CharSequence s, int a, int b, int c) {}
        });

        // Bottom nav
        findViewById(R.id.navStaffHome).setOnClickListener(v -> {
            startActivity(new Intent(this, MainActivity.class)
                .addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP));
        });
        findViewById(R.id.navStaffPos).setOnClickListener(v -> { /* already here */ });
        findViewById(R.id.navStaffOrders).setOnClickListener(v ->
            startActivity(new Intent(this, OrderListActivity.class)
                .addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)));
        findViewById(R.id.navStaffInventory).setOnClickListener(v ->
            startActivity(new Intent(this, InventoryActivity.class)
                .addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)));
        findViewById(R.id.navStaffSettings).setOnClickListener(v ->
            startActivity(new Intent(this, SettingsActivity.class)
                .addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)));

        loadCategories(db);
        loadProducts();
    }

    @Override
    protected void onResume() {
        super.onResume();
        refreshBadge();
        loadProducts();
    }

    private void openCart() {
        if (CartManager.get().isEmpty()) {
            Toast.makeText(this, "Giỏ hàng đang trống", Toast.LENGTH_SHORT).show();
            return;
        }
        startActivity(new Intent(this, CartActivity.class));
    }

    private void loadCategories(DatabaseHelper db) {
        LinearLayout layout = findViewById(R.id.layoutCategories);
        layout.removeAllViews();
        allCategories.clear();
        categoryChips.clear();

        allCategories.add(new Category(Category.ALL_ID, "Tất cả"));
        allCategories.addAll(new CategoryDao(db, shopId).getAll());

        for (int i = 0; i < allCategories.size(); i++) {
            Category cat = allCategories.get(i);
            TextView chip = new TextView(this);
            chip.setText(cat.name);
            chip.setTextSize(14f);
            chip.setGravity(Gravity.CENTER);
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT, dp(40));
            lp.setMarginEnd(dp(8));
            chip.setLayoutParams(lp);
            chip.setPadding(dp(20), 0, dp(20), 0);
            updateChipStyle(chip, cat.id == selectedCategoryId);

            final long catId = cat.id;
            chip.setOnClickListener(v -> {
                selectedCategoryId = catId;
                for (int j = 0; j < categoryChips.size(); j++) {
                    updateChipStyle(categoryChips.get(j), allCategories.get(j).id == selectedCategoryId);
                }
                loadProducts();
            });

            categoryChips.add(chip);
            layout.addView(chip);
        }
    }

    private void updateChipStyle(TextView chip, boolean selected) {
        chip.setBackgroundResource(selected ? R.drawable.bg_pos_cat_active : R.drawable.bg_pos_cat);
        chip.setTextColor(selected ? 0xFFFFFFFF : 0xFF5F6368);
    }

    private void loadProducts() {
        List<Product> products = productDao.searchByCategory(selectedCategoryId, currentQuery);
        adapter.submit(products);
    }

    private void refreshBadge() {
        int count = CartManager.get().getCount();
        if (cartBadge != null) {
            cartBadge.setText(String.valueOf(count));
            cartBadge.setVisibility(count > 0 ? android.view.View.VISIBLE : android.view.View.GONE);
        }
        if (fabBadge != null) {
            fabBadge.setText(String.valueOf(count));
            fabBadge.setVisibility(count > 0 ? android.view.View.VISIBLE : android.view.View.GONE);
        }
    }

    private int dp(int dp) { return Math.round(dp * getResources().getDisplayMetrics().density); }
}
package com.example.mpos.product;

import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import com.example.mpos.R;
import com.example.mpos.dao.ProductDao;
import com.example.mpos.database.DatabaseHelper;
import com.example.mpos.model.Product;
import com.example.mpos.MainActivity;
import com.example.mpos.inventory.InventoryActivity;
import com.example.mpos.order.OrderListActivity;
import com.example.mpos.pos.PosActivity;
import com.example.mpos.settings.SettingsActivity;
import com.example.mpos.utils.CurrencyUtils;
import com.example.mpos.utils.ImageUtils;

import java.util.ArrayList;
import java.util.List;

public class ProductListActivity extends AppCompatActivity {

    private ProductDao dao;
    private ProductListAdapter adapter;
    private EditText searchInput;
    private TextView txtCount;

    @Override
    public void onCreate(Bundle state) {
        super.onCreate(state);
        setContentView(R.layout.activity_product_list);
        bindStaffNav();

        long shopId = new com.example.mpos.auth.SessionManager(this).getShopId();
        dao = new ProductDao(new DatabaseHelper(this), shopId);
        searchInput = findViewById(R.id.inputProductSearch);
        txtCount = findViewById(R.id.txtProductCount);

        adapter = new ProductListAdapter();
        ListView list = findViewById(R.id.listProducts);
        list.setAdapter(adapter);
        list.setOnItemClickListener((p, v, pos, id) -> {
            Product prod = adapter.getItem(pos);
            if (prod == null) return;
            Intent i = new Intent(this, ProductFormActivity.class);
            i.putExtra("product_id", prod.id);
            startActivity(i);
        });

        searchInput.addTextChangedListener(new TextWatcher() {
            public void afterTextChanged(Editable s) { load(); }
            public void beforeTextChanged(CharSequence s, int a, int b, int c) {}
            public void onTextChanged(CharSequence s, int a, int b, int c) {}
        });

        findViewById(R.id.btnSearchProduct).setOnClickListener(v -> load());
        findViewById(R.id.btnBack).setOnClickListener(v -> finish());
        findViewById(R.id.btnAddProduct).setOnClickListener(v ->
            startActivity(new Intent(this, ProductFormActivity.class)));
        findViewById(R.id.btnImportCsv).setOnClickListener(v ->
            startActivity(new Intent(this, CsvImportActivity.class)));

        load();
    }

    @Override
    protected void onResume() { super.onResume(); if (dao != null) load(); }

    private void load() {
        List<Product> products = dao.search(searchInput.getText().toString());
        adapter.setProducts(products);
        txtCount.setText(products.size() + " san pham");
    }

    private void bindStaffNav() {
        navClick(R.id.navStaffHome,      MainActivity.class);
        navClick(R.id.navStaffPos,       PosActivity.class);
        navClick(R.id.navStaffOrders,    OrderListActivity.class);
        navClick(R.id.navStaffInventory, InventoryActivity.class);
        navClick(R.id.navStaffSettings,  SettingsActivity.class);
    }

    private void navClick(int id, Class<?> target) {
        View v = findViewById(id);
        if (v != null) v.setOnClickListener(x -> {
            if (!getClass().equals(target))
                startActivity(new Intent(this, target).addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP));
        });
    }

    class ProductListAdapter extends BaseAdapter {
        private List<Product> products = new ArrayList<>();
        void setProducts(List<Product> list) { products = list; notifyDataSetChanged(); }
        @Override public int getCount() { return products.isEmpty() ? 1 : products.size(); }
        @Override public Product getItem(int p) { return products.isEmpty() ? null : products.get(p); }
        @Override public long getItemId(int p) { return p; }

        @Override
        public View getView(int pos, View convertView, ViewGroup parent) {
            if (products.isEmpty()) {
                TextView tv = new TextView(ProductListActivity.this);
                tv.setText("Chua co san pham\nBam '+ Them' de tao san pham moi");
                tv.setGravity(android.view.Gravity.CENTER);
                tv.setPadding(0, 80, 0, 80);
                tv.setTextColor(getResources().getColor(R.color.text_secondary));
                return tv;
            }
            if (convertView == null || convertView instanceof TextView)
                convertView = LayoutInflater.from(ProductListActivity.this)
                    .inflate(R.layout.item_product_list, parent, false);

            Product p = products.get(pos);

            TextView txtInitials = convertView.findViewById(R.id.txtProductInitials);
            ImageView imgList    = convertView.findViewById(R.id.imgProductList);
            ImageUtils.load(ProductListActivity.this, p.imageUri, imgList, txtInitials, p.name);

            ((TextView) convertView.findViewById(R.id.txtProductListName)).setText(p.name);
            ((TextView) convertView.findViewById(R.id.txtProductListSku)).setText(
                p.sku != null ? "SKU: " + p.sku : "");
            ((TextView) convertView.findViewById(R.id.txtProductListPrice)).setText(
                CurrencyUtils.vnd(p.salePrice));

            TextView stockView = convertView.findViewById(R.id.txtProductListStock);
            boolean inStock  = p.stockQuantity > 0;
            boolean lowStock = inStock && p.stockQuantity <= p.minStockQuantity && p.minStockQuantity > 0;
            if (!inStock) {
                stockView.setText("Het hang");
                stockView.setBackgroundResource(R.drawable.badge_error);
                stockView.setTextColor(ContextCompat.getColor(ProductListActivity.this, R.color.status_error));
            } else if (lowStock) {
                stockView.setText("Con " + p.stockQuantity);
                stockView.setBackgroundResource(R.drawable.badge_warning);
                stockView.setTextColor(ContextCompat.getColor(ProductListActivity.this, R.color.status_warning));
            } else {
                stockView.setText("Con " + p.stockQuantity);
                stockView.setBackgroundResource(R.drawable.badge_success);
                stockView.setTextColor(ContextCompat.getColor(ProductListActivity.this, R.color.status_success));
            }
            return convertView;
        }
    }
}

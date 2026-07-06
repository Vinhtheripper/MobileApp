package com.example.mpos.inventory;

import android.content.Intent;
import android.database.Cursor;
import android.os.Bundle;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import com.example.mpos.R;
import com.example.mpos.MainActivity;
import com.example.mpos.database.DatabaseHelper;
import com.example.mpos.order.OrderListActivity;
import com.example.mpos.pos.PosActivity;
import com.example.mpos.settings.SettingsActivity;

import java.util.ArrayList;
import java.util.List;

public class InventoryActivity extends AppCompatActivity {

    static class StockRow {
        String name, sku; int qty, minQty;
    }

    private InventoryAdapter adapter;
    private DatabaseHelper db;
    private long shopId;

    @Override
    public void onCreate(Bundle state) {
        super.onCreate(state);
        setContentView(R.layout.activity_inventory);

        db = new DatabaseHelper(this);
        shopId = new com.example.mpos.auth.SessionManager(this).getShopId();

        adapter = new InventoryAdapter();
        ((ListView) findViewById(R.id.listInventory)).setAdapter(adapter);

        findViewById(R.id.btnBack).setOnClickListener(v -> finish());
        findViewById(R.id.btnStockAdjust).setOnClickListener(v ->
            startActivity(new Intent(this, StockAdjustmentActivity.class)));
        findViewById(R.id.btnInventoryHistory).setOnClickListener(v ->
            startActivity(new Intent(this, InventoryHistoryActivity.class)));

        bindStaffNav();
        load();
    }

    @Override
    protected void onResume() {
        super.onResume();
        load();
    }

    private void load() {
        Cursor c = db.getReadableDatabase().rawQuery(
            "SELECT name, sku, stock_quantity, min_stock_quantity FROM products WHERE is_active=1 AND shop_id=? ORDER BY stock_quantity ASC",
            new String[]{String.valueOf(shopId)});

        List<StockRow> rows = new ArrayList<>();
        int total = 0, low = 0, out = 0;
        try {
            while (c.moveToNext()) {
                StockRow r = new StockRow();
                r.name    = c.getString(0);
                r.sku     = c.getString(1);
                r.qty     = c.getInt(2);
                r.minQty  = c.getInt(3);
                rows.add(r);
                total++;
                if (r.qty <= 0) out++;
                else if (r.qty <= r.minQty) low++;
            }
        } finally { c.close(); }

        adapter.setRows(rows);

        TextView tvTotal = findViewById(R.id.txtStatTotal);
        TextView tvLow   = findViewById(R.id.txtStatLow);
        TextView tvOut   = findViewById(R.id.txtStatOut);
        if (tvTotal != null) tvTotal.setText(String.valueOf(total));
        if (tvLow   != null) tvLow.setText(String.valueOf(low));
        if (tvOut   != null) tvOut.setText(String.valueOf(out));
    }

    class InventoryAdapter extends BaseAdapter {
        private List<StockRow> rows = new ArrayList<>();
        void setRows(List<StockRow> r) { rows = r; notifyDataSetChanged(); }
        @Override public int getCount() { return rows.isEmpty() ? 1 : rows.size(); }
        @Override public StockRow getItem(int p) { return rows.isEmpty() ? null : rows.get(p); }
        @Override public long getItemId(int p) { return p; }

        @Override
        public View getView(int pos, View convertView, ViewGroup parent) {
            if (rows.isEmpty()) {
                TextView tv = new TextView(InventoryActivity.this);
                tv.setText("Chưa có sản phẩm tồn kho");
                tv.setGravity(Gravity.CENTER);
                tv.setPadding(0, 80, 0, 80);
                tv.setTextColor(getResources().getColor(R.color.text_secondary));
                return tv;
            }
            if (convertView == null || convertView instanceof TextView)
                convertView = LayoutInflater.from(InventoryActivity.this)
                    .inflate(R.layout.item_product_list, parent, false);

            StockRow row = rows.get(pos);
            ((TextView) convertView.findViewById(R.id.txtProductListName)).setText(row.name);
            ((TextView) convertView.findViewById(R.id.txtProductListSku)).setText(
                row.sku != null ? "SKU: " + row.sku : "");
            ((TextView) convertView.findViewById(R.id.txtProductListPrice)).setText("");

            TextView stockView = convertView.findViewById(R.id.txtProductListStock);
            boolean out    = row.qty <= 0;
            boolean low    = !out && row.qty <= row.minQty && row.minQty > 0;

            stockView.setText(out ? "Hết hàng" : "Còn " + row.qty);
            if (out) {
                stockView.setBackgroundResource(R.drawable.badge_error);
                stockView.setTextColor(ContextCompat.getColor(InventoryActivity.this, R.color.status_error));
            } else if (low) {
                stockView.setBackgroundResource(R.drawable.badge_warning);
                stockView.setTextColor(ContextCompat.getColor(InventoryActivity.this, R.color.status_warning));
            } else {
                stockView.setBackgroundResource(R.drawable.badge_success);
                stockView.setTextColor(ContextCompat.getColor(InventoryActivity.this, R.color.status_success));
            }

            // Initials
            TextView txtInitials = convertView.findViewById(R.id.txtProductInitials);
            if (txtInitials != null && row.name != null && row.name.length() >= 2) {
                txtInitials.setText(row.name.substring(0, 2).toUpperCase());
            }
            return convertView;
        }
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
}

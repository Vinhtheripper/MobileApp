package com.example.mpos.customer;

import android.content.Intent;
import android.os.Bundle;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.example.mpos.R;
import com.example.mpos.dao.CustomerDao;
import com.example.mpos.database.DatabaseHelper;
import com.example.mpos.model.Customer;

import java.util.ArrayList;
import java.util.List;

public class CustomerListActivity extends AppCompatActivity {

    private CustomerDao dao;
    private CustomerAdapter adapter;
    private EditText search;
    private List<Customer> items = new ArrayList<>();

    @Override
    public void onCreate(Bundle b) {
        super.onCreate(b);
        setContentView(R.layout.activity_customer_list);

        long shopId = new com.example.mpos.auth.SessionManager(this).getShopId();
        dao = new CustomerDao(new DatabaseHelper(this), shopId);
        search = findViewById(R.id.inputCustomerSearch);

        adapter = new CustomerAdapter();
        ListView list = findViewById(R.id.listCustomers);
        list.setAdapter(adapter);
        list.setOnItemClickListener((p, v, x, id) -> {
            Intent i = new Intent(this, CustomerDetailActivity.class);
            i.putExtra("customer_id", items.get(x).id);
            startActivity(i);
        });

        findViewById(R.id.btnBack).setOnClickListener(v -> finish());
        findViewById(R.id.btnSearchCustomer).setOnClickListener(v -> load());
        findViewById(R.id.btnAddCustomer).setOnClickListener(v ->
            startActivity(new Intent(this, CustomerFormActivity.class)));

        load();
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (dao != null) load();
    }

    private void load() {
        items = dao.search(search.getText().toString());
        adapter.setItems(items);
    }

    class CustomerAdapter extends BaseAdapter {
        private List<Customer> data = new ArrayList<>();
        void setItems(List<Customer> list) { data = list; notifyDataSetChanged(); }
        @Override public int getCount() { return data.isEmpty() ? 1 : data.size(); }
        @Override public Customer getItem(int p) { return data.isEmpty() ? null : data.get(p); }
        @Override public long getItemId(int p) { return p; }

        @Override
        public View getView(int pos, View convertView, ViewGroup parent) {
            if (data.isEmpty()) {
                TextView tv = new TextView(CustomerListActivity.this);
                tv.setText("Chưa có khách hàng\nNhấn '+ Thêm' để tạo mới");
                tv.setGravity(Gravity.CENTER);
                tv.setPadding(0, 80, 0, 80);
                tv.setTextColor(getResources().getColor(R.color.text_secondary));
                return tv;
            }
            if (convertView == null || convertView instanceof TextView)
                convertView = LayoutInflater.from(CustomerListActivity.this)
                    .inflate(R.layout.item_customer, parent, false);

            Customer c = data.get(pos);

            // Try new item_customer layout first; fall back to simple text if not found
            TextView nameView  = convertView.findViewById(R.id.txtCustomerName);
            TextView phoneView = convertView.findViewById(R.id.txtCustomerPhone);
            TextView ptsView   = convertView.findViewById(R.id.txtCustomerPoints);

            if (nameView != null) {
                nameView.setText(c.fullName != null ? c.fullName : "Khách hàng");
                if (phoneView != null) phoneView.setText(c.phone);
                if (ptsView != null) ptsView.setText(c.loyaltyPoints + " điểm");
            }
            return convertView;
        }
    }
}

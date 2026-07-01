package com.example.mpos.fulfillment;

import android.database.Cursor;
import android.os.Bundle;
import android.widget.ArrayAdapter;
import android.widget.ListView;

import androidx.appcompat.app.AppCompatActivity;

import com.example.mpos.R;
import com.example.mpos.auth.PermissionHelper;
import com.example.mpos.constants.OrderConstants;
import com.example.mpos.database.DatabaseHelper;
import com.example.mpos.service.ShippingServiceMock;
import com.example.mpos.utils.CurrencyUtils;

import java.util.ArrayList;
import java.util.List;

public class FulfillmentActivity extends AppCompatActivity {
    @Override public void onCreate(Bundle state) {
        super.onCreate(state);
        if (!PermissionHelper.requireAdmin(this)) return;
        setContentView(R.layout.activity_fulfillment);
        findViewById(R.id.btnBack).setOnClickListener(v -> finish());
        List<String> rows = new ArrayList<>();
        Cursor c = new DatabaseHelper(this).getReadableDatabase().rawQuery(
                "SELECT order_code,channel,status,total_amount,delivery_address FROM orders WHERE status IN (?,?,?,?,?) ORDER BY created_at DESC",
                new String[]{OrderConstants.STATUS_FULFILLMENT_PENDING, OrderConstants.STATUS_PACKING, OrderConstants.STATUS_WAITING_SHIPPER, OrderConstants.STATUS_SHIPPING, OrderConstants.STATUS_DELIVERED});
        try {
            while (c.moveToNext()) {
                ShippingServiceMock.ShipmentQuote quote = new ShippingServiceMock().quote("GHN");
                rows.add(c.getString(0) + " • " + c.getString(2) + "\n" + c.getString(1) + " • " + CurrencyUtils.vnd(c.getLong(3)) + "\nGHN mock: " + CurrencyUtils.vnd(quote.fee) + " • " + quote.eta);
            }
        } finally {
            c.close();
        }
        if (rows.isEmpty()) rows.add("Chưa có đơn giao hàng trong hàng đợi fulfillment");
        ((ListView) findViewById(R.id.listFulfillment)).setAdapter(new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, rows));
    }
}

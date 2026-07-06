package com.example.mpos.ui;

import android.content.Intent;
import android.os.Bundle;
import androidx.appcompat.app.AppCompatActivity;
import com.example.mpos.R;
import com.example.mpos.customer.CustomerListActivity;
import com.example.mpos.employee.ManageEmployeeActivity;
import com.example.mpos.logistics.ShippingActivity;
import com.example.mpos.omnichannel.OmnichannelActivity;
import com.example.mpos.product.ProductListActivity;
import com.example.mpos.profile.ProfileActivity;
import com.example.mpos.shift.ShiftActivity;
import com.example.mpos.sync.SyncStatusActivity;

public class MoreActivity extends AppCompatActivity {
    @Override
    public void onCreate(Bundle b) {
        super.onCreate(b);
        setContentView(R.layout.activity_more);
        BottomNavHelper.bind(this);

        // Kết nối kênh
        findViewById(R.id.itemOmnichannel).setOnClickListener(v ->
            startActivity(new Intent(this, OmnichannelActivity.class)));
        findViewById(R.id.itemShipping).setOnClickListener(v ->
            startActivity(new Intent(this, ShippingActivity.class)));

        // Vận hành
        findViewById(R.id.itemShift).setOnClickListener(v ->
            startActivity(new Intent(this, ShiftActivity.class)));
        findViewById(R.id.itemSync).setOnClickListener(v ->
            startActivity(new Intent(this, SyncStatusActivity.class)));
        findViewById(R.id.itemInbox).setOnClickListener(v ->
            startActivity(new Intent(this, OmnichannelActivity.class)));

        // Quản lý
        findViewById(R.id.itemProducts).setOnClickListener(v ->
            startActivity(new Intent(this, ProductListActivity.class)));
        findViewById(R.id.itemCustomers).setOnClickListener(v ->
            startActivity(new Intent(this, CustomerListActivity.class)));
        findViewById(R.id.itemEmployees).setOnClickListener(v ->
            startActivity(new Intent(this, ManageEmployeeActivity.class)));
        findViewById(R.id.itemProfile).setOnClickListener(v ->
            startActivity(new Intent(this, ProfileActivity.class)));
    }
}

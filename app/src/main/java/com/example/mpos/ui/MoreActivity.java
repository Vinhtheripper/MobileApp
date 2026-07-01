package com.example.mpos.ui;

import android.content.Intent;
import android.os.Bundle;
import androidx.appcompat.app.AppCompatActivity;
import com.example.mpos.R;
import com.example.mpos.auth.PermissionHelper;
import com.example.mpos.customer.CustomerListActivity;
import com.example.mpos.employee.AuditLogActivity;
import com.example.mpos.employee.UserListActivity;
import com.example.mpos.omnichannel.UnifiedInboxActivity;
import com.example.mpos.fulfillment.FulfillmentActivity;
import com.example.mpos.product.ProductListActivity;
import com.example.mpos.profile.ProfileActivity;
import com.example.mpos.settings.SettingsActivity;
import com.example.mpos.shift.ShiftActivity;
import com.example.mpos.sync.SyncStatusActivity;

/** Utility hub for modules that are not in the primary selling flow. */
public class MoreActivity extends AppCompatActivity {
    @Override
    public void onCreate(Bundle b) {
        super.onCreate(b);
        setContentView(R.layout.activity_more);
        BottomNavHelper.bind(this);

        boolean admin = PermissionHelper.isAdmin(this);
        PermissionHelper.showIf(findViewById(R.id.itemSync), admin);
        PermissionHelper.showIf(findViewById(R.id.itemFulfillment), admin);
        PermissionHelper.showIf(findViewById(R.id.itemProducts), admin);
        PermissionHelper.showIf(findViewById(R.id.labelAdmin), admin);
        PermissionHelper.showIf(findViewById(R.id.groupAdmin), admin);

        findViewById(R.id.itemShift).setOnClickListener(v -> startActivity(new Intent(this, ShiftActivity.class)));
        findViewById(R.id.itemSync).setOnClickListener(v -> startActivity(new Intent(this, SyncStatusActivity.class)));
        findViewById(R.id.itemInbox).setOnClickListener(v -> startActivity(new Intent(this, UnifiedInboxActivity.class)));
        findViewById(R.id.itemFulfillment).setOnClickListener(v -> startActivity(new Intent(this, FulfillmentActivity.class)));
        findViewById(R.id.itemProducts).setOnClickListener(v -> startActivity(new Intent(this, ProductListActivity.class)));
        findViewById(R.id.itemCustomers).setOnClickListener(v -> startActivity(new Intent(this, CustomerListActivity.class)));
        findViewById(R.id.itemProfile).setOnClickListener(v -> startActivity(new Intent(this, ProfileActivity.class)));
        findViewById(R.id.itemUsers).setOnClickListener(v -> startActivity(new Intent(this, UserListActivity.class)));
        findViewById(R.id.itemSettings).setOnClickListener(v -> startActivity(new Intent(this, SettingsActivity.class)));
        findViewById(R.id.itemAudit).setOnClickListener(v -> startActivity(new Intent(this, AuditLogActivity.class)));
    }
}

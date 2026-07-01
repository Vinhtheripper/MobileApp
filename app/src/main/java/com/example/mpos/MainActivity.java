package com.example.mpos;

import android.os.Bundle;
import android.content.Intent;
import android.view.View;

import androidx.appcompat.app.AppCompatActivity;

import com.example.mpos.auth.LoginActivity;
import com.example.mpos.customer.CustomerListActivity;
import com.example.mpos.auth.PermissionHelper;
import com.example.mpos.auth.SessionManager;
import com.example.mpos.omnichannel.UnifiedInboxActivity;
import com.example.mpos.order.OrderListActivity;
import com.example.mpos.inventory.InventoryActivity;
import com.example.mpos.report.ReportActivity;
import com.example.mpos.pos.PosActivity;
import com.example.mpos.profile.ProfileActivity;
import com.example.mpos.product.ProductListActivity;
import com.example.mpos.shift.ShiftActivity;
import com.example.mpos.sync.SyncStatusActivity;
import com.example.mpos.ui.MoreActivity;
import com.example.mpos.ui.BottomNavHelper;

/** Entry dashboard. Feature screens are added under their business packages. */
public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (!new SessionManager(this).isLoggedIn()) { startActivity(new Intent(this, LoginActivity.class)); finish(); return; }
        setContentView(R.layout.activity_main);
        BottomNavHelper.bind(this);
        boolean admin = PermissionHelper.isAdmin(this);
        PermissionHelper.showIf(findViewById(R.id.btnProducts), admin);
        PermissionHelper.showIf(findViewById(R.id.btnInventory), admin);
        PermissionHelper.showIf(findViewById(R.id.btnReports), PermissionHelper.canViewReports(this));
        PermissionHelper.showIf(findViewById(R.id.btnSync), admin);
        findViewById(R.id.btnNewSale).setOnClickListener(v -> startActivity(new Intent(this, PosActivity.class)));
        findViewById(R.id.btnShift).setOnClickListener(v -> startActivity(new Intent(this, ShiftActivity.class)));
        findViewById(R.id.btnSync).setOnClickListener(v -> startActivity(new Intent(this, SyncStatusActivity.class)));
        findViewById(R.id.btnInbox).setOnClickListener(v -> startActivity(new Intent(this, UnifiedInboxActivity.class)));
        findViewById(R.id.btnProducts).setOnClickListener(v -> { if (PermissionHelper.canManageCatalog(this)) startActivity(new Intent(this, ProductListActivity.class)); });
        findViewById(R.id.btnCustomers).setOnClickListener(v -> startActivity(new Intent(this, CustomerListActivity.class)));
        findViewById(R.id.btnProfile).setOnClickListener(v -> startActivity(new Intent(this, ProfileActivity.class)));
        findViewById(R.id.btnMore).setOnClickListener(v -> startActivity(new Intent(this, MoreActivity.class)));
        findViewById(R.id.btnOrders).setOnClickListener(v -> startActivity(new Intent(this, OrderListActivity.class)));
        findViewById(R.id.btnInventory).setOnClickListener(v -> { if (PermissionHelper.canManageInventory(this)) startActivity(new Intent(this, InventoryActivity.class)); });
        findViewById(R.id.btnReports).setOnClickListener(v -> { if (PermissionHelper.canViewReports(this)) startActivity(new Intent(this, ReportActivity.class)); });
        findViewById(R.id.btnLogout).setOnClickListener(v -> { new SessionManager(this).clear(); startActivity(new Intent(this, LoginActivity.class)); finish(); });
    }
}

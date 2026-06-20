package com.example.mpos;

import android.os.Bundle;
import android.content.Intent;
import android.view.View;

import androidx.appcompat.app.AppCompatActivity;

import com.example.mpos.auth.LoginActivity;
import com.example.mpos.auth.SessionManager;
import com.example.mpos.omnichannel.UnifiedInboxActivity;
import com.example.mpos.pos.PosActivity;
import com.example.mpos.shift.ShiftActivity;
import com.example.mpos.sync.SyncStatusActivity;

/** Entry dashboard. Feature screens are added under their business packages. */
public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (!new SessionManager(this).isLoggedIn()) { startActivity(new Intent(this, LoginActivity.class)); finish(); return; }
        setContentView(R.layout.activity_main);
        findViewById(R.id.btnNewSale).setOnClickListener(v -> startActivity(new Intent(this, PosActivity.class)));
        findViewById(R.id.btnShift).setOnClickListener(v -> startActivity(new Intent(this, ShiftActivity.class)));
        findViewById(R.id.btnSync).setOnClickListener(v -> startActivity(new Intent(this, SyncStatusActivity.class)));
        findViewById(R.id.btnInbox).setOnClickListener(v -> startActivity(new Intent(this, UnifiedInboxActivity.class)));
        findViewById(R.id.btnLogout).setOnClickListener(v -> { new SessionManager(this).clear(); startActivity(new Intent(this, LoginActivity.class)); finish(); });
    }
}

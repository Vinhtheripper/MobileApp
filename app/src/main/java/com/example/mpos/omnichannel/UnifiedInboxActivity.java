package com.example.mpos.omnichannel;

import android.content.Intent;
import android.os.Bundle;
import androidx.appcompat.app.AppCompatActivity;

public class UnifiedInboxActivity extends AppCompatActivity {
    @Override
    public void onCreate(Bundle state) {
        super.onCreate(state);
        startActivity(new Intent(this, OmnichannelActivity.class));
        finish();
    }
}

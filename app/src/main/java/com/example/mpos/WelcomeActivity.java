package com.example.mpos;

import android.content.Intent;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.ImageView;

import java.util.concurrent.Executors;

import androidx.appcompat.app.AppCompatActivity;

import com.example.mpos.auth.LoginActivity;
import com.example.mpos.auth.RegisterActivity;

public class WelcomeActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().setFlags(
            WindowManager.LayoutParams.FLAG_FULLSCREEN,
            WindowManager.LayoutParams.FLAG_FULLSCREEN
        );
        setContentView(R.layout.activity_welcome);

        // Load image off main thread to avoid UI freeze
        ImageView imgWelcome = findViewById(R.id.imgWelcome);
        Executors.newSingleThreadExecutor().execute(() -> {
            android.graphics.Bitmap bm = BitmapFactory.decodeResource(getResources(), R.drawable.bg_welcome);
            new Handler(Looper.getMainLooper()).post(() -> imgWelcome.setImageBitmap(bm));
        });

        Button btnPrimary   = findViewById(R.id.btnGetStarted);
        Button btnSecondary = findViewById(R.id.btnHaveAccount);

        btnPrimary.setOnClickListener(v -> go(RegisterActivity.class));
        btnSecondary.setOnClickListener(v -> go(LoginActivity.class));
    }

    private void go(Class<?> target) {
        startActivity(new Intent(this, target));
        overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
        finish();
    }


}

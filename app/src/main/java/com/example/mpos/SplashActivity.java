package com.example.mpos;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.animation.ValueAnimator;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.View;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.view.animation.DecelerateInterpolator;
import android.view.animation.OvershootInterpolator;

import androidx.appcompat.app.AppCompatActivity;

import com.example.mpos.auth.LoginActivity;
import com.example.mpos.auth.SessionManager;
import com.example.mpos.dao.ShopDao;
import com.example.mpos.database.DatabaseHelper;
import com.example.mpos.model.Shop;
import com.example.mpos.model.User;
import com.example.mpos.shop.CreateShopActivity;
import com.example.mpos.shop.ShopListActivity;

import java.util.List;

/**
 * Branded splash screen — Quầy mPOS Pro.
 *
 * Enhanced loading experience:
 *  1. Mascot fades in with overshoot bounce scale (0.5 → 1.1 → 1.0)
 *  2. Glow rings pulse outward (scale + alpha)
 *  3. Brand name + tagline slide up sequentially
 *  4. Three dots animate in sequence (bounce up/down)
 *  5. After 2200ms: routes based on session state
 */
public class SplashActivity extends AppCompatActivity {

    private static final int SPLASH_DELAY_MS = 2400;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_splash);

        View mascot      = findViewById(R.id.imgMascot);
        View brand       = findViewById(R.id.txtBrandName);
        View tagline     = findViewById(R.id.txtTagline);
        View dots        = findViewById(R.id.layoutDots);
        View glowOuter   = findViewById(R.id.glowRingOuter);
        View glowInner   = findViewById(R.id.glowRingInner);
        View dot1        = findViewById(R.id.dot1);
        View dot2        = findViewById(R.id.dot2);
        View dot3        = findViewById(R.id.dot3);

        // ── 1. MASCOT: overshoot bounce scale + fade ──
        ObjectAnimator scaleX  = ObjectAnimator.ofFloat(mascot, "scaleX", 0.4f, 1.12f, 1.0f);
        ObjectAnimator scaleY  = ObjectAnimator.ofFloat(mascot, "scaleY", 0.4f, 1.12f, 1.0f);
        ObjectAnimator fadeM   = ObjectAnimator.ofFloat(mascot, "alpha", 0f, 1f);
        AnimatorSet mascotAnim = new AnimatorSet();
        mascotAnim.playTogether(scaleX, scaleY, fadeM);
        mascotAnim.setDuration(600);
        mascotAnim.setInterpolator(new OvershootInterpolator(1.5f));
        mascotAnim.setStartDelay(100);

        // ── 2. GLOW INNER ring: pulse scale ──
        ObjectAnimator glowInScale = ObjectAnimator.ofFloat(glowInner, "scaleX", 0.8f, 1.2f, 1.0f);
        ObjectAnimator glowInScaleY = ObjectAnimator.ofFloat(glowInner, "scaleY", 0.8f, 1.2f, 1.0f);
        ObjectAnimator glowInFade  = ObjectAnimator.ofFloat(glowInner, "alpha", 0f, 0.8f, 0.4f);
        AnimatorSet glowInAnim = new AnimatorSet();
        glowInAnim.playTogether(glowInScale, glowInScaleY, glowInFade);
        glowInAnim.setDuration(700);
        glowInAnim.setInterpolator(new DecelerateInterpolator());
        glowInAnim.setStartDelay(200);

        // ── 3. GLOW OUTER ring: slower pulse ──
        ObjectAnimator glowOutScale  = ObjectAnimator.ofFloat(glowOuter, "scaleX", 0.6f, 1.4f, 1.0f);
        ObjectAnimator glowOutScaleY = ObjectAnimator.ofFloat(glowOuter, "scaleY", 0.6f, 1.4f, 1.0f);
        ObjectAnimator glowOutFade   = ObjectAnimator.ofFloat(glowOuter, "alpha", 0f, 0.5f, 0.2f);
        AnimatorSet glowOutAnim = new AnimatorSet();
        glowOutAnim.playTogether(glowOutScale, glowOutScaleY, glowOutFade);
        glowOutAnim.setDuration(900);
        glowOutAnim.setInterpolator(new DecelerateInterpolator());
        glowOutAnim.setStartDelay(150);

        // ── 4. BRAND TEXT: slide up + fade ──
        ObjectAnimator fadeB  = ObjectAnimator.ofFloat(brand, "alpha", 0f, 1f);
        ObjectAnimator slideB = ObjectAnimator.ofFloat(brand, "translationY", 20f, 0f);
        AnimatorSet brandAnim = new AnimatorSet();
        brandAnim.playTogether(fadeB, slideB);
        brandAnim.setDuration(450);
        brandAnim.setInterpolator(new DecelerateInterpolator());
        brandAnim.setStartDelay(450);

        // ── 5. TAGLINE: fade ──
        ObjectAnimator fadeT = ObjectAnimator.ofFloat(tagline, "alpha", 0f, 1f);
        fadeT.setDuration(400);
        fadeT.setStartDelay(620);

        // ── 6. DOTS container: fade in ──
        ObjectAnimator fadeD = ObjectAnimator.ofFloat(dots, "alpha", 0f, 1f);
        fadeD.setDuration(300);
        fadeD.setStartDelay(800);

        // Start all entrance animations
        mascotAnim.start();
        glowInAnim.start();
        glowOutAnim.start();
        brandAnim.start();
        fadeT.start();
        fadeD.start();

        // ── 7. DOT bounce loop — starts after dots visible ──
        new Handler(Looper.getMainLooper()).postDelayed(() ->
            startDotAnimation(dot1, dot2, dot3), 900);

        // ── 8. Continuous mascot float animation ──
        new Handler(Looper.getMainLooper()).postDelayed(() ->
            startMascotFloat(mascot), 700);

        // Route after delay
        new Handler(Looper.getMainLooper()).postDelayed(this::navigateNext, SPLASH_DELAY_MS);
    }

    /**
     * Gentle up-down float for the mascot (infinite)
     */
    private void startMascotFloat(View mascot) {
        ObjectAnimator floatAnim = ObjectAnimator.ofFloat(mascot, "translationY", 0f, -10f, 0f);
        floatAnim.setDuration(1800);
        floatAnim.setRepeatCount(ValueAnimator.INFINITE);
        floatAnim.setRepeatMode(ValueAnimator.RESTART);
        floatAnim.setInterpolator(new AccelerateDecelerateInterpolator());
        floatAnim.start();
    }

    /**
     * Sequential dot bounce — dot1, dot2, dot3 in loop
     */
    private void startDotAnimation(View dot1, View dot2, View dot3) {
        animateDot(dot1, 0);
        animateDot(dot2, 200);
        animateDot(dot3, 400);
    }

    private void animateDot(View dot, long delay) {
        ObjectAnimator bounce = ObjectAnimator.ofFloat(dot, "translationY", 0f, -12f, 0f);
        bounce.setDuration(600);
        bounce.setRepeatCount(ValueAnimator.INFINITE);
        bounce.setRepeatMode(ValueAnimator.RESTART);
        bounce.setInterpolator(new AccelerateDecelerateInterpolator());
        bounce.setStartDelay(delay);
        bounce.start();
    }

    private void navigateNext() {
        SessionManager session = new SessionManager(this);
        Class<?> target;
        if (session.isLoggedIn()) {
            target = session.hasShopSelected() ? MainActivity.class : com.example.mpos.shop.ShopListActivity.class;
        } else {
            target = WelcomeActivity.class;
        }
        startActivity(new Intent(this, target));
        overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
        finish();
    }
}

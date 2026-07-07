package com.example.mpos.auth;

import android.util.Log;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

/**
 * Mirrors the local SQLite login into Firebase Auth so every user gets
 * a unique UID — used to enforce per-user data isolation in RTDB.
 *
 * Call signIn() after a successful SQLite login; it's fire-and-forget
 * (runs on a background thread) so it never blocks the UI.
 */
public class FirebaseAuthHelper {

    private static final String TAG    = "FbAuth";
    private static final String DB_URL =
        "https://mobile-app-19c4a-default-rtdb.asia-southeast1.firebasedatabase.app";

    /** Sign into Firebase with the same email/password used for the local login. */
    public static void signIn(String email, String plainPassword, long shopId) {
        FirebaseAuth auth = FirebaseAuth.getInstance();

        auth.signInWithEmailAndPassword(email, plainPassword)
            .addOnSuccessListener(result -> {
                String uid = result.getUser().getUid();
                Log.d(TAG, "Firebase sign-in OK uid=" + uid);
                grantShopAccess(uid, shopId);
            })
            .addOnFailureListener(e -> {
                // Account doesn't exist yet — create it
                auth.createUserWithEmailAndPassword(email, plainPassword)
                    .addOnSuccessListener(result -> {
                        String uid = result.getUser().getUid();
                        Log.d(TAG, "Firebase account created uid=" + uid);
                        grantShopAccess(uid, shopId);
                    })
                    .addOnFailureListener(e2 ->
                        Log.w(TAG, "Firebase auth failed: " + e2.getMessage()));
            });
    }

    /**
     * Write shop access grant: users/{uid}/shop_access/{shopId} = true
     * This is what Security Rules check to allow read/write.
     */
    private static void grantShopAccess(String uid, long shopId) {
        if (shopId <= 0) return;
        DatabaseReference ref = FirebaseDatabase.getInstance(DB_URL)
            .getReference("users")
            .child(uid)
            .child("shop_access")
            .child(String.valueOf(shopId));
        ref.setValue(true)
           .addOnFailureListener(e -> Log.w(TAG, "grantShopAccess fail: " + e.getMessage()));
    }

    /** Called on shop switch — grants access to the newly selected shop. */
    public static void onShopSelected(long shopId) {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null || shopId <= 0) return;
        grantShopAccess(user.getUid(), shopId);
    }

    /** Sign out of Firebase when user logs out of the app. */
    public static void signOut() {
        FirebaseAuth.getInstance().signOut();
    }

    public static String currentUid() {
        FirebaseUser u = FirebaseAuth.getInstance().getCurrentUser();
        return u != null ? u.getUid() : null;
    }
}

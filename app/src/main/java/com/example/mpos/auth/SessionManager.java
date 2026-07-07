package com.example.mpos.auth;

import android.content.Context;
import android.content.SharedPreferences;

import com.example.mpos.model.User;

public class SessionManager {
    private static final String PREF = "mpos_session";
    private final SharedPreferences preferences;
    public SessionManager(Context context) { preferences = context.getSharedPreferences(PREF, Context.MODE_PRIVATE); }

    public void save(User user) {
        preferences.edit()
            .putLong("id", user.id)
            .putLong("employee", user.employeeId)
            .putString("name", user.username)
            .putString("role", user.role)
            .apply();
    }

    public void saveShop(long shopId, String shopName, String shopRole) {
        preferences.edit()
            .putLong("shop_id", shopId)
            .putString("shop_name", shopName)
            .putString("shop_role", shopRole)
            .apply();
    }

    public boolean isLoggedIn() { return preferences.getLong("id", -1) > 0; }

    public boolean hasShopSelected() { return preferences.getLong("shop_id", -1) > 0; }

    public void saveSelectedRole(String role) {
        preferences.edit().putString("selected_role", role).apply();
    }

    public User getUser() {
        User user = new User();
        user.id = preferences.getLong("id", -1);
        user.employeeId = preferences.getLong("employee", -1);
        user.username = preferences.getString("name", "");
        String selected = preferences.getString("selected_role", "");
        user.role = selected.isEmpty() ? preferences.getString("role", "STAFF") : selected;
        return user;
    }

    public long getShopId() { return preferences.getLong("shop_id", -1); }
    public String getShopName() { return preferences.getString("shop_name", "Cửa hàng"); }
    public String getShopRole() { return preferences.getString("shop_role", "STAFF"); }

    public void clear() { preferences.edit().clear().apply(); }
}
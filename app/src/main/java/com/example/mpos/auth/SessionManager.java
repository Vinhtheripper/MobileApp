package com.example.mpos.auth;

import android.content.Context;
import android.content.SharedPreferences;

import com.example.mpos.model.User;

public class SessionManager {
    private static final String PREF = "mpos_session";
    private final SharedPreferences preferences;
    public SessionManager(Context context) { preferences = context.getSharedPreferences(PREF, Context.MODE_PRIVATE); }
    public void save(User user) { preferences.edit().putLong("id", user.id).putLong("employee", user.employeeId).putString("name", user.username).putString("role", user.role).apply(); }
    public boolean isLoggedIn() { return preferences.getLong("id", -1) > 0; }
    public User getUser() { User user = new User(); user.id = preferences.getLong("id", -1); user.employeeId = preferences.getLong("employee", -1); user.username = preferences.getString("name", ""); user.role = preferences.getString("role", "STAFF"); return user; }
    public void clear() { preferences.edit().clear().apply(); }
}

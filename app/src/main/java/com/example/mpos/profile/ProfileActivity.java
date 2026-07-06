package com.example.mpos.profile;

import android.content.ContentValues;
import android.content.Intent;
import android.database.Cursor;
import android.os.Bundle;
import android.view.View;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.example.mpos.R;
import com.example.mpos.auth.LoginActivity;
import com.example.mpos.auth.SessionManager;
import com.example.mpos.dao.SettingsDao;
import com.example.mpos.database.DatabaseHelper;
import com.example.mpos.model.User;
import com.example.mpos.omnichannel.OmnichannelActivity;
import com.example.mpos.settings.SettingsActivity;
import com.example.mpos.auth.PasswordUtils;

public class ProfileActivity extends AppCompatActivity {

    private SessionManager session;
    private DatabaseHelper db;
    private SettingsDao settingsDao;
    private boolean passwordExpanded = false;

    @Override
    public void onCreate(Bundle state) {
        super.onCreate(state);
        setContentView(R.layout.activity_profile);
        session     = new SessionManager(this);
        db          = new DatabaseHelper(this);
        settingsDao = new SettingsDao(db);
        bindViews();
        setupActions();
    }

    private void bindViews() {
        User user = session.getUser();
        if (user == null) return;

        String fullName = getFullName(user);
        String initials = fullName.length() >= 2 ? fullName.substring(0, 2).toUpperCase()
            : fullName.isEmpty() ? "?" : fullName.substring(0, 1).toUpperCase();
        setText(R.id.txtAvatarInitials, initials);
        setText(R.id.txtProfileName, fullName.isEmpty() ? user.username : fullName);

        String roleLabel = "ADMIN".equals(user.role) ? "Admin"
            : "MANAGER".equals(user.role) ? "Quản lý" : "Nhân viên";
        setText(R.id.txtProfileRole, roleLabel);
        setText(R.id.txtProfileShop, settingsDao.get("store_name", "Cửa hàng của tôi"));
        setText(R.id.txtFullName, fullName.isEmpty() ? "Chưa cập nhật" : fullName);
        setText(R.id.txtEmail, user.username);
        String phone = getEmployeePhone(user);
        setText(R.id.txtPhone, phone.isEmpty() ? "Chưa cập nhật" : phone);

        String bankCode = settingsDao.get("bank_code", "");
        setText(R.id.txtWalletStatus, bankCode.isEmpty() ? "Chưa cấu hình tài khoản" : "VietQR (" + bankCode + ") đã cài");

        android.content.SharedPreferences prefs = getSharedPreferences("mpos_omnichannel", MODE_PRIVATE);
        int connected = 0;
        for (String k : new String[]{"shopee", "tiktok", "lazada"})
            if (prefs.getBoolean(k + "_connected", false)) connected++;
        setText(R.id.txtChannelStatus, connected > 0 ? connected + " kênh đã kết nối" : "Shopee, TikTok, Lazada");
    }

    private void setupActions() {
        findViewById(R.id.btnBack).setOnClickListener(v -> finish());

        findViewById(R.id.rowFullName).setOnClickListener(v ->
            showEditDialog("Cập nhật họ tên", "Họ và tên đầy đủ", false, this::updateFullName));

        findViewById(R.id.rowPhone).setOnClickListener(v ->
            showEditDialog("Cập nhật số điện thoại", "Số điện thoại", false, this::updatePhone));

        LinearLayout layoutPw = findViewById(R.id.layoutChangePassword);
        findViewById(R.id.rowChangePassword).setOnClickListener(v -> {
            passwordExpanded = !passwordExpanded;
            layoutPw.setVisibility(passwordExpanded ? View.VISIBLE : View.GONE);
        });

        findViewById(R.id.btnSavePassword).setOnClickListener(v -> handleChangePassword());
        findViewById(R.id.rowStoreSettings).setOnClickListener(v ->
            startActivity(new Intent(this, SettingsActivity.class)));
        findViewById(R.id.rowWallet).setOnClickListener(v ->
            startActivity(new Intent(this, SettingsActivity.class)));
        findViewById(R.id.rowOmnichannel).setOnClickListener(v ->
            startActivity(new Intent(this, OmnichannelActivity.class)));

        findViewById(R.id.btnProfileLogout).setOnClickListener(v ->
            new AlertDialog.Builder(this)
                .setTitle("Đăng xuất")
                .setMessage("Bạn có chắc muốn đăng xuất không?")
                .setPositiveButton("Đăng xuất", (d, w) -> {
                    session.clear();
                    Intent i = new Intent(this, com.example.mpos.WelcomeActivity.class);
                    i.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                    startActivity(i);
                    finish();
                })
                .setNegativeButton("Hủy", null).show());
    }

    private void handleChangePassword() {
        String current = et(R.id.etCurrentPassword);
        String newPw   = et(R.id.etNewPassword);
        String confirm = et(R.id.etConfirmPassword);
        if (current.isEmpty() || newPw.isEmpty() || confirm.isEmpty()) {
            toast("Vui lòng nhập đầy đủ"); return;
        }
        if (!newPw.equals(confirm)) { toast("Mật khẩu xác nhận không khớp"); return; }
        if (newPw.length() < 6)    { toast("Mật khẩu phải ít nhất 6 ký tự"); return; }

        User user = session.getUser();
        Cursor c = db.getReadableDatabase().rawQuery(
            "SELECT password_hash FROM users WHERE id=?", new String[]{String.valueOf(user.id)});
        String stored = "";
        try { if (c.moveToFirst()) stored = c.getString(0); } finally { c.close(); }

        if (!PasswordUtils.verify(current, stored)) { toast("Mật khẩu hiện tại không đúng"); return; }

        ContentValues cv = new ContentValues();
        cv.put("password_hash", PasswordUtils.hash(newPw));
        cv.put("updated_at", System.currentTimeMillis());
        db.getWritableDatabase().update("users", cv, "id=?", new String[]{String.valueOf(user.id)});
        toast("Đổi mật khẩu thành công");
        findViewById(R.id.layoutChangePassword).setVisibility(View.GONE);
        passwordExpanded = false;
    }

    private interface OnSave { void save(String v); }
    private void showEditDialog(String title, String hint, boolean numeric, OnSave cb) {
        EditText et = new EditText(this);
        et.setHint(hint);
        et.setPadding(dp(20), dp(12), dp(20), dp(12));
        if (numeric) et.setInputType(android.text.InputType.TYPE_CLASS_NUMBER);
        new AlertDialog.Builder(this).setTitle(title).setView(et)
            .setPositiveButton("Lưu", (d, w) -> { String v = et.getText().toString().trim(); if (!v.isEmpty()) cb.save(v); })
            .setNegativeButton("Hủy", null).show();
    }

    private void updateFullName(String name) {
        User user = session.getUser();
        ContentValues cv = new ContentValues();
        cv.put("full_name", name); cv.put("updated_at", System.currentTimeMillis());
        long empId = getEmployeeId(user);
        if (empId > 0) {
            db.getWritableDatabase().update("employees", cv, "id=?", new String[]{String.valueOf(empId)});
        } else {
            cv.put("is_active", 1);
            long nId = db.getWritableDatabase().insert("employees", null, cv);
            ContentValues ucv = new ContentValues(); ucv.put("employee_id", nId);
            db.getWritableDatabase().update("users", ucv, "id=?", new String[]{String.valueOf(user.id)});
        }
        setText(R.id.txtFullName, name); setText(R.id.txtProfileName, name);
        String ini = name.length() >= 2 ? name.substring(0, 2).toUpperCase() : name.toUpperCase();
        setText(R.id.txtAvatarInitials, ini);
        toast("Đã cập nhật họ tên");
    }

    private void updatePhone(String phone) {
        User user = session.getUser();
        ContentValues cv = new ContentValues(); cv.put("phone", phone); cv.put("updated_at", System.currentTimeMillis());
        long empId = getEmployeeId(user);
        if (empId > 0) db.getWritableDatabase().update("employees", cv, "id=?", new String[]{String.valueOf(empId)});
        setText(R.id.txtPhone, phone); toast("Đã cập nhật số điện thoại");
    }

    private String getFullName(User user) {
        Cursor c = db.getReadableDatabase().rawQuery(
            "SELECT e.full_name FROM employees e JOIN users u ON u.employee_id=e.id WHERE u.id=?",
            new String[]{String.valueOf(user.id)});
        try { return c.moveToFirst() && c.getString(0) != null ? c.getString(0) : ""; } finally { c.close(); }
    }

    private String getEmployeePhone(User user) {
        Cursor c = db.getReadableDatabase().rawQuery(
            "SELECT e.phone FROM employees e JOIN users u ON u.employee_id=e.id WHERE u.id=?",
            new String[]{String.valueOf(user.id)});
        try { return c.moveToFirst() && c.getString(0) != null ? c.getString(0) : ""; } finally { c.close(); }
    }

    private long getEmployeeId(User user) {
        Cursor c = db.getReadableDatabase().rawQuery(
            "SELECT employee_id FROM users WHERE id=?", new String[]{String.valueOf(user.id)});
        try { return c.moveToFirst() && !c.isNull(0) ? c.getLong(0) : -1; } finally { c.close(); }
    }

    private void setText(int id, String text) { ((TextView) findViewById(id)).setText(text); }
    private String et(int id) { return ((EditText) findViewById(id)).getText().toString().trim(); }
    private void toast(String msg) { Toast.makeText(this, msg, Toast.LENGTH_SHORT).show(); }
    private int dp(int dp) { return Math.round(dp * getResources().getDisplayMetrics().density); }
}

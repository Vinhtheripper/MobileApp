package com.example.mpos.settings;

import android.content.Intent;
import android.os.Bundle;
import android.widget.EditText;
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

public class SettingsActivity extends AppCompatActivity {

    private SettingsDao dao;

    private void updateAiKeyStatus(android.widget.TextView tv, android.content.SharedPreferences p) {
        if (tv == null) return;
        String key = p.getString("claude_api_key", "");
        if (key.isEmpty()) {
            tv.setText("Chưa cài đặt");
            tv.setTextColor(0xFF94A3B8);
        } else {
            tv.setText("Đã cài  •  gsk_..." + key.substring(Math.max(0, key.length() - 6)));
            tv.setTextColor(0xFF10B981);
        }
    }

    private void showAiKeyDialog(android.content.SharedPreferences aiPrefs, android.widget.TextView statusTv) {
        android.widget.EditText et = new android.widget.EditText(this);
        et.setHint("gsk_...");
        et.setTextSize(13f);
        et.setText(aiPrefs.getString("claude_api_key", ""));
        et.setPadding(dp(20), dp(12), dp(20), dp(12));

        new AlertDialog.Builder(this)
            .setTitle("Groq API Key")
            .setMessage("Lấy key miễn phí (không cần thẻ) tại:\nconsole.groq.com → API Keys → Create API key")
            .setView(et)
            .setPositiveButton("Lưu", (d, w) -> {
                String k = et.getText().toString().trim();
                aiPrefs.edit().putString("claude_api_key", k).apply();
                updateAiKeyStatus(statusTv, aiPrefs);
                Toast.makeText(this, k.isEmpty() ? "Đã xóa API key" : "Đã lưu API key", Toast.LENGTH_SHORT).show();
            })
            .setNegativeButton("Hủy", null)
            .show();
    }

    private int dp(int v) { return Math.round(v * getResources().getDisplayMetrics().density); }

    private void updatePinStatus(android.content.SharedPreferences pinPrefs) {
        boolean mgrChanged = pinPrefs.contains("pin_manager");
        boolean admChanged = pinPrefs.contains("pin_admin");
        android.widget.TextView tvMgr = findViewById(R.id.txtManagerPinStatus);
        android.widget.TextView tvAdm = findViewById(R.id.txtAdminPinStatus);
        if (tvMgr != null) tvMgr.setText(mgrChanged ? "Đã đặt mã PIN riêng" : "Mặc định: 1234");
        if (tvAdm != null) tvAdm.setText(admChanged ? "Đã đặt mã PIN riêng" : "Mặc định: 0000");
    }

    private void showChangePinDialog(android.content.SharedPreferences pinPrefs, String key,
                                     String defaultPin, String title) {
        android.widget.EditText etNew = new android.widget.EditText(this);
        etNew.setHint("Nhập mã PIN mới (số)");
        etNew.setInputType(android.text.InputType.TYPE_CLASS_NUMBER | android.text.InputType.TYPE_NUMBER_VARIATION_PASSWORD);
        etNew.setPadding(dp(20), dp(12), dp(20), dp(12));

        new AlertDialog.Builder(this)
            .setTitle(title)
            .setMessage("Mã PIN hiện tại: " + pinPrefs.getString(key, defaultPin))
            .setView(etNew)
            .setPositiveButton("Lưu", (d, w) -> {
                String newPin = etNew.getText().toString().trim();
                if (newPin.length() < 4) {
                    Toast.makeText(this, "Mã PIN phải có ít nhất 4 số", Toast.LENGTH_SHORT).show();
                    return;
                }
                pinPrefs.edit().putString(key, newPin).apply();
                updatePinStatus(pinPrefs);
                Toast.makeText(this, "Đã đổi mã PIN thành công", Toast.LENGTH_SHORT).show();
            })
            .setNegativeButton("Hủy", null)
            .show();
    }
    private EditText inputStoreName, inputVat, inputStoreAddress, inputStorePhone, inputTaxCode;
    private EditText inputBankCode, inputBankAccount, inputBankName;
    private EditText inputEarnRate, inputRedeemRate;
    private EditText inputReceiptHeader, inputReceiptFooter;

    @Override
    public void onCreate(Bundle state) {
        super.onCreate(state);
        setContentView(R.layout.activity_settings);

        dao = new SettingsDao(new DatabaseHelper(this));

        inputStoreName     = findViewById(R.id.inputStoreName);
        inputVat           = findViewById(R.id.inputVat);
        inputStoreAddress  = findViewById(R.id.inputStoreAddress);
        inputStorePhone    = findViewById(R.id.inputStorePhone);
        inputTaxCode       = findViewById(R.id.inputTaxCode);
        inputBankCode      = findViewById(R.id.inputBankCode);
        inputBankAccount   = findViewById(R.id.inputBankAccount);
        inputBankName      = findViewById(R.id.inputBankName);
        inputEarnRate      = findViewById(R.id.inputEarnRate);
        inputRedeemRate    = findViewById(R.id.inputRedeemRate);
        inputReceiptHeader = findViewById(R.id.inputReceiptHeader);
        inputReceiptFooter = findViewById(R.id.inputReceiptFooter);

        // Load saved values
        inputStoreName.setText(dao.get("store_name", "Cửa hàng của tôi"));
        inputVat.setText(dao.get("vat_percent", "0"));
        inputStoreAddress.setText(dao.get("store_address", ""));
        inputStorePhone.setText(dao.get("store_phone", ""));
        inputTaxCode.setText(dao.get("tax_code", ""));
        inputBankCode.setText(dao.get("bank_code", ""));
        inputBankAccount.setText(dao.get("bank_account", ""));
        inputBankName.setText(dao.get("bank_name", ""));
        inputEarnRate.setText(dao.get("loyalty_earn_rate", "1000"));
        inputRedeemRate.setText(dao.get("loyalty_redeem_rate", "100"));
        inputReceiptHeader.setText(dao.get("receipt_header", "Cảm ơn quý khách!"));
        inputReceiptFooter.setText(dao.get("receipt_footer", "Hẹn gặp lại!"));

        // Show logged-in user info
        SessionManager session = new SessionManager(this);
        User user = session.getUser();
        if (user != null) {
            TextView txtName    = findViewById(R.id.txtSettingsUserName);
            TextView txtInitial = findViewById(R.id.txtSettingsUserInitial);
            TextView txtRole    = findViewById(R.id.txtSettingsUserRole);
            if (txtName    != null) txtName.setText(user.username);
            if (txtInitial != null && user.username != null && !user.username.isEmpty())
                txtInitial.setText(String.valueOf(user.username.charAt(0)).toUpperCase());
            if (txtRole != null) {
                String label = "ADMIN".equals(user.role) ? "Quản trị viên"
                        : "MANAGER".equals(user.role) ? "Quản lý" : "Nhân viên";
                txtRole.setText(label);
            }
        }

        findViewById(R.id.btnBack).setOnClickListener(v -> finish());

        // Save store info
        findViewById(R.id.btnSaveSettings).setOnClickListener(v -> {
            String storeName = inputStoreName.getText().toString().trim();
            int vatValue;
            try { vatValue = Integer.parseInt(inputVat.getText().toString().trim()); }
            catch (Exception e) {
                Toast.makeText(this, "VAT phải là số từ 0-100", Toast.LENGTH_SHORT).show(); return;
            }
            if (storeName.isEmpty() || vatValue < 0 || vatValue > 100) {
                Toast.makeText(this, "Kiểm tra lại tên cửa hàng và VAT", Toast.LENGTH_SHORT).show(); return;
            }
            dao.put("store_name",    storeName);
            dao.put("vat_percent",   String.valueOf(vatValue));
            dao.put("store_address", inputStoreAddress.getText().toString().trim());
            dao.put("store_phone",   inputStorePhone.getText().toString().trim());
            dao.put("tax_code",      inputTaxCode.getText().toString().trim());
            Toast.makeText(this, "Đã lưu thông tin cửa hàng", Toast.LENGTH_SHORT).show();
        });

        // Save bank / VietQR
        findViewById(R.id.btnSaveBank).setOnClickListener(v -> {
            String bankCode = inputBankCode.getText().toString().trim().toUpperCase();
            String account  = inputBankAccount.getText().toString().trim();
            String name     = inputBankName.getText().toString().trim().toUpperCase();
            if (bankCode.isEmpty() || account.isEmpty() || name.isEmpty()) {
                Toast.makeText(this, "Nhập đầy đủ mã ngân hàng, số TK và tên chủ TK", Toast.LENGTH_SHORT).show(); return;
            }
            dao.put("bank_code",    bankCode);
            dao.put("bank_account", account);
            dao.put("bank_name",    name);
            Toast.makeText(this, "Đã lưu tài khoản VietQR", Toast.LENGTH_SHORT).show();
        });

        // Save loyalty
        findViewById(R.id.btnSaveLoyalty).setOnClickListener(v -> {
            String earn   = inputEarnRate.getText().toString().trim();
            String redeem = inputRedeemRate.getText().toString().trim();
            if (earn.isEmpty() || redeem.isEmpty()) {
                Toast.makeText(this, "Nhập đầy đủ tỉ lệ tích/quy đổi điểm", Toast.LENGTH_SHORT).show(); return;
            }
            dao.put("loyalty_earn_rate",   earn);
            dao.put("loyalty_redeem_rate", redeem);
            Toast.makeText(this, "Đã lưu cài đặt tích điểm", Toast.LENGTH_SHORT).show();
        });

        // Save receipt template
        findViewById(R.id.btnSaveReceipt).setOnClickListener(v -> {
            dao.put("receipt_header", inputReceiptHeader.getText().toString().trim());
            dao.put("receipt_footer", inputReceiptFooter.getText().toString().trim());
            Toast.makeText(this, "Đã lưu mẫu hóa đơn", Toast.LENGTH_SHORT).show();
        });

        // AI Chat API key
        android.content.SharedPreferences aiPrefs = getSharedPreferences("mpos_ai_prefs", android.content.Context.MODE_PRIVATE);
        android.widget.TextView txtAiKeyStatus = findViewById(R.id.txtAiKeyStatus);
        updateAiKeyStatus(txtAiKeyStatus, aiPrefs);
        findViewById(R.id.btnSetAiKey).setOnClickListener(v -> showAiKeyDialog(aiPrefs, txtAiKeyStatus));

        // PIN management (Admin only)
        android.content.SharedPreferences pinPrefs = getSharedPreferences("mpos_pin_prefs", android.content.Context.MODE_PRIVATE);
        android.view.View sectionPin = findViewById(R.id.sectionPinManagement);
        String currentRole = user != null ? user.role : "STAFF";
        if (sectionPin != null && "ADMIN".equals(currentRole)) {
            sectionPin.setVisibility(android.view.View.VISIBLE);
            updatePinStatus(pinPrefs);
            android.view.View btnMgrPin = findViewById(R.id.btnChangeManagerPin);
            if (btnMgrPin != null) btnMgrPin.setOnClickListener(v ->
                showChangePinDialog(pinPrefs, "pin_manager", "1234", "Đổi mã PIN Quản lý"));
            android.view.View btnAdmPin = findViewById(R.id.btnChangeAdminPin);
            if (btnAdmPin != null) btnAdmPin.setOnClickListener(v ->
                showChangePinDialog(pinPrefs, "pin_admin", "0000", "Đổi mã PIN Admin"));
        }

        // Logout
        findViewById(R.id.btnLogout).setOnClickListener(v ->
            new AlertDialog.Builder(this)
                .setTitle("Đăng xuất")
                .setMessage("Bạn có chắc muốn đăng xuất không?")
                .setPositiveButton("Đăng xuất", (d, w) -> {
                    new SessionManager(this).clear();
                    Intent intent = new Intent(this, com.example.mpos.WelcomeActivity.class);
                    intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                    startActivity(intent);
                    finish();
                })
                .setNegativeButton("Hủy", null)
                .show()
        );
    }
}

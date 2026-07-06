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

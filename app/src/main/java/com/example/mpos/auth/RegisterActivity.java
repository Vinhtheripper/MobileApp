package com.example.mpos.auth;

import android.content.Intent;
import android.os.Bundle;
import android.text.method.HideReturnsTransformationMethod;
import android.text.method.PasswordTransformationMethod;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.example.mpos.R;
import com.example.mpos.dao.UserDao;
import com.example.mpos.database.DatabaseHelper;
import com.example.mpos.model.User;
import com.example.mpos.shop.CreateShopActivity;

public class RegisterActivity extends AppCompatActivity {

    private EditText inputFullName, inputEmail, inputPhone, inputPassword, inputConfirmPassword;
    private TextView txtError;
    private FrameLayout checkTermsContainer;
    private TextView checkTermsMark;

    private boolean passwordVisible = false;
    private boolean confirmPasswordVisible = false;
    private boolean termsAccepted = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register);

        inputFullName        = findViewById(R.id.inputFullName);
        inputEmail           = findViewById(R.id.inputEmail);
        inputPhone           = findViewById(R.id.inputPhone);
        inputPassword        = findViewById(R.id.inputPassword);
        inputConfirmPassword = findViewById(R.id.inputConfirmPassword);
        txtError             = findViewById(R.id.txtRegisterError);
        checkTermsContainer  = findViewById(R.id.checkTermsContainer);
        checkTermsMark       = findViewById(R.id.checkTermsMark);

        Button btnBack = findViewById(R.id.btnBack);
        btnBack.setOnClickListener(v -> finish());

        TextView btnTogglePassword = findViewById(R.id.btnTogglePassword);
        btnTogglePassword.setOnClickListener(v -> {
            passwordVisible = !passwordVisible;
            inputPassword.setTransformationMethod(passwordVisible
                ? HideReturnsTransformationMethod.getInstance()
                : PasswordTransformationMethod.getInstance());
            btnTogglePassword.setText(passwordVisible ? "🙈" : "👁");
            inputPassword.setSelection(inputPassword.getText().length());
        });

        TextView btnToggleConfirmPassword = findViewById(R.id.btnToggleConfirmPassword);
        btnToggleConfirmPassword.setOnClickListener(v -> {
            confirmPasswordVisible = !confirmPasswordVisible;
            inputConfirmPassword.setTransformationMethod(confirmPasswordVisible
                ? HideReturnsTransformationMethod.getInstance()
                : PasswordTransformationMethod.getInstance());
            btnToggleConfirmPassword.setText(confirmPasswordVisible ? "🙈" : "👁");
            inputConfirmPassword.setSelection(inputConfirmPassword.getText().length());
        });

        checkTermsContainer.setOnClickListener(v -> toggleTerms());

        TextView btnTermsLink = findViewById(R.id.btnTermsLink);
        if (btnTermsLink != null) btnTermsLink.setOnClickListener(v ->
            showError("Điều khoản sử dụng sẽ sớm được cập nhật"));

        TextView btnPrivacyLink = findViewById(R.id.btnPrivacyLink);
        if (btnPrivacyLink != null) btnPrivacyLink.setOnClickListener(v ->
            showError("Chính sách bảo mật sẽ sớm được cập nhật"));

        Button btnRegister = findViewById(R.id.btnRegister);
        btnRegister.setOnClickListener(v -> doRegister());

        TextView btnGoToLogin = findViewById(R.id.btnGoToLogin);
        if (btnGoToLogin != null) btnGoToLogin.setOnClickListener(v -> finish());
    }

    private void toggleTerms() {
        termsAccepted = !termsAccepted;
        checkTermsContainer.setBackground(getDrawable(termsAccepted
            ? R.drawable.bg_checkbox_checked
            : R.drawable.bg_checkbox_unchecked));
        checkTermsMark.setVisibility(termsAccepted ? View.VISIBLE : View.GONE);
    }

    private void doRegister() {
        String fullName = inputFullName.getText().toString().trim();
        String email    = inputEmail.getText().toString().trim();
        String phone    = inputPhone.getText().toString().trim();
        String password = inputPassword.getText().toString();
        String confirm  = inputConfirmPassword.getText().toString();

        if (fullName.isEmpty()) { showError("Vui lòng nhập họ và tên"); return; }
        if (email.isEmpty() || !android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            showError("Email không hợp lệ"); return; }
        if (phone.isEmpty()) { showError("Vui lòng nhập số điện thoại"); return; }
        if (password.length() < 6) { showError("Mật khẩu phải có ít nhất 6 ký tự"); return; }
        if (!password.equals(confirm)) { showError("Mật khẩu xác nhận không khớp"); return; }
        if (!termsAccepted) { showError("Vui lòng đồng ý với điều khoản sử dụng"); return; }

        try {
            UserDao dao = new UserDao(new DatabaseHelper(this));
            if (dao.isEmailTaken(email)) { showError("Email này đã được sử dụng"); return; }

            // Tất cả tự đăng ký đều là ADMIN (chủ tài khoản)
            // Role trong từng shop được gán qua shop_members khi owner thêm nhân viên
            boolean ok = dao.register(email, fullName, phone, password, "ADMIN");
            if (!ok) { showError("Đăng ký thất bại, vui lòng thử lại"); return; }

            User user = dao.login(email, password);
            if (user != null) {
                new SessionManager(this).save(user);
                startActivity(new Intent(this, CreateShopActivity.class)
                    .addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK | Intent.FLAG_ACTIVITY_NEW_TASK));
            } else {
                finish();
            }
        } catch (Exception e) {
            showError("Lỗi: " + e.getMessage());
        }
    }

    private void showError(String msg) {
        txtError.setText(msg);
        txtError.setVisibility(View.VISIBLE);
    }
}

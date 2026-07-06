package com.example.mpos.auth;

import android.content.Intent;
import android.os.Bundle;
import android.text.method.HideReturnsTransformationMethod;
import android.text.method.PasswordTransformationMethod;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.activity.result.ActivityResult;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;

import com.example.mpos.R;
import com.example.mpos.dao.ShopDao;
import com.example.mpos.dao.UserDao;
import com.example.mpos.database.DatabaseHelper;
import com.example.mpos.model.Shop;
import com.example.mpos.model.User;
import com.example.mpos.shop.CreateShopActivity;
import com.example.mpos.shop.ShopListActivity;
import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.common.api.ApiException;
import com.google.android.gms.tasks.Task;

import java.util.List;

public class LoginActivity extends AppCompatActivity {

    private EditText inputEmail;
    private EditText inputPassword;
    private TextView txtError;
    private ImageView btnTogglePassword;
    private boolean passwordVisible = false;

    private GoogleSignInClient googleSignInClient;

    private final ActivityResultLauncher<Intent> googleSignInLauncher =
        registerForActivityResult(new ActivityResultContracts.StartActivityForResult(),
            result -> handleGoogleSignInResult(result));

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        inputEmail    = findViewById(R.id.inputEmail);
        inputPassword = findViewById(R.id.inputPassword);
        txtError      = findViewById(R.id.txtLoginError);

        View btnLogin = findViewById(R.id.btnLogin);
        btnLogin.setOnClickListener(v -> doLogin());

        btnTogglePassword = findViewById(R.id.btnTogglePassword);
        if (btnTogglePassword != null) btnTogglePassword.setOnClickListener(v -> {
            passwordVisible = !passwordVisible;
            if (passwordVisible) {
                inputPassword.setTransformationMethod(HideReturnsTransformationMethod.getInstance());
                btnTogglePassword.setImageResource(R.drawable.ic_eye_on);
            } else {
                inputPassword.setTransformationMethod(PasswordTransformationMethod.getInstance());
                btnTogglePassword.setImageResource(R.drawable.ic_eye_off);
            }
            inputPassword.setSelection(inputPassword.getText().length());
        });

        TextView btnForgotPassword = findViewById(R.id.btnForgotPassword);
        if (btnForgotPassword != null) btnForgotPassword.setOnClickListener(v ->
            showError("Tính năng đặt lại mật khẩu sẽ sớm được cập nhật")
        );

        // Google Sign-In setup
        GoogleSignInOptions gso = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken(getString(R.string.google_web_client_id))
            .requestEmail()
            .requestProfile()
            .build();
        googleSignInClient = GoogleSignIn.getClient(this, gso);

        LinearLayout btnGoogleSignIn = findViewById(R.id.btnGoogleSignIn);
        if (btnGoogleSignIn != null) btnGoogleSignIn.setOnClickListener(v -> {
            googleSignInClient.signOut().addOnCompleteListener(task ->
                googleSignInLauncher.launch(googleSignInClient.getSignInIntent()));
        });

        LinearLayout btnFacebookSignIn = findViewById(R.id.btnFacebookSignIn);
        if (btnFacebookSignIn != null) btnFacebookSignIn.setOnClickListener(v ->
            new android.app.AlertDialog.Builder(this)
                .setTitle("Đăng nhập với Facebook")
                .setMessage("Tính năng đang được phát triển.\n• Tạo ứng dụng trên Facebook Developer\n• Thêm App ID vào strings.xml")
                .setPositiveButton("Đã hiểu", null).show()
        );

        TextView btnRegister = findViewById(R.id.btnRegister);
        if (btnRegister != null) btnRegister.setOnClickListener(v ->
            startActivity(new Intent(this, RegisterActivity.class))
        );
    }

    private void handleGoogleSignInResult(ActivityResult result) {
        Task<GoogleSignInAccount> task = GoogleSignIn.getSignedInAccountFromIntent(result.getData());
        try {
            GoogleSignInAccount account = task.getResult(ApiException.class);
            String email       = account.getEmail();
            String displayName = account.getDisplayName();
            if (email == null || email.isEmpty()) {
                showError("Không lấy được email từ tài khoản Google");
                return;
            }
            loginOrRegisterWithGoogle(email, displayName);
        } catch (ApiException e) {
            int code = e.getStatusCode();
            if (code == 12501) return; // User cancelled — do nothing
            showError("Google Sign-In thất bại (mã: " + code + ")");
        }
    }

    private void loginOrRegisterWithGoogle(String email, String displayName) {
        try {
            DatabaseHelper db = new DatabaseHelper(this);
            UserDao userDao = new UserDao(db);
            User user = userDao.findByEmail(email);
            if (user == null) {
                user = userDao.registerWithGoogle(email, displayName);
            }
            if (user == null) {
                showError("Không thể tạo tài khoản từ Google");
                return;
            }
            navigateAfterLogin(user);
        } catch (Exception e) {
            showError("Lỗi: " + e.getMessage());
        }
    }

    private void doLogin() {
        String email = inputEmail.getText().toString().trim();
        String pass  = inputPassword.getText().toString();

        if (email.isEmpty() || pass.isEmpty()) {
            showError("Vui lòng nhập đầy đủ thông tin");
            return;
        }

        try {
            User user = new UserDao(new DatabaseHelper(this)).login(email, pass);
            if (user == null) {
                showError("Email hoặc mật khẩu không đúng");
                return;
            }
            txtError.setVisibility(View.GONE);
            navigateAfterLogin(user);
        } catch (Exception e) {
            showError("Lỗi: " + e.getMessage());
        }
    }

    private void navigateAfterLogin(User user) {
        SessionManager session = new SessionManager(this);
        session.save(user);

        ShopDao shopDao = new ShopDao(new DatabaseHelper(this));
        List<Shop> shops = shopDao.getShopsForUser(user.id);
        Intent next = shops.isEmpty()
            ? new Intent(this, CreateShopActivity.class)
            : new Intent(this, ShopListActivity.class);
        startActivity(next);
        finish();
    }

    private void showError(String msg) {
        txtError.setText(msg);
        txtError.setVisibility(View.VISIBLE);
    }
}
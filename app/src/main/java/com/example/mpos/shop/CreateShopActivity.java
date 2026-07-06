package com.example.mpos.shop;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;

import com.example.mpos.MainActivity;
import com.example.mpos.R;
import com.example.mpos.auth.SessionManager;
import com.example.mpos.dao.ShopDao;
import com.example.mpos.database.DatabaseHelper;

public class CreateShopActivity extends AppCompatActivity {

    private EditText inputName, inputAddress, inputPhone;
    private TextView txtError;
    private ImageView imgShopLogo;
    private LinearLayout layoutLogoPlaceholder;
    private String selectedLogoUri = null;

    private final ActivityResultLauncher<String> pickImageLauncher =
        registerForActivityResult(new ActivityResultContracts.GetContent(), uri -> {
            if (uri != null) {
                getContentResolver().takePersistableUriPermission(
                    uri, Intent.FLAG_GRANT_READ_URI_PERMISSION);
                selectedLogoUri = uri.toString();
                imgShopLogo.setImageURI(uri);
                imgShopLogo.setVisibility(View.VISIBLE);
                layoutLogoPlaceholder.setVisibility(View.GONE);
            }
        });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_create_shop);

        inputName    = findViewById(R.id.inputShopName);
        inputAddress = findViewById(R.id.inputShopAddress);
        inputPhone   = findViewById(R.id.inputShopPhone);
        txtError     = findViewById(R.id.txtCreateShopError);
        imgShopLogo  = findViewById(R.id.imgShopLogo);
        layoutLogoPlaceholder = findViewById(R.id.layoutLogoPlaceholder);

        FrameLayout btnPickLogo = findViewById(R.id.btnPickLogo);
        btnPickLogo.setOnClickListener(v -> pickImageLauncher.launch("image/*"));

        Button btnCreate = findViewById(R.id.btnCreateShop);
        btnCreate.setOnClickListener(v -> doCreate());
    }

    @Override public void onBackPressed() { }

    private void doCreate() {
        String name    = inputName.getText().toString().trim();
        String address = inputAddress.getText().toString().trim();
        String phone   = inputPhone.getText().toString().trim();

        if (name.isEmpty()) {
            showError("Vui lòng nhập tên cửa hàng");
            return;
        }

        try {
            SessionManager session = new SessionManager(this);
            long userId = session.getUser().id;
            ShopDao shopDao = new ShopDao(new DatabaseHelper(this));
            long shopId = shopDao.create(name, address, phone, selectedLogoUri, userId);
            if (shopId <= 0) { showError("Tạo cửa hàng thất bại"); return; }

            String memberRole = shopDao.getMemberRole(shopId, userId);
            session.saveShop(shopId, name, memberRole != null ? memberRole : "OWNER");

            startActivity(new Intent(this, MainActivity.class)
                .addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK | Intent.FLAG_ACTIVITY_NEW_TASK));
        } catch (Exception e) {
            showError("Lỗi: " + e.getMessage());
        }
    }

    private void showError(String msg) {
        txtError.setText(msg);
        txtError.setVisibility(View.VISIBLE);
    }
}

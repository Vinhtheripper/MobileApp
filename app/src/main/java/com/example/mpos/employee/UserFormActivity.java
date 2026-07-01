package com.example.mpos.employee;

import android.os.Bundle;
import android.widget.ArrayAdapter;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.mpos.R;
import com.example.mpos.auth.PermissionHelper;
import com.example.mpos.auth.SessionManager;
import com.example.mpos.constants.RoleConstants;
import com.example.mpos.dao.UserDao;
import com.example.mpos.database.DatabaseHelper;
import com.example.mpos.model.User;

public class UserFormActivity extends AppCompatActivity {
    private UserDao dao;
    private User user;
    private EditText fullName, username, password, phone, email, position;
    private Spinner role;
    private CheckBox active;

    @Override public void onCreate(Bundle state) {
        super.onCreate(state);
        if (!PermissionHelper.requireAdmin(this)) return;
        setContentView(R.layout.activity_user_form);
        dao = new UserDao(new DatabaseHelper(this));
        fullName = findViewById(R.id.inputFullName);
        username = findViewById(R.id.inputUsername);
        password = findViewById(R.id.inputPassword);
        phone = findViewById(R.id.inputPhone);
        email = findViewById(R.id.inputEmail);
        position = findViewById(R.id.inputPosition);
        role = findViewById(R.id.spinnerRole);
        active = findViewById(R.id.checkActive);
        role.setAdapter(new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item, new String[]{RoleConstants.STAFF, RoleConstants.ADMIN}));
        long id = getIntent().getLongExtra("user_id", -1);
        user = id > 0 ? dao.findById(id) : new User();
        if (user == null) user = new User();
        bind();
        findViewById(R.id.btnBack).setOnClickListener(v -> finish());
        findViewById(R.id.btnSaveUser).setOnClickListener(v -> save());
        findViewById(R.id.btnDeleteUser).setOnClickListener(v -> deactivate());
    }

    private void bind() {
        fullName.setText(user.fullName);
        username.setText(user.username);
        phone.setText(user.phone);
        email.setText(user.email);
        position.setText(user.position);
        active.setChecked(user.id <= 0 || user.active);
        String current = user.role == null ? RoleConstants.STAFF : user.role;
        for (int i = 0; i < role.getCount(); i++) if (current.equals(role.getItemAtPosition(i))) role.setSelection(i);
        findViewById(R.id.btnDeleteUser).setEnabled(user.id > 0);
    }

    private void save() {
        user.fullName = fullName.getText().toString();
        user.username = username.getText().toString();
        user.phone = phone.getText().toString();
        user.email = email.getText().toString();
        user.position = position.getText().toString();
        user.role = role.getSelectedItem().toString();
        user.active = active.isChecked();
        try {
            dao.saveAccount(user, password.getText().toString());
            Toast.makeText(this, "Đã lưu tài khoản", Toast.LENGTH_SHORT).show();
            finish();
        } catch (Exception e) {
            Toast.makeText(this, e.getMessage() == null ? "Không lưu được tài khoản" : e.getMessage(), Toast.LENGTH_LONG).show();
        }
    }

    private void deactivate() {
        if (user.id <= 0) return;
        if (user.id == new SessionManager(this).getUser().id) {
            Toast.makeText(this, "Không thể khóa tài khoản đang đăng nhập", Toast.LENGTH_LONG).show();
            return;
        }
        dao.setActive(user.id, false);
        Toast.makeText(this, "Đã khóa tài khoản", Toast.LENGTH_SHORT).show();
        finish();
    }
}

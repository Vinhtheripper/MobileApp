package com.example.mpos.employee;

import android.content.ContentValues;
import android.database.Cursor;
import android.os.Bundle;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.example.mpos.R;
import com.example.mpos.database.DatabaseHelper;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class EmployeeDetailActivity extends AppCompatActivity {

    private DatabaseHelper db;
    private long userId;
    private long employeeId;
    private boolean isActive;
    private String currentRole;

    private EditText etFullName, etPosition, etPhone;
    private TextView txtEmpEmail, txtEmpName, txtEmpRole, txtEmpStatus;
    private TextView txtInitials, txtEmpCode, txtCreatedAt;
    private TextView txtSelectedRole, txtToggleLabel, txtStatusDesc;
    private LinearLayout btnRoleStaff, btnRoleManager, btnRoleAdmin, btnToggleStatus;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_employee_detail);

        db = new DatabaseHelper(this);
        userId = getIntent().getLongExtra("user_id", -1);

        etFullName      = findViewById(R.id.etFullName);
        etPosition      = findViewById(R.id.etPosition);
        etPhone         = findViewById(R.id.etPhone);
        txtEmpEmail     = findViewById(R.id.txtEmpEmail);
        txtEmpName      = findViewById(R.id.txtEmpName);
        txtEmpRole      = findViewById(R.id.txtEmpRole);
        txtEmpStatus    = findViewById(R.id.txtEmpStatus);
        txtInitials     = findViewById(R.id.txtInitials);
        txtEmpCode      = findViewById(R.id.txtEmpCode);
        txtCreatedAt    = findViewById(R.id.txtCreatedAt);
        txtSelectedRole = findViewById(R.id.txtSelectedRole);
        txtToggleLabel  = findViewById(R.id.txtToggleLabel);
        txtStatusDesc   = findViewById(R.id.txtStatusDesc);
        btnRoleStaff    = findViewById(R.id.btnRoleStaff);
        btnRoleManager  = findViewById(R.id.btnRoleManager);
        btnRoleAdmin    = findViewById(R.id.btnRoleAdmin);
        btnToggleStatus = findViewById(R.id.btnToggleStatus);

        findViewById(R.id.btnBack).setOnClickListener(v -> finish());
        findViewById(R.id.btnSave).setOnClickListener(v -> saveChanges());

        btnRoleStaff.setOnClickListener(v   -> selectRole("STAFF"));
        btnRoleManager.setOnClickListener(v -> selectRole("MANAGER"));
        btnRoleAdmin.setOnClickListener(v   -> selectRole("ADMIN"));
        btnToggleStatus.setOnClickListener(v -> toggleStatus());

        if (userId >= 0) loadEmployee();
    }

    private void loadEmployee() {
        Cursor c = db.getReadableDatabase().rawQuery(
            "SELECT u.id, u.username, u.role, u.is_active, u.created_at, " +
            "e.id, e.employee_code, e.full_name, e.phone, e.email, e.position " +
            "FROM users u LEFT JOIN employees e ON u.employee_id=e.id " +
            "WHERE u.id=?",
            new String[]{String.valueOf(userId)});
        try {
            if (!c.moveToFirst()) return;
            String username   = c.getString(1);
            currentRole       = c.getString(2);
            isActive          = c.getInt(3) == 1;
            long createdAt    = c.getLong(4);
            employeeId        = c.getLong(5);
            String empCode    = c.getString(6);
            String fullName   = c.getString(7);
            String phone      = c.getString(8);
            String email      = c.getString(9);
            String position   = c.getString(10);

            String displayName = fullName != null ? fullName : username;

            // Banner
            txtEmpName.setText(displayName);
            txtEmpRole.setText(roleLabel(currentRole));
            txtEmpStatus.setText(isActive ? "Đang hoạt động" : "Đã vô hiệu hoá");
            txtEmpStatus.setTextColor(isActive ? 0xFFA5F3C4 : 0xFFFF8A80);
            if (displayName.length() >= 2)
                txtInitials.setText(displayName.substring(0, 2).toUpperCase());

            // Fields
            if (fullName != null)  etFullName.setText(fullName);
            if (position != null)  etPosition.setText(position);
            if (phone    != null)  etPhone.setText(phone);
            txtEmpEmail.setText(username);
            txtEmpCode.setText(empCode != null ? empCode : "—");
            txtCreatedAt.setText(new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
                .format(new Date(createdAt)));

            selectRole(currentRole);
            updateStatusUI();
        } finally { c.close(); }
    }

    private void selectRole(String role) {
        currentRole = role;
        float SELECTED = 1f, DIM = 0.45f;
        btnRoleStaff.setAlpha("STAFF".equals(role)     ? SELECTED : DIM);
        btnRoleManager.setAlpha("MANAGER".equals(role) ? SELECTED : DIM);
        btnRoleAdmin.setAlpha("ADMIN".equals(role)     ? SELECTED : DIM);
        txtSelectedRole.setText("Quyền hiện tại: " + roleLabel(role));
        txtEmpRole.setText(roleLabel(role));
    }

    private void toggleStatus() {
        String msg = isActive
            ? "Vô hiệu hoá nhân viên này sẽ ngăn họ đăng nhập. Tiếp tục?"
            : "Kích hoạt lại tài khoản nhân viên này?";
        new AlertDialog.Builder(this)
            .setTitle(isActive ? "Vô hiệu hoá tài khoản" : "Kích hoạt tài khoản")
            .setMessage(msg)
            .setPositiveButton("Đồng ý", (d, w) -> {
                isActive = !isActive;
                ContentValues cv = new ContentValues();
                cv.put("is_active", isActive ? 1 : 0);
                cv.put("updated_at", System.currentTimeMillis());
                db.getWritableDatabase().update("users", cv, "id=?",
                    new String[]{String.valueOf(userId)});
                updateStatusUI();
                Toast.makeText(this,
                    isActive ? "Đã kích hoạt tài khoản" : "Đã vô hiệu hoá tài khoản",
                    Toast.LENGTH_SHORT).show();
            })
            .setNegativeButton("Hủy", null)
            .show();
    }

    private void updateStatusUI() {
        if (isActive) {
            txtEmpStatus.setText("Đang hoạt động");
            txtEmpStatus.setTextColor(0xFFA5F3C4);
            txtStatusDesc.setText("Nhân viên đang được phép đăng nhập vào hệ thống.");
            txtToggleLabel.setText("Vô hiệu hoá");
        } else {
            txtEmpStatus.setText("Đã vô hiệu hoá");
            txtEmpStatus.setTextColor(0xFFFF8A80);
            txtStatusDesc.setText("Tài khoản bị khoá, nhân viên không thể đăng nhập.");
            txtToggleLabel.setText("Kích hoạt lại");
        }
    }

    private void saveChanges() {
        String name     = etFullName.getText().toString().trim();
        String position = etPosition.getText().toString().trim();
        String phone    = etPhone.getText().toString().trim();

        if (name.isEmpty()) {
            Toast.makeText(this, "Vui lòng nhập họ và tên", Toast.LENGTH_SHORT).show();
            return;
        }

        long now = System.currentTimeMillis();

        // Update employees table
        if (employeeId > 0) {
            ContentValues empCv = new ContentValues();
            empCv.put("full_name", name);
            empCv.put("position", position);
            empCv.put("phone", phone);
            empCv.put("updated_at", now);
            db.getWritableDatabase().update("employees", empCv, "id=?",
                new String[]{String.valueOf(employeeId)});
        }

        // Update users role
        ContentValues userCv = new ContentValues();
        userCv.put("role", currentRole);
        userCv.put("updated_at", now);
        db.getWritableDatabase().update("users", userCv, "id=?",
            new String[]{String.valueOf(userId)});

        // Refresh banner
        txtEmpName.setText(name);
        if (name.length() >= 2)
            txtInitials.setText(name.substring(0, 2).toUpperCase());
        txtEmpRole.setText(roleLabel(currentRole));

        Toast.makeText(this, "Đã lưu thông tin nhân viên", Toast.LENGTH_SHORT).show();
        setResult(RESULT_OK);
    }

    private String roleLabel(String role) {
        if (role == null) return "Nhân viên";
        switch (role) {
            case "ADMIN":   return "Admin";
            case "MANAGER": return "Quản lý";
            default:        return "Nhân viên";
        }
    }
}

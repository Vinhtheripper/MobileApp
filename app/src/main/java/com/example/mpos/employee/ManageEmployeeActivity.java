package com.example.mpos.employee;

import android.content.Intent;
import android.database.Cursor;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TextView;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;

import com.example.mpos.R;
import com.example.mpos.auth.RegisterActivity;
import com.example.mpos.database.DatabaseHelper;
import com.example.mpos.employee.EmployeeKpiActivity;

import java.util.ArrayList;
import java.util.List;

public class ManageEmployeeActivity extends AppCompatActivity {

    static class EmpRow {
        long id;
        String name, email, role;
        boolean isActive;
    }

    private DatabaseHelper db;
    private EmpAdapter adapter;
    private EditText search;
    private TextView txtCount, txtStatAdmin, txtStatManager, txtStatStaff;

    private final ActivityResultLauncher<Intent> detailLauncher =
        registerForActivityResult(new ActivityResultContracts.StartActivityForResult(),
            result -> load());

    @Override
    public void onCreate(Bundle state) {
        super.onCreate(state);
        setContentView(R.layout.activity_manage_employee);

        db = new DatabaseHelper(this);
        search       = findViewById(R.id.inputEmployeeSearch);
        txtCount     = findViewById(R.id.txtEmployeeCount);
        txtStatAdmin   = findViewById(R.id.txtStatAdmin);
        txtStatManager = findViewById(R.id.txtStatManager);
        txtStatStaff   = findViewById(R.id.txtStatStaff);

        ListView list = findViewById(R.id.listEmployees);
        adapter = new EmpAdapter();
        list.setAdapter(adapter);
        list.setOnItemClickListener((parent, v, pos, id) -> {
            EmpRow row = adapter.getItem(pos);
            if (row == null) return;
            Intent i = new Intent(this, EmployeeDetailActivity.class);
            i.putExtra("user_id", row.id);
            detailLauncher.launch(i);
        });

        search.addTextChangedListener(new TextWatcher() {
            public void afterTextChanged(Editable s) { load(); }
            public void beforeTextChanged(CharSequence s, int a, int b, int c) {}
            public void onTextChanged(CharSequence s, int a, int b, int c) {}
        });

        findViewById(R.id.btnBack).setOnClickListener(v -> finish());
        findViewById(R.id.btnSearchEmployee).setOnClickListener(v -> load());
        findViewById(R.id.btnAddEmployee).setOnClickListener(v ->
            startActivity(new Intent(this, RegisterActivity.class)));
        View btnKpi = findViewById(R.id.btnEmployeeKpi);
        if (btnKpi != null)
            btnKpi.setOnClickListener(v ->
                startActivity(new Intent(this, EmployeeKpiActivity.class)));

        load();
    }

    @Override
    protected void onResume() {
        super.onResume();
        load();
    }

    private void load() {
        String q = "%" + search.getText().toString().trim() + "%";
        Cursor c = db.getReadableDatabase().rawQuery(
            "SELECT u.id, e.full_name, u.username, u.role, u.is_active " +
            "FROM users u LEFT JOIN employees e ON u.employee_id=e.id " +
            "WHERE (e.full_name LIKE ? OR u.username LIKE ?) " +
            "ORDER BY u.role, e.full_name",
            new String[]{q, q});

        List<EmpRow> rows = new ArrayList<>();
        int admins = 0, managers = 0, staff = 0;
        try {
            while (c.moveToNext()) {
                EmpRow r = new EmpRow();
                r.id       = c.getLong(0);
                r.name     = c.getString(1);
                r.email    = c.getString(2);
                r.role     = c.getString(3);
                r.isActive = c.getInt(4) == 1;
                rows.add(r);
                if ("ADMIN".equals(r.role))        admins++;
                else if ("MANAGER".equals(r.role)) managers++;
                else                               staff++;
            }
        } finally { c.close(); }

        adapter.setRows(rows);

        int active = 0;
        for (EmpRow r : rows) if (r.isActive) active++;
        txtCount.setText(rows.size() + " nhân viên  •  " + active + " đang hoạt động");
        txtStatAdmin.setText(admins + " Admin");
        txtStatManager.setText(managers + " Quản lý");
        txtStatStaff.setText(staff + " Nhân viên");
    }

    class EmpAdapter extends BaseAdapter {
        private List<EmpRow> rows = new ArrayList<>();
        void setRows(List<EmpRow> r) { rows = r; notifyDataSetChanged(); }
        @Override public int getCount()        { return rows.isEmpty() ? 1 : rows.size(); }
        @Override public EmpRow getItem(int p) { return rows.isEmpty() ? null : rows.get(p); }
        @Override public long getItemId(int p) { return p; }

        @Override
        public View getView(int pos, View convertView, ViewGroup parent) {
            if (rows.isEmpty()) {
                TextView tv = new TextView(ManageEmployeeActivity.this);
                tv.setText("Chưa có nhân viên nào");
                tv.setGravity(Gravity.CENTER);
                tv.setPadding(0, 80, 0, 80);
                tv.setTextColor(getResources().getColor(R.color.text_secondary));
                return tv;
            }
            if (convertView == null || convertView instanceof TextView)
                convertView = LayoutInflater.from(ManageEmployeeActivity.this)
                    .inflate(R.layout.item_employee, parent, false);

            EmpRow row = rows.get(pos);

            TextView nameView     = convertView.findViewById(R.id.txtEmployeeName);
            TextView emailView    = convertView.findViewById(R.id.txtEmployeeEmail);
            TextView roleView     = convertView.findViewById(R.id.txtEmployeeRole);
            TextView initialsView = convertView.findViewById(R.id.txtEmployeeInitials);
            View avatarBg         = convertView.findViewById(R.id.avatarCircle);

            String displayName = row.name != null ? row.name : row.email;
            if (nameView  != null) {
                nameView.setText(displayName);
                nameView.setAlpha(row.isActive ? 1f : 0.5f);
            }
            if (emailView != null) {
                String suffix = row.isActive ? "" : "  (vô hiệu hoá)";
                emailView.setText(row.email + suffix);
                emailView.setAlpha(row.isActive ? 1f : 0.5f);
            }
            if (roleView != null) {
                roleView.setText(roleLabel(row.role));
                roleView.setBackgroundResource(roleBadgeBg(row.role));
                roleView.setTextColor(roleBadgeColor(row.role));
            }
            if (initialsView != null && displayName != null && displayName.length() >= 2)
                initialsView.setText(displayName.substring(0, 2).toUpperCase());
            if (avatarBg != null)
                avatarBg.setBackgroundResource(avatarBgRes(row.role));

            return convertView;
        }

        private String roleLabel(String role) {
            if (role == null) return "Nhân viên";
            switch (role) {
                case "ADMIN":   return "Admin";
                case "MANAGER": return "Quản lý";
                default:        return "Nhân viên";
            }
        }

        private int roleBadgeBg(String role) {
            if ("ADMIN".equals(role))   return R.drawable.badge_error;
            if ("MANAGER".equals(role)) return R.drawable.badge_warning;
            return R.drawable.bg_blue_summary;
        }

        private int roleBadgeColor(String role) {
            if ("ADMIN".equals(role))   return 0xFF991B1B;
            if ("MANAGER".equals(role)) return 0xFF92400E;
            return 0xFF1558B0;
        }

        private int avatarBgRes(String role) {
            if ("ADMIN".equals(role))   return R.drawable.bg_circle_red;
            if ("MANAGER".equals(role)) return R.drawable.bg_circle_orange;
            return R.drawable.bg_circle_blue;
        }
    }
}

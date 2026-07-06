package com.example.mpos.shift;

import android.content.Intent;
import android.database.Cursor;
import android.os.Bundle;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import com.example.mpos.R;
import com.example.mpos.MainActivity;
import com.example.mpos.auth.SessionManager;
import com.example.mpos.dao.ShiftDao;
import com.example.mpos.database.DatabaseHelper;
import com.example.mpos.inventory.InventoryActivity;
import com.example.mpos.order.OrderListActivity;
import com.example.mpos.pos.PosActivity;
import com.example.mpos.utils.CurrencyUtils;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class ShiftActivity extends AppCompatActivity {

    private ShiftDao dao;
    private long userId;
    private DatabaseHelper db;
    private TextView txtStatus, txtOpenTime, txtRevenue;
    private EditText cash, note;

    @Override
    public void onCreate(Bundle state) {
        super.onCreate(state);
        setContentView(R.layout.activity_shift);

        db = new DatabaseHelper(this);
        SessionManager session = new SessionManager(this);
        dao = new ShiftDao(db, session.getShopId());
        userId = session.getUser().id;

        txtStatus   = findViewById(R.id.txtShiftStatus);
        txtOpenTime = findViewById(R.id.txtShiftOpenTime);
        txtRevenue  = findViewById(R.id.txtShiftRevenue);
        cash = findViewById(R.id.inputShiftCash);
        note = findViewById(R.id.inputHandoverNote);

        findViewById(R.id.btnBack).setOnClickListener(v -> finish());
        findViewById(R.id.btnOpenShift).setOnClickListener(v -> {
            if (dao.getOpenShiftId(userId) > 0) { toast("Bạn đang có ca mở"); return; }
            dao.open(userId, parseMoney());
            toast("Đã mở ca thành công");
            refresh();
        });
        findViewById(R.id.btnCloseShift).setOnClickListener(v -> {
            long id = dao.getOpenShiftId(userId);
            if (id < 0) { toast("Chưa có ca mở"); return; }
            dao.close(id, parseMoney(), note.getText().toString().trim());
            toast("Đã đóng ca");
            refresh();
        });
        findViewById(R.id.btnShiftReport).setOnClickListener(v ->
            startActivity(new Intent(this, ShiftReportActivity.class)));

        bindStaffNav();
        refresh();
    }

    private void bindStaffNav() {
        navClick(R.id.navStaffHome,      MainActivity.class);
        navClick(R.id.navStaffPos,       PosActivity.class);
        navClick(R.id.navStaffOrders,    OrderListActivity.class);
        navClick(R.id.navStaffInventory, InventoryActivity.class);
        navClick(R.id.navStaffSettings,  ShiftActivity.class);
    }

    private void navClick(int id, Class<?> target) {
        android.view.View v = findViewById(id);
        if (v != null) v.setOnClickListener(x -> {
            if (!getClass().equals(target))
                startActivity(new Intent(this, target).addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP));
        });
    }

    @Override protected void onResume() { super.onResume(); refresh(); }

    private void refresh() {
        long shiftId = dao.getOpenShiftId(userId);
        boolean isOpen = shiftId > 0;

        txtStatus.setText(isOpen ? "Đang mở ca" : "Chưa mở ca");
        txtStatus.setBackgroundResource(isOpen ? R.drawable.badge_success : R.drawable.badge_warning);
        txtStatus.setTextColor(ContextCompat.getColor(this,
            isOpen ? R.color.status_success : R.color.status_warning));

        if (isOpen) {
            // Shift open time
            Cursor c = db.getReadableDatabase().rawQuery(
                "SELECT opened_at FROM shifts WHERE id=?", new String[]{String.valueOf(shiftId)});
            try {
                if (c.moveToFirst()) {
                    String timeStr = new SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault())
                        .format(new Date(c.getLong(0)));
                    txtOpenTime.setText("Mở ca lúc: " + timeStr);
                }
            } finally { c.close(); }

            // Revenue this shift
            Cursor rev = db.getReadableDatabase().rawQuery(
                "SELECT COALESCE(SUM(total_amount),0), COUNT(*) FROM orders WHERE shift_id=? AND status='PAID'",
                new String[]{String.valueOf(shiftId)});
            try {
                if (rev.moveToFirst()) {
                    long revenue = rev.getLong(0);
                    int count    = rev.getInt(1);
                    txtRevenue.setText(CurrencyUtils.vnd(revenue) + " · " + count + " đơn");
                }
            } finally { rev.close(); }
        } else {
            txtOpenTime.setText("Nhập tiền đầu ca và bấm 'Mở ca'");
            txtRevenue.setText("");
        }
    }

    private long parseMoney() {
        try { return Long.parseLong(cash.getText().toString().trim()); }
        catch (Exception e) { return 0; }
    }

    private void toast(String msg) { Toast.makeText(this, msg, Toast.LENGTH_SHORT).show(); }
}

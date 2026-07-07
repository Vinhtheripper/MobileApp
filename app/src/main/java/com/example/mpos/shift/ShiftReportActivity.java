package com.example.mpos.shift;

import android.database.Cursor;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.example.mpos.R;
import com.example.mpos.auth.SessionManager;
import com.example.mpos.database.DatabaseHelper;
import com.example.mpos.utils.CurrencyUtils;

/**
 * Shift Report Screen — shows the last shift summary for the current user.
 *
 * Layout: activity_shift_report.xml (redesigned)
 * Data: raw SQLite query to shifts table — candidate for DAO extraction in Step 19.
 */
public class ShiftReportActivity extends AppCompatActivity {

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_shift_report);

        // Back button
        View btnBack = findViewById(R.id.btnBack);
        if (btnBack != null) btnBack.setOnClickListener(v -> finish());

        // Share stub
        View btnShare = findViewById(R.id.btnShare);
        if (btnShare != null) btnShare.setOnClickListener(v -> shareReport());

        loadShiftData();
    }

    private void loadShiftData() {
        TextView txtShiftCode   = findViewById(R.id.txtShiftCode);
        TextView txtOpenTime    = findViewById(R.id.txtOpenTime);
        TextView txtCloseTime   = findViewById(R.id.txtCloseTime);
        TextView txtCashier     = findViewById(R.id.txtCashier);
        TextView txtTotalRevenue = findViewById(R.id.txtTotalRevenue);
        TextView txtOrderCount  = findViewById(R.id.txtOrderCount);
        TextView txtAvgOrder    = findViewById(R.id.txtAvgOrder);
        TextView txtOpeningCash = findViewById(R.id.txtOpeningCash);
        TextView txtActualCash  = findViewById(R.id.txtActualCash);
        TextView txtDifference  = findViewById(R.id.txtDifference);
        TextView txtHandoverNote = findViewById(R.id.txtHandoverNote);

        long userId = new SessionManager(this).getUser().id;

        // Query last shift for this user
        try (Cursor c = new DatabaseHelper(this).getReadableDatabase().rawQuery(
                "SELECT s.shift_code, s.status, s.opening_cash, s.expected_cash, " +
                "s.actual_cash, s.difference_amount, s.handover_note, " +
                "strftime('%H:%M %d/%m', datetime(s.opened_at/1000,'unixepoch','localtime')) as open_time, " +
                "strftime('%H:%M %d/%m', datetime(s.closed_at/1000,'unixepoch','localtime')) as close_time, " +
                "u.username as cashier_name " +
                "FROM shifts s LEFT JOIN users u ON s.user_id = u.id " +
                "WHERE s.user_id = ? ORDER BY s.id DESC LIMIT 1",
                new String[]{String.valueOf(userId)})) {

            if (c != null && c.moveToFirst()) {
                if (txtShiftCode != null)
                    txtShiftCode.setText(c.getString(0) != null ? c.getString(0) : "—");
                if (txtOpenTime != null)
                    txtOpenTime.setText(c.getString(7) != null ? c.getString(7) : "—");
                if (txtCloseTime != null) {
                    String closeTime = c.getString(8);
                    txtCloseTime.setText(closeTime != null && !closeTime.isEmpty() ? closeTime : "Đang mở");
                }
                if (txtCashier != null)
                    txtCashier.setText(c.getString(9) != null ? c.getString(9) : "—");

                // Revenue & orders — query separately
                loadRevenueStats(userId, txtTotalRevenue, txtOrderCount, txtAvgOrder);

                long openingCash  = c.getLong(2);
                long actualCash   = c.getLong(4);
                long difference   = c.getLong(5);

                if (txtOpeningCash != null)
                    txtOpeningCash.setText(CurrencyUtils.vnd(openingCash));
                if (txtActualCash != null)
                    txtActualCash.setText(CurrencyUtils.vnd(actualCash));
                if (txtDifference != null) {
                    txtDifference.setText(CurrencyUtils.vnd(difference));
                    // Red if negative
                    if (difference < 0) {
                        txtDifference.setTextColor(0xFFEF4444);
                    }
                }

                String note = c.getString(6);
                if (txtHandoverNote != null)
                    txtHandoverNote.setText(note != null && !note.isEmpty() ? note : "Không có ghi chú");

            } else {
                if (txtShiftCode != null) txtShiftCode.setText("Chưa có ca nào");
                if (txtHandoverNote != null) txtHandoverNote.setText("Không có dữ liệu ca");
            }
        } catch (Exception e) {
            if (txtHandoverNote != null)
                txtHandoverNote.setText("Lỗi tải dữ liệu: " + e.getMessage());
        }
    }

    private void loadRevenueStats(long userId, TextView tvRevenue, TextView tvOrders, TextView tvAvg) {
        try (Cursor c = new DatabaseHelper(this).getReadableDatabase().rawQuery(
                "SELECT COUNT(*), COALESCE(SUM(total_amount), 0) " +
                "FROM orders WHERE user_id = ? AND DATE(created_at/1000,'unixepoch') = DATE('now')",
                new String[]{String.valueOf(userId)})) {

            if (c != null && c.moveToFirst()) {
                int count  = c.getInt(0);
                long total = c.getLong(1);
                long avg   = count > 0 ? total / count : 0;
                if (tvRevenue != null) tvRevenue.setText(CurrencyUtils.vnd(total));
                if (tvOrders != null)  tvOrders.setText(String.valueOf(count));
                if (tvAvg != null)     tvAvg.setText(CurrencyUtils.vnd(avg));
            }
        } catch (Exception ignored) {}
    }

    private void shareReport() {
        android.content.Intent intent = new android.content.Intent(android.content.Intent.ACTION_SEND);
        intent.setType("text/plain");
        intent.putExtra(android.content.Intent.EXTRA_SUBJECT, "Báo cáo ca — Quầy mPOS");
        intent.putExtra(android.content.Intent.EXTRA_TEXT, "Xem báo cáo ca trong ứng dụng Quầy mPOS.");
        startActivity(android.content.Intent.createChooser(intent, "Chia sẻ qua"));
    }
}

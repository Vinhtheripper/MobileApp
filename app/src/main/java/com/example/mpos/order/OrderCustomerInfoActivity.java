package com.example.mpos.order;

import android.content.Intent;
import android.database.Cursor;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.mpos.R;
import com.example.mpos.database.DatabaseHelper;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class OrderCustomerInfoActivity extends AppCompatActivity {

    private EditText inputSmartPaste, inputName, inputPhone, inputAddress, inputNote, inputSearch;
    private LinearLayout layoutResult;
    private TextView txtResultName, txtResultPhone, txtResultInitials;
    private long selectedCustomerId = -1;
    private DatabaseHelper db;

    @Override
    public void onCreate(Bundle state) {
        super.onCreate(state);
        setContentView(R.layout.activity_order_customer_info);

        db = new DatabaseHelper(this);

        inputSmartPaste = findViewById(R.id.inputSmartPaste);
        inputName       = findViewById(R.id.inputCustomerName);
        inputPhone      = findViewById(R.id.inputCustomerPhone);
        inputAddress    = findViewById(R.id.inputCustomerAddress);
        inputNote       = findViewById(R.id.inputOrderNote);
        inputSearch     = findViewById(R.id.inputSearchExistingCustomer);
        layoutResult    = findViewById(R.id.layoutCustomerResult);
        txtResultName   = findViewById(R.id.txtResultName);
        txtResultPhone  = findViewById(R.id.txtResultPhone);
        txtResultInitials = findViewById(R.id.txtResultInitials);

        findViewById(R.id.btnBack).setOnClickListener(v -> finish());
        findViewById(R.id.btnSkip).setOnClickListener(v -> goCheckout());
        findViewById(R.id.btnAutoFill).setOnClickListener(v -> autoFill());
        findViewById(R.id.btnLookupCustomer).setOnClickListener(v -> lookupCustomer());
        findViewById(R.id.layoutCustomerResult).setOnClickListener(v -> applyResult());
        findViewById(R.id.btnProceedCheckout).setOnClickListener(v -> goCheckout());

        inputSearch.addTextChangedListener(new TextWatcher() {
            public void afterTextChanged(Editable s) {
                if (s.length() >= 3) lookupCustomer();
                else layoutResult.setVisibility(View.GONE);
            }
            public void beforeTextChanged(CharSequence s, int a, int b, int c) {}
            public void onTextChanged(CharSequence s, int a, int b, int c) {}
        });
    }

    private void autoFill() {
        String text = inputSmartPaste.getText().toString().trim();
        if (text.isEmpty()) {
            Toast.makeText(this, "Vui lòng dán tin nhắn vào ô trên", Toast.LENGTH_SHORT).show();
            return;
        }

        // Extract Vietnamese phone number
        String phone = extractPhone(text);
        // Extract name (heuristic: word after "tên", "mình là", "em là", or first capitalized sequence)
        String name = extractName(text);
        // Extract address (after "địa chỉ", "giao về", "giao tới", "địa chi")
        String address = extractAddress(text);

        if (!name.isEmpty())    inputName.setText(name);
        if (!phone.isEmpty())   inputPhone.setText(phone);
        if (!address.isEmpty()) inputAddress.setText(address);

        boolean found = !name.isEmpty() || !phone.isEmpty() || !address.isEmpty();
        Toast.makeText(this,
            found ? "Đã tự động điền thông tin!" : "Không nhận diện được thông tin, vui lòng điền thủ công",
            Toast.LENGTH_SHORT).show();
    }

    private String extractPhone(String text) {
        // Match Vietnamese phone: 10 digits starting with 0, or +84 prefix
        Pattern p = Pattern.compile("(?:\\+84|0)(\\d{9})");
        Matcher m = p.matcher(text.replaceAll("[\\s\\-\\.]", ""));
        if (m.find()) {
            String raw = m.group(0);
            return raw.startsWith("+84") ? "0" + raw.substring(3) : raw;
        }
        return "";
    }

    private String extractName(String text) {
        // Look for patterns like "tên [name]", "mình là [name]", "em là [name]", "cho [name] đặt"
        String lower = text.toLowerCase();
        String[] patterns = {"tên ", "mình tên ", "em tên ", "anh tên ", "chị tên ",
                             "mình là ", "em là ", "anh là ", "chị là ", "name: ", "họ tên: "};
        for (String pat : patterns) {
            int idx = lower.indexOf(pat);
            if (idx >= 0) {
                String after = text.substring(idx + pat.length()).trim();
                // Take up to the next comma, newline, or punctuation
                int end = after.length();
                for (char ch : new char[]{',', '\n', '.', '!', '?', '-', ';', '/'}) {
                    int pos = after.indexOf(ch);
                    if (pos > 0 && pos < end) end = pos;
                }
                String name = after.substring(0, end).trim();
                if (name.length() >= 2 && name.length() <= 50) return capitalize(name);
            }
        }
        return "";
    }

    private String extractAddress(String text) {
        String lower = text.toLowerCase();
        String[] markers = {"địa chỉ: ", "địa chỉ:", "giao về ", "giao tới ", "giao đến ",
                            "ship về ", "ship tới ", "địa chi: ", "address: ", "giao hàng: "};
        for (String marker : markers) {
            int idx = lower.indexOf(marker);
            if (idx >= 0) {
                String after = text.substring(idx + marker.length()).trim();
                // Take until double newline or end
                int end = after.indexOf("\n\n");
                if (end < 0) end = after.length();
                int nlEnd = after.indexOf('\n');
                if (nlEnd > 0 && nlEnd < end) {
                    // Check if next line looks like continuation
                    String next = after.substring(nlEnd + 1).trim();
                    if (!next.isEmpty() && Character.isDigit(next.charAt(0))) end = after.indexOf('\n', nlEnd + 1);
                    if (end < 0) end = after.length();
                }
                String addr = after.substring(0, Math.min(end, after.length())).trim();
                if (addr.length() >= 5) return addr;
            }
        }
        return "";
    }

    private String capitalize(String s) {
        if (s == null || s.isEmpty()) return s;
        StringBuilder sb = new StringBuilder();
        boolean cap = true;
        for (char c : s.toCharArray()) {
            sb.append(cap ? Character.toUpperCase(c) : c);
            cap = (c == ' ');
        }
        return sb.toString();
    }

    private void lookupCustomer() {
        String q = "%" + inputSearch.getText().toString().trim() + "%";
        long shopId = new com.example.mpos.auth.SessionManager(this).getShopId();
        Cursor c = db.getReadableDatabase().rawQuery(
            "SELECT c.id, c.full_name, c.phone FROM customers c " +
            "WHERE c.shop_id=? AND (c.full_name LIKE ? OR c.phone LIKE ?) LIMIT 1",
            new String[]{String.valueOf(shopId), q, q});
        try {
            if (c.moveToFirst()) {
                long id      = c.getLong(0);
                String name  = c.getString(1);
                String phone = c.getString(2);
                selectedCustomerId = id;
                txtResultName.setText(name != null ? name : "Khách hàng");
                txtResultPhone.setText(phone != null ? phone : "");
                String initials = (name != null && name.length() >= 2) ? name.substring(0, 2).toUpperCase() : "KH";
                txtResultInitials.setText(initials);
                layoutResult.setVisibility(View.VISIBLE);
            } else {
                layoutResult.setVisibility(View.GONE);
                selectedCustomerId = -1;
            }
        } finally { c.close(); }
    }

    private void applyResult() {
        inputName.setText(txtResultName.getText());
        inputPhone.setText(txtResultPhone.getText());
        Toast.makeText(this, "Đã chọn khách hàng", Toast.LENGTH_SHORT).show();
    }

    private void goCheckout() {
        Intent i = new Intent(this, CheckoutActivity.class);
        String name    = inputName.getText().toString().trim();
        String phone   = inputPhone.getText().toString().trim();
        String address = inputAddress.getText().toString().trim();
        String note    = inputNote.getText().toString().trim();
        if (!name.isEmpty())    i.putExtra("customer_name", name);
        if (!phone.isEmpty())   i.putExtra("customer_phone", phone);
        if (!address.isEmpty()) i.putExtra("customer_address", address);
        if (!note.isEmpty())    i.putExtra("order_note", note);
        if (selectedCustomerId > 0) i.putExtra("customer_id", selectedCustomerId);
        startActivity(i);
    }
}

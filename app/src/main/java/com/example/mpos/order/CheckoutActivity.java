package com.example.mpos.order;

import android.content.Intent;
import android.os.Bundle;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.mpos.R;
import com.example.mpos.auth.SessionManager;
import com.example.mpos.cart.CartManager;
import com.example.mpos.dao.ShiftDao;
import com.example.mpos.database.DatabaseHelper;
import com.example.mpos.receipt.ReceiptActivity;
import com.example.mpos.utils.CurrencyUtils;

public class CheckoutActivity extends AppCompatActivity {
    @Override public void onCreate(Bundle state) { super.onCreate(state); setContentView(R.layout.activity_checkout); findViewById(R.id.btnBack).setOnClickListener(v -> finish()); TextView total=findViewById(R.id.txtTotal); total.setText(CurrencyUtils.vnd(CartManager.get().subtotal())); Spinner method=findViewById(R.id.spinnerMethod); method.setAdapter(new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item, new String[]{"CASH","VNPAY_QR","MOMO_QR"}));
        findViewById(R.id.btnConfirmPayment).setOnClickListener(v -> { SessionManager session=new SessionManager(this); long shiftId=new ShiftDao(new DatabaseHelper(this)).getOpenShiftId(session.getUser().id); if(shiftId<0){Toast.makeText(this,"Cần mở ca trước khi bán",Toast.LENGTH_LONG).show(); return;} try { long orderId=new CheckoutService(new DatabaseHelper(this)).checkout(session.getUser().id, shiftId, ((EditText)findViewById(R.id.inputCustomerPhone)).getText().toString(), method.getSelectedItem().toString(), parseMoney(((EditText)findViewById(R.id.inputReceived)).getText().toString())); Intent intent=new Intent(this, ReceiptActivity.class); intent.putExtra("order_id",orderId); startActivity(intent); finish(); } catch(Exception e){Toast.makeText(this,e.getMessage(),Toast.LENGTH_LONG).show();} });
    }
    private long parseMoney(String value) { try { return Long.parseLong(value.trim()); } catch(Exception ignored) { return 0; } }
}

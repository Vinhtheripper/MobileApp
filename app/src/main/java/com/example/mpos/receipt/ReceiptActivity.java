package com.example.mpos.receipt;

import android.database.Cursor;
import android.os.Bundle;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import com.example.mpos.R;
import com.example.mpos.database.DatabaseHelper;
import com.example.mpos.utils.CurrencyUtils;

public class ReceiptActivity extends AppCompatActivity {
    @Override public void onCreate(Bundle state) { super.onCreate(state); setContentView(R.layout.activity_receipt); findViewById(R.id.btnBack).setOnClickListener(v -> finish()); long id=getIntent().getLongExtra("order_id",-1); Cursor c=new DatabaseHelper(this).getReadableDatabase().rawQuery("SELECT order_code,total_amount,status FROM orders WHERE id=?",new String[]{String.valueOf(id)}); try { TextView text=findViewById(R.id.txtReceipt); if(c.moveToFirst()) text.setText("mPOS Pro\n\nHóa đơn: "+c.getString(0)+"\nTrạng thái: "+c.getString(2)+"\n\nTổng thanh toán\n"+CurrencyUtils.vnd(c.getLong(1))+"\n\nCảm ơn quý khách!"); } finally { c.close(); } }
}

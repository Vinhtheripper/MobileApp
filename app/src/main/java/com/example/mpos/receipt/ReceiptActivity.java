package com.example.mpos.receipt;

import android.database.Cursor;
import android.content.Intent;
import android.os.Bundle;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import com.example.mpos.R;
import com.example.mpos.database.DatabaseHelper;
import com.example.mpos.utils.CurrencyUtils;
import com.example.mpos.pos.PosActivity;

public class ReceiptActivity extends AppCompatActivity {
    @Override public void onCreate(Bundle state) { super.onCreate(state); setContentView(R.layout.activity_receipt); findViewById(R.id.btnBack).setOnClickListener(v -> finish()); findViewById(R.id.btnNewSale).setOnClickListener(v->{startActivity(new Intent(this, PosActivity.class));finish();}); findViewById(R.id.btnPrintReceipt).setOnClickListener(v->Toast.makeText(this,"In hóa đơn sẽ được kết nối ở bước sau",Toast.LENGTH_SHORT).show()); findViewById(R.id.btnShareReceipt).setOnClickListener(v->Toast.makeText(this,"Gửi hóa đơn sẽ được kết nối ở bước sau",Toast.LENGTH_SHORT).show()); long id=getIntent().getLongExtra("order_id",-1); Cursor c=new DatabaseHelper(this).getReadableDatabase().rawQuery("SELECT order_code,total_amount,status FROM orders WHERE id=?",new String[]{String.valueOf(id)}); try { TextView text=findViewById(R.id.txtReceipt); if(c.moveToFirst()) text.setText("mPOS Pro\n\nHóa đơn: "+c.getString(0)+"\nTrạng thái: "+c.getString(2)+"\n\nTổng thanh toán\n"+CurrencyUtils.vnd(c.getLong(1))+"\n\nCảm ơn quý khách!"); } finally { c.close(); } }
}

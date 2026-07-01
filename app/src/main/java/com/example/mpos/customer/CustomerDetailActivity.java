package com.example.mpos.customer;

import android.content.Intent;
import android.database.Cursor;
import android.os.Bundle;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.mpos.R;
import com.example.mpos.cart.CartManager;
import com.example.mpos.constants.OrderConstants;
import com.example.mpos.dao.CustomerDao;
import com.example.mpos.dao.ProductDao;
import com.example.mpos.database.DatabaseHelper;
import com.example.mpos.model.Customer;
import com.example.mpos.model.Product;
import com.example.mpos.pos.PosActivity;
import com.example.mpos.utils.CurrencyUtils;

public class CustomerDetailActivity extends AppCompatActivity {
    private long customerId;
    private DatabaseHelper db;

    @Override public void onCreate(Bundle state) {
        super.onCreate(state); setContentView(R.layout.activity_customer_detail);
        customerId=getIntent().getLongExtra("customer_id",-1); db=new DatabaseHelper(this); render();
        findViewById(R.id.btnBack).setOnClickListener(v->finish());
        findViewById(R.id.btnEditCustomer).setOnClickListener(v->{Intent i=new Intent(this,CustomerFormActivity.class);i.putExtra("customer_id",customerId);startActivity(i);});
        findViewById(R.id.btnRepeatOrder).setOnClickListener(v->repeatLastOrder());
    }
    private void render(){ Customer customer=new CustomerDao(db).findById(customerId); long count=0,total=0; Cursor q=db.getReadableDatabase().rawQuery("SELECT COUNT(*),COALESCE(SUM(total_amount),0) FROM orders WHERE customer_id=? AND status IN (?,?)",new String[]{String.valueOf(customerId), OrderConstants.STATUS_PAID, OrderConstants.STATUS_COMPLETED}); try{if(q.moveToFirst()){count=q.getLong(0);total=q.getLong(1);}}finally{q.close();} ((TextView)findViewById(R.id.txtCustomerDetail)).setText(customer==null?"Không tìm thấy khách hàng":customer.fullName+"\n"+customer.phone+"\n"+(customer.email==null?"":customer.email)+"\n\nTổng đơn: "+count+"\nTổng chi tiêu: "+CurrencyUtils.vnd(total)+"\nĐiểm tích lũy: "+customer.loyaltyPoints); }
    private void repeatLastOrder(){ Cursor last=db.getReadableDatabase().rawQuery("SELECT id FROM orders WHERE customer_id=? ORDER BY created_at DESC LIMIT 1",new String[]{String.valueOf(customerId)}); long orderId=-1;try{if(last.moveToFirst())orderId=last.getLong(0);}finally{last.close();}if(orderId<0){Toast.makeText(this,"Khách chưa có đơn để lặp lại",Toast.LENGTH_SHORT).show();return;}CartManager.get().clear();ProductDao products=new ProductDao(db);Cursor lines=db.getReadableDatabase().rawQuery("SELECT product_id,quantity FROM order_items WHERE order_id=?",new String[]{String.valueOf(orderId)});try{while(lines.moveToNext()){Product p=products.findById(lines.getLong(0));for(int i=0;p!=null&&i<lines.getInt(1);i++)CartManager.get().add(p);}}finally{lines.close();}startActivity(new Intent(this,PosActivity.class)); }
}

package com.example.mpos.inventory;

import android.os.Bundle;
import android.widget.EditText;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import com.example.mpos.R;
import com.example.mpos.dao.ProductDao;
import com.example.mpos.database.DatabaseHelper;
import com.example.mpos.model.Product;
import com.example.mpos.auth.SessionManager;

public class StockAdjustmentActivity extends AppCompatActivity {
 @Override public void onCreate(Bundle b){super.onCreate(b);setContentView(R.layout.activity_stock_adjustment);findViewById(R.id.btnBack).setOnClickListener(v->finish());findViewById(R.id.btnAdjustStock).setOnClickListener(v->{EditText sku=findViewById(R.id.inputAdjustSku),qty=findViewById(R.id.inputAdjustStock);long shopId=new SessionManager(this).getShopId();ProductDao dao=new ProductDao(new DatabaseHelper(this),shopId);Product p=dao.findByBarcodeOrSku(sku.getText().toString().trim());if(p==null){Toast.makeText(this,"Không tìm thấy sản phẩm",Toast.LENGTH_SHORT).show();return;}try{dao.adjustStock(p.id,Integer.parseInt(qty.getText().toString()),new SessionManager(this).getUser().id,"Điều chỉnh thủ công");Toast.makeText(this,"Đã điều chỉnh tồn kho",Toast.LENGTH_SHORT).show();finish();}catch(Exception e){Toast.makeText(this,e.getMessage()==null?"Số lượng không hợp lệ":e.getMessage(),Toast.LENGTH_SHORT).show();}});}
}

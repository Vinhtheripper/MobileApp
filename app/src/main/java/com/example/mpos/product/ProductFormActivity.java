package com.example.mpos.product;

import android.os.Bundle;
import android.widget.EditText;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.mpos.R;
import com.example.mpos.dao.ProductDao;
import com.example.mpos.database.DatabaseHelper;
import com.example.mpos.model.Product;

public class ProductFormActivity extends AppCompatActivity {
    private ProductDao dao; private Product product; private EditText name, sku, barcode, price, stock, threshold;
    @Override public void onCreate(Bundle state){super.onCreate(state);setContentView(R.layout.activity_product_form);dao=new ProductDao(new DatabaseHelper(this));name=findViewById(R.id.inputProductName);sku=findViewById(R.id.inputSku);barcode=findViewById(R.id.inputBarcode);price=findViewById(R.id.inputPrice);stock=findViewById(R.id.inputStock);threshold=findViewById(R.id.inputMinStock);long id=getIntent().getLongExtra("product_id",-1);product=id>0?dao.findById(id):new Product();if(product==null)product=new Product();bind();findViewById(R.id.btnBack).setOnClickListener(v->finish());findViewById(R.id.btnSaveProduct).setOnClickListener(v->save());findViewById(R.id.btnArchiveProduct).setOnClickListener(v->{if(product.id>0){dao.softDelete(product.id);finish();}});}
    private void bind(){name.setText(product.name);sku.setText(product.sku);barcode.setText(product.barcode);if(product.id>0){price.setText(String.valueOf(product.salePrice));stock.setText(String.valueOf(product.stockQuantity));threshold.setText(String.valueOf(product.minStockQuantity));}}
    private void save(){if(name.getText().toString().trim().isEmpty()||sku.getText().toString().trim().isEmpty()){Toast.makeText(this,"Nhập tên và SKU sản phẩm",Toast.LENGTH_SHORT).show();return;}product.name=name.getText().toString().trim();product.sku=sku.getText().toString().trim();product.barcode=barcode.getText().toString().trim();product.salePrice=num(price);product.stockQuantity=(int)num(stock);product.minStockQuantity=(int)num(threshold);dao.save(product);Toast.makeText(this,"Đã lưu sản phẩm",Toast.LENGTH_SHORT).show();finish();}
    private long num(EditText e){try{return Long.parseLong(e.getText().toString().trim());}catch(Exception ignored){return 0;}}
}

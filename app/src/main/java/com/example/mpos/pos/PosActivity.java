package com.example.mpos.pos;

import android.content.Intent;
import android.os.Bundle;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.mpos.R;
import com.example.mpos.cart.CartManager;
import com.example.mpos.dao.ProductDao;
import com.example.mpos.database.DatabaseHelper;
import com.example.mpos.model.Product;
import com.example.mpos.order.CheckoutActivity;
import com.example.mpos.utils.CurrencyUtils;

import java.util.ArrayList;
import java.util.List;

public class PosActivity extends AppCompatActivity {
    private final List<Product> products = new ArrayList<>();
    private ProductDao productDao;
    private ArrayAdapter<String> adapter;
    private TextView cartSummary;
    @Override public void onCreate(Bundle state) { super.onCreate(state); setContentView(R.layout.activity_pos); findViewById(R.id.btnBack).setOnClickListener(v -> finish()); productDao=new ProductDao(new DatabaseHelper(this)); cartSummary=findViewById(R.id.txtCartSummary);
        ListView list=findViewById(R.id.listProducts); adapter=new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, new ArrayList<>()); list.setAdapter(adapter);
        findViewById(R.id.btnSearch).setOnClickListener(v -> load(((EditText)findViewById(R.id.inputSearch)).getText().toString()));
        list.setOnItemClickListener((parent, view, position, id) -> { Product product=products.get(position); if(!CartManager.get().add(product)) Toast.makeText(this,"Sản phẩm đã hết hoặc vượt tồn kho",Toast.LENGTH_SHORT).show(); refreshCart(); });
        findViewById(R.id.btnCheckout).setOnClickListener(v -> { if(CartManager.get().isEmpty()) { Toast.makeText(this,"Giỏ hàng đang trống",Toast.LENGTH_SHORT).show(); return; } startActivity(new Intent(this, CheckoutActivity.class)); });
        load(""); refreshCart();
    }
    @Override protected void onResume() { super.onResume(); refreshCart(); }
    private void load(String query) { products.clear(); products.addAll(productDao.search(query)); List<String> labels=new ArrayList<>(); for(Product p:products) labels.add(p.name+" • "+CurrencyUtils.vnd(p.salePrice)+" • Tồn: "+p.stockQuantity); adapter.clear(); adapter.addAll(labels); adapter.notifyDataSetChanged(); }
    private void refreshCart() { cartSummary.setText("Giỏ: "+CartManager.get().getItems().size()+" SP • "+CurrencyUtils.vnd(CartManager.get().subtotal())); }
}

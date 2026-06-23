package com.example.mpos.cart;

import android.content.Intent;
import android.os.Bundle;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import com.example.mpos.R;
import com.example.mpos.model.CartItem;
import com.example.mpos.order.CheckoutActivity;
import com.example.mpos.utils.CurrencyUtils;
import java.util.ArrayList;
import java.util.List;

public class CartActivity extends AppCompatActivity {
 @Override public void onCreate(Bundle state){super.onCreate(state);setContentView(R.layout.activity_cart);findViewById(R.id.btnBack).setOnClickListener(v->finish());findViewById(R.id.btnClearCart).setOnClickListener(v->{CartManager.get().clear();render();});findViewById(R.id.btnCartCheckout).setOnClickListener(v->{if(CartManager.get().isEmpty()){Toast.makeText(this,"Giỏ hàng đang trống",Toast.LENGTH_SHORT).show();return;}startActivity(new Intent(this,CheckoutActivity.class));});render();}
 @Override protected void onResume(){super.onResume();render();}
 private void render(){List<String> rows=new ArrayList<>();for(CartItem item:CartManager.get().getItems())rows.add(item.product.name+" × "+item.quantity+"\n"+CurrencyUtils.vnd(item.getLineTotal()));if(rows.isEmpty())rows.add("Giỏ hàng đang trống\nChọn sản phẩm để bắt đầu tạo đơn");((ListView)findViewById(R.id.listCart)).setAdapter(new ArrayAdapter<>(this,android.R.layout.simple_list_item_1,rows));((TextView)findViewById(R.id.txtCartTotal)).setText(CurrencyUtils.vnd(CartManager.get().subtotal()));}
}

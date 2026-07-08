package com.example.mpos.cart;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.mpos.R;
import com.example.mpos.model.CartItem;
import com.example.mpos.order.CheckoutActivity;
import com.example.mpos.order.OrderCustomerInfoActivity;
import com.example.mpos.utils.CurrencyUtils;
import com.example.mpos.utils.ImageUtils;

import java.util.List;

public class CartActivity extends AppCompatActivity {
    private CartAdapter adapter;
    private TextView txtSubtotal, txtTax, txtTotal, txtTitle;

    @Override public void onCreate(Bundle state) {
        super.onCreate(state);
        setContentView(R.layout.activity_cart);
        txtSubtotal = findViewById(R.id.txtSubtotal);
        txtTax = findViewById(R.id.txtTax);
        txtTotal = findViewById(R.id.txtCartTotal);
        txtTitle = findViewById(R.id.txtCartTitle);

        adapter = new CartAdapter();
        RecyclerView rv = findViewById(R.id.listCart);
        rv.setLayoutManager(new LinearLayoutManager(this));
        rv.setAdapter(adapter);

        findViewById(R.id.btnBack).setOnClickListener(v -> finish());
        findViewById(R.id.btnClearCart).setOnClickListener(v ->
            new AlertDialog.Builder(this).setTitle("Xóa giỏ hàng")
                .setMessage("Bạn có chắc muốn xóa toàn bộ giỏ hàng?")
                .setPositiveButton("Xóa", (d, w) -> { CartManager.get().clear(); finish(); })
                .setNegativeButton("Hủy", null).show());
        findViewById(R.id.btnCartCheckout).setOnClickListener(v -> {
            if (CartManager.get().isEmpty()) { Toast.makeText(this, "Giỏ hàng đang trống", Toast.LENGTH_SHORT).show(); return; }
            startActivity(new Intent(this, OrderCustomerInfoActivity.class));
        });
        render();
    }

    @Override protected void onResume() {
        super.onResume();
        if (CartManager.get().isEmpty()) { finish(); return; }
        render();
    }

    private void render() {
        List<CartItem> items = CartManager.get().getItems();
        adapter.setItems(items);
        int count = CartManager.get().getCount();
        txtTitle.setText("Giỏ hàng" + (count > 0 ? " (" + count + ")" : ""));
        txtSubtotal.setText(CurrencyUtils.vnd(CartManager.get().subtotal()));
        txtTax.setText(CurrencyUtils.vnd(CartManager.get().tax()));
        txtTotal.setText(CurrencyUtils.vnd(CartManager.get().total()));
    }

    class CartAdapter extends RecyclerView.Adapter<CartAdapter.VH> {
        private List<CartItem> items = new java.util.ArrayList<>();
        void setItems(List<CartItem> list) { items = list; notifyDataSetChanged(); }

        @Override public int getItemCount() { return items.isEmpty() ? 1 : items.size(); }
        @Override public int getItemViewType(int pos) { return items.isEmpty() ? 0 : 1; }

        @Override
        public VH onCreateViewHolder(ViewGroup parent, int viewType) {
            View v;
            if (viewType == 0) {
                // Empty state
                TextView tv = new TextView(CartActivity.this);
                tv.setText("Giỏ hàng đang trống\nChọn sản phẩm từ màn hình POS");
                tv.setGravity(android.view.Gravity.CENTER);
                tv.setPadding(0, 120, 0, 120);
                tv.setTextColor(getResources().getColor(R.color.text_secondary, null));
                return new VH(tv, true);
            }
            v = LayoutInflater.from(CartActivity.this).inflate(R.layout.item_cart, parent, false);
            return new VH(v, false);
        }

        @Override
        public void onBindViewHolder(VH h, int pos) {
            if (h.isEmpty) return;
            CartItem item = items.get(pos);
            h.txtName.setText(item.product.name);
            h.txtUnitPrice.setText(CurrencyUtils.vnd(item.product.salePrice) + " / cái");
            h.txtQty.setText(String.valueOf(item.quantity));
            h.txtTotal.setText(CurrencyUtils.vnd(item.getLineTotal()));
            ImageUtils.load(CartActivity.this, item.product.imageUri, h.imgItem, h.txtInitials, item.product.name);
            long pid = item.product.id;
            h.btnInc.setOnClickListener(v -> { CartManager.get().increment(pid); render(); });
            h.btnDec.setOnClickListener(v -> { CartManager.get().decrement(pid); render(); });
        }

        class VH extends RecyclerView.ViewHolder {
            boolean isEmpty;
            TextView txtName, txtUnitPrice, txtQty, txtTotal, txtInitials;
            android.widget.ImageView imgItem;
            View btnInc, btnDec;

            VH(View v, boolean empty) {
                super(v);
                isEmpty = empty;
                if (!empty) {
                    txtName      = v.findViewById(R.id.txtItemName);
                    txtUnitPrice = v.findViewById(R.id.txtItemUnitPrice);
                    txtQty       = v.findViewById(R.id.txtQty);
                    txtTotal     = v.findViewById(R.id.txtLineTotal);
                    btnInc       = v.findViewById(R.id.btnIncrement);
                    btnDec       = v.findViewById(R.id.btnDecrement);
                    imgItem      = v.findViewById(R.id.imgCartItem);
                    txtInitials  = v.findViewById(R.id.txtCartItemInitials);
                }
            }
        }
    }
}

package com.example.mpos.adapter;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;

import com.example.mpos.R;
import com.example.mpos.model.Product;
import com.example.mpos.utils.CurrencyUtils;

import java.util.ArrayList;
import java.util.List;

/** Compact, touch-friendly product rows for the POS screen. */
public class ProductPosAdapter extends BaseAdapter {
    private final LayoutInflater inflater;
    private final List<Product> items = new ArrayList<>();
    public ProductPosAdapter(Context context) { inflater = LayoutInflater.from(context); }
    public void submit(List<Product> products) { items.clear(); items.addAll(products); notifyDataSetChanged(); }
    @Override public int getCount() { return items.size(); }
    @Override public Product getItem(int position) { return items.get(position); }
    @Override public long getItemId(int position) { return items.get(position).id; }
    @Override public View getView(int position, View convertView, ViewGroup parent) {
        View view = convertView == null ? inflater.inflate(R.layout.item_product_pos, parent, false) : convertView;
        Product product = getItem(position);
        ((TextView) view.findViewById(R.id.txtProductName)).setText(product.name);
        ((TextView) view.findViewById(R.id.txtProductSku)).setText(product.sku == null ? "Không có SKU" : product.sku);
        ((TextView) view.findViewById(R.id.txtProductPrice)).setText(CurrencyUtils.vnd(product.salePrice));
        TextView stock = view.findViewById(R.id.txtProductStock);
        stock.setText(product.stockQuantity > 0 ? "Còn " + product.stockQuantity : "Hết hàng");
        stock.setBackgroundResource(product.stockQuantity > 0 ? R.drawable.badge_success : R.drawable.badge_error);
        stock.setTextColor(view.getResources().getColor(product.stockQuantity > 0 ? R.color.status_success : R.color.status_error));
        return view;
    }
}

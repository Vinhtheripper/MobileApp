package com.example.mpos.adapter;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.example.mpos.R;
import com.example.mpos.model.Product;
import com.example.mpos.utils.CurrencyUtils;
import com.example.mpos.utils.ImageUtils;

import java.util.ArrayList;
import java.util.List;

public class ProductPosAdapter extends BaseAdapter {
    private final LayoutInflater inflater;
    private final List<Product> items = new ArrayList<>();

    public ProductPosAdapter(Context context) { inflater = LayoutInflater.from(context); }

    public void submit(List<Product> products) { items.clear(); items.addAll(products); notifyDataSetChanged(); }

    @Override public int getCount() { return items.size(); }
    @Override public Product getItem(int position) { return items.get(position); }
    @Override public long getItemId(int position) { return items.get(position).id; }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        if (convertView == null) convertView = inflater.inflate(R.layout.item_product_pos, parent, false);
        Product p = getItem(position);

        TextView txtMark          = convertView.findViewById(R.id.txtProductMark);
        ImageView imgProd         = convertView.findViewById(R.id.imgProduct);
        TextView txtName          = convertView.findViewById(R.id.txtProductName);
        TextView txtPrice         = convertView.findViewById(R.id.txtProductPrice);
        LinearLayout stockBadge   = convertView.findViewById(R.id.stockBadge);
        TextView stockDot         = convertView.findViewById(R.id.stockBadgeDot);
        TextView stockText        = convertView.findViewById(R.id.stockBadgeText);
        FrameLayout outOverlay    = convertView.findViewById(R.id.outOfStockOverlay);
        TextView btnAdd           = convertView.findViewById(R.id.btnAddProduct);

        ImageUtils.load(inflater.getContext(), p.imageUri, imgProd, txtMark, p.name);

        txtName.setText(p.name);
        txtPrice.setText(CurrencyUtils.vnd(p.salePrice));

        boolean outOfStock = p.stockQuantity <= 0;
        boolean lowStock   = !outOfStock && p.stockQuantity <= p.minStockQuantity;

        if (outOfStock) {
            stockBadge.setVisibility(View.GONE);
            outOverlay.setVisibility(View.VISIBLE);
            btnAdd.setBackgroundResource(R.drawable.bg_pos_add_disabled);
            btnAdd.setTextColor(0xFF9CA3AF);
            convertView.setAlpha(0.6f);
        } else {
            outOverlay.setVisibility(View.GONE);
            convertView.setAlpha(1f);
            btnAdd.setBackgroundResource(R.drawable.bg_circle_blue);
            btnAdd.setTextColor(0xFFFFFFFF);
            stockBadge.setVisibility(View.VISIBLE);
            if (lowStock) {
                stockBadge.setBackgroundResource(R.drawable.bg_stock_low);
                stockDot.setTextColor(0xFFEAB308);
                stockText.setText("Sắp hết");
                stockText.setTextColor(0xFFA16207);
            } else {
                stockBadge.setBackgroundResource(R.drawable.bg_stock_in);
                stockDot.setTextColor(0xFF22C55E);
                stockText.setText("Còn hàng");
                stockText.setTextColor(0xFF15803D);
            }
        }

        return convertView;
    }
}
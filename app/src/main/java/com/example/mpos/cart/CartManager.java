package com.example.mpos.cart;

import com.example.mpos.model.CartItem;
import com.example.mpos.model.Product;

import java.util.ArrayList;
import java.util.List;

/** Process-local cart. Orders are saved only during confirmed checkout. */
public final class CartManager {
    private static final CartManager INSTANCE = new CartManager();
    private final List<CartItem> items = new ArrayList<>();
    private CartManager() { }
    public static CartManager get() { return INSTANCE; }
    public List<CartItem> getItems() { return new ArrayList<>(items); }
    public boolean add(Product product) {
        for (CartItem item : items) { if (item.product.id == product.id) { if (item.quantity >= product.stockQuantity) return false; item.quantity++; return true; } }
        if (product.stockQuantity < 1) return false;
        CartItem item = new CartItem(); item.product = product; item.quantity = 1; items.add(item); return true;
    }
    public long subtotal() { long total=0; for (CartItem item : items) total += item.getLineTotal(); return total; }
    public boolean isEmpty() { return items.isEmpty(); }
    public void clear() { items.clear(); }
}

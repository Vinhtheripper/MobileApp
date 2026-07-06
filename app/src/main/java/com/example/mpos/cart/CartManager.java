package com.example.mpos.cart;

import com.example.mpos.model.CartItem;
import com.example.mpos.model.Product;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public final class CartManager {
    private static final CartManager INSTANCE = new CartManager();
    private final List<CartItem> items = new ArrayList<>();
    private CartManager() { }
    public static CartManager get() { return INSTANCE; }

    public List<CartItem> getItems() { return new ArrayList<>(items); }

    public boolean add(Product product) {
        for (CartItem item : items) {
            if (item.product.id == product.id) {
                if (item.quantity >= product.stockQuantity) return false;
                item.quantity++; return true;
            }
        }
        if (product.stockQuantity < 1) return false;
        CartItem item = new CartItem(); item.product = product; item.quantity = 1; items.add(item); return true;
    }

    public void increment(long productId) {
        for (CartItem item : items) {
            if (item.product.id == productId) {
                if (item.quantity < item.product.stockQuantity) item.quantity++;
                return;
            }
        }
    }

    public void decrement(long productId) {
        Iterator<CartItem> it = items.iterator();
        while (it.hasNext()) {
            CartItem item = it.next();
            if (item.product.id == productId) {
                item.quantity--;
                if (item.quantity <= 0) it.remove();
                return;
            }
        }
    }

    public int getCount() { int n = 0; for (CartItem i : items) n += i.quantity; return n; }
    public long subtotal() { long t = 0; for (CartItem i : items) t += i.getLineTotal(); return t; }
    public long tax() { return subtotal() / 10; }  // 10%
    public long total() { return subtotal() + tax(); }
    public boolean isEmpty() { return items.isEmpty(); }
    public void clear() { items.clear(); }
}

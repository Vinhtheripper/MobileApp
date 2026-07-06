package com.example.mpos.model;

public class CartItem {
    public Product product;
    public int quantity;

    public long getLineTotal() { return product.salePrice * quantity; }
}

package com.example.mpos.model;

public class Product {
    public long id;
    public String barcode;
    public String sku;
    public String name;
    public long salePrice;
    public long costPrice;
    public String description;
    public int stockQuantity;
    public int minStockQuantity;
    public long categoryId;
      public String imageUri;
    public boolean isActive = true;
    public long createdAt;
    public long updatedAt;
}

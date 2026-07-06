package com.example.mpos.model;

public class Category {
    public static final long ALL_ID = -1;
    public long id;
    public String name;
    public Category() {}
    public Category(long id, String name) { this.id = id; this.name = name; }
    @Override public String toString() { return name; }
}

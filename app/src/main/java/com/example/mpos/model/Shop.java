package com.example.mpos.model;

public class Shop {
    public long id;
    public String name;
    public String address;
    public String phone;
    public String logoUri;
    public long ownerUserId;
    public long createdAt;
    public String memberRole; // role of current user in this shop

    public static class Member {
        public long userId;
        public String fullName;
        public String email;
        public String role;
    }
}
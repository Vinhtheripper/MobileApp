package com.example.mpos.dao;

import android.content.ContentValues;
import android.database.Cursor;

import com.example.mpos.database.DatabaseHelper;
import com.example.mpos.model.Customer;

import java.util.ArrayList;
import java.util.List;

public class CustomerDao {
    private final DatabaseHelper helper;
    public CustomerDao(DatabaseHelper helper){this.helper=helper;}
    public List<Customer> search(String term){String p="%"+term.trim()+"%";Cursor c=helper.getReadableDatabase().rawQuery("SELECT id,full_name,phone,email,address,loyalty_points FROM customers WHERE full_name LIKE ? OR phone LIKE ? ORDER BY updated_at DESC LIMIT 100",new String[]{p,p});List<Customer> result=new ArrayList<>();try{while(c.moveToNext())result.add(map(c));}finally{c.close();}return result;}
    public Customer findById(long id){Cursor c=helper.getReadableDatabase().rawQuery("SELECT id,full_name,phone,email,address,loyalty_points FROM customers WHERE id=?",new String[]{String.valueOf(id)});try{return c.moveToFirst()?map(c):null;}finally{c.close();}}
    public long save(Customer customer){ContentValues v=new ContentValues();v.put("full_name",customer.fullName);v.put("phone",customer.phone);v.put("email",customer.email);v.put("address",customer.address);v.put("updated_at",System.currentTimeMillis());if(customer.id<=0){v.put("created_at",System.currentTimeMillis());return helper.getWritableDatabase().insertOrThrow("customers",null,v);}helper.getWritableDatabase().update("customers",v,"id=?",new String[]{String.valueOf(customer.id)});return customer.id;}
    private Customer map(Cursor c){Customer x=new Customer();x.id=c.getLong(0);x.fullName=c.getString(1);x.phone=c.getString(2);x.email=c.getString(3);x.address=c.getString(4);x.loyaltyPoints=c.getInt(5);return x;}
}

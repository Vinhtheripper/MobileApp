package com.example.mpos.product;

import android.content.Intent;
import android.os.Bundle;
import android.widget.EditText;
import android.widget.ListView;

import androidx.appcompat.app.AppCompatActivity;

import com.example.mpos.R;
import com.example.mpos.adapter.ProductPosAdapter;
import com.example.mpos.dao.ProductDao;
import com.example.mpos.database.DatabaseHelper;

public class ProductListActivity extends AppCompatActivity {
    private ProductDao dao; private ProductPosAdapter adapter; private EditText search;
    @Override public void onCreate(Bundle state) { super.onCreate(state); setContentView(R.layout.activity_product_list); dao=new ProductDao(new DatabaseHelper(this)); search=findViewById(R.id.inputProductSearch); adapter=new ProductPosAdapter(this); ListView list=findViewById(R.id.listProducts); list.setAdapter(adapter); findViewById(R.id.btnBack).setOnClickListener(v->finish()); findViewById(R.id.btnSearchProduct).setOnClickListener(v->load()); findViewById(R.id.btnAddProduct).setOnClickListener(v->startActivity(new Intent(this, ProductFormActivity.class))); list.setOnItemClickListener((p,v,pos,id)->{ Intent i=new Intent(this,ProductFormActivity.class); i.putExtra("product_id",adapter.getItem(pos).id); startActivity(i); }); load(); }
    @Override protected void onResume(){super.onResume(); if(dao!=null) load();}
    private void load(){adapter.submit(dao.search(search.getText().toString()));}
}

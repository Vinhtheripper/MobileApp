package com.example.mpos.settings;

import android.os.Bundle;
import android.widget.EditText;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.mpos.R;
import com.example.mpos.dao.SettingsDao;
import com.example.mpos.database.DatabaseHelper;

public class SettingsActivity extends AppCompatActivity {
    private SettingsDao dao; private EditText store, vat;
    @Override public void onCreate(Bundle state) { super.onCreate(state); setContentView(R.layout.activity_settings); dao=new SettingsDao(new DatabaseHelper(this)); store=findViewById(R.id.inputStoreName); vat=findViewById(R.id.inputVat); store.setText(dao.get("store_name","Cửa hàng của tôi")); vat.setText(dao.get("vat_percent","0")); findViewById(R.id.btnBack).setOnClickListener(v->finish()); findViewById(R.id.btnSaveSettings).setOnClickListener(v->{String storeName=store.getText().toString().trim();int vatValue;try{vatValue=Integer.parseInt(vat.getText().toString().trim());}catch(Exception e){Toast.makeText(this,"VAT phải là số từ 0 đến 100",Toast.LENGTH_SHORT).show();return;}if(storeName.isEmpty()||vatValue<0||vatValue>100){Toast.makeText(this,"Kiểm tra lại tên cửa hàng và VAT",Toast.LENGTH_SHORT).show();return;}dao.put("store_name",storeName);dao.put("vat_percent",String.valueOf(vatValue));Toast.makeText(this,"Đã lưu cấu hình",Toast.LENGTH_SHORT).show();}); }
}

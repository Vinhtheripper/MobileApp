package com.example.mpos.auth;

import android.content.Intent;
import android.os.Bundle;
import android.widget.EditText;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.mpos.MainActivity;
import com.example.mpos.R;
import com.example.mpos.dao.UserDao;
import com.example.mpos.database.DatabaseHelper;
import com.example.mpos.model.User;

public class LoginActivity extends AppCompatActivity {
    @Override public void onCreate(Bundle state) { super.onCreate(state); setContentView(R.layout.activity_login);
        EditText username=findViewById(R.id.inputUsername), password=findViewById(R.id.inputPassword);
        findViewById(R.id.btnLogin).setOnClickListener(v -> { User user=new UserDao(new DatabaseHelper(this)).login(username.getText().toString(), password.getText().toString()); if(user==null){ Toast.makeText(this,"Sai tài khoản hoặc mật khẩu",Toast.LENGTH_SHORT).show(); return; } new SessionManager(this).save(user); startActivity(new Intent(this, MainActivity.class)); finish(); });
    }
}

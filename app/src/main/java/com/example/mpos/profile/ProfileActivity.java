package com.example.mpos.profile;

import android.content.Intent;
import android.os.Bundle;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.example.mpos.R;
import com.example.mpos.auth.LoginActivity;
import com.example.mpos.auth.SessionManager;
import com.example.mpos.settings.SettingsActivity;

public class ProfileActivity extends AppCompatActivity {
    @Override public void onCreate(Bundle state) { super.onCreate(state); setContentView(R.layout.activity_profile); SessionManager session=new SessionManager(this); TextView info=findViewById(R.id.txtProfileInfo); info.setText(session.getUser().username+"\nVai trò: "+session.getUser().role); findViewById(R.id.btnBack).setOnClickListener(v->finish()); findViewById(R.id.btnSettings).setOnClickListener(v->startActivity(new Intent(this, SettingsActivity.class))); findViewById(R.id.btnProfileLogout).setOnClickListener(v->{session.clear();startActivity(new Intent(this, LoginActivity.class));finishAffinity();}); }
}

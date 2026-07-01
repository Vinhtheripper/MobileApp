package com.example.mpos.employee;

import android.content.Intent;
import android.os.Bundle;
import android.widget.ArrayAdapter;
import android.widget.ListView;

import androidx.appcompat.app.AppCompatActivity;

import com.example.mpos.R;
import com.example.mpos.auth.PermissionHelper;
import com.example.mpos.dao.UserDao;
import com.example.mpos.database.DatabaseHelper;
import com.example.mpos.model.User;

import java.util.ArrayList;
import java.util.List;

public class UserListActivity extends AppCompatActivity {
    private final List<User> users = new ArrayList<>();
    private ArrayAdapter<String> adapter;
    private UserDao dao;

    @Override public void onCreate(Bundle state) {
        super.onCreate(state);
        if (!PermissionHelper.requireAdmin(this)) return;
        setContentView(R.layout.activity_user_list);
        dao = new UserDao(new DatabaseHelper(this));
        adapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, new ArrayList<>());
        ((ListView) findViewById(R.id.listUsers)).setAdapter(adapter);
        findViewById(R.id.btnBack).setOnClickListener(v -> finish());
        findViewById(R.id.btnAddUser).setOnClickListener(v -> startActivity(new Intent(this, UserFormActivity.class)));
        ((ListView) findViewById(R.id.listUsers)).setOnItemClickListener((p, v, position, id) -> {
            Intent intent = new Intent(this, UserFormActivity.class);
            intent.putExtra("user_id", users.get(position).id);
            startActivity(intent);
        });
    }

    @Override protected void onResume() {
        super.onResume();
        if (dao != null) load();
    }

    private void load() {
        users.clear();
        users.addAll(dao.listAll());
        List<String> rows = new ArrayList<>();
        for (User user : users) {
            rows.add((user.fullName == null ? user.username : user.fullName) + " • " + user.role + "\n@" + user.username + " • " + (user.active ? "Đang hoạt động" : "Đã khóa"));
        }
        adapter.clear();
        adapter.addAll(rows);
        adapter.notifyDataSetChanged();
    }
}

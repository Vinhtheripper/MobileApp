package com.example.mpos.shift;

import android.os.Bundle;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import com.example.mpos.R;
import com.example.mpos.auth.SessionManager;
import com.example.mpos.dao.ShiftDao;
import com.example.mpos.database.DatabaseHelper;

public class ShiftActivity extends AppCompatActivity {
    private ShiftDao dao; private long userId; private TextView status; private EditText cash, note;
    @Override public void onCreate(Bundle state){super.onCreate(state);setContentView(R.layout.activity_shift);findViewById(R.id.btnBack).setOnClickListener(v->finish());dao=new ShiftDao(new DatabaseHelper(this));userId=new SessionManager(this).getUser().id;status=findViewById(R.id.txtShiftStatus);cash=findViewById(R.id.inputShiftCash);note=findViewById(R.id.inputHandoverNote);findViewById(R.id.btnOpenShift).setOnClickListener(v->{if(dao.getOpenShiftId(userId)>0){toast("Bạn đang có ca mở");return;}dao.open(userId,money());refresh();});findViewById(R.id.btnCloseShift).setOnClickListener(v->{long id=dao.getOpenShiftId(userId);if(id<0){toast("Chưa có ca mở");return;}dao.close(id,money(),note.getText().toString());refresh();});refresh();}
    private void refresh(){status.setText(dao.getOpenShiftId(userId)>0?"Trạng thái: Đang mở ca":"Trạng thái: Chưa mở ca");}
    private long money(){try{return Long.parseLong(cash.getText().toString());}catch(Exception e){return 0;}}
    private void toast(String s){Toast.makeText(this,s,Toast.LENGTH_SHORT).show();}
}

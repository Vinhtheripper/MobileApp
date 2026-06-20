package com.example.mpos.sync;

import android.database.Cursor;
import android.os.Bundle;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;
import com.example.mpos.R;
import com.example.mpos.database.DatabaseHelper;

public class SyncStatusActivity extends AppCompatActivity {
    private DatabaseHelper helper;
    @Override public void onCreate(Bundle state){super.onCreate(state);setContentView(R.layout.activity_sync_status);findViewById(R.id.btnBack).setOnClickListener(v -> finish());helper=new DatabaseHelper(this);findViewById(R.id.btnSyncNow).setOnClickListener(v->{new SyncQueueProcessor(helper).processPending();showStatus();});showStatus();}
    private void showStatus(){Cursor c=helper.getReadableDatabase().rawQuery("SELECT status,COUNT(*) FROM sync_queue GROUP BY status",null);StringBuilder s=new StringBuilder("Trạng thái đồng bộ local\n");try{while(c.moveToNext())s.append(c.getString(0)).append(": ").append(c.getInt(1)).append('\n');}finally{c.close();}((TextView)findViewById(R.id.txtSyncStatus)).setText(s);}
}

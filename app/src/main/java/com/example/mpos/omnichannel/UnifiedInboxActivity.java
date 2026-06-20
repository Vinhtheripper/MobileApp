package com.example.mpos.omnichannel;

import android.database.Cursor;
import android.os.Bundle;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import com.example.mpos.R;
import com.example.mpos.database.DatabaseHelper;

/** Local unified inbox. Remote channel webhooks plug into sync_queue later. */
public class UnifiedInboxActivity extends AppCompatActivity {
    @Override public void onCreate(Bundle state) { super.onCreate(state); setContentView(R.layout.activity_unified_inbox); findViewById(R.id.btnBack).setOnClickListener(v -> finish()); Cursor c=new DatabaseHelper(this).getReadableDatabase().rawQuery("SELECT order_code,channel,status,total_amount FROM orders ORDER BY created_at DESC LIMIT 30",null); StringBuilder text=new StringBuilder("Unified Order Inbox\n\n"); try { while(c.moveToNext()) text.append('[').append(c.getString(1)).append("] ").append(c.getString(0)).append(" • ").append(c.getString(2)).append(" • ").append(c.getLong(3)).append(" ₫\n"); } finally {c.close();} if(text.toString().endsWith("\n\n")) text.append("Chưa có đơn. Đơn walk-in và đơn kênh sau này sẽ tập trung tại đây."); ((TextView)findViewById(R.id.txtInbox)).setText(text); }
}

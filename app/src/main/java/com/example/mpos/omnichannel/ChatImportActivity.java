package com.example.mpos.omnichannel;

import android.content.ContentValues;
import android.content.Intent;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.Bundle;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.mpos.R;
import com.example.mpos.auth.SessionManager;
import com.example.mpos.cart.CartManager;
import com.example.mpos.constants.OrderConstants;
import com.example.mpos.constants.SyncConstants;
import com.example.mpos.dao.ProductDao;
import com.example.mpos.database.DatabaseHelper;
import com.example.mpos.model.Product;
import com.example.mpos.pos.PosActivity;
import com.example.mpos.service.ChatOrderParser;

import java.util.List;

public class ChatImportActivity extends AppCompatActivity {
    private final ChatOrderParser parser = new ChatOrderParser();
    private ChatOrderParser.ParsedChatOrder parsed;

    @Override public void onCreate(Bundle state) {
        super.onCreate(state);
        setContentView(R.layout.activity_chat_import);
        findViewById(R.id.btnBack).setOnClickListener(v -> finish());
        findViewById(R.id.btnParseChat).setOnClickListener(v -> preview());
        findViewById(R.id.btnCreateDraftOrder).setOnClickListener(v -> createDraft());
    }

    private void preview() {
        parsed = parser.parse(((EditText) findViewById(R.id.inputChatOrder)).getText().toString());
        ((TextView) findViewById(R.id.txtParsedPreview)).setText(
                "Tên: " + value(parsed.customerName) +
                "\nSĐT: " + value(parsed.phone) +
                "\nĐịa chỉ: " + value(parsed.address) +
                "\nSản phẩm: " + value(parsed.productKeyword) +
                "\nSố lượng: " + parsed.quantity
        );
    }

    private void createDraft() {
        if (parsed == null) preview();
        DatabaseHelper helper = new DatabaseHelper(this);
        SQLiteDatabase db = helper.getWritableDatabase();
        db.beginTransaction();
        try {
            long now = System.currentTimeMillis();
            long customerId = findOrCreateCustomer(db, now);
            ContentValues order = new ContentValues();
            order.put("order_code", "DRAFT-" + now);
            if (customerId > 0) order.put("customer_id", customerId);
            order.put("user_id", new SessionManager(this).getUser().id);
            order.put("channel", OrderConstants.SOURCE_TIKTOK_CHAT);
            order.put("status", OrderConstants.STATUS_DRAFT);
            order.put("subtotal", 0);
            order.put("total_amount", 0);
            order.put("delivery_address", parsed.address);
            order.put("sync_status", SyncConstants.STATUS_PENDING);
            order.put("note", "Chat import: " + ((EditText) findViewById(R.id.inputChatOrder)).getText());
            order.put("created_at", now);
            order.put("updated_at", now);
            long orderId = db.insertOrThrow("orders", null, order);
            ContentValues sync = new ContentValues();
            sync.put("event_type", SyncConstants.EVENT_ORDER_CREATE);
            sync.put("entity_type", "ORDER");
            sync.put("entity_id", orderId);
            sync.put("action_type", "DRAFT_FROM_CHAT");
            sync.put("status", SyncConstants.STATUS_PENDING);
            sync.put("created_at", now);
            db.insertOrThrow("sync_queue", null, sync);
            db.setTransactionSuccessful();
        } finally {
            db.endTransaction();
        }
        addParsedProductToCart(helper);
        Toast.makeText(this, "Đã tạo đơn nháp từ chat", Toast.LENGTH_SHORT).show();
        startActivity(new Intent(this, PosActivity.class));
    }

    private long findOrCreateCustomer(SQLiteDatabase db, long now) {
        if (parsed.phone == null || parsed.phone.isEmpty()) return -1;
        Cursor cursor = db.rawQuery("SELECT id FROM customers WHERE phone=?", new String[]{parsed.phone});
        try { if (cursor.moveToFirst()) return cursor.getLong(0); } finally { cursor.close(); }
        ContentValues customer = new ContentValues();
        customer.put("phone", parsed.phone);
        customer.put("full_name", parsed.customerName);
        customer.put("address", parsed.address);
        customer.put("source_tags", OrderConstants.SOURCE_TIKTOK_CHAT);
        customer.put("created_at", now);
        customer.put("updated_at", now);
        return db.insertOrThrow("customers", null, customer);
    }

    private void addParsedProductToCart(DatabaseHelper helper) {
        if (parsed.productKeyword == null || parsed.productKeyword.isEmpty()) return;
        List<Product> matches = new ProductDao(helper).search(parsed.productKeyword);
        if (matches.isEmpty()) return;
        CartManager cart = CartManager.get();
        for (int i = 0; i < parsed.quantity; i++) {
            if (!cart.add(matches.get(0))) break;
        }
    }

    private String value(String value) {
        return value == null || value.trim().isEmpty() ? "Chưa rõ" : value.trim();
    }
}

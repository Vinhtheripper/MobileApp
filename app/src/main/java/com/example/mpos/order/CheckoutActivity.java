package com.example.mpos.order;

import android.content.ContentValues;
import android.content.Intent;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.mpos.R;
import com.example.mpos.auth.SessionManager;
import com.example.mpos.cart.CartManager;
import com.example.mpos.dao.SettingsDao;
import com.example.mpos.dao.ShiftDao;
import com.example.mpos.database.DatabaseHelper;
import com.example.mpos.receipt.ReceiptActivity;
import com.example.mpos.utils.CurrencyUtils;

import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;

public class CheckoutActivity extends AppCompatActivity {

    private static final String METHOD_CASH   = "CASH";
    private static final String METHOD_VIETQR = "VIETQR";
    private static final String METHOD_CARD   = "CARD";
    private static final String METHOD_WALLET = "EWALLET";

    private String selectedMethod  = METHOD_CASH;
    private String selectedWallet  = "MOMO";
    private String selectedChannel = "WALK_IN";

    private LinearLayout cardCash, cardVietQR, cardBank, cardWallet;
    private TextView txtCashRadio, txtVietQRRadio, txtBankRadio, txtWalletRadio;
    private LinearLayout layoutCashInput, layoutQR, layoutWalletOptions;
    private LinearLayout btnWalletMomo, btnWalletZalo, btnWalletViettel;
    private EditText inputReceived;
    private TextView txtChange, txtQRAmount, txtQRBankInfo;
    private ImageView imgVietQR;
    private TextView txtLoyaltyPoints, txtLoyaltyDiscount;
    private LinearLayout layoutLoyalty, layoutRedeemRow;
    private TextView btnToggleRedeem;

    private long totalAmount;
    private long customerId = -1;
    private long loyaltyPoints = 0;
    private boolean redeemActive = false;
    private long redeemDiscount = 0;
    private DatabaseHelper db;
    private SettingsDao settingsDao;

    @Override
    public void onCreate(Bundle state) {
        super.onCreate(state);
        setContentView(R.layout.activity_checkout);

        db          = new DatabaseHelper(this);
        settingsDao = new SettingsDao(db);
        totalAmount = CartManager.get().total();

        // Views
        TextView txtTotal   = findViewById(R.id.txtTotal);
        TextView txtSummary = findViewById(R.id.txtSummaryItems);
        cardCash     = findViewById(R.id.cardCash);
        cardVietQR   = findViewById(R.id.cardVietQR);
        cardBank     = findViewById(R.id.cardBank);
        cardWallet   = findViewById(R.id.cardWallet);
        txtCashRadio   = findViewById(R.id.txtCashRadio);
        txtVietQRRadio = findViewById(R.id.txtVietQRRadio);
        txtBankRadio   = findViewById(R.id.txtBankRadio);
        txtWalletRadio = findViewById(R.id.txtWalletRadio);
        layoutCashInput     = findViewById(R.id.layoutCashInput);
        layoutQR            = findViewById(R.id.layoutQR);
        layoutWalletOptions = findViewById(R.id.layoutWalletOptions);
        btnWalletMomo    = findViewById(R.id.btnWalletMomo);
        btnWalletZalo    = findViewById(R.id.btnWalletZalo);
        btnWalletViettel = findViewById(R.id.btnWalletViettel);
        inputReceived = findViewById(R.id.inputReceived);
        txtChange     = findViewById(R.id.txtChange);
        txtQRAmount   = findViewById(R.id.txtQRAmount);
        txtQRBankInfo = findViewById(R.id.txtQRBankInfo);
        imgVietQR     = findViewById(R.id.imgVietQR);
        layoutLoyalty    = findViewById(R.id.layoutLoyalty);
        layoutRedeemRow  = findViewById(R.id.layoutRedeemRow);
        txtLoyaltyPoints  = findViewById(R.id.txtLoyaltyPoints);
        txtLoyaltyDiscount = findViewById(R.id.txtLoyaltyDiscount);
        btnToggleRedeem  = findViewById(R.id.btnToggleRedeem);

        // Channel chips
        TextView chipWalkIn = findViewById(R.id.chipWalkIn);
        TextView chipSocial = findViewById(R.id.chipSocial);
        if (chipWalkIn != null && chipSocial != null) {
            chipWalkIn.setOnClickListener(v -> selectChannel("WALK_IN", chipWalkIn, chipSocial));
            chipSocial.setOnClickListener(v -> selectChannel("ORDER",   chipWalkIn, chipSocial));
        }

        txtTotal.setText(CurrencyUtils.vnd(totalAmount));
        txtQRAmount.setText(CurrencyUtils.vnd(totalAmount));
        int itemCount = CartManager.get().getCount();
        txtSummary.setText(itemCount + " sản phẩm · Thuế " + settingsDao.get("vat_percent", "0") + "%");

        Intent incoming = getIntent();
        if (incoming.hasExtra("customer_phone")) {
            EditText phoneField = findViewById(R.id.inputCustomerPhone);
            if (phoneField != null) {
                phoneField.setText(incoming.getStringExtra("customer_phone"));
                lookupCustomerLoyalty(incoming.getStringExtra("customer_phone"));
            }
        }

        selectMethod(METHOD_CASH);

        cardCash.setOnClickListener(v -> selectMethod(METHOD_CASH));
        cardVietQR.setOnClickListener(v -> selectMethod(METHOD_VIETQR));
        cardBank.setOnClickListener(v -> selectMethod(METHOD_CARD));
        cardWallet.setOnClickListener(v -> selectMethod(METHOD_WALLET));

        setupWalletButtons();
        setupCashPresets();

        inputReceived.addTextChangedListener(new TextWatcher() {
            public void afterTextChanged(Editable s) { updateChange(); }
            public void beforeTextChanged(CharSequence s, int a, int b, int c) {}
            public void onTextChanged(CharSequence s, int a, int b, int c) {}
        });

        EditText phoneField = findViewById(R.id.inputCustomerPhone);
        phoneField.addTextChangedListener(new TextWatcher() {
            public void afterTextChanged(Editable s) {
                if (s.length() >= 9) lookupCustomerLoyalty(s.toString().trim());
                else { layoutLoyalty.setVisibility(View.GONE); customerId = -1; loyaltyPoints = 0; resetRedeem(); }
            }
            public void beforeTextChanged(CharSequence s, int a, int b, int c) {}
            public void onTextChanged(CharSequence s, int a, int b, int c) {}
        });

        btnToggleRedeem.setOnClickListener(v -> toggleRedeem());

        findViewById(R.id.btnBack).setOnClickListener(v -> finish());
        findViewById(R.id.btnConfirmPayment).setOnClickListener(this::handleConfirm);
    }

    // ─── Channel toggle ───────────────────────────────────────────────────────

    private void selectChannel(String channel, TextView chipWalkIn, TextView chipSocial) {
        selectedChannel = channel;
        boolean isWalkIn = "WALK_IN".equals(channel);
        chipWalkIn.setTextColor(isWalkIn ? 0xFFFFFFFF : 0xFF64748B);
        chipWalkIn.setBackgroundResource(isWalkIn ? R.drawable.bg_chip_selected : android.R.color.transparent);
        chipSocial.setTextColor(isWalkIn ? 0xFF64748B : 0xFFFFFFFF);
        chipSocial.setBackgroundResource(isWalkIn ? android.R.color.transparent : R.drawable.bg_chip_selected);
    }

    // ─── Wallet sub-options ───────────────────────────────────────────────────

    private void setupWalletButtons() {
        setWalletSelected(btnWalletMomo, "MOMO");
        btnWalletMomo.setOnClickListener(v -> { selectedWallet = "MOMO";    updateWalletSelection(); });
        btnWalletZalo.setOnClickListener(v -> { selectedWallet = "ZALOPAY"; updateWalletSelection(); });
        btnWalletViettel.setOnClickListener(v -> { selectedWallet = "VIETTELMONEY"; updateWalletSelection(); });
    }

    private void setupCashPresets() {
        TextView btnExact = findViewById(R.id.btnCashExact);
        TextView btn100   = findViewById(R.id.btnCash100);
        TextView btn200   = findViewById(R.id.btnCash200);
        TextView btn500   = findViewById(R.id.btnCash500);
        if (btnExact != null) btnExact.setOnClickListener(v -> {
            inputReceived.setText(String.valueOf(effectiveTotal()));
            updateChange();
        });
        if (btn100 != null) btn100.setOnClickListener(v -> {
            inputReceived.setText(String.valueOf(roundUpToPreset(effectiveTotal(), 100_000L)));
            updateChange();
        });
        if (btn200 != null) btn200.setOnClickListener(v -> {
            inputReceived.setText(String.valueOf(roundUpToPreset(effectiveTotal(), 200_000L)));
            updateChange();
        });
        if (btn500 != null) btn500.setOnClickListener(v -> {
            inputReceived.setText(String.valueOf(roundUpToPreset(effectiveTotal(), 500_000L)));
            updateChange();
        });
    }

    private long roundUpToPreset(long total, long preset) {
        if (total <= preset) return preset;
        return ((total / preset) + 1) * preset;
    }

    private void updateWalletSelection() {
        setWalletSelected(btnWalletMomo,    "MOMO");
        setWalletSelected(btnWalletZalo,    "ZALOPAY");
        setWalletSelected(btnWalletViettel, "VIETTELMONEY");
    }

    private void setWalletSelected(LinearLayout btn, String wallet) {
        boolean sel = selectedWallet.equals(wallet);
        btn.setBackground(sel
            ? makeRoundedBg(0xFF2875FB, 10)
            : makeRoundedBg(0xFFF1F5F9, 10));
        TextView tv = (TextView) btn.getChildAt(0);
        if (tv != null) tv.setTextColor(sel ? 0xFFFFFFFF : 0xFF1C2333);
    }

    private android.graphics.drawable.GradientDrawable makeRoundedBg(int color, int r) {
        android.graphics.drawable.GradientDrawable g = new android.graphics.drawable.GradientDrawable();
        g.setColor(color);
        g.setCornerRadius(r * getResources().getDisplayMetrics().density);
        return g;
    }

    // ─── Loyalty points ───────────────────────────────────────────────────────

    private void lookupCustomerLoyalty(String phone) {
        if (phone == null || phone.length() < 9) return;
        long shopId = new SessionManager(this).getShopId();
        Cursor c = db.getReadableDatabase().rawQuery(
            "SELECT id, loyalty_points FROM customers WHERE phone=? AND shop_id=?",
            new String[]{phone, String.valueOf(shopId)});
        try {
            if (c.moveToFirst()) {
                customerId    = c.getLong(0);
                loyaltyPoints = c.getLong(1);
                txtLoyaltyPoints.setText("Điểm tích lũy: " + loyaltyPoints + " điểm");
                long redeemPer = Long.parseLong(settingsDao.get("loyalty_redeem_rate", "100"));
                long maxDisc = (loyaltyPoints / redeemPer) * 1000;
                txtLoyaltyDiscount.setText("Quy đổi tối đa: " + CurrencyUtils.vnd(maxDisc));
                layoutLoyalty.setVisibility(View.VISIBLE);
            } else {
                layoutLoyalty.setVisibility(View.GONE);
                customerId = -1; loyaltyPoints = 0; resetRedeem();
            }
        } finally { c.close(); }
    }

    private void toggleRedeem() {
        if (redeemActive) {
            resetRedeem();
        } else {
            long redeemPer = Long.parseLong(settingsDao.get("loyalty_redeem_rate", "100"));
            long maxPts    = Math.min(loyaltyPoints, (totalAmount / 1000) * redeemPer);
            redeemDiscount = (maxPts / redeemPer) * 1000;
            if (redeemDiscount <= 0) {
                Toast.makeText(this, "Không đủ điểm để quy đổi", Toast.LENGTH_SHORT).show(); return;
            }
            redeemActive = true;
            btnToggleRedeem.setText("Bỏ quy đổi");
            btnToggleRedeem.setTextColor(0xFFEF4444);
            layoutRedeemRow.setVisibility(View.VISIBLE);
            TextView tvRedeemAmt = findViewById(R.id.txtRedeemAmount);
            if (tvRedeemAmt != null) tvRedeemAmt.setText("−" + CurrencyUtils.vnd(redeemDiscount));
            updateDisplayTotal();
        }
    }

    private void resetRedeem() {
        redeemActive = false; redeemDiscount = 0;
        btnToggleRedeem.setText("Dùng điểm");
        btnToggleRedeem.setTextColor(0xFF2875FB);
        layoutRedeemRow.setVisibility(View.GONE);
        updateDisplayTotal();
    }

    private void updateDisplayTotal() {
        long effective = totalAmount - redeemDiscount;
        TextView txtTotal = findViewById(R.id.txtTotal);
        if (txtTotal != null) txtTotal.setText(CurrencyUtils.vnd(effective));
        txtQRAmount.setText(CurrencyUtils.vnd(effective));
    }

    private long effectiveTotal() { return totalAmount - redeemDiscount; }

    // ─── Method selection ─────────────────────────────────────────────────────

    private void selectMethod(String method) {
        selectedMethod = method;
        setCardSelected(cardCash,   txtCashRadio,   false);
        setCardSelected(cardVietQR, txtVietQRRadio, false);
        setCardSelected(cardBank,   txtBankRadio,   false);
        setCardSelected(cardWallet, txtWalletRadio, false);
        layoutCashInput.setVisibility(View.GONE);
        layoutQR.setVisibility(View.GONE);
        layoutWalletOptions.setVisibility(View.GONE);

        switch (method) {
            case METHOD_CASH:
                setCardSelected(cardCash, txtCashRadio, true);
                layoutCashInput.setVisibility(View.VISIBLE);
                updateChange();
                break;
            case METHOD_VIETQR:
                setCardSelected(cardVietQR, txtVietQRRadio, true);
                layoutQR.setVisibility(View.VISIBLE);
                loadVietQR();
                break;
            case METHOD_CARD:
                setCardSelected(cardBank, txtBankRadio, true);
                break;
            case METHOD_WALLET:
                setCardSelected(cardWallet, txtWalletRadio, true);
                layoutWalletOptions.setVisibility(View.VISIBLE);
                break;
        }
    }

    private void setCardSelected(LinearLayout card, TextView radio, boolean selected) {
        card.setBackgroundResource(selected ? R.drawable.card_selected : R.drawable.card_surface);
        radio.setText(selected ? "●" : "○");
        radio.setTextColor(getResources().getColor(selected ? R.color.blue_primary : R.color.text_secondary));
    }

    private void updateChange() {
        long received = parseMoney(inputReceived.getText().toString());
        long change   = received - effectiveTotal();
        txtChange.setText(CurrencyUtils.vnd(Math.max(change, 0)));
        txtChange.setTextColor(getResources().getColor(change >= 0 ? R.color.status_success : R.color.status_error));
    }

    // ─── VietQR ───────────────────────────────────────────────────────────────

    private void loadVietQR() {
        String bankCode = settingsDao.get("bank_code", "");
        String account  = settingsDao.get("bank_account", "");
        String name     = settingsDao.get("bank_name", "");

        if (bankCode.isEmpty() || account.isEmpty()) {
            imgVietQR.setVisibility(View.GONE);
            txtQRBankInfo.setText("Chưa cấu hình tài khoản ngân hàng.\nVào Cài đặt → VietQR để nhập số TK.");
            return;
        }

        txtQRBankInfo.setText(bankCode + " — " + name + "\nSố TK: " + account);
        imgVietQR.setImageResource(android.R.drawable.ic_menu_gallery);

        long amt = effectiveTotal();
        String orderInfo;
        try { orderInfo = URLEncoder.encode("Thanh toan don hang", "UTF-8"); }
        catch (Exception e) { orderInfo = "Thanh+toan"; }
        String encodedName;
        try { encodedName = URLEncoder.encode(name, "UTF-8"); }
        catch (Exception e) { encodedName = name.replace(" ", "+"); }

        String qrUrl = "https://img.vietqr.io/image/" + bankCode + "-" + account
            + "-compact2.png?amount=" + amt
            + "&addInfo=" + orderInfo
            + "&accountName=" + encodedName;

        new Thread(() -> {
            try {
                URL url = new URL(qrUrl);
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setConnectTimeout(10000);
                conn.setReadTimeout(10000);
                InputStream is = conn.getInputStream();
                Bitmap bmp = BitmapFactory.decodeStream(is);
                is.close();
                if (bmp != null) {
                    runOnUiThread(() -> {
                        imgVietQR.setImageBitmap(bmp);
                        imgVietQR.setVisibility(View.VISIBLE);
                    });
                }
            } catch (Exception ignored) {
                runOnUiThread(() -> {
                    imgVietQR.setVisibility(View.GONE);
                    txtQRBankInfo.setText(txtQRBankInfo.getText() + "\n\n(Không tải được mã QR — kiểm tra kết nối)");
                });
            }
        }).start();
    }

    // ─── Wallet deep links ────────────────────────────────────────────────────

    private void openWalletApp() {
        String pkg;
        switch (selectedWallet) {
            case "ZALOPAY":     pkg = "com.zing.zalo"; break;
            case "VIETTELMONEY": pkg = "com.viettel.pay"; break;
            default:            pkg = "com.mservice.momotransfer"; break;
        }
        Intent intent = getPackageManager().getLaunchIntentForPackage(pkg);
        if (intent != null) startActivity(intent);
        else Toast.makeText(this, "Chưa cài app " + selectedWallet, Toast.LENGTH_SHORT).show();
    }

    // ─── Confirm / checkout ───────────────────────────────────────────────────

    private void handleConfirm(View btnView) {
        if (CartManager.get().isEmpty()) {
            Toast.makeText(this, "Giỏ hàng đang trống", Toast.LENGTH_SHORT).show(); return;
        }
        if (METHOD_CASH.equals(selectedMethod)) {
            long received = parseMoney(inputReceived.getText().toString());
            if (received < effectiveTotal()) {
                Toast.makeText(this, "Tiền khách đưa chưa đủ (cần " + CurrencyUtils.vnd(effectiveTotal()) + ")", Toast.LENGTH_LONG).show(); return;
            }
        }
        if (METHOD_WALLET.equals(selectedMethod)) {
            openWalletApp();
        }

        SessionManager session = new SessionManager(this);
        long shiftId = new ShiftDao(db, session.getShopId()).getOpenShiftId(session.getUser().id);
        if (shiftId < 0) {
            Toast.makeText(this, "Bạn cần mở ca trước khi thanh toán", Toast.LENGTH_LONG).show(); return;
        }

        btnView.setEnabled(false);
        try {
            String phone    = ((EditText) findViewById(R.id.inputCustomerPhone)).getText().toString().trim();
            EditText nameField = findViewById(R.id.inputCustomerName);
            EditText addrField = findViewById(R.id.inputCustomerAddress);
            String custName = nameField != null ? nameField.getText().toString().trim() : "";
            String custAddr = addrField != null ? addrField.getText().toString().trim() : "";
            long received   = METHOD_CASH.equals(selectedMethod) ? parseMoney(inputReceived.getText().toString()) : effectiveTotal();
            String method   = METHOD_WALLET.equals(selectedMethod) ? selectedWallet : selectedMethod;
            long orderId    = new CheckoutService(this, db, session.getShopId())
                .checkoutWithDiscount(session.getUser().id, shiftId, phone, custName, custAddr, selectedChannel, method, received, redeemDiscount);

            // Award loyalty points
            if (customerId > 0) {
                long earnPer    = Long.parseLong(settingsDao.get("loyalty_earn_rate", "1000"));
                long pointsEarned = effectiveTotal() / earnPer;
                long pointsUsed   = redeemDiscount > 0
                    ? (redeemDiscount / 1000) * Long.parseLong(settingsDao.get("loyalty_redeem_rate", "100"))
                    : 0;
                long newPoints = loyaltyPoints + pointsEarned - pointsUsed;
                ContentValues cv = new ContentValues();
                cv.put("loyalty_points", Math.max(newPoints, 0));
                cv.put("updated_at",     System.currentTimeMillis());
                db.getWritableDatabase().update("customers", cv, "id=?", new String[]{String.valueOf(customerId)});
            }

            Intent intent = new Intent(this, ReceiptActivity.class);
            intent.putExtra("order_id",       orderId);
            intent.putExtra("payment_method", method);
            intent.putExtra("received_cash",  received);
            intent.putExtra("order_total",    effectiveTotal());
            startActivity(intent);
            finish();
        } catch (Exception e) {
            btnView.setEnabled(true);
            Toast.makeText(this, e.getMessage(), Toast.LENGTH_LONG).show();
        }
    }

    private long parseMoney(String value) {
        try { return Long.parseLong(value.trim().replaceAll("[^0-9]", "")); }
        catch (Exception ignored) { return 0; }
    }
}

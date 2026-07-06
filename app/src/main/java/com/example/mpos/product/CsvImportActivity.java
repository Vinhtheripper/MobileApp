package com.example.mpos.product;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;

import android.content.ContentValues;
import android.database.Cursor;

import com.example.mpos.R;
import com.example.mpos.dao.ProductDao;
import com.example.mpos.database.DatabaseHelper;
import com.example.mpos.model.Product;
import com.example.mpos.product.ProductListActivity;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;

public class CsvImportActivity extends AppCompatActivity {

    private ProductDao dao;
    private DatabaseHelper dbHelper;
    private Uri selectedUri;

    // Parsed results
    private List<Product> validProducts  = new ArrayList<>();
    private List<Product> updateProducts = new ArrayList<>();
    private List<String>  errors         = new ArrayList<>();

    private TextView txtFileName, txtRowCount, txtValidCount, txtUpdateCount, txtErrorCount;
    private TextView txtErrors, txtResult;
    private Button btnImport;
    private LinearLayout cardPreview, cardResult;

    private final ActivityResultLauncher<Intent> csvPicker =
        registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), result -> {
            if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                selectedUri = result.getData().getData();
                if (selectedUri != null) parseCsv(selectedUri);
            }
        });

    @Override
    protected void onCreate(Bundle state) {
        super.onCreate(state);
        setContentView(R.layout.activity_csv_import);

        dbHelper = new DatabaseHelper(this);
        long shopId = new com.example.mpos.auth.SessionManager(this).getShopId();
        dao = new ProductDao(dbHelper, shopId);

        txtFileName   = findViewById(R.id.txtFileName);
        txtRowCount   = findViewById(R.id.txtRowCount);
        txtValidCount = findViewById(R.id.txtValidCount);
        txtUpdateCount= findViewById(R.id.txtUpdateCount);
        txtErrorCount = findViewById(R.id.txtErrorCount);
        txtErrors     = findViewById(R.id.txtErrors);
        txtResult     = findViewById(R.id.txtResult);
        btnImport     = (Button) findViewById(R.id.btnImport);
        cardPreview   = findViewById(R.id.cardPreview);
        cardResult    = findViewById(R.id.cardResult);

        findViewById(R.id.btnBack).setOnClickListener(v -> finish());
        findViewById(R.id.btnPickFile).setOnClickListener(v -> openFilePicker());
        findViewById(R.id.layoutDropZone).setOnClickListener(v -> openFilePicker());
        btnImport.setOnClickListener(v -> doImport());

        // Download template
        findViewById(R.id.btnDownloadTemplate).setOnClickListener(v -> saveSampleCsv());
    }

    private void openFilePicker() {
        Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
        intent.setType("*/*");
        intent.putExtra(Intent.EXTRA_MIME_TYPES, new String[]{"text/csv", "text/comma-separated-values", "application/csv", "text/plain"});
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        csvPicker.launch(Intent.createChooser(intent, "Chọn file CSV"));
    }

    private void parseCsv(Uri uri) {
        validProducts.clear();
        updateProducts.clear();
        errors.clear();
        cardResult.setVisibility(View.GONE);

        // Get file name for display
        String name = uri.getLastPathSegment();
        if (name != null && name.contains("/")) name = name.substring(name.lastIndexOf('/') + 1);
        txtFileName.setText(name != null ? name : "file.csv");

        try (InputStream in = getContentResolver().openInputStream(uri);
             BufferedReader reader = new BufferedReader(new InputStreamReader(in, "UTF-8"))) {

            String line;
            int lineNum = 0;
            int total = 0;

            while ((line = reader.readLine()) != null) {
                lineNum++;
                if (lineNum == 1) continue; // skip header

                line = line.trim();
                if (line.isEmpty()) continue;
                total++;

                String[] cols = splitCsvLine(line);

                // Expect: name, sku, barcode, sale_price, stock_quantity, min_stock_quantity[, category, image_url]
                if (cols.length < 2) {
                    errors.add("Dòng " + lineNum + ": thiếu cột (cần ít nhất name, sku)");
                    continue;
                }

                String pName = col(cols, 0);
                String sku   = col(cols, 1);

                if (pName.isEmpty()) {
                    errors.add("Dòng " + lineNum + ": tên sản phẩm trống");
                    continue;
                }
                if (sku.isEmpty()) {
                    errors.add("Dòng " + lineNum + ": SKU trống");
                    continue;
                }

                long price     = parseLong(col(cols, 3));
                int stock      = (int) parseLong(col(cols, 4));
                int minStk     = (int) parseLong(col(cols, 5));
                String catName = col(cols, 6);
                String imgUrl  = col(cols, 7);

                Product p = new Product();
                p.name             = pName;
                p.sku              = sku;
                p.barcode          = col(cols, 2);
                p.salePrice        = price;
                p.stockQuantity    = stock;
                p.minStockQuantity = minStk;
                if (!catName.isEmpty()) p.categoryId = getOrCreateCategory(catName);
                if (!imgUrl.isEmpty())  p.imageUri   = imgUrl;

                // Check if SKU exists
                Product existing = dao.findBySku(sku);
                if (existing != null) {
                    p.id = existing.id;
                    updateProducts.add(p);
                } else {
                    validProducts.add(p);
                }
            }

            // Show preview
            int totalValid = validProducts.size() + updateProducts.size();
            txtRowCount.setText(totalValid + errors.size() + " dòng dữ liệu");
            txtValidCount.setText(String.valueOf(validProducts.size()));
            txtUpdateCount.setText(String.valueOf(updateProducts.size()));
            txtErrorCount.setText(String.valueOf(errors.size()));

            if (!errors.isEmpty()) {
                StringBuilder sb = new StringBuilder();
                for (String err : errors) sb.append("• ").append(err).append("\n");
                txtErrors.setText(sb.toString().trim());
                txtErrors.setVisibility(View.VISIBLE);
            } else {
                txtErrors.setVisibility(View.GONE);
            }

            cardPreview.setVisibility(View.VISIBLE);

            boolean canImport = !validProducts.isEmpty() || !updateProducts.isEmpty();
            btnImport.setEnabled(canImport);
            btnImport.setAlpha(canImport ? 1f : 0.5f);

        } catch (IOException e) {
            Toast.makeText(this, "Lỗi đọc file: " + e.getMessage(), Toast.LENGTH_LONG).show();
        }
    }

    private void doImport() {
        int created = 0, updated = 0;
        for (Product p : validProducts)  { dao.save(p); created++; }
        for (Product p : updateProducts) { dao.save(p); updated++; }

        String msg = "Nhập thành công: " + created + " sản phẩm mới, " + updated + " cập nhật";
        txtResult.setText(msg);
        cardResult.setVisibility(View.VISIBLE);

        // "View Products" button
        android.widget.Button btnView = cardResult.findViewWithTag("btn_view_products");
        if (btnView == null) {
            btnView = new android.widget.Button(this);
            btnView.setTag("btn_view_products");
            btnView.setText("Xem danh sách sản phẩm →");
            btnView.setBackgroundResource(R.drawable.bg_pos_cat_active);
            btnView.setTextColor(0xFFFFFFFF);
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, dp(46));
            lp.topMargin = dp(12);
            btnView.setLayoutParams(lp);
            final android.widget.Button finalBtn = btnView;
            finalBtn.setOnClickListener(v ->
                startActivity(new Intent(this, ProductListActivity.class)));
            cardResult.addView(btnView);
        }

        btnImport.setEnabled(false);
        btnImport.setAlpha(0.5f);

        validProducts.clear();
        updateProducts.clear();

        Toast.makeText(this, msg, Toast.LENGTH_LONG).show();
    }

    private int dp(int dp) {
        return Math.round(dp * getResources().getDisplayMetrics().density);
    }

    private long getOrCreateCategory(String name) {
        Cursor c = dbHelper.getReadableDatabase().rawQuery(
            "SELECT id FROM categories WHERE name=?", new String[]{name});
        try {
            if (c.moveToFirst()) return c.getLong(0);
        } finally { c.close(); }
        ContentValues cv = new ContentValues();
        cv.put("name", name);
        return dbHelper.getWritableDatabase().insert("categories", null, cv);
    }

    // ─── CSV Utilities ───────────────────────────────────────────────────────

    /** Basic CSV line splitter (handles quoted fields with commas). */
    private String[] splitCsvLine(String line) {
        List<String> result = new ArrayList<>();
        boolean inQuotes = false;
        StringBuilder cur = new StringBuilder();
        for (int i = 0; i < line.length(); i++) {
            char c = line.charAt(i);
            if (c == '"') {
                inQuotes = !inQuotes;
            } else if (c == ',' && !inQuotes) {
                result.add(cur.toString().trim());
                cur.setLength(0);
            } else {
                cur.append(c);
            }
        }
        result.add(cur.toString().trim());
        return result.toArray(new String[0]);
    }

    private String col(String[] cols, int idx) {
        if (idx >= cols.length) return "";
        String v = cols[idx].trim();
        // Remove surrounding quotes
        if (v.startsWith("\"") && v.endsWith("\"") && v.length() > 1)
            v = v.substring(1, v.length() - 1);
        return v;
    }

    private long parseLong(String s) {
        if (s == null || s.isEmpty()) return 0;
        try { return Long.parseLong(s.replaceAll("[^0-9\\-]", "")); }
        catch (Exception e) { return 0; }
    }

    // ─── Sample CSV ──────────────────────────────────────────────────────────

    private void saveSampleCsv() {
        String csv = "name,sku,barcode,sale_price,stock_quantity,min_stock_quantity,category,image_url\n"
            + "Áo thun nam,AT001,8935001234567,150000,50,10,Thời trang,\n"
            + "Quần jean nữ,QJ002,8935007654321,280000,30,5,Thời trang,\n"
            + "Giày sneaker,GS003,,450000,20,3,Giày dép,https://example.com/shoe.jpg\n"
            + "Bánh cookies,BC004,8935009999888,45000,100,20,Bánh kẹo,\n";

        try {
            Intent intent = new Intent(Intent.ACTION_CREATE_DOCUMENT);
            intent.setType("text/csv");
            intent.putExtra(Intent.EXTRA_TITLE, "mpos_import_template.csv");
            // Write via launcher
            sampleSaveLauncher.launch(intent);
            // Store csv for the callback
            pendingSampleCsv = csv;
        } catch (Exception e) {
            Toast.makeText(this, "Thiết bị không hỗ trợ lưu file trực tiếp", Toast.LENGTH_SHORT).show();
        }
    }

    private String pendingSampleCsv;
    private final ActivityResultLauncher<Intent> sampleSaveLauncher =
        registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), result -> {
            if (result.getResultCode() == RESULT_OK && result.getData() != null && pendingSampleCsv != null) {
                Uri destUri = result.getData().getData();
                if (destUri != null) {
                    try (OutputStream out = getContentResolver().openOutputStream(destUri)) {
                        if (out != null) {
                            out.write(pendingSampleCsv.getBytes("UTF-8"));
                            Toast.makeText(this, "Đã lưu file mẫu CSV", Toast.LENGTH_SHORT).show();
                        }
                    } catch (IOException e) {
                        Toast.makeText(this, "Lỗi lưu file: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    }
                }
            }
        });
}

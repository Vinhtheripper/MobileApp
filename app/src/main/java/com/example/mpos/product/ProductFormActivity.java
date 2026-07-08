package com.example.mpos.product;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.MediaStore;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.core.content.FileProvider;

import com.example.mpos.R;
import com.example.mpos.dao.ProductDao;
import com.example.mpos.database.DatabaseHelper;
import com.example.mpos.model.Product;
import com.example.mpos.utils.ImageUtils;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class ProductFormActivity extends AppCompatActivity {

    private static final int REQ_GALLERY_PERMISSION = 101;
    private static final int REQ_CAMERA_PERMISSION  = 102;

    private ProductDao dao;
    private Product product;

    // Views — IDs match activity_product_form.xml
    private EditText etName, etSku, etBarcode, etPrice, etCostPrice, etStock, etMinStock, etDescription;
    private ImageView imgPreview;
    private TextView txtInitials;
    private Uri cameraUri;

    private final ActivityResultLauncher<Intent> galleryLauncher =
        registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), result -> {
            if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                Uri selected = result.getData().getData();
                if (selected != null) {
                    String savedPath = copyToInternal(selected);
                    if (savedPath != null) {
                        product.imageUri = "file://" + savedPath;
                        loadPreview(Uri.parse(product.imageUri));
                    }
                }
            }
        });

    private final ActivityResultLauncher<Intent> cameraLauncher =
        registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), result -> {
            if (result.getResultCode() == RESULT_OK && cameraUri != null) {
                product.imageUri = cameraUri.toString();
                loadPreview(cameraUri);
            }
        });

    @Override
    protected void onCreate(Bundle state) {
        super.onCreate(state);
        setContentView(R.layout.activity_product_form);

        long shopId = new com.example.mpos.auth.SessionManager(this).getShopId();
        dao     = new ProductDao(new DatabaseHelper(this), shopId);
        product = new Product();

        // Bind views using actual layout IDs
        etName     = findViewById(R.id.inputProductName);
        etSku      = findViewById(R.id.inputSku);
        etBarcode  = findViewById(R.id.inputBarcode);
        etPrice    = findViewById(R.id.inputPrice);
        etCostPrice   = findViewById(R.id.inputCostPrice);
        etStock    = findViewById(R.id.inputStock);
        etMinStock = findViewById(R.id.inputMinStock);
        etDescription = findViewById(R.id.inputDescription);
        imgPreview  = findViewById(R.id.imgPreview);
        txtInitials = findViewById(R.id.txtImgInitials);

        // Load existing product if editing
        long productId = getIntent().getLongExtra("product_id", -1);
        if (productId != -1) {
            product = dao.findById(productId);
            if (product == null) product = new Product();
            else populateForm();
        }

        // Image buttons
        View btnRemove = findViewById(R.id.btnRemoveImage);
        if (btnRemove != null) btnRemove.setOnClickListener(v -> removeImage());

        // Gallery / Camera — layout has two separate buttons
        View btnGallery = findViewById(R.id.btnPickGallery);
        View btnCamera  = findViewById(R.id.btnPickCamera);
        if (btnGallery != null) btnGallery.setOnClickListener(v -> checkGalleryPermission());
        if (btnCamera  != null) btnCamera.setOnClickListener(v -> checkCameraPermission());

        // Save
        findViewById(R.id.btnSaveProduct).setOnClickListener(v -> saveProduct());

        // Delete/Archive
        View btnArchive = findViewById(R.id.btnArchiveProduct);
        if (btnArchive != null) {
            if (product.id > 0) {
                btnArchive.setVisibility(View.VISIBLE);
                btnArchive.setOnClickListener(v -> confirmDelete());
            } else {
                btnArchive.setVisibility(View.GONE);
            }
        }

        findViewById(R.id.btnBack).setOnClickListener(v -> finish());
    }

    private void populateForm() {
        if (product == null) return;
        etName.setText(product.name);
        if (etSku      != null) etSku.setText(product.sku);
        if (etBarcode  != null) etBarcode.setText(product.barcode);
        if (etPrice    != null) etPrice.setText(product.salePrice > 0 ? String.valueOf(product.salePrice) : "");
        if (etCostPrice  != null) etCostPrice.setText(product.costPrice > 0 ? String.valueOf(product.costPrice) : "");
        if (etStock    != null) etStock.setText(String.valueOf(product.stockQuantity));
        if (etMinStock != null) etMinStock.setText(String.valueOf(product.minStockQuantity));
        if (etDescription != null && product.description != null) etDescription.setText(product.description);
        if (product.imageUri != null && !product.imageUri.isEmpty()) {
            ImageUtils.load(this, product.imageUri, imgPreview, txtInitials, product.name);
        }
    }

    private void checkGalleryPermission() {
        String perm = Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU
                ? Manifest.permission.READ_MEDIA_IMAGES
                : Manifest.permission.READ_EXTERNAL_STORAGE;
        if (ContextCompat.checkSelfPermission(this, perm) == PackageManager.PERMISSION_GRANTED) {
            openGallery();
        } else {
            requestPermissions(new String[]{perm}, REQ_GALLERY_PERMISSION);
        }
    }

    private void checkCameraPermission() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) {
            openCamera();
        } else {
            requestPermissions(new String[]{Manifest.permission.CAMERA}, REQ_CAMERA_PERMISSION);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        boolean granted = grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED;
        if (requestCode == REQ_GALLERY_PERMISSION) {
            if (granted) openGallery();
            else Toast.makeText(this, "Cần quyền truy cập thư viện ảnh", Toast.LENGTH_SHORT).show();
        } else if (requestCode == REQ_CAMERA_PERMISSION) {
            if (granted) openCamera();
            else Toast.makeText(this, "Cần quyền truy cập camera", Toast.LENGTH_SHORT).show();
        }
    }

    private void openGallery() {
        Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
        intent.setType("image/*");
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        galleryLauncher.launch(Intent.createChooser(intent, "Chọn ảnh sản phẩm"));
    }

    private void openCamera() {
        try {
            File dir = new File(getFilesDir(), "product_images");
            if (!dir.exists()) dir.mkdirs();
            String ts = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(new Date());
            File photoFile = new File(dir, "CAM_" + ts + ".jpg");
            cameraUri = FileProvider.getUriForFile(this, getPackageName() + ".fileprovider", photoFile);
            Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
            intent.putExtra(MediaStore.EXTRA_OUTPUT, cameraUri);
            cameraLauncher.launch(intent);
        } catch (Exception e) {
            Toast.makeText(this, "Không thể mở camera: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    private String copyToInternal(Uri sourceUri) {
        try {
            File dir = new File(getFilesDir(), "product_images");
            if (!dir.exists()) dir.mkdirs();
            String ts = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(new Date());
            File dest = new File(dir, "PROD_" + ts + ".jpg");
            try (InputStream in  = getContentResolver().openInputStream(sourceUri);
                 OutputStream out = new FileOutputStream(dest)) {
                byte[] buf = new byte[4096];
                int len;
                while ((len = in.read(buf)) > 0) out.write(buf, 0, len);
            }
            return dest.getAbsolutePath();
        } catch (IOException e) {
            Toast.makeText(this, "Lỗi sao chép ảnh: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            return null;
        }
    }

    private void loadPreview(Uri uri) {
        imgPreview.setImageURI(uri);
        imgPreview.setVisibility(View.VISIBLE);
        if (txtInitials != null) txtInitials.setVisibility(View.GONE);
    }

    private void removeImage() {
        product.imageUri = null;
        imgPreview.setImageDrawable(null);
        imgPreview.setVisibility(View.GONE);
        if (txtInitials != null) {
            txtInitials.setText(product.name != null && product.name.length() >= 2
                ? product.name.substring(0, 2).toUpperCase() : "SP");
            txtInitials.setVisibility(View.VISIBLE);
        }
    }

    private void saveProduct() {
        String name = etName.getText().toString().trim();
        if (name.isEmpty()) {
            etName.setError("Tên sản phẩm không được để trống");
            etName.requestFocus();
            return;
        }
        product.name = name;
        if (etSku      != null) product.sku      = etSku.getText().toString().trim();
        if (etBarcode  != null) product.barcode  = etBarcode.getText().toString().trim();
        try { if (etPrice  != null) product.salePrice = Long.parseLong(etPrice.getText().toString().trim()); }
        catch (NumberFormatException e) { product.salePrice = 0; }
        try { if (etStock  != null) product.stockQuantity = Integer.parseInt(etStock.getText().toString().trim()); }
        catch (NumberFormatException e) { product.stockQuantity = 0; }
        try { if (etMinStock  != null) product.minStockQuantity = Integer.parseInt(etMinStock.getText().toString().trim()); }
        catch (NumberFormatException e) { product.minStockQuantity = 0; }
        try { if (etCostPrice != null) product.costPrice = Long.parseLong(etCostPrice.getText().toString().trim()); }
        catch (NumberFormatException e) { product.costPrice = 0; }
        if (etDescription != null) product.description = etDescription.getText().toString().trim();

        try {
            dao.save(product);
            Toast.makeText(this, "Đã lưu sản phẩm", Toast.LENGTH_SHORT).show();
            finish();
        } catch (Exception e) {
            String msg = e.getMessage();
            if (msg != null && msg.contains("UNIQUE")) {
                Toast.makeText(this, "SKU hoặc mã vạch đã tồn tại, vui lòng nhập khác", Toast.LENGTH_LONG).show();
            } else {
                Toast.makeText(this, "Lưu thất bại: " + msg, Toast.LENGTH_LONG).show();
            }
        }
    }

    private void confirmDelete() {
        new AlertDialog.Builder(this)
            .setTitle("Xóa sản phẩm")
            .setMessage("Bạn có chắc muốn xóa \"" + product.name + "\"?")
            .setPositiveButton("Xóa", (d, w) -> {
                dao.delete(product.id);
                Toast.makeText(this, "Đã xóa sản phẩm", Toast.LENGTH_SHORT).show();
                finish();
            })
            .setNegativeButton("Hủy", null)
            .show();
    }
}

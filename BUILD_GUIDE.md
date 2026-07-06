# mPOS Pro — Hướng dẫn Build trong Android Studio

> Follow hướng dẫn này từ đầu đến cuối để chạy app trên máy ảo hoặc điện thoại thật.

---

## BƯỚC 0 — Kiểm tra trước khi mở Android Studio

Mở **PowerShell** và chạy:

```powershell
# Kiểm tra file Gradle wrapper
type "C:\Users\ADMIN\Downloads\MobileApp-main\MobileApp-main\gradle\wrapper\gradle-wrapper.properties"
# Phải thấy: distributionUrl=...gradle-9.3.1-bin.zip
```

Nếu đúng → tiếp tục. Nếu sai → báo lại.

---

## BƯỚC 1 — Mở dự án trong Android Studio

### Cách 1: Dùng file .bat (khuyên dùng)
1. Vào thư mục `C:\Users\ADMIN\Downloads\K234111EAppTeacher4\`
2. Double-click file **`open_mpos.bat`**
3. Android Studio mở thẳng vào dự án

### Cách 2: Mở thủ công
1. Mở **Android Studio**
2. Chọn **File → Open...**
3. Điều hướng đến: `C:\Users\ADMIN\Downloads\MobileApp-main\MobileApp-main`
4. Nhấn **OK**

> ⚠️ Quan trọng: Chọn thư mục `MobileApp-main` BÊN TRONG (có file `build.gradle.kts`), không chọn thư mục cha.

---

## BƯỚC 2 — Gradle Sync

Sau khi mở, Android Studio tự chạy Gradle sync. Chờ thanh progress ở góc dưới bên phải hoàn thành.

**Nếu sync thành công:** thanh progress biến mất, không có thông báo đỏ.

**Nếu có lỗi:** nhìn vào panel **Build** ở dưới màn hình. Copy lỗi và báo lại.

### Trigger sync thủ công (nếu cần):
- Menu **File → Sync Project with Gradle Files**  
- Hoặc nhấn nút **🐘 Sync Now** xuất hiện trên thanh vàng ở đầu editor

---

## BƯỚC 3 — Cấu hình máy ảo (AVD)

### Tạo máy ảo mới:
1. Menu **Tools → Device Manager**
2. Nhấn **+ Create Device**
3. Chọn: **Phone → Pixel 6** (hoặc bất kỳ)
4. System Image: chọn **API 28** (Android 9.0) hoặc cao hơn
5. Nhấn **Finish**

> Nếu đã có AVD sẵn → bỏ qua bước này.

---

## BƯỚC 4 — Chạy App

1. Trên thanh toolbar, chọn device (máy ảo vừa tạo) từ dropdown
2. Nhấn nút ▶ **Run** (hoặc `Shift + F10`)
3. Android Studio build APK và cài vào máy ảo

**App khởi động → màn hình Login xuất hiện.**

---

## BƯỚC 5 — Đăng nhập và test flow cơ bản

### Tài khoản mặc định:
```
Username: admin
Password: admin123
```

### Flow cơ bản để test:

```
1. Đăng nhập
   └── Nhập admin / admin123 → nhấn "Đăng nhập"

2. Mở ca (BẮT BUỘC trước khi bán)
   └── Dashboard → nhấn "Mở ca ngay"
   └── ShiftActivity: nhập số tiền đầu ca (ví dụ: 500000) → nhấn "Mở ca"

3. Tạo đơn bán
   └── Dashboard → nhấn "Tạo đơn mới"
   └── PosActivity: thấy 2 sản phẩm mẫu (Nước suối, Bánh quy)
   └── Tap vào sản phẩm để thêm vào giỏ
   └── Nhấn "Thanh toán"

4. Giỏ hàng
   └── CartActivity: xem danh sách, nhấn "Tiếp tục thanh toán"

5. Thanh toán
   └── CheckoutActivity:
       - Nhập SĐT khách (tùy chọn)
       - Chọn phương thức: CASH
       - Nhập tiền khách đưa (ví dụ: 100000)
       - Nhấn "Xác nhận thanh toán"

6. Hóa đơn
   └── ReceiptActivity: xem hóa đơn, nhấn "Tạo đơn mới" để quay lại POS
```

---

## BƯỚC 6 — Khám phá các màn hình khác

### Từ Dashboard:

| Nút | Mở màn hình | Test gì |
|---|---|---|
| Đơn hàng | OrderListActivity | Xem đơn vừa tạo |
| Sản phẩm | ProductListActivity | Thêm sản phẩm mới |
| Khách hàng | CustomerListActivity | Tìm/thêm khách |
| Tồn kho | InventoryActivity | Xem tồn, điều chỉnh |
| Báo cáo | ReportActivity | Doanh thu, top sản phẩm |
| Unified Inbox | UnifiedInboxActivity | Xem đơn từ kênh |
| Đồng bộ | SyncStatusActivity | Trạng thái sync queue |

### Bottom Navigation (5 tab):
```
[Trang chủ] [Bán hàng] [Đơn] [Sản phẩm] [Thêm]
```

### Từ Tab "Thêm" (MoreActivity):
- Ca làm việc → ShiftActivity
- Đồng bộ dữ liệu → SyncStatusActivity
- Unified Inbox → UnifiedInboxActivity
- Sản phẩm → ProductListActivity
- Khách hàng → CustomerListActivity
- Hồ sơ & Cài đặt → ProfileActivity → SettingsActivity

---

## BƯỚC 7 — Build APK để cài điện thoại thật

### Build Debug APK:
1. Menu **Build → Build Bundle(s) / APK(s) → Build APK(s)**
2. Chờ build xong → thông báo "APK(s) generated successfully"
3. Nhấn **locate** để mở thư mục chứa APK
4. Copy APK sang điện thoại → cài đặt

**Hoặc dùng script:**
```
Double-click: build_debug.bat (trong thư mục gốc dự án)
APK xuất ra: app\build\outputs\apk\debug\app-debug.apk
```

### Chạy trên điện thoại thật:
1. Bật **Developer Options** → bật **USB Debugging**
2. Cắm điện thoại vào máy tính
3. Android Studio tự detect → chọn từ dropdown → nhấn ▶

---

## BƯỚC 8 — Thêm tính năng mới (theo PRD)

### Quy tắc thêm màn hình:

**1. Tạo layout XML** trong `app/src/main/res/layout/`

```xml
<!-- Dùng mẫu chuẩn dự án: -->
<?xml version="1.0" encoding="utf-8"?>
<LinearLayout xmlns:android="http://schemas.android.com/apk/res/android"
    android:layout_width="match_parent"
    android:layout_height="match_parent"
    android:background="@color/app_background"
    android:orientation="vertical"
    android:padding="20dp">

    <!-- Nút back -->
    <Button android:id="@+id/btnBack"
        style="@style/Widget.Mpos.Button.Outline"
        android:layout_width="wrap_content"
        android:layout_height="44dp"
        android:text="‹  Quay lại"/>

    <!-- Nội dung màn hình -->

</LinearLayout>
```

**2. Tạo Activity Java** trong package phù hợp:

```java
package com.example.mpos.ten_module;

import android.os.Bundle;
import androidx.appcompat.app.AppCompatActivity;
import com.example.mpos.R;

public class TenActivity extends AppCompatActivity {
    @Override
    public void onCreate(Bundle state) {
        super.onCreate(state);
        setContentView(R.layout.activity_ten);
        findViewById(R.id.btnBack).setOnClickListener(v -> finish());
        // ... logic
    }
}
```

**3. Khai báo trong AndroidManifest.xml:**

```xml
<activity android:name=".ten_module.TenActivity" />
```

**4. Gọi từ Activity khác:**

```java
startActivity(new Intent(this, TenActivity.class));
```

---

## Cấu trúc thư mục dự án

```
MobileApp-main/
├── app/src/main/
│   ├── java/com/example/mpos/
│   │   ├── auth/           LoginActivity, SessionManager, PasswordUtils
│   │   ├── cart/           CartActivity, CartManager
│   │   ├── customer/       CustomerListActivity, CustomerFormActivity, CustomerDetailActivity
│   │   ├── dao/            UserDao, ProductDao, CustomerDao, ShiftDao, SettingsDao
│   │   ├── database/       DatabaseHelper, DatabaseContract, DatabaseSeeder
│   │   ├── employee/       AuditLogActivity
│   │   ├── inventory/      InventoryActivity, InventoryHistoryActivity, StockAdjustmentActivity
│   │   ├── model/          User, Product, Customer, CartItem
│   │   ├── omnichannel/    UnifiedInboxActivity
│   │   ├── order/          OrderListActivity, OrderDetailActivity, CheckoutActivity, CheckoutService
│   │   ├── pos/            PosActivity
│   │   ├── product/        ProductListActivity, ProductFormActivity
│   │   ├── profile/        ProfileActivity
│   │   ├── receipt/        ReceiptActivity
│   │   ├── report/         ReportActivity
│   │   ├── settings/       SettingsActivity
│   │   ├── shift/          ShiftActivity, ShiftReportActivity
│   │   ├── sync/           SyncStatusActivity, SyncQueueProcessor, AuditLogger
│   │   ├── ui/             BottomNavHelper, MoreActivity
│   │   └── utils/          CurrencyUtils
│   │
│   └── res/
│       ├── layout/         29 layout files
│       ├── drawable/       24 drawable files (backgrounds, buttons, badges)
│       └── values/         colors.xml, styles.xml, themes.xml, dimens.xml, strings.xml
│
├── gradle/
│   └── libs.versions.toml  AGP 8.7.0 + Material 1.12.0
├── PRD.md                  ← Tài liệu yêu cầu sản phẩm
├── BUILD_GUIDE.md          ← File này
└── build_debug.bat         ← Script build nhanh
```

---

## Xử lý lỗi thường gặp

### "Gradle sync failed"
```
Kiểm tra: gradle-wrapper.properties có gradle-9.3.1 không?
Nếu không: chạy PowerShell → cd vào thư mục dự án → 
  $content = "distributionBase=GRADLE_USER_HOME`ndistributionPath=wrapper/dists`ndistributionUrl=https\://services.gradle.org/distributions/gradle-9.3.1-bin.zip`nnetworkTimeout=10000`nzipStoreBase=GRADLE_USER_HOME`nzipStorePath=wrapper/dists"
  Set-Content -Path ".\gradle\wrapper\gradle-wrapper.properties" -Value $content
```

### "Cannot resolve symbol R"
→ Chờ Gradle sync xong, hoặc **Build → Clean Project** rồi Rebuild.

### "App crashes at login"
→ Xóa dữ liệu app trên máy ảo: Settings → Apps → mPOS Pro → Clear Data. App sẽ seed lại DB.

### "Thanh toán báo 'Cần mở ca trước'"
→ Từ Dashboard nhấn **"Mở ca ngay"** → nhập tiền đầu ca → **"Mở ca"**. Sau đó mới thanh toán được.

### "SSL error khi download Gradle"
→ AVG AntiVirus chặn. Giải pháp: tắt AVG tạm thời khi Gradle đang download, hoặc dùng gradle-9.3.1 đã cache sẵn.

---

## Thông tin kỹ thuật

| Thông số | Giá trị |
|---|---|
| AGP (Android Gradle Plugin) | 8.7.0 |
| Gradle wrapper | 9.3.1 |
| compileSdk / targetSdk | 35 |
| minSdk | 26 (Android 8.0) |
| Java version | 11 |
| Material Design | 1.12.0 (Material3) |
| Theme | `Theme.Material3.DayNight.NoActionBar` |
| Database | SQLite (SQLiteOpenHelper) |
| Architecture | Activity → DAO → DatabaseHelper → SQLite |

# Quầy mPOS – Ứng dụng bán hàng di động

Ứng dụng quản lý bán hàng (Point of Sale) dành cho doanh nghiệp vừa và nhỏ tại Việt Nam, xây dựng trên nền tảng Android (Java).

---

## Tính năng chính

| Module | Chức năng |
|---|---|
| **Xác thực** | Đăng nhập email/mật khẩu, Google Sign-In, phân quyền 3 cấp (Admin / Quản lý / Nhân viên) + xác thực PIN |
| **Quản lý sản phẩm** | Thêm, sửa, xóa, tìm kiếm, nhập CSV, quét barcode |
| **Bán hàng (POS)** | Giỏ hàng, áp mã giảm giá, tính VAT, in hoá đơn |
| **Thanh toán** | Tiền mặt, chuyển khoản VietQR, ví điện tử |
| **Quản lý đơn hàng** | Danh sách đơn, chi tiết, lặp lại đơn cũ |
| **Quản lý khách hàng** | CRUD đầy đủ, tích điểm loyalty |
| **Quản lý kho** | Điều chỉnh tồn kho, lịch sử nhập/xuất, cảnh báo hàng sắp hết |
| **Ca làm việc** | Mở/đóng ca, báo cáo theo ca, trạng thái hiển thị real-time |
| **Báo cáo** | Doanh thu theo ngày/tháng, lợi nhuận, top sản phẩm |
| **Vận chuyển** | Tích hợp GHN & GHTK, tạo đơn và theo dõi tracking |
| **Omnichannel** | Kết nối Shopee, TikTok Shop |
| **AI Trợ lý** | Chat hỏi đáp thông minh qua Groq API (Llama 3.1), phân tích dữ liệu shop |
| **Đồng bộ đám mây** | Firebase Realtime Database, push notification (FCM) |
| **Cài đặt** | Thông tin cửa hàng, VietQR, tích điểm, mẫu hoá đơn, quản lý PIN |

---

## Phân quyền

| Role | Quyền hạn | Xác thực |
|---|---|---|
| Nhân viên | POS, đơn hàng, khách hàng, ca làm việc | Email + mật khẩu |
| Quản lý | Tất cả của Nhân viên + Sản phẩm, Kho, Báo cáo | Email + mật khẩu + **PIN Quản lý** |
| Admin | Toàn quyền + Cài đặt hệ thống, đổi PIN | Email + mật khẩu + **PIN Admin** |

> PIN mặc định: Quản lý = `1234` · Admin = `0000`  
> Admin có thể đổi PIN trong mục Cài đặt.

---

## Công nghệ sử dụng

- **Ngôn ngữ:** Java
- **IDE:** Android Studio
- **Database:** SQLite (SQLiteOpenHelper, raw SQL) — offline-first
- **Cloud:** Firebase Realtime Database + Firebase Cloud Messaging (FCM)
- **AI:** Groq API — model `llama-3.1-8b-instant`
- **Vận chuyển:** GHN API, GHTK API
- **Sàn TMĐT:** Shopee Open Platform API v2, TikTok Shop Open API
- **Xác thực:** Google Sign-In (Firebase Auth)
- **Min SDK:** API 21 (Android 5.0)

---

## Cài đặt & Chạy

### Yêu cầu
- Android Studio Hedgehog trở lên
- JDK 17+
- Android SDK API 35

### Các bước

```bash
# 1. Clone repo
git clone https://github.com/Vinhtheripper/MobileApp.git
cd MobileApp

# 2. Thêm file google-services.json vào app/
# (lấy từ Firebase Console của project)

# 3. Build
./gradlew assembleDebug

# 4. Cài lên thiết bị / emulator
adb install -r app/build/outputs/apk/debug/app-debug.apk
```

### Tài khoản demo
| Field | Giá trị |
|---|---|
| Email | `admin@mpos.com` |
| Mật khẩu | `admin123` |
| PIN Admin | `0000` |

---

## Cấu trúc thư mục

```
app/src/main/java/com/example/mpos/
├── auth/          # Đăng nhập, SessionManager, phân quyền
├── dao/           # Data Access Objects (Product, Customer, Order, Shift...)
├── database/      # DatabaseHelper, DatabaseSeeder, DatabaseContract
├── model/         # POJO: Product, Customer, Order, Shop, User...
├── pos/           # Màn hình bán hàng
├── order/         # Đơn hàng, thanh toán, CheckoutService
├── product/       # Quản lý sản phẩm
├── customer/      # Quản lý khách hàng
├── inventory/     # Quản lý kho
├── shift/         # Ca làm việc
├── report/        # Báo cáo doanh thu
├── logistics/     # Vận chuyển GHN/GHTK
├── omnichannel/   # Shopee, TikTok Shop
├── ai/            # AI Chat (Groq)
├── sync/          # Firebase sync, WorkManager
├── settings/      # Cài đặt
└── notification/  # FCM push notification
```

---

## Giấy phép

Dự án được phát triển phục vụ mục đích học thuật (đồ án tốt nghiệp).

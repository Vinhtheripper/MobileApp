# Project Audit — mPOS Pro

Tài liệu này đánh giá toàn diện hệ thống hiện tại của dự án mPOS Pro trước khi tiến hành tối ưu hóa và bổ sung tính năng dựa trên các ứng dụng tham chiếu (Sổ Bán Hàng, Pancake POS).

---

## 1. Kiến trúc hệ thống & Folder Structure

### Hiện trạng kiến trúc
- **Kiểu kiến trúc**: Hệ thống được tổ chức theo mô hình phân lớp truyền thống (Layered Architecture):
  `Activity/Fragment (UI) -> DAO (Data Access Object) -> DatabaseHelper -> SQLite`
- **Luồng điều hướng (Routing)**: Chuyển đổi màn hình bằng cách khởi chạy trực tiếp các `Activity` qua `Intent`. Session được quản lý tập trung bởi `SessionManager` lưu trong `SharedPreferences`.
- **Cấu trúc thư mục (Packages)**: Phân chia theo tính năng kết hợp lớp kỹ thuật:
  - `auth/`: Quản lý đăng nhập, phiên làm việc (`LoginActivity`, `SessionManager`, `PasswordUtils`, `RegisterActivity`).
  - `cart/`: Trạng thái giỏ hàng trong bộ nhớ (`CartManager`) và màn hình giỏ hàng/thanh toán.
  - `customer/`: CRM quản lý khách hàng (`CustomerListActivity`, `CustomerFormActivity`, `CustomerDetailActivity`).
  - `dao/`: Lớp truy vấn dữ liệu thô SQLite cho từng thực thể.
  - `database/`: Cấu hình SQLite, seed dữ liệu mẫu và các file migration lịch sử.
  - `employee/`: Chứa lịch sử thao tác (`AuditLogActivity`).
  - `inventory/`: Điều chỉnh tồn kho và quản lý lịch sử biến động kho.
  - `model/`: Các POJO đại diện cho thực thể dữ liệu.
  - `omnichannel/`: Tích hợp các kênh bán hàng ngoài Walk-in (`UnifiedInboxActivity`).
  - `order/`: Xem danh sách đơn hàng, chi tiết đơn, và quá trình checkout.
  - `pos/`: Giao diện bán hàng chính tại quầy.
  - `product/`: Quản lý danh mục và sản phẩm.
  - `profile/`: Màn hình thông tin tài khoản đang đăng nhập.
  - `receipt/`: Tạo hóa đơn điện tử dạng xem trước và in mock.
  - `report/`: Báo cáo doanh thu và sản phẩm bán chạy.
  - `settings/`: Cài đặt cửa hàng, thuế, máy in.
  - `shift/`: Đóng/mở ca làm việc của nhân viên thu ngân.
  - `sync/`: Quản lý hàng đợi đồng bộ ngoại tuyến (`sync_queue`).
  - `ui/`: Các thành phần giao diện dùng chung (`BottomNavHelper`, `MoreActivity`).
  - `utils/`: Hàm bổ trợ như định dạng tiền tệ (`CurrencyUtils`).

---

## 2. Điểm mạnh (Strengths)
1. **Kiến trúc rõ ràng, tuần tự**: Sự phân chia `Activity -> DAO -> DatabaseHelper` giúp giảm bớt việc viết code SQL trực tiếp trong các Activity.
2. **Quy trình Thanh toán Nguyên tử (Atomic Checkout Transaction)**: Giao dịch checkout được bọc trong một transaction SQLite đơn nhất (`CheckoutService.java`), đảm bảo tính nhất quán cao (tạo đơn, trừ kho, tạo giao dịch kho, ghi log hệ thống, tạo hóa đơn, và thêm vào hàng đợi sync xảy ra đồng thời hoặc cùng fail).
3. **Khả năng Offline-first thực chất**: Ứng dụng chạy mượt mà không cần internet. Mọi hành động làm thay đổi trạng thái dữ liệu đều được lưu trữ local và tự động xếp vào `sync_queue` dưới dạng payload JSON để đồng bộ sau.
4. **Phân quyền truy cập cơ bản hoạt động ổn định**: Đăng nhập đã có định tuyến luồng riêng cho `STAFF` (dẫn tới `StaffDashboardActivity` bị giới hạn menu) và `ADMIN/MANAGER` (dẫn tới `MainActivity`).
5. **Dữ liệu mẫu (Seed Data) phong phú**: Giúp kiểm thử nhanh các bộ dữ liệu bán hàng thực tế mà không cần khởi tạo thủ công.

---

## 3. Điểm yếu & Nợ kỹ thuật (Weaknesses & Technical Debt)

### Nợ kỹ thuật (Technical Debt)
1. **Không có hệ thống quản lý Multi-tenant / Multi-store thực sự**: 
   - Mặc dù cấu trúc dữ liệu hỗ trợ nhiều employee và user, nhưng toàn bộ bảng đều lưu chung một database mà không có cột phân tách cửa hàng hoặc chi nhánh (`store_id` hoặc `tenant_id`). Nếu khách hàng có chuỗi nhiều cửa hàng, việc cô lập dữ liệu là không thể.
2. **Quản lý trạng thái sơ sài (State Management Debt)**:
   - Hệ thống không sử dụng thư viện quản lý trạng thái hoặc mô hình như LiveData / ViewModel. Khi dữ liệu thay đổi (ví dụ: giỏ hàng, thông tin khách), các Activity phải tải lại toàn bộ dữ liệu từ DB ở hàm `onResume()`. Điều này gây lãng phí tài nguyên và làm UI bị nhấp nháy (rebuild không cần thiết).
3. **Hệ thống điều hướng cứng (Hardcoded Navigation)**:
   - Các logic bottom navigation bar (`BottomNavHelper`) được code cứng cho từng bộ layout. Việc duy trì nhiều layout menu khác nhau (`include_bottom_nav_pos`, `include_bottom_nav_orders`...) gây khó khăn khi thay đổi cấu trúc menu.
4. **Màu sắc và Tên biến không đồng nhất**:
   - Như được ghi chú trong `DESIGN_SYSTEM.md`, các biến màu có tiền tố `blue_*` thực chất chứa mã màu cam chủ đạo (do lịch sử chuyển đổi thương hiệu). Đây là món nợ kỹ thuật lớn gây nhầm lẫn cho lập trình viên mới.

### Trùng lặp mã nguồn (Duplicated Logic)
- **Truy vấn thống kê**: Logic lấy thông tin tổng kết doanh thu ngày và sản phẩm bán chạy bị lặp lại một phần giữa `MainActivity` và `ReportActivity`.
- **Mã định dạng**: Các hàm định dạng số tiền VND và ngày tháng được định nghĩa phân tán hoặc dùng trực tiếp `SimpleDateFormat` nhiều nơi thay vì tập trung hóa hoàn toàn vào `DateTimeUtils` hay `CurrencyUtils`.

### Thành phần không sử dụng hoặc dư thừa (Unused Components)
- `txtReport` và `txtInbox` trong một số XML layout được khai báo nhưng luôn đặt trạng thái `visibility=gone` và chỉ dùng làm fallback tĩnh mà không có hoạt động nghiệp vụ nào.

---

## 4. Vấn đề về Khả năng mở rộng, Hiệu năng & Bảo mật

### Vấn đề mở rộng (Scalability Issues)
- **SQLite Single Connection**: SQLite xử lý ghi tuần tự. Nếu ứng dụng có luồng đồng bộ chạy ngầm ghi dữ liệu liên tục song song với nhân viên đang checkout tại quầy, có thể xảy ra tình trạng khóa database (`sqlite3_busy` hoặc `database is locked`).
- **Phân mảnh Tenant**: Thiếu cấu trúc đa chi nhánh cô lập dữ liệu ở mức độ logic database.

### Vấn đề hiệu năng (Performance Issues)
- **I/O trên Main Thread**: Một số tác vụ đọc DB (ví dụ: `loadStats()`, tìm kiếm khách hàng) diễn ra trực tiếp trên Main Thread của Activity, có thể gây giật lag hoặc lỗi ANR (Application Not Responding) nếu số lượng bản ghi tăng lên hàng chục nghìn.
- **Tải ảnh chưa tối ưu**: Các ảnh sản phẩm sử dụng URL từ `loremflickr.com` hoặc file URI được tải trực tiếp mà không qua cơ chế cache/nén ảnh (như Glide hay Coil), dẫn đến tốn băng thông và bộ nhớ RAM lớn.

### Vấn đề bảo mật (Security Issues)
- **Cô lập dữ liệu yếu**: Vì thiếu `store_id` scoped, một lỗi phần mềm nhỏ hoặc SQL Injection có thể làm lộ thông tin đơn hàng/doanh thu của cửa hàng này sang cửa hàng khác trên cùng thiết bị.
- **SQL Injection**: Một số câu lệnh truy vấn thô ghép chuỗi trực tiếp (`rawQuery("... WHERE ... = " + input)`) thay vì sử dụng tham số hóa (`?`) cần được rà soát triệt để.
- **Lưu trữ nhạy cảm**: Token và mật khẩu của phiên làm việc hiện tại được lưu dưới dạng Plain Text trong `SharedPreferences` thay vì sử dụng `EncryptedSharedPreferences`.

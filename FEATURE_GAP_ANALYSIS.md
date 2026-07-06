# Feature Gap Analysis — mPOS Pro

Tài liệu này so sánh tính năng hiện có của **mPOS Pro** với các ứng dụng tham chiếu đầu ngành như **Sổ Bán Hàng** và **Pancake POS** dựa trên phân tích hình ảnh và nghiệp vụ thực tế.

---

## 1. Bản đồ tính năng: Hiện tại vs Tham chiếu

| Tính năng / Nghiệp vụ | Trạng thái mPOS Pro hiện tại | Sổ Bán Hàng / Pancake POS | Mức độ ưu tiên |
|---|---|---|---|
| **Multi-tenant & Multi-store** | Chưa hỗ trợ (chung database, không phân chi nhánh) | Cách biệt hoàn toàn dữ liệu từng cửa hàng, phân quyền chi nhánh | **Critical** (Chí mạng) |
| **Giá vốn & Tính lãi lỗ thực tế** | Chỉ lưu `cost_price` nhưng báo cáo chưa tính giá vốn | Công thức chuẩn: `Doanh thu - Giá vốn + Thu nhập khác` | **Critical** (Chí mạng) |
| **Quản lý Sổ quỹ (Thu chi)** | Chưa có | Quản lý dòng tiền ngoài bán hàng (tiền thuê nhà, lương, điện nước...) | **Critical** (Chí mạng) |
| **Quản lý Sổ nợ & Nhóm khách** | Có CRM cơ bản, chưa có tính năng ghi nợ và nhóm khách | Ghi nợ trực tiếp khi checkout, nhắc nợ, chia nhóm khách hàng | **Recommended** (Nên có) |
| **Quét Barcode qua Camera** | Mock (BarcodeScannerActivity chỉ là stub) | Tích hợp CameraX + ML Kit quét barcode thật siêu tốc | **Recommended** (Nên có) |
| **Thiết lập Khuyến mãi** | Chưa có | 3 cơ chế: Ưu đãi mua nhiều, Tặng kèm sản phẩm, Giảm giá đơn hàng | **Recommended** (Nên có) |
| **Cấu hình & Customize hóa đơn** | Chưa có | Thay đổi cỡ chữ, ẩn/hiện logo, SĐT, địa chỉ, mã QR cửa hàng | **Recommended** (Nên có) |
| **Đồng bộ tự động & Realtime** | Nút mock "Đồng bộ ngay" | Đồng bộ ngầm liên tục khi có mạng qua API REST/Firebase | **Optional** (Tùy chọn) |
| **Kết nối thanh toán tự động** | Mock VNPay/MoMo QR tĩnh | Quét VietQR động + liên kết ngân hàng thông báo tiền về tự động | **Optional** (Tùy chọn) |
| **Ước tính thuế hộ kinh doanh** | Chưa có | Cộng dồn doanh thu so với ngưỡng 500 triệu/năm, dự báo thuế | **Nice-to-have** (Thêm vào) |
| **Danh thiếp cửa hàng & QR** | Chưa có | Tạo ảnh danh thiếp (Maneki-neko...) kèm QR link cửa hàng để chia sẻ | **Nice-to-have** (Thêm vào) |
| **Tích hợp Vận chuyển & Sàn** | Chỉ có UI stub | Liên kết API Shopee, Lazada, TikTok Shop, GHN, Ahamove | **Experimental** (Thử nghiệm) |

---

## 2. Chi tiết phân tích khoảng cách tính năng (Feature Gap Details)

### A. Nhóm tính năng Chí mạng (Critical Missing Features)

#### 1. Kiến trúc Đa chi nhánh / Đa cửa hàng (Multi-tenant Architecture)
- **Tại sao cần**: Một hộ kinh doanh khi phát triển sẽ mở thêm chi nhánh. Nhân viên chi nhánh A không được thấy doanh thu hoặc kho chi nhánh B. Chủ cửa hàng cần xem báo cáo tổng hợp của tất cả hoặc từng chi nhánh.
- **Giá trị kinh doanh**: Cho phép bán gói dịch vụ cao cấp (chuỗi cửa hàng) với mức phí thuê bao cao hơn. Đảm bảo bảo mật dữ liệu nội bộ.
- **Độ phức tạp kỹ thuật**: Cao. Đòi hỏi thay đổi cấu trúc toàn bộ 14 bảng database để thêm trường `store_id` (hoặc `tenant_id`) và `branch_id`, đồng thời cập nhật tất cả các câu truy vấn DAO để áp dụng filter tự động (`WHERE store_id = ?`).

#### 2. Sổ quỹ Thu chi độc lập (Cashbook Module)
- **Tại sao cần**: Cửa hàng không chỉ có nguồn thu từ bán hàng. Họ phải trả tiền thuê mặt bằng, điện nước, lương nhân viên (Khoản chi) hoặc nhận thanh lý tài sản, góp vốn (Khoản thu). Thiếu sổ quỹ, chủ cửa hàng không thể biết dòng tiền mặt thực tế trong két.
- **Giá trị kinh doanh**: Biến app từ một máy POS bán lẻ thành một công cụ quản lý tài chính mini. Giúp chủ cửa hàng kiểm soát thất thoát tiền mặt.
- **Độ phức tạp kỹ thuật**: Trung bình. Cần thêm bảng `cashbook_transactions` và xây dựng màn hình danh sách giao dịch thu/chi, bộ lọc theo phân loại, và form ghi nhận thu chi ngoài bán hàng.

#### 3. Báo cáo Lợi nhuận gộp (P&L Reports)
- **Tại sao cần**: Doanh thu cao không có nghĩa là có lãi. Chủ cửa hàng cần biết lợi nhuận gộp sau khi trừ đi giá vốn của các sản phẩm đã bán.
- **Giá trị kinh doanh**: Giúp đưa ra quyết định nhập hàng và điều chỉnh giá bán chính xác.
- **Độ phức tạp kỹ thuật**: Thấp đến trung bình. Cần sửa logic lưu trữ đơn hàng để lưu lại giá vốn tại thời điểm bán (đề phòng giá vốn sản phẩm thay đổi sau này) và cập nhật màn hình Báo cáo để hỗ trợ tab Lãi/Lỗ.

---

### B. Nhóm tính năng Nên có (Recommended Features)

#### 1. Giao dịch Ghi nợ CRM (Customer Debt Ledger)
- **Tại sao cần**: Khách quen mua hàng thường "ghi sổ" nợ để thanh toán cuối tháng.
- **Giá trị kinh doanh**: Giữ chân khách hàng trung thành, đặc biệt ở các mô hình tạp hóa truyền thống tại Việt Nam.
- **Độ phức tạp kỹ thuật**: Trung bình. Cần thêm cột `payment_status` (PAID, PARTIAL, DEBT) vào bảng `orders` và bảng `customer_debts` để ghi nhận công nợ lịch sử, kèm màn hình thu hồi nợ.

#### 2. Thiết lập cấu hình hóa đơn in nhiệt (Receipt Customizer)
- **Tại sao cần**: Mỗi cửa hàng có kích thước máy in khác nhau (58mm, 80mm) và thông tin liên hệ riêng. Họ muốn ẩn/hiện SĐT hoặc in thêm mã VietQR để khách quét chuyển khoản.
- **Giá trị kinh doanh**: Nâng cao tính chuyên nghiệp cho cửa hàng khi gửi bill cho khách.
- **Độ phức tạp kỹ thuật**: Thấp. Đọc/ghi cấu hình từ bảng `settings` và binding động vào chuỗi HTML/Text in hóa đơn.

#### 3. Tích hợp máy quét mã vạch thật bằng Camera (Barcode Scanner)
- **Tại sao cần**: Bán hàng nhanh yêu cầu quét mã sản phẩm thay vì gõ tìm kiếm thủ công.
- **Giá trị kinh doanh**: Tăng tốc độ phục vụ tại quầy lên gấp 3 lần.
- **Độ phức tạp kỹ thuật**: Trung bình. Tích hợp thư viện Google ML Kit Barcode Scanning kết hợp với CameraX của Android.

---

### C. Nhóm tính năng Tùy chọn & Thử nghiệm (Optional & Experimental)

#### 1. Tự động hóa VietQR (VietQR Payment Automation)
- **Tại sao cần**: Thanh toán chuyển khoản ngân hàng đang chiếm 60% giao dịch tại Việt Nam. Quét VietQR động tự động điền số tiền và nội dung đơn giúp tránh sai sót.
- **Giá trị kinh doanh**: Giảm thiểu thời gian đối soát chuyển khoản cho nhân viên thu ngân.
- **Độ phức tạp kỹ thuật**: Thấp. Tạo mã VietQR dựa trên định dạng NAPAS247 (sử dụng thông tin tài khoản ngân hàng của chủ tiệm lưu trong `settings` và số tiền đơn hàng).

#### 2. Tích hợp Sàn TMĐT & Vận chuyển (Omnichannel & Shipping Sync)
- **Tại sao cần**: Đồng bộ đơn hàng từ Shopee, TikTok Shop về POS và đẩy vận đơn sang GHN, Ahamove.
- **Giá trị kinh doanh**: Dành cho nhà bán hàng đa kênh chuyên nghiệp.
- **Độ phức tạp kỹ thuật**: Rất cao. Đòi hỏi kết nối các API bên thứ ba, xử lý webhook và quản lý trạng thái giao vận phức tạp. Bước đầu nên làm dưới dạng Mock API để demo quy trình.

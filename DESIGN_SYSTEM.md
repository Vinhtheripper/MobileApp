# mPOS Pro — Design System

Tài liệu này mô tả hệ thống màu sắc, typography và component đang được dùng thực tế trong project.

---

## Bảng màu (Color Tokens)

| Token (XML name)      | Giá trị    | Dùng cho                                        |
|-----------------------|------------|-------------------------------------------------|
| `blue_primary`        | `#F57C00`  | CTA chính, trạng thái được chọn               |
| `blue_primary_dark`   | `#E65100`  | Heading, nhấn mạnh thương hiệu                |
| `blue_navy`           | `#E65100`  | Text tiền tệ (Money), màu đậm                 |
| `cyan_accent`         | `#FFB300`  | Nhấn phụ, badge số lượng                      |
| `blue_sky`            | `#FFF3E0`  | Filter được chọn, warning nhẹ                 |
| `app_background`      | `#FFF8F2`  | Nền màn hình                                  |
| `surface`             | `#FFFFFF`  | Card, panel, input                            |
| `border`              | `#F5E6D3`  | Viền card và input                            |
| `divider`             | `#FFF3E0`  | Đường kẻ phân cách                           |
| `deep_navy_text`      | `#1F2937`  | Text chính                                    |
| `text_primary`        | `#1F2937`  | Text chính (alias)                            |
| `text_secondary`      | `#6B7280`  | Text phụ, label                               |
| `text_muted`          | `#9CA3AF`  | Placeholder, hint                             |
| `status_success`      | `#2E7D32`  | Đã thanh toán, đồng bộ, có hàng              |
| `status_warning`      | `#F9A825`  | Sắp hết hàng, cảnh báo                       |
| `status_error`        | `#D32F2F`  | Hủy, xóa, lỗi                                |
| `success_soft`        | `#E8F5E9`  | Nền chip thành công                           |
| `warning_soft`        | `#FFF8E1`  | Nền chip cảnh báo                             |
| `error_soft`          | `#FFEBEE`  | Nền chip lỗi                                  |
| `pending_purple`      | `#7C3AED`  | Trạng thái chờ xử lý                         |
| `pending_soft`        | `#F3EEFF`  | Nền chip chờ xử lý                           |
| `disabled_background` | `#E5E7EB`  | Nền button/input bị disable                  |
| `disabled_text`       | `#9CA3AF`  | Text bị disable                              |

> **
> 






Lưu ý tên biến:** Các biến có tiền tố `blue_*` thực chất chứa màu cam — do lịch sử
> đổi màu chủ đạo từ xanh sang cam. **Không đổi tên biến** vì sẽ phá vỡ toàn bộ layout.

---

## Typography

| Style XML                          | Base Material3                            | Dùng cho                          |
|------------------------------------|-------------------------------------------|-----------------------------------|
| `TextAppearance.Mpos.Title`        | `TextAppearance.Material3.TitleLarge`     | Tiêu đề màn hình (26sp, bold)    |
| `TextAppearance.Mpos.Money`        | `TextAppearance.Material3.HeadlineMedium` | Hiển thị số tiền (bold, cam đậm) |

---

## Components

### Buttons

| Style XML                      | Chiều cao tối thiểu | Dùng khi nào                                      |
|-------------------------------|---------------------|---------------------------------------------------|
| `Widget.Mpos.Button.Primary`  | 52dp                | Hành động chính duy nhất trên màn hình (màu cam) |
| `Widget.Mpos.Button.Success`  | 52dp                | Xác nhận thanh toán thành công (màu xanh lá)     |
| `Widget.Mpos.Button.Outline`  | 48dp                | Điều hướng, hành động có thể hoàn tác           |
| `Widget.Mpos.Button.Ghost`    | 48dp                | Hành động phụ, ít nhấn mạnh                     |

### Card & Input

| Resource                | Quy tắc                                              |
|-------------------------|------------------------------------------------------|
| `@drawable/card_surface`| Nền trắng, bo góc 16dp, border `#F5E6D3`           |
| `input_background`      | Cao 52dp, padding ngang 14–16dp                     |

---

## Responsive

- **Phone**: flow 1 cột, nút thanh toán cố định phía dưới.
- **Tablet (`sw600dp`)**: POS chia 2 panel — danh sách sản phẩm trái, cart/checkout 320dp phải.

---

## Màn hình đã implement

Login · Dashboard · POS · Cart · Checkout · Receipt · Order List · Order Detail ·
Product List · Product Form · Customer List · Customer Form · Customer Detail ·
Inventory · Stock Adjustment · Inventory History · Shift · Shift Report · Report ·
Audit Log · Settings · Profile · CSV Import · Unified Inbox · Sync Status

---

## Quy tắc khi thêm màn hình mới

1. Chỉ dùng token màu trong bảng trên — không hardcode hex.
2. Mỗi màn hình chỉ có **một** `Widget.Mpos.Button.Primary`.
3. Card dùng `@drawable/card_surface`.
4. Input: cao 52dp, padding ngang 14–16dp, background `@drawable/input_background`.
5. Luôn có empty state khi danh sách trống.
6. Không tạo màu hoặc button style mới ngoài bảng này.

# mPOS Pro — Product Requirements Document (PRD)

> **Dự án:** mPOS Pro — Mobile Point of Sale  
> **Tech stack:** Java · Android XML Views · SQLiteOpenHelper (Offline-first)  
> **Package:** `com.example.mpos` · minSdk 26 · compileSdk 35

---

## 1. Tổng quan sản phẩm

**mPOS Pro** biến một chiếc điện thoại Android thành terminal bán hàng đa kênh hoàn chỉnh, không cần phần cứng bổ sung.

| | |
|---|---|
| **Đối tượng** | Hộ kinh doanh vừa và nhỏ (SMB), nhân viên bán hàng tại quầy và online |
| **Điểm khác biệt** | 100% offline — không cần internet để bán hàng, sync khi có mạng |
| **Ngôn ngữ UI** | Tiếng Việt |
| **DB** | SQLite local — 14 bảng, seeded sẵn tài khoản `admin / admin123` |

---

## 2. Functional Requirements

### FR-01 · Đăng nhập & Phiên làm việc
| ID | Yêu cầu | Trạng thái |
|---|---|---|
| FR-01.1 | Đăng nhập bằng username + password (SHA-256 hash) | ✅ Done |
| FR-01.2 | Session lưu trong SharedPreferences, giữ đăng nhập qua lần mở app | ✅ Done |
| FR-01.3 | Đăng xuất xóa sạch session | ✅ Done |
| FR-01.4 | Tài khoản mẫu: `admin` / `admin123` được seed tự động khi cài lần đầu | ✅ Done |

### FR-02 · Quản lý Ca (Shift)
| ID | Yêu cầu | Trạng thái |
|---|---|---|
| FR-02.1 | Mở ca: nhập tiền đầu ca, lưu `opened_at` và `opening_cash` | ✅ Done |
| FR-02.2 | Đóng ca: nhập tiền thực tế, tính chênh lệch so với kỳ vọng | ✅ Done |
| FR-02.3 | Không thể xác nhận thanh toán khi chưa mở ca | ✅ Done |
| FR-02.4 | Báo cáo ca: xem lại thông tin ca gần nhất | ✅ Done |

### FR-03 · Bán hàng (POS Flow)
| ID | Yêu cầu | Trạng thái |
|---|---|---|
| FR-03.1 | Tìm kiếm sản phẩm theo tên, SKU, barcode | ✅ Done |
| FR-03.2 | Thêm sản phẩm vào giỏ hàng, giới hạn theo tồn kho | ✅ Done |
| FR-03.3 | Xem và xóa giỏ hàng | ✅ Done |
| FR-03.4 | Thanh toán: CASH / VNPAY_QR / MOMO_QR (mock) | ✅ Done |
| FR-03.5 | Nhập số điện thoại khách để tra cứu / tạo mới tự động | ✅ Done |
| FR-03.6 | Thanh toán tạo đơn trong 1 SQLite transaction (atomic) | ✅ Done |
| FR-03.7 | Sau thanh toán: trừ tồn kho, ghi movement, ghi audit log | ✅ Done |

### FR-04 · Hóa đơn (Receipt)
| ID | Yêu cầu | Trạng thái |
|---|---|---|
| FR-04.1 | Hiển thị hóa đơn ngay sau thanh toán thành công | ✅ Done |
| FR-04.2 | Nút In hóa đơn (stub — kết nối printer ở phase sau) | ✅ Done |
| FR-04.3 | Nút Gửi hóa đơn (stub — Zalo/Messenger ở phase sau) | ✅ Done |
| FR-04.4 | Nút Tạo đơn mới — quay lại POS | ✅ Done |

### FR-05 · Quản lý Sản phẩm
| ID | Yêu cầu | Trạng thái |
|---|---|---|
| FR-05.1 | Danh sách sản phẩm — tìm kiếm, thêm mới | ✅ Done |
| FR-05.2 | Form sản phẩm: tên, SKU, barcode, giá bán, tồn kho, ngưỡng cảnh báo | ✅ Done |
| FR-05.3 | Ẩn sản phẩm (soft delete — không xóa vĩnh viễn) | ✅ Done |

### FR-06 · Quản lý Khách hàng (CRM)
| ID | Yêu cầu | Trạng thái |
|---|---|---|
| FR-06.1 | Tìm kiếm khách theo tên / SĐT | ✅ Done |
| FR-06.2 | Thêm / sửa thông tin khách hàng | ✅ Done |
| FR-06.3 | Xem chi tiết: lịch sử đơn hàng, điểm tích lũy | ✅ Done |
| FR-06.4 | Auto-tạo khách khi nhập SĐT lúc checkout | ✅ Done |

### FR-07 · Quản lý Đơn hàng
| ID | Yêu cầu | Trạng thái |
|---|---|---|
| FR-07.1 | Danh sách tất cả đơn, sắp xếp theo thời gian mới nhất | ✅ Done |
| FR-07.2 | Chi tiết đơn: mã đơn, trạng thái, sản phẩm, tổng tiền | ✅ Done |

### FR-08 · Tồn kho (Inventory)
| ID | Yêu cầu | Trạng thái |
|---|---|---|
| FR-08.1 | Xem tồn kho tất cả sản phẩm, ưu tiên sản phẩm tồn thấp | ✅ Done |
| FR-08.2 | Điều chỉnh tồn kho thủ công (nhập kho / xuất kho / kiểm kho) | ✅ Done |
| FR-08.3 | Lịch sử biến động tồn kho | ✅ Done |

### FR-09 · Báo cáo
| ID | Yêu cầu | Trạng thái |
|---|---|---|
| FR-09.1 | Tổng đơn đã thanh toán và doanh thu | ✅ Done |
| FR-09.2 | Top 3 sản phẩm bán chạy | ✅ Done |

### FR-10 · Omnichannel Inbox
| ID | Yêu cầu | Trạng thái |
|---|---|---|
| FR-10.1 | Unified Inbox: xem tất cả đơn từ mọi kênh (WALK_IN, ZALO, FB...) | ✅ Done |
| FR-10.2 | Nhận diện và parse tin nhắn → điền form đơn hàng (phase 2) | 🔲 Backlog |

### FR-11 · Đồng bộ (Sync)
| ID | Yêu cầu | Trạng thái |
|---|---|---|
| FR-11.1 | Mọi hành động ghi vào `sync_queue` với status `PENDING` | ✅ Done |
| FR-11.2 | Xem trạng thái hàng chờ đồng bộ | ✅ Done |
| FR-11.3 | Nút "Đồng bộ ngay" — đánh dấu hàng chờ thành SYNCED (mock) | ✅ Done |
| FR-11.4 | Sync thật với Firebase / Backend REST (phase 2) | 🔲 Backlog |

---

## 3. Database Schema (14 bảng)

```
employees        → nhân viên
users            → tài khoản đăng nhập (FK: employee_id)
categories       → danh mục sản phẩm
products         → sản phẩm (FK: category_id)
customers        → khách hàng (key: phone)
shifts           → ca làm việc (FK: user_id)
orders           → đơn hàng (FK: customer_id, user_id, shift_id)
order_items      → dòng sản phẩm trong đơn (FK: order_id, product_id)
payments         → thanh toán (FK: order_id)
inventory_transactions → biến động tồn kho
receipts         → hóa đơn điện tử (FK: order_id)
settings         → cấu hình cửa hàng (key-value)
sync_queue       → hàng chờ đồng bộ
audit_logs       → nhật ký thao tác
```

**Seed data:** 1 employee `Administrator`, 1 user `admin/admin123` (role ADMIN), 1 category `Hàng mẫu`, 2 sản phẩm mẫu.

---

## 4. Navigation Flow

```
LoginActivity
    └── [đăng nhập thành công]
        └── MainActivity (Dashboard)
            ├── [Tạo đơn mới] ──────────────► PosActivity
            │                                    └── [Thanh toán] ──► CartActivity
            │                                                           └── [Tiếp tục] ──► CheckoutActivity
            │                                                                               └── [Xác nhận] ──► ReceiptActivity
            │                                                                                                   └── [Tạo đơn mới] → PosActivity
            ├── [Mở ca] ────────────────────► ShiftActivity
            │                                    └── [Xem báo cáo ca] ──► ShiftReportActivity
            ├── [Đơn hàng] ─────────────────► OrderListActivity
            │                                    └── [Tap đơn] ──► OrderDetailActivity
            ├── [Sản phẩm] ─────────────────► ProductListActivity
            │                                    └── [Thêm / Tap] ──► ProductFormActivity
            ├── [Khách hàng] ───────────────► CustomerListActivity
            │                                    ├── [Tap] ──► CustomerDetailActivity
            │                                    └── [Thêm] ──► CustomerFormActivity
            ├── [Tồn kho] ──────────────────► InventoryActivity
            │                                    ├── [Điều chỉnh] ──► StockAdjustmentActivity
            │                                    └── [Lịch sử] ──► InventoryHistoryActivity
            ├── [Báo cáo] ──────────────────► ReportActivity
            ├── [Unified Inbox] ─────────────► UnifiedInboxActivity
            ├── [Đồng bộ] ──────────────────► SyncStatusActivity
            ├── [Hồ sơ] ────────────────────► ProfileActivity
            │                                    └── [Cấu hình] ──► SettingsActivity
            └── [Thêm] (BottomNav) ──────────► MoreActivity

Bottom Navigation (5 tab):
  Trang chủ → MainActivity
  Bán hàng  → PosActivity
  Đơn       → OrderListActivity
  Sản phẩm  → ProductListActivity
  Thêm      → MoreActivity
```

---

## 5. Màn hình đã xây dựng (29 layouts)

| Layout file | Activity | Mô tả |
|---|---|---|
| `activity_login` | LoginActivity | Màn đăng nhập |
| `activity_main` | MainActivity | Dashboard chính |
| `activity_pos` | PosActivity | Giao diện bán hàng |
| `item_product_pos` | ProductPosAdapter | Item sản phẩm trong POS list |
| `activity_cart` | CartActivity | Giỏ hàng |
| `activity_checkout` | CheckoutActivity | Thanh toán |
| `activity_receipt` | ReceiptActivity | Hóa đơn sau thanh toán |
| `activity_shift` | ShiftActivity | Mở / Đóng ca |
| `activity_shift_report` | ShiftReportActivity | Báo cáo ca |
| `activity_order_list` | OrderListActivity | Danh sách đơn hàng |
| `activity_order_detail` | OrderDetailActivity | Chi tiết đơn |
| `activity_product_list` | ProductListActivity | Danh sách sản phẩm |
| `activity_product_form` | ProductFormActivity | Thêm / Sửa sản phẩm |
| `activity_customer_list` | CustomerListActivity | Danh sách khách hàng |
| `activity_customer_form` | CustomerFormActivity | Thêm / Sửa khách hàng |
| `activity_customer_detail` | CustomerDetailActivity | Chi tiết khách hàng |
| `activity_inventory` | InventoryActivity | Tồn kho |
| `activity_stock_adjustment` | StockAdjustmentActivity | Điều chỉnh tồn kho |
| `activity_inventory_history` | InventoryHistoryActivity | Lịch sử biến động |
| `activity_report` | ReportActivity | Báo cáo doanh thu |
| `activity_unified_inbox` | UnifiedInboxActivity | Unified Inbox |
| `activity_sync_status` | SyncStatusActivity | Trạng thái đồng bộ |
| `activity_profile` | ProfileActivity | Hồ sơ người dùng |
| `activity_settings` | SettingsActivity | Cấu hình cửa hàng |
| `activity_more` | MoreActivity | Menu tiện ích |
| `activity_audit_log` | AuditLogActivity | Nhật ký thao tác |
| `activity_shift_report` | ShiftReportActivity | Báo cáo ca |
| `include_bottom_nav` | (include) | Bottom navigation bar |
| `view_dashboard_stat` | (reusable) | Widget thống kê |

---

## 6. Design System

| Token | Giá trị | Dùng cho |
|---|---|---|
| `@color/blue_primary` | #0A84FF | Nút chính, accent |
| `@color/blue_navy` | #0B1F3A | Tiêu đề, hero text |
| `@color/app_background` | #F5F8FC | Background màn hình |
| `@color/surface` | #FFFFFFFF | Card, input, bottom nav |
| `@color/text_primary` | #111827 | Body text |
| `@color/text_secondary` | #667085 | Caption, placeholder |
| `@color/status_success` | #34C759 | Thành công, tồn kho OK |
| `@color/status_error` | #FF3B30 | Lỗi, tồn thấp |
| `@color/status_warning` | #FFB020 | Cảnh báo |

**Styles có sẵn:**
- `@style/Widget.Mpos.Button.Primary` — nút xanh chính
- `@style/Widget.Mpos.Button.Outline` — nút viền
- `@style/Widget.Mpos.Button.Ghost` — nút text
- `@style/TextAppearance.Mpos.Title` — tiêu đề màn hình

---

## 7. Backlog (Phase 2)

| # | Feature | Mô tả |
|---|---|---|
| 69 | Smart Chat Import | Parse tin nhắn Zalo/FB → auto fill form đơn hàng |
| 72 | Draft Order từ Inbox | Tạo draft order từ nội dung chat, nhân viên confirm |
| — | Barcode Scanner thật | Tích hợp CameraX + ML Kit Barcode |
| — | In hóa đơn | Bluetooth printer (Bluetooth SPP) |
| — | Gửi hóa đơn Zalo | Zalo SDK / Intent |
| — | Firebase Sync thật | Outbox → Firebase Realtime DB |
| — | FCM Notification | Push notification khi đơn có cập nhật |
| — | Logistics API | GHN / GHTK / ViettelPost tạo vận đơn |
| — | Refund | Hoàn đơn, ghi inventory ngược |
| — | Role-based access | ADMIN thấy tất cả, STAFF chỉ thấy POS |

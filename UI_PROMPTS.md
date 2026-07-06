# mPOS Pro — Bộ Prompt Gen UI

> Dùng từng block prompt bên dưới để gen từng file XML riêng.
> Mỗi prompt đã bao gồm đủ: design system + ID khớp Java + backend hook.

---

## SYSTEM PROMPT (dán đầu mỗi cuộc chat)

```
Bạn là Android UI expert. Viết Android XML layout cho project mPOS Pro.

DESIGN SYSTEM BẮT BUỘC:
- Root view: android:background="@color/app_background" + android:fitsSystemWindows="true"
- KHÔNG hardcode màu hex. Chỉ dùng color refs bên dưới.
- KHÔNG dùng Compose, DataBinding, ViewBinding annotation, CardView thủ công.
- Chỉ dùng View/ViewGroup gốc Android: LinearLayout, ScrollView, FrameLayout, ListView, RecyclerView, EditText, Button, TextView, Spinner, CheckBox.

COLOR REFS:
  @color/app_background | @color/surface | @color/blue_primary | @color/blue_primary_dark
  @color/blue_navy | @color/deep_navy_text | @color/text_primary | @color/text_secondary
  @color/status_error | @color/status_success | @color/status_warning

BUTTON STYLES:
  style="@style/Widget.Mpos.Button.Primary"   → CTA chính, dùng 1 lần/màn, height 56dp
  style="@style/Widget.Mpos.Button.Outline"   → thứ cấp, back, filter, height 44-52dp
  style="@style/Widget.Mpos.Button.Ghost"     → hủy, xóa, height 44-48dp

TEXT STYLES:
  style="@style/TextAppearance.Mpos.Title"    → tiêu đề màn hình

DRAWABLE BACKGROUNDS:
  @drawable/card_surface          → card trắng bo góc
  @drawable/card_soft_blue        → card xanh (tổng tiền, hero)
  @drawable/bg_hero_blue_gradient → gradient xanh đậm (banner)
  @drawable/bg_search_bar         → ô tìm kiếm
  @drawable/input_background      → EditText
  @drawable/bg_app_gradient       → nền login fullscreen
  @drawable/chip_selected         → filter chip active
  @drawable/chip_unselected       → filter chip inactive
  @drawable/badge_error           → badge đỏ
  @drawable/badge_success         → badge xanh
  @drawable/badge_warning         → badge vàng
  @drawable/badge_blue            → badge xanh dương

KÍCH THƯỚC:
  Screen padding: android:paddingHorizontal="@dimen/screen_gutter"
  Input/search height: 52dp  |  CTA button: 56dp  |  Icon button: 44×44dp
  Card margin giữa: 12dp     |  Section title margin top: 20dp

HEADER CHUẨN (mọi màn dùng):
  LinearLayout ngang, gravity center_vertical:
    Button ‹ (44×44, Outline, id=btnBack) + TextView Title (Mpos.Title, weight=1) + [action button nếu có]

BOTTOM NAV: chỉ include ở màn home-level:
  <include layout="@layout/include_bottom_nav" android:layout_height="76dp" android:layout_gravity="bottom"/>
  Root phải là FrameLayout khi có bottom nav.

ID CONVENTION: btnXxx | txtXxx | inputXxx | listXxx | recyclerXxx | spinnerXxx | chipXxx
```

---

## PROMPT 1 — `activity_report.xml`

```
Viết lại activity_report.xml cho màn hình Báo cáo doanh thu.

IDs BẮT BUỘC (Java dùng):
  btnBack          → Button quay lại
  txtReport        → TextView hiển thị text summary (ẩn, dùng làm fallback)

CẤU TRÚC:
Root: LinearLayout vertical, background @color/app_background, fitsSystemWindows

1. HEADER: btnBack (‹) + TextView "Báo cáo" (Mpos.Title)

2. FILTER CHIPS (ngang, marginTop 16dp):
   3 chip ngang scroll: "Hôm nay" | "7 ngày" | "Tháng này"
   chip active = @drawable/chip_selected, text @color/blue_primary_dark
   chip inactive = @drawable/chip_unselected, text @color/text_secondary
   IDs: chipToday, chip7Days, chipMonth

3. STAT ROW (3 card ngang bằng nhau, marginTop 14dp):
   Mỗi card: @drawable/card_soft_blue, padding 14dp, LinearLayout vertical
     TextView nhỏ (label): "DOANH THU" / "SỐ ĐƠN" / "SP BÁN RA", textColor #C8DDF8, 11sp
     TextView lớn (value): id txtRevenue / txtOrderCount / txtItemCount
       textColor @color/surface, 22sp, bold
   IDs: txtRevenue, txtOrderCount, txtItemCount

4. SECTION "Sản phẩm bán chạy" (marginTop 22dp):
   TextView "Sản phẩm bán chạy" (16sp, bold, @color/deep_navy_text)
   ListView id=listTopProducts, height 0dp, weight 1
   (adapter sẽ bind từ Java, layout item sẽ là item_report_product.xml)

5. SECTION "Kênh bán" (marginTop 16dp):
   TextView "Theo kênh" (16sp, bold)
   LinearLayout ngang 3 chip: Walk-in | Zalo | TikTok
   IDs: chipWalkin, chipZalo, chipTiktok

6. txtReport (TextView, visibility=gone, dùng fallback)

ScrollView bọc toàn bộ body (trừ header cố định).
KHÔNG có bottom nav.
```

---

## PROMPT 2 — `activity_unified_inbox.xml`

```
Viết lại activity_unified_inbox.xml cho màn Unified Order Inbox.

IDs BẮT BUỘC:
  btnBack    → quay lại
  txtInbox   → TextView fallback (visibility=gone)
  listInbox  → ListView chứa danh sách đơn từ các kênh

CẤU TRÚC:
Root: FrameLayout, background @color/app_background, fitsSystemWindows
(có bottom nav nên dùng FrameLayout)

1. LinearLayout vertical (paddingBottom 88dp để tránh bottom nav):

   HEADER: btnBack (‹) + TextView "Unified Inbox" (Mpos.Title, weight 1)
           + TextView badge đếm đơn: id=txtBadgeCount, background @drawable/badge_error,
             textColor white, paddingHorizontal 10dp, 12sp, text "0"

   FILTER ROW (marginTop 12dp, horizontal scroll):
     5 chip: "Tất cả" | "Walk-in" | "Zalo" | "TikTok" | "Shopee"
     IDs: chipAll, chipWalkin, chipZalo, chipTiktok, chipShopee
     chip_selected cho active, chip_unselected cho inactive

   EMPTY STATE (id=layoutEmpty, visibility=gone, marginTop 60dp, center):
     TextView "Không có đơn nào" (16sp, @color/text_secondary, center)
     TextView "Đơn từ tất cả kênh sẽ hiển thị ở đây" (13sp, @color/text_secondary)

   listInbox (layout_height=0dp, layout_weight=1, marginTop 10dp)
   txtInbox (visibility=gone)

2. <include layout="@layout/include_bottom_nav" gravity=bottom height=76dp/>
```

---

## PROMPT 3 — `activity_audit_log.xml`

```
Viết lại activity_audit_log.xml cho màn Lịch sử hoạt động (Audit Log).

IDs BẮT BUỘC:
  btnBack      → quay lại
  listAudit    → ListView danh sách log

CẤU TRÚC:
Root: LinearLayout vertical, @color/app_background, fitsSystemWindows, paddingHorizontal @dimen/screen_gutter

1. HEADER: btnBack (‹) + TextView "Lịch sử hoạt động" (Mpos.Title)

2. SEARCH BAR (marginTop 12dp):
   LinearLayout ngang:
     EditText id=inputAuditSearch, background @drawable/bg_search_bar,
       hint="Tìm theo hành động...", height 52dp, weight 1
     Button id=btnSearchAudit, Outline style, height 52dp, text "Tìm"

3. LinearLayout ngang chip filter (marginTop 10dp):
   chip "Tất cả" (id=chipAuditAll), "Bán hàng" (chipAuditSale),
   "Kho" (chipAuditStock), "Hệ thống" (chipAuditSystem)

4. listAudit: layout_height=0dp, layout_weight=1, marginTop 10dp
   divider=@android:color/transparent, dividerHeight 6dp

KHÔNG có bottom nav.
```

---

## PROMPT 4 — `activity_sync_status.xml`

```
Viết lại activity_sync_status.xml cho màn Đồng bộ dữ liệu.

IDs BẮT BUỘC:
  btnBack        → quay lại
  btnSyncNow     → Button kích hoạt sync thủ công
  txtSyncStatus  → TextView trạng thái tổng ("Đã đồng bộ" / "Đang chờ")
  txtPendingCount → TextView số lượng item chờ
  listSyncQueue  → ListView danh sách sync_queue items

CẤU TRÚC:
Root: LinearLayout vertical, @color/app_background, fitsSystemWindows,
      paddingHorizontal @dimen/screen_gutter

1. HEADER: btnBack (‹) + TextView "Đồng bộ dữ liệu" (Mpos.Title)

2. STATUS CARD (marginTop 16dp, @drawable/card_soft_blue, padding 20dp):
   LinearLayout vertical:
     TextView "TRẠNG THÁI" (11sp, #C8DDF8, bold)
     txtSyncStatus: "Tất cả đã đồng bộ" (20sp, bold, @color/surface, marginTop 8dp)
     LinearLayout ngang (marginTop 10dp):
       TextView "Chờ xử lý:" (@color/surface, 13sp)
       txtPendingCount: "0" (bold, @color/surface, 13sp, marginStart 6dp)
     btnSyncNow: Outline style, "Đồng bộ ngay", marginTop 14dp, width wrap_content

3. TextView "Chi tiết hàng đợi" (16sp, bold, @color/deep_navy_text, marginTop 22dp)

4. listSyncQueue: layout_height=0dp, layout_weight=1, marginTop 8dp

KHÔNG có bottom nav.
```

---

## PROMPT 5 — `activity_settings.xml`

```
Viết lại activity_settings.xml cho màn Cài đặt cửa hàng.

IDs BẮT BUỘC:
  btnBack         → quay lại
  inputStoreName  → EditText tên cửa hàng
  inputStorePhone → EditText số điện thoại
  inputStoreAddress → EditText địa chỉ
  inputVatPercent → EditText % VAT (number)
  switchVatEnabled → CheckBox bật/tắt VAT
  spinnerPrinterSize → Spinner khổ giấy 58mm/80mm
  btnSaveSettings → Button lưu (Primary)

CẤU TRÚC:
Root: ScrollView, @color/app_background, fitsSystemWindows

LinearLayout vertical bên trong, paddingHorizontal @dimen/screen_gutter, paddingBottom 24dp:

1. HEADER: btnBack (‹) + TextView "Cài đặt" (Mpos.Title)

2. SECTION "Thông tin cửa hàng" (card_surface, padding 18dp, marginTop 16dp):
   TextView "THÔNG TIN CỬA HÀNG" (11sp, @color/text_secondary, bold, marginBottom 14dp)
   Label "Tên cửa hàng" (13sp, @color/text_primary, bold)
   inputStoreName: input_background, 52dp, hint "VD: Shop Minh Anh"
   Label "Số điện thoại" (marginTop 12dp)
   inputStorePhone: input_background, 52dp, inputType phone
   Label "Địa chỉ" (marginTop 12dp)
   inputStoreAddress: input_background, 52dp

3. SECTION "Thuế VAT" (card_surface, padding 18dp, marginTop 12dp):
   TextView "CÀI ĐẶT THUẾ" (11sp, @color/text_secondary, bold, marginBottom 14dp)
   LinearLayout ngang gravity center_vertical:
     TextView "Áp dụng VAT" (@color/text_primary, weight 1)
     switchVatEnabled: CheckBox (không có text)
   LinearLayout ngang marginTop 12dp:
     TextView "% VAT" (@color/text_primary, weight 1)
     inputVatPercent: input_background, width 80dp, height 44dp, inputType number,
       text "10", gravity center, paddingHorizontal 12dp

4. SECTION "In hóa đơn" (card_surface, padding 18dp, marginTop 12dp):
   TextView "MÁY IN NHIỆT" (11sp, @color/text_secondary, bold, marginBottom 14dp)
   TextView "Khổ giấy" (@color/text_primary)
   spinnerPrinterSize: background @drawable/input_background, marginTop 8dp, height 52dp

5. btnSaveSettings: Primary style, "Lưu cài đặt", marginTop 24dp, height 56dp

KHÔNG có bottom nav.
```

---

## PROMPT 6 — `activity_profile.xml`

```
Viết lại activity_profile.xml cho màn Hồ sơ nhân viên.

IDs BẮT BUỘC:
  btnBack      → quay lại
  txtUsername  → TextView hiện username
  txtRole      → TextView hiện role (ADMIN/MANAGER/STAFF)
  txtEmail     → TextView hiện email (nếu có)
  btnLogout    → Button đăng xuất (Ghost style, màu error)

CẤU TRÚC:
Root: LinearLayout vertical, @color/app_background, fitsSystemWindows,
      paddingHorizontal @dimen/screen_gutter

1. HEADER: btnBack (‹) + TextView "Hồ sơ" (Mpos.Title)

2. AVATAR CARD (marginTop 24dp, @drawable/bg_hero_blue_gradient, padding 28dp, center):
   LinearLayout vertical, gravity center:
     TextView avatar chữ cái đầu: id=txtAvatar, 48×48dp,
       background @drawable/bg_hero_blue_gradient, gravity center,
       textColor @color/surface, 22sp, bold
     txtUsername: textColor @color/surface, 20sp, bold, marginTop 14dp
     txtRole: background @drawable/badge_blue, textColor white, 12sp,
       paddingHorizontal 12dp, paddingVertical 4dp, marginTop 8dp
     txtEmail: textColor #C8DDF8, 13sp, marginTop 4dp

3. INFO CARD (marginTop 16dp, @drawable/card_surface, padding 18dp):
   Mỗi row: LinearLayout ngang, paddingVertical 10dp, borderBottom divider
     TextView label (weight 1, @color/text_secondary, 13sp)
     TextView value (bold, @color/text_primary, 13sp)
   Rows: "Vai trò" / "Trạng thái" / "Đăng nhập lần đầu"

4. btnLogout: Ghost style, "Đăng xuất", marginTop 24dp,
   textColor @color/status_error, height 48dp

KHÔNG có bottom nav.
```

---

## PROMPT 7 — `activity_more.xml`

```
Viết lại activity_more.xml cho màn Thêm tiện ích (More menu).

IDs BẮT BUỘC:
  btnBack         → quay lại
  btnAuditLog     → Button Lịch sử hoạt động
  btnSyncStatus   → Button Đồng bộ dữ liệu
  btnSettings     → Button Cài đặt
  btnShiftReport  → Button Báo cáo ca

CẤU TRÚC:
Root: FrameLayout, @color/app_background, fitsSystemWindows (có bottom nav)

LinearLayout vertical bên trong, paddingHorizontal @dimen/screen_gutter, paddingBottom 88dp:

1. HEADER: btnBack (‹) + TextView "Thêm" (Mpos.Title)

2. MENU LIST (marginTop 16dp):
   Mỗi item là LinearLayout ngang, @drawable/card_surface, marginBottom 8dp,
     padding 18dp, gravity center_vertical:
       TextView icon (32×32dp, @drawable/bg_hero_blue_gradient, gravity center,
         textColor @color/surface, 16sp, bold) — dùng emoji: 📋 / 🔄 / ⚙️ / 📊
       LinearLayout vertical (weight 1, marginStart 14dp):
         TextView tên mục (15sp, bold, @color/deep_navy_text)
         TextView mô tả ngắn (12sp, @color/text_secondary)
       TextView "›" (20sp, @color/text_secondary)

   Items:
   - btnAuditLog    | "Lịch sử hoạt động" | "Xem log toàn bộ thao tác"
   - btnSyncStatus  | "Đồng bộ dữ liệu"   | "Trạng thái hàng đợi sync"
   - btnShiftReport | "Báo cáo ca"         | "Xem doanh thu theo ca"
   - btnSettings    | "Cài đặt"            | "Cấu hình cửa hàng & máy in"

3. <include layout="@layout/include_bottom_nav" gravity=bottom height=76dp/>
```

---

## PROMPT 8 — `item_customer.xml` (item cho CustomerListActivity)

```
Viết file item_customer.xml — item row cho ListView danh sách khách hàng.
File này được dùng bởi ArrayAdapter trong CustomerListActivity.

IDs BẮT BUỘC:
  txtCustomerName   → tên khách hàng
  txtCustomerPhone  → số điện thoại
  txtLoyaltyPoints  → điểm tích luỹ
  txtChannelBadge   → badge nguồn kênh (Walk-in / Zalo / TikTok...)

CẤU TRÚC:
Root: LinearLayout ngang, background @drawable/card_surface,
      marginHorizontal 0dp, marginBottom 6dp, padding 14dp,
      gravity center_vertical

1. TextView avatar: 40×40dp, @drawable/bg_hero_blue_gradient,
   gravity center, textColor @color/surface, 16sp, bold
   (Java sẽ set text = chữ cái đầu tên)

2. LinearLayout vertical (weight 1, marginStart 12dp):
   txtCustomerName: 14sp, bold, @color/deep_navy_text
   LinearLayout ngang (marginTop 3dp):
     txtCustomerPhone: 12sp, @color/text_secondary
     txtChannelBadge: 10sp, @color/surface, @drawable/badge_blue,
       paddingHorizontal 8dp, paddingVertical 2dp, marginStart 8dp

3. LinearLayout vertical gravity=center, marginStart 8dp:
   txtLoyaltyPoints: 15sp, bold, @color/blue_primary_dark
   TextView "điểm": 10sp, @color/text_secondary, gravity center
```

---

## PROMPT 9 — `item_order.xml` (item cho OrderListActivity)

```
Viết file item_order.xml — item row cho ListView danh sách đơn hàng.

IDs BẮT BUỘC:
  txtOrderCode     → mã đơn (VD: ORD-1234567890)
  txtOrderStatus   → trạng thái (PAID/PENDING/REFUNDED)
  txtOrderTotal    → tổng tiền (format VND)
  txtOrderTime     → thời gian tạo đơn
  txtOrderChannel  → kênh bán (WALK_IN / ZALO / TIKTOK...)

CẤU TRÚC:
Root: LinearLayout vertical, @drawable/card_surface,
      marginBottom 8dp, padding 16dp

Row trên: LinearLayout ngang gravity center_vertical:
  txtOrderCode: 14sp, bold, @color/deep_navy_text, weight 1
  txtOrderStatus: 11sp, @color/surface, paddingHorizontal 10dp, paddingVertical 4dp
    Java set background: badge_success (PAID) | badge_warning (PENDING) | badge_error (REFUNDED)

Row giữa (marginTop 6dp): LinearLayout ngang:
  txtOrderChannel: 11sp, @color/surface, @drawable/badge_blue,
    paddingHorizontal 8dp, paddingVertical 2dp
  View spacer weight=1
  txtOrderTotal: 16sp, bold, @color/blue_primary_dark

txtOrderTime: 11sp, @color/text_secondary, marginTop 4dp
```

---

## PROMPT 10 — `item_product_list.xml` (item cho ProductListActivity)

```
Viết file item_product_list.xml — item row cho ListView danh sách sản phẩm.

IDs BẮT BUỘC:
  txtProductName   → tên sản phẩm
  txtProductSku    → SKU
  txtProductPrice  → giá bán (VND)
  txtStockQty      → số lượng tồn kho
  txtStockBadge    → badge tồn kho (Còn hàng/Sắp hết/Hết hàng)

CẤU TRÚC:
Root: LinearLayout ngang, @drawable/card_surface, marginBottom 6dp, padding 14dp, gravity center_vertical

1. LinearLayout vertical weight=1:
   txtProductName: 14sp, bold, @color/deep_navy_text
   txtProductSku: 11sp, @color/text_secondary, marginTop 2dp
   txtProductPrice: 15sp, bold, @color/blue_primary_dark, marginTop 4dp

2. LinearLayout vertical gravity=end:
   txtStockQty: 18sp, bold, @color/deep_navy_text, gravity end
   txtStockBadge: 10sp, @color/surface, paddingHorizontal 8dp, paddingVertical 3dp
     Java set: badge_success (>10) | badge_warning (1-10) | badge_error (0)
```

---

## PROMPT 11 — `item_inventory.xml` (item cho InventoryActivity)

```
Viết file item_inventory.xml — item cho ListView tồn kho.

IDs BẮT BUỘC:
  txtInvProductName → tên sản phẩm
  txtInvQty         → số lượng hiện tại
  txtInvMinQty      → tồn kho tối thiểu
  txtInvStatus      → badge trạng thái kho

CẤU TRÚC:
Root: LinearLayout ngang, @drawable/card_surface, marginBottom 6dp, padding 14dp, gravity center_vertical

1. LinearLayout vertical weight=1:
   txtInvProductName: 14sp, bold, @color/deep_navy_text
   LinearLayout ngang marginTop 4dp:
     TextView "Tối thiểu: " (12sp, @color/text_secondary)
     txtInvMinQty: 12sp, @color/text_secondary, bold

2. LinearLayout vertical gravity=end:
   txtInvQty: 22sp, bold, @color/deep_navy_text
   txtInvStatus: 10sp, @color/surface, paddingHorizontal 10dp, paddingVertical 3dp
     (Java set badge_success / badge_warning / badge_error)
```

---

## PROMPT 12 — `item_sync_queue.xml` (item cho SyncStatusActivity)

```
Viết file item_sync_queue.xml — item cho ListView hàng đợi sync.

IDs BẮT BUỘC:
  txtSyncEntity    → loại entity (ORDER / CUSTOMER / PRODUCT)
  txtSyncAction    → hành động (CREATE / UPDATE / DELETE)
  txtSyncStatus    → trạng thái (PENDING / DONE / FAILED)
  txtSyncTime      → thời gian tạo

CẤU TRÚC:
Root: LinearLayout ngang, @drawable/card_surface, marginBottom 6dp, padding 14dp, gravity center_vertical

1. LinearLayout vertical weight=1:
   LinearLayout ngang:
     txtSyncEntity: 13sp, bold, @color/deep_navy_text
     TextView " • ": @color/text_secondary, 13sp
     txtSyncAction: 13sp, @color/blue_primary_dark
   txtSyncTime: 11sp, @color/text_secondary, marginTop 3dp

2. txtSyncStatus: 11sp, @color/surface, paddingHorizontal 10dp, paddingVertical 4dp
   (Java set: badge_warning=PENDING | badge_success=DONE | badge_error=FAILED)
```

---

## GHI CHÚ QUAN TRỌNG KHI GEN

### Thứ tự gen nên theo:
1. Item layouts trước (item_*.xml) — 5 files
2. Activity layouts cần sửa (report, unified_inbox) — 2 files
3. Activity layouts stub (audit_log, sync_status, settings, profile, more) — 5 files

### Sau khi có XML, áp vào Java như sau:

**Thay ArrayAdapter đơn giản bằng custom adapter:**
```java
// Thay dòng này:
new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, rows)

// Bằng custom adapter dùng item_customer.xml:
// Tạo class CustomerAdapter extends ArrayAdapter<Customer>
// inflate R.layout.item_customer trong getView()
// bind txtCustomerName, txtCustomerPhone, txtLoyaltyPoints
```

### Truyền intent extra — không được sai:
```java
// Gửi:
intent.putExtra("order_id", orderId);       // long
intent.putExtra("customer_id", customerId); // long
intent.putExtra("product_id", productId);   // long

// Nhận:
long id = getIntent().getLongExtra("order_id", -1);
if (id == -1) { finish(); return; } // guard bắt buộc
```

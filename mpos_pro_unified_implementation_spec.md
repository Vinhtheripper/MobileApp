# mPOS Pro — Unified Implementation Spec for Coding Agent

> Use this file as the single source of truth when implementing the Android project.  
> Goal: build a clean, stable, offline-first Android POS app for small shops, based on the unified business flow below.

---

## 0. Project Decision

### Target app
**mPOS Pro** is an Android POS application for small shops selling across multiple channels:

- Walk-in store sales
- Zalo / Facebook / TikTok chat orders
- TikTok Shop
- Shopee
- Website
- Livestream / Live Sale

The core idea: **one app, one order inbox, one inventory source, one customer profile.**

### Current implementation rule
Use the current project style unless the existing codebase clearly uses another stack.

Recommended for this assignment:

- Language: **Java**
- UI: **XML layouts**
- Database: **SQLite using SQLiteOpenHelper**
- Architecture: Activity / Fragment + Repository / DAO-like helper classes
- Data mode: **local-first / offline-first**
- External platforms: use **mock services** first, not real TikTok/Shopee/GHN APIs

Do **not** rewrite the whole project to Kotlin, Room, Jetpack Compose, or Firebase unless the existing project already uses them and the user explicitly asks for it.

---

## 1. Unified Business Flow

The whole system must follow this single business flow:

```text
Configure System
→ Login & Open Shift
→ Receive Order
→ Resolve Customer
→ Create Cart
→ Check & Lock Inventory
→ Checkout & Payment
→ Generate Receipt
→ Fulfillment / Delivery
→ Sync Data
→ Reports & Analytics
→ Close Shift
```

The system must use **Order Lifecycle** as the backbone:

```text
ORDER_DRAFT
→ ORDER_CONFIRMED
→ INVENTORY_LOCKED
→ PAYMENT_PENDING
→ PAID
→ FULFILLMENT_PENDING
→ SHIPPING
→ DELIVERED / COMPLETED
```

Cancel / failure branches:

```text
ORDER_CANCELLED
PAYMENT_FAILED
DELIVERY_FAILED
REFUNDED
SYNC_PENDING
SYNC_FAILED
```

---

## 2. Main Actors

### 2.1 Admin
Admin configures and controls the whole system.

Admin can:

- Manage store profile
- Manage products, categories, SKU, barcode, price, VAT, stock
- Manage sales channels
- Manage employees and roles
- Configure payment methods
- Configure shipping providers
- Configure low-stock threshold
- View all reports
- View audit logs

### 2.2 Manager
Manager supervises operation.

Manager can:

- View all orders
- View shift reports
- Manage inventory
- Handle fulfillment queue
- Handle delivery failure / refund / return
- View reports by channel, staff, product, date
- Resolve sync or inventory conflicts

### 2.3 Staff
Staff handles daily selling.

Staff can:

- Login
- Open shift
- Create order
- Import order from chat
- Add products to cart
- Apply discount
- Process payment
- Generate receipt
- Send / print receipt
- Close shift
- Add shift handover note

### 2.4 External Systems / Mock Services
For coding, implement these as mock services first:

- PaymentServiceMock
- ShippingServiceMock
- ChannelSyncServiceMock
- NotificationServiceMock

They should simulate success/failure without requiring real external API keys.

---

## 3. Core Modules to Implement

Implement the app by modules. Do not code random screens without connecting them to the order lifecycle.

### Module 1 — Authentication & Shift Management
Purpose:

- User logs in.
- Staff must open a shift before selling.
- Staff closes shift at the end of work.

Required screens:

- LoginActivity
- DashboardActivity / MainActivity
- ShiftActivity or ShiftFragment
- ShiftReportActivity / ShiftReportFragment

Required features:

- Login with local SQLite user table.
- Default seed accounts:
  - admin / admin123 / ADMIN
  - manager / manager123 / MANAGER
  - staff / staff123 / STAFF
- Save session using SharedPreferences.
- Check role permissions.
- Open shift with starting cash amount.
- Prevent order checkout if no active shift.
- Close shift with actual cash amount.
- Calculate expected cash, actual cash, and difference.
- Allow optional handover note.

Business rules:

- Staff cannot sell if no active shift.
- One staff should only have one active shift at a time.
- A closed shift cannot be reopened.
- Manager/Admin can view all shifts; Staff can view only their own shift.

---

### Module 2 — Omnichannel Order Intake
Purpose:

- Receive orders from many channels but process them in one unified flow.

Order sources:

- WALK_IN
- ZALO
- FACEBOOK
- TIKTOK_CHAT
- TIKTOK_SHOP
- SHOPEE
- WEBSITE
- LIVE_SALE

Required screens:

- UnifiedOrderInboxActivity / Fragment
- NewOrderActivity / Fragment
- ChatImportActivity / Dialog
- OrderDetailActivity / Fragment

Required features:

- Show all orders in one inbox.
- Each order must have a source tag and colored badge.
- Allow filtering by source, status, date.
- Allow creating Walk-in order manually.
- Allow creating Chat order by pasting text.
- Mock importing Shopee/TikTok/Website orders.
- Sort inbox by priority:
  1. SLA / urgent orders
  2. Newest order time
  3. Source priority

Business rules:

- Every order must have a source.
- Online order can be saved as DRAFT if missing customer or address.
- Order cannot be confirmed if cart is empty.
- Shipping order must have delivery address.

---

### Module 3 — Chat Order Parser
Purpose:

- Help staff create order quickly from customer chat.

Input example:

```text
Lan Nguyen, 0912345678, 2 ao size M, giao Q1 HCM
```

Required parser behavior:

- Detect phone number using regex.
- Try to detect customer name.
- Try to detect address after keywords like `giao`, `dia chi`, `address`.
- Try to detect product keyword and quantity.
- Create a draft order with parsed data.
- Staff must be able to review and edit before confirming.

Implementation rule:

- Keep parser simple and understandable.
- Do not over-engineer NLP.
- Use regex and keyword matching.

---

### Module 4 — Customer Management & Identity Resolution
Purpose:

- Keep one customer profile across channels.

Required screens:

- CustomerListActivity / Fragment
- CustomerDetailActivity / Fragment
- CustomerFormActivity / Fragment

Required features:

- Add / edit customer.
- Search by name or phone.
- Use phone as the main identity key.
- Display purchase history.
- Display source tags.
- Display total orders and total spending.
- If phone already exists, reuse existing customer.
- If duplicate customers are found with the same phone, merge or suggest merge.

Business rules:

- Phone number should be unique if available.
- A customer can have multiple source tags.
- A customer can have multiple addresses.
- A new order should always be linked to a customer when phone is available.

---

### Module 5 — Product, Category & Inventory Management
Purpose:

- Manage products and stock.

Required screens:

- ProductListActivity / Fragment
- ProductFormActivity / Fragment
- CategoryActivity / Fragment
- InventoryActivity / Fragment
- StockAdjustmentActivity / Fragment

Required features:

- Add / edit / hide product.
- Add / edit category.
- Search products by name, SKU, barcode.
- Show stock chip on product card.
- Show low-stock warning.
- Stock in.
- Stock adjustment with reason.
- Inventory movement history.

Business rules:

- Do not hard-delete product if it exists in past order items.
- Use `is_active = 0` for hidden/deleted products.
- Every stock change must create an InventoryMovement record.
- Do not allow negative stock unless explicitly configured.

---

### Module 6 — POS Cart & Sales Order Management
Purpose:

- Let staff create and confirm order.

Required screens:

- POSActivity / POSFragment
- CartActivity / CartFragment
- CheckoutActivity / CheckoutFragment
- OrderDetailActivity / Fragment

Required features:

- Add product by search.
- Add product by barcode text input first; camera scanning can be added later.
- Add product from category.
- Edit quantity.
- Remove cart item.
- Apply discount code manually.
- Calculate subtotal, discount, VAT, total.
- Inline stock warning.
- Smart upsell suggestion using simple co-purchase history.
- Repeat previous order for existing customer.

Business rules:

- Cart item quantity must be greater than 0.
- Cart cannot exceed available stock.
- If stock is insufficient, show warning and disable confirm.
- Confirming an order triggers inventory lock.
- Order total must be recalculated every time item, quantity, discount, or VAT changes.

Formula:

```text
line_total = quantity × unit_price × (1 - discount_rate) × (1 + vat_rate)
order_total = sum(line_total) + shipping_fee - order_discount
```

---

### Module 7 — Cross-platform Inventory Lock
Purpose:

- Prevent overselling across channels.

Required behavior:

When an order is confirmed:

1. Check stock locally.
2. Deduct stock immediately in SQLite.
3. Create InventoryMovement with type `SALE_LOCK`.
4. Create SyncQueue record with type `ORDER_CONFIRMED` or `INVENTORY_LOCK`.
5. If stock becomes 0, mark product as out of stock for all channels.
6. Send mock notification to Admin/Manager.

Business rules:

- Inventory lock must happen before payment completion.
- If payment fails and order is cancelled, restore stock.
- If sync fails, local order remains valid but sync status becomes PENDING/FAILED.
- Never allow confirmed order to silently ignore stock deduction.

---

### Module 8 — Checkout, Payment & Receipt
Purpose:

- Process payment and generate receipt.

Payment methods:

- CASH
- QR
- MOMO
- VNPAY
- CARD_NFC
- SPLIT

Required screens:

- CheckoutActivity / Fragment
- PaymentResultActivity / Dialog
- ReceiptActivity / Fragment

Required features:

- Select payment method.
- Cash payment: enter received amount and calculate change.
- QR/e-wallet/card: call PaymentServiceMock.
- Split payment: allow multiple payment rows for one order.
- Show payment success/failure.
- Generate receipt view.
- Share receipt text/image placeholder.
- Print receipt placeholder.

Business rules:

- Payment amount must equal order total.
- If payment succeeds, order status becomes PAID.
- If payment fails, order status becomes PAYMENT_FAILED or PAYMENT_PENDING.
- Receipt can only be generated for paid order, except draft preview.

Receipt must include:

- Store name
- Order code
- Date/time
- Staff name
- Customer name/phone if available
- Items
- Quantity
- Unit price
- Discount
- VAT
- Total
- Payment method

---

### Module 9 — Fulfillment & Logistics
Purpose:

- Manage packing and delivery.

Required screens:

- FulfillmentQueueActivity / Fragment
- ShipmentActivity / Fragment
- ShipmentDetailActivity / Fragment

Required features:

- Put paid delivery orders into fulfillment queue.
- Show Kanban-like statuses:
  - PENDING_PACKING
  - PACKING
  - WAITING_SHIPPER
  - SHIPPING
  - DELIVERED
  - DELIVERY_FAILED
- Create shipment using ShippingServiceMock.
- Generate mock tracking code.
- Track shipment timeline.
- Compare shipping providers mock result:
  - GHN
  - GHTK
  - ViettelPost

Business rules:

- Walk-in orders can be completed immediately.
- Delivery orders must go through fulfillment queue.
- A shipment cannot be created without customer address.
- Failed delivery should allow retry, return, or refund flag.

---

### Module 10 — Offline Sync Queue
Purpose:

- Simulate offline-first behavior.

Required screens:

- SyncStatusActivity / Fragment

Required features:

- Create SyncQueue row for important writes.
- Show sync status badge:
  - PENDING
  - SYNCED
  - FAILED
- Add manual “Sync Now” button.
- Mock network available/unavailable toggle.
- Retry failed sync.

SyncQueue event types:

- ORDER_CREATE
- ORDER_CONFIRM
- PAYMENT_COMPLETE
- INVENTORY_LOCK
- CUSTOMER_CREATE
- CUSTOMER_UPDATE
- SHIPMENT_CREATE
- SHIFT_OPEN
- SHIFT_CLOSE

Business rules:

- Local write must succeed even if mock network is offline.
- If mock network is online, mark sync as SYNCED.
- If mock service fails, mark as FAILED and allow retry.

---

### Module 11 — Reports & Analytics
Purpose:

- Help Manager/Admin monitor business performance.

Required screens:

- ReportDashboardActivity / Fragment
- ChannelReportActivity / Fragment
- StaffReportActivity / Fragment
- InventoryReportActivity / Fragment

Required reports:

- Today revenue
- Number of orders today
- Revenue by date
- Revenue by channel
- Orders by channel
- Best-selling products
- Low-stock products
- Staff performance by shift
- Cash difference by shift

Implementation rule:

- Use SQLite aggregate queries.
- Charts are optional. If chart library is not available, use cards and tables first.

---

### Module 12 — Admin Settings & Employee Management
Purpose:

- Let Admin configure the system.

Required screens:

- AdminSettingsActivity / Fragment
- EmployeeListActivity / Fragment
- EmployeeFormActivity / Fragment
- StoreConfigActivity / Fragment
- ChannelConfigActivity / Fragment

Required features:

- CRUD employees.
- Assign role ADMIN / MANAGER / STAFF.
- Configure store info.
- Configure VAT.
- Configure low-stock threshold.
- Configure payment methods.
- Configure sales channels.

Business rules:

- Only Admin can manage employees and roles.
- Manager can view reports and inventory but should not manage Admin account.
- Staff cannot access Admin settings.

---

## 4. Recommended SQLite Schema

Use these tables or adapt to existing project naming. Keep names consistent.

### users

```sql
CREATE TABLE users (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    username TEXT NOT NULL UNIQUE,
    password_hash TEXT NOT NULL,
    full_name TEXT,
    role TEXT NOT NULL CHECK(role IN ('ADMIN','MANAGER','STAFF')),
    is_active INTEGER NOT NULL DEFAULT 1,
    created_at TEXT NOT NULL,
    updated_at TEXT
);
```

### shifts

```sql
CREATE TABLE shifts (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    user_id INTEGER NOT NULL,
    opened_at TEXT NOT NULL,
    closed_at TEXT,
    opening_cash REAL NOT NULL DEFAULT 0,
    expected_cash REAL NOT NULL DEFAULT 0,
    actual_cash REAL,
    cash_difference REAL,
    handover_note TEXT,
    status TEXT NOT NULL CHECK(status IN ('OPEN','CLOSED')),
    FOREIGN KEY(user_id) REFERENCES users(id)
);
```

### categories

```sql
CREATE TABLE categories (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    name TEXT NOT NULL,
    icon TEXT,
    color TEXT,
    is_active INTEGER NOT NULL DEFAULT 1
);
```

### products

```sql
CREATE TABLE products (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    category_id INTEGER,
    name TEXT NOT NULL,
    sku TEXT UNIQUE,
    barcode TEXT UNIQUE,
    price REAL NOT NULL,
    vat_rate REAL NOT NULL DEFAULT 0,
    stock_qty INTEGER NOT NULL DEFAULT 0,
    low_stock_threshold INTEGER NOT NULL DEFAULT 5,
    image_uri TEXT,
    is_active INTEGER NOT NULL DEFAULT 1,
    created_at TEXT NOT NULL,
    updated_at TEXT,
    FOREIGN KEY(category_id) REFERENCES categories(id)
);
```

### customers

```sql
CREATE TABLE customers (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    name TEXT,
    phone TEXT UNIQUE,
    email TEXT,
    address TEXT,
    source_tags TEXT,
    total_spent REAL NOT NULL DEFAULT 0,
    total_orders INTEGER NOT NULL DEFAULT 0,
    created_at TEXT NOT NULL,
    updated_at TEXT
);
```

### orders

```sql
CREATE TABLE orders (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    order_code TEXT NOT NULL UNIQUE,
    customer_id INTEGER,
    staff_id INTEGER NOT NULL,
    shift_id INTEGER,
    source TEXT NOT NULL,
    status TEXT NOT NULL,
    subtotal REAL NOT NULL DEFAULT 0,
    discount_total REAL NOT NULL DEFAULT 0,
    vat_total REAL NOT NULL DEFAULT 0,
    shipping_fee REAL NOT NULL DEFAULT 0,
    grand_total REAL NOT NULL DEFAULT 0,
    delivery_address TEXT,
    note TEXT,
    sync_status TEXT NOT NULL DEFAULT 'PENDING',
    created_at TEXT NOT NULL,
    updated_at TEXT,
    FOREIGN KEY(customer_id) REFERENCES customers(id),
    FOREIGN KEY(staff_id) REFERENCES users(id),
    FOREIGN KEY(shift_id) REFERENCES shifts(id)
);
```

### order_items

```sql
CREATE TABLE order_items (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    order_id INTEGER NOT NULL,
    product_id INTEGER NOT NULL,
    product_name TEXT NOT NULL,
    quantity INTEGER NOT NULL,
    unit_price REAL NOT NULL,
    discount_rate REAL NOT NULL DEFAULT 0,
    vat_rate REAL NOT NULL DEFAULT 0,
    line_total REAL NOT NULL,
    FOREIGN KEY(order_id) REFERENCES orders(id),
    FOREIGN KEY(product_id) REFERENCES products(id)
);
```

### payments

```sql
CREATE TABLE payments (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    order_id INTEGER NOT NULL,
    method TEXT NOT NULL,
    amount REAL NOT NULL,
    status TEXT NOT NULL,
    transaction_ref TEXT,
    paid_at TEXT,
    FOREIGN KEY(order_id) REFERENCES orders(id)
);
```

### inventory_movements

```sql
CREATE TABLE inventory_movements (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    product_id INTEGER NOT NULL,
    type TEXT NOT NULL,
    quantity_change INTEGER NOT NULL,
    before_qty INTEGER NOT NULL,
    after_qty INTEGER NOT NULL,
    reason TEXT,
    related_order_id INTEGER,
    created_by INTEGER,
    created_at TEXT NOT NULL,
    FOREIGN KEY(product_id) REFERENCES products(id),
    FOREIGN KEY(related_order_id) REFERENCES orders(id),
    FOREIGN KEY(created_by) REFERENCES users(id)
);
```

### shipments

```sql
CREATE TABLE shipments (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    order_id INTEGER NOT NULL,
    provider TEXT NOT NULL,
    tracking_code TEXT,
    fee REAL NOT NULL DEFAULT 0,
    status TEXT NOT NULL,
    created_at TEXT NOT NULL,
    updated_at TEXT,
    FOREIGN KEY(order_id) REFERENCES orders(id)
);
```

### sync_queue

```sql
CREATE TABLE sync_queue (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    event_type TEXT NOT NULL,
    entity_type TEXT NOT NULL,
    entity_id INTEGER NOT NULL,
    payload TEXT,
    status TEXT NOT NULL DEFAULT 'PENDING',
    retry_count INTEGER NOT NULL DEFAULT 0,
    last_error TEXT,
    created_at TEXT NOT NULL,
    updated_at TEXT
);
```

### audit_logs

```sql
CREATE TABLE audit_logs (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    user_id INTEGER,
    action TEXT NOT NULL,
    entity_type TEXT,
    entity_id INTEGER,
    description TEXT,
    created_at TEXT NOT NULL,
    FOREIGN KEY(user_id) REFERENCES users(id)
);
```

---

## 5. Required Status Enums

Use constants in Java instead of magic strings.

### Role

```text
ADMIN
MANAGER
STAFF
```

### OrderSource

```text
WALK_IN
ZALO
FACEBOOK
TIKTOK_CHAT
TIKTOK_SHOP
SHOPEE
WEBSITE
LIVE_SALE
```

### OrderStatus

```text
DRAFT
CONFIRMED
INVENTORY_LOCKED
PAYMENT_PENDING
PAID
FULFILLMENT_PENDING
PACKING
WAITING_SHIPPER
SHIPPING
DELIVERED
COMPLETED
CANCELLED
PAYMENT_FAILED
DELIVERY_FAILED
REFUNDED
```

### PaymentStatus

```text
PENDING
SUCCESS
FAILED
REFUNDED
```

### SyncStatus

```text
PENDING
SYNCED
FAILED
```

### InventoryMovementType

```text
STOCK_IN
STOCK_ADJUST
SALE_LOCK
SALE_RESTORE
LOW_STOCK_ALERT
```

---

## 6. UI / UX Direction

Use a modern blue/navy/cyan POS style.

### Style direction

- Clean dashboard layout
- Large touch targets
- Rounded cards
- Bottom navigation for main staff functions
- Status chips for order source, payment, stock, sync
- Clear empty states
- Clear error messages
- Staff should complete a basic order quickly

### Main navigation for Staff

Bottom nav tabs:

1. Home
2. POS
3. Orders
4. Inventory
5. More

### Main navigation for Manager/Admin

Can include:

1. Dashboard
2. Orders
3. Products
4. Reports
5. Admin

### Important UI rules

- Always show current shift status on dashboard.
- Always show sync status badge.
- In POS cart, show stock warning inline.
- In order detail, show source badge and status timeline.
- In checkout, make total amount large and readable.
- Do not hide important errors in Toast only; show visible message/card.

---

## 7. Suggested Package Structure

Adapt to existing package name.

```text
com.example.mpospro
│
├── data
│   ├── db
│   │   ├── AppDatabaseHelper.java
│   │   ├── DatabaseContract.java
│   │   └── SeedData.java
│   │
│   ├── model
│   │   ├── User.java
│   │   ├── Shift.java
│   │   ├── Product.java
│   │   ├── Category.java
│   │   ├── Customer.java
│   │   ├── Order.java
│   │   ├── OrderItem.java
│   │   ├── Payment.java
│   │   ├── Shipment.java
│   │   ├── InventoryMovement.java
│   │   └── SyncQueueItem.java
│   │
│   ├── repository
│   │   ├── AuthRepository.java
│   │   ├── ShiftRepository.java
│   │   ├── ProductRepository.java
│   │   ├── CustomerRepository.java
│   │   ├── OrderRepository.java
│   │   ├── PaymentRepository.java
│   │   ├── InventoryRepository.java
│   │   ├── ShipmentRepository.java
│   │   ├── ReportRepository.java
│   │   └── SyncRepository.java
│
├── service
│   ├── ChatOrderParser.java
│   ├── PaymentServiceMock.java
│   ├── ShippingServiceMock.java
│   ├── ChannelSyncServiceMock.java
│   ├── NotificationServiceMock.java
│   └── ReceiptGenerator.java
│
├── ui
│   ├── auth
│   ├── dashboard
│   ├── shift
│   ├── pos
│   ├── order
│   ├── customer
│   ├── product
│   ├── inventory
│   ├── payment
│   ├── fulfillment
│   ├── sync
│   ├── report
│   └── admin
│
├── util
│   ├── Constants.java
│   ├── DateTimeUtil.java
│   ├── MoneyUtil.java
│   ├── SessionManager.java
│   ├── ValidationUtil.java
│   └── UiStateHelper.java
```

---

## 8. Implementation Order

Follow this order. Do not start with advanced omnichannel integration before the core POS flow works.

### Phase 1 — Foundation

1. Create SQLite schema.
2. Create model classes.
3. Create repositories.
4. Seed admin / manager / staff accounts.
5. Implement SessionManager.
6. Implement Login.
7. Implement Dashboard.

### Phase 2 — Core POS

1. Implement Open Shift / Close Shift.
2. Implement Product list and seed products.
3. Implement Customer create/search.
4. Implement POS cart.
5. Implement order creation.
6. Implement stock check.
7. Implement inventory lock.
8. Implement checkout with cash payment.
9. Implement receipt screen.

### Phase 3 — Order Management

1. Implement Unified Order Inbox.
2. Implement order detail.
3. Implement order status timeline.
4. Implement chat order parser.
5. Implement repeat previous order.
6. Implement simple smart upsell.

### Phase 4 — Fulfillment & Sync

1. Implement fulfillment queue.
2. Implement mock shipping provider comparison.
3. Implement shipment creation.
4. Implement sync queue.
5. Implement mock network toggle.
6. Implement retry sync.

### Phase 5 — Reports & Admin

1. Implement revenue report.
2. Implement channel report.
3. Implement product report.
4. Implement staff/shift report.
5. Implement employee management.
6. Implement store configuration.
7. Implement audit log.

---

## 9. Must-Have User Flows for Testing

### Flow A — Staff sells walk-in order successfully

1. Login as staff.
2. Open shift.
3. Go to POS.
4. Add customer or skip customer for walk-in.
5. Add product to cart.
6. Confirm order.
7. Inventory is deducted.
8. Pay by cash.
9. Receipt is generated.
10. Order becomes COMPLETED.
11. Dashboard revenue updates.

Expected result:

- Stock decreases.
- Payment is saved.
- Receipt appears.
- Shift expected cash increases.
- SyncQueue has relevant events.

### Flow B — Staff cannot sell before opening shift

1. Login as staff.
2. Do not open shift.
3. Go to POS.
4. Try to checkout.

Expected result:

- System blocks checkout.
- Message: “Please open shift before selling.”

### Flow C — Online order from chat

1. Login as staff.
2. Open shift.
3. Paste chat text.
4. Parser extracts phone/name/address/product.
5. Staff reviews draft.
6. Confirm order.
7. Continue checkout.

Expected result:

- Customer is created or reused by phone.
- Order source is ZALO/FACEBOOK/TIKTOK_CHAT.
- Order is processed normally.

### Flow D — Insufficient stock

1. Add product with stock = 2.
2. Staff enters quantity = 5.

Expected result:

- System shows stock warning.
- Confirm button disabled or blocked.
- No inventory deduction occurs.

### Flow E — Payment failure

1. Confirm order.
2. Select QR/MoMo mock payment.
3. Mock service returns failed.

Expected result:

- Order becomes PAYMENT_FAILED or PAYMENT_PENDING.
- Staff can retry or change payment method.
- If order is cancelled, inventory is restored.

### Flow F — Delivery order

1. Create paid order with delivery address.
2. Add to fulfillment queue.
3. Create shipment with mock provider.
4. Update status to SHIPPING.
5. Mark delivered.

Expected result:

- Shipment has tracking code.
- Order status becomes DELIVERED / COMPLETED.

### Flow G — Close shift

1. Staff completes several cash orders.
2. Staff closes shift.
3. Enters actual cash.

Expected result:

- System calculates expected cash.
- System calculates difference.
- Shift report is saved.

---

## 10. Validation Rules

### Login

- Username is required.
- Password is required.
- Inactive user cannot login.

### Shift

- Opening cash cannot be negative.
- Actual cash cannot be negative.
- Only one active shift per staff.

### Product

- Name is required.
- Price must be >= 0.
- Stock quantity must be >= 0.
- SKU/barcode should be unique if provided.

### Customer

- Phone should be unique if provided.
- Name or phone should be required for delivery order.

### Order

- Order must have source.
- Order must have at least one item before confirm.
- Delivery order must have address.
- Cannot pay cancelled order.
- Cannot generate final receipt before payment success.

### Inventory

- Cannot sell more than available stock.
- Every stock mutation must be logged.

### Payment

- Payment amount must equal order total unless split payment.
- Split payment sum must equal order total.

---

## 11. Coding Rules for Agent

Follow these rules strictly:

1. Do not remove existing working code unless necessary.
2. Do not break existing package names.
3. Prefer small, incremental changes.
4. Every new screen must be connected to real data or seeded demo data.
5. Every database write should go through repository classes.
6. Avoid putting SQL directly inside Activity classes.
7. Avoid business logic inside XML or UI event only.
8. Use constants for roles, statuses, source names, and payment methods.
9. Show user-friendly error messages.
10. Add comments for complex business rules.
11. Keep mock external services replaceable by real API later.
12. Do not require real API keys for MVP.
13. Do not leave buttons without actions.
14. Do not create duplicate screens with same purpose.
15. Do not implement advanced features before core order flow works.

---

## 12. Definition of Done

The project is acceptable when these are true:

- App can build successfully.
- App can login with seeded users.
- Staff can open shift.
- Staff can create a walk-in order.
- Staff can add products to cart.
- App checks stock before confirming order.
- Inventory is deducted after order confirmation.
- Staff can process payment.
- Receipt screen is displayed.
- Order appears in order history.
- Staff can close shift.
- Manager/Admin can view reports.
- App has no major crash in normal flow.
- Empty states and error states are handled.
- Database tables are created successfully on first launch.

---

## 13. Final System Summary

mPOS Pro should not be coded as separate unrelated features.  
Everything must connect to this central lifecycle:

```text
Customer / Channel
→ Order
→ Cart
→ Inventory
→ Payment
→ Receipt
→ Fulfillment
→ Sync
→ Report
```

The most important MVP flow is:

```text
Login
→ Open Shift
→ Create Order
→ Add Product
→ Confirm Order
→ Lock Inventory
→ Pay
→ Generate Receipt
→ Complete Order
→ Close Shift
```

Build this flow first. Then add omnichannel, chat import, fulfillment, sync, reports, and admin features.

# mPOS Pro – Android structure

This project uses Java, XML Views and `SQLiteOpenHelper`. It intentionally does not use Compose, Room or Firebase in the MVP.

## Implemented MVP vertical slice

The runnable baseline includes seeded local login (`admin` / `admin123`), open/close shift, product search by name/SKU/barcode, stock-guarded cart, checkout in one SQLite transaction, cash/VNPay/MoMo mock payment, receipt display, inventory movement, audit log, local sync outbox processing and a local unified inbox view. Product/customer administration, reports, refund, real camera, printer and all third-party APIs remain the next implementation backlog.

## Dependency rules

`Activity/Fragment -> DAO -> DatabaseHelper -> SQLite`

`Activity/Fragment -> Adapter -> Model`

An Activity must not execute SQL. A sale is persisted in one SQLite transaction: order, order items, payment, inventory transaction, receipt and sync queue record.

## Feature folders and planned files

| Folder | Planned Java files | Responsibility |
|---|---|---|
| `model` | `User`, `Employee`, `Product`, `Category`, `Customer`, `Order`, `OrderItem`, `Payment`, `InventoryTransaction`, `Shift`, `Receipt`, `StoreSetting`, `CartItem`, `SyncQueueItem` | Plain data models. |
| `database` | `DatabaseContract`, `DatabaseHelper`, `DatabaseSeeder` | Schema, migrations and local seed data. |
| `dao` | `UserDao`, `EmployeeDao`, `ProductDao`, `CategoryDao`, `CustomerDao`, `OrderDao`, `PaymentDao`, `InventoryDao`, `ShiftDao`, `ReceiptDao`, `SettingsDao`, `SyncQueueDao` | SQLite CRUD and reporting queries. |
| `adapter` | `ProductAdapter`, `CategoryAdapter`, `CartAdapter`, `CustomerAdapter`, `OrderAdapter`, `OrderItemAdapter`, `InventoryTransactionAdapter`, `ShiftAdapter`, `ReportProductAdapter` | RecyclerView bindings only. |
| `ui` | `BaseActivity`, `SplashActivity`, `DashboardActivity`, `dialog/ConfirmDialog`, `widget/BarcodeInputView` | Shared UI components. |
| `auth` | `LoginActivity`, `RegisterActivity`, `SessionManager`, `PasswordUtils` | Login, registration and local session. |
| `pos` | `PosActivity`, `ProductSearchActivity`, `BarcodeScannerActivity`, `PosController` | Fast walk-in sale flow. |
| `cart` | `CartManager`, `CartActivity`, `CheckoutActivity`, `DiscountCalculator`, `TaxCalculator` | In-memory cart and checkout calculations. |
| `payment` | `PaymentActivity`, `PaymentProcessor`, `CashPaymentHandler`, `QrPaymentHandler`, `MomoPaymentHandler`, `VnpayPaymentHandler` | Payment mocks, including split payment later. |
| `product` | `ProductListActivity`, `ProductFormActivity`, `ProductDetailActivity`, `CategoryListActivity`, `CategoryFormActivity` | Product/catalog administration. |
| `customer` | `CustomerListActivity`, `CustomerFormActivity`, `CustomerDetailActivity`, `CustomerOrderHistoryActivity` | Phone-keyed CRM and repeat orders. |
| `order` | `OrderListActivity`, `OrderDetailActivity`, `RefundActivity`, `OrderStatusHelper` | Order history, statuses, refund mock. |
| `receipt` | `ReceiptActivity`, `ReceiptGenerator`, `ReceiptShareHelper`, `ReceiptPrinterMock` | E-receipt, sharing and print mock. |
| `inventory` | `InventoryActivity`, `StockInActivity`, `StockOutActivity`, `StockAdjustmentActivity`, `LowStockActivity`, `InventoryService` | Inventory movements and low-stock guard. |
| `shift` | `ShiftListActivity`, `OpenShiftActivity`, `CloseShiftActivity`, `ShiftReportActivity`, `ShiftCalculator` | Cash shifts and handover. |
| `report` | `ReportActivity`, `RevenueReportActivity`, `TopProductReportActivity`, `InventoryReportActivity`, `ChannelReportActivity` | Local aggregate reports. |
| `employee` | `EmployeeListActivity`, `EmployeeFormActivity`, `UserRoleActivity`, `RolePermissionHelper` | Staff and ADMIN/MANAGER/STAFF access. |
| `settings` | `SettingsActivity`, `StoreInfoActivity`, `VatSettingsActivity`, `PaymentSettingsActivity`, `PrinterSettingsActivity` | Store configuration. |
| `sync` | `SyncManager`, `SyncQueueProcessor`, `SyncWorker`, `remote/ApiClient` | Future outbox synchronization; local queue works offline now. |
| `shipping` | `ShipmentActivity`, `ShipmentDetailActivity`, `ShippingRateComparator` | Future logistics integration/mock. |
| `omnichannel` | `inbox/UnifiedInboxActivity`, `live/LiveSaleActivity`, `fulfillment/FulfillmentActivity`, `allocation/InventoryAllocationActivity` | Extension point for Zalo, Facebook, TikTok, Shopee and Website. |
| `utils` | `DateTimeUtils`, `CurrencyUtils`, `ValidationUtils`, `BarcodeUtils`, `DialogUtils` | Stateless helpers. |
| `constants` | `AppConstants`, `DbConstants`, `OrderConstants`, `PaymentConstants`, `RoleConstants`, `InventoryConstants`, `SyncConstants` | Stable string/status constants. |

## XML layouts to add by feature

`activity_login`, `activity_register`, `activity_dashboard`, `activity_pos`, `activity_product_search`, `activity_cart`, `activity_checkout`, `activity_payment`, `activity_product_list`, `activity_product_form`, `activity_customer_list`, `activity_customer_form`, `activity_customer_detail`, `activity_order_list`, `activity_order_detail`, `activity_receipt`, `activity_inventory`, `activity_stock_in`, `activity_stock_adjustment`, `activity_open_shift`, `activity_close_shift`, `activity_report`, `activity_employee_list`, `activity_settings`, `activity_unified_inbox`, `activity_fulfillment`.

RecyclerView items: `item_product`, `item_product_pos`, `item_category`, `item_cart`, `item_customer`, `item_order`, `item_order_item`, `item_inventory_transaction`, `item_shift`, `item_report_product`. Dialogs: `dialog_confirm`, `dialog_product_quantity`, `dialog_discount`, `dialog_payment_method`.

Use `activity_<screen>.xml`, `item_<entity>.xml`, `dialog_<purpose>.xml`, `<Entity>Activity.java`, `<Entity>Adapter.java`, `<Entity>Dao.java`, and `<Entity>.java`.

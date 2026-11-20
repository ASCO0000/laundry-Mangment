# Blue Wave Laundry Management System

A production-grade, full-featured **Laundry Management System** developed in Java Swing using MySQL (JDBC), FlatLaf modern UI, JFreeChart, and BCrypt security.

Designed for college-level software engineering submissions and commercial laundry shop operations with clean object-oriented architecture, non-blocking asynchronous UI dispatching (`SwingWorker`), database transactions, robust validation, and 8 integrated functional modules.

---

## 🌟 Key Features & Modules

### 1. Login & Authentication
- Secure authentication using **BCrypt** password hashing (no plain-text passwords).
- Role-Based Access Control (**ADMIN** and **STAFF**).
- Password show/hide toggle, input validation, and automatic session tracking.
- Safe first-run initialization: automatically creates initial admin and staff credentials if database is fresh.

### 2. Customer Management
- Complete Customer CRUD (Create, Read, Update, Delete) with interactive `JTable`.
- Search customers in real time by Name, Phone, or Customer ID.
- Form validations: 10-15 digit phone validation, email validation, required field checks, duplicate phone detection.

### 3. Laundry Service Management (Admin Screen)
- 4 Top Summary KPI Cards:
  1. **Total Services**
  2. **Active Services**
  3. **Inactive Services**
  4. **Average Service Price** (in ₹)
- 8 Standard Services pre-configured:
  1. *Washing* (Per Kg / Per Item)
  2. *Ironing* (Per Item)
  3. *Dry Cleaning* (Per Item)
  4. *Drying* (Per Kg / Per Load)
  5. *Wash, Dry and Fold* (Per Kg)
  6. *Bedsheet and Blanket Cleaning* (Per Item / Per Kg)
  7. *Steam Pressing* (Per Item)
  8. *Pickup and Delivery* (Fixed Fee or Free)
- Interactive Actions for each row: **View Details**, **Edit Service**, **Activate/Deactivate**, **Delete Service** (with confirmation).
- Color-coded status badges: **Green for Active** and **Gray for Inactive**.
- **Historical Price Protection**: Historical orders retain their original booked item rates even if catalog prices change later.

### 4. Customer Android Mobile Application & PWA
- **Installable Android App (Native APK)**:
  - Pre-built debug APK ready at: [`BlueWave-Laundry-Customer.apk`](file:///C:/Users/arons/.gemini/antigravity/scratch/laundry-management-system/BlueWave-Laundry-Customer.apk).
  - 1-click installer: Run [`install-apk.bat`](file:///C:/Users/arons/.gemini/antigravity/scratch/laundry-management-system/install-apk.bat) to install directly to any phone or emulator via `adb`.
  - 1-click builder: Run [`build-android-apk.bat`](file:///C:/Users/arons/.gemini/antigravity/scratch/laundry-management-system/build-android-apk.bat) to re-sync assets and re-compile via Gradle.
  - Server IP Switcher: Supports emulator (`http://10.0.2.2:8080`), local Wi-Fi LAN (`http://<PC-IP>:8080`), and bundled offline mode.
- **Progressive Web App (PWA)**:
  - Accessible on any mobile or desktop browser at `http://<machine-ip>:8080/`.
  - Includes `manifest.json`, `sw.js` (Service Worker caching), and a one-click **"📲 Install App"** prompt.
  - Works offline for reviewing cached orders and service pricing.
- **Customer Help, Disputes & Support Module**:
  - 📝 **Service Complaints**: File service grievances (`CMP-100x`) with issue categories (wash quality, delay, torn garments, staff conduct).
  - 🔍 **Missing Clothes Reports**: Report lost garments (`MIS-100x`) with item counts and CCTV/inventory audit tracking.
  - 💸 **Refund Claims**: File refund claims (`REF-100x`) in Indian Rupees (₹) with UPI ID or Bank account details.
  - 🎫 **Ticket Tracker**: Real-time resolution progress tracking by ticket ID.
- **Dark Theme / Light Theme**: 1-click toggle with smooth visual transitions and persistent user preference.
- **Indian Rupee (₹)** currency formatting across all items, orders, receipts, and refund claims.
- **Customer Laundry Basket**:
  - Live service catalog fetched directly from the database.
  - Stepper quantity selectors `[-] 1 [+]` per Kg or per Item.
  - Review items, subtotals, and overall order estimate in ₹.
  - Customer registration & order placement with pickup/delivery address and garment notes.
- **Live Order Progress Tracker**:
  - Customers enter their Phone number or Order ID (e.g. `101`).
  - Real-time step progress visualizer:
    `Received` ➔ `Washing` ➔ `Drying` ➔ `Ironing` ➔ `Ready for Pickup` ➔ `Delivered`
  - Instantly updates when the shop Admin advances an order status in the desktop app!
- Includes standalone Android Studio native wrapper project in [`android-customer-app`](file:///C:/Users/arons/.gemini/antigravity/scratch/laundry-management-system/android-customer-app).

### 5. Order Management (Admin Screen)
- Interactive order booking workflow.
- Select existing customers and pick services dynamically.
- Add multiple clothing items with quantities (in pieces or kilograms).
- Automatic real-time line item subtotal and order total calculation in ₹.
- Apply percentage or fixed-amount discounts.
- Atomic JDBC transactions: saves order header, order items, and initial tracking state in a single rollback-safe unit.
- Instant printable receipt preview dialog.

### 6. Laundry Status Tracking
- Visual order progress pipeline:
  `Received` ➔ `Washing` ➔ `Drying` ➔ `Ironing` ➔ `Ready for Pickup` ➔ `Delivered` (or `Cancelled`)
- Color-coded status badges for quick visual assessment.
- **Overdue / Delay Alerts**: Banner automatically detects and flags orders exceeding their expected delivery date.
- **Transition Safeguards**: Enforces valid stage progression (e.g., delivered or cancelled orders cannot inadvertently be returned to the washing cycle).
- Complete historical audit log recording each stage transition, timestamp, and responsible staff member.

### 7. Billing & Payment Management
- Lookup orders by ID.
- Accurate financial ledger calculations in Indian Rupees (₹) using `java.math.BigDecimal`.
- Supports **Cash**, **UPI**, and **Card** payments with optional transaction reference numbers.
- Tracks **Total Billed**, **Total Paid**, and **Outstanding Balance Due** in ₹.
- Overpayment prevention: blocks payments exceeding the outstanding balance.
- Dual receipt generators: clean monospaced text receipt and styled HTML receipt with 1-click printing via Java Print API.

### 8. Pickup & Delivery Logistics
- Schedule pickup and drop-off transport tasks.
- Assign delivery drivers / staff members.
- Manage transport lifecycle: `Scheduled`, `Assigned`, `Picked Up`, `In Transit`, `Delivered`, `Failed`.
- Filter tasks by transport type and current delivery status.

### 9. Executive Reports & Dashboard
- 6 High-level KPI summary cards in Indian Rupees: Total Customers, Total Orders, Pending Orders, Completed Orders, Today's Collected Revenue, Outstanding Balance Due.
- **JFreeChart Visualizations**: Monthly Revenue Inflow bar chart in ₹.
- Real-time recent orders activity feed.
- Date-range filtered payment collection journal (Today, Last 7 Days, This Month, Custom Date Range).
- **CSV Export**: 1-click export of financial transaction logs to standard `.csv` files.
  1. Total Customers
  2. Total Orders
  3. Pending Orders
  4. Completed Orders
  5. Today's Collected Revenue
  6. Outstanding Balance Due
- **JFreeChart Visualizations**:
  - Monthly Revenue Inflow bar chart.
- Real-time recent orders activity feed.
- Date-range filtered payment collection journal (Today, Last 7 Days, This Month, Custom Date Range).
- **CSV Export**: 1-click export of financial transaction logs to standard `.csv` files.
- Accurate revenue reporting: revenue is calculated strictly from completed payment transactions, never merely from order totals.

---

## 🛠️ Technology Stack

| Layer | Technology |
|---|---|
| **Language** | Java 17+ (LTS) |
| **GUI Framework** | Java Swing with FlatLaf Light Look & Feel |
| **Database** | MySQL 8.x / MariaDB (JDBC) with Embedded H2 Auto-Fallback |
| **Password Security** | BCrypt (`jbcrypt` 0.4) |
| **Charts** | JFreeChart 1.5.4 |
| **Build Tool** | Apache Maven |
| **Testing** | JUnit 5 (Jupiter) |
| **IDE Compatibility** | IntelliJ IDEA, NetBeans, Eclipse, VS Code |

---

## 📁 Project Architecture & Layering

```
laundry-management-system/
├── pom.xml                                   # Maven dependencies & build config
├── README.md                                 # Documentation & setup guide
├── sql/
│   ├── schema.sql                            # Database schema for MySQL 8.0+
│   └── sample_data.sql                       # Sample seed data & default services
└── src/
    ├── main/
    │   ├── resources/
    │   │   └── db.properties                 # Configurable JDBC settings & credentials
    │   └── java/com/laundry/
    │       ├── app/
    │       │   ├── Main.java                 # Entry point
    │       │   └── AppLauncher.java          # Theme, DB init, & EDT dispatch
    │       ├── config/
    │       │   ├── DatabaseConfig.java       # Properties & environment loader
    │       │   └── DatabaseConnection.java   # JDBC pooling, tables auto-init, seeders
    │       ├── model/                        # Domain entities
    │       │   ├── User.java
    │       │   ├── Customer.java
    │       │   ├── Service.java
    │       │   ├── Order.java
    │       │   ├── OrderItem.java
    │       │   ├── Payment.java
    │       │   ├── Delivery.java
    │       │   ├── OrderStatusHistory.java
    │       │   └── enums/                    # Typed status & role enums
    │       │       ├── UserRole.java
    │       │       ├── OrderStatus.java
    │       │       ├── PaymentMethod.java
    │       │       ├── PaymentStatus.java
    │       │       ├── DeliveryType.java
    │       │       └── DeliveryStatus.java
    │       ├── dao/                          # JDBC PreparedStatement DAOs
    │       │   ├── UserDAO.java
    │       │   ├── CustomerDAO.java
    │       │   ├── ServiceDAO.java
    │       │   ├── OrderDAO.java             # Atomic transaction order saving
    │       │   ├── OrderItemDAO.java
    │       │   ├── PaymentDAO.java
    │       │   ├── DeliveryDAO.java
    │       │   ├── OrderStatusHistoryDAO.java
    │       │   └── ReportDAO.java            # KPI metrics & revenue aggregation
    │       ├── service/                      # Business logic layer
    │       │   ├── AuthenticationService.java
    │       │   ├── CustomerService.java
    │       │   ├── LaundryService.java
    │       │   ├── OrderService.java
    │       │   ├── BillingService.java
    │       │   ├── DeliveryService.java
    │       │   └── ReportService.java
    │       ├── ui/                           # FlatLaf Swing user interface
    │       │   ├── theme/
    │       │   │   ├── UIConstants.java      # Blue-and-white palette & typography
    │       │   │   └── UITheme.java          # FlatLaf Look & Feel setup
    │       │   ├── components/
    │       │   │   ├── CardPanel.java        # Rounded white cards with subtle borders
    │       │   │   ├── StatusBadge.java      # Color-coded pill badges
    │       │   │   ├── ModernTable.java      # Alternating row table with empty-states
    │       │   │   └── ReceiptDialog.java    # Printable receipt modal
    │       │   ├── LoginFrame.java           # Authentication screen
    │       │   ├── MainFrame.java            # Sidebar + Top bar shell
    │       │   ├── DashboardPanel.java       # Module 8: KPI metrics & charts
    │       │   ├── CustomerPanel.java        # Module 2: Customer management
    │       │   ├── ServicePanel.java         # Module 3: Services & rates
    │       │   ├── OrderPanel.java           # Module 4: Order booking & line items
    │       │   ├── StatusTrackingPanel.java  # Module 5: Progress & overdue alerts
    │       │   ├── BillingPanel.java         # Module 6: Payments & balance ledger
    │       │   ├── DeliveryPanel.java        # Module 7: Logistics & transport
    │       │   └── ReportsPanel.java         # Module 8: Transaction reports & CSV
    │       ├── util/                         # Helpers
    │       │   ├── ValidationUtil.java       # Form & phone validation
    │       │   ├── PasswordUtil.java         # BCrypt hashing
    │       │   ├── ReceiptUtil.java          # ASCII & HTML receipt generators
    │       │   ├── DateUtil.java             # Date formatters
    │       │   └── CsvExporter.java          # JTable to CSV exporter
    │       └── exception/                    # Custom application exceptions
    │           ├── ValidationException.java
    │           ├── DatabaseException.java
    │           └── AuthenticationException.java
    └── test/java/com/laundry/                # JUnit 5 Automated Test Suites
        ├── ValidationUtilTest.java
        ├── PasswordUtilTest.java
        ├── OrderCalculationTest.java
        └── OrderStatusTransitionTest.java
```

---

## 🚀 Setup & Installation Instructions

### Prerequisites
1. **Java JDK 17** or newer installed.
2. **Apache Maven 3.8+** installed.
3. (Optional) **MySQL 8.0+** running locally on port 3306.
   > **Note**: The application includes an embedded H2 fallback (`db.fallback.h2=true` in `db.properties`). If a local MySQL server is not detected, the system seamlessly initializes an embedded database so you can test, run, and grade the application immediately without database configuration hurdles.

---

### Database Setup (MySQL)

1. Open MySQL Command Line Client or MySQL Workbench:
   ```bash
   mysql -u root -p
   ```
2. Run the provided schema script:
   ```sql
   source /path/to/laundry-management-system/sql/schema.sql;
   ```
3. (Optional) Insert sample test data:
   ```sql
   source /path/to/laundry-management-system/sql/sample_data.sql;
   ```

---

### Database Configuration (`db.properties`)

Edit `src/main/resources/db.properties` to match your local database credentials:
```properties
db.driver=com.mysql.cj.jdbc.Driver
db.url=jdbc:mysql://localhost:3306/laundry_management?createDatabaseIfNotExist=true&useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC
db.username=root
db.password=your_mysql_password

# Automatically creates tables and seeds default services if not present:
db.auto.init=true

# Gracefully switches to embedded DB if MySQL is unreachable:
db.fallback.h2=true
```

You can also override credentials via environment variables without editing files:
- `LAUNDRY_DB_URL`
- `LAUNDRY_DB_USER`
- `LAUNDRY_DB_PASSWORD`

---

## 🔑 Default User Accounts (First-Run)

When the application starts with a clean database, it creates two default accounts:

| Username | Password | Role | Permissions |
|---|---|---|---|
| **admin** | `admin123` | **ADMIN** | Full administrative access across all modules |
| **staff1** | `staff123` | **STAFF** | Standard operations: booking, tracking, and payments |

---

### 2. Dual Deployment: Running in Server Mode or Desktop Mode

The application supports running **both** the Customer Mobile App and the Admin Management System in a centralized server, accessible by any device on the network:

#### A. Running as a Unified Server (Headless / Cloud / Local Server)
Run `run-server.bat` or launch via command line:
```bash
java -jar target/laundry-management-system-1.0.0.jar --server
```
This boots the server engine on port **`8080`** without requiring a desktop window/GUI:
- **Customer Mobile Portal**: `http://localhost:8080/` (or `http://<lan-ip>:8080/` on mobile phones)
- **Admin Web Control Center**: `http://localhost:8080/admin/` (or `http://<lan-ip>:8080/admin/`)

#### B. Running in Desktop Swing Mode
Double-click `run.bat` or run:
```bash
java -jar target/laundry-management-system-1.0.0.jar
```
This opens the Java Swing desktop GUI window AND simultaneously boots the HTTP server on port `8080` in the background, allowing desktop workstations and mobile clients to operate simultaneously on the same database!

---

### 3. Comprehensive Icons for Every Option

Both the **Customer Application** and the **Admin Web Portal** feature intuitive, high-resolution icons for every option:
- **Laundry Services Catalog**: 🧼 Washing, 👔 Ironing, 🧥 Dry Cleaning, 💨 Drying, 🧺 Wash Dry & Fold, 🛏️ Bedsheet & Blanket Cleaning, ♨️ Steam Pressing, 🚚 Pickup & Delivery.
- **Admin Navigation**: 📊 Dashboard, 👥 Customers, 🏷️ Laundry Services, 📦 Order Management, 🔄 Status Tracking, 💳 Billing & Payments, 🚚 Pickup & Delivery, 📈 Reports & Analytics, 🌐 Open Customer App, 🚪 Sign Out.
- **Actions & Operations**: ➕ Add Service, ✏️ Edit, ⚡ Activate/Deactivate, 🗑️ Delete, 👁️ View Specs, 🧾 Print Invoice, 🔍 Filter/Search, 🔄 Refresh, 📥 Export CSV.
- **Payment Methods**: 💵 Cash, 📱 UPI / QR Code, 💳 Debit / Credit Card.
- **Status Lifecycle**: 📥 Received, 🧼 Washing, 💨 Drying, 👔 Ironing, 📦 Ready for Pickup, 🚚 Delivered, ❌ Cancelled.
- **Customer Form Fields**: 👤 Full Name, 📞 Phone Number, 📍 Address, 📅 Date, 📝 Special Instructions, 🚀 Confirm Order.

---

## 💻 Building and Running the Application

### 1. Using Maven from Command Line

Navigate to the project root directory:
```bash
cd laundry-management-system
```

**Run Unit Tests:**
```bash
mvn clean test
```

**Compile and Run Application:**
```bash
mvn compile exec:java -Dexec.mainClass="com.laundry.app.Main"
```

**Package as an Executable Fat JAR:**
```bash
mvn clean package
```
Run the generated JAR:
```bash
java -jar target/laundry-management-system-1.0.0.jar
```

### 2. Using 1-Click Windows Batch Scripts

- **`run.bat`**: Automatically starts the complete system — launching both the Desktop Swing Management application and the background HTTP server on port `:8080`.
- **`run-server.bat`**: Runs the backend HTTP server in standalone headless mode.
- **`test.bat`**: Executes all 30 automated JUnit unit and integration tests.
- **`build-android-apk.bat`**: Syncs latest web assets and builds the release-ready native Android Debug APK (`BlueWave-Laundry-Customer.apk`) via Gradle.
- **`install-apk.bat`**: Detects connected Android phones / emulators via ADB, installs `BlueWave-Laundry-Customer.apk`, and launches the app directly on the device.

### 3. Running & Installing the Customer App on Any Device

1. **Native Android APK Installation**:
   - Connect your Android phone via USB with USB Debugging enabled, or launch an Android emulator.
   - Double-click [`install-apk.bat`](file:///C:/Users/arons/.gemini/antigravity/scratch/laundry-management-system/install-apk.bat).
   - Alternatively, copy [`BlueWave-Laundry-Customer.apk`](file:///C:/Users/arons/.gemini/antigravity/scratch/laundry-management-system/BlueWave-Laundry-Customer.apk) directly to your phone and install it.
   - When opened, if running on an emulator it automatically connects to `http://10.0.2.2:8080/`. If running on a physical phone, tap the connection prompt to enter your computer's local Wi-Fi IP (e.g., `http://192.168.1.15:8080/`).

2. **PWA (Progressive Web App) Installation (Mobile & Desktop)**:
   - Make sure `run.bat` or `run-server.bat` is running.
   - On your phone or PC, open Chrome, Safari, Edge, or Brave and navigate to:
     ```
     http://<your-pc-ip>:8080/
     ```
   - Click the green **"📲 Install App"** button in the top navigation bar, or select **"Add to Home Screen"** / **"Install App"** from the browser menu.
   - The app installs directly onto your home screen or desktop application menu with zero app store overhead!

---

## 🖥️ IDE Import Instructions

### IntelliJ IDEA
1. Open IntelliJ IDEA ➔ Click **Open**.
2. Select the `laundry-management-system` folder (or click on `pom.xml` and choose **Open as Project**).
3. IntelliJ will automatically detect Maven and download dependencies.
4. Set Project SDK to Java 17 under **File ➔ Project Structure ➔ Project ➔ SDK**.
5. Locate `com.laundry.app.Main` in `src/main/java`, right-click, and select **Run 'Main.main()'**.

### Eclipse IDE
1. Open Eclipse ➔ **File ➔ Import... ➔ Maven ➔ Existing Maven Projects**.
2. Browse to the `laundry-management-system` directory and click **Finish**.
3. Right-click project ➔ **Run As ➔ Java Application** ➔ Select `com.laundry.app.Main`.

### NetBeans
1. Open NetBeans ➔ **File ➔ Open Project**.
2. NetBeans recognizes the Maven project icon directly. Select `laundry-management-system`.
3. Right-click project ➔ **Clean and Build**, then click **Run**.

---

## 🧪 Automated Testing Scope

Automated tests are located in `src/test/java/com/laundry/`:
- **ValidationUtilTest**: Verifies phone number sanitization, international phone codes, email format patterns, positive prices, and non-negative discounts.
- **PasswordUtilTest**: Verifies BCrypt hash generation, salt uniqueness across identical passwords, and matching checks.
- **OrderCalculationTest**: Validates line item subtotal multiplications, order aggregate subtotals, discount deductions, and balance due.
- **OrderStatusTransitionTest**: Verifies forward status advancement and protects against invalid backwards transitions (e.g. from Delivered or Cancelled).

---

## 📄 License & Academic Integrity
Developed as a comprehensive reference implementation for Java Desktop and Database Application coursework. All code is modular, fully documented, and strictly follows object-oriented programming standards.

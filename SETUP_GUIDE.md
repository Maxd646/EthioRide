# EthioRide - Complete Setup Guide for IntelliJ IDEA

## ✅ Project Status
- **All UI is now pure Java JavaFX** (no FXML files)
- All FXML files and orphaned controllers have been removed
- Project uses IntelliJ IDEA module system (no Maven/Gradle)

---

## 📋 Prerequisites

### 1. Install Java Development Kit (JDK)
- **Required:** JDK 17 or higher
- Download from: https://www.oracle.com/java/technologies/downloads/
- Verify installation: Open Command Prompt and run `java -version`

### 2. Download JavaFX SDK
- **Required:** JavaFX 17 or higher
- Download from: https://gluonhq.com/products/javafx/
- Choose: **Windows** → **SDK** → Download ZIP
- Extract to: `C:\javafx-sdk-17.0.17\` (or any location you prefer)

### 3. Download MySQL Connector
- Download from: https://dev.mysql.com/downloads/connector/j/
- Choose: **Platform Independent** → Download ZIP
- Extract and find `mysql-connector-j-9.x.x.jar`

### 4. Install MySQL Server (Optional - for database features)
- Download from: https://dev.mysql.com/downloads/mysql/
- Install and remember your root password
- Create database: `ethioride`

---

## 🚀 Setup Steps in IntelliJ IDEA

### Step 1: Open Project
1. Launch **IntelliJ IDEA**
2. Click **File** → **Open**
3. Navigate to: `C:\Users\HP\Desktop\Pro\EthioRide`
4. Click **OK**
5. Wait for IntelliJ to index the project

### Step 2: Configure JavaFX Library
1. Go to **File** → **Project Structure** (or press `Ctrl+Alt+Shift+S`)
2. Click **Libraries** in the left panel
3. You should see a library named **"lib"** pointing to JavaFX
4. If it shows errors (red text):
   - Click the **"lib"** library
   - Click the **"-"** button to remove it
   - Click the **"+"** button → **Java**
   - Navigate to your JavaFX SDK: `C:\javafx-sdk-17.0.17\lib`
   - Select all JAR files in that folder
   - Click **OK**
   - Name it **"lib"**
   - Click **OK**

### Step 3: Configure MySQL Connector (Optional)
1. Still in **Project Structure** → **Libraries**
2. Click **"+"** → **Java**
3. Navigate to where you extracted MySQL Connector
4. Select `mysql-connector-j-9.x.x.jar`
5. Click **OK**
6. Name it **"mysql-connector"**
7. Click **OK**

### Step 4: Verify Module Dependencies
1. In **Project Structure**, click **Modules**
2. For each module (**admin-dashboard**, **client-driver**, **client-passenger**, **server**, **shared**):
   - Click the module name
   - Click the **Dependencies** tab
   - Ensure **"lib"** (JavaFX) is listed
   - For **server** module, ensure **"mysql-connector"** is also listed
   - If missing, click **"+"** → **Library** → Select the library → **Add Selected**

### Step 5: Update Run Configurations
The project already has 4 run configurations. Update the JavaFX path in each:

1. Click **Run** → **Edit Configurations**
2. For each configuration (**AdminDashboard**, **DriverApp**, **PassengerApp**, **Server**):
   - Select the configuration
   - Find **VM options** field
   - Update the path to match your JavaFX location:
     ```
     --module-path "C:\javafx-sdk-17.0.17\lib" --add-modules javafx.controls,javafx.fxml
     ```
   - Click **OK**

---

## ▶️ Running the Project

### Order of Execution

#### 1. Start the Server FIRST
- Click the **Run** dropdown (top-right toolbar)
- Select **Server**
- Click the green **Run** button (or press `Shift+F10`)
- **Wait** until you see: `[EthioRide] Server listening on port 9090`

#### 2. Start Client Applications (any order)
Once the server is running, you can start any or all clients:

**Option A: Admin Dashboard**
- Select **AdminDashboard** from Run dropdown
- Click Run
- Login with: `admin` / `admin` (or any credentials)

**Option B: Driver App**
- Select **DriverApp** from Run dropdown
- Click Run
- Login with driver phone number and password

**Option C: Passenger App**
- Select **PassengerApp** from Run dropdown
- Click Run
- Login or Register as a new passenger

---

## 🗂️ Project Structure

```
EthioRide/
├── shared/                    # Shared DTOs, enums, protocol
│   └── src/main/java/com/ethioride/shared/
│       ├── dto/              # UserDTO, TripRequestDTO
│       ├── enums/            # UserRole, TripStatus, RideCategory
│       ├── protocol/         # Message, MessageType
│       └── constants/        # AppConstants
│
├── server/                    # TCP Server
│   └── src/main/java/com/ethioride/server/
│       ├── EthioRideServer.java    # Main server class
│       ├── config/                  # ServerConfig
│       └── db/                      # Database repositories
│
├── client-passenger/          # Passenger JavaFX App
│   └── src/main/java/com/ethioride/passenger/
│       ├── PassengerApp.java       # Entry point
│       ├── ui/                      # Pure Java UI screens
│       ├── network/                 # ServerConnection
│       └── state/                   # SessionState
│
├── client-driver/             # Driver JavaFX App
│   └── src/main/java/com/ethioride/driver/
│       ├── DriverApp.java          # Entry point
│       ├── ui/                      # Pure Java UI screens
│       ├── network/                 # NetworkClient
│       └── state/                   # DriverSessionState
│
└── admin-dashboard/           # Admin JavaFX App
    └── src/main/java/com/ethioride/admin/
        ├── Main.java               # Entry point
        ├── ui/                      # Pure Java UI screens
        ├── network/                 # AdminSocketClient
        ├── service/                 # AdminService
        └── state/                   # AdminSession
```

---

## 🎨 Features by Application

### Passenger App
- ✅ Login / Register
- ✅ Book rides (Economy / Premium / Elite)
- ✅ Ride history with search/filter
- ✅ Wallet & payment methods
- ✅ Promotions & promo codes
- ✅ Settings & profile

### Driver App
- ✅ Login
- ✅ Online/Offline toggle
- ✅ Accept/Decline ride requests
- ✅ Live earnings tracking
- ✅ Ride history
- ✅ Payments & withdrawals

### Admin Dashboard
- ✅ System monitoring
- ✅ Live driver tracking
- ✅ Trip management
- ✅ System logs
- ✅ Server metrics

---

## 🐛 Troubleshooting

### "Error: JavaFX runtime components are missing"
**Solution:** Update VM options in Run Configuration with correct JavaFX path

### "Cannot connect to server"
**Solution:** 
1. Make sure Server is running first
2. Check console for `[EthioRide] Server listening on port 9090`
3. Verify no firewall is blocking port 9090

### "Module not found" errors
**Solution:**
1. File → Project Structure → Modules
2. Verify all modules are listed
3. Check Dependencies tab for each module

### Red underlines in code
**Solution:**
1. File → Invalidate Caches → Invalidate and Restart
2. Wait for re-indexing to complete

### Database connection errors (Server)
**Solution:**
1. Install MySQL Server
2. Create database: `CREATE DATABASE ethioride;`
3. Update `server/production/server/db.properties` with your credentials
4. Run `server/production/server/schema.sql` to create tables

---

## 📝 Default Credentials

### Admin Dashboard
- Username: `admin`
- Password: `admin`

### Driver / Passenger
- Register new accounts through the app
- Or use test data if database is seeded

---

## 🎯 Quick Start (No Database)

If you want to test the UI without setting up MySQL:

1. Start **Server** (it will run without DB, but login/register won't work)
2. Start any client app
3. UI will load and you can explore the interface
4. Network features will show "Cannot connect" errors (expected)

---

## 📞 Support

For issues or questions:
- Check the console output for error messages
- Verify all prerequisites are installed
- Ensure JavaFX path is correct in VM options
- Make sure Server is running before starting clients

---

## ✨ What's New (Pure Java JavaFX)

- ❌ Removed all FXML files (6 files deleted)
- ❌ Removed orphaned FXML controllers
- ✅ All UI is now pure Java code
- ✅ Cleaner project structure
- ✅ Easier to maintain and modify
- ✅ No FXMLLoader dependencies

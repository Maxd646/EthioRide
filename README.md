# EthioRide — Distributed Ride-Sharing System

A real-time ride-sharing platform for Addis Ababa built with Java, JavaFX, TCP Sockets, and MySQL.

---

## Architecture

Three-tier client-server architecture with 5 modules:

```
EthioRide/
├── shared/          # DTOs, enums, protocol (MessageType)
├── server/          # TCP server, JDBC, business logic
├── client-passenger/# Passenger JavaFX app
├── client-driver/   # Driver JavaFX app
└── admin-dashboard/ # Admin JavaFX dashboard
```

---

## Tech Stack

| Layer       | Technology                        |
|-------------|-----------------------------------|
| Language    | Java 17                           |
| UI          | JavaFX 17 (pure Java, no FXML)    |
| Networking  | Java TCP Sockets                  |
| Database    | MySQL + JDBC                      |
| Pricing     | Google Maps Distance Matrix API   |
| Auth        | SHA-256 password hashing          |

---

## Prerequisites

- JDK 17 (Eclipse Adoptium recommended)
- JavaFX SDK 17 → `C:\javafx-sdk-17.0.17\lib`
- MySQL 8.x
- IntelliJ IDEA
- `mysql-connector-j-9.x.jar` in `lib/` folder

---

## Database Setup

```bash
mysql -u root -p < server/src/main/resources/schema.sql
mysql -u root -p < server/src/main/resources/schema_pricing.sql
mysql -u root -p < server/src/main/resources/default_admin.sql
```

Update `server/src/main/resources/db.properties` with your MySQL password.

---

## Default Admin Credentials

```
Email:    admin@ethioride.com
Phone:    +251 900 000 000
Password: admin123
```

---

## Run Order

1. **server** — must start first (port 9090)
2. **admin-dashboard** — system management
3. **client-passenger** — passenger app
4. **client-driver** — driver app

---

## IntelliJ Setup

1. `File → Project Structure → Modules`
2. Add JavaFX SDK to each module's dependencies
3. Add `lib/mysql-connector-j-9.x.jar` to **server** module dependencies
4. Set VM options for JavaFX modules:
   ```
   --module-path C:\javafx-sdk-17.0.17\lib --add-modules javafx.controls,javafx.fxml
   ```

---

## Features

### Admin Dashboard
- Login with email or phone
- View/manage all users (passengers, drivers, admins)
- Add drivers and admins
- View trips
- Manage pricing rules (base fare, per km rate, per minute rate)

### Passenger App
- Register / Login
- Request rides with pickup & dropoff
- Real-time price estimate via Google Maps API
- Trip history

### Driver App
- Login
- Accept/decline ride requests
- View earnings
- Trip history

### Server
- Multi-threaded TCP socket server
- JDBC MySQL persistence
- SHA-256 authentication
- Google Maps distance calculation
- Dynamic pricing engine

---

## Pricing

Prices are calculated using Google Maps Distance Matrix API:

```
Total = MAX(base_fare + (km × per_km_rate) + (min × per_min_rate) + booking_fee, minimum_fare)
```

| Category | Base  | Per km | Per min | Min Fare |
|----------|-------|--------|---------|----------|
| ECONOMY  | 50 ETB| 15 ETB | 2 ETB   | 30 ETB   |
| PREMIUM  | 80 ETB| 25 ETB | 3 ETB   | 50 ETB   |
| ELITE    |120 ETB| 40 ETB | 5 ETB   | 80 ETB   |

See `GOOGLE_MAPS_SETUP.md` for API key setup.

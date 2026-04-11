## EthioRide: Distributed Ride-Sharing & Transport System

EthioRide is a high-concurrency, distributed transport management system designed to solve urban mobility challenges in Addis Ababa. Built with a robust Client-Server architecture, it leverages Java Sockets and Multithreading to facilitate real-time driver-passenger matching, fare calculation, and trip lifecycle management.

---

## Key Engineering Features

- **High-Concurrency Server:** Utilizes an ExecutorService thread pool to handle simultaneous requests from multiple concurrent users without blocking.

- **Real-time Communication:** Custom TCP/IP socket protocol for low-latency synchronization between the Passenger and Driver applications.

- **Intelligent Matching Engine:** Implements a location-aware algorithm to connect passengers with the nearest available drivers, minimizing "dead mileage."

- **Persistent Data Layer:** ACID-compliant storage using JDBC and MySQL for secure user data and trip history.

- **Fault Tolerance:** Implements Object Serialization for system state snapshots and logging for audit trails.

---

## System Architecture & Design

The system follows a **Three-Tier Architecture** to ensure modularity and scalability.

- **Client Layer (JavaFX):** Separate interfaces for Passengers (ride requests, fare estimation) and Drivers (availability toggle, trip fulfillment).

- **Application Server:** The central engine managing the MatchmakingEngine, SessionManager, and TransactionCoordinator.

- **Data Layer:** A relational schema optimized for ride telemetry and role-based access control (RBAC).

---

## System Architecture

![System Architecture](client-driver/src/main/resources/ui/images/Natural%20Language%20Query%20Flow-2026-04-11-123637.png)
---

## Folder Structure

```text
EthioRide/
│
├── pom.xml                        
├── README.md
├── .gitignore
│
├── docs/
│   ├── architecture/
│   ├── diagrams/
│   ├── api-specs/
│   └── workflow/
│
├── scripts/
│   ├── start-server.sh
│   ├── start-passenger.sh
│   ├── start-driver.sh
│   └── docker/
│       ├── Dockerfile.server
│       └── docker-compose.yml
│
├── data/                           
│   ├── logs/
│   ├── snapshots/
│   └── exports/
│
├── shared/                         
│   ├── pom.xml
│   └── src/
│       ├── main/
│       └── test/
│
├── server/
│   ├── pom.xml
│   └── src/
│       ├── main/
│       └── test/
│
├── client-passenger/
│   ├── pom.xml
│   └── src/
│
├── client-driver/
│   ├── pom.xml
│   └── src/
│
└── tests/
    ├── unit/
    ├── integration/
    └── stress/
```

---

## Technical Challenges & Solutions

- **Double-Booking Problem:**  
  ReentrantLocks are implemented in the MatchingEngine to ensure atomic state updates.

- **Network Resiliency:**  
  Heartbeat mechanism and GracefulShutdown hook clean server resources.

- **Low-Bandwidth Optimization:**  
  Lightweight serialized DTO protocol supports slower networks.

---

## Tech Stack

- **Language:** Java 17+
- **Networking:** Java Sockets (TCP), RMI (Optional)
- **Concurrency:** Threads, ThreadPools
- **GUI:** JavaFX / CSS
- **Persistence:** JDBC, MySQL, File I/O (CSV Reports)

---

## Installation

### Clone

```bash
git clone https://github.com/Maxd646/EthioRide.git
cd EthioRide
```

### Configure Database

Update:

```
server/src/main/resources/db.properties
```

Add your MySQL credentials.

---

## Build & Run

```bash
# Start the Server
java -cp bin com.ethioride.server.Main

# Start Passenger Client
java -cp bin com.ethioride.client.PassengerApp
```

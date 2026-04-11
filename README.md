## EthioRide: Distributed Ride-Sharing & Transport System

EthioRide is a high-concurrency, distributed transport management system designed to solve urban mobility challenges in Addis Ababa. Built with a robust Client-Server architecture, it leverages Java Sockets and Multithreading to facilitate real-time driver-passenger matching, fare calculation, and trip lifecycle management.

## Key Engineering Features

- High-Concurrency Server: Utilizes an ExecutorService thread pool to handle simultaneous requests from multiple concurrent users without blocking.

- Real-time Communication: Custom TCP/IP socket protocol for low-latency synchronization between the Passenger and Driver applications.

- Intelligent Matching Engine: Implements a location-aware algorithm to connect passengers with the nearest available drivers, minimizing "dead mileage."

- Persistent Data Layer: ACID-compliant storage using JDBC and MySQL for secure user data and trip history.

- Fault Tolerance: Implements Object Serialization for system state snapshots and logging for audit trails.

## System Architecture & Design

The system follows a Three-Tier Architecture to ensure modularity and scalability.

- Client Layer (JavaFX): Separate interfaces for Passengers (ride requests, fare estimation) and Drivers (availability toggle, trip fulfillment).

- Application Server: The central engine managing the MatchmakingEngine, SessionManager, and TransactionCoordinator.

- Data Layer: A relational schema optimized for ride telemetry and role-based access control (RBAC).

## System Architecture
![System Architecture](<Natural Language Query Flow-2026-04-11-123637.png>)

## Folder Stucture 
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
│       │   ├── java/com/ethioride/shared/
│       │   │   ├── dto/
│       │   │   │   ├── RideRequestDTO.java
│       │   │   │   ├── DriverDTO.java
│       │   │   │   └── TripDTO.java
│       │   │   │
│       │   │   ├── enums/
│       │   │   │   ├── UserRole.java
│       │   │   │   ├── TripStatus.java
│       │   │   │   └── DriverStatus.java
│       │   │   │
│       │   │   ├── protocol/
│       │   │   │   ├── SocketMessage.java   
│       │   │   │   ├── RequestType.java
│       │   │   │   └── ResponseType.java
│       │   │   │
│       │   │   ├── utils/
│       │   │   │   ├── GeoUtils.java        
│       │   │   │   ├── SerializationUtils.java
│       │   │   │   └── TimeUtils.java
│       │   │   │
│       │   │   ├── validation/              # 
│       │   │   │   ├── PhoneValidator.java  # +251 / 09
│       │   │   │   ├── CoordinateValidator.java
│       │   │   │   └── InputSanitizer.java
│       │   │   │
│       │   │   ├── exceptions/
│       │   │   │   ├── EthioRideException.java
│       │   │   │   ├── DriverNotFoundException.java
│       │   │   │   └── RideConflictException.java
│       │   │   │
│       │   │   └── constants/
│       │   │       └── AppConstants.java
│       │   │
│       │   └── resources/                  
│       │
│       └── test/
│
├── server/
│   ├── pom.xml
│   └── src/
│       ├── main/
│       │   ├── java/com/ethioride/server/
│       │   │   ├── Main.java
│       │   │
│       │   │   ├── config/
│       │   │
│       │   │   ├── network/
│       │   │   │   ├── SocketServer.java
│       │   │   │   ├── ClientHandler.java
│       │   │   │   └── protocol/
│       │   │
│       │   │   ├── core/
│       │   │   │   ├── matchmaking/
│       │   │   │   │   ├── MatchingEngine.java
│       │   │   │   │   └── LocationService.java
│       │   │   │   │
│       │   │   │   ├── session/
│       │   │   │   │   └── SessionManager.java
│       │   │   │   │
│       │   │   │   ├── trip/
│       │   │   │   │   ├── TripManager.java
│       │   │   │   │   └── FareCalculator.java
│       │   │   │   │
│       │   │   │   └── transaction/
│       │   │   │       └── TransactionCoordinator.java
│       │   │
│       │   │   ├── concurrency/
│       │   │   │   ├── ThreadPoolManager.java
│       │   │   │   ├── locks/
│       │   │   │   │   └── DriverLockManager.java
│       │   │   │   └── queues/
│       │   │   │       └── RequestQueue.java
│       │   │
│       │   │   ├── persistence/
│       │   │   │   ├── db/
│       │   │   │   │   ├── DBConnection.java
│       │   │   │   │   └── migrations/
│       │   │   │   │       ├── V1__init.sql
│       │   │   │   │       └── V2__add_ratings.sql
│       │   │   │   │
│       │   │   │   ├── entity/
│       │   │   │   │   ├── User.java
│       │   │   │   │   ├── Driver.java   
│       │   │   │   │   └── Trip.java
│       │   │   │   │
│       │   │   │   └── repository/
│       │   │   │       ├── UserRepository.java
│       │   │   │       ├── DriverRepository.java
│       │   │   │       └── TripRepository.java
│       │   │
│       │   │   ├── security/
│       │   │   │   ├── auth/
│       │   │   │   └── rbac/
│       │   │
│       │   │   ├── fault/
│       │   │   │   ├── HeartbeatMonitor.java
│       │   │   │   ├── GracefulShutdown.java
│       │   │   │   ├── SnapshotManager.java
│       │   │   │   └── RecoveryManager.java
│       │   │
│       │   │   └── logging/
│       │   │       └── LoggerService.java
│       │   │
│       │   └── resources/             
│       │       ├── application.properties
│       │       ├── db.properties
│       │       └── logging.properties
│       │
│       └── test/
│
├── client-passenger/
│   ├── pom.xml
│   └── src/
│       ├── main/
│       │   ├── java/com/ethioride/passenger/
│       │   │   ├── Main.java
│       │   │   ├── network/SocketClient.java
│       │   │   ├── service/PassengerService.java
│       │   │   └── state/PassengerSession.java
│       │   │
│       │   └── resources/              # 
│       │       ├── ui/
│       │       │   ├── views/          # .fxml
│       │       │   ├── styles/         # .css
│       │       │   └── images/
│       │       └── i18n/               # localization
│       │           ├── messages_en.properties
│       │           └── messages_am.properties
│       │
│       └── test/
│
├── client-driver/
│   ├── pom.xml
│   └── src/
│       ├── main/
│       │   ├── java/com/ethioride/driver/
│       │   │   ├── Main.java
│       │   │   ├── network/
│       │   │   ├── service/
│       │   │   └── state/
│       │   │
│       │   └── resources/
│       │       ├── ui/
│       │       └── i18n/
│       │
│       └── test/
│
└── tests/                          # 🧪 System-level testing
    ├── unit/
    ├── integration/
    └── stress/

## Technical Challenges & Solutions

- The Double-Booking Problem: To prevent two passengers from booking the same driver simultaneously, ReentrantLocks are implemented in the MatchingEngine to ensure atomic state updates.

- Network Resiliency: Handled unexpected client disconnections by implementing a heartbeat mechanism and a GracefulShutdown hook to clean up server resources.

- Low-Bandwidth Optimization: Designed a lightweight data transfer protocol using Serialized DTOs to ensure functionality on 3G/EDGE networks.

## Tech Stack

- Language: Java 17+

- Networking: Java Sockets (TCP), RMI (Optional)

- Concurrency: Threads, ThreadPools, Synchronized Blocks

- GUI: JavaFX / CSS

- Persistence: JDBC, MySQL, File I/O (CSV Reports)

## Installation

Clone:https://github.com/Maxd646/EthioRide.git

Configure Database:
Update `db.properties` with your MySQL credentials.

Build & Run:

```bash
# Start the Server
java -cp bin com.ethioride.server.Main

# Start Passenger Client
java -cp bin com.ethioride.client.PassengerApp

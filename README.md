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

## System Workflow

![System Workflow](image.png)

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

-- EthioRide Database Schema
-- Run this once in MySQL before starting the server:
--   mysql -u root -p < schema.sql

CREATE DATABASE IF NOT EXISTS ethioride;
USE ethioride;

-- ── Users ─────────────────────────────────────────────────────────────────────
CREATE TABLE IF NOT EXISTS users (
    id            VARCHAR(36)    PRIMARY KEY,
    full_name     VARCHAR(100)   NOT NULL,
    phone         VARCHAR(20)    NOT NULL UNIQUE,
    email         VARCHAR(100),
    role          ENUM('PASSENGER','DRIVER','ADMIN') NOT NULL DEFAULT 'PASSENGER',
    password_hash VARCHAR(64)    NOT NULL,
    rating        DOUBLE         NOT NULL DEFAULT 5.0,
    created_at    TIMESTAMP      DEFAULT CURRENT_TIMESTAMP
);

-- ── Trips ─────────────────────────────────────────────────────────────────────
CREATE TABLE IF NOT EXISTS trips (
    id               VARCHAR(36)  PRIMARY KEY,
    passenger_id     VARCHAR(36)  NOT NULL,
    driver_id        VARCHAR(36),
    pickup_location  VARCHAR(200) NOT NULL,
    dropoff_location VARCHAR(200) NOT NULL,
    category         ENUM('ECONOMY','PREMIUM','ELITE') DEFAULT 'ECONOMY',
    fare             DOUBLE       NOT NULL DEFAULT 0,
    distance_km      DOUBLE       NOT NULL DEFAULT 0,
    status           ENUM('PENDING','ACCEPTED','IN_PROGRESS','COMPLETED','CANCELLED') DEFAULT 'PENDING',
    created_at       TIMESTAMP    DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (passenger_id) REFERENCES users(id),
    FOREIGN KEY (driver_id)    REFERENCES users(id) ON DELETE SET NULL
);

-- ── Pricing Rules ─────────────────────────────────────────────────────────────
CREATE TABLE IF NOT EXISTS pricing_rules (
    category        ENUM('ECONOMY','PREMIUM','ELITE') PRIMARY KEY,
    base_fare       DOUBLE NOT NULL DEFAULT 50.0,
    per_km_rate     DOUBLE NOT NULL DEFAULT 15.0,
    per_minute_rate DOUBLE NOT NULL DEFAULT 2.0,
    minimum_fare    DOUBLE NOT NULL DEFAULT 30.0,
    booking_fee     DOUBLE NOT NULL DEFAULT 10.0,
    updated_at      TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
);

-- Seed default pricing rules (safe to re-run)
INSERT INTO pricing_rules (category, base_fare, per_km_rate, per_minute_rate, minimum_fare, booking_fee)
VALUES
    ('ECONOMY', 50.0,  15.0, 2.0, 30.0, 10.0),
    ('PREMIUM', 80.0,  22.0, 3.0, 50.0, 15.0),
    ('ELITE',   120.0, 30.0, 4.0, 80.0, 20.0)
ON DUPLICATE KEY UPDATE
    base_fare       = VALUES(base_fare),
    per_km_rate     = VALUES(per_km_rate),
    per_minute_rate = VALUES(per_minute_rate),
    minimum_fare    = VALUES(minimum_fare),
    booking_fee     = VALUES(booking_fee);

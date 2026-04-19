-- EthioRide Database Schema
-- Run this once in MySQL before starting the server:
--   mysql -u root -p < schema.sql

CREATE DATABASE IF NOT EXISTS ethioride;
USE ethioride;

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
    FOREIGN KEY (passenger_id) REFERENCES users(id)
);

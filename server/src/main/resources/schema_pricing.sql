-- EthioRide Pricing & Location Schema
-- Run this after the main schema.sql

USE ethioride;

-- Pricing rules for different vehicle categories
CREATE TABLE IF NOT EXISTS pricing_rules (
    id               VARCHAR(36)  PRIMARY KEY,
    category         ENUM('ECONOMY','PREMIUM','ELITE') NOT NULL,
    base_fare        DOUBLE       NOT NULL DEFAULT 50.0,
    per_km_rate      DOUBLE       NOT NULL DEFAULT 15.0,
    per_minute_rate  DOUBLE       NOT NULL DEFAULT 2.0,
    minimum_fare     DOUBLE       NOT NULL DEFAULT 30.0,
    booking_fee      DOUBLE       NOT NULL DEFAULT 10.0,
    created_at       TIMESTAMP    DEFAULT CURRENT_TIMESTAMP
);

-- Driver earnings/wallet
CREATE TABLE IF NOT EXISTS driver_earnings (
    id               VARCHAR(36)  PRIMARY KEY,
    driver_id        VARCHAR(36)  NOT NULL,
    trip_id          VARCHAR(36),
    amount           DOUBLE       NOT NULL,
    commission       DOUBLE       NOT NULL DEFAULT 0.0,
    net_amount       DOUBLE       NOT NULL,
    status           ENUM('PENDING','PAID','CANCELLED') DEFAULT 'PENDING',
    created_at       TIMESTAMP    DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (driver_id) REFERENCES users(id),
    FOREIGN KEY (trip_id) REFERENCES trips(id)
);

-- Popular locations (optional - for autocomplete)
CREATE TABLE IF NOT EXISTS locations (
    id               VARCHAR(36)  PRIMARY KEY,
    name             VARCHAR(200) NOT NULL,
    address          VARCHAR(300),
    latitude         DOUBLE,
    longitude        DOUBLE,
    type             ENUM('PICKUP','DROPOFF','BOTH') DEFAULT 'BOTH',
    is_active        BOOLEAN      DEFAULT TRUE,
    created_at       TIMESTAMP    DEFAULT CURRENT_TIMESTAMP
);

-- Insert default pricing rules
INSERT INTO pricing_rules (id, category, base_fare, per_km_rate, per_minute_rate, minimum_fare, booking_fee)
VALUES 
    ('pricing-economy', 'ECONOMY', 50.0, 15.0, 2.0, 30.0, 10.0),
    ('pricing-premium', 'PREMIUM', 80.0, 25.0, 3.0, 50.0, 15.0),
    ('pricing-elite',   'ELITE',   120.0, 40.0, 5.0, 80.0, 20.0)
ON DUPLICATE KEY UPDATE
    base_fare = VALUES(base_fare),
    per_km_rate = VALUES(per_km_rate),
    per_minute_rate = VALUES(per_minute_rate),
    minimum_fare = VALUES(minimum_fare),
    booking_fee = VALUES(booking_fee);

-- Insert some popular locations in Addis Ababa
INSERT INTO locations (id, name, address, latitude, longitude, type) VALUES
    ('loc-001', 'Bole International Airport', 'Bole, Addis Ababa', 8.9806, 38.7992, 'BOTH'),
    ('loc-002', 'Meskel Square', 'Meskel Square, Addis Ababa', 9.0107, 38.7613, 'BOTH'),
    ('loc-003', 'Piassa', 'Piassa, Addis Ababa', 9.0333, 38.7469, 'BOTH'),
    ('loc-004', 'Bole Medhanialem', 'Bole, Addis Ababa', 8.9956, 38.7869, 'BOTH'),
    ('loc-005', 'Kazanchis', 'Kazanchis, Addis Ababa', 9.0192, 38.7636, 'BOTH'),
    ('loc-006', 'Sarbet', 'Sarbet, Addis Ababa', 9.0250, 38.7500, 'BOTH'),
    ('loc-007', 'Mexico Square', 'Mexico, Addis Ababa', 9.0089, 38.7636, 'BOTH'),
    ('loc-008', 'Legehar', 'Legehar, Addis Ababa', 9.0333, 38.7333, 'BOTH')
ON DUPLICATE KEY UPDATE
    name = VALUES(name),
    address = VALUES(address),
    latitude = VALUES(latitude),
    longitude = VALUES(longitude);

-- Display pricing rules
SELECT 'Pricing Rules Created:' AS message;
SELECT category, base_fare, per_km_rate, per_minute_rate, minimum_fare, booking_fee 
FROM pricing_rules;

-- Display locations
SELECT 'Popular Locations Added:' AS message;
SELECT name, address, type FROM locations;

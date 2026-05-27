-- EthioRide Pricing & Location Schema
-- Run this after the main schema.sql
-- NOTE: pricing_rules is defined and seeded in schema.sql — do NOT redefine it here.

USE ethioride;

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

-- pricing_rules seed data is in schema.sql — no duplicate INSERT here.

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

-- Display locations
SELECT 'Popular Locations Added:' AS message;
SELECT name, address, type FROM locations;

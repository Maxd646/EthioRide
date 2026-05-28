-- Default Admin Account
-- Phone:    +251900000000
-- Password: admin123

USE ethioride;

-- Remove any existing admin with this phone (clean reinstall)
DELETE FROM users WHERE phone = '+251900000000';

-- Insert default admin (SHA-256 of "admin123")
INSERT INTO users (id, full_name, phone, email, role, password_hash, rating, created_at)
VALUES (
    'admin-default-001',
    'System Administrator',
    '+251900000000',
    'admin@ethioride.com',
    'ADMIN',
    '240be518fabd2724ddb6f04eeb1da5967448d7e831c08c8fa822809f74c720a9',
    5.0,
    CURRENT_TIMESTAMP
);

SELECT 'Admin created.' AS result, '+251900000000' AS phone, 'admin123' AS password;

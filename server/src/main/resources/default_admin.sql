-- Insert Default Admin Account
-- Run this after creating the schema
-- Default credentials: admin / admin123

USE ethioride;

-- Delete existing admin if present (for clean reinstall)
DELETE FROM users WHERE phone = '+251 900 000 000';

-- Insert default admin
-- Password: admin123 (SHA-256 hashed)
INSERT INTO users (id, full_name, phone, email, role, password_hash, rating, created_at)
VALUES (
    'admin-default-001',
    'System Administrator',
    '+251 900 000 000',
    'admin@ethioride.com',
    'ADMIN',
    '240be518fabd2724ddb6f04eeb1da5967448d7e831c08c8fa822809f74c720a9',  -- admin123
    5.0,
    CURRENT_TIMESTAMP
);

-- Verify insertion
SELECT id, full_name, phone, email, role, created_at 
FROM users 
WHERE role = 'ADMIN';

-- Display success message
SELECT 'Default admin created successfully!' AS message,
       'Username: admin@ethioride.com or +251 900 000 000' AS username,
       'Password: admin123' AS password;

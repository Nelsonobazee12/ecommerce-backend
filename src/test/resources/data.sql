-- Insert admin user if not exists
INSERT INTO users (email, password, first_name, last_name, role, is_enabled, is_email_verified, created_at, updated_at)
SELECT 'admin@ecommerce.com', '$2a$10$YourEncodedPasswordHere', 'Admin', 'User', 'ADMIN', true, true, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
WHERE NOT EXISTS (SELECT 1 FROM users WHERE email = 'admin@ecommerce.com');
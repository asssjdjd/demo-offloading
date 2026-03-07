-- =============================================================================
-- Database Initialization Script (MySQL)
-- =============================================================================
CREATE DATABASE IF NOT EXISTS orderdb CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
CREATE USER IF NOT EXISTS 'root' @'%' IDENTIFIED BY '123456';
GRANT ALL PRIVILEGES ON userdb.* TO 'root' @'%';
GRANT ALL PRIVILEGES ON orderdb.* TO 'root' @'%';
FLUSH PRIVILEGES;
-- Users table
CREATE TABLE IF NOT EXISTS users (
    id VARCHAR(36) NOT NULL PRIMARY KEY,
    username VARCHAR(50) NOT NULL UNIQUE,
    email VARCHAR(255) NOT NULL UNIQUE,
    full_name VARCHAR(100) NOT NULL,
    phone VARCHAR(20),
    role VARCHAR(20) NOT NULL DEFAULT 'USER',
    status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_unicode_ci;
-- Index for common queries
CREATE INDEX idx_users_username ON users(username);
CREATE INDEX idx_users_email ON users(email);
CREATE INDEX idx_users_role ON users(role);
CREATE INDEX idx_users_status ON users(status);
-- Seed data
INSERT IGNORE INTO users (
        id,
        username,
        email,
        full_name,
        phone,
        role,
        status
    )
VALUES (
        'a0eebc99-9c0b-4ef8-bb6d-6bb9bd380a11',
        'admin',
        'admin@gmail.com',
        'System Admin',
        '0901234567',
        'ADMIN',
        'ACTIVE'
    ),
    (
        'b0eebc99-9c0b-4ef8-bb6d-6bb9bd380a22',
        'user',
        'user@gmail.com',
        'User',
        '0912345678',
        'USER',
        'ACTIVE'
    );
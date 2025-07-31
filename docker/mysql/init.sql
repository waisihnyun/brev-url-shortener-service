-- MySQL initialization script for Brev application
-- This script will be executed when the MySQL container starts for the first time

-- Create the database if it doesn't exist
CREATE DATABASE IF NOT EXISTS brev CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;

-- Use the database
USE brev;

-- Create application user with proper permissions
CREATE USER IF NOT EXISTS 'brevuser'@'%' IDENTIFIED BY 'brevpassword';
GRANT ALL PRIVILEGES ON brev.* TO 'brevuser'@'%';

-- Create url_mapping table (Spring Boot will handle schema creation, but this ensures consistency)
CREATE TABLE IF NOT EXISTS url_mapping (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    short_code VARCHAR(10) NOT NULL UNIQUE,
    long_url TEXT NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_short_code (short_code),
    INDEX idx_created_at (created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Insert some sample data for testing (optional)
INSERT IGNORE INTO url_mapping (short_code, long_url, created_at) VALUES
('demo01', 'https://www.example.com', NOW()),
('demo02', 'https://github.com', NOW()),
('demo03', 'https://stackoverflow.com', NOW());

FLUSH PRIVILEGES;

-- Show created tables
SHOW TABLES;

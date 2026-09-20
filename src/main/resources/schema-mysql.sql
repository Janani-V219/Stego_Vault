-- StegoVault Database Schema for MySQL 8.0+
-- Run this script in MySQL Workbench or mysql CLI: mysql -u root -p < schema-mysql.sql

CREATE DATABASE IF NOT EXISTS stego_vault CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
USE stego_vault;

-- Users Table
CREATE TABLE IF NOT EXISTS users (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(100) NOT NULL,
    email VARCHAR(150) NOT NULL UNIQUE,
    password VARCHAR(255) NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Encoding History Table
-- NOTE: In strict compliance with security and privacy requirements,
-- secret messages and encryption passwords are NEVER stored in this database.
CREATE TABLE IF NOT EXISTS encoding_history (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    operation_type VARCHAR(50) NOT NULL,
    file_name VARCHAR(255) NOT NULL,
    file_type VARCHAR(50) NOT NULL,
    status VARCHAR(50) NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    user_id BIGINT NOT NULL,
    CONSTRAINT fk_history_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE INDEX idx_history_user_id ON encoding_history(user_id);
CREATE INDEX idx_history_created_at ON encoding_history(created_at);

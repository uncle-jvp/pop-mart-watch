-- Pop Mart Watch Database Schema
-- MySQL 5.7+ / 8.0+

-- Create database (run this manually on your MySQL server)
-- CREATE DATABASE IF NOT EXISTS popmart_watch CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
-- USE popmart_watch;

-- Drop tables if they exist (for clean reinstall)
DROP TABLE IF EXISTS stock_check_history;
DROP TABLE IF EXISTS monitored_products;

-- Monitored Products Table
CREATE TABLE monitored_products (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    url VARCHAR(500) NOT NULL UNIQUE COMMENT 'Product URL',
    product_name VARCHAR(200) NOT NULL COMMENT 'Product name',
    is_active TINYINT(1) NOT NULL DEFAULT 1 COMMENT 'Is monitoring active',
    last_known_stock TINYINT(1) NOT NULL DEFAULT 0 COMMENT 'Last known stock status',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT 'Created timestamp',
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT 'Updated timestamp',
    last_checked_at DATETIME NULL COMMENT 'Last check timestamp',
    last_error TEXT NULL COMMENT 'Last error message',
    added_by_user_id VARCHAR(100) NOT NULL COMMENT 'Discord user ID who added this product',
    deleted TINYINT(1) NOT NULL DEFAULT 0 COMMENT 'Logical delete flag'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='Monitored products table';

-- Create indexes for monitored_products
CREATE INDEX idx_is_active ON monitored_products (is_active);
CREATE INDEX idx_url ON monitored_products (url);
CREATE INDEX idx_user_id ON monitored_products (added_by_user_id);
CREATE INDEX idx_deleted ON monitored_products (deleted);
CREATE INDEX idx_last_checked ON monitored_products (last_checked_at);

-- Stock Check History Table
CREATE TABLE stock_check_history (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    product_id BIGINT NOT NULL COMMENT 'Product ID',
    in_stock TINYINT(1) NOT NULL COMMENT 'Stock status',
    checked_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT 'Check timestamp',
    error_message TEXT NULL COMMENT 'Error message if check failed',
    response_time INT NULL COMMENT 'Response time in milliseconds',
    stock_changed TINYINT(1) NOT NULL DEFAULT 0 COMMENT 'Whether stock status changed'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='Stock check history table';

-- Create indexes for stock_check_history
CREATE INDEX idx_product_id ON stock_check_history (product_id);
CREATE INDEX idx_checked_at ON stock_check_history (checked_at);
CREATE INDEX idx_stock_changed ON stock_check_history (stock_changed);
CREATE INDEX idx_product_checked ON stock_check_history (product_id, checked_at);

-- Add foreign key constraint
ALTER TABLE stock_check_history 
ADD CONSTRAINT fk_stock_check_product 
FOREIGN KEY (product_id) REFERENCES monitored_products(id) ON DELETE CASCADE;

-- Insert sample data (optional)
-- INSERT INTO monitored_products (url, product_name, added_by_user_id) VALUES
-- ('https://www.popmart.com/us/products/1739/', 'Molly Space Series', 'sample_user_123'); 
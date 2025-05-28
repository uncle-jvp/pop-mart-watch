-- Pop Mart Watch Database Schema
-- MySQL 5.7+ / 8.0+

-- Create database (run this manually on your MySQL server)
CREATE DATABASE IF NOT EXISTS popmart_watch CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
USE popmart_watch;

-- Drop tables if they exist (for clean reinstall)
DROP TABLE IF EXISTS stock_check_history;
DROP TABLE IF EXISTS monitored_products;

-- Monitored Products Table
CREATE TABLE monitored_products (
    id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '主键ID',
    url VARCHAR(500) NOT NULL COMMENT '商品URL',
    product_id VARCHAR(20) COMMENT '商品ID（从URL中提取）',
    product_name VARCHAR(255) COMMENT '商品名称',
    is_active BOOLEAN DEFAULT TRUE COMMENT '是否激活监控',
    last_known_stock BOOLEAN DEFAULT FALSE COMMENT '最后已知库存状态',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    last_checked_at TIMESTAMP NULL COMMENT '最后检查时间',
    last_error TEXT COMMENT '最后错误信息',
    added_by_user_id VARCHAR(100) NOT NULL COMMENT '添加用户ID',
    deleted TINYINT DEFAULT 0 COMMENT '逻辑删除标记'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='监控商品表';

-- Create indexes for monitored_products
CREATE INDEX idx_monitored_products_url ON monitored_products(url);
CREATE INDEX idx_monitored_products_product_id ON monitored_products(product_id);
CREATE INDEX idx_monitored_products_user_id ON monitored_products(added_by_user_id);
CREATE INDEX idx_monitored_products_active ON monitored_products(is_active);
CREATE INDEX idx_monitored_products_deleted ON monitored_products(deleted);

-- Stock Check History Table
CREATE TABLE stock_check_history (
    id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '主键ID',
    product_id BIGINT NOT NULL COMMENT '商品ID',
    in_stock BOOLEAN NOT NULL COMMENT '是否有库存',
    response_time INT COMMENT '响应时间(毫秒)',
    checked_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT '检查时间',
    error_message TEXT COMMENT '错误信息',
    stock_changed BOOLEAN DEFAULT FALSE COMMENT '库存状态是否发生变化',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    deleted TINYINT DEFAULT 0 COMMENT '逻辑删除标记'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='库存检查历史表';

-- Create indexes for stock_check_history
CREATE INDEX idx_stock_check_history_product_id ON stock_check_history(product_id);
CREATE INDEX idx_stock_check_history_checked_at ON stock_check_history(checked_at);
CREATE INDEX idx_stock_check_history_deleted ON stock_check_history(deleted);

-- Add foreign key constraint
ALTER TABLE stock_check_history 
ADD CONSTRAINT fk_stock_check_history_product_id 
FOREIGN KEY (product_id) REFERENCES monitored_products(id) 
ON DELETE CASCADE ON UPDATE CASCADE;

-- Insert sample data (optional)
-- INSERT INTO monitored_products (url, product_name, added_by_user_id) VALUES
-- ('https://www.popmart.com/us/products/1739/', 'Molly Space Series', 'sample_user_123'); 
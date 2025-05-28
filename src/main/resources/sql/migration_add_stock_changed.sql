-- Migration script to add stock_changed field to existing database
-- Run this on your existing MySQL database

USE popmart_watch;

-- Add stock_changed column to stock_check_history table
ALTER TABLE stock_check_history 
ADD COLUMN stock_changed BOOLEAN DEFAULT FALSE COMMENT '库存状态是否发生变化' 
AFTER error_message;

-- Update existing records to set stock_changed = false (default)
UPDATE stock_check_history SET stock_changed = FALSE WHERE stock_changed IS NULL;

-- Verify the change
DESCRIBE stock_check_history; 
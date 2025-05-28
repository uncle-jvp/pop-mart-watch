package com.popmart.entity;

import com.baomidou.mybatisplus.annotation.*;
import java.time.LocalDateTime;

@TableName("monitored_products")
public class MonitoredProduct {
    
    @TableId(type = IdType.AUTO)
    private Long id;
    
    @TableField("url")
    private String url;
    
    @TableField("product_id")
    private String productId;
    
    @TableField("product_name")
    private String productName;
    
    @TableField("is_active")
    private Boolean isActive = true;
    
    @TableField("last_known_stock")
    private Boolean lastKnownStock = false;
    
    @TableField(value = "created_at", fill = FieldFill.INSERT)
    private LocalDateTime createdAt;
    
    @TableField(value = "updated_at", fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;
    
    @TableField("last_checked_at")
    private LocalDateTime lastCheckedAt;
    
    @TableField("last_error")
    private String lastError;
    
    @TableField("added_by_user_id")
    private String addedByUserId;
    
    @TableLogic
    @TableField("deleted")
    private Integer deleted = 0;
    
    public MonitoredProduct() {
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }
    
    public MonitoredProduct(String url, String productName, String addedByUserId) {
        this();
        this.url = url;
        this.productName = productName;
        this.addedByUserId = addedByUserId;
    }
    
    // Getters and Setters
    public Long getId() {
        return id;
    }
    
    public void setId(Long id) {
        this.id = id;
    }
    
    public String getUrl() {
        return url;
    }
    
    public void setUrl(String url) {
        this.url = url;
    }
    
    public String getProductId() {
        return productId;
    }
    
    public void setProductId(String productId) {
        this.productId = productId;
    }
    
    public String getProductName() {
        return productName;
    }
    
    public void setProductName(String productName) {
        this.productName = productName;
    }
    
    public Boolean getIsActive() {
        return isActive;
    }
    
    public void setIsActive(Boolean isActive) {
        this.isActive = isActive;
    }
    
    public Boolean getLastKnownStock() {
        return lastKnownStock;
    }
    
    public void setLastKnownStock(Boolean lastKnownStock) {
        this.lastKnownStock = lastKnownStock;
    }
    
    public LocalDateTime getCreatedAt() {
        return createdAt;
    }
    
    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
    
    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }
    
    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }
    
    public LocalDateTime getLastCheckedAt() {
        return lastCheckedAt;
    }
    
    public void setLastCheckedAt(LocalDateTime lastCheckedAt) {
        this.lastCheckedAt = lastCheckedAt;
    }
    
    public String getLastError() {
        return lastError;
    }
    
    public void setLastError(String lastError) {
        this.lastError = lastError;
    }
    
    public String getAddedByUserId() {
        return addedByUserId;
    }
    
    public void setAddedByUserId(String addedByUserId) {
        this.addedByUserId = addedByUserId;
    }
    
    public Integer getDeleted() {
        return deleted;
    }
    
    public void setDeleted(Integer deleted) {
        this.deleted = deleted;
    }
    
    @Override
    public String toString() {
        return "MonitoredProduct{" +
                "id=" + id +
                ", url='" + url + '\'' +
                ", productId='" + productId + '\'' +
                ", productName='" + productName + '\'' +
                ", isActive=" + isActive +
                ", lastKnownStock=" + lastKnownStock +
                ", lastCheckedAt=" + lastCheckedAt +
                '}';
    }
} 
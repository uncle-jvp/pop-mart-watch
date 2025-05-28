package com.popmart.entity;

import com.baomidou.mybatisplus.annotation.*;
import java.time.LocalDateTime;

@TableName("stock_check_history")
public class StockCheckHistory {
    
    @TableId(type = IdType.AUTO)
    private Long id;
    
    @TableField("product_id")
    private Long productId;
    
    @TableField("in_stock")
    private Boolean inStock;
    
    @TableField(value = "checked_at", fill = FieldFill.INSERT)
    private LocalDateTime checkedAt;
    
    @TableField("error_message")
    private String errorMessage;
    
    @TableField("response_time")
    private Integer responseTime; // in milliseconds
    
    // @TableField("stock_changed")  // 暂时注释掉，数据库表中没有此字段
    // private Boolean stockChanged = false;
    
    public StockCheckHistory() {
        this.checkedAt = LocalDateTime.now();
    }
    
    public StockCheckHistory(MonitoredProduct product, Boolean inStock) {
        this();
        this.productId = product.getId();
        this.inStock = inStock;
    }
    
    public StockCheckHistory(MonitoredProduct product, String errorMessage) {
        this();
        this.productId = product.getId();
        this.inStock = false;
        this.errorMessage = errorMessage;
    }
    
    // Getters and Setters
    public Long getId() {
        return id;
    }
    
    public void setId(Long id) {
        this.id = id;
    }
    
    public Long getProductId() {
        return productId;
    }
    
    public void setProductId(Long productId) {
        this.productId = productId;
    }
    
    public Boolean getInStock() {
        return inStock;
    }
    
    public void setInStock(Boolean inStock) {
        this.inStock = inStock;
    }
    
    public LocalDateTime getCheckedAt() {
        return checkedAt;
    }
    
    public void setCheckedAt(LocalDateTime checkedAt) {
        this.checkedAt = checkedAt;
    }
    
    public String getErrorMessage() {
        return errorMessage;
    }
    
    public void setErrorMessage(String errorMessage) {
        this.errorMessage = errorMessage;
    }
    
    public Integer getResponseTime() {
        return responseTime;
    }
    
    public void setResponseTime(Integer responseTime) {
        this.responseTime = responseTime;
    }
    
    // @TableField("stock_changed")  // 暂时注释掉，数据库表中没有此字段
    // public Boolean getStockChanged() {
    //     return stockChanged;
    // }
    
    // @TableField("stock_changed")  // 暂时注释掉，数据库表中没有此字段
    // public void setStockChanged(Boolean stockChanged) {
    //     this.stockChanged = stockChanged;
    // }
    
    @Override
    public String toString() {
        return "StockCheckHistory{" +
                "id=" + id +
                ", productId=" + productId +
                ", inStock=" + inStock +
                ", checkedAt=" + checkedAt +
                // ", stockChanged=" + stockChanged +
                '}';
    }
} 
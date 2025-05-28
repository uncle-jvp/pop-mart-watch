package com.popmart.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 测试库存响应
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TestStockResponse {
    
    /**
     * 商品 URL
     */
    private String url;
    
    /**
     * 是否有库存
     */
    private Boolean inStock;
    
    /**
     * 响应时间（毫秒）
     */
    private Integer responseTime;
    
    /**
     * 检测时间
     */
    private LocalDateTime timestamp;
    
    /**
     * 错误信息
     */
    private String error;
} 
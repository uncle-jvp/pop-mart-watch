package com.popmart.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 库存检查结果
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StockCheckResult {
    
    /**
     * 是否有库存
     */
    private Boolean inStock;
    
    /**
     * 响应时间（毫秒）
     */
    private Integer responseTime;
    
    /**
     * 错误信息
     */
    private String errorMessage;
    
    /**
     * 是否有错误
     */
    public boolean hasError() {
        return errorMessage != null && !errorMessage.trim().isEmpty();
    }
} 
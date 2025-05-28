package com.popmart.dto.response;

import com.popmart.service.MonitoringService;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

/**
 * 监控统计信息
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MonitoringStats {
    
    /**
     * 总商品数
     */
    private Integer totalProducts;
    
    /**
     * 有库存商品数
     */
    private Integer inStockCount;
    
    /**
     * 无库存商品数
     */
    private Integer outOfStockCount;
    
    /**
     * 优先级分布
     */
    private Map<MonitoringService.Priority, Long> priorityDistribution;
    
    @Override
    public String toString() {
        return String.format("MonitoringStats[total=%d, inStock=%d, outOfStock=%d, priorities=%s]",
            totalProducts, inStockCount, outOfStockCount, priorityDistribution);
    }
} 
package com.popmart.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 性能测试结果
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PerformanceTestResult {
    
    /**
     * 总测试次数
     */
    private Integer totalIterations;
    
    /**
     * 成功次数
     */
    private Integer successCount;
    
    /**
     * 错误次数
     */
    private Integer errorCount;
    
    /**
     * 平均响应时间（毫秒）
     */
    private Double averageTime;
    
    /**
     * 最小响应时间（毫秒）
     */
    private Long minTime;
    
    /**
     * 最大响应时间（毫秒）
     */
    private Long maxTime;
    
    /**
     * 成功率（百分比）
     */
    public Double getSuccessRate() {
        if (totalIterations == null || totalIterations == 0) {
            return 0.0;
        }
        return (successCount / (double) totalIterations) * 100;
    }
    
    @Override
    public String toString() {
        return String.format("PerformanceTest[iterations=%d, success=%d, errors=%d, avgTime=%.1fms, minTime=%dms, maxTime=%dms, successRate=%.1f%%]",
            totalIterations, successCount, errorCount, averageTime, minTime, maxTime, getSuccessRate());
    }
} 
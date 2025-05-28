package com.popmart.service;

import com.popmart.config.PopMartConfig;
import com.popmart.dto.response.PerformanceTestResult;
import com.popmart.dto.response.StockCheckResult;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;
import java.lang.reflect.Field;

@SpringBootTest
@TestPropertySource(properties = {
    "popmart.monitor.selenium.headless=true",
    "popmart.monitor.selenium.performance.page-load-timeout=20",
    "popmart.monitor.selenium.performance.script-timeout=15",
    "popmart.monitor.selenium.performance.implicit-wait=8",
    "popmart.monitor.selenium.performance.smart-wait-timeout=15",
    "popmart.monitor.selenium.performance.disable-images=false",
    "popmart.monitor.selenium.performance.eager-loading=false",
    "popmart.monitor.selenium.performance.cache-duration=1000",
    "popmart.monitor.selenium.performance.http-check-timeout=8000",
    "popmart.monitor.stock-detection.keyword=Add to Bag",
    "popmart.monitor.selenium.user-agent=Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/91.0.4472.124 Safari/537.36",
    "spring.autoconfigure.exclude=org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration,com.alibaba.druid.spring.boot.autoconfigure.DruidDataSourceAutoConfigure"
})
public class WebScrapingServiceTest {

    private WebScrapingService webScrapingService;
    private PopMartConfig config;

    @BeforeEach
    public void setUp() {
        // 手动创建配置
        config = new PopMartConfig();
        
        // 设置监控配置
        PopMartConfig.Monitor monitor = new PopMartConfig.Monitor();
        config.setMonitor(monitor);
        
        // 设置Selenium配置
        PopMartConfig.Selenium selenium = new PopMartConfig.Selenium();
        selenium.setHeadless(true);
        monitor.setSelenium(selenium);
        
        // 设置性能配置
        PopMartConfig.Performance performance = new PopMartConfig.Performance();
        performance.setPageLoadTimeout(20);
        performance.setScriptTimeout(15);
        performance.setImplicitWait(8);
        performance.setSmartWaitTimeout(15);
        performance.setDisableImages(false);
        performance.setEagerLoading(false);
        performance.setCacheDuration(1000);
        performance.setHttpCheckTimeout(8000);
        selenium.setPerformance(performance);
        
        selenium.setUserAgent("Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/91.0.4472.124 Safari/537.36");
        
        // 设置库存检测配置
        PopMartConfig.StockDetection stockDetection = new PopMartConfig.StockDetection();
        stockDetection.setKeyword("Add to Bag");
        monitor.setStockDetection(stockDetection);
        
        // 创建服务实例
        webScrapingService = new WebScrapingService();
        
        // 使用反射设置config字段
        try {
            Field configField = WebScrapingService.class.getDeclaredField("config");
            configField.setAccessible(true);
            configField.set(webScrapingService, config);
        } catch (Exception e) {
            throw new RuntimeException("Failed to set config field", e);
        }
        
        // 初始化WebDriver
        webScrapingService.initializeDriverPool();
    }

    @Test
    public void testPopMartStockCheck() {
        String testUrl = "https://www.popmart.com/us/products/1739/THE-MONSTERS-Classic-Series-Sparkly-Plush-Pendant-Blind-Box";
        
        System.out.println("🎯 测试 Pop Mart 库存检测");
        System.out.println("URL: " + testUrl);
        System.out.println("================================");
        
        try {
            StockCheckResult result = webScrapingService.checkStock(testUrl);
            
            System.out.println("📊 检测结果:");
            System.out.println("   库存状态: " + (result.getInStock() ? "有货" : "缺货"));
            System.out.println("   响应时间: " + result.getResponseTime() + "ms");
            
            if (result.hasError()) {
                System.out.println("   错误信息: " + result.getErrorMessage());
            }
            
            System.out.println("================================");
            
            // 验证响应时间是否在合理范围内（应该 < 5秒）
            if (result.getResponseTime() < 5000) {
                System.out.println("✅ 性能测试通过：响应时间 " + result.getResponseTime() + "ms < 5000ms");
            } else {
                System.out.println("⚠️  性能警告：响应时间 " + result.getResponseTime() + "ms >= 5000ms");
            }
            
        } catch (Exception e) {
            System.err.println("❌ 测试失败: " + e.getMessage());
            e.printStackTrace();
        } finally {
            // 清理资源
            webScrapingService.closeAllDrivers();
        }
    }

    @Test
    public void testPerformanceOptimization() {
        String testUrl = "https://www.popmart.com/us/products/1739/";
        
        System.out.println("🚀 性能优化测试");
        System.out.println("URL: " + testUrl);
        System.out.println("测试次数: 3");
        System.out.println("================================");
        
        try {
            PerformanceTestResult result = webScrapingService.performanceTest(testUrl, 3);
            
            System.out.println("📈 性能测试结果:");
            System.out.println("   总测试次数: " + result.getTotalIterations());
            System.out.println("   成功次数: " + result.getSuccessCount());
            System.out.println("   失败次数: " + result.getErrorCount());
            System.out.println("   平均响应时间: " + String.format("%.1f", result.getAverageTime()) + "ms");
            System.out.println("   最快响应时间: " + result.getMinTime() + "ms");
            System.out.println("   最慢响应时间: " + result.getMaxTime() + "ms");
            System.out.println("   成功率: " + String.format("%.1f", result.getSuccessRate()) + "%");
            
            System.out.println("================================");
            
            // 验证性能目标
            if (result.getAverageTime() <= 3000) {
                System.out.println("🎉 性能目标达成：平均响应时间 " + String.format("%.1f", result.getAverageTime()) + "ms <= 3000ms");
            } else if (result.getAverageTime() <= 5000) {
                System.out.println("✅ 性能良好：平均响应时间 " + String.format("%.1f", result.getAverageTime()) + "ms <= 5000ms");
            } else {
                System.out.println("⚠️  性能需要优化：平均响应时间 " + String.format("%.1f", result.getAverageTime()) + "ms > 5000ms");
            }
            
        } catch (Exception e) {
            System.err.println("❌ 性能测试失败: " + e.getMessage());
            e.printStackTrace();
        } finally {
            // 清理资源
            webScrapingService.closeAllDrivers();
        }
    }
} 
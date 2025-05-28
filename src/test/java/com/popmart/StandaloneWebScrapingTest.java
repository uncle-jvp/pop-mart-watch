package com.popmart;

import com.popmart.config.PopMartConfig;
import com.popmart.dto.response.PerformanceTestResult;
import com.popmart.dto.response.StockCheckResult;
import com.popmart.service.WebScrapingService;

public class StandaloneWebScrapingTest {

    public static void main(String[] args) {
        System.out.println("🎯 Pop Mart 独立检测测试");
        System.out.println("================================");
        
        // 创建配置
        PopMartConfig config = createTestConfig();
        
        // 创建服务实例
        WebScrapingService webScrapingService = new WebScrapingService();
        
        try {
            // 使用反射设置config字段
            java.lang.reflect.Field configField = WebScrapingService.class.getDeclaredField("config");
            configField.setAccessible(true);
            configField.set(webScrapingService, config);
            
            // 初始化WebDriver
            webScrapingService.initializeDriverPool();
            
            // 测试URL
            String testUrl = "https://www.popmart.com/us/products/1739/THE-MONSTERS-Classic-Series-Sparkly-Plush-Pendant-Blind-Box";
            
            System.out.println("🔍 测试URL: " + testUrl);
            System.out.println("⏱️  开始检测...");
            
            long startTime = System.currentTimeMillis();
            
            // 执行检测
            StockCheckResult result = webScrapingService.checkStock(testUrl);
            
            long endTime = System.currentTimeMillis();
            long totalTime = endTime - startTime;
            
            System.out.println("================================");
            System.out.println("📊 检测结果:");
            System.out.println("   库存状态: " + (result.getInStock() ? "✅ 有货" : "❌ 缺货"));
            System.out.println("   内部响应时间: " + result.getResponseTime() + "ms");
            System.out.println("   总响应时间: " + totalTime + "ms");
            
            if (result.hasError()) {
                System.out.println("   错误信息: " + result.getErrorMessage());
            }
            
            System.out.println("================================");
            
            // 性能评估
            if (result.getResponseTime() <= 2000) {
                System.out.println("🎉 性能优秀：响应时间 " + result.getResponseTime() + "ms <= 2000ms");
            } else if (result.getResponseTime() <= 3000) {
                System.out.println("✅ 性能良好：响应时间 " + result.getResponseTime() + "ms <= 3000ms");
            } else if (result.getResponseTime() <= 5000) {
                System.out.println("⚠️  性能一般：响应时间 " + result.getResponseTime() + "ms <= 5000ms");
            } else {
                System.out.println("❌ 性能需要优化：响应时间 " + result.getResponseTime() + "ms > 5000ms");
            }
            
            // 如果检测到有货，进行额外验证
            if (result.getInStock()) {
                System.out.println("");
                System.out.println("🎉 成功检测到 'Add to Bag' 按钮！");
                System.out.println("✅ Pop Mart 特定检测算法工作正常");
                System.out.println("✅ 性能优化措施生效");
            } else {
                System.out.println("");
                System.out.println("ℹ️  当前商品显示为缺货状态");
                System.out.println("💡 这可能是正常的，如果商品确实缺货");
            }
            
            // 运行性能测试
            if (args.length > 0 && "performance".equals(args[0])) {
                System.out.println("");
                System.out.println("🚀 运行性能测试...");
                System.out.println("================================");
                
                PerformanceTestResult perfResult =
                    webScrapingService.performanceTest(testUrl, 3);
                
                System.out.println("📈 性能测试结果:");
                System.out.println("   总测试次数: " + perfResult.getTotalIterations());
                System.out.println("   成功次数: " + perfResult.getSuccessCount());
                System.out.println("   失败次数: " + perfResult.getErrorCount());
                System.out.println("   平均响应时间: " + String.format("%.1f", perfResult.getAverageTime()) + "ms");
                System.out.println("   最快响应时间: " + perfResult.getMinTime() + "ms");
                System.out.println("   最慢响应时间: " + perfResult.getMaxTime() + "ms");
                System.out.println("   成功率: " + String.format("%.1f", perfResult.getSuccessRate()) + "%");
                
                // 性能目标评估
                if (perfResult.getAverageTime() <= 3000) {
                    System.out.println("🎉 性能目标达成：平均响应时间 " + String.format("%.1f", perfResult.getAverageTime()) + "ms <= 3000ms");
                } else if (perfResult.getAverageTime() <= 5000) {
                    System.out.println("✅ 性能良好：平均响应时间 " + String.format("%.1f", perfResult.getAverageTime()) + "ms <= 5000ms");
                } else {
                    System.out.println("⚠️  性能需要优化：平均响应时间 " + String.format("%.1f", perfResult.getAverageTime()) + "ms > 5000ms");
                }
            }
            
        } catch (Exception e) {
            System.err.println("❌ 测试失败: " + e.getMessage());
            e.printStackTrace();
        } finally {
            // 清理资源
            try {
                webScrapingService.closeAllDrivers();
                System.out.println("");
                System.out.println("🧹 资源清理完成");
            } catch (Exception e) {
                System.err.println("清理资源时出错: " + e.getMessage());
            }
        }
        
        System.out.println("");
        System.out.println("✨ 测试完成！");
    }
    
    private static PopMartConfig createTestConfig() {
        PopMartConfig config = new PopMartConfig();
        
        // 设置监控配置
        PopMartConfig.Monitor monitor = new PopMartConfig.Monitor();
        config.setMonitor(monitor);
        
        // 设置Selenium配置
        PopMartConfig.Selenium selenium = new PopMartConfig.Selenium();
        selenium.setHeadless(true);  // 使用headless模式
        monitor.setSelenium(selenium);
        
        // 设置性能配置 - 使用优化后的参数
        PopMartConfig.Performance performance = new PopMartConfig.Performance();
        performance.setPageLoadTimeout(15);      // 页面加载超时
        performance.setScriptTimeout(10);        // 脚本执行超时
        performance.setImplicitWait(5);          // 隐式等待
        performance.setSmartWaitTimeout(10);     // 智能等待超时
        performance.setDisableImages(false);     // 启用图片以获得完整页面
        performance.setEagerLoading(false);      // 禁用eager loading，等待完整页面
        performance.setCacheDuration(5000);      // 短缓存用于测试
        performance.setHttpCheckTimeout(5000);   // HTTP检查超时
        selenium.setPerformance(performance);
        
        selenium.setUserAgent("Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/91.0.4472.124 Safari/537.36");
        
        // 设置库存检测配置
        PopMartConfig.StockDetection stockDetection = new PopMartConfig.StockDetection();
        stockDetection.setKeyword("Add to Bag");
        monitor.setStockDetection(stockDetection);
        
        return config;
    }
} 
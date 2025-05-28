package com.popmart.controller;

import com.popmart.dto.request.AddProductRequest;
import com.popmart.dto.request.TestProductRequest;
import com.popmart.dto.response.ApiResponse;
import com.popmart.dto.response.MonitoringStats;
import com.popmart.dto.response.TestStockResponse;
import com.popmart.entity.MonitoredProduct;
import com.popmart.entity.StockCheckHistory;
import com.popmart.service.MonitoringService;
import com.popmart.service.NotificationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;
import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequestMapping("/api/monitor")
@CrossOrigin(origins = "*")
public class MonitoringController {
    
    @Autowired
    private MonitoringService monitoringService;
    
    @Autowired
    private NotificationService notificationService;
    
    @GetMapping("/products")
    public ResponseEntity<ApiResponse<List<MonitoredProduct>>> getAllProducts() {
        List<MonitoredProduct> products = monitoringService.getAllActiveProducts();
        return ResponseEntity.ok(ApiResponse.success(products));
    }
    
    @GetMapping("/products/user/{userId}")
    public ResponseEntity<ApiResponse<List<MonitoredProduct>>> getUserProducts(@PathVariable String userId) {
        List<MonitoredProduct> products = monitoringService.getUserProducts(userId);
        return ResponseEntity.ok(ApiResponse.success(products));
    }
    
    @PostMapping("/products")
    public ResponseEntity<ApiResponse<MonitoredProduct>> addProduct(@Valid @RequestBody AddProductRequest request) {
        try {
            MonitoredProduct product = monitoringService.addProduct(
                request.getUrl(), 
                request.getProductName(), 
                request.getUserId()
            );
            return ResponseEntity.ok(ApiResponse.success("商品添加成功", product));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(ApiResponse.error(400, e.getMessage()));
        }
    }
    
    @DeleteMapping("/products/{productId}")
    public ResponseEntity<ApiResponse<String>> removeProduct(@PathVariable Long productId, @RequestParam String userId) {
        try {
            monitoringService.removeProduct(productId, userId);
            return ResponseEntity.ok(ApiResponse.success("商品移除成功", "Product removed successfully"));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(ApiResponse.error(400, e.getMessage()));
        }
    }
    
    @PostMapping("/products/{productId}/check")
    public ResponseEntity<ApiResponse<StockCheckHistory>> checkProductStock(@PathVariable Long productId, @RequestParam String userId) {
        try {
            StockCheckHistory result = monitoringService.checkProductById(productId, userId);
            return ResponseEntity.ok(ApiResponse.success("库存检查完成", result));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(ApiResponse.error(400, e.getMessage()));
        }
    }
    
    @GetMapping("/stats")
    public ResponseEntity<ApiResponse<MonitoringStats>> getStats() {
        MonitoringStats stats = monitoringService.getMonitoringStats();
        
        return ResponseEntity.ok(ApiResponse.success(stats));
    }
    
    @GetMapping("/health")
    public ResponseEntity<ApiResponse<String>> health() {
        String healthInfo = String.format("Pop Mart Monitor - %s", LocalDateTime.now().toString());
        return ResponseEntity.ok(ApiResponse.success("系统运行正常", healthInfo));
    }
    
    @PostMapping("/test")
    public ResponseEntity<ApiResponse<TestStockResponse>> testProductStock(@Valid @RequestBody TestProductRequest request) {
        try {
            StockCheckHistory result = monitoringService.testProductStock(request.getUrl());
            
            TestStockResponse response = TestStockResponse.builder()
                    .url(request.getUrl())
                    .inStock(result.getInStock())
                    .responseTime(result.getResponseTime())
                    .timestamp(result.getCheckedAt())
                    .error(result.getErrorMessage())
                    .build();
            
            return ResponseEntity.ok(ApiResponse.success("库存检测完成", response));
        } catch (Exception e) {
            TestStockResponse errorResponse = TestStockResponse.builder()
                    .url(request.getUrl())
                    .error(e.getMessage())
                    .timestamp(LocalDateTime.now())
                    .build();
            return ResponseEntity.badRequest().body(ApiResponse.error(400, "库存检测失败"));
        }
    }
    
    @PostMapping("/test-discord")
    public ResponseEntity<ApiResponse<String>> testDiscordNotification() {
        try {
            // 创建一个测试商品对象，使用真实的Pop Mart URL
            MonitoredProduct testProduct = new MonitoredProduct();
            testProduct.setId(999L);
            testProduct.setProductId("1739"); // 设置商品ID
            testProduct.setProductName("测试商品 - THE MONSTERS Classic Series Sparkly Plush Pendant Blind Box");
            testProduct.setUrl("https://www.popmart.com/us/products/1739/THE-MONSTERS-Classic-Series-Sparkly-Plush-Pendant-Blind-Box");
            testProduct.setLastKnownStock(true);
            testProduct.setAddedByUserId("test-user-discord");
            testProduct.setLastCheckedAt(LocalDateTime.now());
            
            // 发送测试通知
            notificationService.sendStockAlert(testProduct);
            
            return ResponseEntity.ok(ApiResponse.success("Discord 测试通知已发送", "请检查您的 Discord 频道是否收到通知，消息中应包含商品 ID: 1739"));
            
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(ApiResponse.error(400, "发送 Discord 测试通知失败: " + e.getMessage()));
        }
    }
} 
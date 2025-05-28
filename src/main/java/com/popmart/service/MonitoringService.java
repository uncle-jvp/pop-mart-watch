package com.popmart.service;

import com.popmart.config.PopMartConfig;
import com.popmart.dto.response.MonitoringStats;
import com.popmart.dto.response.StockCheckResult;
import com.popmart.entity.MonitoredProduct;
import com.popmart.entity.StockCheckHistory;
import com.popmart.repository.MonitoredProductRepository;
import com.popmart.repository.StockCheckHistoryRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ConcurrentHashMap;

import static com.popmart.utils.urlUtils.isValidPopMartUrl;
import static com.popmart.utils.urlUtils.extractProductId;
import static com.popmart.utils.urlUtils.extractProductNameFromUrl;

@Service
public class MonitoringService {
    
    private static final Logger logger = LoggerFactory.getLogger(MonitoringService.class);
    
    @Autowired
    private MonitoredProductRepository productRepository;
    
    @Autowired
    private StockCheckHistoryRepository historyRepository;
    
    @Autowired
    private WebScrapingService webScrapingService;
    
    @Autowired
    private NotificationService notificationService;
    
    @Autowired
    private PopMartConfig config;
    
    // 智能轮询频率管理
    private final Map<Long, ProductPriority> productPriorities = new ConcurrentHashMap<>();
    private final ExecutorService monitoringExecutor = Executors.newFixedThreadPool(5);
    
    // 商品优先级枚举
    public enum Priority {
        HIGH(1),    // 1分钟检查一次
        MEDIUM(3),  // 3分钟检查一次  
        LOW(5),     // 5分钟检查一次
        COLD(10);   // 10分钟检查一次
        
        private final int intervalMinutes;
        
        Priority(int intervalMinutes) {
            this.intervalMinutes = intervalMinutes;
        }
        
        public int getIntervalMinutes() {
            return intervalMinutes;
        }
    }
    
    // 商品优先级信息
    private static class ProductPriority {
        private Priority priority;
        private LocalDateTime lastCheck;
        private int consecutiveOutOfStock;
        private int totalChecks;
        private int stockChanges;
        
        public ProductPriority() {
            this.priority = Priority.MEDIUM;
            this.lastCheck = LocalDateTime.now().minusHours(1);
            this.consecutiveOutOfStock = 0;
            this.totalChecks = 0;
            this.stockChanges = 0;
        }
        
        // Getters and setters
        public Priority getPriority() { return priority; }
        public void setPriority(Priority priority) { this.priority = priority; }
        public LocalDateTime getLastCheck() { return lastCheck; }
        public void setLastCheck(LocalDateTime lastCheck) { this.lastCheck = lastCheck; }
        public int getConsecutiveOutOfStock() { return consecutiveOutOfStock; }
        public void setConsecutiveOutOfStock(int consecutiveOutOfStock) { this.consecutiveOutOfStock = consecutiveOutOfStock; }
        public int getTotalChecks() { return totalChecks; }
        public void setTotalChecks(int totalChecks) { this.totalChecks = totalChecks; }
        public int getStockChanges() { return stockChanges; }
        public void setStockChanges(int stockChanges) { this.stockChanges = stockChanges; }
        
        public void incrementTotalChecks() { this.totalChecks++; }
        public void incrementStockChanges() { this.stockChanges++; }
        public void incrementConsecutiveOutOfStock() { this.consecutiveOutOfStock++; }
        public void resetConsecutiveOutOfStock() { this.consecutiveOutOfStock = 0; }
    }
    
    @Scheduled(fixedRateString = "#{${popmart.monitor.poll-interval} * 60 * 1000}")
    public void checkAllProducts() {
        logger.info("Starting intelligent monitoring cycle");
        
        List<MonitoredProduct> products = productRepository.findByIsActiveTrue();
        if (products.isEmpty()) {
            logger.debug("No products to monitor");
            return;
        }
        
        logger.info("Found {} products to monitor with intelligent scheduling", products.size());
        
        // 并发检查所有需要检查的商品
        List<CompletableFuture<Void>> futures = products.stream()
            .filter(this::shouldCheckProduct)
            .map(product -> CompletableFuture.runAsync(() -> {
                try {
                    checkSingleProduct(product);
                } catch (Exception e) {
                    logger.error("Error checking product {}: {}", product.getId(), e.getMessage());
                }
            }, monitoringExecutor))
            .collect(java.util.stream.Collectors.toList());
        
        // 等待所有检查完成
        CompletableFuture.allOf(futures.toArray(new CompletableFuture[0]))
            .thenRun(() -> logger.info("Intelligent monitoring cycle completed"))
            .exceptionally(throwable -> {
                logger.error("Error in monitoring cycle: {}", throwable.getMessage());
                return null;
            });
    }
    
    /**
     * 判断商品是否需要检查（基于智能调度）
     */
    private boolean shouldCheckProduct(MonitoredProduct product) {
        ProductPriority priority = productPriorities.computeIfAbsent(product.getId(), k -> new ProductPriority());
        
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime nextCheckTime = priority.getLastCheck().plusMinutes(priority.getPriority().getIntervalMinutes());
        
        boolean shouldCheck = now.isAfter(nextCheckTime);
        
        if (shouldCheck) {
            logger.debug("Product {} (priority: {}) is due for check", product.getId(), priority.getPriority());
        }
        
        return shouldCheck;
    }
    
    /**
     * 检查单个商品并更新优先级
     */
    private void checkSingleProduct(MonitoredProduct product) {
        long startTime = System.currentTimeMillis();
        
        try {
            logger.debug("Checking product: {} - {}", product.getId(), product.getProductName());
            
            StockCheckResult result = webScrapingService.checkStock(product.getUrl());
            
            // 更新优先级信息
            ProductPriority priority = productPriorities.get(product.getId());
            priority.setLastCheck(LocalDateTime.now());
            priority.incrementTotalChecks();
            
            // 检查库存状态变化
            boolean currentInStock = result.getInStock();
            priority.incrementStockChanges();
            // 发送通知
            if (currentInStock) {
                logger.info("Sending notification: Product {} is now IN STOCK", product.getId());
                notificationService.sendStockAlert(product);
            }
            
            // 更新商品状态
            product.setLastKnownStock(currentInStock);
            product.setLastCheckedAt(LocalDateTime.now());
            productRepository.updateById(product);
            
            // 记录检查历史
            StockCheckHistory history = new StockCheckHistory();
            history.setProductId(product.getId());
            history.setInStock(currentInStock);
            history.setResponseTime(result.getResponseTime());
            history.setErrorMessage(result.getErrorMessage());
            history.setCheckedAt(LocalDateTime.now());
            
            // Check if stock status changed
            boolean stockChanged = !Boolean.valueOf(result.getInStock()).equals(product.getLastKnownStock());
            history.setStockChanged(stockChanged);
            
            if (stockChanged) {
                logger.info("Stock status changed for {}: {} -> {}", 
                    product.getProductName(), 
                    product.getLastKnownStock() ? "IN STOCK" : "OUT OF STOCK",
                    result.getInStock() ? "IN STOCK" : "OUT OF STOCK");
                
                // Send notification if product came back in stock
                if (result.getInStock()) {
                    notificationService.sendStockAlert(product);
                }
            }
            
            historyRepository.insert(history);
            
            // 动态调整优先级
            adjustProductPriority(product, priority, currentInStock);
            
            long duration = System.currentTimeMillis() - startTime;
            logger.debug("Product {} check completed in {}ms, status: {}, priority: {}", 
                product.getId(), duration, currentInStock ? "IN_STOCK" : "OUT_OF_STOCK", priority.getPriority());
            
        } catch (Exception e) {
            logger.error("Error checking product {}: {}", product.getId(), e.getMessage());
            
            // 记录错误历史
            StockCheckHistory history = new StockCheckHistory();
            history.setProductId(product.getId());
            history.setInStock(false);
            history.setResponseTime(-1);
            history.setErrorMessage(e.getMessage());
            history.setCheckedAt(LocalDateTime.now());
            historyRepository.insert(history);
        }
    }
    
    /**
     * 动态调整商品检查优先级
     */
    private void adjustProductPriority(MonitoredProduct product, ProductPriority priority, boolean currentInStock) {
        if (currentInStock) {
            // 有库存：提高优先级，更频繁检查
            priority.resetConsecutiveOutOfStock();
            priority.setPriority(Priority.HIGH);
            logger.debug("Product {} in stock - priority set to HIGH", product.getId());
        } else {
            priority.incrementConsecutiveOutOfStock();
            
            // 根据连续缺货次数调整优先级
            if (priority.getConsecutiveOutOfStock() >= 20) {
                // 连续20次缺货：冷门商品
                priority.setPriority(Priority.COLD);
                logger.debug("Product {} cold (20+ out of stock) - priority set to COLD", product.getId());
            } else if (priority.getConsecutiveOutOfStock() >= 10) {
                // 连续10次缺货：低优先级
                priority.setPriority(Priority.LOW);
                logger.debug("Product {} low priority (10+ out of stock) - priority set to LOW", product.getId());
            } else if (priority.getStockChanges() > 3) {
                // 库存变化频繁：中等优先级
                priority.setPriority(Priority.MEDIUM);
                logger.debug("Product {} active ({}+ stock changes) - priority set to MEDIUM", 
                    product.getId(), priority.getStockChanges());
            } else {
                // 默认中等优先级
                priority.setPriority(Priority.MEDIUM);
            }
        }
    }
    
    /**
     * 获取监控统计信息
     */
    public MonitoringStats getMonitoringStats() {
        List<MonitoredProduct> allProducts = productRepository.selectList(null);
        
        int totalProducts = allProducts.size();
        int inStockCount = (int) allProducts.stream().filter(p -> p.getLastKnownStock() != null && p.getLastKnownStock()).count();
        int outOfStockCount = totalProducts - inStockCount;
        
        // 按优先级统计
        Map<Priority, Long> priorityStats = productPriorities.values().stream()
            .collect(java.util.stream.Collectors.groupingBy(
                ProductPriority::getPriority,
                java.util.stream.Collectors.counting()
            ));
        
        return MonitoringStats.builder()
                .totalProducts(totalProducts)
                .inStockCount(inStockCount)
                .outOfStockCount(outOfStockCount)
                .priorityDistribution(priorityStats)
                .build();
    }
    
    @Transactional
    public StockCheckHistory checkProductStock(MonitoredProduct product) {
        logger.debug("Checking stock for product: {} ({})", product.getProductName(), product.getUrl());
        
        StockCheckResult result = webScrapingService.checkStock(product.getUrl());
        
        StockCheckHistory history;
        
        if (result.hasError()) {
            history = new StockCheckHistory(product, result.getErrorMessage());
            product.setLastError(result.getErrorMessage());
        } else {
            history = new StockCheckHistory(product, result.getInStock());
            product.setLastError(null);
            
            // Check if stock status changed
            boolean stockChanged = !Boolean.valueOf(result.getInStock()).equals(product.getLastKnownStock());
            history.setStockChanged(stockChanged);
            
            if (stockChanged) {
                logger.info("Stock status changed for {}: {} -> {}", 
                    product.getProductName(), 
                    product.getLastKnownStock() ? "IN STOCK" : "OUT OF STOCK",
                    result.getInStock() ? "IN STOCK" : "OUT OF STOCK");
                
                // Send notification if product came back in stock
                if (result.getInStock()) {
                    notificationService.sendStockAlert(product);
                }
            }
            
            product.setLastKnownStock(result.getInStock());
        }
        
        history.setResponseTime(result.getResponseTime());
        product.setLastCheckedAt(LocalDateTime.now());
        
        // Save to database using MyBatis Plus
        productRepository.updateById(product);
        historyRepository.insert(history);
        
        return history;
    }
    
    /**
     * Test stock check without saving to database
     * Used for Discord /monitor-test command
     */
    public StockCheckHistory testProductStock(String url) {
        logger.debug("Testing stock for URL: {}", url);
        
        StockCheckResult result = webScrapingService.checkStock(url);
        
        // Create a temporary history object without product_id
        StockCheckHistory history = new StockCheckHistory();
        history.setInStock(result.getInStock());
        history.setResponseTime(result.getResponseTime());
        history.setCheckedAt(LocalDateTime.now());
        
        if (result.hasError()) {
            history.setErrorMessage(result.getErrorMessage());
        }
        
        return history;
    }
    
    @Transactional
    public MonitoredProduct addProduct(String url, String productName, String userId) {
        // Check if product already exists
        Optional<MonitoredProduct> existing = productRepository.findByUrl(url);
        if (existing.isPresent()) {
            throw new IllegalArgumentException("Product with this URL is already being monitored");
        }
        
        // Validate URL
        if (!isValidPopMartUrl(url)) {
            throw new IllegalArgumentException("Invalid Pop Mart URL. Must be from popmart.com/us/products/");
        }
        
        // 从URL中提取Product ID
        String extractedProductId = extractProductId(url);
        if (extractedProductId == null) {
            throw new IllegalArgumentException("无法从URL中提取商品ID，请检查URL格式是否正确");
        }
        
        // 如果没有提供商品名称，尝试从URL中提取
        String finalProductName = productName;
        if (finalProductName == null || finalProductName.trim().isEmpty()) {
            finalProductName = extractProductNameFromUrl(url);
            if (finalProductName == null || finalProductName.trim().isEmpty()) {
                finalProductName = "Pop Mart 商品 ID: " + extractedProductId;
            }
        }
        
        MonitoredProduct product = new MonitoredProduct(url, finalProductName, userId);
        // 设置从URL中提取的Product ID
        product.setProductId(extractedProductId);
        
        productRepository.insert(product);
        
        logger.info("Added new product to monitor: {} (ID: {}) by user {}", 
                   finalProductName, extractedProductId, userId);
        
        // Perform initial stock check
        try {
            checkProductStock(product);
        } catch (Exception e) {
            logger.error("Error performing initial stock check for new product", e);
        }
        
        return product;
    }
    
    @Transactional
    public void removeProduct(Long productId, String userId) {
        Optional<MonitoredProduct> productOpt = productRepository.findByProductId(productId);

        if (!productOpt.isPresent()) {
            throw new IllegalArgumentException("Product not found");
        }

        MonitoredProduct product = productOpt.get();
        
        if (!product.getAddedByUserId().equals(userId)) {
            throw new IllegalArgumentException("You can only remove products you added");
        }
        
        product.setIsActive(false);
        productRepository.updateById(product);
        
        logger.info("Deactivated product: {} by user {}", product.getProductName(), userId);
    }
    
    @Transactional
    public void removeProductByUrl(String url, String userId) {
        Optional<MonitoredProduct> productOpt = productRepository.findByUrl(url);
        if (!productOpt.isPresent()) {
            throw new IllegalArgumentException("Product not found");
        }
        
        MonitoredProduct product = productOpt.get();
        if (!product.getAddedByUserId().equals(userId)) {
            throw new IllegalArgumentException("You can only remove products you added");
        }
        
        product.setIsActive(false);
        productRepository.updateById(product);
        
        logger.info("Deactivated product: {} by user {}", product.getProductName(), userId);
    }
    
    public List<MonitoredProduct> getUserProducts(String userId) {
        return productRepository.findByAddedByUserIdAndIsActiveTrue(userId);
    }
    
    public List<MonitoredProduct> getAllActiveProducts() {
        return productRepository.findByIsActiveTrue();
    }
    
    /**
     * Check stock for a specific product by ID
     * Used for Discord bot manual check functionality
     */
    @Transactional
    public StockCheckHistory checkProductById(Long productId, String userId) {
        Optional<MonitoredProduct> productOpt = productRepository.findByProductId(productId);
        if (!productOpt.isPresent()) {
            throw new IllegalArgumentException("Product not found");
        }

        MonitoredProduct product = productOpt.get();

        if (!product.getAddedByUserId().equals(userId)) {
            throw new IllegalArgumentException("You can only check products you added");
        }
        
        if (!product.getIsActive()) {
            throw new IllegalArgumentException("Product is not active");
        }
        
        return checkProductStock(product);
    }
} 
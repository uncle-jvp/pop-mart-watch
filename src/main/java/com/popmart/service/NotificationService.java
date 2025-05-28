package com.popmart.service;

import com.popmart.config.PopMartConfig;
import com.popmart.entity.MonitoredProduct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;
import java.util.List;
import java.util.Arrays;

@Service
public class NotificationService {
    
    private static final Logger logger = LoggerFactory.getLogger(NotificationService.class);
    
    @Autowired
    private PopMartConfig config;
    
    private final RestTemplate restTemplate = new RestTemplate();
    
    public void sendStockAlert(MonitoredProduct product) {
        String notificationType = config.getMonitor().getNotification().getType();
        logger.info("Sending stock alert for product: {} via {}", product.getProductName(), notificationType);
        
        switch (notificationType.toLowerCase()) {
            case "log":
                sendLogNotification(product);
                break;
            case "discord":
                sendDiscordNotification(product);
                break;
            default:
                logger.warn("Unknown notification type: {}", notificationType);
                sendLogNotification(product);
        }
    }
    
    private void sendLogNotification(MonitoredProduct product) {
        logger.info("🎉 STOCK ALERT: {} is now IN STOCK! 🎉", product.getProductName());
        logger.info("Product ID: {}", product.getProductId() != null ? product.getProductId() : "未知");
        logger.info("Product URL: {}", product.getUrl());
        logger.info("Added by user: {}", product.getAddedByUserId());
    }
    
    private void sendDiscordNotification(MonitoredProduct product) {
        String webhookUrl = config.getMonitor().getNotification().getDiscord().getWebhookUrl();
        logger.info("Discord URL: {}", webhookUrl);
        
        if (webhookUrl == null || webhookUrl.trim().isEmpty()) {
            logger.warn("Discord webhook URL not configured, falling back to log notification");
            sendLogNotification(product);
            return;
        }
        
        try {
            // 直接使用实体中存储的Product ID
            String productId = product.getProductId();
            
            // 构建 Discord Embed 消息
            Map<String, Object> embed = new HashMap<>();
            embed.put("title", "🎉 Pop Mart 库存提醒");
            embed.put("description", "您监控的商品现在有货了！");
            embed.put("color", 0x00FF00); // 绿色
            embed.put("timestamp", java.time.Instant.now().toString());
            
            // 添加字段
            Map<String, Object> productField = new HashMap<>();
            productField.put("name", "📦 商品名称");
            productField.put("value", product.getProductName());
            productField.put("inline", false);
            
            // 添加Product ID字段
            Map<String, Object> productIdField = new HashMap<>();
            productIdField.put("name", "🆔 商品 ID");
            productIdField.put("value", productId != null ? "`" + productId + "`" : "未知");
            productIdField.put("inline", true);
            
            Map<String, Object> urlField = new HashMap<>();
            urlField.put("name", "🔗 商品链接");
            urlField.put("value", "[点击查看商品](" + product.getUrl() + ")");
            urlField.put("inline", false);
            
            Map<String, Object> statusField = new HashMap<>();
            statusField.put("name", "📊 库存状态");
            statusField.put("value", "🟢 现货");
            statusField.put("inline", true);
            
            Map<String, Object> timeField = new HashMap<>();
            timeField.put("name", "⏰ 检测时间");
            String timeStr = product.getLastCheckedAt() != null ? 
                product.getLastCheckedAt().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")) : 
                "刚刚";
            timeField.put("value", timeStr);
            timeField.put("inline", true);
            
            // 添加用户信息字段
            Map<String, Object> userField = new HashMap<>();
            userField.put("name", "👤 监控用户");
            userField.put("value", product.getAddedByUserId());
            userField.put("inline", true);
            
            embed.put("fields", Arrays.asList(productField, productIdField, urlField, statusField, timeField, userField));
            
            // 添加缩略图
            Map<String, Object> thumbnail = new HashMap<>();
            thumbnail.put("url", "https://cdn.popmart.com/website/images/logo.png");
            embed.put("thumbnail", thumbnail);
            
            // 添加页脚
            Map<String, Object> footer = new HashMap<>();
            footer.put("text", "Pop Mart 监控系统" + (productId != null ? " | ID: " + productId : ""));
            footer.put("icon_url", "https://cdn.popmart.com/website/images/favicon.ico");
            embed.put("footer", footer);
            
            // 构建完整的 Discord 消息
            Map<String, Object> discordMessage = new HashMap<>();
            discordMessage.put("content", "📢 **库存提醒** 📢" + (productId != null ? " (ID: " + productId + ")" : ""));
            discordMessage.put("embeds", Arrays.asList(embed));
            
            // 设置请求头
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            
            // 创建请求实体
            HttpEntity<Map<String, Object>> request = new HttpEntity<>(discordMessage, headers);
            
            // 发送请求
            ResponseEntity<String> response = restTemplate.exchange(
                webhookUrl, 
                HttpMethod.POST, 
                request, 
                String.class
            );
            
            if (response.getStatusCode().is2xxSuccessful()) {
                logger.info("✅ Discord notification sent successfully for product: {} (ID: {})", 
                    product.getProductName(), productId != null ? productId : "未知");
            } else {
                logger.error("❌ Failed to send Discord notification. Status: {}, Response: {}", 
                    response.getStatusCode(), response.getBody());
                // 发送失败时回退到日志通知
                sendLogNotification(product);
            }
            
        } catch (Exception e) {
            logger.error("❌ Error sending Discord notification for product: {}", product.getProductName(), e);
            // 发送失败时回退到日志通知
            sendLogNotification(product);
        }
    }
} 
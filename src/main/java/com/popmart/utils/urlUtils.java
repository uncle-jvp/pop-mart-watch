package com.popmart.utils;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class urlUtils {
    
    // Pop Mart URL 格式：https://www.popmart.com/us/products/{product_id}/product-name
    private static final Pattern PRODUCT_ID_PATTERN = Pattern.compile("https://www\\.popmart\\.com/us/products/(\\d+)/.*");
    
    public static boolean isValidPopMartUrl(String url) {
        return url != null &&
                url.startsWith("https://www.popmart.com/us/products/") &&
                url.length() > "https://www.popmart.com/us/products/".length();
    }
    
    /**
     * 从 Pop Mart URL 中提取商品 ID
     * 
     * @param url Pop Mart 商品 URL
     * @return 商品 ID，如果无法提取则返回 null
     */
    public static String extractProductId(String url) {
        if (url == null || url.trim().isEmpty()) {
            return null;
        }
        
        try {
            Matcher matcher = PRODUCT_ID_PATTERN.matcher(url.trim());
            if (matcher.matches()) {
                return matcher.group(1);
            }
        } catch (Exception e) {
            // 如果正则匹配失败，尝试简单的字符串解析
            try {
                String[] parts = url.split("/");
                for (int i = 0; i < parts.length; i++) {
                    if ("products".equals(parts[i]) && i + 1 < parts.length) {
                        String potentialId = parts[i + 1];
                        // 验证是否为数字
                        if (potentialId.matches("\\d+")) {
                            return potentialId;
                        }
                    }
                }
            } catch (Exception ex) {
                // 忽略解析错误
            }
        }
        
        return null;
    }
    
    /**
     * 从 Pop Mart URL 中提取商品名称（URL 中的最后一部分）
     * 
     * @param url Pop Mart 商品 URL
     * @return 商品名称，如果无法提取则返回 null
     */
    public static String extractProductNameFromUrl(String url) {
        if (url == null || url.trim().isEmpty()) {
            return null;
        }
        
        try {
            // 移除查询参数和锚点
            String cleanUrl = url.split("\\?")[0].split("#")[0];
            
            // 移除末尾的斜杠
            if (cleanUrl.endsWith("/")) {
                cleanUrl = cleanUrl.substring(0, cleanUrl.length() - 1);
            }
            
            // 获取最后一部分作为商品名称
            String[] parts = cleanUrl.split("/");
            if (parts.length > 0) {
                String productName = parts[parts.length - 1];
                // 将连字符替换为空格，并进行基本的格式化
                return productName.replace("-", " ").trim();
            }
        } catch (Exception e) {
            // 忽略解析错误
        }
        
        return null;
    }
    
    /**
     * 验证提取的商品 ID 是否有效
     * 
     * @param productId 商品 ID
     * @return 是否为有效的商品 ID
     */
    public static boolean isValidProductId(String productId) {
        return productId != null && 
               productId.matches("\\d+") && 
               productId.length() >= 1 && 
               productId.length() <= 10; // 假设商品ID不会超过10位数
    }
}

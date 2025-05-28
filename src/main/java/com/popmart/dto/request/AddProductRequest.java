package com.popmart.dto.request;

import lombok.Data;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Pattern;

/**
 * 添加商品监控请求
 */
@Data
public class AddProductRequest {
    
    /**
     * 商品 URL
     */
    @NotBlank(message = "商品 URL 不能为空")
    @Pattern(regexp = "^https://www\\.popmart\\.com/us/products/.*", 
             message = "URL 必须是 Pop Mart US 官网商品链接")
    private String url;
    
    /**
     * 商品名称
     */
    private String productName;
    
    /**
     * 用户 ID
     */
    @NotBlank(message = "用户 ID 不能为空")
    private String userId;
} 
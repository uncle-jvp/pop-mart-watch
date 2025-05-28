package com.popmart.dto.request;

import lombok.Data;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Pattern;

/**
 * 测试商品库存请求
 */
@Data
public class TestProductRequest {
    
    /**
     * 商品 URL
     */
    @NotBlank(message = "商品 URL 不能为空")
    @Pattern(regexp = "^https://www\\.popmart\\.com/us/products/.*", 
             message = "URL 必须是 Pop Mart US 官网商品链接")
    private String url;
} 
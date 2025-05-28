package com.popmart.utils;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

public class UrlUtilsTest {

    @Test
    public void testExtractProductId() {
        // 测试正常的Pop Mart URL
        String url1 = "https://www.popmart.com/us/products/1739/THE-MONSTERS-Classic-Series-Sparkly-Plush-Pendant-Blind-Box";
        assertEquals("1739", urlUtils.extractProductId(url1));
        
        // 测试带查询参数的URL
        String url2 = "https://www.popmart.com/us/products/2468/product-name?ref=homepage";
        assertEquals("2468", urlUtils.extractProductId(url2));
        
        // 测试带锚点的URL
        String url3 = "https://www.popmart.com/us/products/3579/product-name#details";
        assertEquals("3579", urlUtils.extractProductId(url3));
        
        // 测试末尾有斜杠的URL
        String url4 = "https://www.popmart.com/us/products/4680/product-name/";
        assertEquals("4680", urlUtils.extractProductId(url4));
        
        // 测试无效URL
        String invalidUrl1 = "https://www.popmart.com/us/products/";
        assertNull(urlUtils.extractProductId(invalidUrl1));
        
        String invalidUrl2 = "https://www.popmart.com/us/products/abc/product-name";
        assertNull(urlUtils.extractProductId(invalidUrl2));
        
        String invalidUrl3 = "https://www.example.com/products/1234/product";
        assertNull(urlUtils.extractProductId(invalidUrl3));
        
        // 测试null和空字符串
        assertNull(urlUtils.extractProductId(null));
        assertNull(urlUtils.extractProductId(""));
        assertNull(urlUtils.extractProductId("   "));
    }
    
    @Test
    public void testExtractProductNameFromUrl() {
        // 测试正常的Pop Mart URL
        String url1 = "https://www.popmart.com/us/products/1739/THE-MONSTERS-Classic-Series-Sparkly-Plush-Pendant-Blind-Box";
        assertEquals("THE MONSTERS Classic Series Sparkly Plush Pendant Blind Box", 
                     urlUtils.extractProductNameFromUrl(url1));
        
        // 测试带查询参数的URL
        String url2 = "https://www.popmart.com/us/products/2468/Molly-Space-Series?ref=homepage";
        assertEquals("Molly Space Series", urlUtils.extractProductNameFromUrl(url2));
        
        // 测试末尾有斜杠的URL
        String url3 = "https://www.popmart.com/us/products/3579/Dimoo-World-Tour/";
        assertEquals("Dimoo World Tour", urlUtils.extractProductNameFromUrl(url3));
        
        // 测试无效URL
        assertNull(urlUtils.extractProductNameFromUrl(null));
        assertNull(urlUtils.extractProductNameFromUrl(""));
    }
    
    @Test
    public void testIsValidProductId() {
        // 测试有效的Product ID
        assertTrue(urlUtils.isValidProductId("1739"));
        assertTrue(urlUtils.isValidProductId("123"));
        assertTrue(urlUtils.isValidProductId("0")); // 0也是有效的数字ID
        assertTrue(urlUtils.isValidProductId("9999999999")); // 10位数
        
        // 测试无效的Product ID
        assertFalse(urlUtils.isValidProductId(null));
        assertFalse(urlUtils.isValidProductId(""));
        assertFalse(urlUtils.isValidProductId("abc"));
        assertFalse(urlUtils.isValidProductId("123abc"));
        assertFalse(urlUtils.isValidProductId("12345678901")); // 超过10位数
    }
    
    @Test
    public void testIsValidPopMartUrl() {
        // 测试有效的Pop Mart URL
        assertTrue(urlUtils.isValidPopMartUrl("https://www.popmart.com/us/products/1739/product-name"));
        assertTrue(urlUtils.isValidPopMartUrl("https://www.popmart.com/us/products/123/test"));
        
        // 测试无效的URL
        assertFalse(urlUtils.isValidPopMartUrl(null));
        assertFalse(urlUtils.isValidPopMartUrl(""));
        assertFalse(urlUtils.isValidPopMartUrl("https://www.popmart.com/us/products/"));
        assertFalse(urlUtils.isValidPopMartUrl("https://www.example.com/products/123/test"));
        assertFalse(urlUtils.isValidPopMartUrl("http://www.popmart.com/us/products/123/test")); // http instead of https
    }
} 
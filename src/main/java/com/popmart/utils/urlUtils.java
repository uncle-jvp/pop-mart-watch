package com.popmart.utils;

public class urlUtils {
    public static boolean isValidPopMartUrl(String url) {
        return url != null &&
                url.startsWith("https://www.popmart.com/us/products/") &&
                url.length() > "https://www.popmart.com/us/products/".length();
    }
}

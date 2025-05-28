package com.popmart.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "popmart")
public class PopMartConfig {
    
    private Monitor monitor = new Monitor();
    private Discord discord = new Discord();
    
    public Monitor getMonitor() {
        return monitor;
    }
    
    public void setMonitor(Monitor monitor) {
        this.monitor = monitor;
    }
    
    public Discord getDiscord() {
        return discord;
    }
    
    public void setDiscord(Discord discord) {
        this.discord = discord;
    }
    
    public static class Monitor {
        private int pollInterval = 5;
        private StockDetection stockDetection = new StockDetection();
        private Selenium selenium = new Selenium();
        private Notification notification = new Notification();
        
        public int getPollInterval() {
            return pollInterval;
        }
        
        public void setPollInterval(int pollInterval) {
            this.pollInterval = pollInterval;
        }
        
        public StockDetection getStockDetection() {
            return stockDetection;
        }
        
        public void setStockDetection(StockDetection stockDetection) {
            this.stockDetection = stockDetection;
        }
        
        public Selenium getSelenium() {
            return selenium;
        }
        
        public void setSelenium(Selenium selenium) {
            this.selenium = selenium;
        }
        
        public Notification getNotification() {
            return notification;
        }
        
        public void setNotification(Notification notification) {
            this.notification = notification;
        }
    }
    
    public static class StockDetection {
        private String selector = "button:contains('Add to Bag')";
        private String keyword = "Add to Bag";
        private int timeout = 30;
        
        public String getSelector() {
            return selector;
        }
        
        public void setSelector(String selector) {
            this.selector = selector;
        }
        
        public String getKeyword() {
            return keyword;
        }
        
        public void setKeyword(String keyword) {
            this.keyword = keyword;
        }
        
        public int getTimeout() {
            return timeout;
        }
        
        public void setTimeout(int timeout) {
            this.timeout = timeout;
        }
    }
    
    public static class Selenium {
        private boolean headless = true;
        private int timeout = 30;
        private String userAgent = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36";
        private Performance performance = new Performance();
        
        public boolean isHeadless() {
            return headless;
        }
        
        public void setHeadless(boolean headless) {
            this.headless = headless;
        }
        
        public int getTimeout() {
            return timeout;
        }
        
        public void setTimeout(int timeout) {
            this.timeout = timeout;
        }
        
        public String getUserAgent() {
            return userAgent;
        }
        
        public void setUserAgent(String userAgent) {
            this.userAgent = userAgent;
        }
        
        public Performance getPerformance() {
            return performance;
        }
        
        public void setPerformance(Performance performance) {
            this.performance = performance;
        }
    }
    
    public static class Performance {
        private int pageLoadTimeout = 10;
        private int scriptTimeout = 5;
        private int implicitWait = 3;
        private int smartWaitTimeout = 5;
        private boolean disableImages = true;
        private boolean eagerLoading = true;
        private long cacheDuration = 30000;
        private int httpCheckTimeout = 3000;
        
        public int getPageLoadTimeout() {
            return pageLoadTimeout;
        }
        
        public void setPageLoadTimeout(int pageLoadTimeout) {
            this.pageLoadTimeout = pageLoadTimeout;
        }
        
        public int getScriptTimeout() {
            return scriptTimeout;
        }
        
        public void setScriptTimeout(int scriptTimeout) {
            this.scriptTimeout = scriptTimeout;
        }
        
        public int getImplicitWait() {
            return implicitWait;
        }
        
        public void setImplicitWait(int implicitWait) {
            this.implicitWait = implicitWait;
        }
        
        public int getSmartWaitTimeout() {
            return smartWaitTimeout;
        }
        
        public void setSmartWaitTimeout(int smartWaitTimeout) {
            this.smartWaitTimeout = smartWaitTimeout;
        }
        
        public boolean isDisableImages() {
            return disableImages;
        }
        
        public void setDisableImages(boolean disableImages) {
            this.disableImages = disableImages;
        }
        
        public boolean isEagerLoading() {
            return eagerLoading;
        }
        
        public void setEagerLoading(boolean eagerLoading) {
            this.eagerLoading = eagerLoading;
        }
        
        public long getCacheDuration() {
            return cacheDuration;
        }
        
        public void setCacheDuration(long cacheDuration) {
            this.cacheDuration = cacheDuration;
        }
        
        public int getHttpCheckTimeout() {
            return httpCheckTimeout;
        }
        
        public void setHttpCheckTimeout(int httpCheckTimeout) {
            this.httpCheckTimeout = httpCheckTimeout;
        }
    }
    
    public static class Notification {
        private String type = "log";
        private DiscordNotification discord = new DiscordNotification();
        
        public String getType() {
            return type;
        }
        
        public void setType(String type) {
            this.type = type;
        }
        
        public DiscordNotification getDiscord() {
            return discord;
        }
        
        public void setDiscord(DiscordNotification discord) {
            this.discord = discord;
        }
    }
    
    public static class DiscordNotification {
        private String webhookUrl;
        
        public String getWebhookUrl() {
            return webhookUrl;
        }
        
        public void setWebhookUrl(String webhookUrl) {
            this.webhookUrl = webhookUrl;
        }
    }
    
    public static class Discord {
        private String botToken;
        private String guildId;
        
        public String getBotToken() {
            return botToken;
        }
        
        public void setBotToken(String botToken) {
            this.botToken = botToken;
        }
        
        public String getGuildId() {
            return guildId;
        }
        
        public void setGuildId(String guildId) {
            this.guildId = guildId;
        }
    }
} 
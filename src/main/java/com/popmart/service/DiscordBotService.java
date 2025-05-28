package com.popmart.service;

import com.popmart.config.PopMartConfig;
import com.popmart.dto.response.MonitoringStats;
import com.popmart.entity.MonitoredProduct;
import com.popmart.entity.StockCheckHistory;
import com.popmart.utils.urlUtils;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.JDABuilder;
import net.dv8tion.jda.api.entities.Activity;
import net.dv8tion.jda.api.entities.MessageEmbed;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.Commands;
import net.dv8tion.jda.api.interactions.components.buttons.Button;
import net.dv8tion.jda.api.requests.GatewayIntent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import java.awt.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.concurrent.CompletableFuture;

@Service
public class DiscordBotService extends ListenerAdapter {
    
    private static final Logger logger = LoggerFactory.getLogger(DiscordBotService.class);
    
    @Autowired
    private PopMartConfig config;
    
    @Autowired
    private MonitoringService monitoringService;
    
    private JDA jda;
    
    @PostConstruct  // 重新启用自动初始化
    public void initializeBot() {
        String botToken = config.getDiscord().getBotToken();
        String guildId = config.getDiscord().getGuildId();
        
        if (botToken == null || botToken.trim().isEmpty()) {
            logger.warn("Discord bot token not configured, skipping bot initialization");
            return;
        }
        
        try {
            jda = JDABuilder.createDefault(botToken)
                    .enableIntents(GatewayIntent.GUILD_MESSAGES, GatewayIntent.MESSAGE_CONTENT)
                    .addEventListeners(this)
                    .build();
            
            jda.awaitReady();
            
            // Register slash commands
            if (guildId != null && !guildId.trim().isEmpty()) {
                // Register commands for specific guild (immediate effect)
                logger.info("Registering commands for guild: {}", guildId);
                jda.getGuildById(guildId).updateCommands().addCommands(
                    Commands.slash("monitor-add", "Add a Pop Mart product to monitor")
                        .addOption(OptionType.STRING, "url", "Pop Mart product URL", true)
                        .addOption(OptionType.STRING, "name", "Product name (optional)", false),
                    
                    Commands.slash("monitor-remove", "Remove a product from monitoring")
                        .addOption(OptionType.STRING, "url", "Product URL to remove", true),
                    
                    Commands.slash("monitor-status", "View your monitored products"),
                    
                    Commands.slash("monitor-test", "Manually test a product URL")
                        .addOption(OptionType.STRING, "url", "Pop Mart product URL to test", true),
                    
                    Commands.slash("monitor-stats", "View monitoring statistics")
                ).queue(
                    success -> logger.info("Successfully registered {} slash commands for guild {}", success.size(), guildId),
                    error -> logger.error("Failed to register slash commands for guild {}: {}", guildId, error.getMessage())
                );
            } else {
                // Register global commands (takes up to 1 hour to sync)
                logger.warn("Guild ID not configured, registering global commands (may take up to 1 hour to sync)");
                jda.updateCommands().addCommands(
                    Commands.slash("monitor-add", "Add a Pop Mart product to monitor")
                        .addOption(OptionType.STRING, "url", "Pop Mart product URL", true)
                        .addOption(OptionType.STRING, "name", "Product name (optional)", false),
                    
                    Commands.slash("monitor-remove", "Remove a product from monitoring")
                        .addOption(OptionType.STRING, "url", "Product URL to remove", true),
                    
                    Commands.slash("monitor-status", "View your monitored products"),
                    
                    Commands.slash("monitor-test", "Manually test a product URL")
                        .addOption(OptionType.STRING, "url", "Pop Mart product URL to test", true),
                    
                    Commands.slash("monitor-stats", "View monitoring statistics")
                ).queue(
                    success -> logger.info("Successfully registered {} global slash commands", success.size()),
                    error -> logger.error("Failed to register global slash commands: {}", error.getMessage())
                );
            }
            
            logger.info("Discord bot initialized successfully");
            
        } catch (Exception e) {
            logger.error("Failed to initialize Discord bot", e);
        }
    }
    
    @PreDestroy
    public void shutdownBot() {
        if (jda != null) {
            jda.shutdown();
            logger.info("Discord bot shutdown");
        }
    }
    
    @Override
    public void onSlashCommandInteraction(SlashCommandInteractionEvent event) {
        String userId = event.getUser().getId();
        
        switch (event.getName()) {
            case "monitor-add":
                handleMonitorAdd(event, userId);
                break;
            case "monitor-remove":
                handleMonitorRemove(event, userId);
                break;
            case "monitor-status":
                handleMonitorStatus(event, userId);
                break;
            case "monitor-test":
                handleMonitorTest(event, userId);
                break;
            case "monitor-stats":
                handleMonitorStats(event);
                break;
            default:
                event.reply("Unknown command").setEphemeral(true).queue();
        }
    }
    
    @Override
    public void onButtonInteraction(ButtonInteractionEvent event) {
        String buttonId = event.getComponentId();
        String userId = event.getUser().getId();
        
        if (buttonId.startsWith("check_now_")) {
            String productIdStr = buttonId.substring("check_now_".length());
            try {
                String productId = productIdStr; // 直接使用字符串，不需要解析为Long
                handleCheckNow(event, productId, userId);
            } catch (Exception e) {
                event.reply("❌ Invalid product ID").setEphemeral(true).queue();
            }
        } else if (buttonId.startsWith("stop_monitoring_")) {
            String productIdStr = buttonId.substring("stop_monitoring_".length());
            try {
                String productId = productIdStr; // 直接使用字符串，不需要解析为Long
                handleStopMonitoring(event, productId, userId);
            } catch (Exception e) {
                event.reply("❌ Invalid product ID").setEphemeral(true).queue();
            }
        }
    }
    
    private void handleMonitorAdd(SlashCommandInteractionEvent event, String userId) {
        String url = event.getOption("url").getAsString();
        if (url == null || url.trim().isEmpty() || !urlUtils.isValidPopMartUrl(url)) {
            event.reply("❌ Please provide a valid Pop Mart product URL").setEphemeral(true).queue();
            return;
        }
        String urlProductName = url.substring(url.lastIndexOf("/") + 1);
        String name = event.getOption("name") != null ? event.getOption("name").getAsString() : urlProductName;

        // Immediately acknowledge the command
        event.deferReply().queue(success -> {
            // Send initial response after deferReply succeeds
            EmbedBuilder initialEmbed = new EmbedBuilder()
                .setTitle("⏳ Adding Product to Monitoring...")
                .setDescription("Please wait while we check the product and add it to your monitoring list.")
                .addField("Product Name", name, false)
                .addField("URL", url, false)
                .setColor(Color.YELLOW)
                .setTimestamp(java.time.Instant.now());
            
            event.getHook().editOriginalEmbeds(initialEmbed.build()).queue();
            
            // Process asynchronously
            CompletableFuture.supplyAsync(() -> {
                try {
                    return monitoringService.addProduct(url, name, userId);
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            }).thenAccept(product -> {
                EmbedBuilder embed = new EmbedBuilder()
                    .setTitle("✅ Product Added to Monitoring")
                    .setDescription("Successfully added product to monitoring list")
                    .addField("Product Name", product.getProductName(), false)
                    .addField("Product ID", product.getProductId(), true)
                    .addField("URL", product.getUrl(), false)
                    .addField("Status", product.getLastKnownStock() ? "🟢 In Stock" : "🔴 Out of Stock", false)
                    .setColor(Color.GREEN)
                    .setTimestamp(java.time.Instant.now());
                
                event.getHook().editOriginalEmbeds(embed.build())
                    .setActionRow(
                        Button.primary("check_now_" + product.getProductId(), "Check Now"),
                        Button.danger("stop_monitoring_" + product.getProductId(), "Stop Monitoring")
                    )
                    .queue();
            }).exceptionally(throwable -> {
                String errorMessage = "添加商品失败，请检查URL是否正确或稍后重试";
                if (throwable.getCause() != null && throwable.getCause().getMessage() != null) {
                    String causeMessage = throwable.getCause().getMessage();
                    if (causeMessage.contains("already being monitored")) {
                        errorMessage = "该商品已在监控列表中";
                    } else if (causeMessage.contains("Invalid Pop Mart URL")) {
                        errorMessage = "无效的 Pop Mart 商品链接，请确保链接格式正确";
                    } else if (causeMessage.contains("无法从URL中提取商品ID")) {
                        errorMessage = "无法识别商品ID，请检查链接是否为有效的 Pop Mart 商品页面";
                    }
                }
                event.getHook().editOriginal("❌ " + errorMessage).queue();
                return null;
            });
        }, failure -> {
            // If deferReply fails, use regular reply
            event.reply("❌ Failed to start adding product: " + failure.getMessage()).setEphemeral(true).queue();
        });
    }
    
    private void handleMonitorRemove(SlashCommandInteractionEvent event, String userId) {
        String url = event.getOption("url").getAsString();
        
        try {
            monitoringService.removeProductByUrl(url, userId);
            
            EmbedBuilder embed = new EmbedBuilder()
                .setTitle("✅ Product Removed")
                .setDescription("Successfully removed product from monitoring")
                .addField("URL", url, false)
                .setColor(Color.ORANGE)
                .setTimestamp(java.time.Instant.now());
            
            event.replyEmbeds(embed.build()).queue();
            
        } catch (Exception e) {
            String errorMessage = "移除商品失败";
            if (e.getMessage() != null) {
                if (e.getMessage().contains("Product not found")) {
                    errorMessage = "未找到该商品，可能已被移除或URL不正确";
                } else if (e.getMessage().contains("You can only remove products you added")) {
                    errorMessage = "您只能移除自己添加的商品";
                }
            }
            event.reply("❌ " + errorMessage).setEphemeral(true).queue();
        }
    }
    
    private void handleMonitorStatus(SlashCommandInteractionEvent event, String userId) {
        List<MonitoredProduct> products = monitoringService.getUserProducts(userId);
        
        if (products.isEmpty()) {
            event.reply("You are not monitoring any products yet. Use `/monitor-add` to start monitoring!").setEphemeral(true).queue();
            return;
        }
        
        EmbedBuilder embed = new EmbedBuilder()
            .setTitle("📊 Your Monitored Products")
            .setDescription("Here are all the products you're currently monitoring:")
            .setColor(Color.BLUE)
            .setTimestamp(java.time.Instant.now());
        
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("MM/dd HH:mm");
        
        for (MonitoredProduct product : products) {
            String status = product.getLastKnownStock() ? "🟢 In Stock" : "🔴 Out of Stock";
            String lastChecked = product.getLastCheckedAt() != null ? 
                product.getLastCheckedAt().format(formatter) : "Never";
            
            embed.addField(
                product.getProductName(),
                String.format("Product ID: %s\nStatus: %s\nLast Checked: %s\n[View Product](%s)", 
                    product.getProductId(), status, lastChecked, product.getUrl()),
                false
            );
        }
        
        event.replyEmbeds(embed.build()).queue();
    }
    
    private void handleMonitorTest(SlashCommandInteractionEvent event, String userId) {
        String url = event.getOption("url").getAsString();
        
        // Immediately acknowledge the command
        event.deferReply().queue(success -> {
            // Send initial response after deferReply succeeds
            EmbedBuilder initialEmbed = new EmbedBuilder()
                .setTitle("⏳ Testing Product Stock...")
                .setDescription("Please wait while we check the product stock. This may take 10-30 seconds.")
                .addField("URL", url, false)
                .setColor(Color.YELLOW)
                .setTimestamp(java.time.Instant.now());
            
            event.getHook().editOriginalEmbeds(initialEmbed.build()).queue();
            
            // Process asynchronously
            CompletableFuture.supplyAsync(() -> {
                try {
                    return monitoringService.testProductStock(url);
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            }).thenAccept(result -> {
                EmbedBuilder embed = new EmbedBuilder()
                    .setTitle("🧪 Stock Check Test Results")
                    .addField("URL", url, false)
                    .addField("Status", result.getInStock() ? "🟢 In Stock" : "🔴 Out of Stock", false)
                    .addField("Response Time", result.getResponseTime() + "ms", false)
                    .setColor(result.getInStock() ? Color.GREEN : Color.RED)
                    .setTimestamp(java.time.Instant.now());
                
                if (result.getErrorMessage() != null) {
                    embed.addField("Error", result.getErrorMessage(), false);
                }
                
                event.getHook().editOriginalEmbeds(embed.build()).queue();
            }).exceptionally(throwable -> {
                String errorMessage = "测试失败，请检查URL是否正确或稍后重试";
                if (throwable.getCause() != null && throwable.getCause().getMessage() != null) {
                    String causeMessage = throwable.getCause().getMessage();
                    if (causeMessage.contains("Invalid Pop Mart URL")) {
                        errorMessage = "无效的 Pop Mart 商品链接";
                    } else if (causeMessage.contains("timeout") || causeMessage.contains("TimeoutException")) {
                        errorMessage = "请求超时，请稍后重试";
                    } else if (causeMessage.contains("WebDriverException")) {
                        errorMessage = "网页加载失败，请稍后重试";
                    }
                }
                event.getHook().editOriginal("❌ " + errorMessage).queue();
                return null;
            });
        }, failure -> {
            // If deferReply fails, use regular reply
            event.reply("❌ Failed to start stock check: " + failure.getMessage()).setEphemeral(true).queue();
        });
    }
    
    private void handleMonitorStats(SlashCommandInteractionEvent event) {
        MonitoringStats stats = monitoringService.getMonitoringStats();
        
        EmbedBuilder embed = new EmbedBuilder()
            .setTitle("📈 Monitoring Statistics")
            .addField("Total Products", String.valueOf(stats.getTotalProducts()), true)
            .addField("In Stock", String.valueOf(stats.getInStockCount()), true)
            .addField("Out of Stock", String.valueOf(stats.getOutOfStockCount()), true)
            .setColor(Color.CYAN)
            .setTimestamp(java.time.Instant.now());
        
        // 添加优先级分布信息
        if (!stats.getPriorityDistribution().isEmpty()) {
            StringBuilder priorityInfo = new StringBuilder();
            stats.getPriorityDistribution().forEach((priority, count) -> {
                priorityInfo.append(String.format("%s: %d\n", priority.name(), count));
            });
            embed.addField("Priority Distribution", priorityInfo.toString(), false);
        }
        
        event.replyEmbeds(embed.build()).queue();
    }
    
    private void handleCheckNow(ButtonInteractionEvent event, String productId, String userId) {
        event.deferReply(true).queue(success -> {
            // Send initial response after deferReply succeeds
            event.getHook().editOriginal("⏳ Checking product stock... Please wait.").queue();
            
            // Process asynchronously
            CompletableFuture.supplyAsync(() -> {
                try {
                    return monitoringService.checkProductById(productId, userId);
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            }).thenAccept(result -> {
                if (result != null) {
                    String status = result.getInStock() ? "🟢 In Stock" : "🔴 Out of Stock";
                    event.getHook().editOriginal(
                        String.format("✅ Check completed!\nStatus: %s\nResponse Time: %dms", 
                            status, result.getResponseTime())
                    ).queue();
                } else {
                    event.getHook().editOriginal("❌ Product not found or access denied").queue();
                }
            }).exceptionally(throwable -> {
                String errorMessage = "检查失败，请稍后重试";
                if (throwable.getCause() != null && throwable.getCause().getMessage() != null) {
                    String causeMessage = throwable.getCause().getMessage();
                    if (causeMessage.contains("Product not found")) {
                        errorMessage = "商品未找到";
                    } else if (causeMessage.contains("You can only check products you added")) {
                        errorMessage = "您只能检查自己添加的商品";
                    } else if (causeMessage.contains("Product is not active")) {
                        errorMessage = "商品监控已停用";
                    } else if (causeMessage.contains("timeout") || causeMessage.contains("TimeoutException")) {
                        errorMessage = "请求超时，请稍后重试";
                    }
                }
                event.getHook().editOriginal("❌ " + errorMessage).queue();
                return null;
            });
        }, failure -> {
            // If deferReply fails, use regular reply
            event.reply("❌ Failed to start check: " + failure.getMessage()).setEphemeral(true).queue();
        });
    }
    
    private void handleStopMonitoring(ButtonInteractionEvent event, String productId, String userId) {
        try {
            monitoringService.removeProduct(productId, userId);
            event.reply("✅ Stopped monitoring this product").setEphemeral(true).queue();
        } catch (Exception e) {
            String errorMessage = "停止监控失败";
            if (e.getMessage() != null) {
                if (e.getMessage().contains("Product not found")) {
                    errorMessage = "商品未找到";
                } else if (e.getMessage().contains("You can only remove products you added")) {
                    errorMessage = "您只能停止监控自己添加的商品";
                }
            }
            event.reply("❌ " + errorMessage).setEphemeral(true).queue();
        }
    }
} 
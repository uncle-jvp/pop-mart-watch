#!/bin/bash

# Discord 通知配置脚本
# 帮助用户快速配置 Discord Webhook 通知

set -e

echo "🔧 Pop Mart 监控系统 - Discord 通知配置"
echo "========================================"

# 颜色定义
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

# 检查当前目录
if [ ! -f "pom.xml" ]; then
    echo -e "${RED}❌ 请在项目根目录运行此脚本${NC}"
    exit 1
fi

echo -e "${BLUE}📋 配置 Discord Webhook 通知${NC}"
echo ""

# 获取用户输入
read -p "请输入 Discord Webhook URL: " webhook_url
if [ -z "$webhook_url" ]; then
    echo -e "${RED}❌ Webhook URL 不能为空${NC}"
    exit 1
fi

# 验证 URL 格式
if [[ ! $webhook_url =~ ^https://discord\.com/api/webhooks/ ]]; then
    echo -e "${YELLOW}⚠️  警告: URL 格式可能不正确${NC}"
    echo "正确格式应为: https://discord.com/api/webhooks/..."
    read -p "是否继续? (y/N): " continue_setup
    if [[ ! $continue_setup =~ ^[Yy]$ ]]; then
        echo "配置已取消"
        exit 0
    fi
fi

# 选择配置方式
echo ""
echo "请选择配置方式:"
echo "1) 修改 application.yml (全局配置)"
echo "2) 创建 application-local.yml (本地环境)"
echo "3) 创建 application-production.yml (生产环境)"
echo "4) 仅设置环境变量"
read -p "请选择 (1-4): " config_choice

case $config_choice in
    1)
        # 修改 application.yml
        config_file="src/main/resources/application.yml"
        echo -e "${BLUE}📝 修改 $config_file${NC}"
        
        # 备份原文件
        cp "$config_file" "$config_file.backup.$(date +%Y%m%d_%H%M%S)"
        
        # 修改配置
        sed -i.tmp "s|type: log|type: discord|g" "$config_file"
        sed -i.tmp "s|webhook-url: \${DISCORD_WEBHOOK_URL:}|webhook-url: \"$webhook_url\"|g" "$config_file"
        rm "$config_file.tmp"
        
        echo -e "${GREEN}✅ 已修改 $config_file${NC}"
        ;;
        
    2)
        # 创建本地配置
        config_file="src/main/resources/application-local.yml"
        echo -e "${BLUE}📝 创建 $config_file${NC}"
        
        if [ -f "$config_file" ]; then
            cp "$config_file" "$config_file.backup.$(date +%Y%m%d_%H%M%S)"
        fi
        
        # 从示例文件复制
        if [ -f "src/main/resources/application-local.yml.example" ]; then
            cp "src/main/resources/application-local.yml.example" "$config_file"
        else
            echo -e "${YELLOW}⚠️  示例文件不存在，创建基础配置${NC}"
            cat > "$config_file" << EOF
popmart:
  monitor:
    notification:
      type: discord
      discord:
        webhook-url: "$webhook_url"
EOF
        fi
        
        # 修改配置
        sed -i.tmp "s|type: log|type: discord|g" "$config_file"
        sed -i.tmp "s|webhook-url: \"https://discord.com/api/webhooks/YOUR_TEST_WEBHOOK_URL\"|webhook-url: \"$webhook_url\"|g" "$config_file"
        rm "$config_file.tmp" 2>/dev/null || true
        
        echo -e "${GREEN}✅ 已创建 $config_file${NC}"
        echo -e "${YELLOW}💡 使用方法: java -jar app.jar --spring.profiles.active=local${NC}"
        ;;
        
    3)
        # 创建生产配置
        config_file="src/main/resources/application-production.yml"
        echo -e "${BLUE}📝 创建 $config_file${NC}"
        
        if [ -f "$config_file" ]; then
            cp "$config_file" "$config_file.backup.$(date +%Y%m%d_%H%M%S)"
        fi
        
        # 从示例文件复制
        if [ -f "src/main/resources/application-production.yml.example" ]; then
            cp "src/main/resources/application-production.yml.example" "$config_file"
        else
            echo -e "${YELLOW}⚠️  示例文件不存在，创建基础配置${NC}"
            cat > "$config_file" << EOF
popmart:
  monitor:
    notification:
      type: discord
      discord:
        webhook-url: "$webhook_url"
EOF
        fi
        
        # 修改配置
        sed -i.tmp "s|webhook-url: \"https://discord.com/api/webhooks/YOUR_PRODUCTION_WEBHOOK_URL\"|webhook-url: \"$webhook_url\"|g" "$config_file"
        rm "$config_file.tmp" 2>/dev/null || true
        
        echo -e "${GREEN}✅ 已创建 $config_file${NC}"
        echo -e "${YELLOW}💡 使用方法: java -jar app.jar --spring.profiles.active=production${NC}"
        ;;
        
    4)
        # 设置环境变量
        echo -e "${BLUE}🔧 设置环境变量${NC}"
        echo ""
        echo "请将以下命令添加到您的 shell 配置文件 (~/.bashrc, ~/.zshrc 等):"
        echo ""
        echo -e "${GREEN}export DISCORD_WEBHOOK_URL=\"$webhook_url\"${NC}"
        echo ""
        echo "或者临时设置:"
        echo -e "${GREEN}export DISCORD_WEBHOOK_URL=\"$webhook_url\"${NC}"
        echo ""
        
        # 临时设置环境变量
        export DISCORD_WEBHOOK_URL="$webhook_url"
        echo -e "${GREEN}✅ 已在当前会话中设置环境变量${NC}"
        ;;
        
    *)
        echo -e "${RED}❌ 无效选择${NC}"
        exit 1
        ;;
esac

echo ""
echo -e "${BLUE}🧪 测试 Discord 通知${NC}"
echo "配置完成后，您可以使用以下命令测试通知:"
echo ""
echo -e "${GREEN}# 启动应用${NC}"
case $config_choice in
    2)
        echo "mvn spring-boot:run -Dspring-boot.run.profiles=local"
        ;;
    3)
        echo "mvn spring-boot:run -Dspring-boot.run.profiles=production"
        ;;
    *)
        echo "mvn spring-boot:run"
        ;;
esac

echo ""
echo -e "${GREEN}# 测试通知${NC}"
echo "curl -X POST http://localhost:8080/api/monitor/test-discord"

echo ""
echo -e "${BLUE}📚 更多信息${NC}"
echo "详细配置指南: docs/DISCORD_NOTIFICATION_GUIDE.md"
echo ""
echo -e "${GREEN}🎉 Discord 通知配置完成！${NC}" 
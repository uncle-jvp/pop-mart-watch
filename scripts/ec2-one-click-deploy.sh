#!/bin/bash

# Pop Mart Watch - EC2 一键部署脚本
# 版本: 3.1.0
# 更新日期: 2024-01-XX
# 整合环境设置、配置、构建和部署的所有步骤
# 
# 更新日志:
# v3.1.0 - 降级到 Java 8，修复 Selenium 3.x 兼容性问题
# v3.0.0 - 升级到 Java 17，修复 Selenium 兼容性问题
# v2.0.0 - 修复 Spring Boot 配置文件问题，改进错误诊断
# v1.0.0 - 初始版本

set -e

echo "🚀 Pop Mart Watch EC2 一键部署 v3.1.0 (Java 8)"
echo "=============================================="
echo ""

# 颜色定义
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

# 日志函数
log_info() {
    echo -e "${BLUE}ℹ️  $1${NC}"
}

log_success() {
    echo -e "${GREEN}✅ $1${NC}"
}

log_warning() {
    echo -e "${YELLOW}⚠️  $1${NC}"
}

log_error() {
    echo -e "${RED}❌ $1${NC}"
}

# 检查是否为 root 用户
if [ "$EUID" -eq 0 ]; then
    log_error "请不要使用 root 用户运行此脚本"
    exit 1
fi

# 步骤1: 环境检测和设置
echo "📋 步骤 1/6: 环境检测和设置"
echo "=========================="

# 检测操作系统
if [ -f /etc/os-release ]; then
    . /etc/os-release
    OS=$NAME
    VER=$VERSION_ID
else
    log_error "无法检测操作系统版本"
    exit 1
fi

log_info "检测到操作系统: $OS $VER"

# 检查是否已安装必要工具
check_and_install_tools() {
    log_info "检查并安装必要工具..."
    
    if [[ "$OS" == *"Amazon Linux"* ]]; then
        # 更新系统
        sudo yum update -y
        
        # 安装基础工具（跳过curl，因为curl-minimal已经足够）
        sudo yum install -y wget unzip git htop nano tree
        
        # 检查curl是否可用，如果不可用则安装
        if ! command -v curl &> /dev/null; then
            log_info "安装 curl..."
            # 使用 --allowerasing 解决包冲突
            sudo yum install -y curl --allowerasing
        else
            log_info "curl 已可用，跳过安装"
        fi
        
        # 安装 Java 8
        if ! command -v java &> /dev/null; then
            log_info "安装 Java 8..."
            sudo yum install -y java-1.8.0-openjdk java-1.8.0-openjdk-devel
        fi
        
        # 安装 Maven
        if ! command -v mvn &> /dev/null; then
            log_info "安装 Maven..."
            sudo yum install -y maven
        fi
        
        # 安装 Docker
        if ! command -v docker &> /dev/null; then
            log_info "安装 Docker..."
            sudo yum install -y docker
            sudo systemctl start docker
            sudo systemctl enable docker
            sudo usermod -a -G docker $USER
        fi
        
        # 安装 Chrome
        if ! command -v google-chrome &> /dev/null; then
            log_info "安装 Google Chrome..."
            curl -O https://dl.google.com/linux/direct/google-chrome-stable_current_x86_64.rpm
            sudo yum install -y ./google-chrome-stable_current_x86_64.rpm
            rm -f google-chrome-stable_current_x86_64.rpm
        fi
        
    elif [[ "$OS" == *"Ubuntu"* ]]; then
        # 更新系统
        sudo apt-get update -y && sudo apt-get upgrade -y
        
        # 安装基础工具
        sudo apt-get install -y curl wget unzip git htop nano tree apt-transport-https ca-certificates gnupg lsb-release
        
        # 安装 Java 8
        if ! command -v java &> /dev/null; then
            log_info "安装 Java 8..."
            sudo apt-get install -y openjdk-8-jdk
        fi
        
        # 安装 Maven
        if ! command -v mvn &> /dev/null; then
            log_info "安装 Maven..."
            sudo apt-get install -y maven
        fi
        
        # 安装 Docker
        if ! command -v docker &> /dev/null; then
            log_info "安装 Docker..."
            curl -fsSL https://download.docker.com/linux/ubuntu/gpg | sudo gpg --dearmor -o /usr/share/keyrings/docker-archive-keyring.gpg
            echo "deb [arch=$(dpkg --print-architecture) signed-by=/usr/share/keyrings/docker-archive-keyring.gpg] https://download.docker.com/linux/ubuntu $(lsb_release -cs) stable" | sudo tee /etc/apt/sources.list.d/docker.list > /dev/null
            sudo apt-get update -y
            sudo apt-get install -y docker-ce docker-ce-cli containerd.io
            sudo systemctl start docker
            sudo systemctl enable docker
            sudo usermod -a -G docker $USER
        fi
        
        # 安装 Chrome
        if ! command -v google-chrome &> /dev/null; then
            log_info "安装 Google Chrome..."
            wget -q -O - https://dl.google.com/linux/linux_signing_key.pub | sudo apt-key add -
            echo "deb [arch=amd64] http://dl.google.com/linux/chrome/deb/ stable main" | sudo tee /etc/apt/sources.list.d/google-chrome.list
            sudo apt-get update -y
            sudo apt-get install -y google-chrome-stable
        fi
    else
        log_error "不支持的操作系统: $OS"
        exit 1
    fi
    
    # 安装 Docker Compose
    if ! command -v docker-compose &> /dev/null; then
        log_info "安装 Docker Compose..."
        DOCKER_COMPOSE_VERSION="2.24.0"
        sudo curl -L "https://github.com/docker/compose/releases/download/v${DOCKER_COMPOSE_VERSION}/docker-compose-$(uname -s)-$(uname -m)" -o /usr/local/bin/docker-compose
        sudo chmod +x /usr/local/bin/docker-compose
        sudo ln -sf /usr/local/bin/docker-compose /usr/bin/docker-compose
    fi
}

check_and_install_tools
log_success "环境设置完成"

# 检查 Docker 权限
if ! docker info &> /dev/null 2>&1; then
    log_warning "Docker 权限需要重新登录生效"
    log_info "请运行以下命令应用权限: newgrp docker"
    log_info "或者重新登录 SSH 会话"
    
    # 尝试应用权限
    if command -v newgrp &> /dev/null; then
        log_info "尝试应用 Docker 权限..."
        exec newgrp docker
    fi
fi

# 步骤2: 项目设置
echo ""
echo "📁 步骤 2/6: 项目设置"
echo "==================="

# 检查是否在项目目录中
if [ ! -f "pom.xml" ]; then
    log_error "请在项目根目录运行此脚本"
    log_info "如果还没有克隆项目，请先运行:"
    log_info "git clone https://github.com/your-username/pop-mart-watch.git"
    log_info "cd pop-mart-watch"
    exit 1
fi

# 创建必要目录
mkdir -p logs data/mysql backups
log_success "项目目录设置完成"

# 步骤3: 配置设置
echo ""
echo "⚙️  步骤 3/6: 配置设置"
echo "==================="

# 检查是否已有配置文件
if [ -f ".env" ]; then
    log_warning ".env 文件已存在"
    read -p "是否要重新配置？(y/N): " -n 1 -r
    echo
    if [[ ! $REPLY =~ ^[Yy]$ ]]; then
        log_info "使用现有配置"
        source .env
    else
        # 备份现有配置
        cp .env ".env.backup.$(date +%Y%m%d_%H%M%S)"
        log_info "已备份现有配置"
        RECONFIGURE=true
    fi
else
    RECONFIGURE=true
fi

if [ "$RECONFIGURE" = true ]; then
    log_info "开始配置设置..."
    
    # 数据库配置
    echo ""
    echo "🗄️  数据库配置"
    echo "=============="
    read -p "数据库名称 [popmart_watch]: " DB_NAME
    DB_NAME=${DB_NAME:-popmart_watch}
    
    read -p "数据库用户名 [popmart]: " DB_USERNAME
    DB_USERNAME=${DB_USERNAME:-popmart}
    
    while true; do
        read -s -p "数据库密码: " DB_PASSWORD
        echo
        if [ -n "$DB_PASSWORD" ]; then
            break
        fi
        log_error "密码不能为空，请重新输入"
    done
    
    while true; do
        read -s -p "数据库 root 密码: " DB_ROOT_PASSWORD
        echo
        if [ -n "$DB_ROOT_PASSWORD" ]; then
            break
        fi
        log_error "root 密码不能为空，请重新输入"
    done
    
    # Discord 配置
    echo ""
    echo "🤖 Discord 配置"
    echo "==============="
    read -p "Discord Bot Token: " DISCORD_BOT_TOKEN
    read -p "Discord Guild ID (服务器ID): " DISCORD_GUILD_ID
    read -p "Discord Webhook URL: " DISCORD_WEBHOOK_URL
    
    # 应用配置
    echo ""
    echo "⚙️  应用配置"
    echo "==========="
    read -p "监控间隔(分钟) [5]: " POLL_INTERVAL
    POLL_INTERVAL=${POLL_INTERVAL:-5}
    
    read -p "JVM 内存设置 [-Xmx1g -Xms512m]: " JAVA_OPTS
    JAVA_OPTS=${JAVA_OPTS:--Xmx1g -Xms512m}
    
    # 生成 .env 文件
    cat > .env << EOF
# Pop Mart Watch Docker 环境配置
# 生成时间: $(date)

# 数据库配置
DB_NAME=$DB_NAME
DB_USERNAME=$DB_USERNAME
DB_PASSWORD=$DB_PASSWORD
DB_ROOT_PASSWORD=$DB_ROOT_PASSWORD

# Discord 配置
DISCORD_BOT_TOKEN=$DISCORD_BOT_TOKEN
DISCORD_GUILD_ID=$DISCORD_GUILD_ID
DISCORD_WEBHOOK_URL=$DISCORD_WEBHOOK_URL

# 应用配置
SPRING_PROFILES_ACTIVE=production
JAVA_OPTS="$JAVA_OPTS"

# 监控配置
POPMART_MONITOR_POLL_INTERVAL=$POLL_INTERVAL
POPMART_MONITOR_NOTIFICATION_TYPE=discord

# Docker 配置
COMPOSE_PROJECT_NAME=pop-mart-watch
EOF
    
    chmod 600 .env
    log_success "环境配置文件已生成"
    
    # 创建 Docker 应用配置
    mkdir -p src/main/resources
    cat > src/main/resources/application-production.yml << 'EOF'
# Pop Mart Watch Production 环境配置 (Java 8)
spring:
  datasource:
    url: jdbc:mysql://mysql:3306/${DB_NAME:popmart_watch}?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC&characterEncoding=utf8mb4
    username: ${DB_USERNAME:popmart}
    password: ${DB_PASSWORD}
    driver-class-name: com.mysql.cj.jdbc.Driver
    
    druid:
      initial-size: 5
      min-idle: 5
      max-active: 20
      max-wait: 60000
      time-between-eviction-runs-millis: 60000
      min-evictable-idle-time-millis: 300000
      validation-query: SELECT 1
      test-while-idle: true
      test-on-borrow: false
      test-on-return: false
      pool-prepared-statements: true
      max-pool-prepared-statement-per-connection-size: 20
      
      stat-view-servlet:
        enabled: false
      web-stat-filter:
        enabled: false

# MyBatis Plus 配置
mybatis-plus:
  configuration:
    map-underscore-to-camel-case: true
    log-impl: org.apache.ibatis.logging.stdout.StdOutImpl
  global-config:
    db-config:
      logic-delete-field: deleted
      logic-delete-value: 1
      logic-not-delete-value: 0
      id-type: auto
  mapper-locations: classpath*:mapper/**/*.xml

# Pop Mart 监控配置
popmart:
  monitor:
    poll-interval: ${POPMART_MONITOR_POLL_INTERVAL:5}
    notification:
      type: ${POPMART_MONITOR_NOTIFICATION_TYPE:discord}
      discord:
        webhook-url: ${DISCORD_WEBHOOK_URL}
  
  discord:
    bot-token: ${DISCORD_BOT_TOKEN}
    guild-id: ${DISCORD_GUILD_ID}

# 日志配置
logging:
  level:
    com.popmart: INFO
    org.springframework.web: INFO
    org.mybatis: INFO
  pattern:
    console: "%d{yyyy-MM-dd HH:mm:ss} [%thread] %-5level %logger{36} - %msg%n"
  file:
    name: /app/logs/pop-mart-watch.log
    max-size: 100MB
    max-history: 30

# Actuator 配置
management:
  endpoints:
    web:
      exposure:
        include: health,info,metrics
  endpoint:
    health:
      show-details: when-authorized

# 服务器配置
server:
  port: 8080
  servlet:
    context-path: /
EOF
    
    log_success "Docker 应用配置已生成"
else
    log_info "加载现有配置..."
    # 安全地加载 .env 文件，避免执行意外命令
    if [ -f ".env" ]; then
        # 使用 set -a 自动导出变量，避免 source 执行问题
        set -a
        . .env
        set +a
        log_success "配置加载完成"
    else
        log_error ".env 文件不存在"
        exit 1
    fi
fi

# 步骤4: 构建应用
echo ""
echo "🔨 步骤 4/6: 构建应用"
echo "=================="

# 检查是否需要重新构建
FORCE_REBUILD=false

# 检查配置文件是否比 JAR 文件新
CONFIG_CHANGED=false
if [ -f "target/pop-mart-watch-1.0.0.jar" ] && [ -f "src/main/resources/application-production.yml" ]; then
    if [ "src/main/resources/application-production.yml" -nt "target/pop-mart-watch-1.0.0.jar" ]; then
        CONFIG_CHANGED=true
        log_info "检测到配置文件已更新"
    fi
fi

if [ -f "target/pop-mart-watch-1.0.0.jar" ] && docker images | grep -q "pop-mart-watch" && [ "$CONFIG_CHANGED" = false ]; then
    log_info "检测到现有构建和镜像"
    read -p "是否强制重新构建？(y/N): " -n 1 -r
    echo
    if [[ $REPLY =~ ^[Yy]$ ]]; then
        FORCE_REBUILD=true
    fi
else
    FORCE_REBUILD=true
    if [ "$CONFIG_CHANGED" = true ]; then
        log_info "配置文件已更新，需要重新构建"
    fi
fi

if [ "$FORCE_REBUILD" = true ]; then
    log_info "清理之前的构建..."
    mvn clean

    log_info "编译应用..."
    mvn package -DskipTests

    # 检查 JAR 文件
    JAR_FILE="target/pop-mart-watch-1.0.0.jar"
    if [ ! -f "$JAR_FILE" ]; then
        log_error "JAR 文件不存在: $JAR_FILE"
        exit 1
    fi

    log_success "应用编译完成"

    # 构建 Docker 镜像
    log_info "构建 Docker 镜像..."
    docker build \
        --build-arg JAR_FILE="target/pop-mart-watch-1.0.0.jar" \
        --tag "pop-mart-watch:1.0.0" \
        --tag "pop-mart-watch:latest" \
        .

    log_success "Docker 镜像构建完成"
else
    log_info "跳过构建，使用现有镜像"
    # 确保 JAR 文件存在
    JAR_FILE="target/pop-mart-watch-1.0.0.jar"
    if [ ! -f "$JAR_FILE" ]; then
        log_error "JAR 文件不存在，需要重新构建"
        exit 1
    fi
fi

# 步骤5: 部署服务
echo ""
echo "🚀 步骤 5/6: 部署服务"
echo "=================="

# 检查服务状态
SERVICES_RUNNING=false
if docker-compose ps | grep -q "Up"; then
    SERVICES_RUNNING=true
    log_info "检测到运行中的服务"
fi

# 停止现有服务
if [ "$SERVICES_RUNNING" = true ]; then
    log_info "停止现有服务..."
    docker-compose down
    
    # 等待容器完全停止
    sleep 3
fi

# 拉取基础镜像（仅在需要时）
if ! docker images | grep -q "mysql.*8.0"; then
    log_info "拉取 MySQL 镜像..."
    docker-compose pull mysql
else
    log_info "MySQL 镜像已存在，跳过拉取"
fi

# 启动服务
log_info "启动服务..."
docker-compose up -d

# 等待数据库启动
log_info "等待数据库启动..."
MAX_ATTEMPTS=30
ATTEMPT=1

while [ $ATTEMPT -le $MAX_ATTEMPTS ]; do
    if docker-compose exec -T mysql mysql -u root -p$DB_ROOT_PASSWORD -e "SELECT 1" &> /dev/null; then
        log_success "数据库已就绪"
        break
    fi
    
    echo "⏳ 等待数据库启动... ($ATTEMPT/$MAX_ATTEMPTS)"
    sleep 5
    ATTEMPT=$((ATTEMPT + 1))
done

if [ $ATTEMPT -gt $MAX_ATTEMPTS ]; then
    log_error "数据库启动超时"
    docker-compose logs mysql
    exit 1
fi

# 智能数据库初始化
log_info "检查数据库状态..."
DB_EXISTS=$(docker-compose exec -T mysql mysql -u root -p$DB_ROOT_PASSWORD -e "SELECT SCHEMA_NAME FROM INFORMATION_SCHEMA.SCHEMATA WHERE SCHEMA_NAME='$DB_NAME';" | grep -c "$DB_NAME" || echo "0")

if [ "$DB_EXISTS" = "0" ]; then
    log_info "创建数据库和用户..."
    docker-compose exec -T mysql mysql -u root -p$DB_ROOT_PASSWORD << EOF
CREATE DATABASE IF NOT EXISTS $DB_NAME CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
CREATE USER IF NOT EXISTS '$DB_USERNAME'@'%' IDENTIFIED BY '$DB_PASSWORD';
GRANT ALL PRIVILEGES ON $DB_NAME.* TO '$DB_USERNAME'@'%';
FLUSH PRIVILEGES;
EOF
    
    # 执行数据库脚本
    if [ -f "src/main/resources/sql/schema.sql" ]; then
        log_info "创建数据库表结构..."
        docker-compose exec -T mysql mysql -u $DB_USERNAME -p$DB_PASSWORD $DB_NAME < src/main/resources/sql/schema.sql
        log_success "数据库表结构创建完成"
    fi
else
    log_info "数据库已存在，跳过初始化"
    
    # 检查是否需要更新表结构
    if [ -f "src/main/resources/sql/schema.sql" ]; then
        read -p "是否要更新数据库表结构？(y/N): " -n 1 -r
        echo
        if [[ $REPLY =~ ^[Yy]$ ]]; then
            log_warning "更新表结构可能影响现有数据，建议先备份"
            read -p "确定要继续吗？(y/N): " -n 1 -r
            echo
            if [[ $REPLY =~ ^[Yy]$ ]]; then
                log_info "更新数据库表结构..."
                docker-compose exec -T mysql mysql -u $DB_USERNAME -p$DB_PASSWORD $DB_NAME < src/main/resources/sql/schema.sql
                log_success "数据库表结构更新完成"
            fi
        fi
    fi
fi

# 等待应用启动
log_info "等待应用启动..."
MAX_ATTEMPTS=60
ATTEMPT=1

while [ $ATTEMPT -le $MAX_ATTEMPTS ]; do
    if curl -s http://localhost:8080/actuator/health &> /dev/null; then
        log_success "应用已启动"
        break
    fi
    
    echo "⏳ 等待应用启动... ($ATTEMPT/$MAX_ATTEMPTS)"
    sleep 5
    ATTEMPT=$((ATTEMPT + 1))
done

if [ $ATTEMPT -gt $MAX_ATTEMPTS ]; then
    log_error "应用启动超时"
    log_info "正在收集诊断信息..."
    
    # 显示应用日志
    echo ""
    echo "📋 应用日志 (最后 50 行):"
    docker-compose logs --tail=50 app
    
    # 显示容器状态
    echo ""
    echo "📊 容器状态:"
    docker-compose ps
    
    # 检查常见问题
    echo ""
    echo "🔍 故障排除建议:"
    echo "1. 检查配置文件是否正确: src/main/resources/application-production.yml"
    echo "2. 检查环境变量是否设置: cat .env"
    echo "3. 检查数据库连接: docker-compose exec mysql mysql -u $DB_USERNAME -p$DB_PASSWORD -e 'SELECT 1'"
    echo "4. 重新构建应用: mvn clean package -DskipTests && docker-compose up --build -d"
    echo "5. 查看完整日志: docker-compose logs -f app"
    
    exit 1
fi

# 步骤6: 验证部署
echo ""
echo "🔍 步骤 6/6: 验证部署"
echo "=================="

# 检查服务状态
log_info "检查服务状态..."
docker-compose ps

# 检查健康状态
HEALTH_STATUS=$(curl -s http://localhost:8080/actuator/health | grep -o '"status":"[^"]*"' | cut -d'"' -f4 2>/dev/null || echo "UNKNOWN")
if [ "$HEALTH_STATUS" = "UP" ]; then
    log_success "应用健康状态: $HEALTH_STATUS"
else
    log_warning "应用健康状态: $HEALTH_STATUS"
fi

# 检查API
if curl -s http://localhost:8080/api/monitor/health &> /dev/null; then
    log_success "API 接口正常"
else
    log_warning "API 接口异常"
fi

# 显示资源使用
echo ""
echo "💻 资源使用情况:"
docker stats --no-stream --format "table {{.Container}}\t{{.CPUPerc}}\t{{.MemUsage}}"

# 部署完成
echo ""
echo "🎉 部署完成！"
echo "============"
echo ""
echo "📋 服务信息:"
echo "   数据库: $DB_NAME"
echo "   监控间隔: $POPMART_MONITOR_POLL_INTERVAL 分钟"
echo "   Discord Bot: $([ -n "$DISCORD_BOT_TOKEN" ] && echo "已配置" || echo "未配置")"
echo "   Discord Webhook: $([ -n "$DISCORD_WEBHOOK_URL" ] && echo "已配置" || echo "未配置")"
echo ""
echo "🌐 访问地址:"
echo "   应用主页: http://localhost:8080"
echo "   健康检查: http://localhost:8080/actuator/health"
echo "   API 接口: http://localhost:8080/api/monitor/health"
echo ""
echo "📊 管理命令:"
echo "   查看日志: docker-compose logs -f app"
echo "   查看状态: docker-compose ps"
echo "   停止服务: docker-compose down"
echo "   重启应用: docker-compose restart app"
echo ""
echo "🧪 测试命令:"
echo "   测试 Discord 通知: curl -X POST http://localhost:8080/api/monitor/test-discord"
echo "   查看监控统计: curl http://localhost:8080/api/monitor/stats"
echo ""
echo "📝 添加商品监控:"
echo '   curl -X POST http://localhost:8080/api/monitor/products \'
echo '     -H "Content-Type: application/json" \'
echo '     -d '"'"'{'
echo '       "url": "https://www.popmart.com/us/products/1739/",
echo '       "productName": "THE MONSTERS Classic Series",
echo '       "userId": "test-user"
echo '     }'"'"
echo ""
echo "⚠️  重要提醒:"
echo "   - 请确保 Discord Bot Token 和 Webhook URL 配置正确"
echo "   - 定期备份数据库: docker-compose exec -T mysql mysqldump -u $DB_USERNAME -p$DB_PASSWORD $DB_NAME > backup.sql"
echo "   - 监控系统资源使用情况"
echo ""
echo "🎯 部署成功！Pop Mart Watch 已准备就绪！" 
#!/bin/bash

# Pop Mart Watch - 快速重新部署脚本
# 用于代码更新后的快速重新部署，跳过环境设置和配置

set -e

echo "⚡ Pop Mart Watch 快速重新部署"
echo "=============================="
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

# 检查前置条件
echo "🔍 检查前置条件"
echo "==============="

# 检查是否在项目根目录
if [ ! -f "pom.xml" ]; then
    log_error "请在项目根目录运行此脚本"
    exit 1
fi

# 检查环境配置文件
if [ ! -f ".env" ]; then
    log_error "环境配置文件 .env 不存在"
    log_info "请先运行完整部署脚本: ./scripts/ec2-one-click-deploy.sh"
    exit 1
fi

# 加载环境变量
source .env

# 检查 Docker 是否可用
if ! command -v docker &> /dev/null; then
    log_error "Docker 未安装或不可用"
    exit 1
fi

if ! command -v docker-compose &> /dev/null; then
    log_error "Docker Compose 未安装或不可用"
    exit 1
fi

log_success "前置条件检查通过"

# 步骤1: 构建应用
echo ""
echo "🔨 步骤 1/3: 重新构建应用"
echo "========================"

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
log_info "重新构建 Docker 镜像..."
docker build \
    --build-arg JAR_FILE="target/pop-mart-watch-1.0.0.jar" \
    --tag "pop-mart-watch:1.0.0" \
    --tag "pop-mart-watch:latest" \
    .

log_success "Docker 镜像构建完成"

# 步骤2: 重新部署服务
echo ""
echo "🚀 步骤 2/3: 重新部署服务"
echo "======================="

# 停止现有服务
log_info "停止现有服务..."
docker-compose down

# 等待容器完全停止
sleep 3

# 启动服务
log_info "启动服务..."
docker-compose up -d

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
    docker-compose logs app
    exit 1
fi

# 步骤3: 验证部署
echo ""
echo "🔍 步骤 3/3: 验证部署"
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
echo "🎉 快速重新部署完成！"
echo "==================="
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
echo "⚡ 快速重新部署成功！应用已更新并运行！" 
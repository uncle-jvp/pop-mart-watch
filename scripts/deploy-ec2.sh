#!/bin/bash

# Pop Mart Watch EC2 部署脚本
# 在已配置的 EC2 环境中部署应用

set -e

echo "🚀 Pop Mart Watch EC2 部署"
echo "=========================="

# 检查是否在正确的目录
if [ ! -f "pom.xml" ]; then
    echo "❌ 请在项目根目录运行此脚本"
    exit 1
fi

# 检查生产环境配置文件
PROD_CONFIG="src/main/resources/application-production.yml"
if [ ! -f "$PROD_CONFIG" ]; then
    echo "⚠️  生产环境配置文件不存在: $PROD_CONFIG"
    echo "正在从模板创建..."
    
    if [ ! -f "src/main/resources/application-production.yml.example" ]; then
        echo "❌ 配置模板文件不存在！"
        exit 1
    fi
    
    cp src/main/resources/application-production.yml.example "$PROD_CONFIG"
    echo "✅ 已创建生产环境配置文件"
    echo ""
    echo "⚠️  请编辑 $PROD_CONFIG 并填入实际配置："
    echo "  - 数据库连接信息"
    echo "  - Discord Bot Token"
    echo "  - 其他生产环境设置"
    echo ""
    echo "编辑完成后重新运行此脚本。"
    exit 1
fi

echo "✅ 找到生产环境配置文件"

# 停止现有服务（如果正在运行）
echo "🛑 停止现有服务..."
sudo systemctl stop pop-mart-watch || true

# 备份现有 JAR 文件
if [ -f "target/pop-mart-watch-1.0.0.jar" ]; then
    echo "💾 备份现有应用..."
    cp target/pop-mart-watch-1.0.0.jar target/pop-mart-watch-1.0.0.jar.backup.$(date +%Y%m%d_%H%M%S)
fi

# 构建应用
echo "🔨 构建应用..."
mvn clean package -DskipTests

if [ $? -ne 0 ]; then
    echo "❌ 构建失败！"
    exit 1
fi

# 验证 JAR 文件
if [ ! -f "target/pop-mart-watch-1.0.0.jar" ]; then
    echo "❌ JAR 文件未生成！"
    exit 1
fi

echo "✅ 构建成功"
echo "📦 JAR 文件大小: $(du -h target/pop-mart-watch-1.0.0.jar | cut -f1)"

# 创建日志目录
mkdir -p logs

# 测试配置文件
echo "🧪 测试配置文件..."
timeout 30s java -jar target/pop-mart-watch-1.0.0.jar --spring.profiles.active=production --spring.main.web-application-type=none --logging.level.root=ERROR || {
    echo "⚠️  配置测试超时或失败，请检查配置文件"
}

# 启动服务
echo "🎯 启动服务..."
sudo systemctl start pop-mart-watch

# 等待服务启动
echo "⏳ 等待服务启动..."
sleep 10

# 检查服务状态
if sudo systemctl is-active --quiet pop-mart-watch; then
    echo "✅ 服务启动成功！"
    
    # 显示服务状态
    echo ""
    echo "📊 服务状态："
    sudo systemctl status pop-mart-watch --no-pager -l
    
    echo ""
    echo "🌐 应用访问地址："
    echo "  http://$(curl -s http://169.254.169.254/latest/meta-data/public-ipv4):8080"
    echo ""
    echo "🔍 健康检查："
    echo "  http://$(curl -s http://169.254.169.254/latest/meta-data/public-ipv4):8080/actuator/health"
    
else
    echo "❌ 服务启动失败！"
    echo ""
    echo "📋 查看错误日志："
    sudo journalctl -u pop-mart-watch --no-pager -l
    exit 1
fi

# 启用开机自启
echo "⚙️  启用开机自启..."
sudo systemctl enable pop-mart-watch

echo ""
echo "🎉 部署完成！"
echo ""
echo "🔧 有用的命令："
echo "  sudo systemctl status pop-mart-watch   # 查看服务状态"
echo "  sudo systemctl restart pop-mart-watch  # 重启服务"
echo "  sudo journalctl -u pop-mart-watch -f   # 查看实时日志"
echo "  tail -f logs/pop-mart-watch.log        # 查看应用日志"
echo ""
echo "📊 监控端点："
echo "  /actuator/health  - 健康检查"
echo "  /actuator/info    - 应用信息"
echo "  /actuator/metrics - 应用指标" 
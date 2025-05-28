#!/bin/bash

# Pop Mart Watch EC2 部署脚本
# 适用于 Amazon Linux 2 / Ubuntu 20.04+

set -e

echo "🚀 Pop Mart Watch EC2 部署脚本"
echo "================================"

# 检测操作系统
if [ -f /etc/os-release ]; then
    . /etc/os-release
    OS=$NAME
    VER=$VERSION_ID
else
    echo "❌ 无法检测操作系统"
    exit 1
fi

echo "📋 检测到操作系统: $OS $VER"

# 更新系统包
echo "📦 更新系统包..."
if [[ "$OS" == *"Amazon Linux"* ]]; then
    sudo yum update -y
elif [[ "$OS" == *"Ubuntu"* ]]; then
    sudo apt update && sudo apt upgrade -y
else
    echo "⚠️  未知操作系统，请手动安装依赖"
fi

# 安装 Java 1.8
echo "☕ 安装 Java 1.8..."
if [[ "$OS" == *"Amazon Linux"* ]]; then
    sudo yum install -y java-1.8.0-openjdk java-1.8.0-openjdk-devel
elif [[ "$OS" == *"Ubuntu"* ]]; then
    sudo apt install -y openjdk-8-jdk openjdk-8-jre
fi

# 验证 Java 安装
java -version
if [ $? -ne 0 ]; then
    echo "❌ Java 安装失败"
    exit 1
fi

# 安装 Maven
echo "🔨 安装 Maven..."
if [[ "$OS" == *"Amazon Linux"* ]]; then
    sudo yum install -y maven
elif [[ "$OS" == *"Ubuntu"* ]]; then
    sudo apt install -y maven
fi

# 验证 Maven 安装
mvn -version
if [ $? -ne 0 ]; then
    echo "❌ Maven 安装失败"
    exit 1
fi

# 安装 Docker
echo "🐳 安装 Docker..."
if [[ "$OS" == *"Amazon Linux"* ]]; then
    sudo yum install -y docker
    sudo systemctl start docker
    sudo systemctl enable docker
    sudo usermod -a -G docker ec2-user
    DOCKER_USER="ec2-user"
elif [[ "$OS" == *"Ubuntu"* ]]; then
    sudo apt install -y docker.io
    sudo systemctl start docker
    sudo systemctl enable docker
    sudo usermod -a -G docker $USER
    DOCKER_USER="$USER"
fi

# 安装 Docker Compose
echo "🔧 安装 Docker Compose..."
sudo curl -L "https://github.com/docker/compose/releases/latest/download/docker-compose-$(uname -s)-$(uname -m)" -o /usr/local/bin/docker-compose
sudo chmod +x /usr/local/bin/docker-compose

# 验证 Docker 安装
docker --version
docker-compose --version
if [ $? -ne 0 ]; then
    echo "❌ Docker 安装失败"
    exit 1
fi

# 测试 Docker 权限
echo "🧪 测试 Docker 权限..."
if ! docker info > /dev/null 2>&1; then
    echo "⚠️  Docker 权限未生效，尝试应用新的组权限..."
    # 尝试应用新的组权限
    newgrp docker << EOF
docker info > /dev/null 2>&1
EOF
    if [ $? -ne 0 ]; then
        echo "⚠️  需要重新登录以使 Docker 组权限生效"
        echo "💡 或者运行: newgrp docker"
    fi
fi

# 安装 MySQL 客户端
echo "🗄️  安装 MySQL 客户端..."
if [[ "$OS" == *"Amazon Linux"* ]]; then
    sudo yum install -y mysql
elif [[ "$OS" == *"Ubuntu"* ]]; then
    sudo apt install -y mysql-client
fi

# 安装 Chrome (用于 Selenium)
echo "🌐 安装 Chrome..."
if [[ "$OS" == *"Amazon Linux"* ]]; then
    sudo yum install -y wget
    wget -q -O - https://dl.google.com/linux/linux_signing_key.pub | sudo rpm --import -
    sudo yum install -y https://dl.google.com/linux/direct/google-chrome-stable_current_x86_64.rpm
elif [[ "$OS" == *"Ubuntu"* ]]; then
    wget -q -O - https://dl.google.com/linux/linux_signing_key.pub | sudo apt-key add -
    echo "deb [arch=amd64] http://dl.google.com/linux/chrome/deb/ stable main" | sudo tee /etc/apt/sources.list.d/google-chrome.list
    sudo apt update
    sudo apt install -y google-chrome-stable
fi

# 安装 Git
echo "📚 安装 Git..."
if [[ "$OS" == *"Amazon Linux"* ]]; then
    sudo yum install -y git
elif [[ "$OS" == *"Ubuntu"* ]]; then
    sudo apt install -y git
fi

# 创建应用目录
echo "📁 创建应用目录..."
sudo mkdir -p /opt/pop-mart-watch
sudo chown $USER:$USER /opt/pop-mart-watch
cd /opt/pop-mart-watch

# 克隆项目（如果不存在）
if [ ! -d ".git" ]; then
    echo "📥 克隆项目..."
    echo "请手动克隆项目到 /opt/pop-mart-watch 目录"
    echo "git clone <your-repository-url> ."
fi

# 创建日志目录
mkdir -p logs

# 设置防火墙规则
echo "🔥 配置防火墙..."
if command -v firewall-cmd &> /dev/null; then
    sudo firewall-cmd --permanent --add-port=8080/tcp
    sudo firewall-cmd --permanent --add-port=3306/tcp
    sudo firewall-cmd --reload
elif command -v ufw &> /dev/null; then
    sudo ufw allow 8080/tcp
    sudo ufw allow 3306/tcp
fi

# 创建 systemd 服务文件
echo "⚙️  创建系统服务..."
sudo tee /etc/systemd/system/pop-mart-watch.service > /dev/null <<EOF
[Unit]
Description=Pop Mart Watch Service
After=network.target

[Service]
Type=simple
User=$USER
WorkingDirectory=/opt/pop-mart-watch
ExecStart=/usr/bin/java -jar target/pop-mart-watch-1.0.0.jar --spring.profiles.active=production
Restart=always
RestartSec=10
StandardOutput=journal
StandardError=journal

[Install]
WantedBy=multi-user.target
EOF

# 重新加载 systemd
sudo systemctl daemon-reload

echo ""
echo "✅ EC2 环境设置完成！"
echo ""
echo "📋 接下来的步骤："
echo "1. 克隆项目代码到 /opt/pop-mart-watch"
echo "2. 配置数据库连接"
echo "3. 创建生产环境配置文件"
echo "4. 构建并部署应用"
echo ""
echo "🔧 有用的命令："
echo "  sudo systemctl start pop-mart-watch    # 启动服务"
echo "  sudo systemctl stop pop-mart-watch     # 停止服务"
echo "  sudo systemctl status pop-mart-watch   # 查看状态"
echo "  sudo journalctl -u pop-mart-watch -f   # 查看日志"
echo ""
if ! docker info > /dev/null 2>&1; then
    echo "⚠️  重要：Docker 组权限需要重新登录才能生效"
    echo "   请运行: exit 然后重新 SSH 连接"
    echo "   或者运行: newgrp docker"
else
    echo "✅ Docker 权限已生效，可以直接使用"
fi 
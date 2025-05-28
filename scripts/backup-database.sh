#!/bin/bash

# Pop Mart Watch - 数据库备份脚本

set -e

echo "💾 Pop Mart Watch 数据库备份"
echo "============================"

# 检查是否在项目根目录
if [ ! -f ".env" ]; then
    echo "❌ 环境变量文件 .env 不存在"
    echo "💡 请确保在项目根目录运行此脚本"
    exit 1
fi

# 加载环境变量
source .env

# 检查 Docker Compose 是否可用
if ! command -v docker-compose &> /dev/null; then
    echo "❌ Docker Compose 未安装或不可用"
    exit 1
fi

# 检查 MySQL 容器是否运行
if ! docker-compose ps mysql | grep -q "Up"; then
    echo "❌ MySQL 容器未运行"
    echo "💡 请先启动服务: docker-compose up -d mysql"
    exit 1
fi

# 创建备份目录
BACKUP_DIR="backups"
mkdir -p "$BACKUP_DIR"

# 生成备份文件名
TIMESTAMP=$(date +"%Y%m%d_%H%M%S")
BACKUP_FILE="$BACKUP_DIR/popmart_watch_backup_$TIMESTAMP.sql"

echo "📋 备份信息:"
echo "   数据库: $DB_NAME"
echo "   用户: $DB_USERNAME"
echo "   备份文件: $BACKUP_FILE"
echo ""

# 执行备份
echo "💾 开始备份数据库..."

docker-compose exec -T mysql mysqldump \
    -u "$DB_USERNAME" \
    -p"$DB_PASSWORD" \
    --single-transaction \
    --routines \
    --triggers \
    --add-drop-table \
    --add-locks \
    --create-options \
    --disable-keys \
    --extended-insert \
    --quick \
    --set-charset \
    "$DB_NAME" > "$BACKUP_FILE"

if [ $? -eq 0 ]; then
    echo "✅ 数据库备份完成"
    
    # 压缩备份文件
    echo "🗜️  压缩备份文件..."
    gzip "$BACKUP_FILE"
    COMPRESSED_FILE="${BACKUP_FILE}.gz"
    
    # 显示备份信息
    BACKUP_SIZE=$(du -h "$COMPRESSED_FILE" | cut -f1)
    echo "✅ 备份文件已压缩: $COMPRESSED_FILE"
    echo "📦 备份大小: $BACKUP_SIZE"
    
    # 清理旧备份（保留最近10个）
    echo ""
    echo "🧹 清理旧备份文件..."
    cd "$BACKUP_DIR"
    ls -t popmart_watch_backup_*.sql.gz | tail -n +11 | xargs -r rm
    BACKUP_COUNT=$(ls -1 popmart_watch_backup_*.sql.gz 2>/dev/null | wc -l)
    echo "✅ 当前保留 $BACKUP_COUNT 个备份文件"
    cd ..
    
    echo ""
    echo "🎉 备份完成！"
    echo ""
    echo "📁 备份文件: $COMPRESSED_FILE"
    echo "📊 备份大小: $BACKUP_SIZE"
    echo ""
    echo "💡 恢复命令:"
    echo "   ./scripts/restore-database.sh $COMPRESSED_FILE"
    
else
    echo "❌ 数据库备份失败"
    rm -f "$BACKUP_FILE"
    exit 1
fi 
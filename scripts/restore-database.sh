#!/bin/bash

# Pop Mart Watch - 数据库恢复脚本

set -e

echo "🔄 Pop Mart Watch 数据库恢复"
echo "============================"

# 检查参数
if [ $# -eq 0 ]; then
    echo "❌ 请提供备份文件路径"
    echo "💡 用法: $0 <backup_file.sql.gz>"
    echo "💡 示例: $0 backups/popmart_watch_backup_20240101_120000.sql.gz"
    exit 1
fi

BACKUP_FILE="$1"

# 检查备份文件是否存在
if [ ! -f "$BACKUP_FILE" ]; then
    echo "❌ 备份文件不存在: $BACKUP_FILE"
    exit 1
fi

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

echo "📋 恢复信息:"
echo "   备份文件: $BACKUP_FILE"
echo "   数据库: $DB_NAME"
echo "   用户: $DB_USERNAME"
echo ""

# 确认恢复操作
echo "⚠️  警告: 此操作将覆盖现有数据库数据！"
read -p "确定要继续恢复吗？(y/N): " -n 1 -r
echo
if [[ ! $REPLY =~ ^[Yy]$ ]]; then
    echo "❌ 恢复操作已取消"
    exit 0
fi

# 检查备份文件格式
if [[ "$BACKUP_FILE" == *.gz ]]; then
    echo "🗜️  检测到压缩文件，准备解压..."
    TEMP_SQL_FILE="/tmp/restore_temp_$(date +%s).sql"
    gunzip -c "$BACKUP_FILE" > "$TEMP_SQL_FILE"
    SQL_FILE="$TEMP_SQL_FILE"
    CLEANUP_TEMP=true
else
    SQL_FILE="$BACKUP_FILE"
    CLEANUP_TEMP=false
fi

# 验证 SQL 文件
if ! head -n 5 "$SQL_FILE" | grep -q "MySQL dump"; then
    echo "❌ 无效的 MySQL 备份文件"
    [ "$CLEANUP_TEMP" = true ] && rm -f "$TEMP_SQL_FILE"
    exit 1
fi

echo "✅ 备份文件验证通过"

# 创建恢复前备份
echo ""
echo "💾 创建恢复前备份..."
TIMESTAMP=$(date +"%Y%m%d_%H%M%S")
PRE_RESTORE_BACKUP="backups/pre_restore_backup_$TIMESTAMP.sql"
mkdir -p backups

docker-compose exec -T mysql mysqldump \
    -u "$DB_USERNAME" \
    -p"$DB_PASSWORD" \
    --single-transaction \
    --routines \
    --triggers \
    "$DB_NAME" > "$PRE_RESTORE_BACKUP"

if [ $? -eq 0 ]; then
    gzip "$PRE_RESTORE_BACKUP"
    echo "✅ 恢复前备份已创建: ${PRE_RESTORE_BACKUP}.gz"
else
    echo "❌ 恢复前备份失败"
    [ "$CLEANUP_TEMP" = true ] && rm -f "$TEMP_SQL_FILE"
    exit 1
fi

# 执行恢复
echo ""
echo "🔄 开始恢复数据库..."

# 停止应用以避免数据冲突
echo "🛑 临时停止应用..."
docker-compose stop app

# 执行恢复
docker-compose exec -T mysql mysql \
    -u "$DB_USERNAME" \
    -p"$DB_PASSWORD" \
    "$DB_NAME" < "$SQL_FILE"

if [ $? -eq 0 ]; then
    echo "✅ 数据库恢复完成"
    
    # 重启应用
    echo "🚀 重启应用..."
    docker-compose start app
    
    # 等待应用启动
    echo "⏳ 等待应用启动..."
    MAX_ATTEMPTS=30
    ATTEMPT=1
    
    while [ $ATTEMPT -le $MAX_ATTEMPTS ]; do
        if curl -s http://localhost:8080/actuator/health &> /dev/null; then
            echo "✅ 应用已重新启动"
            break
        fi
        
        echo "⏳ 等待应用启动... ($ATTEMPT/$MAX_ATTEMPTS)"
        sleep 3
        ATTEMPT=$((ATTEMPT + 1))
    done
    
    if [ $ATTEMPT -gt $MAX_ATTEMPTS ]; then
        echo "⚠️  应用启动超时，请手动检查"
        echo "💡 查看日志: docker-compose logs app"
    fi
    
    # 清理临时文件
    [ "$CLEANUP_TEMP" = true ] && rm -f "$TEMP_SQL_FILE"
    
    echo ""
    echo "🎉 数据库恢复成功！"
    echo ""
    echo "📋 恢复信息:"
    echo "   源文件: $BACKUP_FILE"
    echo "   目标数据库: $DB_NAME"
    echo "   恢复前备份: ${PRE_RESTORE_BACKUP}.gz"
    echo ""
    echo "🔍 验证恢复:"
    echo "   检查应用状态: curl http://localhost:8080/actuator/health"
    echo "   查看监控统计: curl http://localhost:8080/api/monitor/stats"
    echo "   查看应用日志: docker-compose logs app"
    
else
    echo "❌ 数据库恢复失败"
    
    # 清理临时文件
    [ "$CLEANUP_TEMP" = true ] && rm -f "$TEMP_SQL_FILE"
    
    # 重启应用
    echo "🚀 重启应用..."
    docker-compose start app
    
    echo ""
    echo "💡 故障排除:"
    echo "   1. 检查备份文件格式是否正确"
    echo "   2. 确认数据库用户权限"
    echo "   3. 查看 MySQL 日志: docker-compose logs mysql"
    echo "   4. 如需回滚，使用恢复前备份: ${PRE_RESTORE_BACKUP}.gz"
    
    exit 1
fi 
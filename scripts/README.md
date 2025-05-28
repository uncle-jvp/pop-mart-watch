# Pop Mart Watch 部署脚本说明

本目录包含了 Pop Mart Watch 项目的核心部署和管理脚本。

## 🚀 一键部署（推荐）

### `ec2-one-click-deploy.sh`
**一键部署脚本** - 整合了所有部署步骤的完整脚本

**功能**：
- 自动检测操作系统（Amazon Linux 2 / Ubuntu）
- 安装所有必要工具（Java、Maven、Docker、Chrome）
- 交互式配置数据库和 Discord
- 构建应用和 Docker 镜像
- 部署完整服务栈
- 验证部署状态

**使用方法**：
```bash
chmod +x scripts/ec2-one-click-deploy.sh
./scripts/ec2-one-click-deploy.sh
```

**适用场景**：
- ✅ 新 EC2 实例首次部署
- ✅ 完整重新部署
- ✅ 新手和高级用户推荐

### `quick-redeploy.sh`
**快速重新部署脚本** - 用于代码更新后的快速重新部署

**功能**：
- 跳过环境设置和配置步骤
- 重新构建应用和 Docker 镜像
- 快速重启服务
- 验证部署状态
- 保留现有数据库数据

**使用方法**：
```bash
# 拉取最新代码后
git pull origin main

# 快速重新部署
chmod +x scripts/quick-redeploy.sh
./scripts/quick-redeploy.sh
```

**适用场景**：
- ✅ 代码更新后的快速部署
- ✅ 应用配置已完成的重新部署
- ✅ 开发和生产环境的快速更新

---

## 🗄️ 数据库管理脚本

### `backup-database.sh`
**数据库备份脚本** - 创建数据库备份

**功能**：
- 完整数据库备份（包含存储过程、触发器）
- 自动压缩备份文件
- 清理旧备份（保留最近10个）

**使用方法**：
```bash
chmod +x scripts/backup-database.sh
./scripts/backup-database.sh
```

**输出**：
- 备份文件：`backups/popmart_watch_backup_YYYYMMDD_HHMMSS.sql.gz`

### `restore-database.sh`
**数据库恢复脚本** - 从备份恢复数据库

**功能**：
- 支持压缩和未压缩的备份文件
- 恢复前自动创建安全备份
- 临时停止应用避免数据冲突
- 恢复后自动重启应用

**使用方法**：
```bash
chmod +x scripts/restore-database.sh
./scripts/restore-database.sh backups/popmart_watch_backup_20240101_120000.sql.gz
```

---

## 📋 使用建议

### 推荐部署流程
1. **首次部署**：使用 `ec2-one-click-deploy.sh` 进行完整部署
2. **初始备份**：部署完成后使用 `backup-database.sh` 创建初始备份
3. **定期维护**：定期运行 `backup-database.sh` 备份数据

### 更新应用流程
1. **拉取最新代码**：`git pull origin main`
2. **快速重新部署**：`./scripts/quick-redeploy.sh`（推荐）
3. **完整重新部署**：`./scripts/ec2-one-click-deploy.sh`（如需重新配置）

### 数据恢复流程
- 如需恢复数据：`./scripts/restore-database.sh <backup_file>`

### 脚本选择指南
- **首次部署** → 使用 `ec2-one-click-deploy.sh`
- **代码更新** → 使用 `quick-redeploy.sh`
- **配置变更** → 使用 `ec2-one-click-deploy.sh`
- **数据备份** → 使用 `backup-database.sh`
- **数据恢复** → 使用 `restore-database.sh`

---

## ⚠️ 注意事项

1. **权限问题**：所有脚本都需要执行权限，使用 `chmod +x scripts/*.sh` 添加
2. **Docker 权限**：首次安装 Docker 后需要重新登录或运行 `newgrp docker`
3. **环境变量**：确保在项目根目录运行脚本
4. **备份重要性**：在进行任何数据库操作前先备份
5. **网络要求**：确保 EC2 安全组开放必要端口（22, 8080）

---

## 🆘 故障排除

如果遇到问题，请按以下顺序检查：

1. **检查脚本权限**：`ls -la scripts/`
2. **检查 Docker 状态**：`docker info`
3. **查看服务日志**：`docker-compose logs app`
4. **检查端口占用**：`netstat -tlnp | grep 8080`
5. **验证配置文件**：`cat .env`

更多故障排除信息请参考主 README.md 文档。

---

## 📁 当前脚本列表

- `ec2-one-click-deploy.sh` - 一键部署脚本（首次部署推荐）
- `quick-redeploy.sh` - 快速重新部署脚本（代码更新推荐）
- `backup-database.sh` - 数据库备份
- `restore-database.sh` - 数据库恢复
- `README.md` - 本说明文档 
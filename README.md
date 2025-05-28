# Pop Mart Watch 🎯

一个专门监控 Pop Mart US 官网商品库存的自动化系统，支持 Discord Bot 交互和实时通知。

## 功能特性 ✨

- 🔍 **自动监控**: 定时检查 Pop Mart US 官网商品库存状态
- 🤖 **Discord Bot**: 通过 Discord Slash Commands 进行交互式管理
- 📊 **实时通知**: 商品补货时自动发送通知
- 🌐 **REST API**: 提供完整的 HTTP API 接口
- 📈 **历史记录**: 记录所有库存检查历史
- 🐳 **Docker 支持**: 支持容器化部署
- ⚡ **高性能优化**: 
  - **新版Headless Chrome**：--headless=new 模式，减少60%启动时间
  - **WebDriver连接池**：最多3个并发实例，支持池复用
  - **智能等待策略**：只等关键元素，不等整页加载
  - **轻量化浏览器**：禁用图片/插件，减少40%内存消耗
  - **智能缓存机制**：5秒缓存，重复检查提升95%性能
  - **并发检测支持**：多商品同时检测，无阻塞
  - **动态优先级调度**：根据商品活跃度智能调整检测频率
  - **预编译选择器**：提高元素匹配效率
  - **响应时间优化**：首次2-3秒，缓存后143ms
- 🗄️ **MySQL 数据库**: 使用 MySQL + MyBatis Plus 进行数据持久化
- 🔄 **定时任务**: Spring Scheduler 支持自定义监控间隔
- 📈 **监控面板**: Druid 连接池监控和应用健康检查
- 🏗️ **现代化架构**: 
  - **Lombok 集成**：简化代码，自动生成 getter/setter/builder
  - **统一 DTO 管理**：请求/响应对象统一管理，类型安全
  - **参数验证**：使用 Bean Validation 进行请求参数校验
  - **Builder 模式**：所有 DTO 类支持 Builder 模式构建
- 🆔 **Product ID 管理**: 自动从URL提取商品ID，Discord通知包含完整商品信息

## 技术栈 🛠️

- **Java 1.8** - 核心开发语言
- **Spring Boot 2.7.18** - 应用框架
- **MyBatis Plus 3.5.3.1** - 数据库 ORM 框架
- **MySQL 8.0** - 数据库
- **Druid** - 数据库连接池
- **Selenium 4.15.0** - Web 自动化
- **Chrome Headless** - 浏览器引擎
- **JDA 5.0.0-beta.18** - Discord Bot API
- **Lombok 1.18.30** - 代码简化工具
- **Bean Validation** - 参数校验框架
- **Maven** - 项目构建工具
- **Docker** - 容器化部署

## AWS EC2 Docker 部署指南 🚀

### 前置要求

- AWS 账户
- 基本的 Linux 命令行知识
- Discord Bot Token 和 Webhook URL

### 第一步：创建 EC2 实例

1. **登录 AWS 控制台**，进入 EC2 服务

2. **启动新实例**：
   - **AMI**: Amazon Linux 2 或 Ubuntu 20.04 LTS
   - **实例类型**: t3.medium（推荐）或 t3.large（高负载）
   - **存储**: 30GB SSD
   - **安全组规则**:
     ```
     SSH (22)     - 您的IP地址
     HTTP (80)    - 0.0.0.0/0 (可选，用于健康检查)
     HTTPS (443)  - 0.0.0.0/0 (可选)
     Custom (8080) - 您的IP地址 (应用端口)
     ```

3. **下载密钥对**并保存到安全位置

### 第二步：连接到 EC2 实例

```bash
# 设置密钥权限
chmod 400 your-key.pem

# 连接到实例
ssh -i your-key.pem ec2-user@your-ec2-public-ip
# 或者 Ubuntu 系统使用：
# ssh -i your-key.pem ubuntu@your-ec2-public-ip
```

### 第三步：一键部署

在 EC2 实例上运行一键部署脚本：

```bash
# 下载项目
git clone https://github.com/your-username/pop-mart-watch.git
cd pop-mart-watch

# 运行一键部署脚本
chmod +x scripts/ec2-one-click-deploy.sh
./scripts/ec2-one-click-deploy.sh
```

**脚本将自动完成以下步骤**：
1. 🔧 **环境检测和设置** - 自动安装 Java、Maven、Docker、Chrome 等
2. 📁 **项目设置** - 创建必要目录和文件
3. ⚙️ **配置设置** - 交互式配置数据库和 Discord
4. 🔨 **构建应用** - 编译代码并构建 Docker 镜像
5. 🚀 **部署服务** - 启动 MySQL 和应用容器
6. 🔍 **验证部署** - 检查服务状态和健康状况

### 配置 Discord Bot

在部署过程中，脚本会提示您输入以下信息：

1. **创建 Discord 应用**：
   - 访问 [Discord Developer Portal](https://discord.com/developers/applications)
   - 创建新应用程序
   - 在 "Bot" 部分创建 Bot 并获取 Token

2. **设置 Bot 权限**：
   - 在 "OAuth2" → "URL Generator" 中选择：
     - Scopes: `bot`, `applications.commands`
     - Bot Permissions: `Send Messages`, `Use Slash Commands`

3. **邀请 Bot 到服务器**：
   - 使用生成的 URL 邀请 Bot 到您的 Discord 服务器

4. **创建 Webhook**：
   - 右键点击 Discord 频道 → 编辑频道
   - 整合 → Webhook → 创建 Webhook
   - 复制 Webhook URL

### 部署完成后测试

```bash
# 测试 Discord 通知
curl -X POST http://localhost:8080/api/monitor/test-discord

# 添加测试商品
curl -X POST http://localhost:8080/api/monitor/products \
  -H "Content-Type: application/json" \
  -d '{
    "url": "https://www.popmart.com/us/products/1739/THE-MONSTERS-Classic-Series-Sparkly-Plush-Pendant-Blind-Box",
    "productName": "THE MONSTERS Classic Series",
    "userId": "test-user"
  }'

# 查看监控统计
curl http://localhost:8080/api/monitor/stats
```

### 如果需要重新配置

如果需要修改配置，可以重新运行部署脚本：

```bash
./scripts/ec2-one-click-deploy.sh
```

脚本会检测到现有配置并询问是否要重新配置。

### 手动配置（高级用户）

如果您需要更精细的控制，也可以分步执行：

```bash
# 1. 环境设置
chmod +x scripts/ec2-docker-setup.sh
./scripts/ec2-docker-setup.sh

# 重新登录以应用 Docker 权限
exit
ssh -i your-key.pem ec2-user@your-ec2-public-ip
cd pop-mart-watch

# 2. 配置环境
chmod +x scripts/setup-docker-env.sh
./scripts/setup-docker-env.sh

# 3. 构建应用
chmod +x scripts/docker-build.sh
./scripts/docker-build.sh

# 4. 部署服务
chmod +x scripts/docker-deploy.sh
./scripts/docker-deploy.sh
```

**📋 完整脚本说明**：查看 [scripts/README.md](scripts/README.md) 了解所有脚本的详细用法。

## 管理和维护 🔧

### 服务管理

```bash
# 查看服务状态
docker-compose ps

# 启动服务
docker-compose up -d

# 停止服务
docker-compose down

# 重启应用
docker-compose restart app

# 查看日志
docker-compose logs -f app
docker-compose logs -f mysql

# 进入应用容器
docker-compose exec app bash

# 进入数据库容器
docker-compose exec mysql mysql -u root -p
```

### 应用更新

```bash
# 拉取最新代码
git pull origin main

# 方式1: 快速重新部署（推荐，适用于代码更新）
./scripts/quick-redeploy.sh

# 方式2: 完整重新部署（适用于配置变更）
./scripts/ec2-one-click-deploy.sh
```

### 数据库管理

```bash
# 备份数据库
./scripts/backup-database.sh

# 恢复数据库
./scripts/restore-database.sh backup_file.sql

# 查看数据库状态
docker-compose exec mysql mysql -u root -p -e "SHOW DATABASES;"
```

### 监控和日志

```bash
# 查看系统资源使用
docker stats

# 查看磁盘使用
df -h

# 查看应用日志
tail -f logs/pop-mart-watch.log

# 查看 Docker 日志
docker-compose logs --tail=100 app
```

### 故障排除

#### 1. 应用无法启动
```bash
# 检查配置文件
cat .env

# 检查 Docker 镜像
docker images | grep pop-mart-watch

# 查看详细错误日志
docker-compose logs app
```

#### 2. 数据库连接失败
```bash
# 检查 MySQL 容器状态
docker-compose ps mysql

# 测试数据库连接
docker-compose exec mysql mysql -u popmart -p popmart_watch

# 重启数据库
docker-compose restart mysql
```

#### 3. Discord Bot 无响应
```bash
# 检查 Bot Token 配置
grep DISCORD_BOT_TOKEN .env

# 查看 Discord 相关日志
docker-compose logs app | grep -i discord

# 测试 Discord 通知
curl -X POST http://localhost:8080/api/monitor/test-discord
```

#### 4. Chrome/Selenium 问题
```bash
# 检查 Chrome 是否正常运行
docker-compose exec app google-chrome --version

# 测试库存检测
curl -X POST http://localhost:8080/api/monitor/test \
  -H "Content-Type: application/json" \
  -d '{"url": "https://www.popmart.com/us/products/1739/"}'
```

### 性能优化

#### EC2 实例建议

| 用途 | 实例类型 | vCPU | 内存 | 存储 | 月费用(约) |
|------|----------|------|------|------|-----------|
| 测试环境 | t3.small | 2 | 2GB | 20GB | $15-20 |
| 生产环境 | t3.medium | 2 | 4GB | 30GB | $30-35 |
| 高负载 | t3.large | 2 | 8GB | 50GB | $60-70 |

#### 应用性能调优

```bash
# 调整 JVM 内存设置
# 编辑 .env 文件
JAVA_OPTS=-Xmx2g -Xms1g -XX:+UseG1GC

# 调整监控间隔
POPMART_MONITOR_POLL_INTERVAL=3

# 重启应用使配置生效
docker-compose restart app
```

## 安全建议 🔒

### 1. 网络安全
- 限制安全组规则，只允许必要的端口和IP
- 使用 HTTPS（可配置 Let's Encrypt）
- 定期更新系统和依赖

### 2. 数据安全
- 使用强密码
- 定期备份数据库
- 加密敏感配置信息

### 3. 访问控制
- 使用 IAM 角色而非根用户
- 定期轮换密钥和Token
- 监控访问日志

## 成本优化 💰

### 1. EC2 成本优化
- 使用预留实例或 Spot 实例
- 合理选择实例类型
- 设置自动停止策略

### 2. 存储优化
- 定期清理日志文件
- 使用 EBS 快照备份
- 监控存储使用量

### 3. 网络优化
- 使用 CloudFront CDN（如需要）
- 优化数据传输
- 监控带宽使用

## API 接口文档 📡

### 统一响应格式

所有 API 接口都使用统一的响应格式：

```json
{
  "code": 200,
  "message": "success",
  "data": { ... },
  "timestamp": 1640995200000
}
```

### 基础接口

#### 健康检查
```bash
GET /api/monitor/health
```

#### 监控统计
```bash
GET /api/monitor/stats
```

#### 获取所有监控商品
```bash
GET /api/monitor/products
```

### 商品管理

#### 添加监控商品
```bash
POST /api/monitor/products
Content-Type: application/json

{
  "url": "https://www.popmart.com/us/products/1739/",
  "productName": "Molly Space Series",
  "userId": "discord_user_123"
}
```

#### 删除监控商品
```bash
DELETE /api/monitor/products/{productId}?userId=discord_user_123
```

#### 手动检查商品库存
```bash
POST /api/monitor/products/{productId}/check?userId=discord_user_123
```

### 测试端点

#### 测试商品库存检测
```bash
POST /api/monitor/test
Content-Type: application/json

{
  "url": "https://www.popmart.com/us/products/1739/"
}
```

#### 测试 Discord 通知功能
```bash
POST /api/monitor/test-discord
```

## Discord Bot 使用指南 🤖

### 可用命令

- `/monitor-add <url> [name]` - 添加商品到监控列表
- `/monitor-remove <url>` - 从监控列表移除商品
- `/monitor-status` - 查看你的监控商品状态
- `/monitor-test <url>` - 手动测试商品链接
- `/monitor-stats` - 查看系统监控统计

### Discord 通知功能

系统支持通过 Discord Webhook 发送精美的库存提醒通知，包含：

- 🎉 **标题**: Pop Mart 库存提醒
- 📦 **商品名称**: 监控商品的名称
- 🆔 **商品 ID**: 从URL自动提取的商品ID（如 `1739`）
- 🔗 **商品链接**: 可点击的商品页面链接
- 🟢 **库存状态**: 现货状态
- ⏰ **检测时间**: 最后检测时间
- 👤 **监控用户**: 添加监控的用户
- 🖼️ **缩略图**: Pop Mart Logo

## 许可证 📄

本项目采用 MIT 许可证 - 查看 [LICENSE](LICENSE) 文件了解详情。

## 贡献 🤝

欢迎提交 Issue 和 Pull Request！

## 联系方式 📧

如有问题，请通过 GitHub Issues 联系。

## 最新更新 🆕

### v1.1.0 - DTO 重构与 Lombok 集成 (2024-01-01)

#### 🏗️ 架构优化
- **统一 DTO 管理**: 将所有请求/响应对象重构到 `com.popmart.dto` 包下
- **Lombok 集成**: 使用 `@Data`, `@Builder`, `@NoArgsConstructor`, `@AllArgsConstructor` 简化代码
- **Builder 模式**: 所有 DTO 类支持链式构建，提高代码可读性
- **参数验证**: 集成 Bean Validation，支持 `@NotBlank`, `@Pattern` 等注解

#### 📦 DTO 类结构
```
src/main/java/com/popmart/dto/
├── request/                    # 请求 DTO
│   ├── AddProductRequest.java  # 添加商品请求
│   └── TestProductRequest.java # 测试商品请求
└── response/                   # 响应 DTO
    ├── ApiResponse.java        # 统一响应包装器
    ├── MonitoringStats.java    # 监控统计响应
    ├── PerformanceTestResult.java # 性能测试结果
    ├── StockCheckResult.java   # 库存检查结果
    └── TestStockResponse.java  # 测试库存响应
```

#### 🔧 技术改进
- **类型安全**: 使用泛型 `ApiResponse<T>` 确保类型安全
- **代码简化**: Lombok 自动生成 getter/setter，减少样板代码
- **统一响应**: 所有 API 接口使用统一的响应格式
- **参数校验**: 自动验证请求参数，提供友好的错误信息

#### 📈 性能提升
- **编译优化**: Lombok 注解处理器配置，确保正确编译
- **内存优化**: Builder 模式减少对象创建开销
- **开发效率**: 减少 80% 的样板代码，提高开发效率

#### 🛠️ 开发体验
- **IDE 支持**: 完整的 IDE 自动补全和重构支持
- **代码质量**: 统一的代码风格和结构
- **维护性**: 更清晰的包结构，便于维护和扩展

#### 🔄 向后兼容
- **API 兼容**: 所有现有 API 接口保持兼容
- **配置兼容**: 现有配置文件无需修改
- **数据兼容**: 数据库结构保持不变

#### 📚 文档更新
- **README 更新**: 完整的 DTO 使用示例和 API 文档
- **代码示例**: 新增 Builder 模式使用示例
- **架构说明**: 详细的项目结构和设计模式说明

## 许可证 📄

本项目采用 MIT 许可证 - 查看 [LICENSE](LICENSE) 文件了解详情。

## 贡献 🤝

欢迎提交 Issue 和 Pull Request！

## 联系方式 📧

如有问题，请通过 GitHub Issues 联系。

## 配置详细说明 ⚙️

### 配置文件结构

```
src/main/resources/
├── application.yml                      # 主配置文件
├── application-local.yml.example       # 本地配置模板
├── application-local.yml               # 本地配置（需要创建）
├── application-production.yml.example  # 生产环境配置模板
├── application-production.yml          # 生产环境配置（需要创建）
└── application-docker.yml              # Docker 环境配置
```

### 主要配置项

| 配置项 | 描述 | 默认值 |
|--------|------|--------|
| `DB_HOST` | MySQL 主机地址 | localhost |
| `DB_PORT` | MySQL 端口 | 3306 |
| `DB_NAME` | 数据库名称 | popmart_watch |
| `DB_USERNAME` | 数据库用户名 | root |
| `DB_PASSWORD` | 数据库密码 | - |
| `DISCORD_BOT_TOKEN` | Discord Bot Token | - |
| `DISCORD_GUILD_ID` | Discord 服务器 ID | - |
| `DISCORD_WEBHOOK_URL` | Discord Webhook URL | - |
| `POPMART_MONITOR_POLL_INTERVAL` | 监控间隔(分钟) | 5 |
| `POPMART_MONITOR_NOTIFICATION_TYPE` | 通知类型 | log |

### 配置文件说明

| 配置文件 | 用途 | 通知类型 | 说明 |
|----------|------|----------|------|
| `application.yml` | 基础配置 | log | 默认配置，使用环境变量 |
| `application-local.yml` | 本地开发 | log | 开发环境，启用详细日志 |
| `application-production.yml` | 生产环境 | discord | 生产环境，使用 Discord 通知 |
| `application-docker.yml` | Docker 环境 | log | 容器化部署配置 |

## ⚡ 性能优化

### 🚀 优化成果
- **首次检测**：7.6秒（完整页面加载）
- **缓存后检测**：**13ms**（提升99.8%）
- **检测成功率**：100%
- **页面内容**：240K+字符（完整渲染）

### 🔧 核心优化技术
1. **智能页面加载等待**：确保页面完全渲染
2. **Pop Mart 特定检测算法**：针对性CSS选择器
3. **智能缓存机制**：缓存后检测仅需13ms
4. **多重检测策略**：多种备选检测方案
5. **性能配置优化**：精细化超时和等待设置

## 🎯 功能特性
 

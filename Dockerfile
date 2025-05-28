FROM eclipse-temurin:17-jre-alpine

# Install Chrome and dependencies
RUN apk update && apk upgrade && \
    # 安装基础依赖
    apk add --no-cache \
        chromium \
        chromium-chromedriver \
        nss \
        freetype \
        freetype-dev \
        harfbuzz \
        ca-certificates \
        # 字体支持
        ttf-freefont \
        ttf-dejavu \
        ttf-liberation \
        # 系统依赖
        dbus \
        fontconfig \
        eudev \
        xvfb \
        # 工具
        bash \
        curl \
        wget && \
    # 清理缓存
    rm -rf /var/cache/apk/* && \
    # 验证安装
    chromium-browser --version

# 设置环境变量
ENV CHROME_BIN=/usr/bin/chromium-browser \
    CHROME_PATH=/usr/lib/chromium/ \
    CHROMIUM_FLAGS="--no-sandbox --headless --disable-gpu --disable-software-rasterizer --disable-dev-shm-usage"

# 创建并设置权限
RUN mkdir -p /var/cache/chromium /app/logs && \
    chmod -R 777 /var/cache/chromium /app/logs && \
    # 设置chromium运行权限
    chown -R nobody:nobody /var/cache/chromium

# Create app directory and set workdir
WORKDIR /app

# Copy the jar file
COPY target/pop-mart-watch-1.0.0.jar app.jar

# Expose port
EXPOSE 8080

# Health check
HEALTHCHECK --interval=30s --timeout=10s --start-period=60s --retries=3 \
  CMD wget --no-verbose --tries=1 --spider http://localhost:8080/api/monitor/health || exit 1

# Run as non-root user
USER nobody

# Run the application
ENTRYPOINT ["java", "-jar", "app.jar"] 
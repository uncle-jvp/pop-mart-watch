FROM eclipse-temurin:17-jre-alpine

# Install Chrome and dependencies
RUN apk update && apk upgrade && \
    apk add --no-cache \
    chromium \
    chromium-chromedriver \
    nss \
    freetype \
    freetype-dev \
    harfbuzz \
    ca-certificates \
    ttf-freefont \
    # 添加必要的系统依赖
    dbus \
    fontconfig \
    udev \
    xvfb \
    # 添加字体支持
    ttf-dejavu \
    ttf-liberation \
    ttf-ubuntu-font-family \
    # 添加其他必要的工具
    bash \
    curl \
    wget

# 设置环境变量
ENV CHROME_BIN=/usr/bin/chromium-browser \
    CHROME_PATH=/usr/lib/chromium/ \
    CHROMIUM_FLAGS="--no-sandbox --headless --disable-gpu --disable-software-rasterizer --disable-dev-shm-usage"

# 创建必要的目录
RUN mkdir -p /var/cache/chromium && \
    chmod 777 /var/cache/chromium

# Create app directory
WORKDIR /app

# Copy the jar file
COPY target/pop-mart-watch-1.0.0.jar app.jar

# Create logs directory
RUN mkdir -p /app/logs && \
    chmod 777 /app/logs

# Expose port
EXPOSE 8080

# Health check
HEALTHCHECK --interval=30s --timeout=10s --start-period=60s --retries=3 \
  CMD wget --no-verbose --tries=1 --spider http://localhost:8080/api/monitor/health || exit 1

# Run the application
ENTRYPOINT ["java", "-jar", "app.jar"] 
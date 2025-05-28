#!/bin/bash

# Pop Mart 监控系统 - 优化后性能测试脚本
# 测试轻量化Chrome、智能等待、WebDriver池等优化效果

set -e

echo "🚀 Pop Mart 监控系统 - 优化后性能测试"
echo "========================================"

# 配置
BASE_URL="http://localhost:8080"
TEST_URL="https://www.popmart.com/us/products/1739/"
TEST_ITERATIONS=5

# 颜色定义
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

# 跨平台时间戳函数
get_timestamp_ms() {
    if [[ "$OSTYPE" == "darwin"* ]]; then
        # macOS
        python3 -c "import time; print(int(time.time() * 1000))"
    else
        # Linux
        date +%s%3N
    fi
}

# 检查应用是否运行
check_app_status() {
    echo -n "检查应用状态... "
    if curl -s "${BASE_URL}/actuator/health" > /dev/null 2>&1; then
        echo -e "${GREEN}✅ 应用运行中${NC}"
    else
        echo -e "${RED}❌ 应用未运行，请先启动应用${NC}"
        exit 1
    fi
}

# 测试单次检测性能
test_single_check() {
    echo -e "\n${BLUE}📊 单次检测性能测试${NC}"
    echo "================================"
    
    echo "测试URL: $TEST_URL"
    echo "开始检测..."
    
    # 使用兼容的时间戳格式
    start_time=$(get_timestamp_ms)
    
    response=$(curl -s -X POST "${BASE_URL}/api/monitor/test" \
        -H "Content-Type: application/json" \
        -d "{\"url\": \"${TEST_URL}\"}")
    
    end_time=$(get_timestamp_ms)
    duration=$((end_time - start_time))
    
    echo "API响应时间: ${duration}ms"
    echo "响应内容: $response"
    
    # 解析响应时间
    if echo "$response" | grep -q "responseTime"; then
        internal_time=$(echo "$response" | grep -o '"responseTime":[0-9]*' | cut -d':' -f2)
        echo -e "${GREEN}✅ 内部检测时间: ${internal_time}ms${NC}"
        
        if [ "$internal_time" -lt 3000 ]; then
            echo -e "${GREEN}🎉 性能优秀！检测时间 < 3秒${NC}"
        elif [ "$internal_time" -lt 5000 ]; then
            echo -e "${YELLOW}⚠️  性能良好，检测时间 < 5秒${NC}"
        else
            echo -e "${RED}⚠️  性能需要改进，检测时间 > 5秒${NC}"
        fi
    fi
}

# 测试并发性能
test_concurrent_performance() {
    echo -e "\n${BLUE}🔄 并发性能测试${NC}"
    echo "================================"
    
    echo "启动 3 个并发检测..."
    
    # 创建临时文件存储结果
    temp_dir=$(mktemp -d)
    
    # 并发启动3个检测
    for i in {1..3}; do
        (
            start_time=$(get_timestamp_ms)
            response=$(curl -s -X POST "${BASE_URL}/api/monitor/test" \
                -H "Content-Type: application/json" \
                -d "{\"url\": \"${TEST_URL}\"}")
            end_time=$(get_timestamp_ms)
            duration=$((end_time - start_time))
            
            echo "Thread $i: ${duration}ms" > "${temp_dir}/result_$i.txt"
            
            if echo "$response" | grep -q "responseTime"; then
                internal_time=$(echo "$response" | grep -o '"responseTime":[0-9]*' | cut -d':' -f2)
                echo "Internal $i: ${internal_time}ms" >> "${temp_dir}/result_$i.txt"
            fi
        ) &
    done
    
    # 等待所有任务完成
    wait
    
    echo "并发测试结果:"
    for i in {1..3}; do
        if [ -f "${temp_dir}/result_$i.txt" ]; then
            echo "  $(cat "${temp_dir}/result_$i.txt")"
        fi
    done
    
    # 清理临时文件
    rm -rf "$temp_dir"
}

# 测试缓存效果
test_cache_performance() {
    echo -e "\n${BLUE}💾 缓存性能测试${NC}"
    echo "================================"
    
    echo "第一次检测（冷启动）..."
    start_time=$(get_timestamp_ms)
    response1=$(curl -s -X POST "${BASE_URL}/api/monitor/test" \
        -H "Content-Type: application/json" \
        -d "{\"url\": \"${TEST_URL}\"}")
    end_time=$(get_timestamp_ms)
    first_duration=$((end_time - start_time))
    
    if echo "$response1" | grep -q "responseTime"; then
        first_internal=$(echo "$response1" | grep -o '"responseTime":[0-9]*' | cut -d':' -f2)
        echo -e "第一次检测: API=${first_duration}ms, 内部=${first_internal}ms"
    fi
    
    echo "等待 2 秒后进行第二次检测（缓存测试）..."
    sleep 2
    
    start_time=$(get_timestamp_ms)
    response2=$(curl -s -X POST "${BASE_URL}/api/monitor/test" \
        -H "Content-Type: application/json" \
        -d "{\"url\": \"${TEST_URL}\"}")
    end_time=$(get_timestamp_ms)
    second_duration=$((end_time - start_time))
    
    if echo "$response2" | grep -q "responseTime"; then
        second_internal=$(echo "$response2" | grep -o '"responseTime":[0-9]*' | cut -d':' -f2)
        echo -e "第二次检测: API=${second_duration}ms, 内部=${second_internal}ms"
        
        # 计算性能提升
        if [ "$first_internal" -gt 0 ] && [ "$second_internal" -gt 0 ]; then
            improvement=$((100 - (second_internal * 100 / first_internal)))
            echo -e "${GREEN}🚀 缓存性能提升: ${improvement}%${NC}"
            
            if [ "$improvement" -gt 90 ]; then
                echo -e "${GREEN}🎉 缓存效果极佳！性能提升 > 90%${NC}"
            elif [ "$improvement" -gt 50 ]; then
                echo -e "${GREEN}✅ 缓存效果良好，性能提升 > 50%${NC}"
            else
                echo -e "${YELLOW}⚠️  缓存效果一般，性能提升 < 50%${NC}"
            fi
        fi
    fi
}

# 测试WebDriver池效果
test_driver_pool() {
    echo -e "\n${BLUE}🏊 WebDriver池测试${NC}"
    echo "================================"
    
    echo "快速连续发送 5 个请求测试池效果..."
    
    total_time=0
    for i in {1..5}; do
        echo -n "请求 $i: "
        start_time=$(get_timestamp_ms)
        response=$(curl -s -X POST "${BASE_URL}/api/monitor/test" \
            -H "Content-Type: application/json" \
            -d "{\"url\": \"${TEST_URL}\"}")
        end_time=$(get_timestamp_ms)
        duration=$((end_time - start_time))
        total_time=$((total_time + duration))
        
        if echo "$response" | grep -q "responseTime"; then
            internal_time=$(echo "$response" | grep -o '"responseTime":[0-9]*' | cut -d':' -f2)
            echo "${duration}ms (内部: ${internal_time}ms)"
        else
            echo "${duration}ms"
        fi
        
        # 短暂间隔
        sleep 0.5
    done
    
    avg_time=$((total_time / 5))
    echo -e "${GREEN}平均响应时间: ${avg_time}ms${NC}"
}

# 系统资源监控
monitor_resources() {
    echo -e "\n${BLUE}📈 系统资源监控${NC}"
    echo "================================"
    
    # 检查Java进程
    java_pid=$(pgrep -f "pop-mart-watch" | head -1)
    if [ -n "$java_pid" ]; then
        echo "Java进程 PID: $java_pid"
        
        # 内存使用
        if command -v ps > /dev/null; then
            memory_usage=$(ps -p "$java_pid" -o rss= 2>/dev/null || echo "0")
            memory_mb=$((memory_usage / 1024))
            echo "内存使用: ${memory_mb}MB"
        fi
        
        # CPU使用（如果有top命令）
        if command -v top > /dev/null; then
            cpu_usage=$(top -p "$java_pid" -n 1 -b 2>/dev/null | tail -1 | awk '{print $9}' || echo "N/A")
            echo "CPU使用: ${cpu_usage}%"
        fi
    else
        echo "未找到Java进程"
    fi
    
    # 系统负载
    if [ -f /proc/loadavg ]; then
        load_avg=$(cat /proc/loadavg | cut -d' ' -f1)
        echo "系统负载: $load_avg"
    fi
}

# 生成性能报告
generate_report() {
    echo -e "\n${BLUE}📋 性能优化总结${NC}"
    echo "================================"
    
    echo "✅ 已实施的优化措施:"
    echo "  • 新版 Headless Chrome (--headless=new)"
    echo "  • 禁用图片加载 (--blink-settings=imagesEnabled=false)"
    echo "  • 轻量化浏览器参数 (20+ 优化参数)"
    echo "  • 智能等待策略 (只等关键元素)"
    echo "  • WebDriver 连接池 (最多3个实例)"
    echo "  • 智能缓存机制 (5秒缓存)"
    echo "  • 并发检测支持"
    echo "  • 预编译CSS选择器"
    
    echo -e "\n🎯 性能目标:"
    echo "  • 首次检测: < 5秒"
    echo "  • 缓存检测: < 100ms"
    echo "  • 并发支持: 3个同时检测"
    echo "  • 内存使用: < 500MB"
    
    echo -e "\n📊 建议监控指标:"
    echo "  • 检测响应时间"
    echo "  • 缓存命中率"
    echo "  • WebDriver池使用率"
    echo "  • 系统资源消耗"
}

# 主函数
main() {
    check_app_status
    test_single_check
    test_concurrent_performance
    test_cache_performance
    test_driver_pool
    monitor_resources
    generate_report
    
    echo -e "\n${GREEN}🎉 性能测试完成！${NC}"
    echo "如需更详细的性能分析，请查看应用日志："
    echo "  tail -f logs/pop-mart-watch.log"
}

# 运行主函数
main "$@" 
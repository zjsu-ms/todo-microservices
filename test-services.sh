#!/bin/bash

# 颜色输出
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

# 打印分隔符
print_separator() {
    echo -e "${BLUE}========================================${NC}"
}

# 打印成功消息
print_success() {
    echo -e "${GREEN}✓ $1${NC}"
}

# 打印错误消息
print_error() {
    echo -e "${RED}✗ $1${NC}"
}

# 打印警告消息
print_warning() {
    echo -e "${YELLOW}⚠ $1${NC}"
}

# 打印标题
print_title() {
    echo -e "\n${BLUE}=== $1 ===${NC}\n"
}

# 检查服务是否运行
check_service() {
    local service_name=$1
    local port=$2

    if curl -s http://localhost:$port/actuator/health > /dev/null 2>&1 || \
       curl -s http://localhost:$port/api/users > /dev/null 2>&1 || \
       curl -s http://localhost:$port/api/todos > /dev/null 2>&1; then
        print_success "$service_name is running on port $port"
        return 0
    else
        print_error "$service_name is NOT running on port $port"
        return 1
    fi
}

# 检查Nacos
check_nacos() {
    if curl -s http://localhost:8848/nacos/ > /dev/null 2>&1; then
        print_success "Nacos is running on port 8848"
        return 0
    else
        print_error "Nacos is NOT running on port 8848"
        return 1
    fi
}

print_title "Todo 微服务项目测试 (v1.2.0)"
echo "测试特性: OpenFeign + LoadBalancer + Resilience4j"
print_separator

# 1. 检查所有服务状态
print_title "1. 检查服务状态"
check_nacos
check_service "user-service" 8081
check_service "todo-service" 8082

# 2. 测试用户服务API
print_title "2. 测试用户服务 API"

echo "2.1 创建用户"
USER_RESPONSE=$(curl -s -X POST http://localhost:8081/api/users \
  -H "Content-Type: application/json" \
  -d '{"username":"张三","email":"zhangsan@example.com"}')
echo $USER_RESPONSE | jq '.'

# 提取用户ID
USER_ID=$(echo $USER_RESPONSE | jq -r '.data.id // .id')
if [ -z "$USER_ID" ] || [ "$USER_ID" = "null" ]; then
    # 尝试使用已存在的用户
    USER_ID=1
    print_warning "无法创建新用户，使用默认用户ID: $USER_ID"
else
    print_success "创建用户成功，ID: $USER_ID"
fi

echo -e "\n2.2 获取所有用户"
curl -s http://localhost:8081/api/users | jq '.'

echo -e "\n2.3 获取单个用户 (ID: $USER_ID)"
curl -s http://localhost:8081/api/users/$USER_ID | jq '.'

# 3. 测试Todo服务API
print_title "3. 测试 Todo 服务 API"

echo "3.1 获取所有Todo"
curl -s http://localhost:8082/api/todos | jq '.'

echo -e "\n3.2 创建Todo（关联到用户 $USER_ID）"
TODO_RESPONSE=$(curl -s -X POST http://localhost:8082/api/todos \
  -H "Content-Type: application/json" \
  -d "{\"title\":\"学习OpenFeign\",\"description\":\"掌握声明式客户端\",\"userId\":$USER_ID}")
echo $TODO_RESPONSE | jq '.'

TODO_ID=$(echo $TODO_RESPONSE | jq -r '.data.id // .id')
if [ -n "$TODO_ID" ] && [ "$TODO_ID" != "null" ]; then
    print_success "创建Todo成功，ID: $TODO_ID"
else
    print_warning "无法获取Todo ID"
    TODO_ID=1
fi

echo -e "\n3.3 获取单个Todo (ID: $TODO_ID)"
curl -s http://localhost:8082/api/todos/$TODO_ID | jq '.'

echo -e "\n3.4 切换完成状态"
curl -s -X PATCH http://localhost:8082/api/todos/$TODO_ID/toggle | jq '.'

# 4. 测试OpenFeign服务间通信
print_title "4. 测试 OpenFeign 服务间通信"

echo "4.1 创建Todo（验证用户存在性 - 成功场景）"
curl -s -X POST http://localhost:8082/api/todos \
  -H "Content-Type: application/json" \
  -d "{\"title\":\"测试OpenFeign\",\"description\":\"验证服务间调用\",\"userId\":$USER_ID}" | jq '.'

echo -e "\n4.2 创建Todo（用户不存在 - 失败场景）"
curl -s -X POST http://localhost:8082/api/todos \
  -H "Content-Type: application/json" \
  -d '{"title":"测试失败场景","description":"用户ID 999不存在","userId":999}' | jq '.'

# 5. 测试Nacos服务发现
print_title "5. 测试 Nacos 服务发现"

echo "5.1 查询已注册的服务"
SERVICES=$(curl -s "http://localhost:8848/nacos/v1/ns/service/list?pageNo=1&pageSize=10")
echo $SERVICES | jq '.'

USER_SERVICE_COUNT=$(echo $SERVICES | jq -r '.doms[]' | grep -c "user-service" || echo "0")
TODO_SERVICE_COUNT=$(echo $SERVICES | jq -r '.doms[]' | grep -c "todo-service" || echo "0")

if [ "$USER_SERVICE_COUNT" -gt 0 ]; then
    print_success "user-service 已注册到 Nacos"
else
    print_error "user-service 未注册到 Nacos"
fi

if [ "$TODO_SERVICE_COUNT" -gt 0 ]; then
    print_success "todo-service 已注册到 Nacos"
else
    print_error "todo-service 未注册到 Nacos"
fi

# 6. 测试熔断器（Resilience4j Circuit Breaker）
print_title "6. 测试 Resilience4j 熔断器"

echo "6.1 正常情况下创建Todo"
curl -s -X POST http://localhost:8082/api/todos \
  -H "Content-Type: application/json" \
  -d "{\"title\":\"熔断器测试\",\"description\":\"正常调用\",\"userId\":$USER_ID}" | jq '.'

echo -e "\n6.2 停止 user-service 测试熔断"
print_warning "即将停止 user-service 容器..."
docker stop user-service > /dev/null 2>&1

sleep 2

echo -e "\n6.3 尝试创建Todo（应该触发重试和熔断）"
print_warning "注意：这个请求会重试3次，每次间隔逐渐增加（指数退避）"
START_TIME=$(date +%s)
FALLBACK_RESPONSE=$(curl -s -X POST http://localhost:8082/api/todos \
  -H "Content-Type: application/json" \
  -d '{"title":"熔断器测试","description":"user-service不可用","userId":2}')
END_TIME=$(date +%s)
DURATION=$((END_TIME - START_TIME))

echo $FALLBACK_RESPONSE | jq '.'
print_warning "请求耗时: ${DURATION}秒 (包含重试时间)"

if echo $FALLBACK_RESPONSE | grep -q "timed out\|Connect timed out\|Connection refused"; then
    print_success "熔断器和重试机制生效（请求超时）"
else
    print_warning "熔断响应异常，请检查日志"
fi

echo -e "\n6.4 查看 todo-service 日志（最后20行）"
print_warning "查找降级和重试相关日志..."
docker logs todo-service 2>&1 | grep -E "fallback|retry|circuit|Failed to verify user" | tail -20

echo -e "\n6.5 重启 user-service"
print_warning "正在重启 user-service..."
docker start user-service > /dev/null 2>&1
sleep 5

echo -e "\n6.6 验证服务恢复"
curl -s -X POST http://localhost:8082/api/todos \
  -H "Content-Type: application/json" \
  -d "{\"title\":\"恢复测试\",\"description\":\"服务已恢复\",\"userId\":$USER_ID}" | jq '.'

# 7. 性能测试（简单负载）
print_title "7. 简单负载测试"

echo "7.1 连续创建10个Todo（测试LoadBalancer）"
for i in {1..10}; do
    RESPONSE=$(curl -s -X POST http://localhost:8082/api/todos \
      -H "Content-Type: application/json" \
      -d "{\"title\":\"负载测试 $i\",\"description\":\"第${i}个请求\",\"userId\":$USER_ID}")
    STATUS=$(echo $RESPONSE | jq -r '.code // .status')
    if [ "$STATUS" = "201" ] || [ "$STATUS" = "200" ]; then
        echo -n "."
    else
        echo -n "x"
    fi
done
echo ""
print_success "负载测试完成"

# 8. 最终状态检查
print_title "8. 最终状态检查"

echo "8.1 统计Todo总数"
TODO_COUNT=$(curl -s http://localhost:8082/api/todos | jq '.data | length')
print_success "当前Todo总数: $TODO_COUNT"

echo -e "\n8.2 统计用户总数"
USER_COUNT=$(curl -s http://localhost:8081/api/users | jq '.data | length')
print_success "当前用户总数: $USER_COUNT"

echo -e "\n8.3 按用户查询Todo"
curl -s "http://localhost:8082/api/todos?userId=$USER_ID" | jq '.'

# 总结
print_separator
print_title "测试完成总结"
echo "✓ 基本API测试完成"
echo "✓ OpenFeign服务间通信测试完成"
echo "✓ Nacos服务发现验证完成"
echo "✓ Resilience4j熔断器测试完成"
echo "✓ 重试机制测试完成"
echo "✓ 负载测试完成"
print_separator

echo -e "\n提示: 可以访问 http://localhost:8080 查看Nacos控制台"
echo "      账号: nacos / nacos"

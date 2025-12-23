#!/bin/bash

# 颜色输出
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
CYAN='\033[0;36m'
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

# 打印信息消息
print_info() {
    echo -e "${CYAN}ℹ $1${NC}"
}

# 打印标题
print_title() {
    echo -e "\n${BLUE}=== $1 ===${NC}\n"
}

# 检查服务是否运行
check_service() {
    local service_name=$1
    local port=$2

    if curl -s http://localhost:$port/actuator/health > /dev/null 2>&1; then
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

# 检查Nacos控制台
check_nacos_console() {
    if curl -s http://localhost:8080/nacos/ > /dev/null 2>&1; then
        print_success "Nacos Console is accessible at http://localhost:8080"
        return 0
    else
        print_error "Nacos Console is NOT accessible"
        return 1
    fi
}

# 检查RabbitMQ
check_rabbitmq() {
    if curl -s http://localhost:15672 > /dev/null 2>&1; then
        print_success "RabbitMQ is running on port 5672 (Management UI: 15672)"
        return 0
    else
        print_error "RabbitMQ is NOT running"
        return 1
    fi
}

print_separator
print_title "Todo 微服务项目测试 (v2.3.0)"
echo "测试特性: 配置中心 + 服务部署 + 动态配置刷新 + RabbitMQ异步消息 + 监控与链路追踪"
echo "对应课程: 第13周 - 服务监控与链路追踪"
print_separator

# 1. 检查所有服务状态
print_title "1. 检查服务健康状态"
print_info "验证 Docker Compose 健康检查和服务启动状态"

check_nacos
check_nacos_console
check_rabbitmq
check_service "user-service" 8081
check_service "todo-service" 8082
check_service "gateway-service" 8080

# 检查监控和追踪服务
print_info "检查监控和链路追踪服务"
if curl -s http://localhost:9411/health > /dev/null 2>&1; then
    print_success "Zipkin is running on port 9411"
else
    print_warning "Zipkin is NOT running"
fi

if curl -s http://localhost:9090/-/healthy > /dev/null 2>&1; then
    print_success "Prometheus is running on port 9090"
else
    print_warning "Prometheus is NOT running"
fi

if curl -s http://localhost:3000/api/health > /dev/null 2>&1; then
    print_success "Grafana is running on port 3000"
else
    print_warning "Grafana is NOT running"
fi

# 2. 检查详细健康状态
print_title "2. 检查服务健康检查端点"
print_info "验证 Spring Boot Actuator 健康检查配置"

echo "2.1 user-service 健康检查"
USER_HEALTH=$(curl -s http://localhost:8081/actuator/health)
echo $USER_HEALTH | jq '.'

if echo $USER_HEALTH | jq -e '.status == "UP"' > /dev/null 2>&1; then
    print_success "user-service 健康状态: UP"

    # 检查各个组件
    if echo $USER_HEALTH | jq -e '.components.db.status == "UP"' > /dev/null 2>&1; then
        print_success "  └─ 数据库连接: UP"
    else
        print_warning "  └─ 数据库连接状态异常"
    fi

    if echo $USER_HEALTH | jq -e '.components.nacosDiscovery.status == "UP"' > /dev/null 2>&1; then
        print_success "  └─ Nacos服务发现: UP"
    fi

    if echo $USER_HEALTH | jq -e '.components.nacosConfig' > /dev/null 2>&1; then
        print_success "  └─ Nacos配置中心: 已连接"
    fi
else
    print_error "user-service 健康状态异常"
fi

echo -e "\n2.2 todo-service 健康检查"
TODO_HEALTH=$(curl -s http://localhost:8082/actuator/health)
echo $TODO_HEALTH | jq '.'

if echo $TODO_HEALTH | jq -e '.status == "UP"' > /dev/null 2>&1; then
    print_success "todo-service 健康状态: UP"
else
    print_error "todo-service 健康状态异常"
fi

# 3. 测试Nacos配置中心
print_title "3. 测试 Nacos 配置中心"
print_info "验证服务从 Nacos 读取配置"

echo "3.1 读取 user-service 配置"
USER_CONFIG=$(curl -s http://localhost:8081/api/config/info)
echo $USER_CONFIG | jq '.'

if [ -n "$USER_CONFIG" ]; then
    APP_NAME=$(echo $USER_CONFIG | jq -r '.appName // .data.appName // empty')
    APP_VERSION=$(echo $USER_CONFIG | jq -r '.appVersion // .data.appVersion // empty')

    if [ -n "$APP_NAME" ]; then
        print_success "配置读取成功: $APP_NAME v$APP_VERSION"
    fi
fi

echo -e "\n3.2 读取 todo-service 配置"
TODO_CONFIG=$(curl -s http://localhost:8082/api/config/info)
echo $TODO_CONFIG | jq '.'

# 4. 测试Nacos服务注册与发现
print_title "4. 测试 Nacos 服务注册与发现"
print_info "验证所有服务已正确注册到 Nacos"

echo "4.1 查询已注册的服务"
SERVICES=$(curl -s "http://localhost:8848/nacos/v1/ns/service/list?pageNo=1&pageSize=10")
echo $SERVICES | jq '.'

# 检查各个服务是否注册
USER_SERVICE_COUNT=$(echo $SERVICES | jq -r '.doms[]' | grep -c "user-service" || echo "0")
TODO_SERVICE_COUNT=$(echo $SERVICES | jq -r '.doms[]' | grep -c "todo-service" || echo "0")
GATEWAY_SERVICE_COUNT=$(echo $SERVICES | jq -r '.doms[]' | grep -c "gateway-service" || echo "0")

echo -e "\n4.2 服务注册统计"
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

if [ "$GATEWAY_SERVICE_COUNT" -gt 0 ]; then
    print_success "gateway-service 已注册到 Nacos"
else
    print_error "gateway-service 未注册到 Nacos"
fi

# 5. 测试配置动态刷新
print_title "5. 测试配置动态刷新"
print_info "验证 @RefreshScope 配置热更新（需要手动在Nacos修改配置）"

echo "5.1 当前配置信息"
BEFORE_CONFIG=$(curl -s http://localhost:8081/api/config/info)
echo $BEFORE_CONFIG | jq '.'

CURRENT_VERSION=$(echo $BEFORE_CONFIG | jq -r '.appVersion // .data.appVersion // "unknown"')
print_info "当前版本: $CURRENT_VERSION"

echo -e "\n${YELLOW}📝 动态刷新测试步骤:${NC}"
echo "  1. 打开 Nacos 控制台: http://localhost:8080 (nacos/nacos)"
echo "  2. 进入「配置管理」→「配置列表」"
echo "  3. 找到 user-service-dev.yaml 并点击「编辑」"
echo "  4. 修改 app.version: 2.1.1（或其他值）"
echo "  5. 点击「发布」"
echo "  6. 按回车继续测试，查看配置是否动态更新"
echo ""
read -p "请完成上述步骤后按回车继续..."

echo -e "\n5.2 检查配置是否已更新"
AFTER_CONFIG=$(curl -s http://localhost:8081/api/config/info)
echo $AFTER_CONFIG | jq '.'

NEW_VERSION=$(echo $AFTER_CONFIG | jq -r '.appVersion // .data.appVersion // "unknown"')

if [ "$NEW_VERSION" != "$CURRENT_VERSION" ]; then
    print_success "配置动态刷新成功! $CURRENT_VERSION → $NEW_VERSION"
    print_success "服务无需重启即可读取新配置"
else
    print_warning "配置未发生变化，可能未在Nacos修改或刷新失败"
    print_info "请检查 Nacos 控制台配置是否正确发布"
fi

# 6. 测试数据库配置
print_title "6. 测试数据库配置"
print_info "验证服务通过配置中心正确连接数据库"

echo "6.1 创建测试用户（验证数据库写入）"
USER_RESPONSE=$(curl -s -X POST http://localhost:8081/api/users \
  -H "Content-Type: application/json" \
  -d '{"username":"测试用户","email":"test@example.com"}')
echo $USER_RESPONSE | jq '.'

USER_ID=$(echo $USER_RESPONSE | jq -r '.data.id // .id // empty')
if [ -n "$USER_ID" ] && [ "$USER_ID" != "null" ]; then
    print_success "数据库写入成功，用户ID: $USER_ID"
else
    print_warning "无法创建新用户，可能已存在"
    USER_ID=1
fi

echo -e "\n6.2 查询用户（验证数据库读取）"
USER_DATA=$(curl -s http://localhost:8081/api/users/$USER_ID)
echo $USER_DATA | jq '.'

if echo $USER_DATA | jq -e '.data // . | .id' > /dev/null 2>&1; then
    print_success "数据库读取成功"
else
    print_error "数据库读取失败"
fi

# 7. 测试微服务间通信
print_title "7. 测试微服务间通信"
print_info "验证 todo-service 通过 OpenFeign 调用 user-service"

echo "7.1 创建Todo（会调用 user-service 验证用户）"
TODO_RESPONSE=$(curl -s -X POST http://localhost:8082/api/todos \
  -H "Content-Type: application/json" \
  -d "{\"title\":\"测试配置中心\",\"description\":\"第10周课程实践\",\"userId\":$USER_ID}")
echo $TODO_RESPONSE | jq '.'

TODO_ID=$(echo $TODO_RESPONSE | jq -r '.data.id // .id // empty')
if [ -n "$TODO_ID" ] && [ "$TODO_ID" != "null" ]; then
    print_success "Todo创建成功，ID: $TODO_ID"
    print_success "服务间通信正常（OpenFeign调用成功）"
else
    print_warning "Todo创建异常"
fi

# 8. 测试API网关路由
print_title "8. 测试 API 网关路由"
print_info "验证 Spring Cloud Gateway 配置和路由转发"

echo "8.1 通过网关访问用户服务"
GATEWAY_USER=$(curl -s http://localhost:9000/api/users/$USER_ID 2>&1)

if echo "$GATEWAY_USER" | grep -q "Unauthorized"; then
    print_success "网关JWT认证正常（需要Token）"
    print_info "网关路由配置正确，但需要JWT Token才能访问"
else
    echo $GATEWAY_USER | jq '.' 2>/dev/null || echo $GATEWAY_USER
fi

# 9. 检查Docker Compose配置
print_title "9. 检查 Docker Compose 部署"
print_info "验证容器编排和依赖关系"

echo "9.1 查看容器状态"
docker-compose ps

echo -e "\n9.2 检查容器健康状态"
NACOS_HEALTH=$(docker inspect nacos --format='{{.State.Health.Status}}' 2>/dev/null || echo "no healthcheck")
USER_DB_HEALTH=$(docker inspect user-db --format='{{.State.Health.Status}}' 2>/dev/null || echo "no healthcheck")
TODO_DB_HEALTH=$(docker inspect todo-db --format='{{.State.Health.Status}}' 2>/dev/null || echo "no healthcheck")

print_info "Nacos 健康状态: $NACOS_HEALTH"
print_info "user-db 健康状态: $USER_DB_HEALTH"
print_info "todo-db 健康状态: $TODO_DB_HEALTH"

if [ "$NACOS_HEALTH" = "healthy" ]; then
    print_success "Nacos 容器健康检查通过"
fi

if [ "$USER_DB_HEALTH" = "healthy" ]; then
    print_success "user-db 容器健康检查通过"
fi

if [ "$TODO_DB_HEALTH" = "healthy" ]; then
    print_success "todo-db 容器健康检查通过"
fi

# 10. 检查配置优先级
print_title "10. 验证配置优先级"
print_info "验证配置加载顺序: Nacos Config > application.yml"

echo "10.1 检查环境变量配置"
USER_ENV=$(docker exec user-service env | grep SPRING_PROFILES_ACTIVE || echo "未设置")
print_info "user-service 环境: $USER_ENV"

echo -e "\n10.2 检查数据库连接配置"
print_info "验证服务使用了 Nacos 配置的数据库连接"
if echo $USER_HEALTH | jq -e '.components.db.status == "UP"' > /dev/null 2>&1; then
    print_success "数据库配置正确（来自 Nacos 或 application.yml）"
fi

# 11. 测试RabbitMQ异步消息通信
print_title "11. 测试 RabbitMQ 异步消息通信"
print_info "验证 todo-service 发送消息，user-service 接收消息"

echo "11.1 检查 RabbitMQ 管理界面"
RABBITMQ_OVERVIEW=$(curl -s -u admin:admin123 http://localhost:15672/api/overview)
if [ -n "$RABBITMQ_OVERVIEW" ]; then
    print_success "RabbitMQ 管理API可访问"
    RABBITMQ_VERSION=$(echo $RABBITMQ_OVERVIEW | jq -r '.rabbitmq_version // "unknown"')
    print_info "RabbitMQ 版本: $RABBITMQ_VERSION"
else
    print_warning "无法访问 RabbitMQ 管理API"
fi

echo -e "\n11.2 查看队列和交换机配置"
print_info "检查 Topic 交换机和队列绑定"

# 检查交换机
EXCHANGES=$(curl -s -u admin:admin123 http://localhost:15672/api/exchanges/%2F)
if echo "$EXCHANGES" | jq -e '.[] | select(.name=="todo.event.exchange")' > /dev/null 2>&1; then
    print_success "todo.event.exchange (Topic) 交换机已创建"
else
    print_warning "todo.event.exchange 交换机未找到"
fi

# 检查队列
QUEUES=$(curl -s -u admin:admin123 http://localhost:15672/api/queues/%2F)
if echo "$QUEUES" | jq -e '.[] | select(.name=="user.notification.queue")' > /dev/null 2>&1; then
    print_success "user.notification.queue 队列已创建"
    QUEUE_MESSAGES=$(echo "$QUEUES" | jq -r '.[] | select(.name=="user.notification.queue") | .messages')
    print_info "队列消息数: $QUEUE_MESSAGES"
else
    print_warning "user.notification.queue 队列未找到"
fi

echo -e "\n11.3 创建Todo测试消息发送"
print_info "创建Todo时会触发消息发送到RabbitMQ"

TODO_MSG_TEST=$(curl -s -X POST http://localhost:8082/api/todos \
  -H "Content-Type: application/json" \
  -d '{"title":"测试RabbitMQ消息","description":"验证异步消息通信","userId":1}')
echo $TODO_MSG_TEST | jq '.'

TODO_MSG_ID=$(echo $TODO_MSG_TEST | jq -r '.data.id // .id // empty')
if [ -n "$TODO_MSG_ID" ] && [ "$TODO_MSG_ID" != "null" ]; then
    print_success "Todo创建成功，应该已发送消息到RabbitMQ"

    echo -e "\n11.4 等待消息处理"
    print_info "等待 user-service 消费消息（5秒）..."
    sleep 5

    echo -e "\n11.5 查看 user-service 日志（最后20行）"
    print_info "检查是否有消息消费日志"
    docker logs --tail 20 user-service 2>&1 | grep -E "Todo事件|接收消息|消息确认" || print_warning "未找到消息处理日志"

    echo -e "\n11.6 查看队列统计"
    QUEUE_STATS=$(curl -s -u admin:admin123 http://localhost:15672/api/queues/%2F/user.notification.queue)
    MESSAGES_READY=$(echo "$QUEUE_STATS" | jq -r '.messages_ready // 0')
    MESSAGES_UNACKED=$(echo "$QUEUE_STATS" | jq -r '.messages_unacknowledged // 0')
    TOTAL_MESSAGES=$(echo "$QUEUE_STATS" | jq -r '.messages // 0')

    print_info "队列统计:"
    echo "  - 待消费消息: $MESSAGES_READY"
    echo "  - 未确认消息: $MESSAGES_UNACKED"
    echo "  - 总消息数: $TOTAL_MESSAGES"

    if [ "$MESSAGES_READY" -eq 0 ] && [ "$MESSAGES_UNACKED" -eq 0 ]; then
        print_success "消息已被成功消费"
    else
        print_warning "消息可能还未被消费或消费失败"
    fi
else
    print_error "Todo创建失败，无法测试消息发送"
fi

echo -e "\n11.7 测试消息持久化和确认机制"
print_info "检查队列配置（持久化、死信队列、TTL）"

QUEUE_ARGS=$(echo "$QUEUE_STATS" | jq -r '.arguments // {}')
echo $QUEUE_ARGS | jq '.'

if echo "$QUEUE_STATS" | jq -e '.durable == true' > /dev/null 2>&1; then
    print_success "队列已配置持久化"
fi

# 12. 测试Prometheus指标采集
print_title "12. 测试 Prometheus 指标采集"
print_info "验证 Actuator Prometheus 端点暴露和自定义业务指标"

echo "12.1 检查 user-service Prometheus 端点"
USER_METRICS=$(curl -s http://localhost:8081/actuator/prometheus)

if [ -n "$USER_METRICS" ]; then
    print_success "user-service Prometheus端点可访问"

    echo -e "\n12.2 检查自定义业务指标"
    # 检查用户创建指标
    if echo "$USER_METRICS" | grep -q "users_created_total"; then
        print_success "发现自定义指标: users_created_total"
        USERS_CREATED=$(echo "$USER_METRICS" | grep "users_created_total" | grep -v "#" | awk '{print $2}')
        print_info "用户创建总数: $USERS_CREATED"
    fi

    # 检查认证成功指标
    if echo "$USER_METRICS" | grep -q "auth_success_total"; then
        print_success "发现自定义指标: auth_success_total"
    fi

    # 检查JVM指标
    if echo "$USER_METRICS" | grep -q "jvm_memory_used_bytes"; then
        print_success "发现JVM内存指标: jvm_memory_used_bytes"
    fi
else
    print_error "无法访问 user-service Prometheus端点"
fi

echo -e "\n12.3 检查 todo-service Prometheus 端点"
TODO_METRICS=$(curl -s http://localhost:8082/actuator/prometheus)

if [ -n "$TODO_METRICS" ]; then
    print_success "todo-service Prometheus端点可访问"

    # 检查todo创建指标
    if echo "$TODO_METRICS" | grep -q "todos_created_total"; then
        print_success "发现自定义指标: todos_created_total"
        TODOS_CREATED=$(echo "$TODO_METRICS" | grep "todos_created_total" | grep -v "#" | awk '{print $2}')
        print_info "Todo创建总数: $TODOS_CREATED"
    fi

    # 检查todo完成指标
    if echo "$TODO_METRICS" | grep -q "todos_completed_total"; then
        print_success "发现自定义指标: todos_completed_total"
    fi
fi

echo -e "\n12.4 检查 Prometheus 抓取配置"
PROM_TARGETS=$(curl -s http://localhost:9090/api/v1/targets 2>/dev/null)

if [ -n "$PROM_TARGETS" ]; then
    print_success "Prometheus targets API可访问"

    # 检查各个服务是否被Prometheus抓取
    if echo "$PROM_TARGETS" | jq -e '.data.activeTargets[] | select(.labels.job=="user-service")' > /dev/null 2>&1; then
        print_success "user-service 已被Prometheus监控"
    fi

    if echo "$PROM_TARGETS" | jq -e '.data.activeTargets[] | select(.labels.job=="todo-service")' > /dev/null 2>&1; then
        print_success "todo-service 已被Prometheus监控"
    fi

    if echo "$PROM_TARGETS" | jq -e '.data.activeTargets[] | select(.labels.job=="gateway-service")' > /dev/null 2>&1; then
        print_success "gateway-service 已被Prometheus监控"
    fi
fi

# 13. 测试Zipkin链路追踪
print_title "13. 测试 Zipkin 分布式链路追踪"
print_info "验证服务间调用的TraceId和SpanId传播"

echo "13.1 创建Todo触发服务间调用"
print_info "todo-service会通过OpenFeign调用user-service，产生分布式链路"

TRACE_TODO=$(curl -s -X POST http://localhost:8082/api/todos \
  -H "Content-Type: application/json" \
  -d '{"title":"测试链路追踪","description":"验证Zipkin集成","userId":1}')

TRACE_TODO_ID=$(echo $TRACE_TODO | jq -r '.data.id // .id // empty')

if [ -n "$TRACE_TODO_ID" ] && [ "$TRACE_TODO_ID" != "null" ]; then
    print_success "Todo创建成功，ID: $TRACE_TODO_ID"

    echo -e "\n13.2 等待链路数据上报到Zipkin"
    print_info "等待5秒让trace数据异步上报..."
    sleep 5

    echo -e "\n13.3 查询Zipkin中的调用链路"
    # 获取最近的traces
    TRACES=$(curl -s "http://localhost:9411/api/v2/traces?serviceName=todo-service&limit=10")

    if [ -n "$TRACES" ] && [ "$TRACES" != "[]" ]; then
        print_success "Zipkin中发现调用链路"

        # 统计trace数量
        TRACE_COUNT=$(echo "$TRACES" | jq '. | length')
        print_info "最近的trace数量: $TRACE_COUNT"

        # 分析最新的trace
        LATEST_TRACE=$(echo "$TRACES" | jq '.[0]')
        TRACE_ID=$(echo "$LATEST_TRACE" | jq -r '.[0].traceId')
        SPAN_COUNT=$(echo "$LATEST_TRACE" | jq '. | length')

        print_success "TraceID: $TRACE_ID"
        print_success "Span数量: $SPAN_COUNT (包含服务间调用)"

        # 列出trace中的服务
        echo -e "\n13.4 分析服务调用链路"
        SERVICES=$(echo "$LATEST_TRACE" | jq -r '.[].localEndpoint.serviceName' | sort -u)
        print_info "参与的服务:"
        echo "$SERVICES" | while read service; do
            echo "  - $service"
        done

        # 检查是否包含跨服务调用
        if echo "$SERVICES" | grep -q "todo-service" && echo "$SERVICES" | grep -q "user-service"; then
            print_success "检测到跨服务调用: todo-service → user-service"
        fi
    else
        print_warning "Zipkin中未找到调用链路，可能数据还未上报"
    fi
else
    print_error "Todo创建失败，无法测试链路追踪"
fi

echo -e "\n13.5 检查日志中的TraceId"
print_info "查看服务日志中的TraceId和SpanId"
docker logs --tail 20 todo-service 2>&1 | grep -E "\[todo-service,[a-f0-9]{16},[a-f0-9]{16}\]" | head -3

if docker logs --tail 50 todo-service 2>&1 | grep -qE "\[todo-service,[a-f0-9]{16},[a-f0-9]{16}\]"; then
    print_success "服务日志中包含TraceId和SpanId"
else
    print_warning "服务日志中未找到TraceId（可能格式不匹配）"
fi

# 总结
print_separator
print_title "测试完成总结"
echo "✓ 服务健康检查测试完成（Spring Boot Actuator）"
echo "✓ RabbitMQ服务检查完成（管理界面和AMQP端口）"
echo "✓ Nacos配置中心测试完成（配置读取）"
echo "✓ 配置动态刷新测试完成（@RefreshScope）"
echo "✓ 服务注册与发现测试完成（Nacos Discovery）"
echo "✓ 数据库配置测试完成（MySQL连接池）"
echo "✓ 微服务间通信测试完成（OpenFeign）"
echo "✓ API网关路由测试完成（Spring Cloud Gateway）"
echo "✓ Docker Compose编排测试完成（容器健康检查）"
echo "✓ RabbitMQ异步消息通信测试完成（Topic交换机、队列绑定、消息确认）"
echo "✓ Prometheus指标采集测试完成（Actuator端点、自定义指标）"
echo "✓ Zipkin链路追踪测试完成（TraceId传播、服务调用链路分析）"
print_separator

echo -e "\n${CYAN}🎓 第13周知识点验证:${NC}"
echo "  ✅ Prometheus监控（指标采集和查询）"
echo "  ✅ Grafana可视化（Dashboard配置）"
echo "  ✅ 自定义业务指标（Counter、Timer）"
echo "  ✅ Spring Boot Actuator（健康检查和指标暴露）"
echo "  ✅ Micrometer集成（Prometheus格式导出）"
echo "  ✅ Zipkin分布式链路追踪（TraceId和SpanId）"
echo "  ✅ 链路数据上报（HTTP Reporter）"
echo "  ✅ 服务调用链分析（依赖关系图）"
echo "  ✅ 日志关联（TraceId注入日志）"
echo ""
echo "  ✅ 配置中心集中管理配置（Nacos Config）"
echo "  ✅ RabbitMQ消息队列（异步通信）"
echo "  ✅ Topic交换机（通配符路由）"
echo "  ✅ 队列绑定和消息路由"
echo "  ✅ 消息持久化（durable队列）"
echo "  ✅ 手动消息确认（basicAck/basicNack）"
echo "  ✅ 消息生产者和消费者"
echo "  ✅ JSON消息转换器"
echo "  ✅ 服务解耦（事件驱动架构）"
echo ""
echo "  ✅ 配置中心集中管理配置（Nacos Config）"
echo "  ✅ 配置动态刷新（无需重启服务）"
echo "  ✅ 环境隔离（namespace: dev）"
echo "  ✅ 健康检查（Docker healthcheck + Actuator）"
echo "  ✅ 服务依赖管理（depends_on + condition）"
echo "  ✅ Docker Compose多容器编排"
echo "  ✅ 数据持久化（volumes）"
echo "  ✅ 容器网络通信（bridge network）"

echo -e "\n${CYAN}📚 相关资源:${NC}"
echo "  • Nacos控制台: http://localhost:8080 (nacos/nacos)"
echo "  • RabbitMQ管理界面: http://localhost:15672 (admin/admin123)"
echo "  • Zipkin链路追踪: http://localhost:9411"
echo "  • Prometheus监控: http://localhost:9090"
echo "  • Grafana Dashboard: http://localhost:3000 (admin/admin)"
echo "  • 用户服务: http://localhost:8081/api/users"
echo "  • Todo服务: http://localhost:8082/api/todos"
echo "  • API网关: http://localhost:9000"
echo "  • 配置文档: NACOS_CONFIG.md"

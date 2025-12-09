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

# 检查命令是否存在
check_command() {
    if command -v $1 &> /dev/null; then
        print_success "$1 已安装"
        return 0
    else
        print_error "$1 未安装"
        return 1
    fi
}

# 检查Docker镜像是否存在
check_image() {
    local image=$1
    if docker images --format "{{.Repository}}:{{.Tag}}" | grep -q "^${image}$"; then
        print_success "镜像 $image 已存在"
        return 0
    else
        print_warning "镜像 $image 不存在"
        return 1
    fi
}

# 下载Docker镜像（使用DaoCloud镜像源）
pull_image() {
    local mirror_image=$1
    local target_image=$2

    print_info "从 DaoCloud 下载 $target_image..."

    if docker pull $mirror_image; then
        docker tag $mirror_image $target_image
        docker rmi $mirror_image > /dev/null 2>&1
        print_success "镜像 $target_image 下载完成"
        return 0
    else
        print_error "镜像 $target_image 下载失败"
        return 1
    fi
}

# 显示帮助信息
show_help() {
    cat << EOF
Todo 微服务项目运行脚本 v2.1.0

用法: ./run.sh [选项]

选项:
    start       启动所有服务（默认）
    stop        停止所有服务
    restart     重启所有服务
    down        停止并删除所有容器
    clean       停止并删除所有容器和数据卷
    logs        查看所有服务日志
    logs <service>  查看指定服务日志
    ps          查看服务状态
    build       仅构建JAR包
    images      仅下载Docker镜像
    test        运行测试脚本
    help        显示帮助信息

示例:
    ./run.sh                    # 启动所有服务
    ./run.sh start              # 启动所有服务
    ./run.sh logs user-service  # 查看user-service日志
    ./run.sh stop               # 停止所有服务
    ./run.sh clean              # 清理所有数据
EOF
}

# 检查环境
check_environment() {
    print_title "1. 检查运行环境"

    local all_good=true

    check_command docker || all_good=false
    check_command docker-compose || check_command docker compose || all_good=false
    check_command java || all_good=false
    check_command mvn || all_good=false
    check_command jq || print_warning "jq 未安装，部分功能可能受限"

    if [ "$all_good" = false ]; then
        print_error "环境检查失败，请安装缺失的工具"
        exit 1
    fi

    print_success "环境检查通过"
}

# 下载所有必需的Docker镜像
download_images() {
    print_title "2. 下载 Docker 镜像"

    # 检查并下载MySQL镜像
    if ! check_image "mysql:8.4"; then
        pull_image "m.daocloud.io/docker.io/library/mysql:8.4" "mysql:8.4"
    fi

    # 检查并下载Nacos镜像
    if ! check_image "nacos/nacos-server:v3.1.0"; then
        pull_image "m.daocloud.io/docker.io/nacos/nacos-server:v3.1.0" "nacos/nacos-server:v3.1.0"
    fi

    # 检查并下载JRE基础镜像
    if ! check_image "eclipse-temurin:25-jre"; then
        pull_image "m.daocloud.io/docker.io/library/eclipse-temurin:25-jre" "eclipse-temurin:25-jre"
    fi

    # 检查并下载Maven构建镜像
    if ! check_image "maven:3.9-eclipse-temurin-25"; then
        pull_image "m.daocloud.io/docker.io/library/maven:3.9-eclipse-temurin-25" "maven:3.9-eclipse-temurin-25"
    fi

    print_success "所有镜像准备完成"
}

# 构建JAR包
build_services() {
    print_title "3. 构建服务 JAR 包"

    local services=("user-service" "todo-service" "gateway-service")

    for service in "${services[@]}"; do
        if [ -d "$service" ]; then
            print_info "构建 $service..."
            cd $service

            # 检查是否可以使用 Maven Wrapper
            if [ -f "mvnw" ] && [ -d ".mvn" ]; then
                print_info "使用 Maven Wrapper"
                ./mvnw clean package -DskipTests
            else
                print_info "使用系统 Maven"
                mvn clean package -DskipTests
            fi

            if [ $? -eq 0 ]; then
                print_success "$service 构建成功"
            else
                print_error "$service 构建失败"
                cd ..
                exit 1
            fi
            cd ..
        else
            print_warning "$service 目录不存在，跳过"
        fi
    done

    print_success "所有服务构建完成"
}

# 启动所有服务
start_services() {
    print_title "4. 启动微服务"

    print_info "启动 Docker Compose..."
    docker-compose up -d --build

    if [ $? -eq 0 ]; then
        print_success "服务启动成功"
        echo ""
        wait_for_services
    else
        print_error "服务启动失败"
        exit 1
    fi
}

# 等待服务就绪
wait_for_services() {
    print_title "5. 等待服务就绪"

    print_info "等待 Nacos 启动..."
    local nacos_ready=false
    for i in {1..30}; do
        if curl -s http://localhost:8848/nacos/ > /dev/null 2>&1; then
            nacos_ready=true
            print_success "Nacos 已就绪"
            break
        fi
        echo -n "."
        sleep 2
    done

    if [ "$nacos_ready" = false ]; then
        print_error "Nacos 启动超时"
        return 1
    fi

    echo ""
    print_info "等待微服务注册..."
    sleep 10

    # 检查服务注册状态
    local services=("user-service" "todo-service" "gateway-service")
    for service in "${services[@]}"; do
        if curl -s "http://localhost:8848/nacos/v1/ns/instance/list?serviceName=$service" | grep -q "\"instanceId\""; then
            print_success "$service 已注册"
        else
            print_warning "$service 未注册"
        fi
    done
}

# 显示服务信息
show_info() {
    print_separator
    print_title "服务访问信息"

    echo -e "${CYAN}Nacos 控制台:${NC}"
    echo -e "  URL: http://localhost:8080"
    echo -e "  用户名: nacos"
    echo -e "  密码: nacos"
    echo ""

    echo -e "${CYAN}API 网关:${NC}"
    echo -e "  URL: http://localhost:9000"
    echo -e "  认证: JWT Token"
    echo ""

    echo -e "${CYAN}用户服务:${NC}"
    echo -e "  API: http://localhost:8081/api/users"
    echo -e "  健康检查: http://localhost:8081/actuator/health"
    echo -e "  配置信息: http://localhost:8081/api/config/info"
    echo ""

    echo -e "${CYAN}Todo 服务:${NC}"
    echo -e "  API: http://localhost:8082/api/todos"
    echo -e "  健康检查: http://localhost:8082/actuator/health"
    echo -e "  配置信息: http://localhost:8082/api/config/info"
    echo ""

    echo -e "${CYAN}数据库:${NC}"
    echo -e "  user-db: localhost:3307"
    echo -e "  todo-db: localhost:3308"
    echo ""

    echo -e "${CYAN}下一步:${NC}"
    echo -e "  1. 运行测试: ${GREEN}./test-services.sh${NC}"
    echo -e "  2. 查看日志: ${GREEN}./run.sh logs${NC}"
    echo -e "  3. 访问 Nacos 控制台查看配置和服务注册"

    print_separator
}

# 停止服务
stop_services() {
    print_title "停止服务"
    docker-compose stop
    print_success "服务已停止"
}

# 重启服务
restart_services() {
    print_title "重启服务"
    docker-compose restart
    print_success "服务已重启"
}

# 删除容器
down_services() {
    print_title "删除容器"
    docker-compose down
    print_success "容器已删除"
}

# 清理所有数据
clean_all() {
    print_title "清理所有数据"
    print_warning "这将删除所有容器和数据卷，数据将不可恢复！"
    read -p "确认继续？(y/N) " -n 1 -r
    echo
    if [[ $REPLY =~ ^[Yy]$ ]]; then
        docker-compose down -v
        print_success "清理完成"
    else
        print_info "已取消"
    fi
}

# 查看日志
view_logs() {
    local service=$1
    if [ -z "$service" ]; then
        docker-compose logs -f
    else
        docker-compose logs -f $service
    fi
}

# 查看服务状态
view_status() {
    print_title "服务状态"
    docker-compose ps
}

# 运行测试
run_tests() {
    print_title "运行测试脚本"
    if [ -f "test-services.sh" ]; then
        bash test-services.sh
    else
        print_error "test-services.sh 不存在"
    fi
}

# 主函数
main() {
    case "${1:-start}" in
        start)
            check_environment
            download_images
            build_services
            start_services
            show_info
            ;;
        stop)
            stop_services
            ;;
        restart)
            restart_services
            ;;
        down)
            down_services
            ;;
        clean)
            clean_all
            ;;
        logs)
            view_logs $2
            ;;
        ps|status)
            view_status
            ;;
        build)
            check_environment
            build_services
            ;;
        images)
            check_environment
            download_images
            ;;
        test)
            run_tests
            ;;
        help|--help|-h)
            show_help
            ;;
        *)
            print_error "未知选项: $1"
            echo ""
            show_help
            exit 1
            ;;
    esac
}

# 运行主函数
main "$@"

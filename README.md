# Todo 微服务项目

这是将单体todo应用拆分为微服务架构的实践项目，集成了Nacos服务注册与发现。

**当前版本**: 2.2.0
**主要特性**: API Gateway统一入口、JWT身份认证、OpenFeign声明式客户端、LoadBalancer负载均衡、Resilience4j熔断与重试、Nacos Config配置中心、动态配置刷新、RabbitMQ异步消息通信

## 📋 项目说明

本项目将单体todo应用拆分为微服务架构,并使用Spring Cloud Gateway作为统一入口，通过JWT实现身份认证，通过RabbitMQ实现服务间异步通信：

- **gateway-service** (API网关) - 端口 9000, 统一入口和JWT认证
- **user-service** (用户服务) - 端口 8081, 数据库 user_db
- **todo-service** (待办事项服务) - 端口 8082, 数据库 todo_db
- **nacos** (服务注册中心) - 端口 8848
- **rabbitmq** (消息队列) - 端口 5672 (AMQP), 15672 (管理界面)

## 🏗️ 架构图

```
┌─────────────────────────────────────────────────────────────┐
│                        客户端应用                            │
└─────────────────────────┬───────────────────────────────────┘
                          │
                          │ JWT Token
                  ┌───────▼───────┐
                  │ gateway-service│
                  │   (API网关)    │
                  │   :9000        │
                  └───┬───────┬────┘
                      │       │
          ┌───────────┘       └───────────┐
          │                               │
  ┌───────▼───────┐              ┌───────▼───────┐
  │ user-service  │              │ todo-service  │
  │   :8081       │◄─────────────┤   :8082       │
  └───────┬───────┘   OpenFeign  └───────┬───────┘
          │   ▲                           │   ▲
          │   │                           │   │
          │   └───────┬───────────────────┘   │
          │     服务注册/发现                  │
          │   ┌───────▼───────┐               │
          │   │    Nacos      │               │
          │   │   :8848       │               │
          │   └───────────────┘               │
          │                                   │
  ┌───────▼───────┐              ┌───────▼───────┐
  │   user_db     │              │   todo_db     │
  │   (MySQL)     │              │   (MySQL)     │
  └───────────────┘              └───────────────┘
```

## 🚀 快速开始

### 前置要求

- JDK 17+
- Maven 3.8+
- Docker 和 Docker Compose

### 使用 Docker Compose 运行（推荐）

1. **下载Docker镜像（国内加速）**

由于Docker Hub访问较慢，建议使用国内镜像源下载：

```bash
# 下载MySQL镜像
docker pull m.daocloud.io/docker.io/library/mysql:8.4
docker tag m.daocloud.io/docker.io/library/mysql:8.4 mysql:8.4

# 下载Nacos镜像
docker pull m.daocloud.io/docker.io/nacos/nacos-server:v3.1.0
docker tag m.daocloud.io/docker.io/nacos/nacos-server:v3.1.0 nacos/nacos-server:v3.1.0

# 下载JRE基础镜像（用于构建服务镜像）
docker pull m.daocloud.io/docker.io/library/eclipse-temurin:25-jre
docker tag m.daocloud.io/docker.io/library/eclipse-temurin:25-jre eclipse-temurin:25-jre

# 验证镜像
docker images | grep -E "mysql|nacos|eclipse-temurin"
```

2. **构建JAR包**

```bash
# 构建user-service
cd user-service
./mvnw clean package -DskipTests

# 构建todo-service
cd ../todo-service
./mvnw clean package -DskipTests

# 构建gateway-service
cd ../gateway-service
./mvnw clean package -DskipTests
```

3. **启动所有服务**

```bash
cd ..
docker-compose up -d --build
```

4. **查看服务状态**

```bash
docker-compose ps
```

5. **访问Nacos控制台**

打开浏览器访问：http://localhost:8080

- 用户名：nacos
- 密码：nacos

在"服务管理" → "服务列表"中可以看到已注册的服务。

6. **查看日志**

```bash
# 查看所有日志
docker-compose logs -f

# 查看特定服务日志
docker-compose logs -f user-service
docker-compose logs -f todo-service
docker-compose logs -f nacos
```

7. **停止服务**

```bash
# 停止所有服务
docker-compose down

# 停止并删除数据卷
docker-compose down -v
```

### 本地运行（不使用Docker）

1. **安装并启动MySQL**

```sql
CREATE DATABASE user_db CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
CREATE DATABASE todo_db CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
```

2. **启动Nacos**

```bash
# 使用Docker启动Nacos
docker run -d \
  --name nacos \
  -p 8848:8848 \
  -p 9848:9848 \
  -e MODE=standalone \
  nacos/nacos-server:v3.1.0
```

3. **启动服务**

```bash
# 终端1：启动user-service
cd user-service
./mvnw spring-boot:run

# 终端2：启动todo-service
cd todo-service
./mvnw spring-boot:run
```

## 🧪 测试服务

### 使用自动化测试脚本（推荐）

项目提供了全面的自动化测试脚本，测试v1.2.0的所有新特性：

```bash
./test-services.sh
```

测试脚本包含以下测试项：

1. **服务状态检查** - 验证Nacos、user-service、todo-service是否运行
2. **用户服务API测试** - 创建用户、查询用户
3. **Todo服务API测试** - 创建Todo、查询Todo、切换状态
4. **OpenFeign服务间通信** - 验证声明式客户端调用
5. **Nacos服务发现** - 确认服务已正确注册
6. **Resilience4j熔断器测试** - 停止user-service验证熔断和降级
7. **重试机制测试** - 验证指数退避重试（3次，500ms起）
8. **简单负载测试** - 连续创建10个Todo测试稳定性

### 手动测试

```bash
# 测试用户服务
curl http://localhost:8081/api/users

# 创建用户
curl -X POST http://localhost:8081/api/users \
  -H "Content-Type: application/json" \
  -d '{"username":"张三","email":"zhangsan@example.com"}'

# 测试Todo服务
curl http://localhost:8082/api/todos

# 创建Todo（关联到用户1，会通过OpenFeign验证用户）
curl -X POST http://localhost:8082/api/todos \
  -H "Content-Type: application/json" \
  -d '{"title":"学习微服务","description":"完成拆分实践","userId":1}'

# 测试熔断器（先停止user-service）
docker stop user-service
curl -X POST http://localhost:8082/api/todos \
  -H "Content-Type: application/json" \
  -d '{"title":"测试熔断","userId":1}'
# 观察重试和超时行为（约10秒）
docker start user-service
```

## 📊 API文档

### API网关 (端口 9000)

**所有API请求应通过网关访问，网关会进行JWT认证和路由转发。**

#### 认证API（无需Token）

| 方法 | URL | 说明 |
|------|-----|------|
| POST | `/api/auth/login` | 用户登录，返回JWT Token |
| POST | `/api/auth/register` | 用户注册 |

**登录示例**：

```bash
curl -X POST http://localhost:9000/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{"username":"testuser","password":"password123"}'

# 响应
{
  "code": 200,
  "message": "登录成功",
  "data": {
    "token": "eyJhbGciOiJIUzUxMiJ9...",
    "user": {
      "id": 1,
      "username": "testuser",
      "role": "USER"
    }
  }
}
```

**注册示例**：

```bash
curl -X POST http://localhost:9000/api/auth/register \
  -H "Content-Type: application/json" \
  -d '{"username":"testuser","password":"password123","email":"test@example.com"}'
```

#### 受保护的API（需要Token）

所有其他API都需要在请求头中携带JWT Token：

```bash
Authorization: Bearer <your-jwt-token>
```

### 用户服务 API (通过网关访问)

| 方法 | URL | 说明 |
|------|-----|------|
| GET | `/api/users` | 获取所有用户 |
| GET | `/api/users/{id}` | 获取单个用户 |
| POST | `/api/users` | 创建用户 |
| PUT | `/api/users/{id}` | 更新用户 |
| DELETE | `/api/users/{id}` | 删除用户 |

### Todo服务 API (通过网关访问)

**示例：使用JWT Token创建Todo**：

```bash
TOKEN="eyJhbGciOiJIUzUxMiJ9..."  # 从登录接口获取

curl -X POST http://localhost:9000/api/todos \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d '{"title":"学习微服务","description":"完成API网关实践","userId":1}'
```

| 方法 | URL | 说明 |
|------|-----|------|
| GET | `/api/todos` | 获取所有Todo |
| GET | `/api/todos?userId={id}` | 按用户筛选Todo |
| GET | `/api/todos/{id}` | 获取单个Todo |
| POST | `/api/todos` | 创建Todo |
| PUT | `/api/todos/{id}` | 更新Todo |
| PATCH | `/api/todos/{id}/toggle` | 切换完成状态 |
| DELETE | `/api/todos/{id}` | 删除Todo |

### Nacos控制台

| URL | 说明 |
|-----|------|
| http://localhost:8080 | Nacos控制台（账号：nacos/nacos）|

## 🔧 技术栈

- **Spring Boot** 3.5.7
- **Spring Cloud** 2025.0.0
- **Spring Cloud Alibaba** 2025.0.0.0
- **Spring Cloud Gateway** - API网关，统一入口
- **Nacos** 3.1.0 - 服务注册与发现、配置中心
- **Nacos Config** - 集中配置管理，动态配置刷新
- **RabbitMQ** 3-management - 异步消息队列，服务解耦
- **Spring AMQP** - RabbitMQ客户端，消息收发
- **Spring Boot Actuator** - 健康检查和监控端点
- **OpenFeign** - 声明式HTTP客户端，服务间通信
- **Spring Cloud LoadBalancer** - 客户端负载均衡
- **Resilience4j** - 熔断器和重试机制
- **JWT (jjwt)** - JSON Web Token身份认证
- **BCrypt** - 密码加密
- **Java** 25
- **Maven** 3.8+
- **MySQL** 8.4
- **Docker** & Docker Compose

## 📁 项目结构

```
todo-microservices/
├── docker-compose.yml           # Docker编排文件
├── test-services.sh             # 测试脚本
├── README.md                    # 项目文档
├── NACOS_CONFIG.md              # Nacos配置中心文档
│
├── gateway-service/             # API网关
│   ├── src/main/java/com/zjgsu/gateway/
│   │   ├── filter/
│   │   │   ├── JwtAuthenticationFilter.java  # JWT认证过滤器
│   │   │   └── LoggingFilter.java            # 日志过滤器
│   │   └── util/
│   │       └── JwtUtil.java                  # JWT工具类
│   ├── src/main/resources/
│   │   ├── bootstrap.yml                     # Nacos配置启动文件
│   │   ├── application.yml
│   │   └── application-prod.yml
│   ├── Dockerfile
│   ├── Dockerfile.multistage                 # 多阶段构建
│   └── pom.xml
│
├── user-service/                # 用户服务
│   ├── src/main/java/com/zjgsu/user/
│   │   ├── controller/
│   │   │   ├── UserController.java
│   │   │   ├── AuthController.java           # 认证控制器
│   │   │   └── ConfigController.java         # 配置查询控制器
│   │   ├── dto/
│   │   │   ├── LoginRequest.java
│   │   │   └── LoginResponse.java
│   │   └── util/
│   │       └── JwtUtil.java                  # JWT工具类
│   ├── src/main/resources/
│   │   ├── bootstrap.yml                     # Nacos配置启动文件
│   │   ├── application.yml
│   │   └── application-prod.yml
│   ├── Dockerfile
│   ├── Dockerfile.multistage                 # 多阶段构建
│   └── pom.xml
│
└── todo-service/                # Todo服务
    ├── src/main/java/com/zjgsu/todoservice/
    │   ├── controller/
    │   │   └── ConfigController.java         # 配置查询控制器
    ├── src/main/resources/
    │   ├── bootstrap.yml                     # Nacos配置启动文件
    │   ├── application.yml
    │   └── application-prod.yml
    ├── Dockerfile
    ├── Dockerfile.multistage                 # 多阶段构建
    └── pom.xml
```

## 🌳 Git Subtree 管理

本项目使用 Git Subtree 管理子服务，user-service 和 todo-service 作为独立的 Git 仓库被集成到主项目中。

### 仓库结构

- **主仓库**: `todo-microservices` - 包含 docker-compose 和项目文档
- **子仓库**:
  - `user-service` → https://github.com/zjsu-ms/user-service.git
  - `todo-service` → https://github.com/zjsu-ms/todo-service.git

### 初始化设置（仅首次）

如果您是首次克隆此项目，子服务目录可能为空，需要添加 subtree：

```bash
cd /path/to/todo-microservices

# 添加 user-service 作为 subtree
git subtree add --prefix=user-service https://github.com/zjsu-ms/user-service.git main --squash

# 添加 todo-service 作为 subtree
git subtree add --prefix=todo-service https://github.com/zjsu-ms/todo-service.git main --squash
```

**注意**：这些命令需要访问 GitHub，如果在国内环境可能需要配置代理。

### 日常开发命令

#### 拉取子项目更新

```bash
# 从 user-service 远程仓库拉取最新代码
git subtree pull --prefix=user-service https://github.com/zjsu-ms/user-service.git main --squash

# 从 todo-service 远程仓库拉取最新代码
git subtree pull --prefix=todo-service https://github.com/zjsu-ms/todo-service.git main --squash
```

#### 推送子项目更改

当您修改了 user-service 或 todo-service 的代码后：

```bash
# 推送 user-service 的更改到其远程仓库
git subtree push --prefix=user-service https://github.com/zjsu-ms/user-service.git main

# 推送 todo-service 的更改到其远程仓库
git subtree push --prefix=todo-service https://github.com/zjsu-ms/todo-service.git main
```

### 使用远程别名（可选，简化命令）

为了避免每次都输入完整的 URL，可以配置远程别名：

```bash
# 添加远程别名
git remote add user-service-remote https://github.com/zjsu-ms/user-service.git
git remote add todo-service-remote https://github.com/zjsu-ms/todo-service.git

# 使用别名拉取
git subtree pull --prefix=user-service user-service-remote main --squash
git subtree pull --prefix=todo-service todo-service-remote main --squash

# 使用别名推送
git subtree push --prefix=user-service user-service-remote main
git subtree push --prefix=todo-service todo-service-remote main
```

### 工作流建议

1. **修改子服务代码**: 直接在 `user-service/` 或 `todo-service/` 目录下修改
2. **提交到主仓库**: `git add . && git commit -m "描述"`
3. **推送到主仓库**: `git push origin main`
4. **同步到子仓库**: 使用 `git subtree push` 命令推送到对应的子仓库

### 首次推送子项目到 GitHub

如果子服务仓库在其他位置已经存在，需要先推送到 GitHub：

```bash
# 示例：推送已存在的 user-service
cd /path/to/existing/user-service
git remote add origin https://github.com/zjsu-ms/user-service.git
git push -u origin main

# 示例：推送已存在的 todo-service
cd /path/to/existing/todo-service
git remote add origin https://github.com/zjsu-ms/todo-service.git
git push -u origin main
```

然后返回主项目使用 `git subtree add` 命令添加。

## 🔍 服务间通信

todo-service通过OpenFeign声明式客户端调用user-service验证用户存在性：

```java
// UserClient.java - Feign客户端接口
@FeignClient(
    name = "user-service",
    fallback = UserClientFallback.class
)
public interface UserClient {
    @GetMapping("/api/users/{id}")
    Map<String, Object> getUser(@PathVariable("id") Long id);
}

// UserClientFallback.java - 降级处理
@Component
public class UserClientFallback implements UserClient {
    @Override
    public Map<String, Object> getUser(Long id) {
        logger.warn("User service unavailable, returning fallback for user ID: {}", id);

        Map<String, Object> fallbackUser = new HashMap<>();
        fallbackUser.put("id", id);
        fallbackUser.put("username", "默认用户");
        fallbackUser.put("email", "default@example.com");
        fallbackUser.put("fallback", true);

        return fallbackUser;
    }
}

// TodoService.java - 使用Feign客户端
@Service
public class TodoService {
    private final UserClient userClient;

    private void verifyUserExists(Long userId) {
        try {
            Map<String, Object> user = userClient.getUser(userId);

            // 检查是否是降级响应
            if (user.containsKey("fallback") && Boolean.TRUE.equals(user.get("fallback"))) {
                logger.warn("User service is unavailable, using fallback data");
            }
        } catch (HttpClientErrorException.NotFound e) {
            throw new ResourceNotFoundException("User", userId);
        }
    }
}
```

### OpenFeign的优势

相比手动使用RestTemplate + DiscoveryClient，OpenFeign提供了更优雅的服务调用方式：

| 功能特性 | RestTemplate + DiscoveryClient | OpenFeign + LoadBalancer |
|---------|-------------------------------|-------------------------|
| **代码风格** | 命令式，需要手动构建URL | 声明式，类似本地方法调用 |
| **负载均衡** | 需要手动实现（如随机选择） | 自动集成LoadBalancer |
| **熔断降级** | 需要手动集成Resilience4j | 通过fallback自动支持 |
| **重试机制** | 需要手动实现 | 自动集成Resilience4j重试 |
| **代码量** | 约15-20行 | 约5-10行 |
| **维护性** | 较低，URL拼接易出错 | 高，类型安全，编译时检查 |

### Resilience4j配置

项目集成了熔断器和重试机制：

```yaml
resilience4j:
  circuitbreaker:
    instances:
      user-service:
        failure-rate-threshold: 50              # 失败率阈值50%
        sliding-window-size: 10                 # 滑动窗口10次调用
        wait-duration-in-open-state: 10s        # 熔断后等待10秒

  retry:
    instances:
      user-service:
        max-attempts: 3                         # 最大重试3次
        wait-duration: 500ms                    # 重试间隔500ms
        enable-exponential-backoff: true        # 启用指数退避
        exponential-backoff-multiplier: 2       # 退避乘数2
```

## ⚙️ Nacos配置中心

v2.1.0新增了Nacos Config配置中心支持，实现配置的集中管理和动态刷新。

### 核心功能

- **集中管理配置**：所有服务的配置统一在Nacos控制台管理
- **动态配置刷新**：通过`@RefreshScope`实现配置热更新，无需重启服务
- **环境隔离**：支持dev、test、prod等多环境配置
- **配置优先级**：bootstrap.yml → Nacos远程配置 → application.yml → 命令行参数

### 配置架构

```yaml
# bootstrap.yml - 在application.yml之前加载
spring:
  application:
    name: user-service
  cloud:
    nacos:
      config:
        server-addr: ${NACOS_SERVER_ADDR:localhost:8848}
        file-extension: yaml
        namespace: ${NACOS_NAMESPACE:dev}  # 环境隔离
        group: DEFAULT_GROUP
        refresh-enabled: true               # 启用动态刷新
      discovery:
        server-addr: ${NACOS_SERVER_ADDR:localhost:8848}

# 健康检查端点
management:
  endpoints:
    web:
      exposure:
        include: health,info,refresh
```

### 测试配置动态刷新

1. **访问配置接口**查看当前配置：

```bash
curl http://localhost:8081/api/config/info
# 返回：
{
  "appName": "User Service",
  "appVersion": "1.0.0",
  "appDescription": "用户管理微服务",
  "message": "配置信息来自Nacos配置中心"
}
```

2. **在Nacos控制台修改配置**：
   - 登录 http://localhost:8080
   - 进入"配置管理" → "配置列表"
   - 找到`user-service-dev.yaml`
   - 修改`app.version: 2.1.0`并发布

3. **立即查看配置更新**（无需重启）：

```bash
curl http://localhost:8081/api/config/info
# 返回更新后的配置：
{
  "appName": "User Service",
  "appVersion": "2.1.0",  # 已动态更新
  "appDescription": "用户管理微服务",
  "message": "配置信息来自Nacos配置中心"
}
```

### Nacos配置文件示例

详细的Nacos配置文件模板和配置步骤请参考 [NACOS_CONFIG.md](./NACOS_CONFIG.md)。

**user-service-dev.yaml** 示例：

```yaml
server:
  port: 8081

spring:
  datasource:
    url: jdbc:mysql://user-db:3306/user_db?useSSL=false&serverTimezone=UTC
    username: user_user
    password: user_pass

jwt:
  secret: your-256-bit-secret-key-here
  expiration: 86400000

app:
  name: 用户管理服务
  version: 2.1.0
  description: 提供用户管理和JWT认证功能
```

### 健康检查端点

所有服务都暴露了健康检查端点：

```bash
# 查看服务健康状态
curl http://localhost:8081/actuator/health

# 返回详细状态
{
  "status": "UP",
  "components": {
    "db": {"status": "UP"},
    "nacosConfig": {"status": "UP"},
    "nacosDiscovery": {"status": "UP"},
    "refreshScope": {"status": "UP"}
  }
}
```

## 🐛 常见问题

### Q1: 服务无法注册到Nacos？

检查：
1. Nacos是否已启动？`curl http://localhost:8848/nacos/`
2. 服务配置中的`server-addr`是否正确？
3. Docker环境下是否使用了正确的容器名（nacos而非localhost）

### Q2: 服务间调用失败？

检查：
1. 两个服务是否都已注册到Nacos？查看Nacos控制台
2. 服务名是否正确？（大小写敏感）
3. Docker网络是否正确？`docker network inspect todo-microservices_todo-network`

### Q3: 数据库连接失败？

```bash
# 检查容器状态
docker-compose ps

# 查看数据库日志
docker-compose logs user-db
docker-compose logs todo-db

# 验证连接
docker exec -it user-db mysql -u user_user -puser_pass -e "SHOW DATABASES;"
```

### Q4: JAR包构建失败？

```bash
# 清理并重新构建
cd user-service
./mvnw clean compile

cd ../todo-service
./mvnw clean compile
```

## 📝 下一步

服务注册与发现、声明式客户端、熔断降级、API网关、JWT认证、配置中心、异步消息通信已完成，可以考虑以下改进：

1. ~~**服务注册与发现**：集成Nacos~~ ✅ 已完成（v1.0.0）
2. ~~**声明式客户端**：使用OpenFeign替代RestTemplate~~ ✅ 已完成（v1.2.0）
3. ~~**熔断降级**：使用Resilience4j~~ ✅ 已完成（v1.2.0）
4. ~~**API网关**：添加Spring Cloud Gateway~~ ✅ 已完成（v2.0.0）
5. ~~**JWT认证**：实现基于Token的身份认证~~ ✅ 已完成（v2.0.0）
6. ~~**配置中心**：使用Nacos Config集中管理配置~~ ✅ 已完成（v2.1.0）
7. **异步消息通信**：集成RabbitMQ实现服务解耦 🔧 部分完成（v2.2.0，见已知问题）
8. **链路追踪**：集成Sleuth和Zipkin
9. **服务监控**：集成Prometheus和Grafana

## 🐞 已知问题

### RabbitMQ消息反序列化问题 (v2.2.0)

**问题描述**：user-service 无法正确消费 todo-service 发送的消息。

**错误信息**：
```
ClassNotFoundException: com.zjgsu.todoservice.dto.TodoEventMessage
```

**原因分析**：
- todo-service 使用 `Jackson2JsonMessageConverter` 发送消息时，会在消息头中包含类型信息（`__TypeId__`）
- user-service 接收消息时，Jackson 转换器尝试根据类型信息反序列化为 `TodoEventMessage` 类
- 但 user-service 中不存在 `com.zjgsu.todoservice.dto.TodoEventMessage` 类，导致 ClassNotFoundException

**已尝试的解决方案**（均未生效）：
1. 设置 `typeMapper.setTrustedPackages("*")` - 仍然尝试加载不存在的类
2. 设置 `typeMapper.setTypePrecedence(INFERRED)` - 未能阻止类型查找

**可能的解决方案**（待实现）：
1. **共享DTO模块**：创建独立的 `common-dto` 模块，包含共享的消息类
2. **移除类型信息**：在发送端配置不包含类型头信息
3. **使用SimpleMessageConverter**：改用简单字符串消息，接收端手动解析JSON

**当前状态**：
- ✅ todo-service 成功发送消息到 RabbitMQ
- ❌ user-service 消费消息失败

## 📚 参考资源

- [拆分方案文档](../todo/SPLITTING.md)
- [Spring Boot官方文档](https://spring.io/projects/spring-boot)
- [Nacos官方文档](https://nacos.io/docs/latest/what-is-nacos/)
- [Spring Cloud Alibaba](https://spring-cloud-alibaba-group.github.io/github-pages/2022/zh-cn/index.html)
- [Docker Compose官方文档](https://docs.docker.com/compose/)
- [微服务架构设计模式](https://microservices.io/patterns/index.html)

# Nacos配置中心配置说明

本文档说明如何在Nacos中创建配置文件，用于微服务配置管理。

## 登录Nacos控制台

访问: http://localhost:8080 (通过 Docker Compose 映射)
- 用户名: nacos
- 密码: nacos

注: Nacos Server 实际端口是 8848，通过 Docker Compose 映射到宿主机的 8080 端口

## 创建命名空间

1. 进入"命名空间"菜单
2. 新建命名空间:
   - 命名空间名: dev
   - 命名空间ID: dev (记住这个ID,配置文件中需要使用)
   - 描述: 开发环境

## 创建配置文件

进入"配置管理" → "配置列表"，选择"dev"命名空间，创建以下配置：

### 1. user-service-dev.yaml

Data ID: `user-service-dev.yaml`
Group: `DEFAULT_GROUP`
配置格式: `YAML`
配置内容:

```yaml
# User Service配置
server:
  port: 8081

spring:
  datasource:
    url: jdbc:mysql://user-db:3306/user_db?useSSL=false&serverTimezone=UTC
    username: user_user
    password: user_pass
    driver-class-name: com.mysql.cj.jdbc.Driver
    hikari:
      maximum-pool-size: 20
      minimum-idle: 5

  jpa:
    hibernate:
      ddl-auto: update
    show-sql: false

# JWT配置
jwt:
  secret: your-256-bit-secret-key-here-must-be-long-enough-for-HS512-algorithm
  expiration: 86400000  # 24小时

# 自定义配置(用于演示配置动态刷新)
app:
  name: 用户管理服务
  version: 2.1.0
  description: 提供用户管理和JWT认证功能

  # 功能开关配置 (演示 @ConfigurationProperties Map类型)
  features:
    cache-enabled: true
    async-enabled: false

  # 应用设置 (演示复杂对象配置)
  settings:
    max-login-attempts: "5"
    session-timeout: "3600"
    enable-email-notification: "true"

logging:
  level:
    com.zjgsu.user: INFO
```

### 2. gateway-service-dev.yaml

Data ID: `gateway-service-dev.yaml`
Group: `DEFAULT_GROUP`
配置格式: `YAML`
配置内容:

```yaml
# Gateway Service配置
server:
  port: 8080

spring:
  cloud:
    gateway:
      routes:
        - id: user-service-route
          uri: lb://user-service
          predicates:
            - Path=/api/users/**,/api/auth/**,/api/config/**

        - id: todo-service-route
          uri: lb://todo-service
          predicates:
            - Path=/api/todos/**

      globalcors:
        cors-configurations:
          '[/**]':
            allowedOriginPatterns: "*"
            allowedMethods:
              - GET
              - POST
              - PUT
              - DELETE
              - PATCH
            allowedHeaders: "*"
            allowCredentials: true
            maxAge: 3600

# JWT配置
jwt:
  secret: your-256-bit-secret-key-here-must-be-long-enough-for-HS512-algorithm
  expiration: 86400000

logging:
  level:
    com.zjgsu.gateway: INFO
    org.springframework.cloud.gateway: INFO
```

### 3. todo-service-dev.yaml

Data ID: `todo-service-dev.yaml`
Group: `DEFAULT_GROUP`
配置格式: `YAML`
配置内容:

```yaml
# Todo Service配置
server:
  port: 8082

spring:
  datasource:
    url: jdbc:mysql://todo-db:3306/todo_db?useSSL=false&serverTimezone=UTC
    username: todo_user
    password: todo_pass
    driver-class-name: com.mysql.cj.jdbc.Driver
    hikari:
      maximum-pool-size: 20
      minimum-idle: 5

  jpa:
    hibernate:
      ddl-auto: update
    show-sql: false

  cloud:
    loadbalancer:
      ribbon:
        enabled: false
      cache:
        enabled: true
        ttl: 35s
        capacity: 256

# Feign配置
feign:
  client:
    config:
      default:
        connectTimeout: 3000
        readTimeout: 5000
      user-service:
        connectTimeout: 2000
        readTimeout: 3000
  circuitbreaker:
    enabled: true

# Resilience4j熔断器配置
resilience4j:
  circuitbreaker:
    instances:
      user-service:
        failure-rate-threshold: 50
        slow-call-rate-threshold: 50
        slow-call-duration-threshold: 2s
        sliding-window-type: COUNT_BASED
        sliding-window-size: 10
        minimum-number-of-calls: 5
        wait-duration-in-open-state: 10s
        permitted-number-of-calls-in-half-open-state: 3
        automatic-transition-from-open-to-half-open-enabled: true
        register-health-indicator: true

  retry:
    instances:
      user-service:
        max-attempts: 3
        wait-duration: 500ms
        enable-exponential-backoff: true
        exponential-backoff-multiplier: 2
        retry-exceptions:
          - java.net.ConnectException
          - java.net.SocketTimeoutException

# 自定义配置(用于演示配置动态刷新)
app:
  name: 待办事项管理服务
  version: 2.1.0
  description: 提供待办事项CRUD功能和OpenFeign集成

  # 功能开关配置 (演示 @ConfigurationProperties Map类型)
  features:
    cache-enabled: false
    async-enabled: true

  # 应用设置 (演示复杂对象配置)
  settings:
    max-todos-per-user: "100"
    enable-notifications: "false"

logging:
  level:
    com.zjgsu.todoservice: INFO
    com.zjgsu.todoservice.client: DEBUG
```

## 配置读取方式

本项目演示了两种配置读取方式：

### 方式1: @Value (简单配置)

```java
@Value("${app.name}")
private String appName;
```

**优点**: 简单直观，适合单个配置项
**缺点**: 不支持复杂对象（List/Map），需要逐个声明

**测试接口**: `GET /api/config/info`

### 方式2: @ConfigurationProperties (推荐)

```java
@Data
@Component
@ConfigurationProperties(prefix = "app")
@RefreshScope
public class AppConfig {
    private String name;
    private String version;
    private Map<String, Boolean> features;
    private Map<String, String> settings;
}
```

**优点**:
- 类型安全，自动类型转换
- 支持复杂对象（List, Map等）
- 配置集中管理，便于维护
- IDE 自动补全和提示

**测试接口**: `GET /api/config/info-advanced`

### 对比两种方式

访问 `GET /api/config/comparison` 可以查看两种方式的详细对比和示例输出。

## 配置动态刷新

本项目实现了两种配置刷新机制：

### 1. @RefreshScope 注解

```java
@RestController
@RefreshScope  // 配置变更时自动刷新
public class ConfigController {
    // ...
}
```

当 Nacos 配置变更时，标注了 `@RefreshScope` 的 Bean 会自动刷新，无需重启服务。

### 2. 配置监听器

项目实现了 `NacosConfigListener` 来监听配置变更事件：

```java
@Component
public class NacosConfigListener implements ApplicationListener<RefreshScopeRefreshedEvent> {
    @Override
    public void onApplicationEvent(RefreshScopeRefreshedEvent event) {
        // 配置刷新后的业务逻辑
        log.info("配置已刷新: {}", appConfig.getVersion());
    }
}
```

**应用场景**:
- 配置变更后重新加载缓存
- 动态调整限流阈值
- 更新业务规则
- 通知其他组件配置已更新

**查看日志**: 修改配置后，在服务日志中可以看到详细的配置刷新信息。

## 测试配置动态刷新

### 步骤1: 查看当前配置

```bash
# 方式1: 使用 @Value
curl http://localhost:8081/api/config/info

# 方式2: 使用 @ConfigurationProperties (推荐)
curl http://localhost:8081/api/config/info-advanced

# 对比两种方式
curl http://localhost:8081/api/config/comparison
```

### 步骤2: 修改配置

1. 访问 Nacos 控制台: http://localhost:8080
2. 进入「配置管理」→「配置列表」
3. 选择 `dev` 命名空间
4. 找到 `user-service-dev.yaml` 并点击「编辑」
5. 修改配置（例如修改 `app.version: 2.1.1` 或添加新的 feature）
6. 点击「发布」

### 步骤3: 验证配置已刷新

```bash
# 等待几秒后，再次访问
curl http://localhost:8081/api/config/info-advanced
```

### 步骤4: 查看监听器日志

```bash
# 查看服务日志，可以看到配置刷新事件
docker-compose logs -f user-service

# 输出示例:
# ============================================================
# 📢 检测到配置刷新事件！
# ============================================================
# 🔧 当前配置信息:
#   应用名称: 用户管理服务
#   应用版本: 2.1.1
#   功能开关:
#     cache-enabled = true
# ✅ 配置刷新完成，无需重启服务
```

## 共享配置（可选）

如果多个服务需要共享某些配置（如数据库、Redis等），可以使用共享配置功能。

### 创建共享配置

在 Nacos 控制台创建共享配置文件：

**Data ID**: `common-mysql.yaml`
**Group**: `COMMON_GROUP`
**配置内容**:

```yaml
spring:
  datasource:
    driver-class-name: com.mysql.cj.jdbc.Driver
    hikari:
      maximum-pool-size: 20
      minimum-idle: 5
      connection-timeout: 30000
      idle-timeout: 600000
```

### 启用共享配置

在 `bootstrap.yml` 中取消注释：

```yaml
spring:
  cloud:
    nacos:
      config:
        shared-configs:
          - data-id: common-mysql.yaml
            group: COMMON_GROUP
            refresh: true
```

## 配置优先级

从高到低：

1. 命令行参数 (最高)
2. Java系统属性 (`-Dkey=value`)
3. `application-{profile}.yml` (本地环境特定配置)
4. `{prefix}-{profile}.{extension}` (Nacos远程环境特定配置)
5. `{prefix}.{extension}` (Nacos远程默认配置)
6. `extension-configs` (Nacos扩展配置)
7. `shared-configs` (Nacos共享配置，最低)

## 配置 API 端点

本项目提供以下配置相关 API:

| 端点 | 方法 | 说明 | 示例 |
|------|------|------|------|
| `/api/config/info` | GET | 获取配置(@Value方式) | `curl http://localhost:8081/api/config/info` |
| `/api/config/info-advanced` | GET | 获取配置(@ConfigurationProperties) | `curl http://localhost:8081/api/config/info-advanced` |
| `/api/config/comparison` | GET | 对比两种配置读取方式 | `curl http://localhost:8081/api/config/comparison` |
| `/actuator/refresh` | POST | 手动触发配置刷新 | `curl -X POST http://localhost:8081/actuator/refresh` |

## 最佳实践

1. **配置分离**: 敏感信息使用环境变量，业务配置使用 Nacos
2. **版本管理**: 充分利用 Nacos 的配置历史和回滚功能
3. **命名规范**: 配置 Data ID 遵循 `{service-name}-{profile}.{extension}` 格式
4. **环境隔离**: 使用命名空间隔离不同环境（dev/test/prod）
5. **推荐方式**: 优先使用 `@ConfigurationProperties` 而非 `@Value`
6. **配置监听**: 重要配置变更时使用监听器执行相应业务逻辑
7. **共享配置**: 多服务共用的配置使用 shared-configs
8. **审计追踪**: 在 Nacos 控制台记录配置变更原因

## 注意事项

1. 确保所有服务的 `spring.application.name` 与配置文件的 Data ID 前缀一致
2. `namespace` 必须使用创建时的命名空间ID (dev)
3. 敏感信息建议使用环境变量或加密存储
4. 配置变更后，使用 `@RefreshScope` 注解的Bean会自动刷新
5. 配置监听器中避免执行耗时操作，可能会影响服务性能
6. 修改配置前建议先备份或使用 Nacos 的克隆功能

## 故障排查

### 配置未刷新

1. 检查 `@RefreshScope` 注解是否添加
2. 检查 `refresh-enabled: true` 配置
3. 检查网络连接到 Nacos Server
4. 查看服务日志是否有错误信息
5. 尝试手动调用 `/actuator/refresh` 端点

### 配置读取失败

1. 检查 Data ID 和 Group 是否匹配
2. 检查命名空间 ID 是否正确
3. 确认配置文件格式正确（YAML缩进）
4. 查看 Nacos Server 日志

### 监听器未触发

1. 确认配置已在 Nacos 控制台发布
2. 检查监听器类是否标注 `@Component`
3. 查看服务日志确认监听器已加载

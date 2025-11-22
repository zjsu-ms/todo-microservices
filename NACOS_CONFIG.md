# Nacos配置中心配置说明

本文档说明如何在Nacos中创建配置文件，用于微服务配置管理。

## 登录Nacos控制台

访问: http://localhost:8848/nacos
- 用户名: nacos
- 密码: nacos

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

logging:
  level:
    com.zjgsu.todoservice: INFO
    com.zjgsu.todoservice.client: DEBUG
```

## 测试配置动态刷新

1. 启动所有服务后，访问配置接口:
   - http://localhost:9000/api/config/info (通过网关)
   - 或直接访问: http://localhost:8081/api/config/info

2. 在Nacos控制台修改配置:
   - 修改 `app.name` 或 `app.description`
   - 点击"发布"

3. 等待几秒后，再次访问配置接口，配置已自动刷新！

## 配置优先级

1. bootstrap.yml (最先加载)
2. Nacos远程配置 (覆盖本地配置)
3. application.yml (本地配置)
4. 命令行参数 (最高优先级)

## 注意事项

1. 确保所有服务的 `spring.application.name` 与配置文件的 Data ID 前缀一致
2. `namespace` 必须使用创建时的命名空间ID (dev)
3. 敏感信息建议使用环境变量或加密存储
4. 配置变更后，使用 `@RefreshScope` 注解的Bean会自动刷新

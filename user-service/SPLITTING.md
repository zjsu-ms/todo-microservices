# Todo 单体应用拆分为微服务方案

## 📋 拆分目标

将单体todo应用拆分为两个独立的微服务：

- **user-service** (用户服务) - 端口 8081, 数据库 user_db
- **todo-service** (待办事项服务) - 端口 8082, 数据库 todo_db

## 🎯 拆分原则

1. **业务领域拆分**：按照DDD（领域驱动设计）原则，用户和待办事项是两个独立的业务域
2. **数据库独立**：每个服务拥有独立的数据库，避免共享数据库
3. **服务间通信**：通过HTTP/REST API进行服务间通信
4. **渐进式拆分**：保持原有API契约不变，降低客户端影响

## 🏗️ 目标架构

```
┌─────────────────────────────────────────────────────────────┐
│                        客户端应用                            │
└─────────────────┬───────────────────────┬───────────────────┘
                  │                       │
                  │                       │
          ┌───────▼───────┐      ┌───────▼───────┐
          │ user-service  │      │ todo-service  │
          │   :8081       │◄─────┤   :8082       │
          └───────┬───────┘ HTTP └───────┬───────┘
                  │                       │
          ┌───────▼───────┐      ┌───────▼───────┐
          │   user_db     │      │   todo_db     │
          │   (MySQL)     │      │   (MySQL)     │
          └───────────────┘      └───────────────┘
```

## 📁 拆分后的项目结构

```
todo-microservices/
├── docker-compose.yml           # Docker编排文件
├── README.md                     # 项目说明文档
│
├── user-service/                 # 用户服务
│   ├── src/
│   │   └── main/
│   │       ├── java/com/zjgsu/user/
│   │       │   ├── UserServiceApplication.java
│   │       │   ├── model/
│   │       │   │   └── User.java
│   │       │   ├── repository/
│   │       │   │   └── UserRepository.java
│   │       │   ├── service/
│   │       │   │   └── UserService.java
│   │       │   ├── controller/
│   │       │   │   └── UserController.java
│   │       │   ├── common/
│   │       │   │   └── ApiResponse.java
│   │       │   └── exception/
│   │       │       ├── ResourceNotFoundException.java
│   │       │       └── GlobalExceptionHandler.java
│   │       └── resources/
│   │           ├── application.yml
│   │           └── db/
│   │               └── schema.sql
│   ├── Dockerfile
│   └── pom.xml
│
└── todo-service/                 # Todo服务
    ├── src/
    │   └── main/
    │       ├── java/com/zjgsu/todoservice/
    │       │   ├── TodoServiceApplication.java
    │       │   ├── model/
    │       │   │   └── Todo.java
    │       │   ├── repository/
    │       │   │   └── TodoRepository.java
    │       │   ├── service/
    │       │   │   └── TodoService.java
    │       │   ├── controller/
    │       │   │   └── TodoController.java
    │       │   ├── common/
    │       │   │   └── ApiResponse.java
    │       │   ├── config/
    │       │   │   └── RestTemplateConfig.java
    │       │   └── exception/
    │       │       ├── ResourceNotFoundException.java
    │       │       └── GlobalExceptionHandler.java
    │       └── resources/
    │           ├── application.yml
    │           └── db/
    │               └── schema.sql
    ├── Dockerfile
    └── pom.xml
```

## 🔧 详细拆分步骤

### 步骤1：创建user-service项目

#### 1.1 复制项目结构

```bash
# 在projects目录下创建微服务目录
cd projects
mkdir -p todo-microservices/user-service

# 复制完整的todo项目到user-service
cp -r todo/* todo-microservices/user-service/
```

#### 1.2 修改包名和项目配置

编辑 `user-service/pom.xml`：

```xml
<groupId>com.zjgsu</groupId>
<artifactId>user-service</artifactId>
<version>1.0.0</version>
<name>user-service</name>
<description>User Management Microservice</description>
```

#### 1.3 保留User相关代码，删除Todo相关代码

**保留的文件**：
- `model/User.java` ✅
- `repository/UserRepository.java` ✅
- `service/UserService.java` ✅
- `controller/UserController.java` ✅
- `common/ApiResponse.java` ✅
- `exception/` 目录下所有文件 ✅

**删除的文件**：
```bash
cd user-service/src/main/java/com/zjgsu/todo
rm model/Todo.java
rm repository/TodoRepository.java
rm service/TodoService.java
rm controller/TodoController.java
rm controller/UserTodoController.java
```

#### 1.4 重命名包

将包名从 `com.zjgsu.todo` 改为 `com.zjgsu.user`：

```bash
cd user-service/src/main/java/com/zjgsu
mv todo user
```

然后使用IDE的重构功能批量替换包名（或使用sed）：

```bash
find . -type f -name "*.java" -exec sed -i 's/com\.zjgsu\.todo/com.zjgsu.user/g' {} +
```

#### 1.5 修改主类名

`UserServiceApplication.java`:

```java
package com.zjgsu.user;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class UserServiceApplication {
    public static void main(String[] args) {
        SpringApplication.run(UserServiceApplication.class, args);
    }
}
```

#### 1.6 修改配置文件

`user-service/src/main/resources/application.yml`:

```yaml
server:
  port: 8081

spring:
  application:
    name: user-service

  datasource:
    url: jdbc:mysql://localhost:3306/user_db?useSSL=false&serverTimezone=UTC&characterEncoding=utf8mb4&useUnicode=true
    username: user_user
    password: user_pass
    driver-class-name: com.mysql.cj.jdbc.Driver

  jpa:
    hibernate:
      ddl-auto: update
    show-sql: true
    properties:
      hibernate:
        format_sql: true
        dialect: org.hibernate.dialect.MySQL8Dialect

logging:
  level:
    com.zjgsu.user: INFO
    org.springframework.web: INFO
```

`user-service/src/main/resources/application-prod.yml`:

```yaml
spring:
  datasource:
    url: ${DB_URL:jdbc:mysql://user-db:3306/user_db?useSSL=false&serverTimezone=UTC}
    username: ${DB_USERNAME:user_user}
    password: ${DB_PASSWORD:user_pass}

  jpa:
    hibernate:
      ddl-auto: validate
    show-sql: false

logging:
  level:
    com.zjgsu.user: WARN
```

#### 1.7 创建Dockerfile

`user-service/Dockerfile`:

```dockerfile
FROM eclipse-temurin:25-jre

WORKDIR /app

# 复制JAR文件
COPY target/user-service-1.0.0.jar app.jar

# 暴露端口
EXPOSE 8081

# 运行应用
ENTRYPOINT ["java", "-jar", "app.jar"]
```

---

### 步骤2：创建todo-service项目

#### 2.1 复制项目结构

```bash
# 复制完整的todo项目到todo-service
cp -r todo/* todo-microservices/todo-service/
```

#### 2.2 修改包名和项目配置

编辑 `todo-service/pom.xml`：

```xml
<groupId>com.zjgsu</groupId>
<artifactId>todo-service</artifactId>
<version>1.0.0</version>
<name>todo-service</name>
<description>Todo Management Microservice</description>
```

#### 2.3 保留Todo相关代码，删除User相关代码

**保留的文件**：
- `model/Todo.java` ✅
- `repository/TodoRepository.java` ✅
- `service/TodoService.java` ✅
- `controller/TodoController.java` ✅
- `common/ApiResponse.java` ✅
- `exception/` 目录下所有文件 ✅

**删除的文件**：
```bash
cd todo-service/src/main/java/com/zjgsu/todo
rm model/User.java
rm repository/UserRepository.java
rm service/UserService.java
rm controller/UserController.java
rm controller/UserTodoController.java
```

#### 2.4 重命名包

将包名从 `com.zjgsu.todo` 改为 `com.zjgsu.todoservice`：

```bash
cd todo-service/src/main/java/com/zjgsu
mv todo todoservice
find . -type f -name "*.java" -exec sed -i 's/com\.zjgsu\.todo/com.zjgsu.todoservice/g' {} +
```

#### 2.5 修改主类名

`TodoServiceApplication.java`:

```java
package com.zjgsu.todoservice;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.web.client.RestTemplate;

@SpringBootApplication
public class TodoServiceApplication {

    @Bean
    public RestTemplate restTemplate() {
        return new RestTemplate();
    }

    public static void main(String[] args) {
        SpringApplication.run(TodoServiceApplication.class, args);
    }
}
```

#### 2.6 修改TodoService实现服务间调用

`TodoService.java`:

```java
package com.zjgsu.todoservice.service;

import com.zjgsu.todoservice.exception.ResourceNotFoundException;
import com.zjgsu.todoservice.model.Todo;
import com.zjgsu.todoservice.repository.TodoRepository;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;

import java.util.List;
import java.util.Map;
import java.util.Optional;

@Service
public class TodoService {
    private final TodoRepository todoRepository;
    private final RestTemplate restTemplate;

    @Value("${user-service.url}")
    private String userServiceUrl;

    public TodoService(TodoRepository todoRepository, RestTemplate restTemplate) {
        this.todoRepository = todoRepository;
        this.restTemplate = restTemplate;
    }

    @PostConstruct
    public void init() {
        if (todoRepository.count() == 0) {
            createTodo(new Todo(null, "学习Spring Boot", "完成基础教程", 1L));
            createTodo(new Todo(null, "实现微服务", "拆分单体应用", 1L));
        }
    }

    public List<Todo> findAll() {
        return todoRepository.findAll();
    }

    public List<Todo> findByUserId(Long userId) {
        return todoRepository.findByUserId(userId);
    }

    public Optional<Todo> findById(Long id) {
        return todoRepository.findById(id);
    }

    @Transactional
    public Todo createTodo(Todo todo) {
        // 调用用户服务验证用户是否存在
        if (todo.getUserId() != null) {
            verifyUserExists(todo.getUserId());
        }
        return todoRepository.save(todo);
    }

    @Transactional
    public Todo updateTodo(Long id, Todo todo) {
        if (!todoRepository.existsById(id)) {
            throw new ResourceNotFoundException("Todo", id);
        }
        // 验证用户存在
        if (todo.getUserId() != null) {
            verifyUserExists(todo.getUserId());
        }
        todo.setId(id);
        return todoRepository.save(todo);
    }

    @Transactional
    public boolean deleteTodo(Long id) {
        if (!todoRepository.existsById(id)) {
            throw new ResourceNotFoundException("Todo", id);
        }
        todoRepository.deleteById(id);
        return true;
    }

    @Transactional
    public Todo toggleComplete(Long id) {
        Todo todo = todoRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Todo", id));
        todo.setCompleted(!todo.getCompleted());
        return todoRepository.save(todo);
    }

    /**
     * 调用用户服务验证用户是否存在
     */
    private void verifyUserExists(Long userId) {
        String url = userServiceUrl + "/api/users/" + userId;
        try {
            restTemplate.getForObject(url, Map.class);
        } catch (HttpClientErrorException.NotFound e) {
            throw new ResourceNotFoundException("User", userId);
        } catch (Exception e) {
            throw new RuntimeException("Failed to verify user: " + e.getMessage());
        }
    }
}
```

#### 2.7 修改配置文件

`todo-service/src/main/resources/application.yml`:

```yaml
server:
  port: 8082

spring:
  application:
    name: todo-service

  datasource:
    url: jdbc:mysql://localhost:3306/todo_db?useSSL=false&serverTimezone=UTC&characterEncoding=utf8mb4&useUnicode=true
    username: todo_user
    password: todo_pass
    driver-class-name: com.mysql.cj.jdbc.Driver

  jpa:
    hibernate:
      ddl-auto: update
    show-sql: true
    properties:
      hibernate:
        format_sql: true
        dialect: org.hibernate.dialect.MySQL8Dialect

# 用户服务地址配置
user-service:
  url: http://localhost:8081

logging:
  level:
    com.zjgsu.todoservice: INFO
    org.springframework.web: INFO
```

`todo-service/src/main/resources/application-prod.yml`:

```yaml
spring:
  datasource:
    url: ${DB_URL:jdbc:mysql://todo-db:3306/todo_db?useSSL=false&serverTimezone=UTC}
    username: ${DB_USERNAME:todo_user}
    password: ${DB_PASSWORD:todo_pass}

  jpa:
    hibernate:
      ddl-auto: validate
    show-sql: false

# 用户服务地址（Docker环境）
user-service:
  url: ${USER_SERVICE_URL:http://user-service:8081}

logging:
  level:
    com.zjgsu.todoservice: WARN
```

#### 2.8 创建Dockerfile

`todo-service/Dockerfile`:

```dockerfile
FROM eclipse-temurin:25-jre

WORKDIR /app

# 复制JAR文件
COPY target/todo-service-1.0.0.jar app.jar

# 暴露端口
EXPOSE 8082

# 运行应用
ENTRYPOINT ["java", "-jar", "app.jar"]
```

---

### 步骤3：创建Docker Compose配置

在 `todo-microservices/` 根目录创建 `docker-compose.yml`：

```yaml
version: '3.8'

services:
  # 用户服务数据库
  user-db:
    image: mysql:8.4
    container_name: user-db
    environment:
      MYSQL_ROOT_PASSWORD: root_password
      MYSQL_DATABASE: user_db
      MYSQL_USER: user_user
      MYSQL_PASSWORD: user_pass
    ports:
      - "3307:3306"
    volumes:
      - user-data:/var/lib/mysql
    healthcheck:
      test: ["CMD", "mysqladmin", "ping", "-h", "localhost", "-u", "root", "-proot_password"]
      interval: 10s
      timeout: 5s
      retries: 5
    command:
      - --character-set-server=utf8mb4
      - --collation-server=utf8mb4_unicode_ci
    networks:
      - todo-network

  # Todo服务数据库
  todo-db:
    image: mysql:8.4
    container_name: todo-db
    environment:
      MYSQL_ROOT_PASSWORD: root_password
      MYSQL_DATABASE: todo_db
      MYSQL_USER: todo_user
      MYSQL_PASSWORD: todo_pass
    ports:
      - "3308:3306"
    volumes:
      - todo-data:/var/lib/mysql
    healthcheck:
      test: ["CMD", "mysqladmin", "ping", "-h", "localhost", "-u", "root", "-proot_password"]
      interval: 10s
      timeout: 5s
      retries: 5
    command:
      - --character-set-server=utf8mb4
      - --collation-server=utf8mb4_unicode_ci
    networks:
      - todo-network

  # 用户服务
  user-service:
    build:
      context: ./user-service
      dockerfile: Dockerfile
    container_name: user-service
    environment:
      SPRING_PROFILES_ACTIVE: prod
      DB_URL: jdbc:mysql://user-db:3306/user_db?useSSL=false&serverTimezone=UTC&characterEncoding=utf8mb4&useUnicode=true
      DB_USERNAME: user_user
      DB_PASSWORD: user_pass
    ports:
      - "8081:8081"
    depends_on:
      user-db:
        condition: service_healthy
    networks:
      - todo-network
    restart: unless-stopped

  # Todo服务
  todo-service:
    build:
      context: ./todo-service
      dockerfile: Dockerfile
    container_name: todo-service
    environment:
      SPRING_PROFILES_ACTIVE: prod
      DB_URL: jdbc:mysql://todo-db:3306/todo_db?useSSL=false&serverTimezone=UTC&characterEncoding=utf8mb4&useUnicode=true
      DB_USERNAME: todo_user
      DB_PASSWORD: todo_pass
      USER_SERVICE_URL: http://user-service:8081
    ports:
      - "8082:8082"
    depends_on:
      user-service:
        condition: service_started
      todo-db:
        condition: service_healthy
    networks:
      - todo-network
    restart: unless-stopped

volumes:
  user-data:
    driver: local
  todo-data:
    driver: local

networks:
  todo-network:
    driver: bridge
```

---

## 🚀 构建和部署

### 本地开发环境

#### 1. 构建JAR包

```bash
# 构建user-service
cd user-service
./mvnw clean package -DskipTests

# 构建todo-service
cd ../todo-service
./mvnw clean package -DskipTests
```

#### 2. 本地运行（不使用Docker）

需要先安装并启动MySQL，创建数据库：

```sql
CREATE DATABASE user_db CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
CREATE DATABASE todo_db CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
```

然后分别运行服务：

```bash
# 终端1：启动user-service
cd user-service
./mvnw spring-boot:run

# 终端2：启动todo-service
cd todo-service
./mvnw spring-boot:run
```

### Docker部署

#### 1. 构建镜像并启动服务

```bash
cd todo-microservices

# 构建并启动所有服务
docker-compose up -d --build

# 查看服务状态
docker-compose ps

# 查看日志
docker-compose logs -f
```

#### 2. 验证部署

```bash
# 测试用户服务
curl http://localhost:8081/api/users

# 测试Todo服务
curl http://localhost:8082/api/todos
```

#### 3. 停止服务

```bash
# 停止所有服务
docker-compose down

# 停止并删除数据卷
docker-compose down -v
```

---

## 🧪 测试服务间通信

创建测试脚本 `test-services.sh`：

```bash
#!/bin/bash

echo "=== 测试微服务拆分 ==="

echo -e "\n1. 测试用户服务 - 创建用户"
USER_RESPONSE=$(curl -s -X POST http://localhost:8081/api/users \
  -H "Content-Type: application/json" \
  -d '{"username":"张三","email":"zhangsan@example.com"}')
echo $USER_RESPONSE

# 提取用户ID
USER_ID=$(echo $USER_RESPONSE | grep -o '"id":[0-9]*' | grep -o '[0-9]*')
echo "创建的用户ID: $USER_ID"

echo -e "\n2. 测试用户服务 - 获取所有用户"
curl -s http://localhost:8081/api/users | jq '.'

echo -e "\n3. 测试Todo服务 - 创建Todo（关联到存在的用户）"
curl -s -X POST http://localhost:8082/api/todos \
  -H "Content-Type: application/json" \
  -d "{\"title\":\"学习微服务\",\"description\":\"完成拆分实践\",\"userId\":$USER_ID}" | jq '.'

echo -e "\n4. 测试Todo服务 - 获取所有Todo"
curl -s http://localhost:8082/api/todos | jq '.'

echo -e "\n5. 测试服务间通信 - 创建Todo（用户不存在，应该失败）"
curl -s -X POST http://localhost:8082/api/todos \
  -H "Content-Type: application/json" \
  -d '{"title":"测试","userId":999}' | jq '.'

echo -e "\n=== 测试完成 ==="
```

运行测试：

```bash
chmod +x test-services.sh
./test-services.sh
```

---

## 📊 架构对比

### 单体应用 vs 微服务

| 维度 | 单体应用 | 微服务架构 |
|------|----------|------------|
| **部署** | 一次性部署，所有功能在一个应用 | 独立部署，每个服务可单独更新 |
| **扩展** | 整体扩展，资源利用率低 | 按需扩展，只扩展高负载服务 |
| **数据库** | 共享数据库（todo_db） | 独立数据库（user_db, todo_db） |
| **服务端口** | 单一端口（8080） | 多端口（8081, 8082） |
| **故障隔离** | 一个模块故障可能影响整体 | 服务隔离，故障影响范围小 |
| **开发团队** | 团队共用代码库 | 团队可独立开发不同服务 |
| **技术栈** | 统一技术栈 | 可选择不同技术栈 |
| **复杂度** | 简单，易于理解 | 复杂，需要服务治理 |

---

## 🔍 常见问题

### Q1: 为什么Todo服务无法调用User服务？

**检查清单**：

1. User服务是否已启动？
   ```bash
   curl http://localhost:8081/api/users
   ```

2. Docker网络是否正确？
   ```bash
   docker network inspect todo-microservices_todo-network
   ```

3. 配置的URL是否正确？
   - 本地开发：`http://localhost:8081`
   - Docker环境：`http://user-service:8081`

### Q2: 数据库连接失败？

**解决方法**：

1. 检查数据库容器是否启动：
   ```bash
   docker-compose ps
   ```

2. 查看数据库日志：
   ```bash
   docker-compose logs user-db
   docker-compose logs todo-db
   ```

3. 验证数据库可连接：
   ```bash
   docker exec -it user-db mysql -u user_user -puser_pass -e "SHOW DATABASES;"
   ```

### Q3: JAR包构建失败？

**常见原因**：

1. 包名修改后，某些import未更新
2. 删除的类仍被引用

**解决方法**：

使用IDE的"Find Usages"功能查找所有引用，或使用Maven验证：

```bash
./mvnw clean compile
```

### Q4: 如何调试微服务？

**方法1：查看日志**

```bash
# Docker环境
docker-compose logs -f user-service
docker-compose logs -f todo-service

# 本地运行
./mvnw spring-boot:run -Dspring-boot.run.arguments="--logging.level.com.zjgsu=DEBUG"
```

**方法2：进入容器调试**

```bash
docker exec -it user-service /bin/sh
docker exec -it todo-service /bin/sh
```

---

## 📝 下一步

拆分完成后，可以考虑以下改进：

1. **服务注册与发现**：集成Nacos或Eureka，实现动态服务发现
2. **API网关**：添加Spring Cloud Gateway统一入口
3. **配置中心**：使用Nacos Config或Spring Cloud Config管理配置
4. **链路追踪**：集成Sleuth和Zipkin
5. **熔断降级**：使用Resilience4j实现熔断器
6. **服务监控**：集成Prometheus和Grafana

---

## 📚 参考资源

- [Spring Boot官方文档](https://spring.io/projects/spring-boot)
- [Docker Compose官方文档](https://docs.docker.com/compose/)
- [微服务架构设计模式](https://microservices.io/patterns/index.html)
- [领域驱动设计(DDD)](https://www.domainlanguage.com/ddd/)

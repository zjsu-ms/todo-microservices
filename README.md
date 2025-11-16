# Todo 微服务项目

这是将单体todo应用拆分为微服务架构的实践项目。

## 📋 项目说明

本项目将单体todo应用拆分为两个独立的微服务：

- **user-service** (用户服务) - 端口 8081, 数据库 user_db
- **todo-service** (待办事项服务) - 端口 8082, 数据库 todo_db

## 🏗️ 架构图

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

## 🚀 快速开始

### 前置要求

- JDK 17+
- Maven 3.8+
- Docker 和 Docker Compose

### 使用 Docker Compose 运行（推荐）

1. **构建JAR包**

```bash
# 构建user-service
cd user-service
./mvnw clean package -DskipTests

# 构建todo-service
cd ../todo-service
./mvnw clean package -DskipTests
```

2. **启动所有服务**

```bash
cd ..
docker-compose up -d --build
```

3. **查看服务状态**

```bash
docker-compose ps
```

4. **查看日志**

```bash
# 查看所有日志
docker-compose logs -f

# 查看特定服务日志
docker-compose logs -f user-service
docker-compose logs -f todo-service
```

5. **停止服务**

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

2. **启动服务**

```bash
# 终端1：启动user-service
cd user-service
./mvnw spring-boot:run

# 终端2：启动todo-service
cd todo-service
./mvnw spring-boot:run
```

## 🧪 测试服务

### 使用测试脚本

```bash
./test-services.sh
```

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

# 创建Todo（关联到用户1）
curl -X POST http://localhost:8082/api/todos \
  -H "Content-Type: application/json" \
  -d '{"title":"学习微服务","description":"完成拆分实践","userId":1}'
```

## 📊 API文档

### 用户服务 API (端口 8081)

| 方法 | URL | 说明 |
|------|-----|------|
| GET | `/api/users` | 获取所有用户 |
| GET | `/api/users/{id}` | 获取单个用户 |
| POST | `/api/users` | 创建用户 |
| PUT | `/api/users/{id}` | 更新用户 |
| DELETE | `/api/users/{id}` | 删除用户 |

### Todo服务 API (端口 8082)

| 方法 | URL | 说明 |
|------|-----|------|
| GET | `/api/todos` | 获取所有Todo |
| GET | `/api/todos?userId={id}` | 按用户筛选Todo |
| GET | `/api/todos/{id}` | 获取单个Todo |
| POST | `/api/todos` | 创建Todo |
| PUT | `/api/todos/{id}` | 更新Todo |
| PATCH | `/api/todos/{id}/toggle` | 切换完成状态 |
| DELETE | `/api/todos/{id}` | 删除Todo |

## 🔧 技术栈

- **Spring Boot** 3.5.6
- **Java** 25
- **Maven** 3.8+
- **MySQL** 8.4
- **Docker** & Docker Compose
- **RestTemplate** - 服务间通信

## 📁 项目结构

```
todo-microservices/
├── docker-compose.yml           # Docker编排文件
├── test-services.sh             # 测试脚本
├── README.md                    # 项目文档
│
├── user-service/                # 用户服务
│   ├── src/main/java/com/zjgsu/user/
│   ├── src/main/resources/
│   │   ├── application.yml
│   │   └── application-prod.yml
│   ├── Dockerfile
│   └── pom.xml
│
└── todo-service/                # Todo服务
    ├── src/main/java/com/zjgsu/todoservice/
    ├── src/main/resources/
    │   ├── application.yml
    │   └── application-prod.yml
    ├── Dockerfile
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

todo-service通过HTTP调用user-service验证用户存在性：

```java
// TodoService.java
private void verifyUserExists(Long userId) {
    String url = userServiceUrl + "/api/users/" + userId;
    try {
        restTemplate.getForObject(url, Map.class);
    } catch (HttpClientErrorException.NotFound e) {
        throw new ResourceNotFoundException("User", userId);
    }
}
```

## 🐛 常见问题

### Q1: 服务间调用失败？

检查：
1. User服务是否已启动？`curl http://localhost:8081/api/users`
2. Docker网络是否正确？`docker network inspect todo-microservices_todo-network`
3. 配置的URL是否正确？本地开发使用`http://localhost:8081`，Docker环境使用`http://user-service:8081`

### Q2: 数据库连接失败？

```bash
# 检查容器状态
docker-compose ps

# 查看数据库日志
docker-compose logs user-db
docker-compose logs todo-db

# 验证连接
docker exec -it user-db mysql -u user_user -puser_pass -e "SHOW DATABASES;"
```

### Q3: JAR包构建失败？

```bash
# 清理并重新构建
cd user-service
./mvnw clean compile

cd ../todo-service
./mvnw clean compile
```

## 📝 下一步

拆分完成后，可以考虑以下改进：

1. **服务注册与发现**：集成Nacos或Eureka
2. **API网关**：添加Spring Cloud Gateway
3. **配置中心**：使用Nacos Config
4. **链路追踪**：集成Sleuth和Zipkin
5. **熔断降级**：使用Resilience4j
6. **服务监控**：集成Prometheus和Grafana

## 📚 参考资源

- [拆分方案文档](../todo/SPLITTING.md)
- [Spring Boot官方文档](https://spring.io/projects/spring-boot)
- [Docker Compose官方文档](https://docs.docker.com/compose/)
- [微服务架构设计模式](https://microservices.io/patterns/index.html)

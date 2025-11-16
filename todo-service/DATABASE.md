# 数据库快速开始指南

本文档提供快速配置和使用数据库的指南。完整的教程请参考 [README.md](README.md)。

## 快速开始

### 选项1：使用H2内存数据库（推荐初学者）

无需任何配置，直接启动：

```bash
./mvnw spring-boot:run -Dspring-boot.run.profiles=dev
```

访问H2控制台：http://localhost:8080/h2-console

- **JDBC URL**: `jdbc:h2:mem:todo_db`
- **用户名**: `sa`
- **密码**: (留空)

**优点**：
- ✅ 零配置，开箱即用
- ✅ 快速开发和测试
- ✅ 每次重启数据重置

**缺点**：
- ❌ 数据不持久化
- ❌ 应用关闭后数据丢失

---

### 选项2：使用MySQL数据库（生产环境）

#### 第一步：安装MySQL

根据你的操作系统：

- **Windows**: 下载 [MySQL Installer](https://dev.mysql.com/downloads/installer/)
- **macOS**: `brew install mysql`
- **Ubuntu/Debian**: `sudo apt install mysql-server`

#### 第二步：初始化数据库

登录MySQL并执行初始化脚本：

```bash
# 登录MySQL（Windows用户可能需要使用mysql.exe的完整路径）
mysql -u root -p

# 在MySQL中执行初始化脚本
source src/main/resources/db/init.sql

# 或直接在命令行执行
mysql -u root -p < src/main/resources/db/init.sql
```

这将自动：
- 创建 `todo_db` 数据库
- 创建 `todo_user` 用户（密码：`todo_password`）
- 创建 `users` 和 `todos` 表
- 插入测试数据

#### 第三步：配置应用

编辑 `src/main/resources/application.yml`，确保数据库连接信息正确：

```yaml
spring:
  datasource:
    url: jdbc:mysql://localhost:3306/todo_db?useSSL=false&serverTimezone=UTC&characterEncoding=utf8
    username: todo_user
    password: todo_password
```

#### 第四步：启动应用

```bash
./mvnw spring-boot:run
```

或使用生产环境配置（推荐）：

```bash
# 设置环境变量
export DB_URL="jdbc:mysql://localhost:3306/todo_db?useSSL=false&serverTimezone=UTC&characterEncoding=utf8"
export DB_USERNAME="todo_user"
export DB_PASSWORD="todo_password"

# 启动应用
./mvnw spring-boot:run -Dspring-boot.run.profiles=prod
```

#### 第五步：验证连接

启动日志应显示：

```
HikariPool-1 - Starting...
HikariPool-1 - Start completed.
```

测试API：

```bash
curl http://localhost:8080/api/users
```

应该返回预置的测试用户数据。

---

## 数据库表结构

### users 表

| 字段 | 类型 | 说明 |
|------|------|------|
| id | BIGINT | 主键，自增 |
| username | VARCHAR(50) | 用户名，唯一 |
| email | VARCHAR(100) | 邮箱，唯一 |
| created_at | TIMESTAMP | 创建时间 |

### todos 表

| 字段 | 类型 | 说明 |
|------|------|------|
| id | BIGINT | 主键，自增 |
| title | VARCHAR(200) | 标题 |
| description | TEXT | 描述 |
| completed | TINYINT(1) | 是否完成 |
| user_id | BIGINT | 用户ID，外键 |
| created_at | TIMESTAMP | 创建时间 |
| updated_at | TIMESTAMP | 更新时间 |

---

## 环境配置对比

| 特性 | 开发环境 (dev) | 默认环境 | 生产环境 (prod) |
|------|---------------|----------|-----------------|
| 数据库 | H2内存数据库 | MySQL | MySQL |
| 数据持久化 | ❌ 否 | ✅ 是 | ✅ 是 |
| DDL模式 | create-drop | update | validate |
| SQL日志 | 显示 | 显示 | 隐藏 |
| H2控制台 | 启用 | 禁用 | 禁用 |
| 日志级别 | DEBUG | INFO | WARN |
| 配置方式 | 硬编码 | 硬编码 | 环境变量 |

---

## 常见问题

### 1. MySQL连接失败

**错误**：`Connection refused: localhost:3306`

**解决**：
```bash
# 检查MySQL是否运行
mysql --version

# Linux启动MySQL
sudo systemctl start mysql
sudo systemctl status mysql

# macOS启动MySQL
brew services start mysql

# Windows
# 通过"服务"管理器启动MySQL服务
```

### 2. 认证失败

**错误**：`Access denied for user 'todo_user'@'localhost'`

**解决**：重新创建用户
```sql
mysql -u root -p

DROP USER IF EXISTS 'todo_user'@'localhost';
CREATE USER 'todo_user'@'localhost' IDENTIFIED WITH mysql_native_password BY 'todo_password';
GRANT ALL PRIVILEGES ON todo_db.* TO 'todo_user'@'localhost';
FLUSH PRIVILEGES;
```

### 3. 时区错误

**错误**：`The server time zone value 'CST' is unrecognized`

**解决**：在连接URL中指定时区
```yaml
spring:
  datasource:
    url: jdbc:mysql://localhost:3306/todo_db?serverTimezone=Asia/Shanghai
```

### 4. 表已存在

**错误**：`Table 'users' already exists`

**解决**：
- 开发环境：使用 `ddl-auto: create-drop` 每次重建表
- 生产环境：使用 `ddl-auto: validate` 不修改表结构
- 手动删除表：
  ```sql
  USE todo_db;
  DROP TABLE IF EXISTS todos;
  DROP TABLE IF EXISTS users;
  ```

### 5. 中文乱码

**解决**：确保字符编码正确
```yaml
spring:
  datasource:
    url: jdbc:mysql://localhost:3306/todo_db?characterEncoding=utf8
```

并确保数据库使用utf8mb4：
```sql
ALTER DATABASE todo_db CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
```

---

## 数据库管理工具

推荐使用以下工具管理MySQL数据库：

1. **MySQL Workbench**（官方GUI工具）
   - 下载：https://dev.mysql.com/downloads/workbench/
   - 功能：图形化管理、查询编辑、ER图设计

2. **DBeaver**（通用数据库工具）
   - 下载：https://dbeaver.io/
   - 功能：支持多种数据库、免费开源

3. **DataGrip**（JetBrains出品）
   - 下载：https://www.jetbrains.com/datagrip/
   - 功能：强大的SQL IDE，学生免费

4. **IntelliJ IDEA Ultimate**（内置数据库工具）
   - 集成在IDEA中，无需额外安装
   - 功能：查询、ER图、数据导入导出

---

## 下一步

完成数据库配置后，你可以：

1. ✅ 测试所有API接口，验证数据持久化
2. ✅ 查看H2控制台（开发环境）或使用MySQL工具查询数据
3. ✅ 尝试修改数据后重启应用，验证数据是否保存
4. ✅ 学习JPA高级特性：分页、复杂查询、级联操作
5. ✅ 添加数据库迁移工具：Flyway或Liquibase

完整教程请参考：[README.md](README.md)

---

## 参考资源

- [Spring Data JPA文档](https://spring.io/projects/spring-data-jpa)
- [Hibernate ORM文档](https://hibernate.org/orm/documentation/)
- [MySQL官方文档](https://dev.mysql.com/doc/)
- [H2数据库文档](https://www.h2database.com/html/main.html)

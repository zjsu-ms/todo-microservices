# 变更日志

## [Unreleased] - 2025-01-XX

### Added - 数据库持久化支持

#### 依赖项
- ✅ spring-boot-starter-data-jpa - JPA和Hibernate支持
- ✅ mysql-connector-j - MySQL JDBC驱动
- ✅ h2 - H2内存数据库（开发环境）

#### 实体类 (Entity)
- ✅ User.java - 添加JPA注解 (@Entity, @Table, @Id, @GeneratedValue, @Column, @PrePersist)
- ✅ Todo.java - 添加JPA注解和自动时间戳管理 (@PreUpdate)

#### 数据访问层 (Repository)
- ✅ UserRepository.java - 用户数据访问接口
  - findByUsername - 按用户名查询
  - findByEmail - 按邮箱查询
  - existsByUsername - 检查用户名是否存在
  - existsByEmail - 检查邮箱是否存在

- ✅ TodoRepository.java - Todo数据访问接口
  - findByUserId - 按用户ID查询Todo列表
  - findByUserIdAndCompleted - 按用户ID和完成状态查询
  - findByCompleted - 按完成状态查询
  - findByTitleContaining - 标题模糊查询

#### 业务逻辑层 (Service)
- ✅ UserService.java - 重构为使用Repository
  - 从ConcurrentHashMap迁移到JpaRepository
  - 添加@Transactional事务管理
  - 添加用户名/邮箱唯一性验证
  - @PostConstruct初始化测试数据（仅当数据库为空时）

- ✅ TodoService.java - 重构为使用Repository
  - 从ConcurrentHashMap迁移到JpaRepository
  - 添加@Transactional事务管理
  - 保留用户存在性验证
  - @PostConstruct初始化测试数据（仅当数据库为空时）

#### 配置文件
- ✅ application.yml - 主配置（MySQL默认配置）
  - 数据源配置：localhost:3306/todo_db
  - JPA配置：ddl-auto=update, show-sql=true
  - Hibernate方言：MySQL8Dialect
  - SQL日志配置

- ✅ application-dev.yml - 开发环境配置（H2内存数据库）
  - H2内存数据库：jdbc:h2:mem:todo_db
  - H2控制台：启用，路径/h2-console
  - JPA配置：ddl-auto=create-drop
  - DEBUG日志级别

- ✅ application-prod.yml - 生产环境配置（环境变量）
  - 环境变量配置：${DB_URL}, ${DB_USERNAME}, ${DB_PASSWORD}
  - JPA配置：ddl-auto=validate（安全）
  - SQL日志关闭
  - WARN日志级别

#### 数据库脚本
- ✅ src/main/resources/db/init.sql - MySQL初始化脚本
  - 创建todo_db数据库（utf8mb4字符集）
  - 创建todo_user用户并授权
  - 创建users表（id, username, email, created_at）
  - 创建todos表（id, title, description, completed, user_id, created_at, updated_at）
  - 创建索引：username, email, user_id, completed
  - 外键约束：user_id -> users(id) CASCADE删除
  - 插入测试数据：2个用户，3个Todo

#### 文档
- ✅ README.md - 完整更新
  - 更新项目说明，添加数据库特性
  - 更新技术栈，添加MySQL和H2
  - 新增"数据库配置"章节
    - 开发环境（H2）使用指南
    - 生产环境（MySQL）完整配置步骤
    - 数据库配置参数说明
    - 常见数据库问题解决方案
  - 更新"从零开始构建项目"
    - 第八步：配置数据库（完整数据库集成教程）
    - 添加JPA注解说明
    - Repository层创建教程
    - Service层迁移教程
    - 事务管理说明
  - 更新项目结构图
  - 更新学习要点总结

- ✅ DATABASE.md - 数据库快速开始指南
  - 快速开始：H2和MySQL两种选项
  - 数据库表结构说明
  - 环境配置对比表
  - 常见问题FAQ
  - 数据库管理工具推荐
  - 下一步学习建议

### Changed - 架构改进

#### 从内存存储到数据库持久化
- **之前**：使用ConcurrentHashMap内存存储，应用重启数据丢失
- **现在**：使用JPA/Hibernate持久化到数据库，支持多环境配置

#### 事务管理
- **之前**：无事务管理，并发控制依赖ConcurrentHashMap
- **现在**：使用@Transactional声明式事务，ACID保证

#### 数据验证
- **之前**：基本的存在性检查
- **现在**：增强验证（用户名唯一性、邮箱唯一性、外键完整性）

#### 多环境支持
- **之前**：单一配置
- **现在**：三种环境配置
  - dev：H2内存数据库，快速开发
  - default：MySQL，生产级配置
  - prod：MySQL+环境变量，云部署友好

### Technical Details - 技术细节

#### JPA映射策略
- **主键生成**：IDENTITY策略，利用数据库自增
- **字段映射**：明确指定列名、长度、约束
- **时间戳管理**：@PrePersist和@PreUpdate自动管理
- **命名策略**：表名复数（users, todos），列名下划线（user_id）

#### Repository查询方法命名规则
- `findBy` + 字段名 - 精确查询
- `findBy` + 字段名 + `And` + 字段名 - AND条件
- `findBy` + 字段名 + `Containing` - 模糊查询
- `existsBy` + 字段名 - 存在性检查

#### 事务边界
- **查询操作**：不需要事务（只读操作）
- **创建操作**：需要事务（@Transactional）
- **更新操作**：需要事务（@Transactional）
- **删除操作**：需要事务（@Transactional）

#### 数据库初始化策略
- **开发环境**：create-drop - 每次重建，方便测试
- **默认环境**：update - 自动更新表结构，开发友好
- **生产环境**：validate - 仅验证，不修改，生产安全

### Migration Guide - 迁移指南

如果你已有基于内存存储的代码，按以下步骤迁移：

1. **添加依赖**：在pom.xml中添加JPA、MySQL、H2依赖
2. **实体类注解**：为User和Todo添加JPA注解
3. **创建Repository**：创建继承JpaRepository的接口
4. **重构Service**：
   - 移除ConcurrentHashMap和AtomicLong
   - 注入Repository
   - 使用repository.save()代替map.put()
   - 使用repository.findById()代替map.get()
   - 添加@Transactional注解
5. **配置数据库**：创建application.yml及环境特定配置
6. **测试**：先使用dev配置（H2）测试，确保无误后切换到MySQL

### Breaking Changes - 破坏性变更

⚠️ **无破坏性变更** - API接口保持不变，仅内部实现改变

Controller层无需修改，客户端代码无需更新。

### Performance Considerations - 性能考虑

#### 优势
- ✅ 数据持久化，应用重启不丢失数据
- ✅ 数据库连接池（HikariCP）提供高性能连接管理
- ✅ Hibernate二级缓存可选（未启用）
- ✅ 支持复杂查询和关联操作

#### 注意事项
- ⚠️ N+1查询问题：需注意懒加载和关联查询
- ⚠️ 事务开销：适当控制事务边界
- ⚠️ 连接池配置：生产环境需根据负载调整

### Security Improvements - 安全改进

- ✅ 生产环境使用环境变量配置数据库凭据
- ✅ 数据库用户权限最小化（仅授权todo_db）
- ✅ SQL注入防护（JPA参数化查询）
- ✅ 外键约束保证数据完整性

### Testing - 测试

#### 手动测试
```bash
# 测试开发环境（H2）
./mvnw spring-boot:run -Dspring-boot.run.profiles=dev

# 测试生产环境（MySQL）
./mvnw spring-boot:run -Dspring-boot.run.profiles=prod
```

#### API测试
所有原有API接口保持兼容，可使用：
- IntelliJ IDEA HTTP Client（api-test.http）
- Postman（导入openapi.yaml）
- curl命令行测试

#### 数据库验证
- H2控制台：http://localhost:8080/h2-console
- MySQL客户端：`mysql -u todo_user -p todo_db`
- 数据库管理工具：MySQL Workbench、DBeaver等

### Future Enhancements - 未来改进

计划在后续版本中添加：

- [ ] Flyway/Liquibase数据库版本管理
- [ ] Spring Data JPA分页和排序
- [ ] 查询性能优化（索引、查询优化）
- [ ] Redis缓存集成
- [ ] 数据库读写分离
- [ ] 审计日志（创建人、修改人、修改时间）
- [ ] 软删除支持
- [ ] 实体关系映射（@ManyToOne, @OneToMany）
- [ ] 自定义Repository实现
- [ ] JPA Specification动态查询

### References - 参考文档

- [Spring Data JPA](https://spring.io/projects/spring-data-jpa)
- [Hibernate ORM](https://hibernate.org/orm/)
- [MySQL Reference Manual](https://dev.mysql.com/doc/)
- [H2 Database](https://www.h2database.com/)

---

## 作者

浙江工商大学 - 微服务架构课程

## 日期

2025年1月

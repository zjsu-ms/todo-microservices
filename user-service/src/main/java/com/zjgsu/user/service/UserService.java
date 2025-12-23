package com.zjgsu.user.service;

import com.zjgsu.user.exception.ResourceNotFoundException;
import com.zjgsu.user.model.User;
import com.zjgsu.user.repository.UserRepository;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import jakarta.annotation.PostConstruct;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

/**
 * 用户服务层
 * 使用数据库存储
 */
@Service
public class UserService {
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final MeterRegistry meterRegistry;

    // 自定义业务指标
    private final Counter userCreatedCounter;
    private final Counter userDeletedCounter;
    private final Counter authSuccessCounter;
    private final Counter authFailureCounter;
    private final Timer userQueryTimer;

    public UserService(UserRepository userRepository, MeterRegistry meterRegistry) {
        this.userRepository = userRepository;
        this.passwordEncoder = new BCryptPasswordEncoder();
        this.meterRegistry = meterRegistry;

        // 初始化自定义指标
        this.userCreatedCounter = Counter.builder("users.created")
                .description("用户创建数量")
                .tag("service", "user-service")
                .register(meterRegistry);

        this.userDeletedCounter = Counter.builder("users.deleted")
                .description("用户删除数量")
                .tag("service", "user-service")
                .register(meterRegistry);

        this.authSuccessCounter = Counter.builder("auth.success")
                .description("认证成功次数")
                .tag("service", "user-service")
                .register(meterRegistry);

        this.authFailureCounter = Counter.builder("auth.failure")
                .description("认证失败次数")
                .tag("service", "user-service")
                .register(meterRegistry);

        this.userQueryTimer = Timer.builder("users.query.time")
                .description("用户查询耗时")
                .tag("service", "user-service")
                .register(meterRegistry);
    }

    /**
     * 初始化测试数据
     */
    @PostConstruct
    public void init() {
        // 只在数据库为空时初始化测试数据
        if (userRepository.count() == 0) {
            User user1 = new User(null, "张三", "zhangsan@example.com");
            user1.setPassword("password");  // 密码将被加密
            user1.setRole("USER");
            createUser(user1);

            User user2 = new User(null, "李四", "lisi@example.com");
            user2.setPassword("password");
            user2.setRole("USER");
            createUser(user2);

            // 创建管理员账号
            User admin = new User(null, "admin", "admin@example.com");
            admin.setPassword("admin123");
            admin.setRole("ADMIN");
            createUser(admin);
        }
    }

    /**
     * 获取所有用户
     */
    public List<User> findAll() {
        return userRepository.findAll();
    }

    /**
     * 根据ID查找用户
     */
    public Optional<User> findById(Long id) {
        return userQueryTimer.record(() -> userRepository.findById(id));
    }

    /**
     * 创建用户
     */
    @Transactional
    public User createUser(User user) {
        // 检查用户名是否已存在
        if (userRepository.existsByUsername(user.getUsername())) {
            throw new IllegalArgumentException("用户名已存在: " + user.getUsername());
        }
        // 检查邮箱是否已存在
        if (userRepository.existsByEmail(user.getEmail())) {
            throw new IllegalArgumentException("邮箱已存在: " + user.getEmail());
        }

        // 加密密码
        if (user.getPassword() != null && !user.getPassword().isEmpty()) {
            user.setPassword(passwordEncoder.encode(user.getPassword()));
        }

        User savedUser = userRepository.save(user);

        // 增加用户创建计数
        userCreatedCounter.increment();

        return savedUser;
    }

    /**
     * 更新用户
     */
    @Transactional
    public User updateUser(Long id, User user) {
        User existingUser = userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User", id));

        // 更新字段
        existingUser.setUsername(user.getUsername());
        existingUser.setEmail(user.getEmail());

        return userRepository.save(existingUser);
    }

    /**
     * 删除用户
     */
    @Transactional
    public boolean deleteUser(Long id) {
        if (!userRepository.existsById(id)) {
            throw new ResourceNotFoundException("User", id);
        }
        userRepository.deleteById(id);

        // 增加用户删除计数
        userDeletedCounter.increment();

        return true;
    }

    /**
     * 检查用户是否存在
     */
    public boolean existsById(Long id) {
        return userRepository.existsById(id);
    }

    /**
     * 用户认证
     * @param username 用户名
     * @param password 密码（明文）
     * @return 认证成功返回用户对象，失败返回null
     */
    public User authenticate(String username, String password) {
        Optional<User> userOpt = userRepository.findByUsername(username);

        if (userOpt.isEmpty()) {
            // 认证失败计数
            authFailureCounter.increment();
            return null;
        }

        User user = userOpt.get();

        // 验证密码
        if (passwordEncoder.matches(password, user.getPassword())) {
            // 认证成功计数
            authSuccessCounter.increment();
            return user;
        }

        // 认证失败计数
        authFailureCounter.increment();
        return null;
    }
}

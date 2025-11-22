package com.zjgsu.user.service;

import com.zjgsu.user.exception.ResourceNotFoundException;
import com.zjgsu.user.model.User;
import com.zjgsu.user.repository.UserRepository;
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

    public UserService(UserRepository userRepository) {
        this.userRepository = userRepository;
        this.passwordEncoder = new BCryptPasswordEncoder();
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
        return userRepository.findById(id);
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

        return userRepository.save(user);
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
            return null;
        }

        User user = userOpt.get();

        // 验证密码
        if (passwordEncoder.matches(password, user.getPassword())) {
            return user;
        }

        return null;
    }
}

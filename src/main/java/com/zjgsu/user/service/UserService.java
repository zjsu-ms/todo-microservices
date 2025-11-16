package com.zjgsu.user.service;

import com.zjgsu.user.exception.ResourceNotFoundException;
import com.zjgsu.user.model.User;
import com.zjgsu.user.repository.UserRepository;
import jakarta.annotation.PostConstruct;
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

    public UserService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    /**
     * 初始化测试数据
     */
    @PostConstruct
    public void init() {
        // 只在数据库为空时初始化测试数据
        if (userRepository.count() == 0) {
            createUser(new User(null, "张三", "zhangsan@example.com"));
            createUser(new User(null, "李四", "lisi@example.com"));
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
}

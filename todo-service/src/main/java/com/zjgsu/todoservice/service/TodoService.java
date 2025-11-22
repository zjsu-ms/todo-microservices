package com.zjgsu.todoservice.service;

import com.zjgsu.todoservice.client.UserClient;
import com.zjgsu.todoservice.exception.ResourceNotFoundException;
import com.zjgsu.todoservice.model.Todo;
import com.zjgsu.todoservice.repository.TodoRepository;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.HttpClientErrorException;

import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Todo服务层
 * 使用数据库存储，通过OpenFeign调用user-service验证用户
 */
@Service
public class TodoService {
    private static final Logger logger = LoggerFactory.getLogger(TodoService.class);

    private final TodoRepository todoRepository;
    private final UserClient userClient;

    public TodoService(TodoRepository todoRepository, UserClient userClient) {
        this.todoRepository = todoRepository;
        this.userClient = userClient;
    }

    /**
     * 初始化测试数据
     */
    @PostConstruct
    public void init() {
        // 只在数据库为空时初始化测试数据
        if (todoRepository.count() == 0) {
            createTodo(new Todo(null, "学习Spring Boot", "完成基础教程", 1L));
            createTodo(new Todo(null, "实现微服务", "拆分单体应用", 1L));
        }
    }

    /**
     * 获取所有Todo
     */
    public List<Todo> findAll() {
        return todoRepository.findAll();
    }

    /**
     * 根据用户ID获取Todo列表
     */
    public List<Todo> findByUserId(Long userId) {
        return todoRepository.findByUserId(userId);
    }

    /**
     * 根据ID查找Todo
     */
    public Optional<Todo> findById(Long id) {
        return todoRepository.findById(id);
    }

    /**
     * 创建Todo
     */
    @Transactional
    public Todo createTodo(Todo todo) {
        // 调用用户服务验证用户是否存在
        if (todo.getUserId() != null) {
            verifyUserExists(todo.getUserId());
        }
        return todoRepository.save(todo);
    }

    /**
     * 更新Todo
     */
    @Transactional
    public Todo updateTodo(Long id, Todo todo) {
        Todo existingTodo = todoRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Todo", id));

        // 验证用户存在
        if (todo.getUserId() != null) {
            verifyUserExists(todo.getUserId());
        }

        // 更新字段
        existingTodo.setTitle(todo.getTitle());
        existingTodo.setDescription(todo.getDescription());
        existingTodo.setCompleted(todo.getCompleted());
        existingTodo.setUserId(todo.getUserId());

        return todoRepository.save(existingTodo);
    }

    /**
     * 删除Todo
     */
    @Transactional
    public boolean deleteTodo(Long id) {
        if (!todoRepository.existsById(id)) {
            throw new ResourceNotFoundException("Todo", id);
        }
        todoRepository.deleteById(id);
        return true;
    }

    /**
     * 切换Todo完成状态
     */
    @Transactional
    public Todo toggleComplete(Long id) {
        Todo todo = todoRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Todo", id));
        todo.setCompleted(!todo.getCompleted());
        return todoRepository.save(todo);
    }

    /**
     * 通过OpenFeign调用用户服务验证用户是否存在
     * 集成了负载均衡、熔断和重试机制
     */
    private void verifyUserExists(Long userId) {
        try {
            Map<String, Object> user = userClient.getUser(userId);

            // 检查是否是降级响应
            if (user.containsKey("fallback") && Boolean.TRUE.equals(user.get("fallback"))) {
                logger.warn("User service is unavailable, using fallback data for user ID: {}", userId);
            }
        } catch (HttpClientErrorException.NotFound e) {
            throw new ResourceNotFoundException("User", userId);
        } catch (Exception e) {
            logger.error("Failed to verify user: {}", e.getMessage());
            throw new RuntimeException("Failed to verify user: " + e.getMessage());
        }
    }
}

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

/**
 * Todo服务层
 * 使用数据库存储，通过HTTP调用user-service验证用户
 */
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

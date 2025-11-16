package com.zjgsu.todoservice.controller;

import com.zjgsu.todoservice.common.ApiResponse;
import com.zjgsu.todoservice.model.Todo;
import com.zjgsu.todoservice.service.TodoService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Todo管理Controller
 * 演示RESTful API的CRUD操作和资源嵌套
 */
@RestController
@RequestMapping("/api/todos")
public class TodoController {

    private final TodoService todoService;

    public TodoController(TodoService todoService) {
        this.todoService = todoService;
    }

    /**
     * 获取所有Todo
     * GET /api/todos
     */
    @GetMapping
    public ResponseEntity<ApiResponse<List<Todo>>> getAllTodos(
            @RequestParam(required = false) Long userId) {
        List<Todo> todos;
        if (userId != null) {
            todos = todoService.findByUserId(userId);
        } else {
            todos = todoService.findAll();
        }
        return ResponseEntity.ok(ApiResponse.success(todos));
    }

    /**
     * 根据ID获取Todo
     * GET /api/todos/{id}
     */
    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<Todo>> getTodoById(@PathVariable Long id) {
        return todoService.findById(id)
                .map(todo -> ResponseEntity.ok(ApiResponse.success(todo)))
                .orElse(ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(ApiResponse.notFound("Todo not found with id: " + id)));
    }

    /**
     * 创建Todo
     * POST /api/todos
     */
    @PostMapping
    public ResponseEntity<ApiResponse<Todo>> createTodo(@RequestBody Todo todo) {
        Todo created = todoService.createTodo(todo);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.created(created));
    }

    /**
     * 更新Todo
     * PUT /api/todos/{id}
     */
    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<Todo>> updateTodo(
            @PathVariable Long id,
            @RequestBody Todo todo) {
        Todo updated = todoService.updateTodo(id, todo);
        return ResponseEntity.ok(ApiResponse.success(updated));
    }

    /**
     * 删除Todo
     * DELETE /api/todos/{id}
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> deleteTodo(@PathVariable Long id) {
        todoService.deleteTodo(id);
        return ResponseEntity.ok(ApiResponse.success("Todo deleted successfully"));
    }

    /**
     * 切换Todo完成状态
     * PATCH /api/todos/{id}/toggle
     */
    @PatchMapping("/{id}/toggle")
    public ResponseEntity<ApiResponse<Todo>> toggleComplete(@PathVariable Long id) {
        Todo updated = todoService.toggleComplete(id);
        return ResponseEntity.ok(ApiResponse.success(updated));
    }
}

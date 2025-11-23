package com.zjgsu.todoservice.dto;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * Todo事件消息DTO
 * 用于RabbitMQ消息传递
 */
public class TodoEventMessage implements Serializable {

    private Long todoId;
    private String title;
    private String description;
    private Long userId;
    private String eventType;  // created, updated, deleted, completed
    private LocalDateTime timestamp;

    public TodoEventMessage() {
        this.timestamp = LocalDateTime.now();
    }

    public TodoEventMessage(Long todoId, String title, String description, Long userId, String eventType) {
        this.todoId = todoId;
        this.title = title;
        this.description = description;
        this.userId = userId;
        this.eventType = eventType;
        this.timestamp = LocalDateTime.now();
    }

    // Getters and Setters
    public Long getTodoId() {
        return todoId;
    }

    public void setTodoId(Long todoId) {
        this.todoId = todoId;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public String getEventType() {
        return eventType;
    }

    public void setEventType(String eventType) {
        this.eventType = eventType;
    }

    public LocalDateTime getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(LocalDateTime timestamp) {
        this.timestamp = timestamp;
    }

    @Override
    public String toString() {
        return "TodoEventMessage{" +
                "todoId=" + todoId +
                ", title='" + title + '\'' +
                ", userId=" + userId +
                ", eventType='" + eventType + '\'' +
                ", timestamp=" + timestamp +
                '}';
    }
}

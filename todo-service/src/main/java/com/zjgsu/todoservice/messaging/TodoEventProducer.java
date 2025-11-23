package com.zjgsu.todoservice.messaging;

import com.zjgsu.todoservice.dto.TodoEventMessage;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * Todo事件消息生产者
 * 负责发送todo相关事件到RabbitMQ
 */
@Component
public class TodoEventProducer {

    @Autowired
    private RabbitTemplate rabbitTemplate;

    private static final String EXCHANGE = "todo.event.exchange";

    /**
     * 发送todo创建事件
     */
    public void sendTodoCreatedEvent(TodoEventMessage message) {
        message.setEventType("created");
        sendMessage("todo.created", message);
        System.out.println("发送Todo创建事件: " + message);
    }

    /**
     * 发送todo更新事件
     */
    public void sendTodoUpdatedEvent(TodoEventMessage message) {
        message.setEventType("updated");
        sendMessage("todo.updated", message);
        System.out.println("发送Todo更新事件: " + message);
    }

    /**
     * 发送todo删除事件
     */
    public void sendTodoDeletedEvent(TodoEventMessage message) {
        message.setEventType("deleted");
        sendMessage("todo.deleted", message);
        System.out.println("发送Todo删除事件: " + message);
    }

    /**
     * 发送todo完成状态切换事件
     */
    public void sendTodoToggledEvent(TodoEventMessage message) {
        message.setEventType("toggled");
        sendMessage("todo.toggled", message);
        System.out.println("发送Todo状态切换事件: " + message);
    }

    /**
     * 通用消息发送方法
     */
    private void sendMessage(String routingKey, TodoEventMessage message) {
        try {
            rabbitTemplate.convertAndSend(EXCHANGE, routingKey, message);
        } catch (Exception e) {
            System.err.println("发送消息失败: " + e.getMessage());
            e.printStackTrace();
        }
    }
}

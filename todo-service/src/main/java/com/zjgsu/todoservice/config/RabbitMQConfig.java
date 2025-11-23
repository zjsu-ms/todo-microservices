package com.zjgsu.todoservice.config;

import org.springframework.amqp.core.*;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * RabbitMQ配置类
 * 配置交换机、队列、绑定关系和死信队列
 */
@Configuration
public class RabbitMQConfig {

    // ========== 交换机定义 ==========

    /**
     * Topic交换机 - 用于事件路由
     * 支持通配符匹配: * 匹配一个单词, # 匹配零个或多个单词
     */
    @Bean
    public TopicExchange todoEventExchange() {
        return ExchangeBuilder
                .topicExchange("todo.event.exchange")
                .durable(true)  // 持久化
                .build();
    }

    /**
     * Direct交换机 - 用于通知路由
     */
    @Bean
    public DirectExchange notificationExchange() {
        return ExchangeBuilder
                .directExchange("notification.exchange")
                .durable(true)
                .build();
    }

    /**
     * Fanout交换机 - 用于广播消息
     */
    @Bean
    public FanoutExchange broadcastExchange() {
        return ExchangeBuilder
                .fanoutExchange("broadcast.exchange")
                .durable(true)
                .build();
    }

    // ========== 死信交换机 ==========

    @Bean
    public DirectExchange deadLetterExchange() {
        return ExchangeBuilder
                .directExchange("dlx.exchange")
                .durable(true)
                .build();
    }

    // ========== 队列定义 ==========

    /**
     * Todo创建队列 - 带死信队列配置
     */
    @Bean
    public Queue todoCreatedQueue() {
        return QueueBuilder
                .durable("todo.created.queue")
                .deadLetterExchange("dlx.exchange")      // 死信交换机
                .deadLetterRoutingKey("dlx.todo.key")     // 死信路由键
                .ttl(300000)                               // 消息TTL 5分钟
                .build();
    }

    /**
     * Todo更新队列
     */
    @Bean
    public Queue todoUpdatedQueue() {
        return QueueBuilder
                .durable("todo.updated.queue")
                .deadLetterExchange("dlx.exchange")
                .deadLetterRoutingKey("dlx.todo.key")
                .build();
    }

    /**
     * 通知队列
     */
    @Bean
    public Queue notificationQueue() {
        return QueueBuilder
                .durable("notification.queue")
                .build();
    }

    /**
     * 死信队列
     */
    @Bean
    public Queue deadLetterQueue() {
        return QueueBuilder
                .durable("dlx.todo.queue")
                .build();
    }

    // ========== 绑定关系 ==========

    /**
     * 绑定todo.created队列到topic交换机
     * routing key: todo.created
     */
    @Bean
    public Binding todoCreatedBinding() {
        return BindingBuilder
                .bind(todoCreatedQueue())
                .to(todoEventExchange())
                .with("todo.created");
    }

    /**
     * 绑定todo.updated队列到topic交换机
     * routing key: todo.updated
     */
    @Bean
    public Binding todoUpdatedBinding() {
        return BindingBuilder
                .bind(todoUpdatedQueue())
                .to(todoEventExchange())
                .with("todo.updated");
    }

    /**
     * 绑定通知队列到direct交换机
     */
    @Bean
    public Binding notificationBinding() {
        return BindingBuilder
                .bind(notificationQueue())
                .to(notificationExchange())
                .with("notification.key");
    }

    /**
     * 绑定死信队列
     */
    @Bean
    public Binding deadLetterBinding() {
        return BindingBuilder
                .bind(deadLetterQueue())
                .to(deadLetterExchange())
                .with("dlx.todo.key");
    }

    // ========== RabbitTemplate配置 ==========

    /**
     * 配置RabbitTemplate使用JSON消息转换器
     */
    @Bean
    public RabbitTemplate rabbitTemplate(ConnectionFactory connectionFactory) {
        RabbitTemplate rabbitTemplate = new RabbitTemplate(connectionFactory);
        rabbitTemplate.setMessageConverter(jackson2JsonMessageConverter());

        // 配置确认回调
        rabbitTemplate.setConfirmCallback((correlationData, ack, cause) -> {
            if (ack) {
                System.out.println("消息发送到交换机成功");
            } else {
                System.err.println("消息发送到交换机失败: " + cause);
            }
        });

        // 配置退回回调
        rabbitTemplate.setReturnsCallback(returned -> {
            System.err.println("消息未路由到队列: " +
                returned.getMessage() + ", routing key: " + returned.getRoutingKey());
        });

        return rabbitTemplate;
    }

    /**
     * JSON消息转换器
     */
    @Bean
    public Jackson2JsonMessageConverter jackson2JsonMessageConverter() {
        return new Jackson2JsonMessageConverter();
    }
}

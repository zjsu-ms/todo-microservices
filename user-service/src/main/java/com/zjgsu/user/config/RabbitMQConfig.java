package com.zjgsu.user.config;

import org.springframework.amqp.core.*;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.DefaultJackson2JavaTypeMapper;
import org.springframework.amqp.support.converter.Jackson2JavaTypeMapper;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * User Service RabbitMQ配置类
 * 主要用于接收todo事件通知
 */
@Configuration
public class RabbitMQConfig {

    /**
     * Topic交换机 - 接收todo事件
     */
    @Bean
    public TopicExchange todoEventExchange() {
        return ExchangeBuilder
                .topicExchange("todo.event.exchange")
                .durable(true)
                .build();
    }

    /**
     * 用户通知队列 - 接收所有todo事件
     * 使用通配符 todo.* 匹配所有todo相关事件
     */
    @Bean
    public Queue userNotificationQueue() {
        return QueueBuilder
                .durable("user.notification.queue")
                .build();
    }

    /**
     * 绑定用户通知队列到todo事件交换机
     * routing key: todo.* (匹配todo.created, todo.updated等所有todo事件)
     */
    @Bean
    public Binding userNotificationBinding() {
        return BindingBuilder
                .bind(userNotificationQueue())
                .to(todoEventExchange())
                .with("todo.*");  // 使用通配符匹配所有todo事件
    }

    /**
     * 配置RabbitTemplate使用JSON消息转换器
     */
    @Bean
    public RabbitTemplate rabbitTemplate(ConnectionFactory connectionFactory) {
        RabbitTemplate rabbitTemplate = new RabbitTemplate(connectionFactory);
        rabbitTemplate.setMessageConverter(jackson2JsonMessageConverter());
        return rabbitTemplate;
    }

    /**
     * JSON消息转换器
     * 配置信任所有包，允许反序列化任何类型（仅用于开发/测试环境）
     */
    @Bean
    public Jackson2JsonMessageConverter jackson2JsonMessageConverter() {
        Jackson2JsonMessageConverter converter = new Jackson2JsonMessageConverter();
        DefaultJackson2JavaTypeMapper typeMapper = new DefaultJackson2JavaTypeMapper();
        // 信任所有包，允许反序列化来自其他服务的消息
        typeMapper.setTrustedPackages("*");
        converter.setJavaTypeMapper(typeMapper);
        return converter;
    }
}

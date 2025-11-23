package com.zjgsu.user.messaging;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.rabbitmq.client.Channel;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Map;

/**
 * Todo事件消费者
 * 监听todo相关事件并处理
 * 实现手动确认机制确保消息可靠性
 */
@Component
public class TodoEventConsumer {

    private final ObjectMapper objectMapper = new ObjectMapper();

    /**
     * 监听用户通知队列
     * 接收todo.* 路由的所有todo事件
     */
    @RabbitListener(queues = "user.notification.queue")
    public void handleTodoEvent(Message message, Channel channel) throws IOException {
        long deliveryTag = message.getMessageProperties().getDeliveryTag();

        try {
            // 解析消息内容为JSON Map
            String messageBody = new String(message.getBody(), StandardCharsets.UTF_8);
            Map<String, Object> todoEvent = objectMapper.readValue(messageBody, Map.class);

            System.out.println("==============================================");
            System.out.println("用户服务接收到Todo事件: " + messageBody);
            System.out.println("路由键: " + message.getMessageProperties().getReceivedRoutingKey());
            System.out.println("Todo ID: " + todoEvent.get("todoId"));
            System.out.println("标题: " + todoEvent.get("title"));
            System.out.println("用户ID: " + todoEvent.get("userId"));
            System.out.println("事件类型: " + todoEvent.get("eventType"));
            System.out.println("时间戳: " + message.getMessageProperties().getTimestamp());
            System.out.println("==============================================");

            // 处理业务逻辑
            processTodoEvent(todoEvent, message.getMessageProperties().getReceivedRoutingKey());

            // 手动确认消息
            channel.basicAck(deliveryTag, false);
            System.out.println("✓ 消息确认成功: delivery tag = " + deliveryTag);

        } catch (Exception e) {
            System.err.println("✗ 处理消息失败: " + e.getMessage());
            e.printStackTrace();

            // 拒绝消息并重新入队（最多重试3次，配置在bootstrap.yml中）
            // false = 不批量拒绝, true = 重新入队
            channel.basicNack(deliveryTag, false, true);
            System.err.println("消息已重新入队: delivery tag = " + deliveryTag);
        }
    }

    /**
     * 处理todo事件业务逻辑
     */
    private void processTodoEvent(Map<String, Object> todoEvent, String routingKey) {
        String eventType = (String) todoEvent.get("eventType");
        String title = (String) todoEvent.get("title");
        Object userId = todoEvent.get("userId");

        // 根据路由键判断事件类型
        if ("created".equals(eventType) || routingKey.contains("created")) {
            System.out.println(">>> 处理Todo创建事件");
            System.out.println("    - 发送邮件通知用户 " + userId + ": 新Todo '" + title + "' 已创建");
        } else if ("updated".equals(eventType) || routingKey.contains("updated")) {
            System.out.println(">>> 处理Todo更新事件");
            System.out.println("    - 更新用户 " + userId + " 的活动记录");
        } else if ("deleted".equals(eventType) || routingKey.contains("deleted")) {
            System.out.println(">>> 处理Todo删除事件");
            System.out.println("    - 清理用户 " + userId + " 的相关资源");
        } else if ("toggled".equals(eventType) || routingKey.contains("toggled")) {
            System.out.println(">>> 处理Todo状态切换事件");
            System.out.println("    - 更新用户 " + userId + " 的统计信息");
        }

        // 模拟处理耗时
        try {
            Thread.sleep(500);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}

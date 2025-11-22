package com.zjgsu.todoservice.client;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

/**
 * User Service Fallback
 * 当user-service不可用时的降级处理
 */
@Component
public class UserClientFallback implements UserClient {

    private static final Logger logger = LoggerFactory.getLogger(UserClientFallback.class);

    @Override
    public Map<String, Object> getUser(Long id) {
        logger.warn("User service unavailable, returning fallback for user ID: {}", id);

        // 返回降级数据
        Map<String, Object> fallbackUser = new HashMap<>();
        fallbackUser.put("id", id);
        fallbackUser.put("username", "默认用户");
        fallbackUser.put("email", "default@example.com");
        fallbackUser.put("fallback", true);

        return fallbackUser;
    }
}

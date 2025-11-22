package com.zjgsu.todoservice.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import java.util.Map;

/**
 * User Service Feign Client
 * 使用声明式HTTP客户端调用user-service
 */
@FeignClient(
    name = "user-service",
    fallback = UserClientFallback.class
)
public interface UserClient {

    /**
     * 根据ID获取用户
     * @param id 用户ID
     * @return 用户信息
     */
    @GetMapping("/api/users/{id}")
    Map<String, Object> getUser(@PathVariable("id") Long id);
}

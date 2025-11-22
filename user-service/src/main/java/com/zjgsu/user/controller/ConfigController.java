package com.zjgsu.user.controller;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.context.config.annotation.RefreshScope;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;

/**
 * 配置中心测试控制器
 * 演示配置动态刷新功能
 */
@RestController
@RequestMapping("/api/config")
@RefreshScope  // 支持配置动态刷新
public class ConfigController {

    @Value("${app.name:User Service}")
    private String appName;

    @Value("${app.version:1.0.0}")
    private String appVersion;

    @Value("${app.description:用户管理微服务}")
    private String appDescription;

    /**
     * 获取当前配置信息
     */
    @GetMapping("/info")
    public Map<String, String> getConfigInfo() {
        Map<String, String> config = new HashMap<>();
        config.put("appName", appName);
        config.put("appVersion", appVersion);
        config.put("appDescription", appDescription);
        config.put("message", "配置信息来自Nacos配置中心");
        return config;
    }
}

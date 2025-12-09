package com.zjgsu.todoservice.controller;

import com.zjgsu.todoservice.config.AppConfig;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.context.config.annotation.RefreshScope;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;

/**
 * 配置中心测试控制器
 * 演示两种配置读取方式和动态刷新功能
 *
 * 方式1: @Value - 适合简单配置
 * 方式2: @ConfigurationProperties - 适合复杂配置（推荐）
 */
@RestController
@RequestMapping("/api/config")
@RefreshScope  // 支持配置动态刷新
public class ConfigController {

    // ========== 方式1: 使用 @Value 注解 ==========
    @Value("${app.name:Todo Service}")
    private String appName;

    @Value("${app.version:1.0.0}")
    private String appVersion;

    @Value("${app.description:待办事项管理微服务}")
    private String appDescription;

    // ========== 方式2: 使用 @ConfigurationProperties（推荐）==========
    @Autowired
    private AppConfig appConfig;

    /**
     * 获取配置信息 - 使用 @Value 方式
     * 演示简单配置的读取
     */
    @GetMapping("/info")
    public Map<String, Object> getConfigInfo() {
        Map<String, Object> config = new HashMap<>();
        config.put("method", "@Value");
        config.put("appName", appName);
        config.put("appVersion", appVersion);
        config.put("appDescription", appDescription);
        config.put("message", "配置信息来自Nacos配置中心（使用@Value读取）");
        return config;
    }

    /**
     * 获取配置信息 - 使用 @ConfigurationProperties 方式（推荐）
     * 演示复杂配置的读取，包括 Map 类型的配置
     */
    @GetMapping("/info-advanced")
    public Map<String, Object> getAdvancedConfigInfo() {
        Map<String, Object> config = new HashMap<>();
        config.put("method", "@ConfigurationProperties (推荐)");
        config.put("appName", appConfig.getName());
        config.put("appVersion", appConfig.getVersion());
        config.put("appDescription", appConfig.getDescription());
        config.put("features", appConfig.getFeatures());
        config.put("settings", appConfig.getSettings());
        config.put("message", "配置信息来自Nacos配置中心（使用@ConfigurationProperties读取）");
        return config;
    }

    /**
     * 对比两种方式
     * 展示 @ConfigurationProperties 相比 @Value 的优势
     */
    @GetMapping("/comparison")
    public Map<String, Object> compareConfigMethods() {
        Map<String, Object> comparison = new HashMap<>();

        // @Value 方式
        Map<String, Object> valueMethod = new HashMap<>();
        valueMethod.put("method", "@Value");
        valueMethod.put("appName", appName);
        valueMethod.put("appVersion", appVersion);
        valueMethod.put("appDescription", appDescription);
        valueMethod.put("advantages", "简单直观，适合单个配置项");
        valueMethod.put("limitations", "不支持复杂对象（List/Map），需要逐个声明");

        // @ConfigurationProperties 方式
        Map<String, Object> configPropsMethod = new HashMap<>();
        configPropsMethod.put("method", "@ConfigurationProperties");
        configPropsMethod.put("appName", appConfig.getName());
        configPropsMethod.put("appVersion", appConfig.getVersion());
        configPropsMethod.put("appDescription", appConfig.getDescription());
        configPropsMethod.put("features", appConfig.getFeatures());
        configPropsMethod.put("settings", appConfig.getSettings());
        configPropsMethod.put("advantages", "类型安全，支持复杂对象，配置集中，IDE友好");
        configPropsMethod.put("limitations", "需要额外的配置类");

        comparison.put("valueMethod", valueMethod);
        comparison.put("configPropertiesMethod", configPropsMethod);
        comparison.put("recommendation", "推荐使用 @ConfigurationProperties 方式");

        return comparison;
    }
}

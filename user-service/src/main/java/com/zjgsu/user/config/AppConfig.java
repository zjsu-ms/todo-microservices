package com.zjgsu.user.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.cloud.context.config.annotation.RefreshScope;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * 应用配置类
 * 演示 @ConfigurationProperties 的使用（推荐方式）
 *
 * 相比于 @Value 的优势：
 * 1. 类型安全，自动类型转换
 * 2. 支持复杂对象（List, Map等）
 * 3. 支持配置验证（@Validated）
 * 4. 配置集中管理，便于维护
 * 5. IDE 自动补全和提示
 */
@Component
@ConfigurationProperties(prefix = "app")
@RefreshScope
public class AppConfig {

    /**
     * 应用名称
     * 对应配置: app.name
     */
    private String name;

    /**
     * 应用版本
     * 对应配置: app.version
     */
    private String version;

    /**
     * 应用描述
     * 对应配置: app.description
     */
    private String description;

    /**
     * 功能开关配置
     * 对应配置: app.features
     * 示例:
     *   app.features.cache-enabled: true
     *   app.features.async-enabled: false
     */
    private Map<String, Boolean> features;

    /**
     * 应用设置
     * 对应配置: app.settings
     * 支持任意键值对配置
     */
    private Map<String, String> settings;

    // Getters and Setters

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getVersion() {
        return version;
    }

    public void setVersion(String version) {
        this.version = version;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public Map<String, Boolean> getFeatures() {
        return features;
    }

    public void setFeatures(Map<String, Boolean> features) {
        this.features = features;
    }

    public Map<String, String> getSettings() {
        return settings;
    }

    public void setSettings(Map<String, String> settings) {
        this.settings = settings;
    }
}

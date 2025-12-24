package com.zjgsu.user.controller;

import com.zjgsu.user.config.AppConfig;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.context.refresh.ContextRefresher;
import org.springframework.core.env.Environment;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/**
 * 配置刷新控制器
 * 提供手动刷新配置的接口
 */
@RestController
@RequestMapping("/api/refresh")
public class RefreshController {

    @Autowired(required = false)
    private ContextRefresher contextRefresher;

    @Autowired
    private Environment environment;

    @Autowired
    private AppConfig appConfig;

    /**
     * 手动触发配置刷新
     */
    @PostMapping("/manual")
    public Map<String, Object> manualRefresh() {
        Map<String, Object> result = new HashMap<>();

        try {
            if (contextRefresher != null) {
                Set<String> keys = contextRefresher.refresh();
                result.put("status", "success");
                result.put("refreshedKeys", keys);
                result.put("message", "配置刷新成功");
            } else {
                result.put("status", "error");
                result.put("message", "ContextRefresher 不可用");
            }
        } catch (Exception e) {
            result.put("status", "error");
            result.put("message", e.getMessage());
        }

        return result;
    }

    /**
     * 获取当前环境中的配置值
     */
    @PostMapping("/check")
    public Map<String, Object> checkConfig() {
        Map<String, Object> result = new HashMap<>();

        // 从 Environment 直接读取
        result.put("fromEnvironment", Map.of(
            "appName", environment.getProperty("app.name", "N/A"),
            "appVersion", environment.getProperty("app.version", "N/A"),
            "appDescription", environment.getProperty("app.description", "N/A")
        ));

        // 从 @ConfigurationProperties Bean 读取
        result.put("fromAppConfig", Map.of(
            "appName", appConfig.getName(),
            "appVersion", appConfig.getVersion(),
            "appDescription", appConfig.getDescription(),
            "features", appConfig.getFeatures(),
            "settings", appConfig.getSettings()
        ));

        return result;
    }
}

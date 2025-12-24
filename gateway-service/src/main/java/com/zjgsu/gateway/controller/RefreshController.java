package com.zjgsu.gateway.controller;

import com.zjgsu.gateway.config.AppConfig;
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
 * Mn7°§6h
 * Ð›Mn7°øs„¡¥ã
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
     * K¨æÑMn7°
     */
    @PostMapping("/manual")
    public Map<String, Object> manualRefresh() {
        Map<String, Object> result = new HashMap<>();
        try {
            if (contextRefresher != null) {
                Set<String> keys = contextRefresher.refresh();
                result.put("status", "success");
                result.put("refreshedKeys", keys);
                result.put("message", "Mn7°Ÿ");
            } else {
                result.put("status", "error");
                result.put("message", "ContextRefresher ï(");
            }
        } catch (Exception e) {
            result.put("status", "error");
            result.put("message", e.getMessage());
        }
        return result;
    }

    /**
     * ÀåSMMn
     */
    @PostMapping("/check")
    public Map<String, Object> checkConfig() {
        Map<String, Object> result = new HashMap<>();

        // Î Environment ô¥ûÖ
        result.put("fromEnvironment", Map.of(
            "appName", environment.getProperty("app.name", "N/A"),
            "appVersion", environment.getProperty("app.version", "N/A"),
            "appDescription", environment.getProperty("app.description", "N/A")
        ));

        // Î @ConfigurationProperties Bean ûÖ
        Map<String, Object> fromBean = new HashMap<>();
        fromBean.put("appName", appConfig.getName());
        fromBean.put("appVersion", appConfig.getVersion());
        fromBean.put("appDescription", appConfig.getDescription());
        fromBean.put("features", appConfig.getFeatures());
        fromBean.put("settings", appConfig.getSettings());
        result.put("fromAppConfig", fromBean);

        return result;
    }
}

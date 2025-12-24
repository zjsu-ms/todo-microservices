package com.zjgsu.gateway.controller;

import com.zjgsu.gateway.config.AppConfig;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.context.config.annotation.RefreshScope;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;

/**
 * Mn-ÃKÕ§6h
 * :$ÍMnûÖ¹Œ¨7°Ÿý
 */
@RestController
@RequestMapping("/api/config")
@RefreshScope
public class ConfigController {

    // ¹1: ( @Value èã
    @Value("${app.name:API Gateway}")
    private String appName;

    @Value("${app.version:1.0.0}")
    private String appVersion;

    @Value("${app.description:APIQs¡}")
    private String appDescription;

    // ¹2: ( @ConfigurationProperties
    @Autowired
    private AppConfig appConfig;

    /**
     * ·ÖMnáo - ( @Value ¹
     */
    @GetMapping("/info")
    public Map<String, Object> getConfigInfo() {
        Map<String, Object> config = new HashMap<>();
        config.put("method", "@Value");
        config.put("appName", appName);
        config.put("appVersion", appVersion);
        config.put("appDescription", appDescription);
        config.put("message", "MnáoeêNacosMn-Ã(@ValueûÖ	");
        return config;
    }

    /**
     * ·ÖMnáo - ( @ConfigurationProperties ¹
     */
    @GetMapping("/info-advanced")
    public Map<String, Object> getAdvancedConfigInfo() {
        Map<String, Object> config = new HashMap<>();
        config.put("method", "@ConfigurationProperties");
        config.put("appName", appConfig.getName());
        config.put("appVersion", appConfig.getVersion());
        config.put("appDescription", appConfig.getDescription());
        config.put("features", appConfig.getFeatures());
        config.put("settings", appConfig.getSettings());
        config.put("message", "MnáoeêNacosMn-Ã(@ConfigurationPropertiesûÖ	");
        return config;
    }
}

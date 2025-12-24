package com.zjgsu.user.config;

import com.alibaba.cloud.nacos.NacosConfigManager;
import com.alibaba.nacos.api.config.listener.Listener;
import com.alibaba.nacos.api.exception.NacosException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.properties.bind.Binder;
import org.springframework.cloud.context.environment.EnvironmentChangeEvent;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.MapPropertySource;
import org.yaml.snakeyaml.Yaml;

import jakarta.annotation.PostConstruct;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.concurrent.Executor;

/**
 * Nacos 配置动态刷新配置类
 * 直接监听 Nacos 配置变更，实现真正的动态刷新
 */
@Configuration
public class NacosDynamicRefreshConfig {

    private static final Logger log = LoggerFactory.getLogger(NacosDynamicRefreshConfig.class);

    @Autowired(required = false)
    private NacosConfigManager nacosConfigManager;

    @Autowired
    private ConfigurableEnvironment environment;

    @Autowired
    private ApplicationEventPublisher publisher;

    @Autowired
    private AppConfig appConfig;

    @Value("${spring.application.name}")
    private String applicationName;

    @Value("${spring.profiles.active:dev}")
    private String profile;

    private static final String DATA_ID_SUFFIX = ".yaml";
    private static final String GROUP = "DEFAULT_GROUP";

    @PostConstruct
    public void init() {
        if (nacosConfigManager == null) {
            log.warn("NacosConfigManager 不可用，跳过动态刷新配置");
            return;
        }

        String dataId = applicationName + "-" + profile + DATA_ID_SUFFIX;

        try {
            nacosConfigManager.getConfigService().addListener(dataId, GROUP, new Listener() {
                @Override
                public Executor getExecutor() {
                    return null;
                }

                @Override
                public void receiveConfigInfo(String configInfo) {
                    log.info("🔔 收到 Nacos 配置更新推送，DataId: {}", dataId);
                    refreshConfig(configInfo);
                }
            });

            log.info("✅ 成功注册 Nacos 配置监听器，DataId: {}, Group: {}", dataId, GROUP);
        } catch (NacosException e) {
            log.error("❌ 注册 Nacos 配置监听器失败", e);
        }
    }

    /**
     * 刷新配置
     */
    private void refreshConfig(String configContent) {
        try {
            // 解析 YAML 配置
            Yaml yaml = new Yaml();
            Map<String, Object> configMap = yaml.load(configContent);

            if (configMap == null) {
                log.warn("配置内容为空");
                return;
            }

            // 更新 Environment
            MapPropertySource propertySource = new MapPropertySource("nacos-dynamic", flattenMap(configMap));
            environment.getPropertySources().addFirst(propertySource);

            // 重新绑定 @ConfigurationProperties Bean
            if (configMap.containsKey("app")) {
                Map<String, Object> appConfigMap = (Map<String, Object>) configMap.get("app");
                updateAppConfig(appConfigMap);
            }

            // 发布配置变更事件
            Set<String> keys = Set.of("app.version", "app.name", "app.description", "app.features", "app.settings");
            publisher.publishEvent(new EnvironmentChangeEvent(keys));

            log.info("✅ 配置刷新成功");
            logCurrentConfig();

        } catch (Exception e) {
            log.error("❌ 配置刷新失败", e);
        }
    }

    /**
     * 更新 AppConfig Bean
     */
    private void updateAppConfig(Map<String, Object> appConfigMap) {
        if (appConfigMap.containsKey("name")) {
            appConfig.setName((String) appConfigMap.get("name"));
        }
        if (appConfigMap.containsKey("version")) {
            appConfig.setVersion((String) appConfigMap.get("version"));
        }
        if (appConfigMap.containsKey("description")) {
            appConfig.setDescription((String) appConfigMap.get("description"));
        }
        if (appConfigMap.containsKey("features")) {
            appConfig.setFeatures((Map<String, Boolean>) appConfigMap.get("features"));
        }
        if (appConfigMap.containsKey("settings")) {
            appConfig.setSettings((Map<String, String>) appConfigMap.get("settings"));
        }
    }

    /**
     * 扁平化 Map（将嵌套 Map 转为 a.b.c 格式）
     */
    private Map<String, Object> flattenMap(Map<String, Object> map) {
        return flattenMap("", map, new java.util.HashMap<>());
    }

    private Map<String, Object> flattenMap(String prefix, Map<String, Object> map, Map<String, Object> result) {
        for (Map.Entry<String, Object> entry : map.entrySet()) {
            String key = prefix.isEmpty() ? entry.getKey() : prefix + "." + entry.getKey();
            Object value = entry.getValue();

            if (value instanceof Map) {
                flattenMap(key, (Map<String, Object>) value, result);
            } else {
                result.put(key, value);
            }
        }
        return result;
    }

    /**
     * 记录当前配置
     */
    private void logCurrentConfig() {
        log.info("📋 当前配置:");
        log.info("  应用名称: {}", appConfig.getName());
        log.info("  应用版本: {}", appConfig.getVersion());
        log.info("  应用描述: {}", appConfig.getDescription());
        if (appConfig.getFeatures() != null) {
            log.info("  功能开关: {}", appConfig.getFeatures());
        }
        if (appConfig.getSettings() != null) {
            log.info("  应用设置: {}", appConfig.getSettings());
        }
    }
}

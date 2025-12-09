package com.zjgsu.user.listener;

import com.zjgsu.user.config.AppConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.context.scope.refresh.RefreshScopeRefreshedEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.stereotype.Component;

/**
 * Nacos 配置刷新监听器
 * 演示 Spring 事件监听方式监控配置变更
 *
 * 当 Nacos 配置发生变更并推送到服务时，会触发 RefreshScopeRefreshedEvent 事件
 * 可以在此监听器中执行配置变更后的业务逻辑
 *
 * 应用场景：
 * - 配置变更后重新加载缓存
 * - 动态调整限流阈值
 * - 更新业务规则
 * - 通知其他组件配置已更新
 */
@Component
public class NacosConfigListener implements ApplicationListener<RefreshScopeRefreshedEvent> {

    private static final Logger log = LoggerFactory.getLogger(NacosConfigListener.class);

    @Autowired
    private AppConfig appConfig;

    @Override
    public void onApplicationEvent(RefreshScopeRefreshedEvent event) {
        log.info("=".repeat(60));
        log.info("📢 检测到配置刷新事件！");
        log.info("=".repeat(60));

        // 记录当前配置信息
        log.info("🔧 当前配置信息:");
        log.info("  应用名称: {}", appConfig.getName());
        log.info("  应用版本: {}", appConfig.getVersion());
        log.info("  应用描述: {}", appConfig.getDescription());

        if (appConfig.getFeatures() != null) {
            log.info("  功能开关:");
            appConfig.getFeatures().forEach((key, value) ->
                log.info("    {} = {}", key, value)
            );
        }

        if (appConfig.getSettings() != null) {
            log.info("  应用设置:");
            appConfig.getSettings().forEach((key, value) ->
                log.info("    {} = {}", key, value)
            );
        }

        log.info("=".repeat(60));
        log.info("✅ 配置刷新完成，无需重启服务");
        log.info("=".repeat(60));

        // 在这里可以执行配置变更后的业务逻辑
        handleConfigChange();
    }

    /**
     * 处理配置变更的业务逻辑
     */
    private void handleConfigChange() {
        // 示例：检查功能开关变化
        if (appConfig.getFeatures() != null) {
            Boolean cacheEnabled = appConfig.getFeatures().get("cache-enabled");
            if (cacheEnabled != null) {
                if (cacheEnabled) {
                    log.info("🔄 缓存功能已启用，开始初始化缓存...");
                    // initializeCache();
                } else {
                    log.info("🔄 缓存功能已禁用，清除缓存数据...");
                    // clearCache();
                }
            }

            Boolean asyncEnabled = appConfig.getFeatures().get("async-enabled");
            if (asyncEnabled != null) {
                log.info("🔄 异步功能状态: {}", asyncEnabled ? "启用" : "禁用");
            }
        }

        // 可以在这里添加更多业务逻辑
        // 例如：
        // - 重新加载业务规则
        // - 更新限流配置
        // - 刷新第三方服务连接
        // - 通知其他微服务
    }
}

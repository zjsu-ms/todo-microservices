package com.zjgsu.user.listener;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.context.refresh.ContextRefresher;
import org.springframework.cloud.endpoint.event.RefreshEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.stereotype.Component;

/**
 * Nacos 配置变更监听器
 * 监听配置变更并主动触发 Spring Context 刷新
 */
@Component
public class NacosConfigChangeListener implements ApplicationListener<RefreshEvent> {

    private static final Logger log = LoggerFactory.getLogger(NacosConfigChangeListener.class);

    @Autowired(required = false)
    private ContextRefresher contextRefresher;

    @Override
    public void onApplicationEvent(RefreshEvent event) {
        log.info("🔔 收到 Nacos 配置变更事件");

        if (contextRefresher != null) {
            try {
                log.info("🔄 开始刷新配置上下文...");
                contextRefresher.refresh();
                log.info("✅ 配置上下文刷新成功");
            } catch (Exception e) {
                log.error("❌ 配置上下文刷新失败", e);
            }
        }
    }
}

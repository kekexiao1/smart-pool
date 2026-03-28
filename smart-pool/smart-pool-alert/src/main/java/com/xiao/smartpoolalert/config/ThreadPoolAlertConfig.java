package com.xiao.smartpoolalert.config;

import com.xiao.smartpoolalert.rule.AlertRule;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 线程池告警配置
 * 支持每个线程池独立的告警规则配置
 */
@Data
@ConfigurationProperties(prefix = "thread-pool")
public class ThreadPoolAlertConfig {
    
    /**
     * 线程池告警配置映射
     * key: 线程池名称
     * value: 该线程池的告警配置
     */
    private Map<String, PoolAlertConfig> pools = new HashMap<>();
    
    /**
     * 默认告警配置（用于未明确配置的线程池）
     */
    private PoolAlertConfig defaultConfig = new PoolAlertConfig();
    
    @Data
    public static class PoolAlertConfig {
        /**
         * 告警规则项
         */
        private List<AlertRule> rules = new ArrayList<>();
        
        /**
         * 监控间隔（毫秒）
         */
        private int monitorInterval = 5000;
    }
}
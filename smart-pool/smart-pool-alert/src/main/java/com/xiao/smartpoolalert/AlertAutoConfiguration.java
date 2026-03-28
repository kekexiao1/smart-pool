package com.xiao.smartpoolalert;

import com.xiao.smartpoolalert.callback.PoolAlertLogService;
import com.xiao.smartpoolalert.callback.impl.NoPoolAlertLogServiceImpl;
import com.xiao.smartpoolalert.config.AlertProperties;
import com.xiao.smartpoolalert.config.ThreadPoolAlertConfig;
import com.xiao.smartpoolalert.manager.AlertInitializer;
import com.xiao.smartpoolalert.manager.AlertManager;
import com.xiao.smartpoolalert.monitor.ThreadPoolMonitorScheduler;
import com.xiao.smartpoolalert.monitor.engine.AlertEngine;
import com.xiao.smartpoolalert.util.ApplicationContextHolder;
import com.xiao.smartpoolcore.core.CoreAutoConfiguration;
import com.xiao.smartpoolmetrics.MetricsAutoConfiguration;
import com.xiao.smartpoolmetrics.metrics.ThreadPoolMetricCollector;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.AutoConfigureAfter;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;

/**
 * 告警模块自动配置
 */
@Slf4j
@AutoConfiguration
@EnableConfigurationProperties({AlertProperties.class, ThreadPoolAlertConfig.class})
@AutoConfigureAfter({CoreAutoConfiguration.class, MetricsAutoConfiguration.class})
public class AlertAutoConfiguration {

    @Bean
    public ApplicationContextHolder applicationContextHolder() {
        log.info("ApplicationContextHolder 配置完成");
        return new ApplicationContextHolder();
    }

    @Bean
    @ConditionalOnMissingBean(PoolAlertLogService.class)
    public AlertManager alertManager() {
        log.info("使用空实现的PoolAlertLogService");
        return new AlertManager(new NoPoolAlertLogServiceImpl());
    }
    
    @Bean
    @ConditionalOnBean(PoolAlertLogService.class)
    public AlertManager alertManagerWithRealService(PoolAlertLogService poolAlertLogService) {
        log.info("使用真实的PoolAlertLogService实现");
        return new AlertManager(poolAlertLogService);
    }

    @Bean
    @ConditionalOnBean({ThreadPoolMetricCollector.class, AlertManager.class})
    public AlertEngine alertEngine(AlertManager alertManager, ThreadPoolMetricCollector metricCollector){
        log.info("AlertEngine 配置完成");
        return new AlertEngine(alertManager, metricCollector);
    }

    @Bean
    public AlertInitializer alertInitializer(AlertManager alertManager,
                                             AlertProperties alertProperties,
                                             ThreadPoolAlertConfig threadPoolAlertConfig) {
        log.info("AlertInitializer 配置完成");
        return new AlertInitializer(alertManager, alertProperties, threadPoolAlertConfig);
    }

    @Bean
    @ConditionalOnBean({AlertEngine.class, ThreadPoolMetricCollector.class})
    public ThreadPoolMonitorScheduler threadPoolMonitorScheduler(AlertEngine alertEngine,
                                                                 ThreadPoolAlertConfig threadPoolAlertConfig,
                                                                 ThreadPoolMetricCollector threadPoolMetricCollector){
        return new ThreadPoolMonitorScheduler(alertEngine, threadPoolAlertConfig, threadPoolMetricCollector);
    }

}
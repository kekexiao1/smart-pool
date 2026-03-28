package com.xiao.smartpoolstarter;

import com.xiao.smartpoolalert.AlertAutoConfiguration;
import com.xiao.smartpoolcore.core.CoreAutoConfiguration;
import com.xiao.smartpoolmetrics.MetricsAutoConfiguration;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

/**
 * 动态线程池 Starter 自动配置类
 * 依赖于 smart-pool-core 的自动配置，提供额外的功能集成
 */

@Slf4j
@EnableConfigurationProperties
@AutoConfiguration
@ConditionalOnProperty(prefix = "smart-pool", name = "enabled", havingValue = "true", matchIfMissing = true)
@ConditionalOnClass({AlertAutoConfiguration.class, CoreAutoConfiguration.class,
		MetricsAutoConfiguration.class})
public class SmartPoolAutoConfiguration {

}
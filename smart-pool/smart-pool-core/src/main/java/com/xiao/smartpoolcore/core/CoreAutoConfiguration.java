package com.xiao.smartpoolcore.core;

import com.alibaba.cloud.nacos.NacosConfigManager;
import com.xiao.smartpoolcore.callback.PoolLogService;
import com.xiao.smartpoolcore.callback.impl.NoPoolLogServiceImpl;
import com.xiao.smartpoolcore.common.util.ApplicationContextHolder;
import com.xiao.smartpoolcore.core.handler.ConfigChangeHandler;
import com.xiao.smartpoolcore.core.manager.DynamicThreadPoolManager;
import com.xiao.smartpoolcore.core.registry.ThreadPoolRegistry;
import com.xiao.smartpoolcore.listener.nacos.NacosConfigListener;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;

@AutoConfiguration
public class CoreAutoConfiguration {

	@Bean
	public ApplicationContextHolder applicationContextHolder() {
		return new ApplicationContextHolder();
	}

	@Bean
	public ThreadPoolRegistry threadPoolRegistry() {
		return new ThreadPoolRegistry();
	}

	@Bean
	@ConditionalOnBean(PoolLogService.class)
	public ConfigChangeHandler configChangeHandlerWithRealService(ThreadPoolRegistry registry,
																  PoolLogService poolLogService) {
		return new ConfigChangeHandler(registry, poolLogService);
	}

	@Bean
	@ConditionalOnMissingBean(PoolLogService.class)
	public ConfigChangeHandler configChangeHandler(ThreadPoolRegistry registry) {
		return new ConfigChangeHandler(registry, new NoPoolLogServiceImpl());
	}

	@Bean
	@ConditionalOnProperty(prefix = "spring.cloud.nacos.config", name = "server-addr")
	public NacosConfigListener nacosConfigListener(ConfigChangeHandler configChangeHandler,
												   NacosConfigManager nacosConfigManager) {
		return new NacosConfigListener(configChangeHandler, nacosConfigManager);
	}

	@Bean
	@ConditionalOnProperty(prefix = "spring.cloud.nacos.config", name = "server-addr")
	public DynamicThreadPoolManager dynamicThreadPoolManager(ThreadPoolRegistry registry,
															 ConfigChangeHandler configChangeHandler,
															 NacosConfigListener nacosConfigListener,
															 ApplicationContextHolder applicationContextHolder) {
		return new DynamicThreadPoolManager(registry, configChangeHandler, nacosConfigListener);
	}
}

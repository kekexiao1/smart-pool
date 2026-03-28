package com.xiao.smartpoolmetrics;

import com.xiao.smartpoolcore.core.CoreAutoConfiguration;
import com.xiao.smartpoolcore.core.registry.ThreadPoolRegistry;
import com.xiao.smartpoolmetrics.metrics.ThreadPoolMetricCollector;
import com.xiao.smartpoolmetrics.monitor.ThreadPoolMonitor;
import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.AutoConfigureAfter;
import org.springframework.context.annotation.Bean;
import lombok.extern.slf4j.Slf4j;

@AutoConfiguration
@AutoConfigureAfter(CoreAutoConfiguration.class)
@Slf4j
public class MetricsAutoConfiguration {

	@Bean
	public ThreadPoolMetricCollector threadPoolMetricCollector(ThreadPoolRegistry registry){
		log.info("ThreadPoolMetricCollector 配置完成");
		return new ThreadPoolMetricCollector(registry);
	}

	@Bean
	public ThreadPoolMonitor threadPoolMonitor(ThreadPoolRegistry threadPoolRegistry,
									   MeterRegistry meterRegistry){
		log.info("ThreadPoolMonitor 配置完成");
		return new ThreadPoolMonitor(threadPoolRegistry, meterRegistry);
	}

}

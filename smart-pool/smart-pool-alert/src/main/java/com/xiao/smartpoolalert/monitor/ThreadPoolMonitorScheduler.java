package com.xiao.smartpoolalert.monitor;

import com.xiao.smartpoolalert.config.ThreadPoolAlertConfig;
import com.xiao.smartpoolalert.monitor.engine.AlertEngine;
import com.xiao.smartpoolmetrics.metrics.ThreadPoolMetricCollector;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

@RequiredArgsConstructor
@Slf4j
public class ThreadPoolMonitorScheduler {
	private final AlertEngine alertEngine;
	private final ThreadPoolAlertConfig threadPoolAlertConfig;
	private final ThreadPoolMetricCollector metricCollector;
	
	// 记录每个线程池的上次检查时间
	private final Map<String, Long> lastCheckTimeMap = new ConcurrentHashMap<>();
	
	// 基础检查间隔（毫秒），用于轮询检查
	private static final int BASE_CHECK_INTERVAL = 1000;


	@Scheduled(fixedDelay = BASE_CHECK_INTERVAL)
	public void collectAndAlert(){
		// 获取所有配置的线程池
		Map<String, ThreadPoolAlertConfig.PoolAlertConfig> pools = threadPoolAlertConfig.getPools();
		
		// 检查每个线程池是否需要执行监控
		pools.forEach((poolName, poolConfig) -> {
			long currentTime = System.currentTimeMillis();
			long lastCheckTime = lastCheckTimeMap.getOrDefault(poolName, 0L);
			int monitorInterval = poolConfig.getMonitorInterval();
			
			// 检查是否达到监控间隔
			if (currentTime - lastCheckTime >= monitorInterval) {
				try {
					alertEngine.checkSingleThreadPool(poolName);
					lastCheckTimeMap.put(poolName, currentTime);
					log.debug("线程池 {} 监控检查完成，监控间隔: {}ms", poolName, monitorInterval);
				} catch (Exception e) {
					log.error("线程池 {} 监控检查异常: {}", poolName, e.getMessage());
				}
			}
		});
		
		// 检查未明确配置的线程池（使用默认配置）
		checkDefaultPools();
	}
	
	/**
	 * 检查使用默认配置的线程池
	 */
	private void checkDefaultPools() {
		// 获取所有已注册的线程池名称
		Set<String> allPoolNames = metricCollector.getAllPoolNames();
		
		// 过滤出未在配置中明确配置的线程池
		allPoolNames.stream()
			.filter(poolName -> !threadPoolAlertConfig.getPools().containsKey(poolName))
			.forEach(poolName -> {
				long currentTime = System.currentTimeMillis();
				long lastCheckTime = lastCheckTimeMap.getOrDefault(poolName, 0L);
				int defaultMonitorInterval = threadPoolAlertConfig.getDefaultConfig().getMonitorInterval();
				
				// 检查是否达到默认监控间隔
				if (currentTime - lastCheckTime >= defaultMonitorInterval) {
					try {
						alertEngine.checkSingleThreadPool(poolName);
						lastCheckTimeMap.put(poolName, currentTime);
						log.debug("默认配置线程池 {} 监控检查完成，监控间隔: {}ms", poolName, defaultMonitorInterval);
					} catch (Exception e) {
						log.error("默认配置线程池 {} 监控检查异常: {}", poolName, e.getMessage());
					}
				}
			});
	}
	
	/**
	 * 获取所有已注册的线程池名称
	 */
	public Set<String> getAllPoolNames() {
		return metricCollector.getAllPoolNames();
	}
	
	/**
	 * 手动触发指定线程池的监控检查
	 */
	public void triggerManualCheck(String poolName) {
		alertEngine.checkSingleThreadPool(poolName);
		lastCheckTimeMap.put(poolName, System.currentTimeMillis());
		log.info("手动触发线程池 {} 监控检查", poolName);
	}
}

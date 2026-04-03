package com.xiao.smartpoolmetrics.metrics;

import com.alibaba.nacos.api.utils.StringUtils;
import com.xiao.smartpoolcore.config.CountingRejectedExecutionHandler;
import com.xiao.smartpoolcore.core.executor.DynamicThreadPoolExecutor;
import com.xiao.smartpoolcore.core.registry.ThreadPoolRegistry;
import com.xiao.smartpoolmetrics.monitor.ThreadPoolMonitor;
import io.micrometer.core.instrument.Timer;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import java.util.Date;
import java.util.Set;
import java.util.concurrent.RejectedExecutionHandler;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

@RequiredArgsConstructor
@Slf4j
public class ThreadPoolMetricCollector {

	private final ThreadPoolRegistry registry;

	public Set<String> getAllPoolNames(){
		return registry.getAllThreadPoolNames();
	}


	public ThreadPoolMetrics currentMetrics(String poolName){
		if(StringUtils.isEmpty(poolName)){
			throw new RuntimeException("采集指标时，线程池名字不能为空");
		}
		DynamicThreadPoolExecutor dynamicExecutor = registry.getExecutor(poolName);

		if(dynamicExecutor==null){
			throw new RuntimeException("采集指标时，线程池: ["+ poolName + "] 不能为空");
		}

		ThreadPoolExecutor executor = dynamicExecutor.getExecutor();
		//队列容量
		int queueCapacity=executor.getQueue().size() + executor.getQueue().remainingCapacity();
		// 获取拒绝策略
		RejectedExecutionHandler handler = executor.getRejectedExecutionHandler();
		long rejectedCount=0;
		if(handler instanceof CountingRejectedExecutionHandler){
			CountingRejectedExecutionHandler countingHandler = (CountingRejectedExecutionHandler) handler;
			rejectedCount = countingHandler.getRejectedCount();
			// 重置
			countingHandler.setRejectedCount(0);
		}

		// 获取任务执行时间统计
		double avgExecutionTime = getAverageExecutionTime(poolName);
		double maxExecutionTime = getMaxExecutionTime(poolName);

		ThreadPoolMetrics threadPoolMetrics = ThreadPoolMetrics.builder()
				.poolName(poolName)
				.activeCount(executor.getActiveCount())
				.maximumPoolSize(executor.getMaximumPoolSize())
				.poolSize(executor.getPoolSize())
				.queueCapacity(queueCapacity)
				.queueSize(executor.getQueue().size())
				.completedTaskCount(executor.getCompletedTaskCount())
				.taskCount(executor.getTaskCount())
				.rejectedTask(rejectedCount>0 ? true:false)
				.avgExecutionTime(avgExecutionTime)
				.maxExecutionTime(maxExecutionTime)
				.timestamp(new Date().getTime())
				.build();
		return threadPoolMetrics;
	}

	/**
	 * 获取平均任务执行时间（毫秒）
	 */
	private double getAverageExecutionTime(String poolName) {
		try {
			Timer executeTimer = ThreadPoolMonitor.EXECUTE_TIMER_CACHE.get(poolName);
			if (executeTimer != null) {
				// 从Timer中获取平均执行时间（转换为毫秒）
				return executeTimer.totalTime(TimeUnit.MILLISECONDS) / Math.max(1, executeTimer.count());
			}
		} catch (Exception e) {
			// 忽略异常，返回默认值
		}
		return 0.0;
	}

	/**
	 * 获取最大任务执行时间（毫秒）
	 */
	private double getMaxExecutionTime(String poolName) {
		try {
			Timer executeTimer = ThreadPoolMonitor.EXECUTE_TIMER_CACHE.get(poolName);
			if (executeTimer != null) {
				// 从Timer中获取最大执行时间（转换为毫秒）
				// 注意：Micrometer Timer的max方法返回的是最近时间窗口内的最大值
				return executeTimer.max(TimeUnit.MILLISECONDS);
			}
		} catch (Exception e) {
			// 忽略异常，返回默认值
			log.warn("获取线程池 {} 的最大执行时间失败: {}", poolName, e.getMessage());
		}
		return 0.0;
	}
}

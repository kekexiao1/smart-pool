package com.xiao.smartpooladminserver.service;

import com.xiao.smartpooladminserver.model.dto.ThreadPoolAccumulateMetricsDTO;
import com.xiao.smartpooladminserver.model.dto.ThreadPoolRealTimeMetricsDTO;
import com.xiao.smartpooladminserver.model.vo.ThreadPoolMetricsVO;
import com.xiao.smartpoolcore.core.executor.DynamicThreadPoolExecutor;
import com.xiao.smartpoolcore.core.registry.ThreadPoolRegistry;
import com.xiao.smartpoolcore.model.dto.ThreadPoolConfig;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class ThreadPoolMetricsService {

	private final ThreadPoolRegistry registry;

	public List<ThreadPoolMetricsVO> getAllThreadPoolMetrics() {
		Map<String, DynamicThreadPoolExecutor> allExecutors = registry.getAllExecutors();
		return allExecutors.entrySet()
				.stream()
				.map(entry -> buildThreadPoolMetricsVO(entry.getKey(), entry.getValue()))
				.collect(Collectors.toList());
	}

	public void executeTestTask(String poolName, int taskCount) {
		DynamicThreadPoolExecutor executor = registry.getExecutor(poolName);
		if (executor == null) {
			throw new IllegalArgumentException("线程池[" + poolName + "]不存在");
		}

		for (int i = 0; i < taskCount; i++) {
			int taskId = i + 1;
			executor.execute(() -> {
				try {
					Thread.sleep(1000);
					log.info("线程 [{}] 成功第{}次执行任务", poolName, taskId);
				} catch (InterruptedException e) {
					Thread.currentThread().interrupt();
				}
			});
		}
	}

	public String testQueueFullAlert(String poolName, int durationSeconds) {
		DynamicThreadPoolExecutor executor = registry.getExecutor(poolName);
		if (executor == null) {
			throw new IllegalArgumentException("线程池[" + poolName + "]不存在");
		}

		ThreadPoolConfig config = executor.getConfig();
		int queueCapacity = config.getQueueCapacity();
		queueCapacity=520;
		int targetQueueSize = (int) (queueCapacity * 0.9);

		log.info("开始测试ThreadPoolQueueFull告警 - 线程池: {}, 队列容量: {}, 目标队列大小: {}, 持续时间: {}秒",
				poolName, queueCapacity, targetQueueSize, durationSeconds);

		for (int i = 0; i < targetQueueSize; i++) {
			int taskId = i + 1;
			executor.execute(() -> {
				try {
					Thread.sleep(durationSeconds * 1000L);
					log.info("告警测试任务[{}]完成", taskId);
				} catch (InterruptedException e) {
					Thread.currentThread().interrupt();
				}
			});
		}

		return String.format("ThreadPoolQueueFull告警测试已启动。线程池: %s, 当前队列大小: %d/%d (%.1f%%), 测试将持续: %d秒",
				poolName, executor.getQueueSize(), queueCapacity,
				(executor.getQueueSize() * 100.0 / queueCapacity), durationSeconds);
	}

	public String testRunTimeoutAlert(String poolName, int durationSeconds) {
		DynamicThreadPoolExecutor executor = registry.getExecutor(poolName);
		if (executor == null) {
			throw new IllegalArgumentException("线程池[" + poolName + "]不存在");
		}

		log.info("开始测试ThreadPoolRunTimeout告警 - 线程池: {}, 持续时间: {}秒",
				poolName, durationSeconds);

		executor.execute(() -> {
			try {
				Thread.sleep(durationSeconds * 1000L);
				log.info("执行超时告警测试任务完成，执行时间: {}秒", durationSeconds);
			} catch (InterruptedException e) {
				Thread.currentThread().interrupt();
			}
		});

		return String.format("ThreadPoolRunTimeout告警测试已启动。线程池: %s, 任务执行时间: %d秒, 超时阈值: 5秒",
				poolName, durationSeconds);
	}

	private ThreadPoolMetricsVO buildThreadPoolMetricsVO(String poolName, DynamicThreadPoolExecutor executor) {
		ThreadPoolConfig config = executor.getConfig();

		ThreadPoolAccumulateMetricsDTO accumulateMetrics = ThreadPoolAccumulateMetricsDTO.builder()
				.exceptionCount(executor.getExceptionCount())
				.rejectCount(executor.getRejectedCount())
				.completedTaskCount(executor.getCompletedTaskCount())
				.build();

		ThreadPoolRealTimeMetricsDTO realTimeMetricsDTO = ThreadPoolRealTimeMetricsDTO.builder()
				.activeCount(executor.getActiveCount())
				.queueSize(executor.getQueueSize())
				.currentPoolSize(executor.getPoolSize())
				.activeThreadRate(executor.getActiveThreadRate())
				.queueRemainingCapacity(executor.getQueueRemainingCapacity())
				.build();

		return ThreadPoolMetricsVO.builder()
				.threadPoolName(poolName)
				.config(config)
				.realTimeMetrics(realTimeMetricsDTO)
				.accumulateMetrics(accumulateMetrics)
				.build();
	}
}

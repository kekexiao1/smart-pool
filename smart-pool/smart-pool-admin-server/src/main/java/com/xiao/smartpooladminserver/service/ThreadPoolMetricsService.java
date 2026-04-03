package com.xiao.smartpooladminserver.service;

import com.xiao.smartpooladminserver.model.dto.ThreadPoolAccumulateMetricsDTO;
import com.xiao.smartpooladminserver.model.dto.ThreadPoolRealTimeMetricsDTO;
import com.xiao.smartpooladminserver.model.vo.ThreadPoolMetricsVO;
import com.xiao.smartpoolcore.config.CountingRejectedExecutionHandler;
import com.xiao.smartpoolcore.core.executor.DynamicThreadPoolExecutor;
import com.xiao.smartpoolcore.core.registry.ThreadPoolRegistry;
import com.xiao.smartpoolcore.model.dto.ThreadPoolConfig;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.concurrent.RejectedExecutionHandler;
import java.util.concurrent.ThreadPoolExecutor;
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


	private ThreadPoolMetricsVO buildThreadPoolMetricsVO(String poolName, DynamicThreadPoolExecutor dynamicExecutor) {
		ThreadPoolConfig config = dynamicExecutor.getConfig();

		ThreadPoolExecutor executor = dynamicExecutor.getExecutor();
		RejectedExecutionHandler handler = executor.getRejectedExecutionHandler();
		long rejectCount=0;
		if(handler instanceof CountingRejectedExecutionHandler){
			CountingRejectedExecutionHandler countingHandler=(CountingRejectedExecutionHandler) handler;
			rejectCount=countingHandler.getRejectedCount();
		}

		ThreadPoolAccumulateMetricsDTO accumulateMetrics = ThreadPoolAccumulateMetricsDTO.builder()
				.rejectCount(rejectCount)
				.completedTaskCount(executor.getCompletedTaskCount())
				.build();

		ThreadPoolRealTimeMetricsDTO realTimeMetricsDTO = ThreadPoolRealTimeMetricsDTO.builder()
				.activeCount(dynamicExecutor.getActiveCount())
				.queueSize(dynamicExecutor.getQueueSize())
				.currentPoolSize(dynamicExecutor.getPoolSize())
				.activeThreadRate(dynamicExecutor.getActiveThreadRate())
				.queueRemainingCapacity(dynamicExecutor.getQueueRemainingCapacity())
				.build();

		return ThreadPoolMetricsVO.builder()
				.threadPoolName(poolName)
				.config(config)
				.realTimeMetrics(realTimeMetricsDTO)
				.accumulateMetrics(accumulateMetrics)
				.build();
	}
}

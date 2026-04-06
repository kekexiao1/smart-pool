package com.xiao.smartpooladminserver.service.impl;

import com.xiao.smartpooladminserver.model.dto.ThreadPoolAccumulateMetricsDTO;
import com.xiao.smartpooladminserver.model.dto.ThreadPoolRealTimeMetricsDTO;
import com.xiao.smartpooladminserver.service.ThreadPoolMetricsCollectorService;
import com.xiao.smartpoolcore.config.DynamicCapacityBlockingQueue;
import com.xiao.smartpoolcore.core.executor.DynamicThreadPoolExecutor;
import com.xiao.smartpoolcore.core.registry.ThreadPoolRegistry;
import com.xiao.smartpoolcore.config.CountingRejectedExecutionHandler;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.RejectedExecutionHandler;
import java.util.concurrent.ThreadPoolExecutor;

@Slf4j
@Service
@RequiredArgsConstructor
public class ThreadPoolMetricsCollectorServiceImpl implements ThreadPoolMetricsCollectorService {

    private final ThreadPoolRegistry registry;
    private final ThreadPoolMetricsRedisServiceImpl metricsRedisService;
    
    // 采集间隔（5秒）
    private static final long COLLECTION_INTERVAL_MS = 5000;
    
    /**
     * 定时采集线程池指标并保存到Redis
     */
    @Scheduled(fixedRate = COLLECTION_INTERVAL_MS)
    public void collectAndStoreMetrics() {

        try {
            Map<String, DynamicThreadPoolExecutor> allExecutors = registry.getAllExecutors();
            
            if (allExecutors.isEmpty()) {
                log.info("未发现注册的线程池，跳过指标采集");
                return;
            }
            
            for (Map.Entry<String, DynamicThreadPoolExecutor> entry : allExecutors.entrySet()) {
                String poolName = entry.getKey();
                DynamicThreadPoolExecutor dynamicExecutor = entry.getValue();
                
                try {
                    // 收集实时指标
                    ThreadPoolRealTimeMetricsDTO realTimeMetrics = collectRealTimeMetrics(dynamicExecutor);
                    
                    // 收集累计指标
                    ThreadPoolAccumulateMetricsDTO accumulateMetrics = collectAccumulateMetrics(dynamicExecutor);
                    
                    // 保存到Redis
                    metricsRedisService.saveMetrics(poolName, realTimeMetrics, accumulateMetrics);
                    
                    log.info("成功采集线程池 {} 指标", poolName);
                } catch (Exception e) {
                    log.error("采集线程池 {} 指标时发生错误: {}", poolName, e.getMessage(), e);
                }
            }
            
            log.debug("完成所有线程池指标采集，共采集 {} 个线程池", allExecutors.size());
        } catch (Exception e) {
            log.error("指标采集任务执行失败: {}", e.getMessage(), e);
        }
    }
    
    /**
     * 收集实时指标
     */
    private ThreadPoolRealTimeMetricsDTO collectRealTimeMetrics(DynamicThreadPoolExecutor dynamicExecutor) {
        
        return ThreadPoolRealTimeMetricsDTO.builder()
                .activeCount(dynamicExecutor.getActiveCount())
                .queueSize(dynamicExecutor.getQueueSize())
                .currentPoolSize(dynamicExecutor.getPoolSize())
                .activeThreadRate(dynamicExecutor.getActiveThreadRate())
                .queueRemainingCapacity(dynamicExecutor.getQueueRemainingCapacity())
                .build();
    }
    
    /**
     * 收集累计指标
     */
    private ThreadPoolAccumulateMetricsDTO collectAccumulateMetrics(DynamicThreadPoolExecutor dynamicExecutor) {
        ThreadPoolExecutor executor = dynamicExecutor.getExecutor();
        RejectedExecutionHandler handler = executor.getRejectedExecutionHandler();
        long rejectCount = 0;
        
        if (handler instanceof CountingRejectedExecutionHandler) {
            CountingRejectedExecutionHandler countingHandler = (CountingRejectedExecutionHandler) handler;
            rejectCount = countingHandler.getRejectedCount();
        }

        BlockingQueue<Runnable> queue = dynamicExecutor.getExecutor().getQueue();
        long avgWaitTime=0;
        if(queue instanceof DynamicCapacityBlockingQueue){
            DynamicCapacityBlockingQueue<?> dynamicQueue=(DynamicCapacityBlockingQueue<?>) queue;
            avgWaitTime=dynamicQueue.getAvgWaitTime();
        }
        return ThreadPoolAccumulateMetricsDTO.builder()
                .rejectCount(rejectCount)
                .avgWaitTime(avgWaitTime)
                .completedTaskCount(executor.getCompletedTaskCount())
                .build();
    }
    
    /**
     * 手动触发指标采集
     */
    public void triggerManualCollection() {
        log.info("手动触发指标采集");
        collectAndStoreMetrics();
    }

//    public List<ThreadPoolMetricsVO> getAllThreadPoolMetrics() {
//        Map<String, DynamicThreadPoolExecutor> allExecutors = registry.getAllExecutors();
//        return allExecutors.entrySet()
//                .stream()
//                .map(entry -> buildThreadPoolMetricsVO(entry.getKey(), entry.getValue()))
//                .collect(Collectors.toList());
//    }
//
//
//    private ThreadPoolMetricsVO buildThreadPoolMetricsVO(String poolName, DynamicThreadPoolExecutor dynamicExecutor) {
//        ThreadPoolConfig config = dynamicExecutor.getConfig();
//
//        ThreadPoolExecutor executor = dynamicExecutor.getExecutor();
//        RejectedExecutionHandler handler = executor.getRejectedExecutionHandler();
//        long rejectCount=0;
//        if(handler instanceof CountingRejectedExecutionHandler){
//            CountingRejectedExecutionHandler countingHandler=(CountingRejectedExecutionHandler) handler;
//            rejectCount=countingHandler.getRejectedCount();
//        }
//
//        ThreadPoolAccumulateMetricsDTO accumulateMetrics = ThreadPoolAccumulateMetricsDTO.builder()
//                .rejectCount(rejectCount)
//                .completedTaskCount(executor.getCompletedTaskCount())
//                .build();
//
//        ThreadPoolRealTimeMetricsDTO realTimeMetricsDTO = ThreadPoolRealTimeMetricsDTO.builder()
//                .activeCount(dynamicExecutor.getActiveCount())
//                .queueSize(dynamicExecutor.getQueueSize())
//                .currentPoolSize(dynamicExecutor.getPoolSize())
//                .activeThreadRate(dynamicExecutor.getActiveThreadRate())
//                .queueRemainingCapacity(dynamicExecutor.getQueueRemainingCapacity())
//                .build();
//
//        return ThreadPoolMetricsVO.builder()
//                .threadPoolName(poolName)
//                .config(config)
//                .realTimeMetrics(realTimeMetricsDTO)
//                .accumulateMetrics(accumulateMetrics)
//                .build();
//    }
}
package com.xiao.smartpooladminserver.service;

import com.xiao.smartpooladminserver.model.dto.ThreadPoolMetricsHistoryDTO;
import com.xiao.smartpooladminserver.model.dto.ThreadPoolRealTimeMetricsDTO;
import com.xiao.smartpooladminserver.model.dto.ThreadPoolAccumulateMetricsDTO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeUnit;

@Slf4j
@Service
@RequiredArgsConstructor
public class ThreadPoolMetricsRedisService {

    private final RedisTemplate<String, Object> redisTemplate;
    
    private static final String METRICS_HISTORY_KEY_PREFIX = "threadpool:metrics:history:";
    private static final String LATEST_METRICS_KEY_PREFIX = "threadpool:metrics:latest:";
    private static final String METRICS_TIMESERIES_KEY_PREFIX = "threadpool:metrics:timeseries:";
    
    // 指标过期时间7天
    private static final long METRICS_TTL_SECONDS = 7 * 24 * 60 * 60;
    
    // 时间序列过期时间
    private static final long TIMESERIES_TTL_SECONDS = 30 * 24 * 60 * 60;
    
    /**
     * 保存线程池指标到Redis
     */
    public void saveMetrics(String threadPoolName, ThreadPoolRealTimeMetricsDTO realTimeMetrics, 
                           ThreadPoolAccumulateMetricsDTO accumulateMetrics) {
        LocalDateTime now = LocalDateTime.now();
        ThreadPoolMetricsHistoryDTO metricsHistory = ThreadPoolMetricsHistoryDTO.builder()
                .threadPoolName(threadPoolName)
                .realTimeMetrics(realTimeMetrics)
                .accumulateMetrics(accumulateMetrics)
                .timestamp(now)
                .build();
        
        // 保存最新指标
        String latestKey = LATEST_METRICS_KEY_PREFIX + threadPoolName;
        redisTemplate.opsForValue().set(latestKey, metricsHistory, METRICS_TTL_SECONDS, TimeUnit.SECONDS);
        
        // 保存历史记录（使用时间戳作为score）
        String historyKey = METRICS_HISTORY_KEY_PREFIX + threadPoolName;
        double timestampScore = System.currentTimeMillis();
        redisTemplate.opsForZSet().add(historyKey, metricsHistory, timestampScore);
        
        // 设置历史记录的TTL
        redisTemplate.expire(historyKey, METRICS_TTL_SECONDS, TimeUnit.SECONDS);
        
        // 保存时间序列数据（用于图表展示）
        saveTimeSeriesData(threadPoolName, realTimeMetrics, accumulateMetrics, now);
        
        log.debug("保存线程池 {} 指标到Redis", threadPoolName);
    }
    
    /**
     * 保存时间序列数据（简化版，用于图表展示）
     */
    private void saveTimeSeriesData(String threadPoolName, ThreadPoolRealTimeMetricsDTO realTimeMetrics,
                                   ThreadPoolAccumulateMetricsDTO accumulateMetrics, LocalDateTime timestamp) {
        String timeSeriesKey = METRICS_TIMESERIES_KEY_PREFIX + threadPoolName;
        
        // 使用Hash存储每个时间点的关键指标
        String timestampKey = timestamp.format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss"));
        // 线程名：时间
        String hashKey = timeSeriesKey + ":" + timestampKey;
        
        redisTemplate.opsForHash().put(hashKey, "activeCount", realTimeMetrics.getActiveCount());
        redisTemplate.opsForHash().put(hashKey, "queueSize", realTimeMetrics.getQueueSize());
        redisTemplate.opsForHash().put(hashKey, "currentPoolSize", realTimeMetrics.getCurrentPoolSize());
        redisTemplate.opsForHash().put(hashKey, "activeThreadRate", realTimeMetrics.getActiveThreadRate());
        redisTemplate.opsForHash().put(hashKey, "rejectCount", accumulateMetrics.getRejectCount());
        redisTemplate.opsForHash().put(hashKey, "completedTaskCount", accumulateMetrics.getCompletedTaskCount());
        redisTemplate.opsForHash().put(hashKey, "timestamp", timestamp.toString());
        
        // 设置TTL
        redisTemplate.expire(hashKey, TIMESERIES_TTL_SECONDS, TimeUnit.SECONDS);
        
        // 维护时间序列索引
        String indexKey = timeSeriesKey + ":index";
        redisTemplate.opsForZSet().add(indexKey, timestampKey, System.currentTimeMillis());
        redisTemplate.expire(indexKey, TIMESERIES_TTL_SECONDS, TimeUnit.SECONDS);
    }
    
    /**
     * 获取线程池的最新指标
     */
    public ThreadPoolMetricsHistoryDTO getLatestMetrics(String threadPoolName) {
        String key = LATEST_METRICS_KEY_PREFIX + threadPoolName;
        return (ThreadPoolMetricsHistoryDTO) redisTemplate.opsForValue().get(key);
    }
    
    /**
     * 获取所有线程池的最新指标
     */
    public List<ThreadPoolMetricsHistoryDTO> getAllLatestMetrics() {
        List<ThreadPoolMetricsHistoryDTO> result = new ArrayList<>();
        
        // 获取所有最新指标key
        Set<String> keys = redisTemplate.keys(LATEST_METRICS_KEY_PREFIX + "*");
        if (keys != null) {
            for (String key : keys) {
                ThreadPoolMetricsHistoryDTO metrics = (ThreadPoolMetricsHistoryDTO) redisTemplate.opsForValue().get(key);
                if (metrics != null) {
                    result.add(metrics);
                }
            }
        }
        
        return result;
    }
    
    /**
     * 获取线程池的历史指标（最近N条）
     */
    public List<ThreadPoolMetricsHistoryDTO> getMetricsHistory(String threadPoolName, int limit) {
        List<ThreadPoolMetricsHistoryDTO> result = new ArrayList<>();
        String key = METRICS_HISTORY_KEY_PREFIX + threadPoolName;
        
        Set<Object> metricsSet = redisTemplate.opsForZSet().reverseRange(key, 0, limit - 1);
        if (metricsSet != null) {
            for (Object metrics : metricsSet) {
                result.add((ThreadPoolMetricsHistoryDTO) metrics);
            }
        }
        
        return result;
    }
    
    /**
     * 获取线程池的时间序列数据（用于图表展示）
     */
    public List<ThreadPoolMetricsHistoryDTO> getTimeSeriesData(String threadPoolName, LocalDateTime startTime, LocalDateTime endTime) {
        List<ThreadPoolMetricsHistoryDTO> result = new ArrayList<>();
        String timeSeriesKey = METRICS_TIMESERIES_KEY_PREFIX + threadPoolName;
        
        // 获取时间范围内的索引
        String indexKey = timeSeriesKey + ":index";
        long startScore = startTime.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli();
        long endScore = endTime.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli();
        
        Set<Object> timestampKeys = redisTemplate.opsForZSet().rangeByScore(indexKey, startScore, endScore);
        if (timestampKeys != null) {
            for (Object timestampKey : timestampKeys) {
                String hashKey = timeSeriesKey + ":" + timestampKey;
                
                // 从Hash中重建指标对象
                ThreadPoolRealTimeMetricsDTO realTimeMetrics = ThreadPoolRealTimeMetricsDTO.builder()
                        .activeCount((Integer) redisTemplate.opsForHash().get(hashKey, "activeCount"))
                        .queueSize((Integer) redisTemplate.opsForHash().get(hashKey, "queueSize"))
                        .currentPoolSize((Integer) redisTemplate.opsForHash().get(hashKey, "currentPoolSize"))
                        .activeThreadRate((Double) redisTemplate.opsForHash().get(hashKey, "activeThreadRate"))
                        .queueRemainingCapacity((Integer) redisTemplate.opsForHash().get(hashKey, "queueRemainingCapacity"))
                        .build();
                
                ThreadPoolAccumulateMetricsDTO accumulateMetrics = ThreadPoolAccumulateMetricsDTO.builder()
                        .rejectCount((Long) redisTemplate.opsForHash().get(hashKey, "rejectCount"))
                        .completedTaskCount((Long) redisTemplate.opsForHash().get(hashKey, "completedTaskCount"))
                        .build();
                
                String timestampStr = (String) redisTemplate.opsForHash().get(hashKey, "timestamp");
                LocalDateTime timestamp = LocalDateTime.parse(timestampStr);
                
                ThreadPoolMetricsHistoryDTO historyDTO = ThreadPoolMetricsHistoryDTO.builder()
                        .threadPoolName(threadPoolName)
                        .realTimeMetrics(realTimeMetrics)
                        .accumulateMetrics(accumulateMetrics)
                        .timestamp(timestamp)
                        .build();
                
                result.add(historyDTO);
            }
        }
        
        return result;
    }
    
    /**
     * 清理过期的指标数据
     */
    public void cleanupExpiredMetrics() {
        // Redis会自动清理过期的key，这里可以添加额外的清理逻辑
        log.info("执行Redis指标数据清理");
    }
}
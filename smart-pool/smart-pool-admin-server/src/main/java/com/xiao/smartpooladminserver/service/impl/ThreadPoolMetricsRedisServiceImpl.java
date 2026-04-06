package com.xiao.smartpooladminserver.service.impl;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.xiao.smartpooladminserver.model.dto.RejectTrendDTO;
import com.xiao.smartpooladminserver.model.dto.ThreadPoolMetricsHistoryDTO;
import com.xiao.smartpooladminserver.model.dto.ThreadPoolRealTimeMetricsDTO;
import com.xiao.smartpooladminserver.model.dto.ThreadPoolAccumulateMetricsDTO;
import com.xiao.smartpooladminserver.service.ThreadPoolMetricsRedisService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

@Slf4j
@Service
@RequiredArgsConstructor
public class ThreadPoolMetricsRedisServiceImpl implements ThreadPoolMetricsRedisService {

    private final RedisTemplate<String, Object> redisTemplate;
    private final ObjectMapper objectMapper;
    
    private static final String LATEST_METRICS_KEY_PREFIX = "threadpool:metrics:latest:";
    private static final String METRICS_HISTORY_5M_KEY_PREFIX = "threadpool:metrics:history:5m:";
    private static final String METRICS_HISTORY_15M_KEY_PREFIX = "threadpool:metrics:history:15m:";
    private static final String METRICS_HISTORY_1H_KEY_PREFIX = "threadpool:metrics:history:1h:";
    private static final String METRICS_HISTORY_4H_KEY_PREFIX = "threadpool:metrics:history:4h:";
    private static final String METRICS_HISTORY_1D_KEY_PREFIX = "threadpool:metrics:history:1d:";
    private static final String REJECT_COUNT_HISTORY_KEY_PREFIX = "threadpool:reject:history:";
    
    // 指标过期时间
    private static final long LATEST_METRICS_TTL_SECONDS = 7 * 24 * 60 * 60; // 7天
    private static final long HISTORY_5M_TTL_SECONDS = 10 * 60; // 10分钟
    private static final long HISTORY_15M_TTL_SECONDS = 30 * 60; // 30分钟
    private static final long HISTORY_1H_TTL_SECONDS = 2 * 60 * 60; // 2小时
    private static final long HISTORY_4H_TTL_SECONDS = 8 * 60 * 60; // 8小时
    private static final long HISTORY_1D_TTL_SECONDS = 2 * 24 * 60 * 60; // 2天
    
    // 拒绝任务历史记录过期时间（保存最近10分钟数据）
    private static final long REJECT_HISTORY_TTL_SECONDS = 10 * 60;
    
    // 数据聚合配置
    private static final int HISTORY_5M_MAX_SIZE = 12; // 5分钟数据保留12条（25秒一条）
    private static final int HISTORY_15M_MAX_SIZE = 18; // 15分钟数据保留18条（50秒一条）
    private static final int HISTORY_1H_MAX_SIZE = 12; // 1小时数据保留12条（5分钟一条）
    private static final int HISTORY_4H_MAX_SIZE = 24; // 4小时数据保留24条（10分钟一条）
    private static final int HISTORY_1D_MAX_SIZE = 24; // 1天数据保留24条（1小时一条）
    
    // 存储间隔配置（秒）
    private static final int INTERVAL_5M = 25;   // 5分钟粒度：25秒存一条
    private static final int INTERVAL_15M = 50;  // 15分钟粒度：50秒存一条
    private static final int INTERVAL_1H = 300;  // 1小时粒度：5分钟存一条
    private static final int INTERVAL_4H = 600;  // 4小时粒度：10分钟存一条
    private static final int INTERVAL_1D = 3600; // 1天粒度：1小时存一条
    
    // 记录每个线程池各粒度数据的上次存储时间
    private final Map<String, Map<String, Long>> lastStoreTimeMap = new ConcurrentHashMap<>();
    
    /**
     * 保存线程池指标到Redis（多时间粒度存储策略）
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
        
        try {
            // 序列化为JSON字符串
            String jsonData = objectMapper.writeValueAsString(metricsHistory);
            
            // 保存最新指标
            String latestKey = LATEST_METRICS_KEY_PREFIX + threadPoolName;
            redisTemplate.opsForValue().set(latestKey, jsonData, LATEST_METRICS_TTL_SECONDS, TimeUnit.SECONDS);
            
            // 多时间粒度存储
            saveMultiGranularityData(threadPoolName, jsonData, now);
            
            // 保存拒绝任务历史数据
            saveRejectCountHistory(threadPoolName, accumulateMetrics.getRejectCount(), now);
            
            log.debug("保存线程池 {} 指标到Redis", threadPoolName);
            
        } catch (JsonProcessingException e) {
            log.error("序列化线程池 {} 指标失败: {}", threadPoolName, e.getMessage(), e);
        }
    }
    
    /**
     * 多时间粒度数据存储（基于时间差值判断，不依赖精准系统时间）
     */
    private void saveMultiGranularityData(String threadPoolName, String jsonData, LocalDateTime timestamp) {
        long now = System.currentTimeMillis() / 1000; // 当前时间（秒）
        long timestampScore = timestamp.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli();
        
        // 初始化时间记录
        lastStoreTimeMap.putIfAbsent(threadPoolName, new ConcurrentHashMap<>());
        Map<String, Long> lastTime = lastStoreTimeMap.get(threadPoolName);
        
        // 5分钟粒度数据：（25s一条，保留12条覆盖1分钟）
        if(needStore(lastTime, "25s", now, INTERVAL_5M)){
            saveToRedis(METRICS_HISTORY_5M_KEY_PREFIX + threadPoolName, jsonData, timestampScore, HISTORY_5M_MAX_SIZE, HISTORY_5M_TTL_SECONDS);
        }
        
        // 15分钟粒度数据：50秒存一条
        if (needStore(lastTime, "15m", now, INTERVAL_15M)) {
            saveToRedis(METRICS_HISTORY_15M_KEY_PREFIX + threadPoolName, jsonData, timestampScore, HISTORY_15M_MAX_SIZE, HISTORY_15M_TTL_SECONDS);
            log.debug("保存15分钟粒度数据: {}", threadPoolName);
        }
        
        // 1小时粒度数据：5分钟存一条
        if (needStore(lastTime, "1h", now, INTERVAL_1H)) {
            saveToRedis(METRICS_HISTORY_1H_KEY_PREFIX + threadPoolName, jsonData, timestampScore, HISTORY_1H_MAX_SIZE, HISTORY_1H_TTL_SECONDS);
            log.debug("保存1小时粒度数据: {}", threadPoolName);
        }
        
        // 4小时粒度数据：10分钟存一条
        if (needStore(lastTime, "4h", now, INTERVAL_4H)) {
            saveToRedis(METRICS_HISTORY_4H_KEY_PREFIX + threadPoolName, jsonData, timestampScore, HISTORY_4H_MAX_SIZE, HISTORY_4H_TTL_SECONDS);
            log.debug("保存4小时粒度数据: {}", threadPoolName);
        }
        
        // 1天粒度数据：1小时存一条
        if (needStore(lastTime, "1d", now, INTERVAL_1D)) {
            saveToRedis(METRICS_HISTORY_1D_KEY_PREFIX + threadPoolName, jsonData, timestampScore, HISTORY_1D_MAX_SIZE, HISTORY_1D_TTL_SECONDS);
            log.debug("保存1天粒度数据: {}", threadPoolName);
        }
    }
    
    /**
     * 判断是否达到存储间隔（不依赖系统精准时间，永远有效）
     */
    private boolean needStore(Map<String, Long> lastTime, String type, long now, int interval) {
        long last = lastTime.getOrDefault(type, 0L);
        if (now - last >= interval) {
            lastTime.put(type, now);
            return true;
        }
        return false;
    }
    
    /**
     * 统一Redis存储（ZSet + 截取长度 + 过期时间）
     */
    private void saveToRedis(String key, String data, double score, int maxSize, long ttl) {
        redisTemplate.opsForZSet().add(key, data, score);
        redisTemplate.opsForZSet().removeRange(key, 0, -maxSize - 1);
        redisTemplate.expire(key, ttl, TimeUnit.SECONDS);
    }
    
    /**
     * 根据时间范围获取对应的Redis键前缀
     */
    private String getHistoryKeyPrefixByTimeRange(int timeRangeMinutes) {
        if (timeRangeMinutes <= 5) {
            return METRICS_HISTORY_5M_KEY_PREFIX;
        } else if (timeRangeMinutes <= 15) {
            return METRICS_HISTORY_15M_KEY_PREFIX;
        } else if (timeRangeMinutes <= 60) {
            return METRICS_HISTORY_1H_KEY_PREFIX;
        } else if (timeRangeMinutes <= 240) {
            return METRICS_HISTORY_4H_KEY_PREFIX;
        } else {
            return METRICS_HISTORY_1D_KEY_PREFIX;
        }
    }

    /**
     * 获取线程池的时间序列数据（支持多时间粒度）
     */
    public List<ThreadPoolMetricsHistoryDTO> getTimeSeriesData(String threadPoolName, LocalDateTime startTime, LocalDateTime endTime) {
        List<ThreadPoolMetricsHistoryDTO> result = new ArrayList<>();

        // 计算时间范围（分钟）
        long durationMinutes = Duration.between(startTime, endTime).toMinutes();
        int timeRangeMinutes = (int) Math.min(durationMinutes, 1440); // 最大1天

        // 根据时间范围选择对应的Redis键
        String keyPrefix = getHistoryKeyPrefixByTimeRange(timeRangeMinutes);
        String historyKey = keyPrefix + threadPoolName;

        // 获取时间范围内的所有数据
        long startScore = startTime.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli();
        long endScore = endTime.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli();

        Set<Object> jsonSet = redisTemplate.opsForZSet().rangeByScore(historyKey, startScore, endScore);
        if (jsonSet != null) {
            for (Object json : jsonSet) {
                if (json instanceof String) {
                    try {
                        ThreadPoolMetricsHistoryDTO metrics = objectMapper.readValue((String) json, ThreadPoolMetricsHistoryDTO.class);
                        result.add(metrics);
                    } catch (Exception e) {
                        log.error("反序列化线程池 {} 时间序列数据失败: {}", threadPoolName, e.getMessage(), e);
                    }
                }
            }
        }

        // 按时间戳排序
        result.sort((a, b) -> a.getTimestamp().compareTo(b.getTimestamp()));

        log.info("成功获取线程池 {} 时间序列数据，时间范围: {}分钟，数据条数: {}",
                threadPoolName, timeRangeMinutes, result.size());
        return result;
    }
    
    /**
     * 获取线程池的最新指标
     */
    public ThreadPoolMetricsHistoryDTO getLatestMetrics(String threadPoolName) {
        String key = LATEST_METRICS_KEY_PREFIX + threadPoolName;
        Object value = redisTemplate.opsForValue().get(key);
        
        if (value instanceof String) {
            try {
                return objectMapper.readValue((String) value, ThreadPoolMetricsHistoryDTO.class);
            } catch (Exception e) {
                log.error("反序列化线程池 {} 最新指标失败: {}", threadPoolName, e.getMessage(), e);
            }
        }
        
        return null;
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
                Object value = redisTemplate.opsForValue().get(key);
                
                if (value instanceof String) {
                    try {
                        ThreadPoolMetricsHistoryDTO metrics = objectMapper.readValue((String) value, ThreadPoolMetricsHistoryDTO.class);
                        result.add(metrics);
                    } catch (Exception e) {
                        log.error("反序列化线程池指标失败 (key: {}): {}", key, e.getMessage(), e);
                    }
                }
            }
        }
        
        return result;
    }

    /**
     * 获取最近一小时的时间序列数据
     */
    public List<ThreadPoolMetricsHistoryDTO> getRecentTimeSeriesData(String threadPoolName) {
        LocalDateTime endTime = LocalDateTime.now();
        LocalDateTime startTime = endTime.minusHours(1);
        return getTimeSeriesData(threadPoolName, startTime, endTime);
    }
    
    /**
     * 保存拒绝任务历史数据
     */
    private void saveRejectCountHistory(String threadPoolName, long rejectCount, LocalDateTime timestamp) {
        try {
            String key = REJECT_COUNT_HISTORY_KEY_PREFIX + threadPoolName;
            
            // 使用时间戳作为score，存储拒绝任务数
            double score = timestamp.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli();
            redisTemplate.opsForZSet().add(key, String.valueOf(rejectCount), score);
            
            // 设置过期时间
            redisTemplate.expire(key, REJECT_HISTORY_TTL_SECONDS, TimeUnit.SECONDS);
            
        } catch (Exception e) {
            log.error("保存拒绝任务历史数据失败: {}", e.getMessage(), e);
        }
    }
    
    /**
     * 获取拒绝任务趋势数据
     * @param threadPoolName 线程池名称
     * @param minutesAgo 多少分钟前的数据（默认5分钟）
     * @return 趋势数据对象，包含当前值、历史值、变化量、变化百分比
     */
    public RejectTrendDTO getRejectTrend(String threadPoolName, int minutesAgo) {
        String key = REJECT_COUNT_HISTORY_KEY_PREFIX + threadPoolName;
        
        // 获取当前时间
        long currentTime = System.currentTimeMillis();
        long historyTime = currentTime - (minutesAgo * 60 * 1000L);
        
        // 获取当前拒绝任务数（最新的数据）
        Set<Object> currentSet = redisTemplate.opsForZSet().reverseRange(key, 0, 0);
        long currentRejectCount = 0;
        if (currentSet != null && !currentSet.isEmpty()) {
            try {
                currentRejectCount = Long.parseLong((String) currentSet.iterator().next());
            } catch (Exception e) {
                log.error("解析当前拒绝任务数失败: {}", e.getMessage());
            }
        }
        
        // 获取历史拒绝任务数（minutesAgo分钟前的数据）
        Set<Object> historySet = redisTemplate.opsForZSet().rangeByScore(key, historyTime - 60000, historyTime + 60000);
        long historyRejectCount = 0;
        if (historySet != null && !historySet.isEmpty()) {
            try {
                // 取最接近历史时间的数据
                historyRejectCount = Long.parseLong((String) historySet.iterator().next());
            } catch (Exception e) {
                log.error("解析历史拒绝任务数失败: {}", e.getMessage());
            }
        }
        
        // 计算趋势
        long change = currentRejectCount - historyRejectCount;
        double changePercentage = 0.0;
        if (historyRejectCount > 0) {
            changePercentage = ((double) change / historyRejectCount) * 100;
        } else if (change > 0) {
            changePercentage = 100.0; // 历史为0，当前有值，增长100%
        }
        
        return RejectTrendDTO.builder()
                .currentValue(currentRejectCount)
                .historyValue(historyRejectCount)
                .change(change)
                .changePercentage(changePercentage)
                .build();
    }

}
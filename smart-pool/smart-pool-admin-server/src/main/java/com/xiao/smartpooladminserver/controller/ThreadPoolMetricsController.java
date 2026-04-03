package com.xiao.smartpooladminserver.controller;

import com.xiao.smartpooladminserver.common.result.Result;
import com.xiao.smartpooladminserver.model.dto.ThreadPoolMetricsHistoryDTO;
import com.xiao.smartpooladminserver.service.ThreadPoolMetricsRedisService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@RestController
@RequestMapping("/metrics")
@RequiredArgsConstructor
public class ThreadPoolMetricsController {

    private final ThreadPoolMetricsRedisService metricsRedisService;
    
    /**
     * 获取所有线程池的最新指标
     */
    @GetMapping("/latest")
    public Result<List<ThreadPoolMetricsHistoryDTO>> getAllLatestMetrics() {
        try {
            List<ThreadPoolMetricsHistoryDTO> metrics = metricsRedisService.getAllLatestMetrics();
            log.debug("获取所有线程池最新指标，共 {} 个", metrics.size());
            return Result.success(metrics);
        } catch (Exception e) {
            log.error("获取所有线程池最新指标失败: {}", e.getMessage(), e);
            return Result.failure("获取所有线程池最新指标失败: " + e.getMessage());
        }
    }
    
    /**
     * 获取指定线程池的最新指标
     */
    @GetMapping("/{threadPoolName}/latest")
    public Result<ThreadPoolMetricsHistoryDTO> getLatestMetrics(@PathVariable String threadPoolName) {
        try {
            ThreadPoolMetricsHistoryDTO metrics = metricsRedisService.getLatestMetrics(threadPoolName);
            if (metrics == null) {
                log.warn("未找到线程池 {} 的最新指标", threadPoolName);
                return Result.failure(404, "未找到线程池 " + threadPoolName + " 的最新指标");
            }
            log.debug("获取线程池 {} 最新指标", threadPoolName);
            return Result.success(metrics);
        } catch (Exception e) {
            log.error("获取线程池 {} 最新指标失败: {}", threadPoolName, e.getMessage(), e);
            return Result.failure("获取线程池 " + threadPoolName + " 最新指标失败: " + e.getMessage());
        }
    }
    
    /**
     * 获取指定线程池的历史指标
     */
    @GetMapping("/{threadPoolName}/history")
    public Result<List<ThreadPoolMetricsHistoryDTO>> getMetricsHistory(
            @PathVariable String threadPoolName,
            @RequestParam(defaultValue = "50") int limit) {
        try {
            List<ThreadPoolMetricsHistoryDTO> history = metricsRedisService.getMetricsHistory(threadPoolName, limit);
            log.debug("获取线程池 {} 历史指标，共 {} 条", threadPoolName, history.size());
            return Result.success(history);
        } catch (Exception e) {
            log.error("获取线程池 {} 历史指标失败: {}", threadPoolName, e.getMessage(), e);
            return Result.failure("获取线程池 " + threadPoolName + " 历史指标失败: " + e.getMessage());
        }
    }
    
    /**
     * 获取指定线程池的时间序列数据（用于图表展示）
     */
    @GetMapping("/{threadPoolName}/timeseries")
    public Result<List<ThreadPoolMetricsHistoryDTO>> getTimeSeriesData(
            @PathVariable String threadPoolName,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startTime,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endTime) {
        try {
            List<ThreadPoolMetricsHistoryDTO> timeSeriesData = 
                    metricsRedisService.getTimeSeriesData(threadPoolName, startTime, endTime);
            log.debug("获取线程池 {} 时间序列数据，从 {} 到 {}，共 {} 条", 
                    threadPoolName, startTime, endTime, timeSeriesData.size());
            return Result.success(timeSeriesData);
        } catch (Exception e) {
            log.error("获取线程池 {} 时间序列数据失败: {}", threadPoolName, e.getMessage(), e);
            return Result.failure("获取线程池 " + threadPoolName + " 时间序列数据失败: " + e.getMessage());
        }
    }
    
    /**
     * 获取最近一小时的时间序列数据（简化接口）
     */
    @GetMapping("/{threadPoolName}/timeseries/recent")
    public Result<List<ThreadPoolMetricsHistoryDTO>> getRecentTimeSeriesData(
            @PathVariable String threadPoolName) {
        try {
            LocalDateTime endTime = LocalDateTime.now();
            LocalDateTime startTime = endTime.minusHours(1);
            
            List<ThreadPoolMetricsHistoryDTO> timeSeriesData = 
                    metricsRedisService.getTimeSeriesData(threadPoolName, startTime, endTime);
            log.debug("获取线程池 {} 最近一小时时间序列数据，共 {} 条", threadPoolName, timeSeriesData.size());
            return Result.success(timeSeriesData);
        } catch (Exception e) {
            log.error("获取线程池 {} 最近一小时时间序列数据失败: {}", threadPoolName, e.getMessage(), e);
            return Result.failure("获取线程池 " + threadPoolName + " 最近一小时时间序列数据失败: " + e.getMessage());
        }
    }
}
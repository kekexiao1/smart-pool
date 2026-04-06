package com.xiao.smartpooladminserver.service;

import com.xiao.smartpooladminserver.model.dto.ThreadPoolAccumulateMetricsDTO;
import com.xiao.smartpooladminserver.model.dto.ThreadPoolMetricsHistoryDTO;
import com.xiao.smartpooladminserver.model.dto.ThreadPoolRealTimeMetricsDTO;

import java.util.List;
import java.util.Map;

public interface ThreadPoolMetricsRedisService {

    /**
     * 保存线程池指标到Redis
     * @param poolName 线程池名称
     * @param realTimeMetrics 实时指标
     * @param accumulateMetrics 累计指标
     */
    void saveMetrics(String poolName, ThreadPoolRealTimeMetricsDTO realTimeMetrics, ThreadPoolAccumulateMetricsDTO accumulateMetrics);
    
    /**
     * 获取指定线程池的最新指标
     * @param poolName 线程池名称
     * @return 实时指标数据
     */
	ThreadPoolMetricsHistoryDTO getLatestMetrics(String poolName);
    
    /**
     * 获取所有线程池的最新指标
     * @return 线程池名称到实时指标的映射
     */
	List<ThreadPoolMetricsHistoryDTO> getAllLatestMetrics();
    

}

package com.xiao.smartpooladminserver.service;

public interface ThreadPoolMetricsCollectorService {
    
    /**
     * 定时采集线程池指标并保存到Redis
     */
    void collectAndStoreMetrics();
    
    /**
     * 手动触发指标采集
     */
    void triggerManualCollection();
    
//    /**
//     * 获取指标采集状态
//     * @return 采集状态信息
//     */
//    String getCollectionStatus();
}

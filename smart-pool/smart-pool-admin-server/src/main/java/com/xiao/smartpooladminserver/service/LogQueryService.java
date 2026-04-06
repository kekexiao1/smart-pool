package com.xiao.smartpooladminserver.service;

import com.github.pagehelper.PageInfo;
import com.xiao.smartpooladminserver.model.dto.PoolAlertLogQueryDTO;
import com.xiao.smartpooladminserver.model.dto.PoolConfigLogQueryDTO;
import com.xiao.smartpoolcore.model.entity.PoolAlertLog;
import com.xiao.smartpoolcore.model.entity.PoolConfigLog;

public interface LogQueryService {
    
    /**
     * 查询配置变更日志
     */
    PageInfo<PoolConfigLog> queryConfigLogs(PoolConfigLogQueryDTO queryDTO);
    
    /**
     * 查询告警日志
     */
    PageInfo<PoolAlertLog> queryAlertLogs(PoolAlertLogQueryDTO queryDTO);
    
    /**
     * 标记告警为已处理
     */
    void handleAlert(Long id, String handler);
    
//    /**
//     * 获取未处理的告警数量
//     * @return 未处理告警数量
//     */
//    long getUnhandledAlertCount();
//
//    /**
//     * 获取告警统计信息
//     * @return 告警统计
//     */
//    Map<String, Object> getAlertStatistics();
//
//    /**
//     * 清理过期日志
//     * @param daysBefore 保留天数
//     * @return 清理结果
//     */
//    Map<String, Object> cleanupExpiredLogs(int daysBefore);
//
//    /**
//     * 导出配置变更日志
//     * @param queryDTO 查询条件
//     * @return 日志数据
//     */
//    List<PoolConfigLog> exportConfigLogs(PoolConfigLogQueryDTO queryDTO);
//
//    /**
//     * 导出告警日志
//     * @param queryDTO 查询条件
//     * @return 日志数据
//     */
//    List<PoolAlertLog> exportAlertLogs(PoolAlertLogQueryDTO queryDTO);
}
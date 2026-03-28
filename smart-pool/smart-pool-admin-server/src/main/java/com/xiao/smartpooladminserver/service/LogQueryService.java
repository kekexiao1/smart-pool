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
}
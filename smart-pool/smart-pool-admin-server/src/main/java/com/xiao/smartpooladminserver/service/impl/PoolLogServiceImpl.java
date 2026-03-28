package com.xiao.smartpooladminserver.service.impl;

import com.xiao.smartpooladminserver.mapper.PoolLogMapper;
import com.xiao.smartpoolcore.callback.PoolLogService;
import com.xiao.smartpoolcore.model.dto.PoolConfigLogDTO;
import com.xiao.smartpoolcore.model.entity.PoolConfigLog;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Slf4j
@RequiredArgsConstructor
@Service
public class PoolLogServiceImpl implements PoolLogService {

    private final PoolLogMapper poolLogMapper;

    @Override
    public void logConfigChange(PoolConfigLog poolConfigLog) {
        try {
            poolLogMapper.save(poolConfigLog);
            log.info("配置变更日志记录成功: appName={}, poolName={}, changeType={}", 
                    poolConfigLog.getAppName(), poolConfigLog.getPoolName(), poolConfigLog.getChangeType());
        } catch (Exception e) {
            log.error("记录配置变更日志失败: appName={}, poolName={}", 
                    poolConfigLog.getAppName(), poolConfigLog.getPoolName(), e);
        }
    }

    @Override
    public void logConfigInit(PoolConfigLogDTO poolConfigLogDTO) {
        PoolConfigLog logEntity = new PoolConfigLog();
        logEntity.setAppName(poolConfigLogDTO.getAppName());
        logEntity.setPoolName(poolConfigLogDTO.getPoolName());
        logEntity.setNewConfig(poolConfigLogDTO.getNewConfig());
        logEntity.setChangeType(poolConfigLogDTO.getChangeType());
        logEntity.setOperator(poolConfigLogDTO.getOperator());
        logEntity.setCreateTime(LocalDateTime.now());
        logEntity.setSource(poolConfigLogDTO.getSource());
        logConfigChange(logEntity);
    }

    @Override
    public void logConfigUpdate(PoolConfigLogDTO poolConfigLogDTO) {
        PoolConfigLog logEntity = new PoolConfigLog();
        logEntity.setAppName(poolConfigLogDTO.getAppName());
        logEntity.setPoolName(poolConfigLogDTO.getPoolName());
        logEntity.setOldConfig(poolConfigLogDTO.getOldConfig());
        logEntity.setNewConfig(poolConfigLogDTO.getNewConfig());
        logEntity.setChangeType(poolConfigLogDTO.getChangeType());
        logEntity.setOperator(poolConfigLogDTO.getOperator());
        logEntity.setCreateTime(LocalDateTime.now());
        logEntity.setSource(poolConfigLogDTO.getSource());
        logConfigChange(logEntity);
    }
}

package com.xiao.smartpooladminserver.service.impl;

import com.xiao.smartpooladminserver.mapper.PoolAlertLogMapper;
import com.xiao.smartpoolalert.callback.PoolAlertLogService;
import com.xiao.smartpoolcore.model.entity.PoolAlertLog;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Date;

@Slf4j
@RequiredArgsConstructor
@Service
public class PoolAlertLogServiceImpl implements PoolAlertLogService {

    private final PoolAlertLogMapper poolAlertLogMapper;

    @Override
    public void logAlert(PoolAlertLog poolAlertLog) {
        try {
            poolAlertLog.setCreateTime(LocalDateTime.now());
            poolAlertLog.setStatus(0); // 默认未处理状态
            poolAlertLogMapper.save(poolAlertLog);
            log.info("告警日志记录成功: appName={}, poolName={}, alertType={}", 
                    poolAlertLog.getAppName(), poolAlertLog.getPoolName(), poolAlertLog.getAlertType());
        } catch (Exception e) {
            log.error("记录告警日志失败: appName={}, poolName={}", 
                    poolAlertLog.getAppName(), poolAlertLog.getPoolName(), e);
        }
    }

    @Override
    public void logAlertHandle(PoolAlertLog poolAlertLog) {
        try {
            poolAlertLog.setHandleTime(LocalDateTime.now());
            poolAlertLog.setStatus(1); // 已处理状态
            poolAlertLogMapper.update(poolAlertLog);
            log.info("告警处理日志记录成功: id={}, handler={}", 
                    poolAlertLog.getId(), poolAlertLog.getHandler());
        } catch (Exception e) {
            log.error("记录告警处理日志失败: id={}", poolAlertLog.getId(), e);
        }
    }
}
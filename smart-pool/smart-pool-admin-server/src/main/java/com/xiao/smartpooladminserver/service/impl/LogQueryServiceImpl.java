package com.xiao.smartpooladminserver.service.impl;

import com.github.pagehelper.PageHelper;
import com.github.pagehelper.PageInfo;
import com.xiao.smartpooladminserver.mapper.PoolAlertLogMapper;
import com.xiao.smartpooladminserver.mapper.PoolLogMapper;
import com.xiao.smartpooladminserver.model.dto.PoolAlertLogQueryDTO;
import com.xiao.smartpooladminserver.model.dto.PoolConfigLogQueryDTO;
import com.xiao.smartpooladminserver.service.LogQueryService;
import com.xiao.smartpoolcore.model.entity.PoolAlertLog;
import com.xiao.smartpoolcore.model.entity.PoolConfigLog;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@RequiredArgsConstructor
@Service
public class LogQueryServiceImpl implements LogQueryService {

    private final PoolLogMapper poolLogMapper;
    private final PoolAlertLogMapper poolAlertLogMapper;

    @Override
    public PageInfo<PoolConfigLog> queryConfigLogs(PoolConfigLogQueryDTO queryDTO) {
        try {
            // 开启分页
            PageHelper.startPage(queryDTO.getPageNum(), queryDTO.getPageSize());
            
            List<PoolConfigLog> poolConfigLogVOS = poolLogMapper.selectLogsList(queryDTO);
            return new PageInfo<>(poolConfigLogVOS);
        } catch (Exception e) {
            log.error("查询配置变更日志失败", e);
            throw new RuntimeException("查询配置变更日志失败", e);
        }
    }

    @Override
    public PageInfo<PoolAlertLog> queryAlertLogs(PoolAlertLogQueryDTO queryDTO) {
        try {
            PageHelper.startPage(queryDTO.getPageNum(), queryDTO.getPageSize());

            List<PoolAlertLog> list = poolAlertLogMapper.selectAlertsList(queryDTO);

            return new PageInfo<>(list);
        } catch (Exception e) {
            log.error("查询告警日志失败", e);
            throw new RuntimeException("查询告警日志失败", e);
        }
    }

    @Override
    public void handleAlert(Long id, String handler) {
        try {
            PoolAlertLog alertLog = new PoolAlertLog();
            alertLog.setId(id);
            alertLog.setHandler(handler);
            alertLog.setStatus(1);
            alertLog.setHandleTime(LocalDateTime.now());
            
            poolAlertLogMapper.update(alertLog);
            log.info("告警处理成功: id={}, handler={}", id, handler);
        } catch (Exception e) {
            log.error("处理告警失败: id={}", id, e);
            throw new RuntimeException("处理告警失败", e);
        }
    }
}
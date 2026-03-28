package com.xiao.smartpooladminserver.controller;

import com.github.pagehelper.PageInfo;
import com.xiao.smartpooladminserver.common.result.Result;
import com.xiao.smartpooladminserver.model.dto.PoolAlertLogQueryDTO;
import com.xiao.smartpooladminserver.model.dto.PoolConfigLogQueryDTO;
import com.xiao.smartpooladminserver.service.LogQueryService;
import com.xiao.smartpoolcore.model.entity.PoolAlertLog;
import com.xiao.smartpoolcore.model.entity.PoolConfigLog;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/log")
@Slf4j
@RequiredArgsConstructor
public class LogQueryController {

    private final LogQueryService logQueryService;

    /**
     * 查询配置变更日志
     */
    @GetMapping("/config")
    public Result<PageInfo<PoolConfigLog>> queryConfigLogs(PoolConfigLogQueryDTO queryDTO) {
        try {
            PageInfo<PoolConfigLog> logs = logQueryService.queryConfigLogs(queryDTO);
            return Result.success(logs);
        } catch (Exception e) {
            log.error("查询配置变更日志失败", e);
            return Result.failure("查询配置变更日志失败: " + e.getMessage());
        }
    }

    /**
     * 查询告警日志
     */
    @GetMapping("/alert")
    public Result<PageInfo<PoolAlertLog>> queryAlertLogs(PoolAlertLogQueryDTO queryDTO) {
        try {
            PageInfo<PoolAlertLog> logs = logQueryService.queryAlertLogs(queryDTO);
            return Result.success(logs);
        } catch (Exception e) {
            log.error("查询告警日志失败", e);
            return Result.failure("查询告警日志失败: " + e.getMessage());
        }
    }

    /**
     * 标记告警为已处理
     */
    @PutMapping("/alert/{id}/handle")
    public Result<Void> handleAlert(@PathVariable("id") Long id, @RequestParam String handler) {
        try {
            logQueryService.handleAlert(id, handler);
            return Result.success();
        } catch (Exception e) {
            log.error("处理告警失败: id={}", id, e);
            return Result.failure("处理告警失败: " + e.getMessage());
        }
    }
}
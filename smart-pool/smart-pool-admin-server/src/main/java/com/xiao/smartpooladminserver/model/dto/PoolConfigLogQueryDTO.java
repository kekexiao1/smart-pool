package com.xiao.smartpooladminserver.model.dto;

import lombok.Data;
import java.time.LocalDateTime;

@Data
public class PoolConfigLogQueryDTO {
    
    /**
     * 应用名称
     */
    private String appName;
    
    /**
     * 线程池名称
     */
    private String poolName;
    
    /**
     * 变更类型
     */
    private String changeType;
    
    /**
     * 操作人
     */
    private String operator;
    
    /**
     * 开始时间
     */
    private LocalDateTime startTime;
    
    /**
     * 结束时间
     */
    private LocalDateTime endTime;
    
    /**
     * 页码
     */
    private Integer pageNum = 1;
    
    /**
     * 每页大小
     */
    private Integer pageSize = 20;
}
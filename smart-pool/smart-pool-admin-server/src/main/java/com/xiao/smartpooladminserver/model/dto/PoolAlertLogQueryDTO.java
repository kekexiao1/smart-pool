package com.xiao.smartpooladminserver.model.dto;

import lombok.Data;
import java.time.LocalDateTime;

@Data
public class PoolAlertLogQueryDTO {
    
    /**
     * 应用名称
     */
    private String appName;
    
    /**
     * 线程池名称
     */
    private String poolName;
    
    /**
     * 告警类型
     */
    private String alertType;
    
    /**
     * 告警级别
     */
    private String alertLevel;
    
    /**
     * 告警状态：0-未处理，1-已处理
     */
    private Integer status;
    
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
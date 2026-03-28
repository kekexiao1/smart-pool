package com.xiao.smartpooladminserver.model.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import javax.validation.constraints.Min;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class ThreadPoolConfigTemplateDTO {
    
    @NotBlank(message = "模板名称不能为空")
    private String templateName;
    
    private String description;
    
    @NotNull(message = "核心线程数不能为空")
    @Min(value = 1, message = "核心线程数必须大于0")
    private Integer corePoolSize;
    
    @NotNull(message = "最大线程数不能为空")
    @Min(value = 1, message = "最大线程数必须大于0")
    private Integer maximumPoolSize;
    
    @NotNull(message = "存活时间不能为空")
    @Min(value = 1, message = "存活时间必须大于0")
    private Long keepAliveTime;
    
    @NotNull(message = "队列容量不能为空")
    @Min(value = 1, message = "队列容量必须大于0")
    private Integer queueCapacity;
    
    @NotBlank(message = "拒绝策略不能为空")
    private String rejectedHandler;
    
    private Integer queueWarnThreshold;
    
    private Integer activeThreadRateThreshold;
}
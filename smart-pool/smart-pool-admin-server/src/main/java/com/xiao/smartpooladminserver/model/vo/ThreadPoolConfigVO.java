package com.xiao.smartpooladminserver.model.vo;

import com.xiao.smartpoolcore.model.dto.ThreadPoolConfig;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class ThreadPoolConfigVO {
    
    private String applicationName;
    
    private String environment;
    
    private String threadPoolName;
    
    private ThreadPoolConfig config;
    
    private String rejectedHandler;
    
    private String createTime;
    
    private String updateTime;
}
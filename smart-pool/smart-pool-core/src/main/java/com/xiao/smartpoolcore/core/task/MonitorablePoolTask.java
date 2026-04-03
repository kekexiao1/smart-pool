package com.xiao.smartpoolcore.core.task;

import com.xiao.smartpoolcore.core.manager.TaskExecutionCallbackManager;
import lombok.extern.slf4j.Slf4j;

/**
 * 可监控的线程池任务
 * 继承自PoolTask，集成执行时间和等待时间监控功能
 */
@Slf4j
public class MonitorablePoolTask extends PoolTask {
    
    private final long submitTimeNanos;
    
    public MonitorablePoolTask(Runnable realTask, String taskId, String taskType, String businessType, String payload,
                               String originalPoolName, long submitTimeNanos) {
        super(realTask, taskId, taskType, businessType, payload);
        this.submitTimeNanos = submitTimeNanos;
        
        // 设置原始线程池名称
        if (originalPoolName != null) {
            this.setOriginalPoolName(originalPoolName);
        }
    }
    
    @Override
    public void run() {
        long startExecuteNanos = System.nanoTime();
        long waitTimeNanos = startExecuteNanos - submitTimeNanos;
        
        // 记录等待时间
        String poolName = getOriginalPoolName() != null ? getOriginalPoolName() : "unknown-pool";
        TaskExecutionCallbackManager.recordWaitTime(poolName, waitTimeNanos);
        
        try {
            // 执行实际任务
            super.run();
            
        } catch (Exception e) {
            throw new RuntimeException("任务执行异常", e);
        } finally {
            long endExecuteNanos = System.nanoTime();
            long executeTimeNanos = endExecuteNanos - startExecuteNanos;

            // 记录执行时间
            TaskExecutionCallbackManager.recordExecuteTime(poolName, executeTimeNanos);
        }
    }
}
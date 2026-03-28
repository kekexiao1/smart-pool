package com.xiao.smartpoolcore.callback;

/**
 * 任务执行监控回调接口
 * 用于在不产生循环依赖的情况下监控任务执行时间
 */
public interface TaskExecutionCallback {
    
    /**
     * 记录任务执行时间
     * @param threadPoolName 线程池名称
     * @param executeTimeNanos 执行时间（纳秒）
     */
    void recordExecuteTime(String threadPoolName, long executeTimeNanos);
    
    /**
     * 记录任务等待时间
     * @param threadPoolName 线程池名称
     * @param waitTimeNanos 等待时间（纳秒）
     */
    void recordWaitTime(String threadPoolName, long waitTimeNanos);


}
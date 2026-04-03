package com.xiao.smartpoolcore.reject.local;

import com.xiao.smartpoolcore.common.util.LocalDiskHelper;
import com.xiao.smartpoolcore.reject.AbstractRejectPolicy;
import lombok.extern.slf4j.Slf4j;

import java.util.concurrent.ThreadPoolExecutor;

/**
 * 本地磁盘拒绝策略
 * 直接将拒绝的任务保存到本地磁盘文件
 */
@Slf4j
public class LocalDiskRejectPolicy extends AbstractRejectPolicy {

    @Override
    protected void doReject(Runnable task, ThreadPoolExecutor executor) {
        String threadPoolName = getThreadPoolName(executor);
        
        try {
            // 直接使用本地磁盘持久化
            LocalDiskHelper.saveTask(task, threadPoolName);
            log.info("任务成功持久化到本地磁盘，线程池: {}", threadPoolName);
            
        } catch (Exception e) {
            log.error("本地磁盘持久化失败，线程池: {}, 错误: {}", threadPoolName, e.getMessage());
            
            // 最终兜底：记录到日志文件
            logFinalFallback(task, executor);
        }
    }

    /**
     * 最终兜底：记录到日志文件
     */
    private void logFinalFallback(Runnable task, ThreadPoolExecutor executor) {
        String threadPoolName = getThreadPoolName(executor);
        String taskInfo = getTaskInfo(task);
        
        log.error("任务持久化完全失败，任务丢失！线程池: {}, 任务信息: {}, 时间: {}", 
            threadPoolName, taskInfo, System.currentTimeMillis());
    }
}
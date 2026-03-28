package com.xiao.smartpoolcore.core.manager;

import com.xiao.smartpoolcore.callback.TaskExecutionCallback;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * 任务执行回调管理器
 * 用于管理多个监控回调
 */
public class TaskExecutionCallbackManager {
    
    private static final List<TaskExecutionCallback> callbacks = new CopyOnWriteArrayList<>();
    
    /**
     * 注册监控回调
     * @param callback 回调接口
     */
    public static void registerCallback(TaskExecutionCallback callback) {
        if (callback != null) {
            callbacks.add(callback);
        }
    }
    
    /**
     * 注销监控回调
     * @param callback 回调接口
     */
    public static void unregisterCallback(TaskExecutionCallback callback) {
        callbacks.remove(callback);
    }
    
    /**
     * 记录执行时间
     * @param threadPoolName 线程池名称
     * @param executeTimeNanos 执行时间（纳秒）
     */
    public static void recordExecuteTime(String threadPoolName, long executeTimeNanos) {
        for (TaskExecutionCallback callback : callbacks) {
            try {
                callback.recordExecuteTime(threadPoolName, executeTimeNanos);
            } catch (Exception e) {
                // 忽略单个回调的异常，不影响其他回调
            }
        }
    }
    
    /**
     * 记录等待时间
     * @param threadPoolName 线程池名称
     * @param waitTimeNanos 等待时间（纳秒）
     */
    public static void recordWaitTime(String threadPoolName, long waitTimeNanos) {
        for (TaskExecutionCallback callback : callbacks) {
            try {
                callback.recordWaitTime(threadPoolName, waitTimeNanos);
            } catch (Exception e) {
                // 忽略单个回调的异常，不影响其他回调
            }
        }
    }
    
    /**
     * 清空所有回调
     */
    public static void clearCallbacks() {
        callbacks.clear();
    }
    
    /**
     * 获取回调数量
     * @return 回调数量
     */
    public static int getCallbackCount() {
        return callbacks.size();
    }
}
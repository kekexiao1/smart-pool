package com.xiao.smartpoolmetrics.callback;

import com.xiao.smartpoolcore.callback.TaskExecutionCallback;
import com.xiao.smartpoolmetrics.monitor.ThreadPoolMonitor;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import io.micrometer.core.instrument.Timer;

import java.util.concurrent.TimeUnit;

/**
 * 指标监控回调实现
 * 将任务执行时间和等待时间记录到Micrometer指标中
 */
@Slf4j
@AllArgsConstructor
public class MetricsTaskExecutionCallback implements TaskExecutionCallback {

    @Override
    public void recordExecuteTime(String threadPoolName, long executeTimeNanos) {
        try {
            Timer executeTimer = ThreadPoolMonitor.EXECUTE_TIMER_CACHE.get(threadPoolName);
            if (executeTimer != null) {
                executeTimer.record(executeTimeNanos, TimeUnit.NANOSECONDS);
            } else {
                log.warn("线程池 [{}] 的执行时间指标未注册", threadPoolName);
            }
        } catch (Exception e) {
            log.error("记录执行时间指标失败 - 线程池: {}, 错误: {}", threadPoolName, e.getMessage());
        }
    }
    
    @Override
    public void recordWaitTime(String threadPoolName, long waitTimeNanos) {
        try {
            Timer waitTimer = ThreadPoolMonitor.WAIT_TIMER_CACHE.get(threadPoolName);
            if (waitTimer != null) {
                waitTimer.record(waitTimeNanos, TimeUnit.NANOSECONDS);
            } else {
                log.warn("线程池 [{}] 的等待时间指标未注册", threadPoolName);
            }
        } catch (Exception e) {
            log.error("记录等待时间指标失败 - 线程池: {}, 错误: {}", threadPoolName, e.getMessage());
        }
    }

}
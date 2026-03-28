package com.xiao.smartpoolalert.monitor.engine;

import com.xiao.smartpoolalert.constant.AlertType;
import com.xiao.smartpoolalert.context.AlertContext;
import com.xiao.smartpoolalert.manager.AlertManager;
import com.xiao.smartpoolmetrics.metrics.ThreadPoolMetricCollector;
import com.xiao.smartpoolmetrics.metrics.ThreadPoolMetrics;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import java.util.Map;
import java.util.Set;

/**
 * 告警引擎
 * 负责定时收集线程池指标并触发告警检查
 */
@RequiredArgsConstructor
@Slf4j
public class AlertEngine {
    
    private final AlertManager alertManager;
    private final ThreadPoolMetricCollector metricCollector;
    

    /**
     * 检查所有线程池的指标并触发告警
     */
    public void checkAllThreadPools() {
        Set<String> poolNames = metricCollector.getAllPoolNames();
        
        if (poolNames.isEmpty()) {
            log.debug("当前没有注册的线程池");
            return;
        }
        
        for (String poolName : poolNames) {
            try {
                checkSingleThreadPool(poolName);
            } catch (Exception e) {
                log.error("检查线程池 {} 指标异常: {}", poolName, e.getMessage());
            }
        }
    }
    
    /**
	 * 检查单个线程池的指标
	 */
	public void checkSingleThreadPool(String poolName) {
		// 从配置中获取超时阈值（秒），如果未配置则使用默认值10秒
		int runTimeoutThreshold = alertManager.getAlertThreshold(poolName, "RUN_TIMEOUT");
		long runTimeoutThresholdSeconds = runTimeoutThreshold > 0 ? runTimeoutThreshold : 10;
		
		ThreadPoolMetrics metrics = metricCollector.currentMetrics(poolName);
        if (metrics == null) {
            log.warn("线程池 {} 指标采集失败", poolName);
            return;
        }
        
        // 检查队列容量告警
        checkQueueCapacityAlert(poolName, metrics);
        
        // 检查活跃度告警
        checkLivenessAlert(poolName, metrics);

        // 检查执行超时告警
        checkRunTimeOut(poolName, metrics);

        // 检查拒绝任务告警
        checkRejectAlert(poolName, metrics);

    }
    
    /**
     * 检查队列容量告警
     */
    private void checkQueueCapacityAlert(String poolName, ThreadPoolMetrics metrics) {
        if (metrics.getQueueCapacity() > 0) {
            double queueUsageRate = (double) metrics.getQueueSize() / metrics.getQueueCapacity() * 100;
            alertManager.checkAndTriggerAlert(poolName, AlertType.CAPACITY.name(), queueUsageRate);
            log.debug("线程池 {} 队列使用率: {:.2f}%", poolName, queueUsageRate);
        }
    }
    
    /**
     * 检查活跃度告警
     */
    private void checkLivenessAlert(String poolName, ThreadPoolMetrics metrics) {
        if (metrics.getMaximumPoolSize() > 0) {
            double livenessRate = (double) metrics.getActiveCount() / metrics.getMaximumPoolSize() * 100;
            alertManager.checkAndTriggerAlert(poolName, AlertType.LIVENESS.name(), livenessRate);
            if (log.isDebugEnabled()) {
                log.debug("线程池 {} 活跃度: {:.2f}%", poolName, livenessRate);
            }
        }
    }

    /**
     * 检查执行超时告警
     */
    private void checkRunTimeOut(String poolName, ThreadPoolMetrics metrics){
        String runTimeoutName = AlertType.RUN_TIMEOUT.name();
        
        // 获取最大执行时间（秒）
        double maxExecutionTimeSeconds = metrics.getMaxExecutionTime() / 1000.0;
        
        // 检查最大执行时间是否超过阈值
        int timeoutThreshold = alertManager.getAlertThreshold(poolName, runTimeoutName);

        
        if (timeoutThreshold > 0 && maxExecutionTimeSeconds > timeoutThreshold) {
            // 有超时任务，触发告警
            Map<String, AlertContext> alertContexts = alertManager.getAlertContexts(poolName);
            AlertContext alertContext = alertContexts.get(runTimeoutName);
            // 检查告警上下文是否存在
            if (alertContext == null) {
                log.warn("线程池 {} 的执行超时告警配置未初始化", poolName);
                return;
            }
            // 在静默期
            if(alertManager.isInSilencePeriod(alertContext)){
                return;
            }
            alertManager.triggerAlert(alertContext, maxExecutionTimeSeconds);

        } else {
            log.debug("线程池 {} 执行超时告警检查 - 最大执行时间: {:.2f}s <= 阈值: {}s", 
                    poolName, maxExecutionTimeSeconds, timeoutThreshold);
        }
    }
    
    /**
     * 检查拒绝任务告警
     */
    private void checkRejectAlert(String poolName, ThreadPoolMetrics metrics) {
        if (metrics.isRejectedTask()) {
            Map<String, AlertContext> alertContexts = alertManager.getAlertContexts(poolName);
            AlertContext alertContext = alertContexts.get(AlertType.REJECT.name());
            // 检查告警上下文是否存在
            if (alertContext == null) {
                log.warn("线程池 {} 的拒绝任务告警配置未初始化", poolName);
                return;
            }
            // 在静默期
            if(alertManager.isInSilencePeriod(alertContext)){
                return;
            }
            alertManager.triggerAlert(alertContext, 1);
            log.debug("线程池 {} 拒绝任务告警触发", poolName);
        }else{
            log.debug("线程池 {} 拒绝告警检查 -未达到告警条件", poolName);
        }
    }
    
    /**
     * 获取当前监控的线程池数量
     */
    public int getMonitoredPoolCount() {
        return metricCollector.getAllPoolNames().size();
    }
}
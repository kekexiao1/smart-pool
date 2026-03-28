package com.xiao.smartpoolalert.manager;

import com.xiao.smartpoolalert.callback.PoolAlertLogService;
import com.xiao.smartpoolalert.context.AlertContext;
import com.xiao.smartpoolalert.context.TimestampedValue;
import com.xiao.smartpoolalert.notification.AlertNotifier;
import com.xiao.smartpoolalert.rule.AlertRule;
import com.xiao.smartpoolcore.model.entity.PoolAlertLog;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * 告警管理器
 * 负责告警规则的检查、触发和通知发送
 * 支持线程池级别的规则管理和滑动窗口统计
 */
@Slf4j
@RequiredArgsConstructor
public class AlertManager {
    
    private final PoolAlertLogService poolAlertLogService;
    
    private final List<AlertNotifier> notifiers = new ArrayList<>();
    private final Map<String, Map<String, AlertContext>> poolAlertContexts = new ConcurrentHashMap<>();
    
    // 通知器与告警类型的映射关系
    private final Map<String, Set<String>> notifierAlertTypeMapping = new ConcurrentHashMap<>();

    /**
     * 注册告警通知器，并指定处理的告警类型
     * @param notifier 通知器
     * @param alertTypes 处理的告警类型列表，null表示处理所有类型
     */
    public void registerNotifier(AlertNotifier notifier, List<String> alertTypes) {
        notifiers.add(notifier);
        
        if (alertTypes != null && !alertTypes.isEmpty()) {
            notifierAlertTypeMapping.put(notifier.getType(), new HashSet<>(alertTypes));
            log.info("注册告警通知器: {}，处理告警类型: {}", notifier.getType(), alertTypes);
        } else {
            log.info("注册告警通知器: {}，处理所有告警类型", notifier.getType());
        }
    }
    
    /**
     * 为线程池注册告警规则
     * @param appName 应用名称
     * @param poolName 线程池名称
     * @param rules 告警规则列表
     */
    public void registerAlertRules(String appName, String poolName, List<AlertRule> rules , List<AlertRule> defaultRules) {
        Map<String, AlertContext> poolContexts = poolAlertContexts.computeIfAbsent(poolName, k -> new ConcurrentHashMap<>());

        List<AlertRule> alertRules;
        if(rules!=null){
            alertRules=rules;
        }else{
            alertRules=defaultRules;
        }
        for (AlertRule alertRule : alertRules) {
            if(!alertRule.isEnabled()){
                continue;
            }
            AlertContext context = new AlertContext();
            context.setAppName(appName);
            context.setPoolName(poolName);
            context.setRule(alertRule);
            context.setRecentValues(new LinkedList<>());
            poolContexts.put(alertRule.getType().name(), context);
            log.info("注册告警规则 - 应用: {}, 线程池: {}, 类型: {}, 阈值: {}, count: {}, period: {},  silencePeriod{}",
                    appName, poolName, alertRule.getType(), alertRule.getThreshold(), alertRule.getCount(),
                    alertRule.getPeriod(), alertRule.getSilencePeriod());
        }
    }
    
    /**
     * 检查并触发告警
     * @param poolName 线程池名称
     * @param alertType 告警类型
     * @param currentValue 当前值
     */
    public void checkAndTriggerAlert(String poolName, String alertType, double currentValue) {
        Map<String, AlertContext> poolContexts = poolAlertContexts.get(poolName);
        if (poolContexts == null) {
            // 如果该线程池的告警规则未初始化，使用默认规则
            poolContexts = poolAlertContexts.get("default");
            if (poolContexts == null) {
                log.debug("线程池 {} 的告警规则未初始化，且无默认规则", poolName);
                return;
            }
        }
        
        AlertContext context = poolContexts.get(alertType);
        if (context == null || !context.getRule().isEnabled()) {
            return;
        }
        
        // 检查是否在静默期内
        if (isInSilencePeriod(context)) {
            return;
        }
        
        // 更新滑动窗口数据
        updateSlidingWindow(context, currentValue);
        
        // 检查是否达到阈值条件
        if (checkThresholdCondition(context)) {
            triggerAlert(context, currentValue);
        }
    }
    
    /**
     * 更新滑动窗口数据
     */
    private void updateSlidingWindow(AlertContext context, double currentValue) {
        Queue<TimestampedValue> recentValues = context.getRecentValues();
        long currentTime = System.currentTimeMillis();
        
        // 清理过期数据
        cleanExpiredData(context, currentTime);
        
        // 添加带时间戳的新数据
        recentValues.offer(TimestampedValue.of(currentValue));
        
        // 记录时间戳
        context.setLastUpdateTime(currentTime);
    }
    
    /**
     * 清理过期数据
     */
    private void cleanExpiredData(AlertContext context, long currentTime) {
        Queue<TimestampedValue> recentValues = context.getRecentValues();
        long periodMillis = context.getRule().getPeriod() * 1000L;
        
        // 移除超过时间窗口的数据
        while (!recentValues.isEmpty()) {
            TimestampedValue oldestValue = recentValues.peek();
            if (oldestValue != null && (currentTime - oldestValue.getTimestamp()) > periodMillis) {
                // 数据已过期，移除
                recentValues.poll();
            } else {
                // 遇到未过期数据，停止清理
                break;
            }
        }
    }
    
    /**
     * 检查阈值条件
     */
    private boolean checkThresholdCondition(AlertContext context) {
        AlertRule rule = context.getRule();
        Queue<TimestampedValue> recentValues = context.getRecentValues();
        
        // 检查数据量是否足够
        if (recentValues.size() < rule.getCount()) {
            return false;
        }
        
        // 检查在时间窗口内达到阈值的次数
        int exceedCount = 0;
        for (TimestampedValue timestampedValue : recentValues) {
            if (timestampedValue.getValue() >= rule.getThreshold()) {
                exceedCount++;
            }
        }
        
        return exceedCount >= rule.getCount();
    }
    
    /**
     * 检查是否在静默期内
     */
    public boolean isInSilencePeriod(AlertContext context) {
        long currentTime = System.currentTimeMillis();
        long lastAlertTime = context.getLastAlertTime();
        long silencePeriodMillis = context.getRule().getSilencePeriod() * 1000L;
        
        return currentTime - lastAlertTime < silencePeriodMillis;
    }
    
    /**
     * 触发告警
     */
    public void triggerAlert(AlertContext context, double currentValue) {
        String message = buildAlertMessage(context, currentValue);
        String alertType = context.getRule().getType().name();
        
        // 根据告警类型选择对应的通知器
        List<AlertNotifier> matchedNotifiers = getNotifiersForAlertType(alertType);
        
        // 发送告警通知
        boolean success = false;

        for (AlertNotifier notifier : matchedNotifiers) {
            if (notifier.sendAlert(context, message)) {
                success = true;
                log.debug("告警通知发送成功 - 通知器: {}, 告警类型: {}", notifier.getType(), alertType);
            }
        }
        
        if (success) {
            // 更新最后告警时间
            context.setLastAlertTime(System.currentTimeMillis());
            // 清空滑动窗口数据
            context.getRecentValues().clear();
            
            // 记录告警日志
            log.info("记录告警日志 - 线程池: {}, 类型: {}, 当前值: {}", 
                    context.getPoolName(), alertType, currentValue);
            logAlertToDatabase(context, message);
            
            log.info("告警触发成功 - 线程池: {}, 类型: {}, 当前值: {}, 使用通知器: {}", 
                    context.getPoolName(), alertType, currentValue, 
                    matchedNotifiers.stream().map(AlertNotifier::getType).collect(Collectors.toList()));
        } else {
            log.warn("告警触发失败 - 线程池: {}, 类型: {}, 当前值: {}, 无可用通知器", 
                    context.getPoolName(), alertType, currentValue);
        }
    }
    
    /**
     * 记录告警日志到数据库
     */
    private void logAlertToDatabase(AlertContext context, String message) {
        try {
            PoolAlertLog alertLog = new PoolAlertLog();
            alertLog.setAppName(context.getAppName());
            alertLog.setPoolName(context.getPoolName());
            alertLog.setAlertType(context.getRule().getType().name());
            alertLog.setAlertLevel(context.getRule().getLevel().getCode());
            alertLog.setContent(message);
            
            poolAlertLogService.logAlert(alertLog);
            log.debug("告警日志记录成功 - 线程池: {}, 类型: {}", context.getPoolName(), context.getRule().getType());
        } catch (Exception e) {
            log.error("记录告警日志失败 - 线程池: {}, 类型: {}", context.getPoolName(), context.getRule().getType(), e);
        }
    }
    
    /**
     * 根据告警类型获取对应的通知器
     * @param alertType 告警类型
     * @return 匹配的通知器列表
     */
    private List<AlertNotifier> getNotifiersForAlertType(String alertType) {
        return notifiers.stream()
                .filter(notifier -> {
                    Set<String> supportedTypes = notifierAlertTypeMapping.get(notifier.getType());
                    // 如果没有配置映射关系，或者配置了映射关系且包含当前告警类型，则使用该通知器
                    return supportedTypes == null || supportedTypes.contains(alertType);
                })
                .collect(Collectors.toList());
    }
    
    /**
     * 构建告警消息
     */
    private String buildAlertMessage(AlertContext context, double currentValue) {
        AlertRule rule = context.getRule();
        return String.format(
            "线程池 [%s] 触发告警\n" +
            "告警类型: %s\n" +
            "当前值: %.2f\n" +
            "阈值: %d\n" +
            "触发条件: %d秒内达到%d次\n" +
            "时间: %s",
            context.getPoolName(),
            rule.getType(),
            currentValue,
            rule.getThreshold(),
            rule.getPeriod(),
            rule.getCount(),
            LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"))
        );
    }
    
    /**
     * 获取线程池的告警上下文
     */
    public Map<String, AlertContext> getAlertContexts(String poolName) {
        Map<String, AlertContext> contexts = poolAlertContexts.get(poolName);
        return contexts != null ? new ConcurrentHashMap<>(contexts) : Collections.emptyMap();
    }
    
    /**
     * 获取所有线程池的告警配置
     */
    public Map<String, Map<String, AlertContext>> getAllAlertContexts() {
        return new ConcurrentHashMap<>(poolAlertContexts);
    }
    
    /**
     * 获取指定告警类型的阈值配置
     * @param poolName 线程池名称
     * @param alertType 告警类型
     * @return 阈值配置，如果未配置返回-1
     */
    public int getAlertThreshold(String poolName, String alertType) {
        Map<String, AlertContext> poolContexts = poolAlertContexts.get(poolName);
        if (poolContexts == null) {
            return -1;
        }
        
        AlertContext context = poolContexts.get(alertType);
        if (context == null || !context.getRule().isEnabled()) {
            return -1;
        }
        
        return context.getRule().getThreshold();
    }
}
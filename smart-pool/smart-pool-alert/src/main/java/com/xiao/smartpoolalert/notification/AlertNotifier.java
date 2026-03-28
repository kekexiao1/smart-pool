package com.xiao.smartpoolalert.notification;

import com.xiao.smartpoolalert.context.AlertContext;

/**
 * 告警通知器接口
 * 使用策略模式，方便后续扩展不同的通知方式
 */
public interface AlertNotifier {
    
    /**
     * 发送告警通知
     * @param context 告警上下文
     * @param message 告警消息
     * @return 是否发送成功
     */
    boolean sendAlert(AlertContext context, String message);
    
    /**
     * 获取通知器类型
     * @return 通知器类型
     */
    String getType();
}
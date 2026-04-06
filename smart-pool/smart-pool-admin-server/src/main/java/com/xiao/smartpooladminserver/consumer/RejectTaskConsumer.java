package com.xiao.smartpooladminserver.consumer;

import com.alibaba.fastjson2.JSON;

import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.apache.rocketmq.spring.annotation.RocketMQMessageListener;
import org.apache.rocketmq.spring.core.RocketMQListener;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

import java.util.Map;

@Slf4j
@Service
@ConditionalOnProperty(prefix = "rocketmq", name = "name-server")
@RocketMQMessageListener(
    topic = "THREAD_POOL_REJECT_TOPIC",
    consumerGroup = "reject-consumer-group",
    selectorExpression = "*"
)
public class RejectTaskConsumer implements RocketMQListener<String> {
    @PostConstruct
    public void init() {
        log.info("RejectTaskConsumer 已加载，准备消费消息");
    }

    @Override
    public void onMessage(String message) {
        log.info("收到消息: {}", message);

        if (message == null || message.trim().isEmpty()) {
            log.warn("收到空消息，跳过处理");
            return;
        }
        
        // 判断消息类型：JSON格式还是普通文本
        if (isJsonMessage(message)) {
            // 处理JSON格式的拒绝任务消息
            processJsonMessage(message);
        } else {
            // 处理普通文本消息（测试消息）
            processPlainTextMessage(message);
        }
    }
    
    private boolean isJsonMessage(String message) {
        String trimmed = message.trim();
        return trimmed.startsWith("{") && trimmed.endsWith("}");
    }
    
    private void processJsonMessage(String message) {
        try {
            Map<String, Object> taskData = JSON.parseObject(message, Map.class);
            
            String originalPoolName = (String) taskData.get("originalPoolName");
            String taskId = (String) taskData.get("taskId");
            String businessType = (String) taskData.get("businessType");
            Object payload = taskData.get("payload");
            Long createTime = (Long) taskData.get("createTime");

            log.info("处理被拒绝任务 - 线程池: {}, 任务ID: {}, 业务类型: {}, 创建时间: {}",
                originalPoolName, taskId, businessType, createTime);

            // 处理业务
            try {
                processRejectedTask(taskData);
                log.info("消息处理成功: {}", taskData.get("taskId"));
            } catch (Exception e) {
                log.error("业务处理失败: {}", message, e);
                // 关键：抛出异常，让 RocketMQ 重试
                throw new RuntimeException("业务处理失败", e);
            }
        } catch (Exception e) {
            log.error("JSON解析失败: {}", message, e);
            // 解析失败不重试，直接返回
        }
    }
    
    private void processPlainTextMessage(String message) {
        log.info("处理普通文本消息: {}", message);
        // 对于测试消息，简单记录即可，不需要复杂的业务处理
        if (message.contains("测试消息")) {
            log.info("收到测试消息，确认消费者正常工作");
        }
    }
    
    private void logFailedMessage(String message, Exception e) {
        // 记录失败消息到日志文件或Redis，用于后续处理
        log.error("消息处理失败，原始消息: {}, 异常: {}", message, e.getMessage());
    }

    private void processRejectedTask(Map<String, Object> taskData) {
        String businessType = (String) taskData.get("businessType");
        
        if (businessType == null) {
            log.warn("任务缺少业务类型，跳过处理");
            return;
        }
        
        switch (businessType) {
            case "ORDER":
                processOrderTask(taskData);
                break;
            case "PAYMENT":
                processPaymentTask(taskData);
                break;
            default:
                log.info("未知业务类型: {}，记录任务信息", businessType);
                recordTask(taskData);
        }
    }

    private void processOrderTask(Map<String, Object> taskData) {
        log.info("处理订单任务: taskId={}", taskData.get("taskId"));
    }

    private void processPaymentTask(Map<String, Object> taskData) {
        log.info("处理支付任务: taskId={}", taskData.get("taskId"));
    }

    private void recordTask(Map<String, Object> taskData) {
        log.info("记录任务: {}", JSON.toJSONString(taskData));
    }
}

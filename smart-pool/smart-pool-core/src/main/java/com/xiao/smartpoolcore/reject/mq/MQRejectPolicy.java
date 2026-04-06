package com.xiao.smartpoolcore.reject.mq;

import com.alibaba.fastjson2.JSON;
import com.xiao.smartpoolcore.core.task.PoolTask;
import com.xiao.smartpoolcore.reject.AbstractRejectPolicy;
import com.xiao.smartpoolcore.common.util.LocalDiskHelper;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.rocketmq.spring.core.RocketMQTemplate;
import org.springframework.messaging.Message;
import org.springframework.messaging.support.MessageBuilder;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ThreadPoolExecutor;

/**
 * RocketMQ拒绝策略
 * 将拒绝的任务发送到RocketMQ进行异步处理
 */
@AllArgsConstructor
@Slf4j
public class MQRejectPolicy extends AbstractRejectPolicy {

    private static final String REJECT_TASK_TOPIC = "THREAD_POOL_REJECT_TOPIC";
    private static final String REJECT_TASK_TAG = "reject";
    private static final int MAX_RETRY_COUNT = 3;

    private final RocketMQTemplate rocketMQTemplate;

    @Override
    protected void doReject(Runnable task, ThreadPoolExecutor executor) {

        // 尝试MQ持久化
        boolean mqSuccess = tryMQPersist(task, executor);
        
        if (!mqSuccess) {
            boolean redisSuccess=false;
            if (!redisSuccess) {
                // Redis失败，尝试本地磁盘持久化
                tryLocalDiskFallback(task, executor);
            }
        }
    }

    /**
     * 尝试RocketMQ持久化
     */
    private boolean tryMQPersist(Runnable task, ThreadPoolExecutor executor) {
        String threadPoolName = getThreadPoolName(executor);
        
        // 检查RocketMQTemplate是否可用
        if (rocketMQTemplate == null) {
            log.warn("RocketMQTemplate不可用，直接降级到本地磁盘策略，线程池: {}", threadPoolName);
            return false;
        }
        
        for (int retry = 0; retry < MAX_RETRY_COUNT; retry++) {
            try {
                // 创建消息
                Message<String> message = createRejectTaskMessage(task, threadPoolName);
                
                // 发送到RocketMQ
                String destination = REJECT_TASK_TOPIC + ":" + REJECT_TASK_TAG;
                rocketMQTemplate.send(destination, message);
                
                log.info("任务成功发送到RocketMQ，线程池: {}, 重试次数: {}", threadPoolName, retry);
                return true;
                
            } catch (Exception e) {
                log.warn("RocketMQ持久化失败，线程池: {}, 重试: {}/{}, 错误: {}", 
                    threadPoolName, retry + 1, MAX_RETRY_COUNT, e.getMessage());
                
                if (retry == MAX_RETRY_COUNT - 1) {
                    log.error("RocketMQ持久化最终失败，线程池: {}", threadPoolName);
                    return false;
                }
                // 短暂延迟后重试
                try {
                    Thread.sleep(100 * (retry + 1));
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                    return false;
                }
            }
        }
        
        return false;
    }

    /**
     * 创建拒绝任务消息
     */
    private Message<String> createRejectTaskMessage(Runnable task, String threadPoolName) {
        try {
            // 构建消息内容
            String messageContent = buildMessageContent(task, threadPoolName);
            
            // 创建Spring Message，设置headers
            return MessageBuilder.withPayload(messageContent)
                    .setHeader("threadPoolName", threadPoolName)
                    .setHeader("taskClass", task.getClass().getName())
                    .setHeader("rejectTime", System.currentTimeMillis())
                    .setHeader("contentType", "application/json")
                    .build();
            
        } catch (Exception e) {
            log.error("创建RocketMQ消息失败，线程池: {}, 错误: {}", threadPoolName, e.getMessage());
            throw new RuntimeException("创建RocketMQ消息失败", e);
        }
    }

    /**
     * 构建消息内容
     */
    private String buildMessageContent(Runnable task, String threadPoolName) {
        Map<String, Object> content = new HashMap<>();
        
        content.put("originalPoolName", threadPoolName);
        if (task instanceof PoolTask) {
            PoolTask poolTask = (PoolTask) task;
            content.put("taskId", poolTask.getTaskId());
            content.put("businessType", poolTask.getBusinessType());
            content.put("payload", poolTask.getPayload());
            content.put("createTime", poolTask.getCreateTime());
        }
        
        return JSON.toJSONString(content);
    }

    /**
     * Redis兜底策略
     */
    private boolean tryRedisFallback(Runnable task, ThreadPoolExecutor executor) {
        String threadPoolName = getThreadPoolName(executor);
        
        try {
            // 这里需要RedisTemplate，暂时记录日志
        
            log.warn("MQ失败，尝试Redis兜底（需要配置RedisTemplate），线程池: {}", threadPoolName);
            return false;
        } catch (Exception e) {
            log.error("Redis兜底失败，线程池: {}", threadPoolName);
            return false;
        }
    }

    /**
     * 本地磁盘兜底策略
     */
    private void tryLocalDiskFallback(Runnable task, ThreadPoolExecutor executor) {
        String threadPoolName = getThreadPoolName(executor);
        
        try {
            LocalDiskHelper.saveTask(task, threadPoolName);
            log.info("任务成功持久化到本地磁盘（MQ兜底），线程池: {}", threadPoolName);
        } catch (Exception e) {
            log.error("本地磁盘持久化失败（MQ兜底），线程池: {}, 错误: {}", threadPoolName, e.getMessage());
            
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
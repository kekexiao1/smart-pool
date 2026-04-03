package com.xiao.smartpoolcore.reject.redis;

import com.alibaba.fastjson.JSON;
import com.xiao.smartpoolcore.core.task.PoolTask;
import com.xiao.smartpoolcore.reject.AbstractRejectPolicy;
import com.xiao.smartpoolcore.common.util.LocalDiskHelper;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ThreadPoolExecutor;

@AllArgsConstructor
@Slf4j
public class RedisRejectPolicy extends AbstractRejectPolicy {
	private static final String THREAD_POOL_REJECT_QUEUE = "thread:pool:reject:queue";
	private static final int MAX_RETRY_COUNT = 3;

	private final RedisTemplate<String, Object> redisTemplate;

	@Override
	protected void doReject(Runnable task, ThreadPoolExecutor executor) {
		
		// 尝试Redis持久化
		boolean redisSuccess = tryRedisPersist(task, executor);
		
		if (!redisSuccess) {
			// Redis失败，尝试本地磁盘持久化
			tryLocalDiskPersist(task, executor);
		}
	}

	/**
	 * 尝试Redis持久化
	 */
	private boolean tryRedisPersist(Runnable task, ThreadPoolExecutor executor) {
		String threadPoolName = getThreadPoolName(executor);

		if(redisTemplate==null){
			log.warn("RedisTemplate，直接降级到本地磁盘策略，线程池: {}", threadPoolName);
			return false;
		}

		for (int retry = 0; retry < MAX_RETRY_COUNT; retry++) {
			try {
				// 构建标准任务消息格式
				Map<String, Object> taskMessage = buildTaskMessage(task, threadPoolName);
				String taskJson = JSON.toJSONString(taskMessage);
				
				// 验证序列化结果
				if (taskJson == null || taskJson.trim().isEmpty()) {
					log.error("任务序列化失败，线程池: {}", threadPoolName);
					return false;
				}
				
				// 存入Redis队列（左进右出）
				redisTemplate.opsForList().leftPush(THREAD_POOL_REJECT_QUEUE, taskJson);
			
				log.info("任务成功持久化到Redis，线程池: {}, 重试次数: {}", threadPoolName, retry);
				return true;
				
			} catch (Exception e) {
				log.warn("Redis持久化失败，线程池: {}, 重试: {}/{}, 错误: {}", 
					threadPoolName, retry + 1, MAX_RETRY_COUNT, e.getMessage());
				
				if (retry == MAX_RETRY_COUNT - 1) {
					log.error("Redis持久化最终失败，线程池: {}", threadPoolName);
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
	 * 构建标准任务消息格式
	 */
	private Map<String, Object> buildTaskMessage(Runnable task, String threadPoolName) {
		Map<String, Object> message = new HashMap<>();
		
		// 基础拒绝信息
		message.put("rejectTime", System.currentTimeMillis());
		message.put("rejectReason", "THREAD_POOL_FULL");
		message.put("originalPoolName", threadPoolName);
		
		// 如果是PoolTask，包含详细信息
		if (task instanceof PoolTask) {
			PoolTask poolTask = (PoolTask) task;
			
			// 必填字段
			message.put("taskId", poolTask.getTaskId());
			message.put("taskType", poolTask.getTaskType());
			message.put("businessType", poolTask.getBusinessType());
			message.put("payload", poolTask.getPayload());
			message.put("createTime", poolTask.getCreateTime());
			message.put("retryCount", poolTask.getRetryCount().get());
			
			// 可选字段
			if (poolTask.getMaxRetries() > 0) {
				message.put("maxRetries", poolTask.getMaxRetries());
			}
			if (poolTask.getOriginalPoolName() != null) {
				message.put("originalPoolName", poolTask.getOriginalPoolName());
			}
			if (poolTask.getPriority() != 5) {
				message.put("priority", poolTask.getPriority());
			}
			if (poolTask.getTraceId() != null) {
				message.put("traceId", poolTask.getTraceId());
			}
			if (poolTask.getTtl() != null) {
				message.put("ttl", poolTask.getTtl());
			}
			if (poolTask.getExceptionStack() != null) {
				message.put("exceptionStack", poolTask.getExceptionStack());
			}
		}
		
		return message;
	}

	/**
	 * 尝试本地磁盘持久化
	 */
	private void tryLocalDiskPersist(Runnable task, ThreadPoolExecutor executor) {
		String threadPoolName = getThreadPoolName(executor);
		
		try {
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

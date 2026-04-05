package com.xiao.smartpoolcore.reject.redis;

import com.alibaba.fastjson.JSON;
import com.xiao.smartpoolcore.core.task.PoolTask;
import com.xiao.smartpoolcore.reject.AbstractRejectPolicy;
import com.xiao.smartpoolcore.common.util.LocalDiskHelper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

@Slf4j
public class RedisRejectPolicy extends AbstractRejectPolicy {
	private static final String THREAD_POOL_REJECT_QUEUE = "thread:pool:reject:queue";
	private static final int BATCH_SIZE = 100;
	private static final int FLUSH_INTERVAL_MS = 1000;
	private static final int QUEUE_CAPACITY = 10000;

	private final RedisTemplate<String, Object> redisTemplate;
	private final BlockingQueue<Map<String, Object>> taskQueue;
	private final AtomicBoolean running;
	private final Thread flushThread;

	public RedisRejectPolicy(RedisTemplate<String, Object> redisTemplate) {
		this.redisTemplate = redisTemplate;
		this.taskQueue = new LinkedBlockingQueue<>(QUEUE_CAPACITY);
		this.running = new AtomicBoolean(true);
		this.flushThread = new Thread(this::flushTask, "redis-reject-flush-thread");
		this.flushThread.setDaemon(true);
		this.flushThread.start();
	}

	@Override
	protected void doReject(Runnable task, ThreadPoolExecutor executor) {
		String threadPoolName = getThreadPoolName(executor);
		Map<String, Object> message = buildTaskMessage(task, threadPoolName);
		
		if (!taskQueue.offer(message)) {
			log.warn("任务队列已满，降级到本地磁盘策略，线程池: {}", threadPoolName);
			tryLocalDiskPersist(task, executor);
		}
	}

	private Map<String, Object> buildTaskMessage(Runnable task, String threadPoolName) {
		Map<String, Object> message = new HashMap<>();
		
		message.put("originalPoolName", threadPoolName);
		if (task instanceof PoolTask) {
			PoolTask poolTask = (PoolTask) task;
			message.put("taskId", poolTask.getTaskId());
			message.put("businessType", poolTask.getBusinessType());
			message.put("payload", poolTask.getPayload());
			message.put("createTime", poolTask.getCreateTime());
		}
		return message;
	}

	private void flushTask() {
		List<Map<String, Object>> batch = new ArrayList<>(BATCH_SIZE);
		
		while (running.get() || !taskQueue.isEmpty()) {
			try {
				batch.clear();
				
				Map<String, Object> first = taskQueue.poll(FLUSH_INTERVAL_MS, TimeUnit.MILLISECONDS);
				if (first == null) {
					continue;
				}
				batch.add(first);
				
				taskQueue.drainTo(batch, BATCH_SIZE - 1);
				
				if (!batch.isEmpty()) {
					flushToRedis(batch);
				}
				
			} catch (InterruptedException e) {
				Thread.currentThread().interrupt();
				break;
			}
		}
		
		if (!batch.isEmpty()) {
			flushToRedis(batch);
		}
	}

	private void flushToRedis(List<Map<String, Object>> batch) {
		if (redisTemplate == null) {
			log.warn("RedisTemplate 为空，无法批量保存 {} 个任务", batch.size());
			return;
		}

		try {
			List<String> jsonList = new ArrayList<>(batch.size());
			for (Map<String, Object> message : batch) {
				String json = JSON.toJSONString(message);
				if (json != null && !json.trim().isEmpty()) {
					jsonList.add(json);
				}
			}

			if (!jsonList.isEmpty()) {
				redisTemplate.opsForList().leftPushAll(THREAD_POOL_REJECT_QUEUE, jsonList.toArray());
				log.info("批量保存 {} 个任务到 Redis 成功", jsonList.size());
			}
			
		} catch (Exception e) {
			log.error("批量保存任务到 Redis 失败，任务数: {}, 错误: {}", batch.size(), e.getMessage());
		}
	}

	private void tryLocalDiskPersist(Runnable task, ThreadPoolExecutor executor) {
		String threadPoolName = getThreadPoolName(executor);
		
		try {
			LocalDiskHelper.saveTask(task, threadPoolName);
			log.info("任务成功持久化到本地磁盘，线程池: {}", threadPoolName);
		} catch (Exception e) {
			log.error("本地磁盘持久化失败，线程池: {}, 错误: {}", threadPoolName, e.getMessage());
		}
	}

	public void shutdown() {
		running.set(false);
		flushThread.interrupt();
		try {
			flushThread.join(5000);
		} catch (InterruptedException e) {
			Thread.currentThread().interrupt();
		}
	}
}

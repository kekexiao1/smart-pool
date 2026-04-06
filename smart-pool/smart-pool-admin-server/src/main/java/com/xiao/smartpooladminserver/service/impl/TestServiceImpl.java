package com.xiao.smartpooladminserver.service.impl;

import com.xiao.smartpooladminserver.service.TestService;
import com.xiao.smartpoolcore.core.executor.DynamicThreadPoolExecutor;
import com.xiao.smartpoolcore.core.registry.ThreadPoolRegistry;
import com.xiao.smartpoolcore.model.dto.ThreadPoolConfig;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.rocketmq.spring.core.RocketMQTemplate;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.stereotype.Service;

import java.util.Random;

@Service
@Slf4j
@RequiredArgsConstructor
public class TestServiceImpl implements TestService {

	private final RocketMQTemplate rocketMQTemplate;

	private final ThreadPoolRegistry registry;

	/**
	 * 发送普通同步消息（最常用，等待Broker确认返回结果）
	 * @param topic 主题名
	 * @param message 消息内容
	 */
	public void sendSyncMessage(String topic, String message) {
		// topic:tag 格式，如果不需要tag，直接写topic即可
		String destination = topic + ":TagA";

		// 发送同步消息
		rocketMQTemplate.syncSend(destination, MessageBuilder.withPayload(message).build());
		System.out.println("【生产者】发送同步消息成功：Topic=" + topic + "，内容=" + message);
	}


	public void executeTestTask(String poolName, int taskCount) {
		DynamicThreadPoolExecutor executor = registry.getExecutor(poolName);
		if (executor == null) {
			throw new IllegalArgumentException("线程池[" + poolName + "]不存在");
		}

		Random random = new Random();
		int sleepTime = random.nextInt(20)+10;
		for (int i = 0; i < taskCount; i++) {
			int taskId=i+1;
			Runnable task=() -> {
				try {
					Thread.sleep(sleepTime);
				} catch (InterruptedException e) {
					Thread.currentThread().interrupt();
				}
			};
			String payload = String.format("{\"orderId\":\"order-%d\"}", taskId);
			executor.execute(task, "taskId-"+taskId, "TEST_TASK", "ORDER", payload);
		}
	}

	public String testQueueFullAlert(String poolName, int durationSeconds) {
		DynamicThreadPoolExecutor executor = registry.getExecutor(poolName);
		if (executor == null) {
			throw new IllegalArgumentException("线程池[" + poolName + "]不存在");
		}

		ThreadPoolConfig config = executor.getConfig();
		int queueCapacity = config.getQueueCapacity();
		int targetQueueSize = (int) (queueCapacity * 0.9);

		log.info("开始测试ThreadPoolQueueFull告警 - 线程池: {}, 队列容量: {}, 目标队列大小: {}, 持续时间: {}秒",
				poolName, queueCapacity, targetQueueSize, durationSeconds);

		for (int i = 0; i < targetQueueSize; i++) {
			int taskId=i+1;
			Runnable task=() -> {
				try {
					Thread.sleep(1000);
				} catch (InterruptedException e) {
					Thread.currentThread().interrupt();
				}
			};
			String payload = String.format("{\"orderId\":\"order-%d\"}", taskId);
			executor.execute(task, "taskId-"+taskId, "QUEUE_TEST", "ORDER", payload);
		}

		return String.format("ThreadPoolQueueFull告警测试已启动。线程池: %s, 当前队列大小: %d/%d (%.1f%%), 测试将持续: %d秒",
				poolName, executor.getQueueSize(), queueCapacity,
				(executor.getQueueSize() * 100.0 / queueCapacity), durationSeconds);
	}

	public String testRunTimeoutAlert(String poolName, int durationSeconds) {
		DynamicThreadPoolExecutor executor = registry.getExecutor(poolName);
		if (executor == null) {
			throw new IllegalArgumentException("线程池[" + poolName + "]不存在");
		}

		log.info("开始测试ThreadPoolRunTimeout告警 - 线程池: {}, 持续时间: {}秒",
				poolName, durationSeconds);

		Runnable task=() -> {
			try {
				Thread.sleep(durationSeconds);
			} catch (InterruptedException e) {
				Thread.currentThread().interrupt();
			}
		};
		String payload = String.format("{\"orderId\":\"order-1\",\"duration\":%d}", durationSeconds);
		executor.execute(task, "taskId-1", "TIMEOUT_TEST", "ORDER", payload);

		return String.format("ThreadPoolRunTimeout告警测试已启动。线程池: %s, 任务执行时间: %d秒, 超时阈值: 5秒",
				poolName, durationSeconds);
	}
}

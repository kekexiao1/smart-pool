package com.xiao.smartpooladminserver.controller;

import com.xiao.smartpooladminserver.common.result.Result;
import com.xiao.smartpooladminserver.service.impl.TestServiceImpl;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/test")
@RequiredArgsConstructor
@Slf4j
public class TestAlert {

	private final TestServiceImpl testService;

	// 测试Topic，和你之前创建的保持一致
	private static final String TEST_TOPIC = "test-topic";

	@GetMapping("/test")
	public Result runTask(@RequestParam String poolName) {
		try {
			testService.executeTestTask(poolName, 1);
			return Result.success();
		} catch (IllegalArgumentException e) {
			log.error(e.getMessage());
			return Result.failure(e.getMessage());
		}
	}

	@GetMapping("/queue-full-alert")
	public Result<String> testQueueFullAlert(
			@RequestParam(defaultValue = "order-pool") String poolName,
			@RequestParam(defaultValue = "3") int durationSeconds) {
		try {
			String result = testService.testQueueFullAlert(poolName, durationSeconds);
			return Result.success(result);
		} catch (IllegalArgumentException e) {
			return Result.failure(e.getMessage());
		}
	}

	@GetMapping("/run-timeout-alert")
	public Result<String> testRunTimeoutAlert(
			@RequestParam(defaultValue = "order-pool") String poolName,
			@RequestParam(defaultValue = "15") int durationSeconds) {
		try {
			String result = testService.testRunTimeoutAlert(poolName, durationSeconds);
			return Result.success(result);
		} catch (IllegalArgumentException e) {
			return Result.failure(e.getMessage());
		}
	}

	@GetMapping("/sync")
	public String testSyncMessage() {
		// 发送JSON格式的测试消息，与消费者期望的格式一致
		String jsonMessage = String.format("{\"originalPoolName\":\"test-pool\",\"taskId\":\"test-%d\",\"businessType\":\"TEST\",\"payload\":\"这是一条同步测试消息：%d\",\"createTime\":%d}",
			System.currentTimeMillis(), System.currentTimeMillis(), System.currentTimeMillis());
		testService.sendSyncMessage(TEST_TOPIC, jsonMessage);
		return "同步消息发送成功！消息格式：JSON";
	}
	
	@GetMapping("/sync-text")
	public String testSyncTextMessage() {
		// 发送普通文本消息，测试消费者对多种消息格式的支持
		testService.sendSyncMessage(TEST_TOPIC, "这是一条同步测试消息：" + System.currentTimeMillis());
		return "同步文本消息发送成功！消息格式：普通文本";
	}

}

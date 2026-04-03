package com.xiao.smartpooladminserver.controller;

import com.xiao.smartpooladminserver.common.result.Result;
import com.xiao.smartpooladminserver.service.TestService;
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

	private final TestService testService;

	@GetMapping("/test")
	public Result runTask() {
		log.info("开始测试");
		try {
			testService.executeTestTask("order-service-pool", 30);
			return Result.success();
		} catch (IllegalArgumentException e) {
			log.error(e.getMessage());
			return Result.failure(e.getMessage());
		}
	}

	@GetMapping("/test/queue-full-alert")
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

	@GetMapping("/test/run-timeout-alert")
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
}

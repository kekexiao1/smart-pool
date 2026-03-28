package com.xiao.smartpooladminserver.controller;

import com.xiao.smartpooladminserver.model.vo.ThreadPoolMetricsVO;
import com.xiao.smartpooladminserver.common.result.Result;
import com.xiao.smartpooladminserver.service.ThreadPoolMetricsService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/threadpool")
@Slf4j
@RequiredArgsConstructor
public class ThreadPoolMetricsController {

	private final ThreadPoolMetricsService metricsService;

	@GetMapping("/metrics")
	public Result<List<ThreadPoolMetricsVO>> listAllThreadPoolMetrics() {
		List<ThreadPoolMetricsVO> metricsList = metricsService.getAllThreadPoolMetrics();
		return Result.success(metricsList);
	}

	@GetMapping("/test")
	public Result runTask() {
		log.info("开始测试");
		try {
			metricsService.executeTestTask("order-service-pool", 30);
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
			String result = metricsService.testQueueFullAlert(poolName, durationSeconds);
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
			String result = metricsService.testRunTimeoutAlert(poolName, durationSeconds);
			return Result.success(result);
		} catch (IllegalArgumentException e) {
			return Result.failure(e.getMessage());
		}
	}
}

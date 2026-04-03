package com.xiao.smartpoolcore.core.task;

import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import java.io.Serializable;
import java.util.concurrent.atomic.AtomicInteger;

@Slf4j
@Data
public class PoolTask implements Runnable, Serializable {
	private static final long serialVersionUID = 1L;

	// 必填字段
	private String taskId;                                        // 任务唯一标识
	private String taskType;                                      // 任务类型/业务码
	private String businessType;								  // 业务类型
	private String payload;                                       // 任务参数(JSON格式)
	private long createTime = System.currentTimeMillis();         // 任务创建时间
	private AtomicInteger retryCount = new AtomicInteger(0);      // 当前重试次数

	// 可选字段
	private int maxRetries = 3;                                   // 最大重试次数
	private String originalPoolName;                              // 原始线程池名称
	private int priority = 5;                                     // 任务优先级(1-10, 1最高)
	private String traceId;                                       // 分布式链路追踪ID
	private Long ttl;                                             // 任务有效期(毫秒)
	private String exceptionStack;                                // 异常堆栈信息

	// 业务任务
	private Runnable realTask;

	public PoolTask(Runnable realTask, String taskId, String taskType, String businessType,String payload) {
		this.taskId = taskId;
		this.taskType = taskType;
		this.payload = payload;
		this.realTask = realTask;
		this.businessType=businessType;
	}

	public PoolTask(Runnable realTask, String taskId, String taskType, String businessType, String payload,
					  int priority, int maxRetries, String originalPoolName, String traceId) {
		this(realTask, taskId, taskType, businessType, payload);
		this.priority = priority;
		this.maxRetries = maxRetries;
		this.originalPoolName = originalPoolName;
		this.traceId = traceId;
	}

	@Override
	public void run() {
		try{
			realTask.run();
			log.info("任务执行成功 | taskId={}, taskType={}", taskId, taskType);
		}catch (Exception e){
			int current = retryCount.incrementAndGet();
			if(maxRetries == current){
				log.error("任务最终执行失败, taskId={}, taskType={}", taskId, taskType);
				throw new RuntimeException("达到最大重试次数");
			}
			log.error("任务执行失败 {}/{}, taskId={}, taskType={}", current, maxRetries, taskId, taskType, e);
			throw e;
		}

	}
}

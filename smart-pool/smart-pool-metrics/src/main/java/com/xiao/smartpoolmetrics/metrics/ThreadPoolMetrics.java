package com.xiao.smartpoolmetrics.metrics;


import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class ThreadPoolMetrics {

	/** 线程池名称 */
	private String poolName;

	/** 当前线程数 */
	private int poolSize;

	/** 活跃线程数 */
	private int activeCount;

	/** 最大线程数 */
	private int maximumPoolSize;

	/** 队列当前大小 */
	private int queueSize;

	/** 队列容量 */
	private int queueCapacity;

	/** 已提交任务数 */
	private long taskCount;

	/** 已完成任务数 */
	private long completedTaskCount;

	 /** 平均任务执行时间（毫秒） */
	 private double avgExecutionTime;

	 /** 最大任务执行时间（毫秒） */
	private double maxExecutionTime;

	/** 拒绝任务数 */
	private boolean rejectedTask;

	/** 采集时间 */
	private long timestamp = System.currentTimeMillis();

}

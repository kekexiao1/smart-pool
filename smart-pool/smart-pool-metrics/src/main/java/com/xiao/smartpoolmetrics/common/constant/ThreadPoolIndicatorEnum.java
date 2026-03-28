package com.xiao.smartpoolmetrics.common.constant;


import lombok.Getter;

@Getter
public enum ThreadPoolIndicatorEnum {
	// ========== Gauge指标（瞬时值）==========
	ACTIVE_COUNT("thread_pool_active_count", "线程池活跃线程数"),
	QUEUE_SIZE("thread_pool_queue_size", "线程池队列积压数"),
	QUEUE_CAPACITY("thread_pool_queue_capacity", "线程池队列容量"),
	CORE_POOL_SIZE("thread_pool_core_pool_size", "线程池核心线程数"),
	MAX_POOL_SIZE("thread_pool_max_pool_size", "线程池最大线程数"),
	CURRENT_POOL_SIZE("thread_pool_current_pool_size", "线程池当前线程数"),
	QUEUE_REMAINING_CAPACITY("thread_pool_queue_remaining_capacity", "线程池队列剩余容量"),


	EXECUTE_TIME("thread_pool_execute_time", "任务执行所需时间"),
	WAIT_TIME("thread_pool_wait_time", "任务入队等待时间"),
	
	// ========== Counter指标（累计值）==========
	TASK_COUNT("thread_pool_task_count", "任务总数"),
	RUN_TIMEOUT_TOTAL("thread_pool_run_timeout_count","超时任务数"),
	REJECT_COUNT_TOTAL("thread_pool_reject_count", "线程池拒绝任务总数"),
	EXCEPTION_COUNT_TOTAL("thread_pool_exception_count", "线程池任务执行异常总数"),
	COMPLETED_TASK_TOTAL("thread_pool_completed_task_count", "线程池完成任务总数");

	private final String indicatorName; // Prometheus展示的指标名
	private final String desc;          // 指标描述（Prometheus HELP信息）

	ThreadPoolIndicatorEnum(String indicatorName, String desc) {
		this.indicatorName = indicatorName;
		this.desc = desc;
	}
}

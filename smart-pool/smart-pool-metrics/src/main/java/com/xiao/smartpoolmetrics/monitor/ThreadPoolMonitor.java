package com.xiao.smartpoolmetrics.monitor;

import com.xiao.smartpoolcore.core.executor.DynamicThreadPoolExecutor;
import com.xiao.smartpoolcore.core.manager.TaskExecutionCallbackManager;
import com.xiao.smartpoolcore.core.registry.ThreadPoolRegistry;
import com.xiao.smartpoolmetrics.callback.MetricsTaskExecutionCallback;
import com.xiao.smartpoolmetrics.common.constant.ThreadPoolIndicatorEnum;
import io.micrometer.core.instrument.*;
import io.micrometer.core.instrument.binder.MeterBinder;
import jakarta.annotation.PreDestroy;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ThreadPoolExecutor;

@AllArgsConstructor
@Slf4j
public class ThreadPoolMonitor implements MeterBinder, ApplicationRunner {

	private final ThreadPoolRegistry threadPoolRegistry;

	private final MeterRegistry meterRegistry;

	public static final Map<String, Timer> WAIT_TIMER_CACHE=new ConcurrentHashMap<>();

	public static final Map<String, Timer> EXECUTE_TIMER_CACHE=new ConcurrentHashMap<>();

	@Override
	public void run(ApplicationArguments args) {
		log.info("ThreadPoolMonitor 启动");
		// 等待一段时间确保线程池初始化完成
        try {
            Thread.sleep(1000);
        } catch (InterruptedException e) {
            log.error("线程池监控采集器启动时，线程池初始化超时", e);
        }
		// 注册任务执行监控回调
		MetricsTaskExecutionCallback callback = new MetricsTaskExecutionCallback();
		TaskExecutionCallbackManager.registerCallback(callback);
		log.info("任务执行监控回调已注册");
		
		registerAllMetrics();
		bindTo(meterRegistry);
		log.info("线程池监控采集器已启动，所有指标已注册");
	}

	/**
	 * 注册所有指标
	 */
	private void registerAllMetrics() {
		threadPoolRegistry.getAllExecutors().forEach((threadPoolName, executor) -> {
			Tags tags = Tags.of("thread_pool_name", threadPoolName);
			registerGaugeMetrics(executor, tags);
			registerCounterMetrics(executor, tags);
			registerTimerMetrics(executor, tags);
		});
	}

	private void registerTimerMetrics(DynamicThreadPoolExecutor executor, Tags tags){
		// 注册执行时间指标
		Timer executeTimer = Timer.builder(ThreadPoolIndicatorEnum.EXECUTE_TIME.getIndicatorName())
				.tags(tags)
				.description(ThreadPoolIndicatorEnum.EXECUTE_TIME.getDesc())
				.register(meterRegistry);
		EXECUTE_TIMER_CACHE.put(executor.getThreadPoolName() ,executeTimer);

		//注册入队等待指标
		Timer waitTimer = Timer.builder(ThreadPoolIndicatorEnum.WAIT_TIME.getIndicatorName())
				.tags(tags)
				.description(ThreadPoolIndicatorEnum.WAIT_TIME.getDesc())
				.register(meterRegistry);
		WAIT_TIMER_CACHE.put(executor.getThreadPoolName(), waitTimer);
	}

	/**
	 * 注册 Gauge 指标（瞬时值）
	 */
	private void registerGaugeMetrics(DynamicThreadPoolExecutor executor, Tags tags) {
		// 活跃线程数
		Gauge.builder(ThreadPoolIndicatorEnum.ACTIVE_COUNT.getIndicatorName(), executor::getActiveCount)
				.tags(tags)
				.description(ThreadPoolIndicatorEnum.ACTIVE_COUNT.getDesc())
				.register(meterRegistry);

		// 队列大小
		Gauge.builder(ThreadPoolIndicatorEnum.QUEUE_SIZE.getIndicatorName(), executor::getQueueSize)
				.tags(tags)
				.description(ThreadPoolIndicatorEnum.QUEUE_SIZE.getDesc())
				.register(meterRegistry);

		// 队列容量（配置值）
		Gauge.builder(ThreadPoolIndicatorEnum.QUEUE_CAPACITY.getIndicatorName(),
					() -> executor.getConfig().getQueueCapacity())
				.tags(tags)
				.description(ThreadPoolIndicatorEnum.QUEUE_CAPACITY.getDesc())
				.register(meterRegistry);

		// 核心线程数（配置值）
		Gauge.builder(ThreadPoolIndicatorEnum.CORE_POOL_SIZE.getIndicatorName(),
					() -> executor.getConfig().getCorePoolSize())
				.tags(tags)
				.description(ThreadPoolIndicatorEnum.CORE_POOL_SIZE.getDesc())
				.register(meterRegistry);

		// 最大线程数
		Gauge.builder(ThreadPoolIndicatorEnum.MAX_POOL_SIZE.getIndicatorName(),
					() -> executor.getConfig().getMaximumPoolSize())
				.tags(tags)
				.description(ThreadPoolIndicatorEnum.MAX_POOL_SIZE.getDesc())
				.register(meterRegistry);

		// 当前线程数
		Gauge.builder(ThreadPoolIndicatorEnum.CURRENT_POOL_SIZE.getIndicatorName(),
					() -> executor.getExecutor().getPoolSize())
				.tags(tags)
				.description(ThreadPoolIndicatorEnum.CURRENT_POOL_SIZE.getDesc())
				.register(meterRegistry);

		// 队列剩余容量
		Gauge.builder(ThreadPoolIndicatorEnum.QUEUE_REMAINING_CAPACITY.getIndicatorName(),
					() -> executor.getExecutor().getQueue().remainingCapacity())
				.tags(tags)
				.description(ThreadPoolIndicatorEnum.QUEUE_REMAINING_CAPACITY.getDesc())
				.register(meterRegistry);
	}

	/**
	 * 注册 Counter 指标（累计值，单调递增）
	 */
	private void registerCounterMetrics(DynamicThreadPoolExecutor dynamicExecutor, Tags tags) {

		ThreadPoolExecutor executor = dynamicExecutor.getExecutor();

		// 任务总数
		FunctionCounter.builder(ThreadPoolIndicatorEnum.TASK_COUNT.getIndicatorName(),
						executor, ThreadPoolExecutor::getTaskCount)
				.tags(tags)
				.description(ThreadPoolIndicatorEnum.TASK_COUNT.getDesc())
				.register(meterRegistry);

		// 拒绝任务总数
		FunctionCounter.builder(ThreadPoolIndicatorEnum.REJECT_COUNT_TOTAL.getIndicatorName(),
						dynamicExecutor, DynamicThreadPoolExecutor::getRejectedCount)
				.tags(tags)
				.description(ThreadPoolIndicatorEnum.REJECT_COUNT_TOTAL.getDesc())
				.register(meterRegistry);

//		// 执行异常总数
//		FunctionCounter.builder(ThreadPoolIndicatorEnum.EXCEPTION_COUNT_TOTAL.getIndicatorName(),
//						dynamicExecutor, DynamicThreadPoolExecutor::getExceptionCount)
//				.tags(tags)
//				.description(ThreadPoolIndicatorEnum.EXCEPTION_COUNT_TOTAL.getDesc())
//				.register(meterRegistry);

		// 完成任务总数
		FunctionCounter.builder(ThreadPoolIndicatorEnum.COMPLETED_TASK_TOTAL.getIndicatorName(),
						executor, ThreadPoolExecutor::getCompletedTaskCount)
				.tags(tags)
				.description(ThreadPoolIndicatorEnum.COMPLETED_TASK_TOTAL.getDesc())
				.register(meterRegistry);
	}

	@Override
	public void bindTo(MeterRegistry registry) {

	}

	@PreDestroy
	public void stopMonitor() {
		log.info("线程池监控采集器已关闭");
	}
}
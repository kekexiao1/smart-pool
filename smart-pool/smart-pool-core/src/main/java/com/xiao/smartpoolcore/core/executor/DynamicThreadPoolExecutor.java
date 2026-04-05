package com.xiao.smartpoolcore.core.executor;

import com.xiao.smartpoolcore.common.util.ParseUtil;
import com.xiao.smartpoolcore.config.DynamicCapacityBlockingQueue;
import com.xiao.smartpoolcore.config.NamedThreadFactory;
import com.xiao.smartpoolcore.core.task.MonitorablePoolTask;
import com.xiao.smartpoolcore.model.dto.ThreadPoolConfig;
import lombok.extern.slf4j.Slf4j;

import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
/**
 * 动态线程池执行器
 * 支持动态调整线程池参数
 */
@Slf4j
public class DynamicThreadPoolExecutor{

	// 动态线程池配置类
	private ThreadPoolConfig config;
	// 原生线程池类
	private final ThreadPoolExecutor executor;

	public DynamicThreadPoolExecutor(ThreadPoolConfig config) {
		this.config = config;

		// 创建动态阻塞队列
		DynamicCapacityBlockingQueue<Runnable> queue =  new DynamicCapacityBlockingQueue(config.getQueueCapacity());

		RejectedExecutionHandler rejectedExecutionHandler = ParseUtil.parseRejectedHandler(config.getRejectedHandlerClass());

		// 创建执行器
		this.executor = new ThreadPoolExecutor(
				config.getCorePoolSize(),
				config.getMaximumPoolSize(),
				config.getKeepAliveTime(),
				config.getUnit(),
				queue,
				new NamedThreadFactory(config.getThreadPoolName()),
				rejectedExecutionHandler
		);
	}

	/**
	 * 执行任务
	 * @param task 实际任务
	 * @param taskId 任务唯一标识
	 * @param taskType 任务类型/业务码
	 * @param payload 任务参数(JSON格式)
	 */
	public void execute(Runnable task, String taskId, String taskType, String businessType,String payload) {
		long submitTimeNanos = System.nanoTime();

		// 创建可监控的任务，集成时间监控功能
		MonitorablePoolTask monitorableTask = new MonitorablePoolTask(
			task, taskId, taskType, businessType, payload, getThreadPoolName(), submitTimeNanos
		);

		try {
			executor.execute(monitorableTask);
		} catch (RejectedExecutionException e) {
			throw new RuntimeException("AbortPolicy抛出异常");
		}
	}

	/**
	 * 获取executor配置
	 * @return
	 */
	public ThreadPoolConfig getConfig() {
		return new ThreadPoolConfig(this.config);
	}


	/**
	 * 返回Executor
	 */
	public ThreadPoolExecutor getExecutor(){
		return this.executor;
	}

	/**
	 *
	 * @param newConfig
	 */
	public void setConfig(ThreadPoolConfig newConfig) {
		if(config==null){
			log.error("设置的config不能为空");
			return;
		}
		this.config = newConfig;
	}

	public String getThreadPoolName() {
		return this.config.getThreadPoolName();
	}


//	/**
//	 * 返回当前拒绝策略
//	 * @return
//	 */
//	public AtomicReference getCurrentRejectPolicy(){
//		return this.currentRejectPolicy;
//	}


	public boolean isShutDown() {
		return executor.isShutdown();
	}

	/**
	 * 关闭线程池
	 */
	public void shutDown() {
		executor.shutdown();
		try {
			if (!executor.awaitTermination(10, TimeUnit.SECONDS)) {
				executor.shutdownNow();
				log.warn("线程池 [{}] 强制关闭", getThreadPoolName());
			} else {
				log.info("线程池 [{}] 正常关闭", getThreadPoolName());
			}
		} catch (InterruptedException e) {
			executor.shutdownNow();
			Thread.currentThread().interrupt();
		}
	}

	// ============================== 监控指标获取 ==============================

	/**
	 * 活跃线程数
	 * @return
	 */
	public int getActiveCount() {
		return executor.getActiveCount();
	}

	/**
	 * 等待执行的任务数量，队列长度
	 * @return
	 */
	public int getQueueSize() {
		return executor.getQueue().size();
	}

//	/**
//	 * 拒绝任务总数
//	 * @return
//	 */
//	public long getRejectedCount() {
//		return rejectedCount.get();
//	}

	/**
	 * 完成任务总数
	 * @return
	 */
	public long getCompletedTaskCount() {
		return executor.getCompletedTaskCount();
	}

	/**
	 * 当前线程池中实际存在的线程总数
	 * @return
	 */
	public int getPoolSize() {
		return executor.getPoolSize();
	}

	/**
	 * 剩余队列长度
	 * @return
	 */
	public int getQueueRemainingCapacity(){
		return this.config.getQueueCapacity() - executor.getQueue().size();
	}

	/**
	 *  活跃线程占比 = 活跃线程数 / 当前线程总数
	 * @return
	 */
	public double getActiveThreadRate(){
		int poolSize = getPoolSize();  // 当前线程总数
		if (poolSize == 0) {
			return 0.0;  // 避免除零错误
		}
		return (double)getActiveCount() / poolSize;  // 活跃线程数 / 当前线程总数
	}
}

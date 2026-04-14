package com.xiao.smartpoolcore.reject;

import com.xiao.smartpoolcore.config.NamedThreadFactory;
import com.xiao.smartpoolcore.core.task.PoolTask;
import lombok.extern.slf4j.Slf4j;
import java.util.concurrent.RejectedExecutionHandler;
import java.util.concurrent.ThreadPoolExecutor;
/**
 * 抽象兜底拒绝策略：定义通用能力
 */
@Slf4j
public abstract class AbstractRejectPolicy implements RejectedExecutionHandler {

//	public AtomicLong rejectedCount=new AtomicLong(0);

	@Override
	public void rejectedExecution(Runnable task, ThreadPoolExecutor executor){
		// 1. 记录拒绝任务信息
		logRejectionInfo(task, executor);
		
		// 2. 尝试最后一次提交（线程池可能刚释放资源）
		if(!executor.isShutdown() && executor.getQueue().remainingCapacity() > 0){
			try{
				executor.execute(task);
				log.info("任务重新提交成功，线程池: {}", getThreadPoolName(executor));
				return;
			}catch (Exception e){
				log.warn("任务重新提交失败，线程池: {}, 错误: {}", getThreadPoolName(executor), e.getMessage());
			}
		}

		// 3. 提交失败，执行兜底逻辑
		doReject(task, executor);
	}

	/**
	 * 记录拒绝任务信息
	 */
	protected void logRejectionInfo(Runnable task, ThreadPoolExecutor executor) {
		try {
			String threadPoolName = getThreadPoolName(executor);
			String taskInfo = getTaskInfo(task);
			
			log.warn("线程池 [{}] 拒绝任务 - 活跃线程: {}/{}, 队列大小: {}/{}, 任务信息: {}",
				threadPoolName,
				executor.getActiveCount(),
				executor.getMaximumPoolSize(),
				executor.getQueue().size(),
				executor.getQueue().remainingCapacity() + executor.getQueue().size(),
				taskInfo);
			
			// 记录指标
			recordRejectionMetrics(threadPoolName);
		} catch (Exception e) {
			log.error("记录拒绝任务信息失败: {}", e.getMessage());
		}
	}

	/**
	 * 获取线程池名称
	 */
	protected String getThreadPoolName(ThreadPoolExecutor executor) {
		if(executor.getThreadFactory() instanceof NamedThreadFactory){
			String threadPoolName = ((NamedThreadFactory) executor.getThreadFactory()).getThreadPoolName();
			return threadPoolName;
		}
		return "unknown-thread-pool";
	}

	/**
	 * 获取任务信息
	 */
	protected String getTaskInfo(Runnable task) {
		try {
			if (task instanceof PoolTask) {
				PoolTask poolTask = (PoolTask) task;
				long age = System.currentTimeMillis() - poolTask.getCreateTime();
				return String.format("PoolTask[taskId=%s, taskType=%s, priority=%d, age=%,dms, retry=%d/%d]",
					poolTask.getTaskId(), poolTask.getTaskType(), poolTask.getPriority(), age, 
					poolTask.getRetryCount().get(), poolTask.getMaxRetries());
			}
			return task.getClass().getSimpleName();
		} catch (Exception e) {
			return "UnknownTask";
		}
	}

	/**
	 * 记录拒绝指标
	 */
	protected void recordRejectionMetrics(String threadPoolName) {
		// 这里可以集成到指标系统中
		// Metrics.recordRejection(threadPoolName);
	}

	/**
	 * 兜底逻辑：子类实现（Redis/MQ）
	 */
	protected abstract void doReject(Runnable task, ThreadPoolExecutor executor);

}

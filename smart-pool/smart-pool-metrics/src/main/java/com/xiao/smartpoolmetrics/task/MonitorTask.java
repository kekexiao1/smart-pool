package com.xiao.smartpoolmetrics.task;

import com.xiao.smartpoolmetrics.monitor.ThreadPoolMonitor;
import lombok.Data;
import io.micrometer.core.instrument.Timer;
import lombok.extern.slf4j.Slf4j;
import java.util.concurrent.TimeUnit;

@Data
@Slf4j
public class MonitorTask implements Runnable{

	private String poolName;

	private Runnable originalTask;
	/**
	 * 提交开始时间
	 */
	private long submitTimeNanos;

	/**
	 * 执行耗时
	 */
	private long executionTime;

	public MonitorTask(String poolName, Runnable task){
		this.originalTask=task;
		this.poolName=poolName;
		this.submitTimeNanos = System.nanoTime(); // 记录入队时刻
	}


	@Override
	public void run() {
		long startExecuteNanos  = System.nanoTime();
		long waitTime = startExecuteNanos - submitTimeNanos;
		getWaitTimer(poolName).record(waitTime, TimeUnit.NANOSECONDS);
		try{
			originalTask.run();
		}finally {
			long endExecuteNanos = System.nanoTime();
			long executeNanos  = endExecuteNanos - startExecuteNanos;
			getExecuteTimer(poolName).record(executeNanos, TimeUnit.NANOSECONDS);
		}

	}

	private Timer getWaitTimer(String poolName){
		Timer timer = ThreadPoolMonitor.WAIT_TIMER_CACHE.get(poolName);
		if(timer==null){
			log.error("获取等待Timer指标失败");
		}
		return timer;
	}

	private Timer getExecuteTimer(String poolName){
		Timer timer = ThreadPoolMonitor.EXECUTE_TIMER_CACHE.get(poolName);
		if(timer==null){
			log.error("获取执行Timer指标失败");
		}
		return timer;
	}

}

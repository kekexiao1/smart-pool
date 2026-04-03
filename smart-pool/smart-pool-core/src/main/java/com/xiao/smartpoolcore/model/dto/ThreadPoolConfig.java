package com.xiao.smartpoolcore.model.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.util.concurrent.TimeUnit;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ThreadPoolConfig{

	public String getThreadPoolName() {
		return threadPoolName;
	}

	public int getCorePoolSize() {
		return corePoolSize;
	}

	public int getMaximumPoolSize() {
		return maximumPoolSize;
	}

	public long getKeepAliveTime() {
		return keepAliveTime;
	}

	public TimeUnit getUnit() {
		return unit;
	}

	public int getQueueCapacity() {
		return queueCapacity;
	}

	public String getRejectedHandlerClass() {
		return rejectedHandlerClass;
	}


	// 线程池名字
	private String threadPoolName;
	// 核心线程数
	private int corePoolSize;
	// 最大线程数
	private int maximumPoolSize;
	// 存活时间
	private long keepAliveTime=60;
	// 时间单位
	private TimeUnit unit = TimeUnit.SECONDS;
	// 队列容量
	private int queueCapacity;
	// 拒绝策略类名（全限定名）
	private String rejectedHandlerClass= "CallerRunsPolicy";

	/**
	 * 深拷贝构造方法
	 * @param source 要拷贝的源对象
	 */
	public ThreadPoolConfig(ThreadPoolConfig source) {
		if (source == null) {
			throw new IllegalArgumentException("Source config cannot be null");
		}
		this.threadPoolName=source.threadPoolName;
		this.corePoolSize = source.corePoolSize;
		this.maximumPoolSize = source.maximumPoolSize;
		this.keepAliveTime = source.keepAliveTime;
		this.queueCapacity = source.queueCapacity;
		this.rejectedHandlerClass = source.rejectedHandlerClass; // String 不可变，可共享
		// TimeUnit 是枚举，也是不可变的，直接引用即可
		this.unit = source.unit;
	}

	@Override
	public String toString() {
		return
				 threadPoolName + ":" + "\n    " +
						"corePoolSize: " + corePoolSize + "\n    " +
						"maximumPoolSize: " + maximumPoolSize + "\n    " +
						"keepAliveTime: " + keepAliveTime + "\n    " +
						"unit: " + unit.name() + "\n    " +
						"queueCapacity: " + queueCapacity + "\n    " +
						"rejectedHandlerClass: " + rejectedHandlerClass;
	}


}

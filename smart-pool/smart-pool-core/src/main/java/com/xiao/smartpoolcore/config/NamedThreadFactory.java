package com.xiao.smartpoolcore.config;


import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicInteger;

public class NamedThreadFactory implements ThreadFactory {

	private final String threadPoolName;
	private final AtomicInteger threadNumber = new AtomicInteger(1);

	public NamedThreadFactory(String threadPoolName){
		this.threadPoolName=threadPoolName;
	}

	@Override
	public Thread newThread(Runnable r) {
		Thread thread = new Thread(r, threadPoolName + "-thread" + "-" + threadNumber.getAndIncrement());
		thread.setDaemon(true);	// 守护线程，避免阻止 JVM 退出
		thread.setPriority(Thread.NORM_PRIORITY);
		return thread;
	}

	public String getThreadPoolName(){
		return this.threadPoolName;
	}
}

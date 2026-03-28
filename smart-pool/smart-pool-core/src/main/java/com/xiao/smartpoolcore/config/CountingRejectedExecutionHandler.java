package com.xiao.smartpoolcore.config;

import lombok.extern.slf4j.Slf4j;

import java.util.concurrent.RejectedExecutionHandler;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.atomic.AtomicLong;


@Slf4j
public class CountingRejectedExecutionHandler implements RejectedExecutionHandler {

	private final RejectedExecutionHandler handler;

	private final AtomicLong rejectedCount=new AtomicLong(0);


	public CountingRejectedExecutionHandler(RejectedExecutionHandler handler){
		this.handler=handler;
	}

	@Override
	public void rejectedExecution(Runnable r, ThreadPoolExecutor executor) {
		rejectedCount.incrementAndGet();
		handler.rejectedExecution(r, executor);
	}

	public void setRejectedCount(long count){
		this.rejectedCount.set(count);
	}

	public long getRejectedCount(){
		return rejectedCount.get();
	}
}

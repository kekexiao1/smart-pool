package com.xiao.smartpoolcore.config;

import lombok.extern.slf4j.Slf4j;
import java.util.concurrent.LinkedBlockingQueue;


/**
 * 动态容量阻塞队列
 * 用于动态调整线程池的队列容量
 */
@Slf4j
public class DynamicCapacityBlockingQueue extends LinkedBlockingQueue<Runnable> {

	// 新容量
	private volatile int capacity;

	public DynamicCapacityBlockingQueue(int capacity) {
		// 让父类认为容量是无限大的，这样父类的 offer/put 永远不会因为容量不足而拒绝
		super(Integer.MAX_VALUE);
		this.capacity=capacity;
	}

	/**
	 * 设置队列长度,不需要移动队列内任务
	 * @param newCapacity
	 */
	public void setCapacity(int newCapacity){
		this.capacity=newCapacity;
	}

	/**
	 * 重写 offer 方法（非阻塞）
	 * 只有当 size < capacity 时才允许入队
	 */
	@Override
	public boolean offer(Runnable o) {
		if (size() >= capacity) {
			return false;
		}

		return super.offer(o);
	}

	/**
	 * 重写Put方法（阻塞）
	 * 只有当 size < capacity 时才允许入队
	 * @param o the element to add
	 * @throws InterruptedException
	 */
	@Override
	public void put(Runnable o) throws InterruptedException {
		if(size() >= this.capacity){
			// 队列已满
			if(!offer(o)){
				throw new IllegalStateException("队列已满");
			}
		}
		if (!offer(o)) {
			throw new IllegalStateException("队列已满");
		}
	}

	/**
	 * 剩余容量
	 * @return
	 */
	@Override
	public int remainingCapacity() {
		return this.capacity - size();
	}
}

package com.xiao.smartpoolcore.config;

import com.xiao.smartpoolcore.core.task.MonitorablePoolTask;
import lombok.extern.slf4j.Slf4j;
import org.checkerframework.checker.units.qual.A;

import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;


/**
 * 动态容量阻塞队列
 * 用于动态调整线程池的队列容量
 */
@Slf4j
public class DynamicCapacityBlockingQueue<E> extends LinkedBlockingQueue<E> {

	// 新容量
	private volatile int capacity;

	// 总队列内等待时间
	private final AtomicLong totalWaitTime=new AtomicLong(0);
	// 总出队任务数
	private final AtomicLong completedTaskCount = new AtomicLong(0);

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
		if(newCapacity < 1){
			log.error("队列大小不能小于1");
			return;
		}
		this.capacity=newCapacity;
	}

	/**
	 * 重写 offer 方法（非阻塞）
	 * 只有当 size < capacity 时才允许入队
	 */
	@Override
	public boolean offer(E o) {
		if (size() >= capacity) {
			return false;
		}

		return super.offer(o);
	}

	@Override
	public E poll(){
		E e = super.poll();
		recordMetrics(e);
		return e;
	}

	@Override
	public E poll(long timeout, TimeUnit unit){
		E e = super.poll();
		recordMetrics(e);
		return e;
	}

	@Override
	public E take() throws InterruptedException {
		E e = super.take();
		recordMetrics(e);
		return e;
	}


	/**
	 * 重写Put方法（阻塞）
	 * 只有当 size < capacity 时才允许入队
	 * @param o the element to add
	 * @throws InterruptedException
	 */
	@Override
	public void put(E o) throws InterruptedException {
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

	private void recordMetrics(E e){
		if(e instanceof MonitorablePoolTask){
			MonitorablePoolTask task=(MonitorablePoolTask) e;
			// 入队时间
			long enqueueTimeNanos = task.getSubmitTimeNanos();
			// 出队时间
			long dequeueTimeNanos = System.nanoTime();
			// 计算等待时间（纳秒差值转换为毫秒）
			long waitTimeNanos = dequeueTimeNanos - enqueueTimeNanos;
			long waitTimeMs = waitTimeNanos / 1_000_000;

			totalWaitTime.addAndGet(waitTimeMs);

			completedTaskCount.incrementAndGet();

			// 这里的打印日志或上报监控可以做成异步，避免影响性能
			if(waitTimeMs > 3000){
				log.error("等待时间过长，时间：{}ms", waitTimeMs);
			}
		}
	}

	/**
	 * 获取平均等待时间
	 * @return
	 */
	public long getAvgWaitTime(){
		long total = completedTaskCount.get();

		return total!=0 ? totalWaitTime.get() / total : 0;
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

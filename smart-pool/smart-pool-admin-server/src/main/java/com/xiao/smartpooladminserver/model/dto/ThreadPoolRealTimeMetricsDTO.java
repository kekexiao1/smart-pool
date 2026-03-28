package com.xiao.smartpooladminserver.model.dto;


import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Builder
@Data
@AllArgsConstructor
@NoArgsConstructor
public class ThreadPoolRealTimeMetricsDTO {
	private int activeCount;               // 活跃线程数
	private int queueSize;                 // 队列积压数
	private int currentPoolSize;           // 当前线程数
	private int queueRemainingCapacity;    // 队列剩余容量
	private double activeThreadRate;          // 活跃线程占比（%）
}

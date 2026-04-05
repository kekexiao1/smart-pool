package com.xiao.smartpooladminserver.model.dto;


import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class ThreadPoolAccumulateMetricsDTO {

	private long rejectCount;        // 拒绝任务总数
	private long avgWaitTime;		// 任务平均等待时间
	private long completedTaskCount; // 完成任务总数
}

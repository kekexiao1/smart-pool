package com.xiao.smartpooladminserver.model.vo;

import com.xiao.smartpooladminserver.model.dto.ThreadPoolAccumulateMetricsDTO;
import com.xiao.smartpooladminserver.model.dto.ThreadPoolRealTimeMetricsDTO;
import com.xiao.smartpoolcore.model.dto.ThreadPoolConfig;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class ThreadPoolMetricsVO {
	/**
	 * 线程池名称（唯一标识）
	 */
	private String threadPoolName;

	/**
	 * 基础配置
	 */
	private ThreadPoolConfig config;

	/**
	 * 实时指标
	 */
	private ThreadPoolRealTimeMetricsDTO realTimeMetrics;

	/**
	 * 累计指标
	 */
	private ThreadPoolAccumulateMetricsDTO accumulateMetrics;

}

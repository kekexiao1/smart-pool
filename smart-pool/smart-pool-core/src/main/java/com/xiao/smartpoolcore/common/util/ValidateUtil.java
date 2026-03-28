package com.xiao.smartpoolcore.common.util;

import com.xiao.smartpoolcore.model.dto.ThreadPoolConfig;

public class ValidateUtil {

	public static void validateConfig( ThreadPoolConfig config) {
		if (config.getCorePoolSize() < 0) {
			throw new IllegalArgumentException("核心线程数不能小于0");
		}
		if (config.getMaximumPoolSize() < config.getCorePoolSize()) {
			throw new IllegalArgumentException("最大线程数不能小于核心线程数");
		}
		if (config.getQueueCapacity() < 0) {
			throw new IllegalArgumentException("队列容量不能小于0");
		}
		if (config.getKeepAliveTime() < 0) {
			throw new IllegalArgumentException("过期时间不能小于0");
		}
	}
}

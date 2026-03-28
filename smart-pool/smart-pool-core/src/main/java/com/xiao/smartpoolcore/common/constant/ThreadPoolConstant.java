package com.xiao.smartpoolcore.common.constant;

public class ThreadPoolConstant {


	public static final String LOCAL="LOCAL";
	public static final String NACOS="NACOS";

	// 默认配置常量
	public static final int DEFAULT_CORE_POOL_SIZE = 4;
	public static final int DEFAULT_MAXIMUM_POOL_SIZE = 8;
	public static final long DEFAULT_KEEP_ALIVE_TIME = 60L;
	public static final int DEFAULT_QUEUE_CAPACITY = 1024;
	public static final String DEFAULT_REJECTED_HANDLER = "AbortPolicy";
}

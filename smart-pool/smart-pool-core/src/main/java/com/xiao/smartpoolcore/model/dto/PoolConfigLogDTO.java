package com.xiao.smartpoolcore.model.dto;

import lombok.Data;

@Data
public class PoolConfigLogDTO {
	/**
	 * 应用名称，微服务用
	 */
	private String appName;

	/**
	 * 线程池名称
	 */
	private String poolName;

	/**
	 * 旧配置，JSON格式
	 */
	private String oldConfig;

	/**
	 * 新配置，JSON格式
	 */
	private String newConfig;

	/**
	 * 变更类型，暂时只有INIT,UPDATE
	 */
	private String changeType;

	/**
	 * 配置变更人
	 */
	private String operator;

	/**
	 * 配置变更源
	 */
	private String source;

}

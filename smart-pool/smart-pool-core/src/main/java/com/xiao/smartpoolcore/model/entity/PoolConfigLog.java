package com.xiao.smartpoolcore.model.entity;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class PoolConfigLog {
	/**
	 * 主键
	 */
	private Long id;

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
	 * 源头，Nacos, Web
	 */
	private String source;

	/**
	 * 配置变更时间
	 */
	private LocalDateTime createTime;
}

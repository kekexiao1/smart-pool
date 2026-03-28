package com.xiao.smartpoolalert.model.dto;

import lombok.Data;

@Data
public class PoolAlertLogDTO {
	/**
	 * 应用名称，微服务用
	 */
	private String appName;

	/**
	 * 线程池名称
	 */
	private String poolName;

	/**
	 * 告警类型
	 */
	private String alertType;

	/**
	 * 告警级别
	 */
	private String alertLevel;

	/**
	 * 告警详情
	 */
	private String content;

	/**
	 * 告警信息状态，0：未处理，1：已处理
	 */
	private Integer status;

	/**
	 * 处理人
	 */
	private String handler;
}
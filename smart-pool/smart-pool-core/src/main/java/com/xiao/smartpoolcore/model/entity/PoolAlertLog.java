package com.xiao.smartpoolcore.model.entity;


import lombok.Data;

import java.time.LocalDateTime;
import java.util.Date;

@Data
public class PoolAlertLog {
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
	/**
	 * 处理时间
	 */
	private LocalDateTime handleTime;

	/**
	 * 告警发生时间
	 */
	private LocalDateTime createTime;
}


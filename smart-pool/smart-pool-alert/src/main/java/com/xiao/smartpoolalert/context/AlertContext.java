package com.xiao.smartpoolalert.context;

import com.xiao.smartpoolalert.rule.AlertRule;
import lombok.Data;

import java.util.Queue;

@Data
public class AlertContext {

	/** 应用名称 */
	private String appName;

	/** 线程池名称 */
	private String poolName;

	/** 最近一次告警时间 */
	private volatile long lastAlertTime = 0;

	/** 最近一次数据更新时间 */
	private volatile long lastUpdateTime = 0;

	/** 最近 N 次采样值（滑动窗口），带时间戳 */
	private Queue<TimestampedValue> recentValues;

	/** 当前告警项规则 */
	private AlertRule rule;
}

package com.xiao.smartpoolalert.rule;

import com.xiao.smartpoolalert.constant.AlertLevel;
import com.xiao.smartpoolalert.constant.AlertType;
import lombok.Data;

@Data
public class AlertRule {
	/** 告警类型 */
	private AlertType type;

	/** 告警级别 */
	private AlertLevel level = AlertLevel.WARNING;

	/** 阈值，比如队列使用率 80 */
	private int threshold;

	/** 在一个 period 内达到阈值多少次才触发告警 */
	private int count;

	/** 统计时间窗口（秒） */
	private int period;

	/** 告警后静默时间（秒） */
	private int silencePeriod = 300;

	/** 是否开启 */
	private boolean enabled = true;
}

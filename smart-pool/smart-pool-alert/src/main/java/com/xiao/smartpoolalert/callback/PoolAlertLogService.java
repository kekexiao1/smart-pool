package com.xiao.smartpoolalert.callback;

import com.xiao.smartpoolcore.model.entity.PoolAlertLog;

public interface PoolAlertLogService {

	/**
	 * 记录告警日志
	 */
	void logAlert(PoolAlertLog poolAlertLog);
	
	/**
	 * 记录告警处理日志
	 */
	void logAlertHandle(PoolAlertLog poolAlertLog);
}

package com.xiao.smartpoolalert.callback.impl;

import com.xiao.smartpoolalert.callback.PoolAlertLogService;
import com.xiao.smartpoolcore.model.entity.PoolAlertLog;

/**
 * 空实现，防止外部服务不导入告警日志服务而报错，接口无实现
 */
public class NoPoolAlertLogServiceImpl implements PoolAlertLogService {

	@Override
	public void logAlert(PoolAlertLog poolAlertLog) {

	}

	@Override
	public void logAlertHandle(PoolAlertLog poolAlertLog) {

	}
}

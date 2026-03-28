package com.xiao.smartpoolcore.callback.impl;

import com.xiao.smartpoolcore.callback.PoolLogService;
import com.xiao.smartpoolcore.model.dto.PoolConfigLogDTO;
import com.xiao.smartpoolcore.model.entity.PoolConfigLog;


/**
 * 空实现，防止外部服务不导入日志开启日志服务而报错，接口无实现
 */
public class NoPoolLogServiceImpl implements PoolLogService {

	@Override
	public void logConfigChange(PoolConfigLog poolConfigLog) {

	}

	@Override
	public void logConfigInit(PoolConfigLogDTO poolConfigLogDTO) {

	}

	@Override
	public void logConfigUpdate(PoolConfigLogDTO poolConfigLogDTO) {

	}

}

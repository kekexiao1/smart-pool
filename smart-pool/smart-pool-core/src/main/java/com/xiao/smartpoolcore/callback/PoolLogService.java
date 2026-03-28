package com.xiao.smartpoolcore.callback;

import com.xiao.smartpoolcore.model.dto.PoolConfigLogDTO;
import com.xiao.smartpoolcore.model.entity.PoolConfigLog;

public interface PoolLogService {

	/**
	 * 记录线程池配置变更日志
	 */
	void logConfigChange(PoolConfigLog poolConfigLog);
	
	/**
	 * 记录线程池初始化配置日志
	 */
	void logConfigInit(PoolConfigLogDTO poolConfigLogDTO);
	
	/**
	 * 记录线程池配置更新日志
	 */
	void logConfigUpdate(PoolConfigLogDTO poolConfigLogDTO);
}

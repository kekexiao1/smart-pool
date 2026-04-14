package com.xiao.smartpoolcore.listener;

public interface ConfigSourceListener {
	/**
	 * 开始监听
	 */
	void startListening();

	/**
	 * 停止监听
	 */
	void stopListening();

	/**
	 * 获取配置源类型
	 */
	String getSourceType();
}

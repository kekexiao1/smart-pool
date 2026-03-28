package com.xiao.smartpoolalert.constant;


public enum AlertType {
	CHANGE,              // 配置变更
	CAPACITY,            // 队列容量
	LIVENESS,            // 活跃度
	REJECT,              // 拒绝策略
	RUN_TIMEOUT,         // 执行超时
	QUEUE_TIMEOUT,       // 排队超时
	REJECT_POLICY_CHANGE // 拒绝策略变更

}


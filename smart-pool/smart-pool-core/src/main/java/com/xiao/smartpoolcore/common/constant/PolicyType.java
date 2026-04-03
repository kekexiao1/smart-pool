package com.xiao.smartpoolcore.common.constant;

/**
 * 拒绝策略类型枚举
 */
public enum PolicyType {

	REDIS("Redis", "Redis持久化策略"),
	MQ("Mq", "消息队列策略"),
	LOCAL_DISK("LocalDisk", "本地磁盘策略"),
	CALLER_RUNS("CallerRuns", "调用者执行策略"),
	ABORT("Abort", "中止策略"),
	DISCARD("Discard", "丢弃策略"),
	DISCARD_OLDEST("DiscardOldest", "丢弃最老策略");

	private final String code;
	private final String description;

	PolicyType(String code, String description) {
		this.code = code;
		this.description = description;
	}

	public String getCode() {
		return code;
	}

	public String getDescription() {
		return description;
	}

	public static PolicyType fromCode(String code) {
		for (PolicyType type : values()) {
			if (type.code.equalsIgnoreCase(code)) {
				return type;
			}
		}
		return ABORT; // 默认使用中止策略
	}
}

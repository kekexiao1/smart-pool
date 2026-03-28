package com.xiao.smartpoolcore.common.constant;

public enum ChangeConfigType {

	INIT("INIT", "配置初始化"),
	UPDATE("UPDATE", "更新配置");

	private String type;
	private String desc;

	ChangeConfigType(String type, String desc) {
		this.type = type;
		this.desc = desc;
	}

	public String getType() {
		return type;
	}

	public String getDesc() {
		return desc;
	}
}

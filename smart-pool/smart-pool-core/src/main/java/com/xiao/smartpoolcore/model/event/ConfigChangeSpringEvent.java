package com.xiao.smartpoolcore.model.event;

import org.springframework.context.ApplicationEvent;

public class ConfigChangeSpringEvent extends ApplicationEvent {

	private final ConfigChangeEvent configChangeEvent;

	public ConfigChangeSpringEvent(Object source, ConfigChangeEvent configChangeEvent) {
		super(source);
		this.configChangeEvent = configChangeEvent;
	}

	public ConfigChangeEvent getConfigChangeEvent() {
		return configChangeEvent;
	}
}

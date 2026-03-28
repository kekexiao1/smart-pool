package com.xiao.smartpoolcore.listener;

import com.xiao.smartpoolcore.core.handler.ConfigChangeHandler;
import com.xiao.smartpoolcore.model.event.ConfigChangeEvent;
import com.xiao.smartpoolcore.model.event.ConfigChangeSpringEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(prefix = "spring.cloud.nacos.config", name = "server-addr")
public class ConfigChangeSpringEventListener {

	private final ConfigChangeHandler configChangeHandler;

	@Async
	@EventListener
	public void handleConfigChangeEvent(ConfigChangeSpringEvent event) {
		ConfigChangeEvent configChangeEvent = event.getConfigChangeEvent();
		
		log.info("收到配置变更事件: dataId={}, group={}, fromAdmin={}", 
				configChangeEvent.getDataId(), 
				configChangeEvent.getGroup(),
				configChangeEvent.isFromAdmin());
		
		try {
			configChangeHandler.handleConfigUpdate(configChangeEvent);
		} catch (Exception e) {
			log.error("处理配置变更事件失败", e);
		}
	}
}

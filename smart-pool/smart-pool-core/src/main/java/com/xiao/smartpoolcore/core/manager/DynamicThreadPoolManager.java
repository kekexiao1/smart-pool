package com.xiao.smartpoolcore.core.manager;

import com.xiao.smartpoolcore.core.handler.ConfigChangeHandler;
import com.xiao.smartpoolcore.core.registry.ThreadPoolRegistry;
import com.xiao.smartpoolcore.listener.nacos.NacosConfigListener;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.DisposableBean;

/**
 * 动态线程池管理器
 * 负责线程池的初始化和销毁管理
 */
@RequiredArgsConstructor
public class DynamicThreadPoolManager implements DisposableBean {

	private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(DynamicThreadPoolManager.class);

	private final ThreadPoolRegistry registry;
	private final ConfigChangeHandler configChangeHandler;
	private final NacosConfigListener nacosConfigListener;

	/**
	 * 应用启动后自动初始化线程池配置
	 */
	@PostConstruct
	public void initConfig() {
		log.info("开始初始化动态线程池...");
		
		try {
			// 获取初始配置
			String configInfo = nacosConfigListener.getConfig();
			if (configInfo == null) {
				log.error("初始化获取配置失败");
				return;
			}
			
			// 使用配置变更处理器处理初始化
			configChangeHandler.handleConfigInit(configInfo);
			
			// 启动配置监听
			nacosConfigListener.startListening();
			
			log.info("动态线程池初始化完成");
			
		} catch (Exception e) {
			log.error("动态线程池初始化失败", e);
		}
	}

	/**
	 * 应用关闭时销毁所有线程池
	 */
	@Override
	public void destroy() {
		log.info("开始销毁所有动态线程池...");
		registry.shutDownAll();
		log.info("所有动态线程池已销毁");
	}
}

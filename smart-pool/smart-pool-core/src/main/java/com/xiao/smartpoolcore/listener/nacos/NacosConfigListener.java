package com.xiao.smartpoolcore.listener.nacos;

import com.alibaba.cloud.nacos.NacosConfigManager;
import com.alibaba.nacos.api.config.ConfigService;
import com.alibaba.nacos.api.config.listener.Listener;
import com.alibaba.nacos.api.exception.NacosException;
import com.xiao.smartpoolcore.callback.PoolLogService;
import com.xiao.smartpoolcore.common.constant.PoolLogConstant;
import com.xiao.smartpoolcore.common.constant.ThreadPoolConstant;
import com.xiao.smartpoolcore.core.handler.ConfigChangeHandler;
import com.xiao.smartpoolcore.model.dto.PoolConfigLogDTO;
import com.xiao.smartpoolcore.model.event.ConfigChangeEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.cloud.endpoint.event.RefreshEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.*;

@RequiredArgsConstructor
@ConditionalOnClass(name = "com.alibaba.nacos.api.config.ConfigService")
public class NacosConfigListener implements ConfigSourceListener, InitializingBean, DisposableBean, ApplicationListener<RefreshEvent> {

	private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(NacosConfigListener.class);

	private final ConfigChangeHandler configChangeHandler;

	private ConfigService configService;

	private final NacosConfigManager configManager;

	private final PoolLogService poolLogService;

	private final Map<String, Listener> listenerMap=new ConcurrentHashMap<>();

	private Listener listener;
	private volatile boolean isListening = false;
	// 线程器执行器
	private ExecutorService listenerExecutor;

	@Value("${spring.cloud.nacos.config.server-addr:${spring.cloud.nacos.discovery.server-addr:}}")
	private String serverAddr;

	@Value("${spring.cloud.nacos.config.namespace:${spring.cloud.nacos.discovery.namespace:}}")
	private String namespace;

	@Value("${spring.cloud.nacos.config.data-id:smart-pool.yaml}")
	private String dataId;

	@Value("${spring.cloud.nacos.config.group:DEFAULT_GROUP}")
	private String group;


	/**
	 * 重写InitializingBean内方法
	 * @throws NacosException
	 */
	@Override
	public void afterPropertiesSet(){
		log.info("Nacos配置参数: dataId={}, group={}, serverAddr={}", dataId, group, serverAddr);
		try {
			configService = configManager.getConfigService();

			log.info("Nacos ConfigService初始化成功: serverAddr={}", serverAddr);
		} catch (Exception e) {
			log.error("初始化Nacos ConfigService失败", e);
			throw e;
		}
		// 监听线程池
		listenerExecutor = Executors.newFixedThreadPool(
				2,
				r -> {
					Thread thread = new Thread(r, "nacos-config-listener");
					thread.setDaemon(true);
					return thread;
				}
		);
	}

	/**
	 * 监听
	 * @param event the event to respond to
	 */
	@Override
	public void onApplicationEvent(RefreshEvent event) {
		try{
			String config = configService.getConfig(dataId, group, 3000);
			if(StringUtils.hasText(config)){
				log.info("获取到初始配置: config={}", config);
				listener.receiveConfigInfo(config);
			}
		}catch (Exception e){
			log.warn("获取配置失败", e);
		}
	}

	/**
	 * 获取配置
	 * @return
	 */
	public String getConfig(){
		if(configService==null){
			log.error("ConfigService未初始化，无法获取配置");
			return null;
		}
		
		log.info("正在从Nacos获取配置: dataId={}, group={}, serverAddr={}, namespace={}", 
				 dataId, group, serverAddr, namespace);

		String config = null;
		try {
			config = configService.getConfig(dataId, group, 5000);
		} catch (NacosException e) {
			log.warn("获取nacos配置失效");
			throw new RuntimeException(e);
		}

		if (config == null) {
			log.warn("从Nacos获取配置返回null， dataId={}, group={}",dataId, group);
		} else {
			log.info("从Nacos获取配置成功: dataId={}, group={}, 配置长度={}",
					 dataId, group, config.length());
			return config;
		}
		return null;
	}

	/**
	 * 开启监听
	 */
	@Override
	public void startListening()  {
		if (configService == null) {
			log.error("ConfigService未初始化，无法启动监听");
			return;
		}
		// 避免重复监听
		if(isListening){
			log.warn("配置文件监听中dataId={}, group={}", dataId, group);
			return;
		}
		synchronized (this) {
			// 双重检查
			if (isListening) {
				log.warn("配置文件监听中dataId={}, group={}", dataId, group);
				return;
			}
			try{
				// 添加配置监听
				listener = new Listener() {
					@Override
					public Executor getExecutor() {
						return listenerExecutor;
					}
					@Override
					public void receiveConfigInfo(String configInfo) {

						ConfigChangeEvent event = ConfigChangeEvent.builder()
						.configSource(ThreadPoolConstant.NACOS)
						.newConfig(configInfo)
						.dataId(dataId)
						.group(group)
						.timestamp(System.currentTimeMillis())
						.fromAdmin(false)
						.allUpdate(true)
						.build();
					configChangeHandler.handleConfigUpdate(event);
					}
				};
				configService.addListener(dataId, group, listener);
				isListening=true;
			}catch (Exception e){
				log.error("启动Nacos监听失败", e);
			}
		}
	}

	@Override
	public void stopListening() {
		log.info("正在停止Nacos监听...");

		synchronized (this) {
			if (!isListening) {
				log.info("Nacos监听未启动，无需停止");
				return;
			}
			
			if (configService != null && listener != null) {
				try {
					configService.removeListener(dataId, group, listener);
					log.info("Nacos监听已停止: dataId={}, group={}", dataId, group);
				} catch (Exception e) {
					log.error("停止Nacos监听时发生错误", e);
				}
			}
			isListening = false;
		}
	}

	/**
	 * 配置源类型
	 * @return
	 */
	@Override
	public String getSourceType() {
		return "nacos";
	}

	/**
	 * 销毁时清理资源
	 * @throws Exception
	 */
	@Override
	public void destroy() throws Exception {
		log.info("销毁NacosConfigListener...");
		// 停止监听
		stopListening();

		// 关闭 listenerExecutor 线程池
		if (listenerExecutor != null) {
			log.info("正在关闭 listenerExecutor 线程池...");
			listenerExecutor.shutdown();
			try {
				if (!listenerExecutor.awaitTermination(5, TimeUnit.SECONDS)) {
					listenerExecutor.shutdownNow();
				}
			} catch (InterruptedException e) {
				listenerExecutor.shutdownNow();
				Thread.currentThread().interrupt();
			}
			log.info("listenerExecutor 线程池已关闭");
		}
	}

	public boolean isListening(String threadPoolName) {
		return listenerMap.containsKey(threadPoolName);
	}

	public Set<String> getListeningThreadPools() {
		return new HashSet<>(listenerMap.keySet());
	}

}

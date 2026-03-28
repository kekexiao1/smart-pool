package com.xiao.smartpooladminserver.service.impl;

import com.alibaba.cloud.nacos.NacosConfigManager;
import com.alibaba.nacos.api.config.ConfigType;
import com.alibaba.nacos.api.exception.NacosException;
import com.xiao.smartpooladminserver.model.dto.ThreadPoolConfigDTO;
import com.xiao.smartpooladminserver.model.dto.ThreadPoolConfigTemplateDTO;
import com.xiao.smartpooladminserver.service.ThreadPoolConfigService;
import com.xiao.smartpooladminserver.model.vo.ThreadPoolConfigVO;
import com.xiao.smartpoolcore.callback.PoolLogService;
import com.xiao.smartpoolcore.common.constant.ThreadPoolConstant;
import com.xiao.smartpoolcore.common.util.ParseUtil;
import com.xiao.smartpoolcore.common.util.ValidateUtil;
import com.xiao.smartpoolcore.model.dto.DynamicThreadPoolProperties;
import com.xiao.smartpoolcore.model.dto.ThreadPoolConfig;
import com.xiao.smartpoolcore.model.event.ConfigChangeEvent;
import com.xiao.smartpoolcore.model.event.ConfigChangeSpringEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.yaml.snakeyaml.DumperOptions;
import org.yaml.snakeyaml.Yaml;

import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@RequiredArgsConstructor
@Service
public class ThreadPoolConfigServiceImpl implements ThreadPoolConfigService {

	private final NacosConfigManager nacosConfigManager;

	private final ApplicationEventPublisher eventPublisher;

	@Value("${spring.cloud.nacos.config.data-id:DynamicThreadPoolConfig.yaml}")
	private String dataId;

	@Value("${spring.cloud.nacos.config.group:DEFAULT_GROUP}")
	private String group;

	// 默认配置模板
	private static final Map<String, ThreadPoolConfigTemplateDTO> DEFAULT_TEMPLATES = new HashMap<>();

	static {
		// 高性能模板
		DEFAULT_TEMPLATES.put("high-performance", ThreadPoolConfigTemplateDTO.builder()
				.templateName("high-performance")
				.description("高性能线程池模板")
				.corePoolSize(16)
				.maximumPoolSize(32)
				.keepAliveTime(60L)
				.queueCapacity(1024)
				.rejectedHandler("CallerRunsPolicy")
				.queueWarnThreshold(800)
				.activeThreadRateThreshold(80)
				.build());

		// 平衡模板
		DEFAULT_TEMPLATES.put("balanced", ThreadPoolConfigTemplateDTO.builder()
				.templateName("balanced")
				.description("平衡型线程池模板")
				.corePoolSize(8)
				.maximumPoolSize(16)
				.keepAliveTime(60L)
				.queueCapacity(512)
				.rejectedHandler("CallerRunsPolicy")
				.queueWarnThreshold(400)
				.activeThreadRateThreshold(70)
				.build());

		// 保守模板
		DEFAULT_TEMPLATES.put("conservative", ThreadPoolConfigTemplateDTO.builder()
				.templateName("conservative")
				.description("保守型线程池模板")
				.corePoolSize(4)
				.maximumPoolSize(8)
				.keepAliveTime(60L)
				.queueCapacity(256)
				.rejectedHandler("CallerRunsPolicy")
				.queueWarnThreshold(200)
				.activeThreadRateThreshold(60)
				.build());
	}

	/**
	 * 获取所有线程池配置列表
	 */
	@Override
	public List<ThreadPoolConfigVO> listAllConfigs() {
		try {
			String configContent = nacosConfigManager.getConfigService().getConfig(dataId, group, 3000);
			if (configContent == null || configContent.trim().isEmpty()) {
				return Collections.emptyList();
			}

			DynamicThreadPoolProperties properties = ParseUtil.parseConfig(configContent);
			if (properties.getExecutors() == null) {
				return Collections.emptyList();
			}

			return properties.getExecutors().entrySet().stream()
					.map(entry -> {
						ThreadPoolConfig config = entry.getValue();
						return ThreadPoolConfigVO.builder()
								.threadPoolName(entry.getKey())
								.config(config)
								.rejectedHandler(config.getRejectedHandlerClass())

								.createTime(new Date().toString())
								.updateTime(new Date().toString())
								.build();
					})
					.collect(Collectors.toList());

		} catch (NacosException e) {
			log.error("获取Nacos配置失败", e);
			throw new RuntimeException("获取配置失败: " + e.getMessage(), e);
		}
	}

	/**
	 * 新增线程池配置
	 */
	@Override
	public void addConfig(ThreadPoolConfigDTO configDTO) {
		try {
			String currentConfig = nacosConfigManager.getConfigService().getConfig(dataId, group, 3000);
			DynamicThreadPoolProperties properties;

			if (currentConfig == null || currentConfig.trim().isEmpty()) {
				properties = new DynamicThreadPoolProperties();
				properties.setExecutors(new HashMap<>());
			} else {
				properties = ParseUtil.parseConfig(currentConfig);
				if (properties.getExecutors() == null) {
					properties.setExecutors(new HashMap<>());
				}
			}

			// 检查线程池是否已存在
			if (properties.getExecutors().containsKey(configDTO.getThreadPoolName())) {
				throw new RuntimeException("线程池配置已存在: " + configDTO.getThreadPoolName());
			}

			// 创建新的线程池配置
			ThreadPoolConfig newConfig = new ThreadPoolConfig();
			newConfig.setThreadPoolName(configDTO.getThreadPoolName());
			newConfig.setCorePoolSize(configDTO.getCorePoolSize());
			newConfig.setMaximumPoolSize(configDTO.getMaximumPoolSize());
			newConfig.setKeepAliveTime(configDTO.getKeepAliveTime());
			newConfig.setUnit(configDTO.getUnit());
			newConfig.setQueueCapacity(configDTO.getQueueCapacity());
			newConfig.setRejectedHandlerClass(configDTO.getRejectedHandler());

			// 验证配置参数
			ValidateUtil.validateConfig(newConfig);

			properties.getExecutors().put(configDTO.getThreadPoolName(), newConfig);

			boolean success = notifyAndPublish(properties);

			if (!success) {
				throw new RuntimeException("发布配置到Nacos失败");
			}

			log.info("新增线程池配置成功: {}", configDTO.getThreadPoolName());

		} catch (NacosException e) {
			log.error("新增线程池配置失败", e);
			throw new RuntimeException("新增配置失败: " + e.getMessage(), e);
		}
	}

	/**
	 * 编辑单个线程池配置
	 */
	@Override
	public void updateConfig(ThreadPoolConfigDTO configDTO) {
		try {
			String currentConfig = nacosConfigManager.getConfigService().getConfig(dataId, group, 3000);
			if (currentConfig == null || currentConfig.trim().isEmpty()) {
				throw new RuntimeException("配置不存在");
			}

			DynamicThreadPoolProperties properties = ParseUtil.parseConfig(currentConfig);
			if (properties.getExecutors() == null || !properties.getExecutors().containsKey(configDTO.getThreadPoolName())) {
				throw new RuntimeException("线程池配置不存在: " + configDTO.getThreadPoolName());
			}

			// 新配置
			ThreadPoolConfig updateConfig = ThreadPoolConfig.builder()
					.threadPoolName(configDTO.getThreadPoolName())
					.corePoolSize(configDTO.getCorePoolSize())
					.maximumPoolSize(configDTO.getMaximumPoolSize())
					.queueCapacity(configDTO.getQueueCapacity())
					.keepAliveTime(configDTO.getKeepAliveTime())
					.unit(configDTO.getUnit())
					.rejectedHandlerClass(configDTO.getRejectedHandler()).build();

			// 校验配置
			ValidateUtil.validateConfig(updateConfig);
			properties.getExecutors().put(configDTO.getThreadPoolName(), updateConfig);

			boolean success = notifyAndPublish(properties);
			if (!success) {
				throw new RuntimeException("更新配置到Nacos失败");
			}

			log.info("更新线程池配置成功: {}", configDTO.getThreadPoolName());

		} catch (NacosException e) {
			log.error("更新线程池配置失败", e);
			throw new RuntimeException("更新配置失败: " + e.getMessage(), e);
		}
	}

	/**
	 * 删除线程池配置
	 */
	@Override
	public void deleteConfig(String threadPoolName) {
		try {
			String currentConfig = nacosConfigManager.getConfigService().getConfig(dataId, group, 3000);
			if (currentConfig == null || currentConfig.trim().isEmpty()) {
				throw new RuntimeException("配置不存在");
			}

			DynamicThreadPoolProperties properties = ParseUtil.parseConfig(currentConfig);
			if (properties.getExecutors() == null || !properties.getExecutors().containsKey(threadPoolName)) {
				throw new RuntimeException("线程池配置不存在: " + threadPoolName);
			}

			properties.getExecutors().remove(threadPoolName);

			// 通知变更，发布到nacos
			boolean success = notifyAndPublish(properties);
			if (!success) {
				throw new RuntimeException("删除配置失败");

			}

			log.info("删除线程池配置成功: {}", threadPoolName);

		} catch (NacosException e) {
			log.error("删除线程池配置失败", e);
			throw new RuntimeException("删除配置失败: " + e.getMessage(), e);
		}
	}

	public boolean notifyAndPublish(DynamicThreadPoolProperties properties) throws NacosException {
		String newConfigContent = convertToYaml(properties);
		
		ConfigChangeEvent event = ConfigChangeEvent.builder()
				.configSource(ThreadPoolConstant.LOCAL)
				.dataId(dataId)
				.group(group)
				.newConfig(newConfigContent)
				.fromAdmin(true)
				.allUpdate(true)
				.build();
		
		ConfigChangeSpringEvent springEvent = new ConfigChangeSpringEvent(this, event);
		eventPublisher.publishEvent(springEvent);

		return nacosConfigManager.getConfigService().publishConfig(dataId, group, newConfigContent, ConfigType.YAML.getType());
	}

	/**
	 * 获取所有配置模板
	 */
	@Override
	public List<ThreadPoolConfigTemplateDTO> getTemplates() {
		return new ArrayList<>(DEFAULT_TEMPLATES.values());
	}

	/**
	 * 根据模板创建配置DTO
	 */
	@Override
	public ThreadPoolConfigDTO createFromTemplate(String templateName, String applicationName,
												  String environment, String threadPoolName) {
		ThreadPoolConfigTemplateDTO template = DEFAULT_TEMPLATES.get(templateName);
		if (template == null) {
			throw new RuntimeException("模板不存在: " + templateName);
		}

		return ThreadPoolConfigDTO.builder()
				.applicationName(applicationName)
				.environment(environment)
				.threadPoolName(threadPoolName)
				.corePoolSize(template.getCorePoolSize())
				.maximumPoolSize(template.getMaximumPoolSize())
				.keepAliveTime(template.getKeepAliveTime())
				.queueCapacity(template.getQueueCapacity())
				.rejectedHandler(template.getRejectedHandler())
				.queueWarnThreshold(template.getQueueWarnThreshold())
				.activeThreadRateThreshold(template.getActiveThreadRateThreshold())
				.build();
	}

	/**
	 * 将配置对象转换为YAML格式
	 */
	private String convertToYaml(DynamicThreadPoolProperties properties) {
		DumperOptions options = new DumperOptions();
		options.setDefaultFlowStyle(DumperOptions.FlowStyle.BLOCK);
		options.setPrettyFlow(true);
		options.setIndent(2);
		// 设置不输出类标签，避免类型信息出现在YAML中
		options.setExplicitStart(false);
		options.setExplicitEnd(false);

		Yaml yaml = new Yaml(options);

		// 创建一个新的Map，过滤掉threadPoolName字段
		Map<String, Object> root = new HashMap<>();
		Map<String, Object> executorsMap = new HashMap<>();

		if (properties.getExecutors() != null) {
			for (Map.Entry<String, ThreadPoolConfig> entry : properties.getExecutors().entrySet()) {
				ThreadPoolConfig config = entry.getValue();
				
				// 为每个线程池创建新的配置Map
				Map<String, Object> configMap = new LinkedHashMap<>();

				// 注意：不包含 threadPoolName
				configMap.put("corePoolSize", config.getCorePoolSize());
				configMap.put("maximumPoolSize", config.getMaximumPoolSize());
				configMap.put("keepAliveTime", config.getKeepAliveTime());
				configMap.put("unit", config.getUnit().name());
				configMap.put("queueCapacity", config.getQueueCapacity());
				configMap.put("rejectedHandlerClass", config.getRejectedHandlerClass());
				executorsMap.put(entry.getKey(), configMap);
			}
		}
		root.put("executors", executorsMap);
		return yaml.dump(root);
	}
}

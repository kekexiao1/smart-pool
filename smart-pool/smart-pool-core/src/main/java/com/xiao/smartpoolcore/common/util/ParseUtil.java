package com.xiao.smartpoolcore.common.util;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import com.xiao.smartpoolcore.config.CountingRejectedExecutionHandler;
import com.xiao.smartpoolcore.model.dto.DynamicThreadPoolProperties;
import com.xiao.smartpoolcore.model.dto.ThreadPoolConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.RejectedExecutionHandler;
import java.util.concurrent.ThreadPoolExecutor;


public class ParseUtil {

	private static final Logger log = LoggerFactory.getLogger(ParseUtil.class);

	// YAML 解析器
	private static final ObjectMapper YAML_MAPPER = new ObjectMapper(new YAMLFactory());
	static {
		YAML_MAPPER.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
	}

	/**
	 * 解析 YAML 配置字符串
	 * @param configInfo 配置内容
	 * @return 线程池配置对象
	 */
	public static DynamicThreadPoolProperties parseConfig(String configInfo) {
		if (configInfo == null || configInfo.trim().isEmpty()) {
			throw new IllegalArgumentException("配置信息不能为空");
		}
		try {
			return YAML_MAPPER.readValue(configInfo.trim(), DynamicThreadPoolProperties.class);
		} catch (Exception e) {
			throw new RuntimeException("解析 YAML 配置失败: " + e.getMessage(), e);
		}
	}


	/**
	 * 解析拒绝策略
	 * @param handlerClass
	 * @return
	 */
	public static RejectedExecutionHandler parseRejectedHandler(String handlerClass) {

		if (handlerClass == null || handlerClass.trim().isEmpty()) {
			return new ThreadPoolExecutor.AbortPolicy();
		}

		try {
			// 支持简写名称
			RejectedExecutionHandler handler = resolveHandlerClassName(handlerClass);
			// 转成可计算拒绝任务数量的策略
			return new CountingRejectedExecutionHandler(handler);
		} catch (Exception e) {
			log.warn("创建拒绝策略 [{}] 失败，使用默认策略", handlerClass);
			return new ThreadPoolExecutor.AbortPolicy();
		}
	}


	/**
	 * 解析拒绝策略类名（支持简写）
	 */
	private static RejectedExecutionHandler resolveHandlerClassName(String policyName) {
		if (policyName == null || policyName.trim().isEmpty()) {
			throw new IllegalArgumentException("策略类名不能为空");
		}

		// 支持简写名称映射
		switch (policyName) {
			case "AbortPolicy":
			case "ThreadPoolExecutor.AbortPolicy":
			case "java.util.concurrent.ThreadPoolExecutor.AbortPolicy":
				return new ThreadPoolExecutor.AbortPolicy();
			case "CallerRunsPolicy":
			case "ThreadPoolExecutor.CallerRunsPolicy":
			case "java.util.concurrent.ThreadPoolExecutor.CallerRunsPolicy":
				return new ThreadPoolExecutor.CallerRunsPolicy();
			case "DiscardPolicy":
			case "ThreadPoolExecutor.DiscardPolicy":
			case "java.util.concurrent.ThreadPoolExecutor.DiscardPolicy":
				return  new ThreadPoolExecutor.DiscardPolicy();
			case "DiscardOldestPolicy":
			case "ThreadPoolExecutor.DiscardOldestPolicy":
			case "java.util.concurrent.ThreadPoolExecutor.DiscardOldestPolicy":
				return new ThreadPoolExecutor.DiscardOldestPolicy();
			default:
				try{
					Class<?> aClass = Class.forName(policyName);
					return (RejectedExecutionHandler) aClass.getDeclaredConstructor().newInstance();
				}catch (Exception e){
					log.warn("拒绝策略创建失败: {}", policyName, e);
					return null;
				}
		}
	}

}

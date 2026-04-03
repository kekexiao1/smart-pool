package com.xiao.smartpoolcore.common.util;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import com.xiao.smartpoolcore.config.CountingRejectedExecutionHandler;
import com.xiao.smartpoolcore.model.dto.DynamicThreadPoolProperties;
import com.xiao.smartpoolcore.reject.RejectPolicyFactory;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.util.concurrent.RejectedExecutionHandler;
import java.util.concurrent.ThreadPoolExecutor;


@Slf4j
public class ParseUtil {

	private static final ObjectMapper YAML_MAPPER = new ObjectMapper(new YAMLFactory());
	static {
		YAML_MAPPER.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
	}


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
			return new CountingRejectedExecutionHandler(new ThreadPoolExecutor.AbortPolicy());
		}

		try {
			RejectedExecutionHandler handler = createHandler(handlerClass);
			return new CountingRejectedExecutionHandler(handler);
		} catch (Exception e) {
			log.warn("创建拒绝策略 [{}] 失败，使用默认策略", handlerClass);
			return new CountingRejectedExecutionHandler(new ThreadPoolExecutor.AbortPolicy());
		}
	}

	private static RejectedExecutionHandler createHandler(String handlerClass) {
		String normalizedName = normalizeHandlerName(handlerClass);
		
		RejectedExecutionHandler handler = RejectPolicyFactory.createPolicyByName(normalizedName);
		if (handler != null) {
			return handler;
		}

		return createByReflection(handlerClass);
	}

	/**
	 * 标准化名称，与PolicyType对应
	 * @param handlerClass
	 * @return
	 */
	private static String normalizeHandlerName(String handlerClass) {
		if (handlerClass.contains(".")) {
			String simpleName = handlerClass.substring(handlerClass.lastIndexOf('.') + 1);
			return mapSimpleNameToCode(simpleName);
		}
		return mapSimpleNameToCode(handlerClass);
	}

	/**
	 * 拒绝策略映射
	 * @param simpleName
	 * @return
	 */
	private static String mapSimpleNameToCode(String simpleName) {
		switch (simpleName) {
			case "AbortPolicy":
				return "Abort";
			case "CallerRunsPolicy":
				return "CallerRuns";
			case "DiscardPolicy":
				return "Discard";
			case "DiscardOldestPolicy":
				return "DiscardOldest";
			case "LocalDiskRejectPolicy":
			case "LocalDisk":
				return "LocalDisk";
			case "RedisRejectPolicy":
			case "Redis":
				return "Redis";
			case "MQRejectPolicy":
			case "MQ":
			case "Mq":
				return "Mq";
			default:
				return simpleName;
		}
	}

	/**
	 * 根据反射创建拒绝策略
	 * @param className
	 * @return
	 */
	private static RejectedExecutionHandler createByReflection(String className) {
		try {
			Class<?> clazz = Class.forName(className);
			return (RejectedExecutionHandler) clazz.getDeclaredConstructor().newInstance();
		} catch (Exception e) {
			log.warn("通过反射创建拒绝策略失败: {}", className, e);
			return new ThreadPoolExecutor.AbortPolicy();
		}
	}
}

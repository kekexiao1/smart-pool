package com.xiao.smartpoolalert.manager;

import com.xiao.smartpoolalert.config.AlertProperties;
import com.xiao.smartpoolalert.config.ThreadPoolAlertConfig;
import com.xiao.smartpoolalert.notification.AlertNotifier;
import com.xiao.smartpoolalert.notification.DingTalkAlertNotifier;
import com.xiao.smartpoolalert.notification.EmailAlertNotifier;
import com.xiao.smartpoolalert.notification.WeChatAlertNotifier;
import com.xiao.smartpoolalert.rule.AlertRule;
import com.xiao.smartpoolcore.common.util.ApplicationContextHolder;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.SmartInitializingSingleton;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import java.util.List;

@Slf4j
@RequiredArgsConstructor
public class AlertInitializer implements SmartInitializingSingleton {

	private final AlertManager alertManager;
	private final AlertProperties alertProperties;
	private final ThreadPoolAlertConfig threadPoolAlertConfig;
	
	@Value("${spring.application.name:unknown}")
	private String appName;

	@Override
	public void afterSingletonsInstantiated() {
		registerNotifiers();
		registerThreadPoolAlertRules();
		log.info("告警模块初始化完成");
	}

	private void registerNotifiers() {
		// 注册统一配置的通知器
		List<AlertProperties.NotifierConfig> notifierConfigs = alertProperties.getNotifiers();
		for (AlertProperties.NotifierConfig config : notifierConfigs) {
			if (!config.isEnabled()) {
				continue;
			}

			AlertNotifier notifier = createNotifier(config);
			if (notifier != null) {
				// 根据配置的告警类型映射注册通知器
				alertManager.registerNotifier(notifier, config.getAlertTypes());
				log.info("{} 告警通知器已注册，处理告警类型: {}", config.getType(),
						config.getAlertTypes().isEmpty() ? "所有类型" : config.getAlertTypes());
			}
		}
	}

	private AlertNotifier createNotifier(AlertProperties.NotifierConfig config) {
		switch (config.getType().toUpperCase()) {
			case "EMAIL":
				return createEmailNotifier(config);
			case "DING":
				return new DingTalkAlertNotifier(config.getWebhook().getUrl(), config.getWebhook().getSecret());
			case "WECHAT":
				return new WeChatAlertNotifier(config.getWebhook().getUrl());
			default:
				log.warn("未知的通知器类型: {}", config.getType());
				return null;
		}
	}

	private AlertNotifier createEmailNotifier(AlertProperties.NotifierConfig config) {
		try {
			if (config.getEmail() == null) {
				log.warn("EMAIL 通知器配置缺失 email 配置项");
				return null;
			}
			
			if (config.getEmail().getFrom() == null || config.getEmail().getTo() == null) {
				log.warn("EMAIL 通知器配置缺失 from 或 to 邮箱地址 - from: {}, to: {}", 
						config.getEmail().getFrom(), config.getEmail().getTo());
				return null;
			}
			
			JavaMailSender mailSender = ApplicationContextHolder.getBean(JavaMailSender.class);
			if (mailSender == null) {
				log.warn("JavaMailSender bean 未找到，请检查 spring.mail 配置是否正确");
				return null;
			}
			
			log.info("EMAIL 通知器创建成功 - from: {}, to: {}", 
					config.getEmail().getFrom(), config.getEmail().getTo());
			return new EmailAlertNotifier(
					mailSender,
					config.getEmail().getFrom(),
					config.getEmail().getTo()
			);
		} catch (Exception e) {
			log.error("创建 EMAIL 通知器失败: {}", e.getMessage(), e);
			return null;
		}
	}

	private void registerThreadPoolAlertRules() {
		// 注册多线程池告警规则
		if (!threadPoolAlertConfig.getPools().isEmpty()) {
			// 获取默认告警规则
			List<AlertRule> defaultRules = threadPoolAlertConfig.getDefaultConfig().getRules();
			log.info("多线程池告警规则配置");
			// 为每个配置的线程池注册独立的告警规则
			threadPoolAlertConfig.getPools().forEach((poolName, poolConfig) -> {
				if (!poolConfig.getRules().isEmpty()) {
					alertManager.registerAlertRules(appName, poolName, poolConfig.getRules(), defaultRules);
					log.info("应用 {} 线程池 {} 告警规则已注册，监控间隔: {}ms", appName, poolName, poolConfig.getMonitorInterval());
				}
			});
		}
	}
}

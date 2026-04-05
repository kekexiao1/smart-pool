package com.xiao.smartpoolcore.common.util;

import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.stereotype.Component;

@Component
public class ApplicationContextHolder implements ApplicationContextAware {

	private static ApplicationContext applicationContext;

	@Override
	public void setApplicationContext(ApplicationContext context) throws BeansException {
		applicationContext = context;
	}

	public static <T> T getBean(Class<T> clazz) {
		if (applicationContext == null) {
			return null;
		}
		try {
			return applicationContext.getBean(clazz);
		} catch (BeansException e) {
			return null;
		}
	}

	public static Object getBean(String name) {
		if (applicationContext == null) {
			return null;
		}
		try {
			return applicationContext.getBean(name);
		} catch (BeansException e) {
			return null;
		}
	}

	public static <T> T getBeanOrNull(Class<T> clazz) {
		return getBean(clazz);
	}

	public static ApplicationContext getApplicationContext() {
		return applicationContext;
	}
}

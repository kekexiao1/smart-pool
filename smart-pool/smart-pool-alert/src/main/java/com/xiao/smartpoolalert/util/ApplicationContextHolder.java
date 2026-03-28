package com.xiao.smartpoolalert.util;

import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.stereotype.Component;

/**
 * Spring应用上下文持有器
 * 用于在非Spring管理的类中获取Bean
 */
@Component
public class ApplicationContextHolder implements ApplicationContextAware {
    
    private static ApplicationContext applicationContext;
    
    @Override
    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
        ApplicationContextHolder.applicationContext = applicationContext;
    }
    
    /**
     * 获取Spring Bean
     * @param clazz Bean类型
     * @return Bean实例，如果未找到返回null
     */
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
    
    /**
     * 获取Spring Bean
     * @param beanName Bean名称
     * @param clazz Bean类型
     * @return Bean实例，如果未找到返回null
     */
    public static <T> T getBean(String beanName, Class<T> clazz) {
        if (applicationContext == null) {
            return null;
        }
        try {
            return applicationContext.getBean(beanName, clazz);
        } catch (BeansException e) {
            return null;
        }
    }
    
    /**
     * 检查Bean是否存在
     * @param clazz Bean类型
     * @return 是否存在
     */
    public static boolean containsBean(Class<?> clazz) {
        if (applicationContext == null) {
            return false;
        }
        try {
            applicationContext.getBean(clazz);
            return true;
        } catch (BeansException e) {
            return false;
        }
    }
}
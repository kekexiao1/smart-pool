package com.xiao.smartpoolalert.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.ArrayList;
import java.util.List;

/**
 * 告警配置属性
 */
@Data
@ConfigurationProperties(prefix = "pool-alert.alert")
public class AlertProperties {
    
    /**
     * 统一通知器配置列表
     */
    private List<NotifierConfig> notifiers = new ArrayList<>();
    
    @Data
    public static class NotifierConfig {
        
        /**
         * 通知器类型 (EMAIL, DING, WECHAT)
         */
        private String type;
        
        /**
         * 是否启用
         */
        private boolean enabled = false;
        
        /**
         * 处理的告警类型列表，空表示处理所有类型
         */
        private List<String> alertTypes = new ArrayList<>();
        
        /**
         * 邮件配置 (仅EMAIL类型使用)
         */
        private EmailConfig email = new EmailConfig();
        
        /**
         * Webhook配置 (DING/WECHAT类型使用)
         */
        private WebhookConfig webhook = new WebhookConfig();
    }
    
    @Data
    public static class EmailConfig {
        /**
         * 发件人邮箱
         */
        private String from ;
        
        /**
         * 收件人邮箱
         */
        private String to ;
        
        /**
         * SMTP服务器主机
         */
        private String host;
        
        /**
         * SMTP服务器端口
         */
        private int port = 587;
        
        /**
         * 邮箱用户名
         */
        private String username ;
        
        /**
         * 邮箱密码或授权码
         */
        private String password ;
        
        /**
         * 是否启用SSL
         */
        private boolean ssl = true;
    }
    
    @Data
    public static class WebhookConfig {
        /**
         * Webhook URL
         */
        private String url;
        
        /**
         * 密钥/令牌
         */
        private String secret;
    }
}
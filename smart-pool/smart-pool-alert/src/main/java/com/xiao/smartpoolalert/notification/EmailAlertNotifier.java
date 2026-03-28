package com.xiao.smartpoolalert.notification;

import com.xiao.smartpoolalert.context.AlertContext;
import lombok.extern.slf4j.Slf4j;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;

import java.time.LocalDateTime;

/**
 * 邮件告警通知器
 * 支持QQ邮箱等SMTP邮件服务
 */
@Slf4j
public class EmailAlertNotifier implements AlertNotifier {
    
    private final JavaMailSender mailSender;
    private final String fromEmail;
    private final String toEmail;
    
    public EmailAlertNotifier(JavaMailSender mailSender, String fromEmail, String toEmail) {
        this.mailSender = mailSender;
        this.fromEmail = fromEmail;
        this.toEmail = toEmail;
    }
    
    @Override
    public boolean sendAlert(AlertContext context, String message) {
        try {
            SimpleMailMessage mailMessage = new SimpleMailMessage();
            mailMessage.setFrom(fromEmail);
            mailMessage.setTo(toEmail);
            mailMessage.setSubject("线程池告警通知 - " + context.getPoolName());
            mailMessage.setText(buildEmailContent(context, message));
            
            mailSender.send(mailMessage);
            log.info("邮件告警发送成功 - 线程池: {}, 消息: {}", context.getPoolName(), message);
            return true;
        } catch (Exception e) {
            log.error("邮件告警发送失败 - 线程池: {}, 错误: {}", context.getPoolName(), e.getMessage());
            return false;
        }
    }
    
    @Override
    public String getType() {
        return "EMAIL";
    }
    
    private String buildEmailContent(AlertContext context, String message) {
        return String.format(
            "线程池告警通知\n" +
            "线程池名称: %s\n" +
            "告警类型: %s\n" +
            "告警时间: %s\n" +
            "告警消息: %s\n" +
            "\n请及时处理！",
            context.getPoolName(),
            context.getRule().getType(),
            LocalDateTime.now(),
            message
        );
    }
}
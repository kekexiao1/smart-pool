package com.xiao.smartpoolalert.notification;

import com.xiao.smartpoolalert.context.AlertContext;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.*;
import org.springframework.web.client.RestTemplate;

import java.util.HashMap;
import java.util.Map;

/**
 * 企业微信群机器人通知器
 */
@Slf4j
public class WeChatAlertNotifier implements AlertNotifier {
    
    private final String webhook;
    private final RestTemplate restTemplate;
    
    public WeChatAlertNotifier(String webhook) {
        this.webhook = webhook;
        this.restTemplate = new RestTemplate();
    }
    
    @Override
    public boolean sendAlert(AlertContext context, String message) {
        try {
            Map<String, Object> requestBody = new HashMap<>();
            Map<String, String> textContent = new HashMap<>();
            
            textContent.put("content", buildWeChatMessage(context, message));
            requestBody.put("msgtype", "text");
            requestBody.put("text", textContent);
            
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(requestBody, headers);
            
            ResponseEntity<String> response = restTemplate.postForEntity(webhook, entity, String.class);
            
            if (response.getStatusCode() == HttpStatus.OK) {
                log.info("企业微信告警发送成功 - 线程池: {}, 消息: {}", context.getPoolName(), message);
                return true;
            } else {
                log.error("企业微信告警发送失败 - 线程池: {}, 状态码: {}", context.getPoolName(), response.getStatusCode());
                return false;
            }
        } catch (Exception e) {
            log.error("企业微信告警发送异常 - 线程池: {}, 错误: {}", context.getPoolName(), e.getMessage());
            return false;
        }
    }
    
    @Override
    public String getType() {
        return "WECHAT";
    }
    
    private String buildWeChatMessage(AlertContext context, String message) {
        return String.format(
            "线程池告警通知\n" +
            "线程池名称: %s\n" +
            "告警类型: %s\n" +
            "告警时间: %s\n" +
            "告警消息: %s\n" +
            "\n请及时处理！",
            context.getPoolName(),
            context.getRule().getType(),
            java.time.LocalDateTime.now(),
            message
        );
    }
}
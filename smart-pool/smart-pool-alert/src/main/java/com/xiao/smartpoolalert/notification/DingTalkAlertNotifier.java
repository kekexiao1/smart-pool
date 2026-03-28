package com.xiao.smartpoolalert.notification;

import com.xiao.smartpoolalert.context.AlertContext;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.*;
import org.springframework.web.client.RestTemplate;

import java.util.HashMap;
import java.util.Map;

/**
 * 钉钉群机器人通知器
 */
@Slf4j
public class DingTalkAlertNotifier implements AlertNotifier {
    
    private final String webhook;
    private final String secret;
    private final RestTemplate restTemplate;
    
    public DingTalkAlertNotifier(String webhook, String secret) {
        this.webhook = webhook;
        this.secret = secret;
        this.restTemplate = new RestTemplate();
    }
    
    @Override
    public boolean sendAlert(AlertContext context, String message) {
        try {
            Map<String, Object> requestBody = new HashMap<>();
            Map<String, String> textContent = new HashMap<>();
            
            textContent.put("content", buildDingTalkMessage(context, message));
            requestBody.put("msgtype", "text");
            requestBody.put("text", textContent);
            
            // 如果有密钥，需要计算签名
            String finalWebhook = webhook;
            if (secret != null && !secret.trim().isEmpty()) {
                long timestamp = System.currentTimeMillis();
                String sign = calculateSign(timestamp);
                finalWebhook = webhook + "&timestamp=" + timestamp + "&sign=" + sign;
            }
            
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(requestBody, headers);
            
            ResponseEntity<String> response = restTemplate.postForEntity(finalWebhook, entity, String.class);
            
            if (response.getStatusCode() == HttpStatus.OK) {
                log.info("钉钉告警发送成功 - 线程池: {}, 消息: {}", context.getPoolName(), message);
                return true;
            } else {
                log.error("钉钉告警发送失败 - 线程池: {}, 状态码: {}", context.getPoolName(), response.getStatusCode());
                return false;
            }
        } catch (Exception e) {
            log.error("钉钉告警发送异常 - 线程池: {}, 错误: {}", context.getPoolName(), e.getMessage());
            return false;
        }
    }
    
    @Override
    public String getType() {
        return "DING";
    }
    
    private String buildDingTalkMessage(AlertContext context, String message) {
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
    
    private String calculateSign(long timestamp) {
        try {
            String stringToSign = timestamp + "\n" + secret;
            javax.crypto.Mac mac = javax.crypto.Mac.getInstance("HmacSHA256");
            mac.init(new javax.crypto.spec.SecretKeySpec(secret.getBytes("UTF-8"), "HmacSHA256"));
            byte[] signData = mac.doFinal(stringToSign.getBytes("UTF-8"));
            return java.util.Base64.getEncoder().encodeToString(signData);
        } catch (Exception e) {
            log.error("计算钉钉签名失败: {}", e.getMessage());
            return "";
        }
    }
}
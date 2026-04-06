package com.xiao.smartpooladminserver.controller;

import lombok.extern.slf4j.Slf4j;
import org.apache.rocketmq.client.producer.DefaultMQProducer;
import org.apache.rocketmq.spring.core.RocketMQTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/rocketmq")
@Slf4j
public class RocketMQController {

    @Autowired
    private RocketMQTemplate rocketMQTemplate;

    @GetMapping("/health")
    public Map<String, Object> healthCheck() {
        Map<String, Object> result = new HashMap<>();
        
        try {
            DefaultMQProducer producer = rocketMQTemplate.getProducer();
            if (producer != null) {
                result.put("producerStatus", "RUNNING");
                result.put("producerGroup", producer.getProducerGroup());
                result.put("nameServerAddr", producer.getNamesrvAddr());
            } else {
                result.put("producerStatus", "NOT_AVAILABLE");
            }
            result.put("status", "HEALTHY");
        } catch (Exception e) {
            log.error("RocketMQ健康检查失败", e);
            result.put("status", "UNHEALTHY");
            result.put("error", e.getMessage());
        }
        
        return result;
    }

    @PostMapping("/test/send")
    public Map<String, Object> testSendMessage(@RequestParam String topic, 
                                              @RequestParam String message) {
        Map<String, Object> result = new HashMap<>();
        
        try {
            rocketMQTemplate.convertAndSend(topic, message);
            result.put("success", true);
            result.put("message", "消息发送成功");
            result.put("topic", topic);
            log.info("测试消息发送成功 - Topic: {}, Message: {}", topic, message);
        } catch (Exception e) {
            log.error("测试消息发送失败", e);
            result.put("success", false);
            result.put("error", e.getMessage());
        }
        
        return result;
    }

    @GetMapping("/config")
    public Map<String, Object> getConfig() {
        Map<String, Object> config = new HashMap<>();
        
        try {
            DefaultMQProducer producer = rocketMQTemplate.getProducer();
            if (producer != null) {
                config.put("nameServer", producer.getNamesrvAddr());
                config.put("producerGroup", producer.getProducerGroup());
                config.put("sendMsgTimeout", producer.getSendMsgTimeout());
                config.put("retryTimes", producer.getRetryTimesWhenSendFailed());
            }
        } catch (Exception e) {
            log.error("获取RocketMQ配置失败", e);
        }
        
        return config;
    }
}
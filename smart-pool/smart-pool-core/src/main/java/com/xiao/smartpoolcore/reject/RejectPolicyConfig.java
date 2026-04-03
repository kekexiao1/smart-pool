package com.xiao.smartpoolcore.reject;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 拒绝策略配置类
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RejectPolicyConfig {

    /**
     * 策略类型
     */
    @Builder.Default
    private String policyType = "abort";

    /**
     * Redis队列名称（Redis策略使用）
     */
    @Builder.Default
    private String redisQueueName = "thread:pool:reject:queue";

    /**
     * MQ交换机名称（MQ策略使用）
     */
    @Builder.Default
    private String mqExchangeName = "thread.pool.reject.exchange";

    /**
     * MQ路由键（MQ策略使用）
     */
    @Builder.Default
    private String mqRoutingKey = "thread.pool.reject";

    /**
     * 本地磁盘存储目录（本地磁盘策略使用）
     */
    @Builder.Default
    private String localDiskDir = "logs/rejected-tasks";

    /**
     * 最大重试次数
     */
    @Builder.Default
    private int maxRetryCount = 3;

    /**
     * 重试间隔（毫秒）
     */
    @Builder.Default
    private long retryInterval = 100;

    /**
     * 是否启用详细日志
     */
    @Builder.Default
    private boolean enableDetailedLogging = true;

    /**
     * 是否启用指标记录
     */
    @Builder.Default
    private boolean enableMetrics = true;

    /**
     * 本地文件保留天数
     */
    @Builder.Default
    private int localFileRetentionDays = 7;

    /**
     * 是否启用任务序列化验证
     */
    @Builder.Default
    private boolean enableSerializationCheck = true;

    /**
     * 创建默认配置
     */
    public static RejectPolicyConfig defaultConfig() {
        return RejectPolicyConfig.builder().build();
    }

    /**
     * 创建Redis策略配置
     */
    public static RejectPolicyConfig redisConfig() {
        return RejectPolicyConfig.builder()
                .policyType("redis")
                .build();
    }

    /**
     * 创建MQ策略配置
     */
    public static RejectPolicyConfig mqConfig() {
        return RejectPolicyConfig.builder()
                .policyType("mq")
                .build();
    }

    /**
     * 创建本地磁盘策略配置
     */
    public static RejectPolicyConfig localDiskConfig() {
        return RejectPolicyConfig.builder()
                .policyType("local_disk")
                .build();
    }

    /**
     * 验证配置有效性
     */
    public boolean isValid() {
        if (policyType == null || policyType.trim().isEmpty()) {
            return false;
        }

        if (maxRetryCount < 0) {
            return false;
        }

        if (retryInterval < 0) {
            return false;
        }

        if (localFileRetentionDays < 1) {
            return false;
        }

        return true;
    }
}
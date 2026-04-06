package com.xiao.smartpoolcore.reject;

import com.xiao.smartpoolcore.common.constant.PolicyType;
import com.xiao.smartpoolcore.common.util.ApplicationContextHolder;
import com.xiao.smartpoolcore.reject.local.LocalDiskRejectPolicy;
import com.xiao.smartpoolcore.reject.mq.MQRejectPolicy;
import com.xiao.smartpoolcore.reject.redis.LazyRedisRejectPolicy;
import lombok.extern.slf4j.Slf4j;
import org.apache.rocketmq.spring.core.RocketMQTemplate;

import java.util.concurrent.RejectedExecutionHandler;
import java.util.concurrent.ThreadPoolExecutor;

@Slf4j
public class RejectPolicyFactory {

    public static RejectedExecutionHandler createPolicy(PolicyType policyType) {
        if (policyType == null) {
            log.warn("拒绝策略类型为空，使用默认中止策略");
            return new ThreadPoolExecutor.AbortPolicy();
        }

        try {
            switch (policyType) {
                case REDIS:
                    return createRedisPolicy();
                case MQ:
                    return createMQPolicy();
                case LOCAL_DISK:
                    return new LocalDiskRejectPolicy();
                case CALLER_RUNS:
                    return new ThreadPoolExecutor.CallerRunsPolicy();
                case ABORT:
                    return new ThreadPoolExecutor.AbortPolicy();
                case DISCARD:
                    return new ThreadPoolExecutor.DiscardPolicy();
                case DISCARD_OLDEST:
                    return new ThreadPoolExecutor.DiscardOldestPolicy();
                default:
                    log.warn("未知的拒绝策略类型: {}, 使用默认中止策略", policyType);
                    return new ThreadPoolExecutor.AbortPolicy();
            }
        } catch (Exception e) {
            log.error("创建拒绝策略失败: {}, 使用默认中止策略", policyType, e);
            return new ThreadPoolExecutor.AbortPolicy();
        }
    }

    private static RejectedExecutionHandler createRedisPolicy() {
        log.info("创建 Redis 拒绝策略（延迟加载模式）");
        return new LazyRedisRejectPolicy();
    }

    private static RejectedExecutionHandler createMQPolicy() {
        RocketMQTemplate rocketMQTemplate = ApplicationContextHolder.getBean(RocketMQTemplate.class);
        log.warn("注册RocketMq拒绝策略");
        if (rocketMQTemplate == null) {
            log.warn("RocketMQTemplate 未提供，降级为 LocalDiskRejectPolicy");
            return new LocalDiskRejectPolicy();
        }
        return new MQRejectPolicy(rocketMQTemplate);
    }

    public static RejectedExecutionHandler createPolicyByName(String policyName) {
        PolicyType policyType = PolicyType.fromCode(policyName);
        return createPolicy(policyType);
    }

    public static PolicyType[] getSupportedPolicyTypes() {
        return PolicyType.values();
    }

    public static boolean isPolicySupported(String policyCode) {
        try {
            PolicyType.fromCode(policyCode);
            return true;
        } catch (Exception e) {
            return false;
        }
    }
}

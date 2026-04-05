package com.xiao.smartpoolcore.reject.redis;

import com.xiao.smartpoolcore.common.util.ApplicationContextHolder;
import com.xiao.smartpoolcore.reject.local.LocalDiskRejectPolicy;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationContext;
import org.springframework.data.redis.core.RedisTemplate;

import java.util.concurrent.RejectedExecutionHandler;
import java.util.concurrent.ThreadPoolExecutor;

@Slf4j
public class LazyRedisRejectPolicy implements RejectedExecutionHandler {

    private volatile RejectedExecutionHandler delegate = null;
    private volatile boolean initialized = false;

    @Override
    public void rejectedExecution(Runnable task, ThreadPoolExecutor executor) {
        if (!initialized) {
            synchronized (this) {
                if (!initialized) {
                    delegate = createDelegate();
                    initialized = true;
                }
            }
        }
        delegate.rejectedExecution(task, executor);
    }

    private RejectedExecutionHandler createDelegate() {
        log.info("========== 延迟初始化 Redis 拒绝策略 ==========");
        
        ApplicationContext ctx = ApplicationContextHolder.getApplicationContext();
        log.info("ApplicationContext 是否为空: {}", ctx == null);
        
        if (ctx != null) {
            String[] beanNames = ctx.getBeanNamesForType(RedisTemplate.class);
            log.info("RedisTemplate 类型的 Bean 名称: {}", String.join(", ", beanNames));
        }
        
        RedisTemplate<String, Object> redisTemplate = (RedisTemplate<String, Object>) ApplicationContextHolder.getBean("redisTemplate");
        log.info("获取到的 RedisTemplate: {}", redisTemplate);
        
        if (redisTemplate == null) {
            log.warn("RedisTemplate 未提供，降级为 LocalDiskRejectPolicy");
            return new LocalDiskRejectPolicy();
        }

        log.info("Redis 拒绝策略初始化成功");
        return new RedisRejectPolicy(redisTemplate);
    }
}

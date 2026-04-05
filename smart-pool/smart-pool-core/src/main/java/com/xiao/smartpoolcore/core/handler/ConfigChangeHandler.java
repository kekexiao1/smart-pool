package com.xiao.smartpoolcore.core.handler;

import com.xiao.smartpoolcore.callback.PoolLogService;
import com.xiao.smartpoolcore.common.constant.PoolLogConstant;
import com.xiao.smartpoolcore.common.constant.ThreadPoolConstant;
import com.xiao.smartpoolcore.common.util.ParseUtil;
import com.xiao.smartpoolcore.common.util.ValidateUtil;
import com.xiao.smartpoolcore.config.CountingRejectedExecutionHandler;
import com.xiao.smartpoolcore.config.DynamicCapacityBlockingQueue;
import com.xiao.smartpoolcore.core.executor.DynamicThreadPoolExecutor;
import com.xiao.smartpoolcore.core.registry.ThreadPoolRegistry;
import com.xiao.smartpoolcore.model.dto.ConfigChanges;
import com.xiao.smartpoolcore.model.dto.DynamicThreadPoolProperties;
import com.xiao.smartpoolcore.model.dto.PoolConfigLogDTO;
import com.xiao.smartpoolcore.model.dto.ThreadPoolConfig;
import com.xiao.smartpoolcore.model.event.ConfigChangeEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.RejectedExecutionHandler;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

/**
 * 配置变更处理器
 * 负责处理配置初始化和更新逻辑，统一管理配置变更流程
 */
@Slf4j
@RequiredArgsConstructor
public class ConfigChangeHandler {

    private final ThreadPoolRegistry registry;
    private final PoolLogService poolLogService;

    /**
     * 处理配置初始化
     * @param configInfo 配置信息
     */
    public void handleConfigInit(String configInfo) {
        log.info("开始处理配置初始化");
        // 解析配置
        DynamicThreadPoolProperties properties = ParseUtil.parseConfig(configInfo);
        Map<String, ThreadPoolConfig> configs = properties.getExecutors();

        if (configs == null || configs.isEmpty()) {
            log.warn("配置文件中未发现线程池配置");
            return;
        }

        // 初始化所有线程池
        configs.forEach((poolName, config) -> {
            try {
                config.setThreadPoolName(poolName);
                // 初始化不填充默认值
                ValidateUtil.validateConfig(config);

                DynamicThreadPoolExecutor executor = createExecutor(config);
                if (executor != null) {
                    registry.register(poolName, executor, config);
                    log.info("初始化线程池 [{}] 成功", poolName);
                }
            } catch (Exception e) {
                log.error("初始化线程池 [{}] 失败", poolName, e);
            }
        });
        // 记录初始化日志
        PoolConfigLogDTO initLogDTO = new PoolConfigLogDTO();
        initLogDTO.setAppName(PoolLogConstant.APP_NAME);
        initLogDTO.setPoolName(PoolLogConstant.ALL_POLL);
        initLogDTO.setNewConfig(configInfo);
        initLogDTO.setOperator(PoolLogConstant.INIT_OPERATOR);
        initLogDTO.setChangeType(PoolLogConstant.INIT_CHANGE_TYPE);
        initLogDTO.setSource(PoolLogConstant.SOURCE_NACOS);
        poolLogService.logConfigInit(initLogDTO);
        log.info("配置初始化处理完成");
    }

    /**
     * 处理配置更新（全量更新）
     * @param event 配置变更事件
     */
    public void handleConfigUpdate(ConfigChangeEvent event) {
        log.info("开始处理配置更新");
        
        String newConfigInfo = event.getNewConfig();
        
        try {
            // 处理空配置的情况（删除所有线程池）
            if (newConfigInfo == null || newConfigInfo.trim().isEmpty()) {
                log.info("检测到空配置，将删除所有线程池");
                deleteAllPools(event);
                return;
            }
            
            DynamicThreadPoolProperties properties = ParseUtil.parseConfig(newConfigInfo);
            Map<String, ThreadPoolConfig> configs = properties.getExecutors();
            
            if (configs == null) {
                configs = new HashMap<>();
            }

            // 找出被删除的线程池
            List<String> removedPools = findRemovedPools(configs);
            
            // 构建旧配置信息用于日志记录
            StringBuilder oldConfigInfoBuilder = new StringBuilder().append("executors:")
                    .append("\n  ");
            Map<String, ThreadPoolConfig> allConfigs = registry.getAllConfigs();
            if (allConfigs != null) {
                allConfigs.forEach((poolName, config) -> {
                    oldConfigInfoBuilder.append(config).append("\n  ");
                });
            }

            // 处理每个线程池的配置变更
            configs.forEach((poolName, newConfig) -> {
                try {
                    newConfig.setThreadPoolName(poolName);
                    
                    if (removedPools != null && removedPools.contains(poolName)) {
                        return; // 跳过被删除的线程池
                    }

                    if (!registry.containExecutors(poolName)) {
                        // 新增线程池
                        handleNewPool(newConfig);
                    } else {
                        // 更新现有线程池
                        handlePoolUpdate(poolName, newConfig);
                    }
                } catch (Exception e) {
                    log.error("处理线程池 [{}] 配置更新失败", poolName, e);
                }
            });

            // 删除被移除的线程池
            if (removedPools != null && !removedPools.isEmpty()) {
                registry.deletePools(removedPools);
                log.info("已删除线程池: {}", removedPools);
            }

            // 记录更新日志
            if(!event.isFromAdmin()){
                PoolConfigLogDTO updateLogDTO = getPoolConfigLogDTO(newConfigInfo, oldConfigInfoBuilder);
                poolLogService.logConfigUpdate(updateLogDTO);
                log.info("配置更新处理完成（Nacos监听器触发）");
            }
            
        } catch (Exception e) {
            log.error("配置更新处理失败", e);
            throw new RuntimeException("配置更新处理失败: " + e.getMessage(), e);
        }
    }

    private PoolConfigLogDTO getPoolConfigLogDTO(String newConfigInfo, StringBuilder oldConfigInfoBuilder) {
        PoolConfigLogDTO updateLogDTO = new PoolConfigLogDTO();
        updateLogDTO.setAppName(PoolLogConstant.APP_NAME);
        updateLogDTO.setPoolName(PoolLogConstant.ALL_POLL);
        updateLogDTO.setChangeType(PoolLogConstant.UPDATE_CHANGE_TYPE);
        updateLogDTO.setOperator(PoolLogConstant.NACOS_OPERATOR);
        updateLogDTO.setNewConfig(newConfigInfo);
        updateLogDTO.setOldConfig(oldConfigInfoBuilder.toString());
        updateLogDTO.setSource(PoolLogConstant.SOURCE_NACOS);
        return updateLogDTO;
    }


    /**
     * 处理新增线程池
     */
    private void handleNewPool(ThreadPoolConfig newConfig) {
        fillDefaultValues(newConfig);
        ValidateUtil.validateConfig(newConfig);
        
        DynamicThreadPoolExecutor executor = createExecutor(newConfig);
        if (executor != null) {
            registry.register(newConfig.getThreadPoolName(), executor, newConfig);
            log.info("新增线程池 [{}] 成功", newConfig.getThreadPoolName());
        } else {
            log.error("新增线程池 [{}] 失败", newConfig.getThreadPoolName());
        }
    }

    /**
     * 处理线程池更新
     */
    private void handlePoolUpdate(String poolName, ThreadPoolConfig newConfig) {
        ThreadPoolConfig oldConfig = registry.getConfig(poolName);
        DynamicThreadPoolExecutor executor = registry.getExecutor(poolName);
        
        fillDefaultValues(newConfig);
        updateConfig(executor, oldConfig, newConfig);
        
        registry.setConfig(poolName, newConfig);
        log.info("线程池 [{}] 配置更新成功", poolName);
    }

    /**
     * 创建线程池执行器
     */
    private synchronized DynamicThreadPoolExecutor createExecutor(ThreadPoolConfig config) {
        String threadPoolName = config.getThreadPoolName();
        if (threadPoolName == null || threadPoolName.trim().isEmpty()) {
            log.error("无法创建线程池，线程池名称为空");
            return null;
        }
        
        if (registry.getExecutor(threadPoolName) != null) {
            return null; // 已存在，直接返回
        }
        return new DynamicThreadPoolExecutor(config);
    }

    /**
     * 填充配置默认值
     */
    private void fillDefaultValues(ThreadPoolConfig config) {

        if (config.getCorePoolSize() <= 0) {
            config.setCorePoolSize(ThreadPoolConstant.DEFAULT_CORE_POOL_SIZE);
        }
        if (config.getMaximumPoolSize() <= 0) {
            config.setMaximumPoolSize(ThreadPoolConstant.DEFAULT_MAXIMUM_POOL_SIZE);
        }
        if (config.getKeepAliveTime() <= 0) {
            config.setKeepAliveTime(ThreadPoolConstant.DEFAULT_KEEP_ALIVE_TIME);
        }
        if (config.getUnit() == null) {
            config.setUnit(TimeUnit.SECONDS);
        }
        if (config.getQueueCapacity() <= 0) {
            config.setQueueCapacity(ThreadPoolConstant.DEFAULT_QUEUE_CAPACITY);
        }
        if (config.getRejectedHandlerClass() == null || config.getRejectedHandlerClass().trim().isEmpty()) {
            config.setRejectedHandlerClass(ThreadPoolConstant.DEFAULT_REJECTED_HANDLER);
        }
    }

    /**
     * 更新线程池配置
     */
    private synchronized void updateConfig(DynamicThreadPoolExecutor dynamicExecutor, 
                                         ThreadPoolConfig oldConfig, ThreadPoolConfig newConfig) {
        try {
            ValidateUtil.validateConfig(newConfig);
            ConfigChanges changes = detectChanges(oldConfig, newConfig);

            if (!changes.hasChanges()) {
                log.info("线程池 [{}] 配置无变化，跳过更新", dynamicExecutor.getThreadPoolName());
                return;
            }

            ThreadPoolExecutor executor = dynamicExecutor.getExecutor();
            updatePoolSizeIncrementally(executor, oldConfig, newConfig);
            executor.setKeepAliveTime(newConfig.getKeepAliveTime(), newConfig.getUnit());
            updateQueueCapacity(executor, newConfig.getQueueCapacity());
            updateRejectedHandler(dynamicExecutor, newConfig.getRejectedHandlerClass());
            dynamicExecutor.setConfig(newConfig);

            log.info("线程池 [{}] 配置更新成功，变更详情: {}", newConfig.getThreadPoolName(), changes);

        } catch (Exception e) {
            log.error("线程池 [{}] 配置更新失败，开始回滚", dynamicExecutor.getThreadPoolName(), e);
            rollback(dynamicExecutor, oldConfig);
            throw new RuntimeException("配置更新失败，已回滚", e);
        }
    }

    /**
     * 增量更新线程池大小
     */
    private void updatePoolSizeIncrementally(ThreadPoolExecutor executor, 
                                           ThreadPoolConfig oldConfig, ThreadPoolConfig newConfig) {
        if (newConfig.getMaximumPoolSize() > oldConfig.getMaximumPoolSize()) {
            executor.setMaximumPoolSize(newConfig.getMaximumPoolSize());
        }
        if (newConfig.getCorePoolSize() > oldConfig.getCorePoolSize()) {
            executor.setCorePoolSize(newConfig.getCorePoolSize());
        }
        if (newConfig.getCorePoolSize() < oldConfig.getCorePoolSize()) {
            executor.setCorePoolSize(newConfig.getCorePoolSize());
        }
        if (newConfig.getMaximumPoolSize() < oldConfig.getMaximumPoolSize()) {
            executor.setMaximumPoolSize(newConfig.getMaximumPoolSize());
        }
    }

    /**
     * 更新队列容量
     */
    private void updateQueueCapacity(ThreadPoolExecutor executor, int capacity) {
        BlockingQueue<Runnable> queue = executor.getQueue();
        if (queue instanceof DynamicCapacityBlockingQueue) {
            ((DynamicCapacityBlockingQueue) queue).setCapacity(capacity);
        }
    }

    /**
     * 更新拒绝策略
     */
    private void updateRejectedHandler(DynamicThreadPoolExecutor dynamicExecutor, String rejectedClassName) {
        ThreadPoolExecutor executor = dynamicExecutor.getExecutor();
        RejectedExecutionHandler oldHandler = executor.getRejectedExecutionHandler();
        
        RejectedExecutionHandler newHandler = ParseUtil.parseRejectedHandler(rejectedClassName);
        if (newHandler != null) {
            if (oldHandler instanceof CountingRejectedExecutionHandler && 
                newHandler instanceof CountingRejectedExecutionHandler) {
                CountingRejectedExecutionHandler oldCounting = (CountingRejectedExecutionHandler) oldHandler;
                CountingRejectedExecutionHandler newCounting = (CountingRejectedExecutionHandler) newHandler;
                newCounting.setRejectedCount(oldCounting.getRejectedCount());
            }
            executor.setRejectedExecutionHandler(newHandler);
            log.info("[{}] 拒绝策略更新为：{}", dynamicExecutor.getThreadPoolName(), rejectedClassName);
        } else {
            throw new IllegalArgumentException("未知的拒绝策略: " + rejectedClassName);
        }
    }

    /**
     * 检测配置变更
     */
    private ConfigChanges detectChanges(ThreadPoolConfig oldConfig, ThreadPoolConfig newConfig) {
        return new ConfigChanges(oldConfig, newConfig);
    }

    /**
     * 回滚配置
     */
    private void rollback(DynamicThreadPoolExecutor dynamicExecutor, ThreadPoolConfig oldConfig) {
        ThreadPoolExecutor executor = dynamicExecutor.getExecutor();
        List<String> failedItems = new ArrayList<>();

        try {
            executor.setCorePoolSize(oldConfig.getCorePoolSize());
        } catch (Exception e) {
            failedItems.add("corePoolSize");
        }

        try {
            executor.setMaximumPoolSize(oldConfig.getMaximumPoolSize());
        } catch (Exception e) {
            failedItems.add("maximumPoolSize");
        }

        try {
            BlockingQueue<Runnable> queue = executor.getQueue();
            if (queue instanceof DynamicCapacityBlockingQueue) {
                ((DynamicCapacityBlockingQueue) queue).setCapacity(oldConfig.getQueueCapacity());
            }
        } catch (Exception e) {
            failedItems.add("queueCapacity");
        }

        try {
            updateRejectedHandler(dynamicExecutor, oldConfig.getRejectedHandlerClass());
        } catch (Exception e) {
            failedItems.add("rejectedHandler");
        }

        try {
            executor.setKeepAliveTime(oldConfig.getKeepAliveTime(), oldConfig.getUnit());
        } catch (Exception e) {
            failedItems.add("keepAliveTime");
        }

        dynamicExecutor.setConfig(oldConfig);

        if (failedItems.isEmpty()) {
            log.info("线程池 [{}] 配置已完全回滚", oldConfig.getThreadPoolName());
        } else {
            log.error("线程池 [{}] 配置部分回滚失败，失败项: {}", oldConfig.getThreadPoolName(), failedItems);
        }
    }

    /**
	 * 删除所有线程池
	 */
	private void deleteAllPools(ConfigChangeEvent event) {
		Map<String, ThreadPoolConfig> allConfigs = registry.getAllConfigs();
		if (allConfigs == null || allConfigs.isEmpty()) {
			log.info("当前没有线程池需要删除");
			return;
		}
		
		List<String> poolNames = new ArrayList<>(allConfigs.keySet());
		registry.deletePools(poolNames);
		log.info("已删除所有线程池: {}", poolNames);
		
		// 只有当事件不是来自管理端时才记录日志（避免重复记录）
		if (!event.isFromAdmin()) {
			// 记录删除日志
			PoolConfigLogDTO deleteLogDTO = new PoolConfigLogDTO();
			deleteLogDTO.setAppName(PoolLogConstant.APP_NAME);
			deleteLogDTO.setPoolName(PoolLogConstant.ALL_POLL);
			deleteLogDTO.setChangeType(PoolLogConstant.DELETE_CHANGE_TYPE);
			deleteLogDTO.setOperator(PoolLogConstant.NACOS_OPERATOR);
			deleteLogDTO.setOldConfig("所有线程池配置");
			deleteLogDTO.setNewConfig("空配置");
			deleteLogDTO.setSource(PoolLogConstant.SOURCE_NACOS);
			poolLogService.logConfigUpdate(deleteLogDTO);
			log.info("所有线程池删除处理完成（Nacos监听器触发）");
		} else {
			log.info("所有线程池删除处理完成（管理端触发，跳过重复日志记录）");
		}
	}

	/**
	 * 找出被删除的线程池
	 */
	private List<String> findRemovedPools(Map<String, ThreadPoolConfig> newConfigs) {
        List<String> removed = new ArrayList<>();
        Map<String, ThreadPoolConfig> allConfigs = registry.getAllConfigs();

        if (newConfigs == null || newConfigs.isEmpty()) {
            if (allConfigs != null) {
                removed.addAll(allConfigs.keySet());
            }
            return removed;
        }

        if (allConfigs == null || allConfigs.isEmpty()) {
            return null;
        }

        for (String poolName : allConfigs.keySet()) {
            if (!newConfigs.containsKey(poolName)) {
                removed.add(poolName);
            }
        }
        return removed;
    }
}
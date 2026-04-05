package com.xiao.smartpoolcore.core.registry;

import com.alibaba.cloud.nacos.utils.StringUtils;
import com.xiao.smartpoolcore.model.dto.ThreadPoolConfig;
import com.xiao.smartpoolcore.core.executor.DynamicThreadPoolExecutor;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 线程池注册中心
 * 纯粹的注册表管理，负责线程池的注册、获取、移除和关闭
 */
@Slf4j
public class ThreadPoolRegistry {
	private final Map<String, DynamicThreadPoolExecutor> executorMap = new ConcurrentHashMap<>();
	private final Map<String, ThreadPoolConfig> configMap = new ConcurrentHashMap<>();

	public ThreadPoolRegistry() {
	}

	/**
	 * 注册线程池
	 * @param threadPoolName 线程池名称
	 * @param executor 线程池执行器
	 * @param config 线程池配置
	 */
	public void register(String threadPoolName, DynamicThreadPoolExecutor executor, ThreadPoolConfig config) {
		if (StringUtils.isEmpty(threadPoolName) || executor == null || config == null) {
			throw new IllegalArgumentException("线程池名称、实例、配置不能为空");
		}

		DynamicThreadPoolExecutor oldExecutor = executorMap.put(threadPoolName, executor);
		configMap.put(threadPoolName, config);

		// 关闭旧的线程池
		if (oldExecutor != null && !oldExecutor.isShutDown()) {
			oldExecutor.shutDown();
			log.info("已关闭旧的线程池: {}", threadPoolName);
		}
		
		log.info("线程池 [{}] 注册成功", threadPoolName);
	}



	/**
	 * 获取配置
	 * @param threadPoolName
	 * @return
	 */
	public ThreadPoolConfig getConfig(String threadPoolName){
		if(!configMap.containsKey(threadPoolName)){
			return null;
		}
		return configMap.get(threadPoolName);
	}

	/**
	 * 修改配置
	 * @param threadPoolName
	 * @param config
	 * @return
	 */
	public ThreadPoolConfig setConfig(String threadPoolName, ThreadPoolConfig config){
		if(StringUtils.isEmpty(threadPoolName)){
			log.error("config修改失败，threadPoolName为空");
			return null;
		}
		if(config==null){
			log.error("config修改失败，config为空");
			return null;
		}
		return configMap.put(threadPoolName, config);
	}

	/**
	 * 根据名称获取线程池
	 * @param threadPoolName 线程池名称
	 * @return 线程池执行器，不存在返回 null
	 */
	public DynamicThreadPoolExecutor getExecutor(String threadPoolName) {
		if (threadPoolName == null) {
			throw new IllegalArgumentException("获取线程池时，线程名不能为空");
		}

		return executorMap.get(threadPoolName);
	}


	/**
	 * 修改线程池
	 * @param threadPoolName
	 * @param executor
	 * @return
	 */
	public DynamicThreadPoolExecutor setExecutor(String threadPoolName, DynamicThreadPoolExecutor executor){
		if(StringUtils.isEmpty(threadPoolName)){
			log.error("executor修改失败，threadPoolName为空");
		}
		if(executor==null){
			log.error("executor修改失败，config为空");
			return null;
		}
		return executorMap.put(threadPoolName, executor);
	}

	/**
	 * 判断线程池是否存在
	 * @param threadPoolName 线程池名称
	 * @return 是否存在
	 */
	public boolean containExecutors(String threadPoolName) {
		if (threadPoolName == null) {
			return false;
		}
		return executorMap.containsKey(threadPoolName);
	}

	/**
	 * 配置变更时批量删除
	 * @param removedPools
	 */
	public void deletePools(List<String> removedPools){
		if(removedPools!=null && removedPools.size()!=0){
			removedPools.forEach((poolName) -> {
				try {
					remove(poolName);
				} catch (Exception e) {
					log.error("批量删除线程池 [{}] 失败", poolName, e);
				}
			});

		}
	}

	/**
	 * 移除指定线程池
	 * @param poolName 线程池名称
	 */
	public void remove(String poolName) {
		if (poolName == null) {
			return;
		}
		
		DynamicThreadPoolExecutor executor = executorMap.remove(poolName);
		configMap.remove(poolName);
		try{
			if (executor!=null && !executor.isShutDown()) {
				executor.shutDown();
				log.info("已关闭线程池: {}", poolName);
			}
		}catch (Exception e){
			log.error("关闭线程池失败: [{}]", poolName, e);
			throw new RuntimeException("关闭线程池失败: " + poolName, e);
		}
	}


	/**
	 * 关闭所有线程池
	 */
	public void shutDownAll() {
		if (executorMap.isEmpty()) {
			return;
		}
		
		log.info("开始关闭所有线程池，数量: {}", executorMap.size());

		executorMap.forEach((name, executor) -> {
			try{
				if (executor!=null && !executor.isShutDown()) {
					executor.shutDown();
					log.info("已关闭线程池: {}", name);
				}
			}catch (Exception e){
				log.error("关闭线程池失败: {}", e);
				throw new RuntimeException(e);
			}
		});
		executorMap.clear();
		configMap.clear();
		log.info("所有线程池已关闭");
	}
	
	/**
	 * 获取所有线程池名称
	 * @return 线程池名称集合（不可修改）
	 */
	public Set<String> getAllThreadPoolNames() {
		return Collections.unmodifiableSet(executorMap.keySet());
	}
	
//	/**
//	 * 获取所有线程池
//	 * @return 线程池映射（不可修改）
//	 */
//	public Map<String, DynamicThreadPoolExecutor> getAllExecutors() {
//		return Collections.unmodifiableMap(executorMap);
//	}

	/**
	 * 获取所有config
	 * @return 不可修改
	 */
	public Map<String, ThreadPoolConfig> getAllConfigs(){
		return Collections.unmodifiableMap(configMap);
	}

	/**
	 * 获取所有线程池执行器
	 * @return 所有线程池执行器
	 */
	public Map<String, DynamicThreadPoolExecutor> getAllExecutors() {
		return new HashMap<>(executorMap);
	}

	/**
	 * 获取所有线程池名称
	 * @return 所有线程池名称
	 */
	public Set<String> getAllPoolNames() {
		return new HashSet<>(executorMap.keySet());
	}

	/**
	 * 获取当前线程池数量
	 * @return 线程池数量
	 */
	public int size() {
		return executorMap.size();
	}
	
	/**
	 * 判断是否为空
	 * @return 是否没有注册任何线程池
	 */
	public boolean isEmpty() {
		return executorMap.isEmpty();
	}
}

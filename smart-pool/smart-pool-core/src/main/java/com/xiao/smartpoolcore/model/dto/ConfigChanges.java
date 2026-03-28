package com.xiao.smartpoolcore.model.dto;

import java.util.Objects;

/**
 * 配置变更记录类
 */
public class ConfigChanges {
	private final ThreadPoolConfig oldConfig;
	private final ThreadPoolConfig newConfig;

	public ConfigChanges(ThreadPoolConfig oldConfig,
						 ThreadPoolConfig newConfig) {
		this.oldConfig = oldConfig;
		this.newConfig = newConfig;
	}

	public boolean hasChanges() {
		// 初始化配置时有一个为null
		if(oldConfig ==null || newConfig==null){
			return true;
		}
		return isCorePoolSizeChanged() || isMaximumPoolSizeChanged()
				|| isKeepAliveTimeChanged() || isQueueCapacityChanged()
				|| isRejectedHandlerChanged();
	}

	public boolean isCorePoolSizeChanged() {
		return oldConfig.getCorePoolSize() != newConfig.getCorePoolSize();
	}

	public boolean isMaximumPoolSizeChanged() {
		return oldConfig.getMaximumPoolSize() != newConfig.getMaximumPoolSize();
	}

	public boolean isKeepAliveTimeChanged() {
		return oldConfig.getKeepAliveTime() != newConfig.getKeepAliveTime()
				|| oldConfig.getUnit() != newConfig.getUnit();
	}

	public boolean isQueueCapacityChanged() {
		return oldConfig.getQueueCapacity() != newConfig.getQueueCapacity();
	}

	public boolean isRejectedHandlerChanged() {
		return !Objects.equals(oldConfig.getRejectedHandlerClass(),
				newConfig.getRejectedHandlerClass());
	}

	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder();
		if (isCorePoolSizeChanged()) {
			sb.append("corePoolSize: ").append(oldConfig.getCorePoolSize())
					.append(" -> ").append(newConfig.getCorePoolSize()).append(", ");
		}
		if (isMaximumPoolSizeChanged()) {
			sb.append("maximumPoolSize: ").append(oldConfig.getMaximumPoolSize())
					.append(" -> ").append(newConfig.getMaximumPoolSize()).append(", ");
		}
		if (isKeepAliveTimeChanged()) {
			sb.append("keepAliveTime: ").append(oldConfig.getKeepAliveTime())
					.append(oldConfig.getUnit())
					.append(" -> ").append(newConfig.getKeepAliveTime())
					.append(newConfig.getUnit()).append(", ");
		}
		if (isQueueCapacityChanged()) {
			sb.append("queueCapacity: ").append(oldConfig.getQueueCapacity())
					.append(" -> ").append(newConfig.getQueueCapacity()).append(", ");
		}
		if (isRejectedHandlerChanged()) {
			sb.append("rejectedHandler: ").append(oldConfig.getRejectedHandlerClass())
					.append(" -> ").append(newConfig.getRejectedHandlerClass()).append(", ");
		}
		// 移除最后的", "
		if (sb.length() > 2) {
			sb.setLength(sb.length() - 2);
		}
		return sb.toString();
	}
}
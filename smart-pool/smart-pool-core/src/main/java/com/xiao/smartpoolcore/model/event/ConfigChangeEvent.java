package com.xiao.smartpoolcore.model.event;

import com.xiao.smartpoolcore.model.dto.ThreadPoolConfig;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 配置变更事件
 * 用于传递配置变更信息
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ConfigChangeEvent {

//	public boolean isAllUpdate() {
//		return allUpdate;
//	}
//
//	public String getConfigSource() {
//		return configSource;
//	}
//
//	public String getDataId() {
//		return dataId;
//	}
//
//	public static ConfigChangeEventBuilder builder() {
//		return new ConfigChangeEventBuilder();
//	}

//	public static class ConfigChangeEventBuilder {
//		private ThreadPoolConfig singleConfig;
//		private String newConfig;
//		private String dataId;
//		private String group;
//		private String configSource;
//		private long timestamp;
//		private boolean allUpdate;
//		private boolean fromAdmin;
//
//		public ConfigChangeEventBuilder singleConfig(ThreadPoolConfig singleConfig) {
//			this.singleConfig = singleConfig;
//			return this;
//		}
//
//		public ConfigChangeEventBuilder newConfig(String newConfig) {
//			this.newConfig = newConfig;
//			return this;
//		}
//
//		public ConfigChangeEventBuilder dataId(String dataId) {
//			this.dataId = dataId;
//			return this;
//		}
//
//		public ConfigChangeEventBuilder group(String group) {
//			this.group = group;
//			return this;
//		}
//
//		public ConfigChangeEventBuilder configSource(String configSource) {
//			this.configSource = configSource;
//			return this;
//		}
//
//		public ConfigChangeEventBuilder timestamp(long timestamp) {
//			this.timestamp = timestamp;
//			return this;
//		}
//
//		public ConfigChangeEventBuilder allUpdate(boolean allUpdate) {
//			this.allUpdate = allUpdate;
//			return this;
//		}
//
//		public ConfigChangeEventBuilder fromAdmin(boolean fromAdmin) {
//			this.fromAdmin = fromAdmin;
//			return this;
//		}
//
//		public ConfigChangeEvent build() {
//			ConfigChangeEvent event = new ConfigChangeEvent();
//			event.setSingleConfig(singleConfig);
//			event.setNewConfig(newConfig);
//			event.setDataId(dataId);
//			event.setGroup(group);
//			event.setConfigSource(configSource);
//			event.setTimestamp(timestamp);
//			event.setAllUpdate(allUpdate);
//			event.setFromAdmin(fromAdmin);
//			return event;
//		}
//	}

	/**
	 * 修改单个线程池参数
	 */
	private ThreadPoolConfig singleConfig;

	/**
	 * 监听新的全配置内容
	 */
	private String newConfig;

	/**
	 * 配置ID
	 */
	private String dataId;

	/**
	 *  组
	 */
	private String group;

	/**
	 * 配置源：nacos、apollo、local
	 */
	private String configSource;

	/**
	 * 时间戳
	 */
	private long timestamp;
	/**
	 * 是否全量更新
	 */
	private boolean allUpdate;

	/**
	 * 是否来自管理端变更
	 */
	private boolean fromAdmin;
//
//	public boolean isFromAdmin() {
//		return fromAdmin;
//	}
}

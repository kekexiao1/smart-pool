package com.xiao.smartpoolcore.listener.nacos;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties("spring.cloud.nacos.config")
@Data
public class NacosConfig {
	private String serverAddr;
	private String namespace;
	private String dataId;
	private String group;
	private String username;
	private String password;
}

package com.xiao.smartpoolcore.model.dto;

import lombok.Data;
import lombok.extern.slf4j.Slf4j;

import java.util.Map;

@Data
@Slf4j
public class DynamicThreadPoolProperties {

	private Map<String, ThreadPoolConfig> executors;

}

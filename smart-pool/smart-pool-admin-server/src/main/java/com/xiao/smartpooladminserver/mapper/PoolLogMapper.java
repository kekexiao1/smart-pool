package com.xiao.smartpooladminserver.mapper;

import com.xiao.smartpooladminserver.model.dto.PoolConfigLogQueryDTO;
import com.xiao.smartpoolcore.model.entity.PoolConfigLog;
import org.apache.ibatis.annotations.*;
import java.util.List;

@Mapper
public interface PoolLogMapper {

//	 @Insert("INSERT INTO pool_config_log (app_name, pool_name, old_config, new_config, change_type, operator, create_time, source) " +
//	 		"VALUES (#{appName}, #{poolName}, #{oldConfig}, #{newConfig}, #{changeType}, #{operator}, #{createTime}, #{source})")
//	 @Options(useGeneratedKeys = true, keyProperty = "id")
	int save(PoolConfigLog poolConfigLog);


	 List<PoolConfigLog> selectLogsList(PoolConfigLogQueryDTO queryDTO);

}

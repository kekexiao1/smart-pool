package com.xiao.smartpooladminserver.mapper;

import com.xiao.smartpooladminserver.model.dto.PoolAlertLogQueryDTO;
import com.xiao.smartpoolcore.model.entity.PoolAlertLog;
import org.apache.ibatis.annotations.*;

import java.util.List;

@Mapper
public interface PoolAlertLogMapper {

//	@Insert("INSERT INTO pool_alert_log (app_name, pool_name, alert_type, alert_level, content, status, create_time) " +
//			"VALUES (#{appName}, #{poolName}, #{alertType}, #{alertLevel}, #{content}, #{status}, #{createTime})")
//	@Options(useGeneratedKeys = true, keyProperty = "id")
	int save(PoolAlertLog poolAlertLog);

//	@Update("UPDATE pool_alert_log SET handler = #{handler}, handle_time = #{handleTime}, status = #{status} WHERE id = #{id}")
	int update(PoolAlertLog poolAlertLog);


	List<PoolAlertLog> selectAlertsList(PoolAlertLogQueryDTO queryDTO);

}
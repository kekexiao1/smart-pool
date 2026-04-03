package com.xiao.smartpooladminserver.mapper;

import com.xiao.smartpooladminserver.model.dto.PoolAlertLogQueryDTO;
import com.xiao.smartpoolcore.model.entity.PoolAlertLog;
import org.apache.ibatis.annotations.*;

import java.util.List;

@Mapper
public interface PoolAlertLogMapper {

	int save(PoolAlertLog poolAlertLog);

	int update(PoolAlertLog poolAlertLog);

	List<PoolAlertLog> selectAlertsList(PoolAlertLogQueryDTO queryDTO);

}
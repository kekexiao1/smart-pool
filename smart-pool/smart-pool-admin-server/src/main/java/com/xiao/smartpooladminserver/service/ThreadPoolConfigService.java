package com.xiao.smartpooladminserver.service;

import com.xiao.smartpooladminserver.model.dto.ThreadPoolConfigDTO;
import com.xiao.smartpooladminserver.model.dto.ThreadPoolConfigTemplateDTO;
import com.xiao.smartpooladminserver.model.vo.ThreadPoolConfigVO;
import org.springframework.stereotype.Service;

import java.util.*;

public interface ThreadPoolConfigService {

    /**
     * 获取所有线程池配置列表
     */
    List<ThreadPoolConfigVO> listAllConfigs();

    /**
     * 新增线程池配置
     */
    void addConfig(ThreadPoolConfigDTO configDTO);

    /**
     * 编辑线程池配置
     */
    void updateConfig(ThreadPoolConfigDTO configDTO);

    /**
     * 删除线程池配置
     */
    void deleteConfig(String threadPoolName);

    /**
     * 获取配置模板列表
     */
    List<ThreadPoolConfigTemplateDTO> getTemplates();

    /**
     * 根据模板创建配置
     */
    ThreadPoolConfigDTO createFromTemplate(String templateName, String applicationName, String environment, String threadPoolName);
}
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
    
//    /**
//     * 获取指定线程池的配置详情
//     * @param threadPoolName 线程池名称
//     * @return 配置详情
//     */
//    ThreadPoolConfigVO getConfigDetail(String threadPoolName);
//
//    /**
//     * 验证线程池配置是否有效
//     * @param configDTO 配置信息
//     * @return 验证结果
//     */
//    boolean validateConfig(ThreadPoolConfigDTO configDTO);
//
//    /**
//     * 批量导入线程池配置
//     * @param configList 配置列表
//     * @return 导入结果
//     */
//    Map<String, Object> batchImportConfigs(List<ThreadPoolConfigDTO> configList);
//
//    /**
//     * 导出线程池配置
//     * @return 配置列表
//     */
//    List<ThreadPoolConfigVO> exportConfigs();
//
//    /**
//     * 搜索线程池配置
//     * @param keyword 关键词
//     * @return 匹配的配置列表
//     */
//    List<ThreadPoolConfigVO> searchConfigs(String keyword);
}
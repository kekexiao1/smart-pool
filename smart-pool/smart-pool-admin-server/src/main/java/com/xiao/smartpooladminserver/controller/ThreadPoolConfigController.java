package com.xiao.smartpooladminserver.controller;

import com.xiao.smartpooladminserver.common.result.Result;
import com.xiao.smartpooladminserver.model.dto.ThreadPoolConfigDTO;
import com.xiao.smartpooladminserver.model.dto.ThreadPoolConfigTemplateDTO;
import com.xiao.smartpooladminserver.service.ThreadPoolConfigService;
import com.xiao.smartpooladminserver.model.vo.ThreadPoolConfigVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import javax.validation.Valid;
import java.util.List;

@RestController
@RequestMapping("/config")
@Slf4j
@RequiredArgsConstructor
@Validated
public class ThreadPoolConfigController {

    private final ThreadPoolConfigService configService;

    /**
     * 获取所有线程池配置列表
     */
    @GetMapping("/list")
    public Result<List<ThreadPoolConfigVO>> listAllConfigs() {
        try {
            List<ThreadPoolConfigVO> configs = configService.listAllConfigs();
            return Result.success(configs);
        } catch (Exception e) {
            log.error("获取线程池配置列表失败", e);
            return Result.failure("获取配置列表失败: " + e.getMessage());
        }
    }

    /**
     * 新增线程池配置
     */
    @PostMapping("/add")
    public Result<Void> addConfig(@Valid @RequestBody ThreadPoolConfigDTO configDTO) {
        try {
            configService.addConfig(configDTO);
            return Result.success();
        } catch (Exception e) {
            log.error("新增线程池配置失败", e);
            return Result.failure("新增配置失败: " + e.getMessage());
        }
    }

    /**
     * 编辑线程池配置
     */
    @PutMapping("/update")
    public Result<Void> updateConfig(@Valid @RequestBody ThreadPoolConfigDTO configDTO) {
        try {

            configService.updateConfig(configDTO);
            return Result.success();
        } catch (Exception e) {
            log.error("更新线程池配置失败", e);
            return Result.failure("更新配置失败: " + e.getMessage());
        }
    }

    /**
     * 删除线程池配置
     */
    @DeleteMapping("/delete/{threadPoolName}")
    public Result<Void> deleteConfig(@PathVariable String threadPoolName) {
        try {
            configService.deleteConfig(threadPoolName);
            return Result.success();
        } catch (Exception e) {
            log.error("删除线程池配置失败", e);
            return Result.failure("删除配置失败: " + e.getMessage());
        }
    }

    /**
     * 获取配置模板列表
     */
    @GetMapping("/templates")
    public Result<List<ThreadPoolConfigTemplateDTO>> getTemplates() {
        try {
            List<ThreadPoolConfigTemplateDTO> templates = configService.getTemplates();
            return Result.success(templates);
        } catch (Exception e) {
            log.error("获取配置模板失败", e);
            return Result.failure("获取模板失败: " + e.getMessage());
        }
    }

    /**
     * 根据模板创建配置
     */
    @PostMapping("/create-from-template")
    public Result<ThreadPoolConfigDTO> createFromTemplate(
            @RequestParam String templateName,
            @RequestParam String applicationName,
            @RequestParam String environment,
            @RequestParam String threadPoolName) {
        try {
            ThreadPoolConfigDTO configDTO = configService.createFromTemplate(
                    templateName, applicationName, environment, threadPoolName);
            return Result.success(configDTO);
        } catch (Exception e) {
            log.error("根据模板创建配置失败", e);
            return Result.failure("创建配置失败: " + e.getMessage());
        }
    }

    /**
     * 获取线程池配置详情
     */
    @GetMapping("/detail/{threadPoolName}")
    public Result<ThreadPoolConfigVO> getConfigDetail(@PathVariable String threadPoolName) {
        try {
            List<ThreadPoolConfigVO> configs = configService.listAllConfigs();
            ThreadPoolConfigVO config = configs.stream()
                    .filter(c -> c.getThreadPoolName().equals(threadPoolName))
                    .findFirst()
                    .orElse(null);
            
            if (config == null) {
                return Result.failure("配置不存在: " + threadPoolName);
            }
            
            return Result.success(config);
        } catch (Exception e) {
            log.error("获取线程池配置详情失败", e);
            return Result.failure("获取配置详情失败: " + e.getMessage());
        }
    }
}
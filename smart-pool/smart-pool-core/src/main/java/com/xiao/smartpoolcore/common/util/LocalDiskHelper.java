package com.xiao.smartpoolcore.common.util;

import com.xiao.smartpoolcore.core.task.PoolTask;
import lombok.extern.slf4j.Slf4j;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * 本地磁盘持久化工具类
 */
@Slf4j
public class LocalDiskHelper {

    private static final String REJECT_TASK_DIR = "logs/rejected-tasks";
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    private static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    /**
     * 保存被拒绝的任务到本地磁盘
     */
    public static void saveTask(Runnable task, String threadPoolName) throws IOException {
        ensureDirectoryExists();
        
        String fileName = generateFileName(threadPoolName);
        String content = generateTaskContent(task, threadPoolName);
        
        writeToFile(fileName, content);
    }

    /**
     * 确保目录存在
     */
    private static void ensureDirectoryExists() throws IOException {
        Path dirPath = Paths.get(REJECT_TASK_DIR);
        if (!Files.exists(dirPath)) {
            Files.createDirectories(dirPath);
            log.info("创建拒绝任务目录: {}", dirPath.toAbsolutePath());
        }
    }

    /**
     * 生成文件名
     */
    private static String generateFileName(String threadPoolName) {
        String dateStr = LocalDateTime.now().format(DATE_FORMATTER);
        String safePoolName = threadPoolName.replaceAll("[^a-zA-Z0-9._-]", "_");
        return String.format("%s/%s-%s.txt", REJECT_TASK_DIR, safePoolName, dateStr);
    }

    /**
     * 生成任务内容
     */
    private static String generateTaskContent(Runnable task, String threadPoolName) {
        StringBuilder sb = new StringBuilder();
        
        String timestamp = LocalDateTime.now().format(TIME_FORMATTER);
        
        sb.append("===== 拒绝任务记录 =====\n");
        sb.append("线程池: ").append(threadPoolName).append("\n");
        sb.append("时间: ").append(timestamp).append("\n");
        sb.append("任务类型: ").append(task.getClass().getName()).append("\n");
        sb.append("任务哈希: ").append(System.identityHashCode(task)).append("\n");
        
        // 尝试获取任务详细信息
        try {
            if (task instanceof PoolTask) {
               PoolTask poolTask = (PoolTask) task;
                sb.append("任务ID: ").append(poolTask.getTaskId()).append("\n");
                sb.append("任务类型: ").append(poolTask.getTaskType()).append("\n");
                sb.append("业务类型: ").append(poolTask.getBusinessType()).append("\n");
                sb.append("原始线程池: ").append(poolTask.getOriginalPoolName()).append("\n");
                sb.append("任务参数: ").append(poolTask.getPayload()).append("\n");
                sb.append("创建时间: ").append(poolTask.getCreateTime()).append("\n");
                sb.append("重试次数: ").append(poolTask.getRetryCount().get()).append("\n");
                sb.append("最大重试次数: ").append(poolTask.getMaxRetries()).append("\n");
                sb.append("优先级: ").append(poolTask.getPriority()).append("\n");
                sb.append("追踪ID: ").append(poolTask.getTraceId()).append("\n");
                sb.append("有效期: ").append(poolTask.getTtl()).append("\n");
                sb.append("异常堆栈: ").append(poolTask.getExceptionStack()).append("\n");
            }
        } catch (Exception e) {
            sb.append("任务详情解析失败: ").append(e.getMessage()).append("\n");
        }
        
        sb.append("=== 结束 ===\n\n");
        
        return sb.toString();
    }

    /**
     * 写入文件
     */
    private static void writeToFile(String fileName, String content) throws IOException {
        File file = new File(fileName);
        
        try (FileOutputStream fos = new FileOutputStream(file, true);
             OutputStreamWriter writer = new OutputStreamWriter(fos, StandardCharsets.UTF_8)) {
            
            writer.write(content);
            writer.flush();
            
        } catch (IOException e) {
            log.error("写入拒绝任务文件失败: {}", fileName, e);
            throw e;
        }
    }

    /**
     * 清理过期文件（可定期调用）
     */
    public static void cleanupOldFiles(int daysToKeep) {
        try {
            File dir = new File(REJECT_TASK_DIR);
            if (!dir.exists()) {
                return;
            }
            
            File[] files = dir.listFiles();
            if (files == null) {
                return;
            }
            
            long cutoffTime = System.currentTimeMillis() - (daysToKeep * 24L * 60 * 60 * 1000);
            
            for (File file : files) {
                if (file.isFile() && file.lastModified() < cutoffTime) {
                    if (file.delete()) {
                        log.info("删除过期拒绝任务文件: {}", file.getName());
                    } else {
                        log.warn("删除过期拒绝任务文件失败: {}", file.getName());
                    }
                }
            }
            
        } catch (Exception e) {
            log.error("清理过期拒绝任务文件失败: {}", e.getMessage());
        }
    }
}
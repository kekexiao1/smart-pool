package com.xiao.smartpoolcore.common.util;

import com.alibaba.fastjson.JSON;
import lombok.extern.slf4j.Slf4j;

import java.io.*;

/**
 * 任务序列化验证工具类
 * 用于验证任务是否可以被正确序列化和反序列化
 */
@Slf4j
public class TaskSerializationValidator {

    /**
     * 验证任务是否可序列化
     */
    public static boolean isSerializable(Runnable task) {
        if (task == null) {
            return false;
        }

        // 检查类是否实现Serializable接口
        if (!(task instanceof Serializable)) {
            log.warn("任务类 {} 未实现Serializable接口", task.getClass().getName());
            return false;
        }

        // 尝试Java序列化
        boolean javaSerializable = testJavaSerialization(task);
        
        // 尝试JSON序列化
        boolean jsonSerializable = testJsonSerialization(task);
        
        return javaSerializable || jsonSerializable;
    }

    /**
     * 测试Java序列化
     */
    private static boolean testJavaSerialization(Runnable task) {
        try {
            // 序列化
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            try (ObjectOutputStream oos = new ObjectOutputStream(baos)) {
                oos.writeObject(task);
                oos.flush();
            }

            byte[] serializedData = baos.toByteArray();
            
            // 反序列化
            ByteArrayInputStream bais = new ByteArrayInputStream(serializedData);
            try (ObjectInputStream ois = new ObjectInputStream(bais)) {
                Object deserializedTask = ois.readObject();
                
                // 验证反序列化后的对象类型
                if (deserializedTask != null && deserializedTask.getClass().equals(task.getClass())) {
                    log.debug("任务Java序列化验证成功: {}", task.getClass().getName());
                    return true;
                }
            }
            
        } catch (Exception e) {
            log.debug("任务Java序列化失败: {}, 错误: {}", task.getClass().getName(), e.getMessage());
        }
        
        return false;
    }

    /**
     * 测试JSON序列化
     */
    private static boolean testJsonSerialization(Runnable task) {
        try {
            // JSON序列化
            String jsonString = JSON.toJSONString(task);
            
            if (jsonString == null || jsonString.trim().isEmpty()) {
                return false;
            }
            
            // 验证JSON格式
            if (!isValidJson(jsonString)) {
                return false;
            }
            
            log.debug("任务JSON序列化验证成功: {}", task.getClass().getName());
            return true;
            
        } catch (Exception e) {
            log.debug("任务JSON序列化失败: {}, 错误: {}", task.getClass().getName(), e.getMessage());
        }
        
        return false;
    }

    /**
     * 验证JSON字符串格式
     */
    private static boolean isValidJson(String jsonString) {
        if (jsonString == null || jsonString.trim().isEmpty()) {
            return false;
        }
        
        try {
            // 尝试解析JSON
            Object parsed = JSON.parse(jsonString);
            return parsed != null;
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * 获取任务序列化信息
     */
    public static SerializationInfo getSerializationInfo(Runnable task) {
        SerializationInfo info = new SerializationInfo();
        
        if (task == null) {
            info.setSerializable(false);
            info.setErrorMessage("任务为空");
            return info;
        }

        info.setTaskClass(task.getClass().getName());
        info.setImplementsSerializable(task instanceof Serializable);
        
        if (!info.isImplementsSerializable()) {
            info.setSerializable(false);
            info.setErrorMessage("任务类未实现Serializable接口");
            return info;
        }

        // 测试Java序列化
        try {
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            try (ObjectOutputStream oos = new ObjectOutputStream(baos)) {
                oos.writeObject(task);
                oos.flush();
            }
            
            byte[] data = baos.toByteArray();
            info.setJavaSerializable(true);
            info.setJavaSerializedSize(data.length);
            
        } catch (Exception e) {
            info.setJavaSerializable(false);
            info.setJavaSerializationError(e.getMessage());
        }

        // 测试JSON序列化
        try {
            String jsonString = JSON.toJSONString(task);
            info.setJsonSerializable(jsonString != null && !jsonString.trim().isEmpty());
            info.setJsonSerializedSize(jsonString != null ? jsonString.length() : 0);
            
        } catch (Exception e) {
            info.setJsonSerializable(false);
            info.setJsonSerializationError(e.getMessage());
        }

        info.setSerializable(info.isJavaSerializable() || info.isJsonSerializable());
        
        if (!info.isSerializable()) {
            info.setErrorMessage("任务无法通过任何序列化方式");
        }

        return info;
    }

    /**
     * 序列化信息类
     */
    public static class SerializationInfo {
        private String taskClass;
        private boolean implementsSerializable;
        private boolean serializable;
        private boolean javaSerializable;
        private boolean jsonSerializable;
        private int javaSerializedSize;
        private int jsonSerializedSize;
        private String javaSerializationError;
        private String jsonSerializationError;
        private String errorMessage;

        // Getters and Setters
        public String getTaskClass() { return taskClass; }
        public void setTaskClass(String taskClass) { this.taskClass = taskClass; }
        
        public boolean isImplementsSerializable() { return implementsSerializable; }
        public void setImplementsSerializable(boolean implementsSerializable) { this.implementsSerializable = implementsSerializable; }
        
        public boolean isSerializable() { return serializable; }
        public void setSerializable(boolean serializable) { this.serializable = serializable; }
        
        public boolean isJavaSerializable() { return javaSerializable; }
        public void setJavaSerializable(boolean javaSerializable) { this.javaSerializable = javaSerializable; }
        
        public boolean isJsonSerializable() { return jsonSerializable; }
        public void setJsonSerializable(boolean jsonSerializable) { this.jsonSerializable = jsonSerializable; }
        
        public int getJavaSerializedSize() { return javaSerializedSize; }
        public void setJavaSerializedSize(int javaSerializedSize) { this.javaSerializedSize = javaSerializedSize; }
        
        public int getJsonSerializedSize() { return jsonSerializedSize; }
        public void setJsonSerializedSize(int jsonSerializedSize) { this.jsonSerializedSize = jsonSerializedSize; }
        
        public String getJavaSerializationError() { return javaSerializationError; }
        public void setJavaSerializationError(String javaSerializationError) { this.javaSerializationError = javaSerializationError; }
        
        public String getJsonSerializationError() { return jsonSerializationError; }
        public void setJsonSerializationError(String jsonSerializationError) { this.jsonSerializationError = jsonSerializationError; }
        
        public String getErrorMessage() { return errorMessage; }
        public void setErrorMessage(String errorMessage) { this.errorMessage = errorMessage; }

        @Override
        public String toString() {
            return "SerializationInfo{" +
                    "taskClass='" + taskClass + '\'' +
                    ", implementsSerializable=" + implementsSerializable +
                    ", serializable=" + serializable +
                    ", javaSerializable=" + javaSerializable +
                    ", jsonSerializable=" + jsonSerializable +
                    ", javaSerializedSize=" + javaSerializedSize +
                    ", jsonSerializedSize=" + jsonSerializedSize +
                    ", javaSerializationError='" + javaSerializationError + '\'' +
                    ", jsonSerializationError='" + jsonSerializationError + '\'' +
                    ", errorMessage='" + errorMessage + '\'' +
                    '}';
        }
    }
}
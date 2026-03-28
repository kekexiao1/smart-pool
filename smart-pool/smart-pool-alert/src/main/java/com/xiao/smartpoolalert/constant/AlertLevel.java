package com.xiao.smartpoolalert.constant;

/**
 * 告警等级枚举
 */
public enum AlertLevel {
    
    INFO("INFO", "信息", 1),
    WARNING("WARNING", "警告", 2),
    ERROR("ERROR", "错误", 3),
    CRITICAL("CRITICAL", "严重", 4);
    
    private final String code;
    private final String description;
    private final int level;
    
    AlertLevel(String code, String description, int level) {
        this.code = code;
        this.description = description;
        this.level = level;
    }
    
    public String getCode() {
        return code;
    }
    
    public String getDescription() {
        return description;
    }
    
    public int getLevel() {
        return level;
    }
    
    /**
     * 根据code获取枚举
     */
    public static AlertLevel getByCode(String code) {
        for (AlertLevel level : values()) {
            if (level.getCode().equalsIgnoreCase(code)) {
                return level;
            }
        }
        return WARNING; // 默认返回WARNING
    }
    
    /**
     * 根据level获取枚举
     */
    public static AlertLevel getByLevel(int level) {
        for (AlertLevel alertLevel : values()) {
            if (alertLevel.getLevel() == level) {
                return alertLevel;
            }
        }
        return WARNING; // 默认返回WARNING
    }
}
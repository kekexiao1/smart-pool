package com.xiao.smartpoolalert.context;

import lombok.AllArgsConstructor;
import lombok.Data;

/**
 * 带时间戳的数值
 * 用于滑动窗口统计中的过期数据清理
 */
@Data
@AllArgsConstructor
public class TimestampedValue {
    
    /**
     * 数值
     */
    private double value;
    
    /**
     * 时间戳（毫秒）
     */
    private long timestamp;
    
    /**
     * 创建带时间戳的数值
     * @param value 数值
     * @return TimestampedValue 实例
     */
    public static TimestampedValue of(double value) {
        return new TimestampedValue(value, System.currentTimeMillis());
    }
}
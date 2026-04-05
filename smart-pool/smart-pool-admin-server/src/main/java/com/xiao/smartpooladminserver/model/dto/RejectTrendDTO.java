package com.xiao.smartpooladminserver.model.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class RejectTrendDTO {
    private long currentValue;          // 当前拒绝任务数
    private long historyValue;          // 历史拒绝任务数
    private long change;                // 变化量
    private double changePercentage;    // 变化百分比
}
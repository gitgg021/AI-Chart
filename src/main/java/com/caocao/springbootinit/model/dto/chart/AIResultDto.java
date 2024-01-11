package com.caocao.springbootinit.model.dto.chart;

import lombok.Data;

@Data
public class AIResultDto {
    /**
     * 图表id，需要保存成功后才有
     */
    private Long chartId;
    /**
     * 图像数据
     */
    private String chartData;
    /**
     * 分析结果
     */
    private String onAnalysis;
}

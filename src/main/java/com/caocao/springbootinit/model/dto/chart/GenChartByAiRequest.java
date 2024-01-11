package com.caocao.springbootinit.model.dto.chart;

import lombok.Data;

import java.io.Serializable;

/**
 * 文件上传请求
 *
 */
@Data
public class GenChartByAiRequest implements Serializable {

    /**
     * 图标名称
     */
    private String name;

    /**
     * 分析目标
     */
    private String goal;


    /**
     * 图标类型
     */
    private String chartType;

    private static final long serialVersionUID = 1L;
}
package com.caocao.springbootinit.service;

import com.caocao.springbootinit.model.entity.Chart;
import com.baomidou.mybatisplus.extension.service.IService;
import lombok.extern.slf4j.Slf4j;

/**
* @author caocao
* @description 针对表【chart(图表信息表)】的数据库操作Service
* @createDate 2024-01-05 17:55:30
*/

public interface ChartService extends IService<Chart> {
    //定义一个异常工具类
    public void handleChartUpdateError(long chartId, String execMessage);
}

package com.caocao.springbootinit.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.caocao.springbootinit.model.entity.Chart;
import com.caocao.springbootinit.service.ChartService;
import com.caocao.springbootinit.mapper.ChartMapper;
import org.springframework.stereotype.Service;

/**
* @author caocao
* @description 针对表【chart(图表信息表)】的数据库操作Service实现
* @createDate 2024-01-05 17:55:30
*/
@Service
public class ChartServiceImpl extends ServiceImpl<ChartMapper, Chart>
    implements ChartService{

    @Override
    public void handleChartUpdateError(long chartId, String execMessage) {
        Chart updateChartResult = new Chart();
        updateChartResult.setId(chartId);
        updateChartResult.setStatus("failed");
        updateChartResult.setExecMessage(execMessage);
        boolean updateResult = updateById(updateChartResult);
        if (!updateResult) {
            log.error("更新图表失败" + chartId + "," + execMessage);
        }
    }
    }






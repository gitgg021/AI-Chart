package com.caocao.springbootinit.mq;

import com.caocao.springbootinit.common.ErrorCode;
import com.caocao.springbootinit.exception.BusinessException;
import com.caocao.springbootinit.manager.RedisLimiterManager;
import com.caocao.springbootinit.model.dto.chart.AIResultDto;
import com.caocao.springbootinit.model.entity.Chart;
import com.caocao.springbootinit.service.ChartService;
import com.caocao.springbootinit.utils.AiUtils;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.rabbitmq.client.Channel;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.redisson.api.RedissonClient;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.support.AmqpHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.io.IOException;

@Component
@Slf4j
public class BiConsumer {
    @Resource
    ChartService chartService;
    @Resource
    RedisLimiterManager redisLimiterManager;
    @Resource
    RedissonClient redissonClient;
    @RabbitListener(queues = {"bi_queue"},ackMode = "MANUAL")
    public void receiveMessage(String message, Channel channel, @Header(AmqpHeaders.DELIVERY_TAG) long deliveryTag) throws JsonProcessingException {
        if (StringUtils.isBlank(message)) {
            log.error("信息为空");
            //空消息是没有价值的，直接确认
            try {
                channel.basicAck(deliveryTag, false);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
            return;
        }
        //解析消息
        String msg[] = message.split(",");
        //从数据库查询当前这个图表
        Chart chart = chartService.getById(Long.valueOf(msg[0]));
        String goal = chart.getGoal();
        String data = chart.getChartData();
        String chartType = chart.getChartType();

        //ai提问
        StringBuffer res = new StringBuffer();
        res.append("你是一个数据分析师和前端开发专家，接下来我会按照以下固定格式给你提供内容：");
        res.append("\n").append("分析需求：").append("\n").append("{").append(goal).append("}").append("\n");
        res.append("原始数据:").append("\n").append(data);
        res.append("请根据这两部分内容，按照以下指定格式生成内容（此外不要输出任何多余的开头、结尾、注释）\n【【【【【\n先输出上面原始数据的分析结果：\n然后输出【【【【【\n{(JSON格式代码) 前端 Echarts V5 option 配置对象 JSON ,生成}");
        res.append(chartType);
        res.append("合理地将数据进行可视化，不要生成任何多余的内容，不要注释}");

        chart.setStatus("running");
        boolean update = chartService.updateById(chart);
        if (!update) {
            try {
                channel.basicNack(deliveryTag, false, true);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
            chartService.handleChartUpdateError(chart.getId(), "更新图表失败");
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "更新图表失败");
        }
        //限流
        redisLimiterManager.doRateLimit("aiLimiter:" + msg[1]);
        AiUtils aiUtils = new AiUtils(redissonClient);
        AIResultDto ans = aiUtils.getAns(chart.getId(), res.toString());
        String chartData = ans.getChartData();
        String onAnalysis = ans.getOnAnalysis();
        if (!chartData.equals("服务错误") && !onAnalysis.equals("服务错误")) {
            Chart succeedChart = new Chart();
            succeedChart.setId(chart.getId());
            succeedChart.setStatus("succeed");
            succeedChart.setGenChart(chartData);
            succeedChart.setGenResult(onAnalysis);
            boolean success = chartService.updateById(succeedChart);

            if (!success) {
                try {
                    channel.basicNack(deliveryTag, false, false);
                } catch (IOException e) {
                    e.printStackTrace();
                }
                chartService.handleChartUpdateError(chart.getId(), "更新图表成功状态失败");
            }

            try {
                channel.basicAck(deliveryTag, false);
            } catch (IOException e) {
                e.printStackTrace();
            }


        }
    }
}




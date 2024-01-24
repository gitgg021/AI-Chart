package com.caocao.springbootinit.utils;

import com.caocao.springbootinit.model.dto.chart.AIResultDto;
import com.fasterxml.jackson.core.JsonProcessingException;
import org.redisson.api.RedissonClient;


public class AiUtils {
    private final RedissonClient redissonClient;

    public AiUtils(RedissonClient redissonClient) {
        this.redissonClient = redissonClient;
    }

    public AIResultDto getAns(long chartId, String question) throws JsonProcessingException {
        BigModelChar bigModelChar = new BigModelChar(chartId,redissonClient);
        bigModelChar.getResult(question);
        String aReturn = bigModelChar.getReturn();
        String chartData = "服务错误";
        String onAnalysis = "服务错误";
        //这段代码的作用是从字符串aReturn中截取一部分子字符串，并将其赋值给变量onAnalysis。具体来说，它会找到第一个冒号（"："）的位置，然后从该位置的下一个字符开始截取，一直截取到字符串"然后输出【【【【【"之前的位置。
        if(aReturn.contains("：") && aReturn.contains("然后输出【【【【【"))
            onAnalysis = aReturn.substring(aReturn.indexOf("：") + 1,aReturn.indexOf("然后输出【【【【【"));
        String[] split = aReturn.split("```json");
        if(split.length == 2){
             chartData = split[1].substring(0, split[1].indexOf("```"));
        }


     /*   if(split.length == 2){
            String chartDataStrNotJSON = split[1].substring(0, split[1].indexOf("```"));
            ObjectMapper objectMapper = new ObjectMapper();
            JsonNode chartDataJson = objectMapper.readTree(chartDataStrNotJSON);
            chartData = chartDataJson.toString();
        }*/

        AIResultDto aiResultDto = new AIResultDto();
        aiResultDto.setChartData(chartData);
        aiResultDto.setOnAnalysis(onAnalysis.trim());

        return aiResultDto;
    }
}


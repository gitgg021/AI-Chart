package com.caocao.springbootinit.controller;

import cn.hutool.core.io.FileUtil;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.caocao.springbootinit.annotation.AuthCheck;
import com.caocao.springbootinit.common.BaseResponse;
import com.caocao.springbootinit.common.DeleteRequest;
import com.caocao.springbootinit.common.ErrorCode;
import com.caocao.springbootinit.common.ResultUtils;
import com.caocao.springbootinit.constant.CommonConstant;
import com.caocao.springbootinit.constant.UserConstant;
import com.caocao.springbootinit.exception.BusinessException;
import com.caocao.springbootinit.exception.ThrowUtils;
import com.caocao.springbootinit.manager.RedisLimiterManager;
import com.caocao.springbootinit.model.dto.chart.*;
import com.caocao.springbootinit.model.entity.Chart;
import com.caocao.springbootinit.model.entity.User;
import com.caocao.springbootinit.mq.MyMessageProducer;
import com.caocao.springbootinit.service.ChartService;
import com.caocao.springbootinit.service.UserService;
import com.caocao.springbootinit.utils.AiUtils;
import com.caocao.springbootinit.utils.ExcelUtils;
import com.caocao.springbootinit.utils.SqlUtils;
import com.fasterxml.jackson.core.JsonProcessingException;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.ObjectUtils;
import org.apache.commons.lang3.StringUtils;
import org.redisson.api.RedissonClient;
import org.springframework.beans.BeanUtils;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ThreadPoolExecutor;

/**
 * 接口
 */
@RestController
@RequestMapping("/chart")
@Slf4j
public class ChartController {

    @Resource
    private ChartService chartService;

    @Resource
    private UserService userService;

    @Resource
    RedissonClient redissonClient;

    @Resource
    private RedisLimiterManager redisLimiterManager;

    @Resource
    private ThreadPoolExecutor threadPoolExecutor;

    @Resource
    MyMessageProducer myMessageProducer;

    /**
     * 创建
     *
     * @param chartAddRequest
     * @param request
     * @return
     */
    @PostMapping("/add")
    public BaseResponse<Long> addChart(@RequestBody ChartAddRequest chartAddRequest, HttpServletRequest request) {
        if (chartAddRequest == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        Chart chart = new Chart();
        BeanUtils.copyProperties(chartAddRequest, chart);
        User loginUser = userService.getLoginUser(request);
        chart.setUserId(loginUser.getId());
        boolean result = chartService.save(chart);
        ThrowUtils.throwIf(!result, ErrorCode.OPERATION_ERROR);
        long newChartId = chart.getId();
        return ResultUtils.success(newChartId);
    }

    /**
     * 删除
     *
     * @param deleteRequest
     * @param request
     * @return
     */
    @PostMapping("/delete")
    public BaseResponse<Boolean> deleteChart(@RequestBody DeleteRequest deleteRequest, HttpServletRequest request) {
        if (deleteRequest == null || deleteRequest.getId() <= 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        User user = userService.getLoginUser(request);
        long id = deleteRequest.getId();
        // 判断是否存在
        Chart oldChart = chartService.getById(id);
        ThrowUtils.throwIf(oldChart == null, ErrorCode.NOT_FOUND_ERROR);
        // 仅本人或管理员可删除
        if (!oldChart.getUserId().equals(user.getId()) && !userService.isAdmin(request)) {
            throw new BusinessException(ErrorCode.NO_AUTH_ERROR);
        }
        boolean b = chartService.removeById(id);
        return ResultUtils.success(b);
    }

    /**
     * 更新（仅管理员）
     *
     * @param chartUpdateRequest
     * @return
     */
    @PostMapping("/update")
    @AuthCheck(mustRole = UserConstant.ADMIN_ROLE)
    public BaseResponse<Boolean> updateChart(@RequestBody ChartUpdateRequest chartUpdateRequest) {
        if (chartUpdateRequest == null || chartUpdateRequest.getId() <= 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        Chart chart = new Chart();
        BeanUtils.copyProperties(chartUpdateRequest, chart);
        long id = chartUpdateRequest.getId();
        // 判断是否存在
        Chart oldChart = chartService.getById(id);
        ThrowUtils.throwIf(oldChart == null, ErrorCode.NOT_FOUND_ERROR);
        boolean result = chartService.updateById(chart);
        return ResultUtils.success(result);
    }

    /**
     * 根据 id 获取
     *
     * @param id
     * @return
     */
    @GetMapping("/get")
    public BaseResponse<Chart> getChartById(long id, HttpServletRequest request) {
        if (id <= 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        Chart chart = chartService.getById(id);
        if (chart == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND_ERROR);
        }
        return ResultUtils.success(chart);
    }

    /**
     * 分页获取列表（仅管理员）
     *
     * @param chartQueryRequest
     * @return
     */
    @PostMapping("/list/page")
    @AuthCheck(mustRole = UserConstant.ADMIN_ROLE)
    public BaseResponse<Page<Chart>> listChartByPage(@RequestBody ChartQueryRequest chartQueryRequest) {
        long current = chartQueryRequest.getCurrent();
        long size = chartQueryRequest.getPageSize();
        Page<Chart> chartPage = chartService.page(new Page<>(current, size),
                getQueryWrapper(chartQueryRequest));
        return ResultUtils.success(chartPage);
    }

    /**
     * 分页获取列表（封装类）
     *
     * @param chartQueryRequest
     * @param request
     * @return
     */
    @PostMapping("/list/page/vo")
    public BaseResponse<Page<Chart>> listChartByPage(@RequestBody ChartQueryRequest chartQueryRequest,
                                                     HttpServletRequest request) {
        long current = chartQueryRequest.getCurrent();
        long size = chartQueryRequest.getPageSize();
        // 限制爬虫
        ThrowUtils.throwIf(size > 20, ErrorCode.PARAMS_ERROR);
        Page<Chart> chartPage = chartService.page(new Page<>(current, size),
                getQueryWrapper(chartQueryRequest));
        return ResultUtils.success(chartPage);
    }

    /**
     * 分页获取当前用户创建的资源列表
     *
     * @param chartQueryRequest
     * @param request
     * @return
     */
    @PostMapping("/my/list/page/vo")
    public BaseResponse<Page<Chart>> listMyChartByPage(@RequestBody ChartQueryRequest chartQueryRequest,
                                                       HttpServletRequest request) {
        if (chartQueryRequest == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        User loginUser = userService.getLoginUser(request);
        chartQueryRequest.setUserId(loginUser.getId());
        long current = chartQueryRequest.getCurrent();
        long size = chartQueryRequest.getPageSize();
        // 限制爬虫
        ThrowUtils.throwIf(size > 20, ErrorCode.PARAMS_ERROR);
        Page<Chart> chartPage = chartService.page(new Page<>(current, size),
                getQueryWrapper(chartQueryRequest));
        return ResultUtils.success(chartPage);
    }

    // endregion


    /**
     * 编辑（用户）
     *
     * @param chartEditRequest
     * @param request
     * @return
     */
    @PostMapping("/edit")
    public BaseResponse<Boolean> editChart(@RequestBody ChartEditRequest chartEditRequest, HttpServletRequest request) {
        if (chartEditRequest == null || chartEditRequest.getId() <= 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        Chart chart = new Chart();
        BeanUtils.copyProperties(chartEditRequest, chart);
        User loginUser = userService.getLoginUser(request);
        long id = chartEditRequest.getId();
        // 判断是否存在
        Chart oldChart = chartService.getById(id);
        ThrowUtils.throwIf(oldChart == null, ErrorCode.NOT_FOUND_ERROR);
        // 仅本人或管理员可编辑
        if (!oldChart.getUserId().equals(loginUser.getId()) && !userService.isAdmin(loginUser)) {
            throw new BusinessException(ErrorCode.NO_AUTH_ERROR);
        }
        boolean result = chartService.updateById(chart);
        return ResultUtils.success(result);
    }

    /**
     * 获取查询包装类
     *
     * @param chartQueryRequest
     * @return
     */

    private QueryWrapper<Chart> getQueryWrapper(ChartQueryRequest chartQueryRequest) {
        QueryWrapper<Chart> queryWrapper = new QueryWrapper<>();
        if (chartQueryRequest == null) {
            return queryWrapper;
        }

        Long id = chartQueryRequest.getId();
        String name = chartQueryRequest.getName();
        String goal = chartQueryRequest.getGoal();
        String chartType = chartQueryRequest.getChartType();
        Long userId = chartQueryRequest.getUserId();
        String sortField = chartQueryRequest.getSortField();
        String sortOrder = chartQueryRequest.getSortOrder();

        queryWrapper.eq(id != null && id > 0, "id", id);
        queryWrapper.like(StringUtils.isNotBlank(name), "name", name);
        queryWrapper.eq(StringUtils.isNotBlank(goal), "goal", goal);
        queryWrapper.like(StringUtils.isNotBlank(chartType), "chartType", chartType);
        queryWrapper.eq(ObjectUtils.isNotEmpty(userId), "userId", userId);
        queryWrapper.eq("isDelete", false);
        queryWrapper.orderBy(SqlUtils.validSortField(sortField), sortOrder.equals(CommonConstant.SORT_ORDER_ASC),
                sortField);
        return queryWrapper;
    }

    /**
     * 智能分析(同步)
     *
     * @param multipartFile
     * @param genChartByAiRequest
     * @param request
     * @return
     */
    @PostMapping("/gen")
    public BaseResponse<AIResultDto> getChartByAi(@RequestPart("file") MultipartFile multipartFile,
                                                  GenChartByAiRequest genChartByAiRequest, HttpServletRequest request) throws JsonProcessingException {
        //通过response对象拿到用户id(必须登录才能使用)
        User loginUser = userService.getLoginUser(request);
        String name = genChartByAiRequest.getName();
        String goal = genChartByAiRequest.getGoal();
        String chartType = genChartByAiRequest.getChartType();


        //校验
        //分析目标为空,就抛出请求参数异常,并给出提示
        ThrowUtils.throwIf(StringUtils.isBlank(goal), ErrorCode.PARAMS_ERROR, "目标为空");
        //如果不为空,并且名称长度大于100,就抛出异常,并给出提示
        ThrowUtils.throwIf(StringUtils.isNotBlank(name) && name.length() > 100, ErrorCode.PARAMS_ERROR, "名称过长");

        //校验文件
        //判断大小是否超过1MB
        final long ONE_MB = 1024 * 1024L;
        long size = multipartFile.getSize();
        ThrowUtils.throwIf(size > ONE_MB, ErrorCode.PARAMS_ERROR, "文件过大");

        //判断文件类型
        String originalFilename = multipartFile.getOriginalFilename();
        String suffix = FileUtil.getSuffix(originalFilename);
        ThrowUtils.throwIf(StringUtils.isBlank(suffix), ErrorCode.PARAMS_ERROR, "文件名异常");
        boolean isExcel = suffix.equals("xlsx") || suffix.equals("xls");
        ThrowUtils.throwIf(!isExcel, ErrorCode.PARAMS_ERROR, "文件类型错误");

        //限流判断,每个用户一个限流器
        redisLimiterManager.doRateLimit("genChartByAi_" + loginUser.getId());


        //根据用户上传的数据，压缩ai提问语
        StringBuffer res = new StringBuffer();
        res.append("你是一个数据分析师和前端开发专家，接下来我会按照以下固定格式给你提供内容：");
        res.append("\n").append("分析需求：").append("\n").append("{").append(goal).append("}").append("\n");

        String data = ExcelUtils.excelToCsv(multipartFile);
        res.append("原始数据:").append("\n").append(data);
        res.append("请根据这两部分内容，按照以下指定格式生成内容（此外不要输出任何多余的开头、结尾、注释）\n【【【【【\n先输出上面原始数据的分析结果：\n然后输出【【【【【\n{前端 Echarts V5 的 option 配置对象JSON代码，生成");
        res.append(chartType);
        res.append("合理地将数据进行可视化，不要生成任何多余的内容，不要注释}");

        Chart chart = new Chart();
        chart.setName(name);
        chart.setGoal(goal);
        chart.setChartData(data);
        chart.setChartType(chartType);
        chart.setUserId(loginUser.getId());
        chart.setStatus("succeed");


        //将创建的图表保存到数据库
        boolean save = chartService.save(chart);
        ThrowUtils.throwIf(!save, ErrorCode.SYSTEM_ERROR, "图表保存失败");

        //调用ai
        AiUtils aiUtils = new AiUtils(redissonClient);
        AIResultDto ans = aiUtils.getAns(chart.getId(), res.toString());

        chart.setGenChart(ans.getChartData());
        chart.setGenResult(ans.getOnAnalysis());

        save = chartService.updateById(chart);
        ThrowUtils.throwIf(!save, ErrorCode.SYSTEM_ERROR, "图表状态更新失败");
        ans.setChartId(chart.getId());
        return ResultUtils.success(ans);
    }

    /**
     * 智能分析(异步)
     *
     * @param multipartFile
     * @param genChartByAiRequest
     * @param request
     * @return
     */
    @PostMapping("/gen/async")
    public BaseResponse<AIResultDto> getChartByAiAsync(@RequestPart("file") MultipartFile multipartFile,
                                                       GenChartByAiRequest genChartByAiRequest, HttpServletRequest request) {
        //通过response对象拿到用户id(必须登录才能使用)
        User loginUser = userService.getLoginUser(request);
        String name = genChartByAiRequest.getName();
        String goal = genChartByAiRequest.getGoal();
        String chartType = genChartByAiRequest.getChartType();


        //校验
        //分析目标为空,就抛出请求参数异常,并给出提示
        ThrowUtils.throwIf(StringUtils.isBlank(goal), ErrorCode.PARAMS_ERROR, "目标为空");
        //如果不为空,并且名称长度大于100,就抛出异常,并给出提示
        ThrowUtils.throwIf(StringUtils.isNotBlank(name) && name.length() > 100, ErrorCode.PARAMS_ERROR, "名称过长");
        /**
         * 校验文件
         * 首先,拿到用户请求的文件
         * 取到原始文件大小
         */
        //判断大小是否超过1MB
        final long ONE_MB = 1024 * 1024L;
        long size = multipartFile.getSize();
        ThrowUtils.throwIf(size > ONE_MB, ErrorCode.PARAMS_ERROR, "文件过大");

        //判断文件类型
        String originalFilename = multipartFile.getOriginalFilename();
        String suffix = FileUtil.getSuffix(originalFilename);
        ThrowUtils.throwIf(StringUtils.isBlank(suffix), ErrorCode.PARAMS_ERROR, "文件名异常");
        boolean isExcel = suffix.equals("xlsx") || suffix.equals("xls");
        ThrowUtils.throwIf(!isExcel, ErrorCode.PARAMS_ERROR, "文件类型错误");

        //限流判断,每个用户一个限流器
        redisLimiterManager.doRateLimit("genChartByAi_" + loginUser.getId());


        //根据用户上传的数据，压缩ai提问语
        StringBuffer res = new StringBuffer();
        res.append("你是一个数据分析师和前端开发专家，接下来我会按照以下固定格式给你提供内容：");
        res.append("\n").append("分析需求：").append("\n").append("{").append(goal).append("}").append("\n");

        String data = ExcelUtils.excelToCsv(multipartFile);
        res.append("原始数据:").append("\n").append(data);
        res.append("请根据这两部分内容，按照以下指定格式生成内容（此外不要输出任何多余的开头、结尾、注释）\n【【【【【\n先输出上面原始数据的分析结果：\n然后输出【【【【【\n{前端 Echarts V5 的 option 配置对象JSON代码，生成");
        res.append(chartType);
        res.append("合理地将数据进行可视化，不要生成任何多余的内容，不要注释}");

        Chart chart = new Chart();
        chart.setName(name);
        chart.setGoal(goal);
        chart.setChartData(data);
        chart.setChartType(chartType);

        //设置任务状态为排队中
        chart.setStatus("wait");
        chart.setUserId(loginUser.getId());

        //将创建的图表保存到数据库
        boolean save = chartService.save(chart);
        ThrowUtils.throwIf(!save, ErrorCode.SYSTEM_ERROR, "图表保存失败");


        //使用线程池执行任务
        //在最终的返回结果提交一个任务
        //todo 处理任务队列满了后抛异常
        CompletableFuture.runAsync(() -> {
            //先修改图表任务状态为 “执行中”。等执行成功后，修改为 “已完成”、保存执行结果；执行失败后，状态修改为 “失败”，记录任务失败信息。(为了防止同一个任务被多次执行)
            Chart updateChart = new Chart();
            updateChart.setId(chart.getId());
            //把任务状态改为执行中
            updateChart.setStatus("running");
            boolean update = chartService.updateById(updateChart);
            //如果提交失败(一般情况下,更新失败可能意味着你的数据库出问题了)
            if (!update) {
                chartService.handleChartUpdateError(chart.getId(), "更新图表执行中状态失败");
                return;
            }
            //调用ai
            AiUtils aiUtils = new AiUtils(redissonClient);
            AIResultDto ans = null;
            try {
                ans = aiUtils.getAns(chart.getId(), res.toString());
            } catch (JsonProcessingException e) {
                e.printStackTrace();
            }

            //调用AI得到结果之后,再更新一次
            Chart updateChartResult = new Chart();
            updateChartResult.setId(chart.getId());
            updateChartResult.setGenChart(ans.getChartData());
            updateChartResult.setGenResult(ans.getOnAnalysis());
            updateChartResult.setStatus("succeed");
            boolean updateById = chartService.updateById(updateChartResult);
            if (!updateById) {
                chartService.handleChartUpdateError(chart.getId(), "更新图表失败");
            }
        }, threadPoolExecutor);


        AIResultDto ans = new AIResultDto();
        ans.setChartId(chart.getId());
        return ResultUtils.success(ans);
    }


    /**
     * 智能分析(消息队列异步)
     *
     * @param multipartFile
     * @param genChartByAiRequest
     * @param request
     * @return
     */
    @PostMapping("/gen/async/mq")
    public BaseResponse<AIResultDto> genChartByAiAsyncMq(@RequestPart("file") MultipartFile multipartFile,
                                                         GenChartByAiRequest genChartByAiRequest, HttpServletRequest request) {
        //通过response对象拿到用户id(必须登录才能使用)
        User loginUser = userService.getLoginUser(request);
        String name = genChartByAiRequest.getName();
        String goal = genChartByAiRequest.getGoal();
        String chartType = genChartByAiRequest.getChartType();


        //校验
        //分析目标为空,就抛出请求参数异常,并给出提示
        ThrowUtils.throwIf(StringUtils.isBlank(goal), ErrorCode.PARAMS_ERROR, "目标为空");
        //如果不为空,并且名称长度大于100,就抛出异常,并给出提示
        ThrowUtils.throwIf(StringUtils.isNotBlank(name) && name.length() > 100, ErrorCode.PARAMS_ERROR, "名称过长");
        /**
         * 校验文件
         * 首先,拿到用户请求的文件
         * 取到原始文件大小
         */
        //判断大小是否超过1MB
        final long ONE_MB = 1024 * 1024L;
        long size = multipartFile.getSize();
        ThrowUtils.throwIf(size > ONE_MB, ErrorCode.PARAMS_ERROR, "文件过大");

        //判断文件类型
        String originalFilename = multipartFile.getOriginalFilename();
        String suffix = FileUtil.getSuffix(originalFilename);
        ThrowUtils.throwIf(StringUtils.isBlank(suffix), ErrorCode.PARAMS_ERROR, "文件名异常");
        boolean isExcel = suffix.equals("xlsx") || suffix.equals("xls");
        ThrowUtils.throwIf(!isExcel, ErrorCode.PARAMS_ERROR, "文件类型错误");


        String data = ExcelUtils.excelToCsv(multipartFile);


        Chart chart = new Chart();
        chart.setName(name);
        chart.setGoal(goal);
        chart.setChartData(data);
        chart.setChartType(chartType);

        //设置任务状态为排队中
        chart.setStatus("wait");
        chart.setUserId(loginUser.getId());

        //将创建的图表保存到数据库
        boolean save = chartService.save(chart);
        ThrowUtils.throwIf(!save, ErrorCode.SYSTEM_ERROR, "图表保存失败");


        //使用mq发送任务
        try {
            String message = chart.getId() + "," + loginUser.getId();
            myMessageProducer.sendMessage(message);

        } catch (Exception e) {
            chartService.handleChartUpdateError(chart.getId(), "Ai生成图表失败" + e.getMessage());
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "Ai生成图表失败");
        }

        AIResultDto ans = new AIResultDto();
        ans.setChartId(chart.getId());
        return ResultUtils.success(ans);
    }


}



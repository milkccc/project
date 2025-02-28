package com.xzzn.pollux.controller;

import com.xzzn.pollux.annotation.AuthCheck;
import com.xzzn.pollux.common.ResultResponse;
import com.xzzn.pollux.model.pojo.ActivationInfo;
import com.xzzn.pollux.model.pojo.AnalyseCache;
import com.xzzn.pollux.model.vo.response.analyse.AnalyseActivationResponse;
import com.xzzn.pollux.model.vo.response.analyse.AnalyseOverviewResponse;
import com.xzzn.pollux.service.AnalyseService;
import com.xzzn.pollux.utils.ResultUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;
import java.util.HashMap;

/**
 * <p>
 * 后台分析 前端控制器
 * </p>
 *
 * @author xzzn
 */
@RestController
@RequestMapping("/analyse")
@Slf4j
public class AnalyseController {

    private static final String QUARTER = "quarter";

    private static final String MONTH = "month";


    @Resource
    private AnalyseService analyseService;

    /**
     * 获取系统分析数据总览
     */
    @GetMapping("/overview")
    @AuthCheck(mustRole = "admin")
    public ResultResponse<AnalyseOverviewResponse> getAnalyseOverview(
            @RequestHeader("ACCESS-KEY") String accessKey) {
        AnalyseCache analyseCatch = analyseService.getAnalyseCache();
        AnalyseOverviewResponse overviewResponse = new AnalyseOverviewResponse();
        overviewResponse.setTotalUserNum(analyseCatch.getTotalUser());
        overviewResponse.setOnlineUserNum(analyseService.getOnlineUser());
        overviewResponse.setDataGenerateNum(analyseCatch.getGenerateData());
        overviewResponse.setDataReviewNum(analyseCatch.getReviewData());

        return ResultUtils.success(overviewResponse);
    }

    @GetMapping("/userinfo")
    @AuthCheck(mustRole = "admin")
    public ResultResponse<AnalyseActivationResponse> getAnalyseUserInfo(
            @RequestHeader("ACCESS-KEY") String accessKey) {

        AnalyseActivationResponse userActivation = new AnalyseActivationResponse();
        AnalyseCache analyseCatch = analyseService.getAnalyseCache();
        //日
        ActivationInfo dayInfo = new ActivationInfo();
        dayInfo.setDateList(analyseCatch.getDayDateList());
        dayInfo.setNumList(analyseCatch.getDayUserActiveList());
        userActivation.getResultMap().put("day", dayInfo);
        //周
        ActivationInfo weekInfo = new ActivationInfo();
        weekInfo.setDateList(analyseCatch.getWeekDateList());
        weekInfo.setNumList(analyseCatch.getWeekUserActiveList());
        userActivation.getResultMap().put("week", weekInfo);
        //月
        ActivationInfo monthInfo = new ActivationInfo();
        monthInfo.setDateList(analyseCatch.getMonthDateList());
        monthInfo.setNumList(analyseCatch.getMonthUserActiveList());
        userActivation.getResultMap().put(MONTH, monthInfo);
        //季度
        ActivationInfo quarterInfo = new ActivationInfo();
        quarterInfo.setDateList(analyseCatch.getQuarterDateList());
        quarterInfo.setNumList(analyseCatch.getQuarterUserActiveList());
        userActivation.getResultMap().put(QUARTER, quarterInfo);
        //年
        ActivationInfo yearInfo = new ActivationInfo();
        yearInfo.setDateList(analyseCatch.getYearDateList());
        yearInfo.setNumList(analyseCatch.getYearUserActiveList());
        userActivation.getResultMap().put("year", yearInfo);

        return ResultUtils.success(userActivation);
    }

    @GetMapping("/data/generate")
    @AuthCheck(mustRole = "admin")
    public ResultResponse<AnalyseActivationResponse> getAnalyseDataGenerateInfo(
            @RequestHeader("ACCESS-KEY") String accessKey) {
        AnalyseActivationResponse dataGenActivation = new AnalyseActivationResponse();
        AnalyseCache analyseCatch = analyseService.getAnalyseCache();
        //日
        ActivationInfo dayInfo = new ActivationInfo();
        dayInfo.setDateList(analyseCatch.getDayDateList());
        dayInfo.setNumList(analyseCatch.getDayDateGenList());
        dataGenActivation.getResultMap().put("day", dayInfo);
        //周
        ActivationInfo weekInfo = new ActivationInfo();
        weekInfo.setDateList(analyseCatch.getWeekDateList());
        weekInfo.setNumList(analyseCatch.getWeekDateGenList());
        dataGenActivation.getResultMap().put("week", weekInfo);
        //月
        ActivationInfo monthInfo = new ActivationInfo();
        monthInfo.setDateList(analyseCatch.getMonthDateList());
        monthInfo.setNumList(analyseCatch.getMonthDateGenList());
        dataGenActivation.getResultMap().put(MONTH, monthInfo);
        //季度
        ActivationInfo quarterInfo = new ActivationInfo();
        quarterInfo.setDateList(analyseCatch.getQuarterDateList());
        quarterInfo.setNumList(analyseCatch.getQuarterDateGenList());
        dataGenActivation.getResultMap().put(QUARTER, quarterInfo);
        //年
        ActivationInfo yearInfo = new ActivationInfo();
        yearInfo.setDateList(analyseCatch.getYearDateList());
        yearInfo.setNumList(analyseCatch.getYearDateGenList());
        dataGenActivation.getResultMap().put("year", yearInfo);

        return ResultUtils.success(dataGenActivation);
    }

    @GetMapping("/data/review")
    @AuthCheck(mustRole = "admin")
    public ResultResponse<AnalyseActivationResponse> getAnalyseDataReviewInfo(
            @RequestHeader("ACCESS-KEY") String accessKey) {
        AnalyseActivationResponse dataRevActivation = new AnalyseActivationResponse();
        AnalyseCache analyseCatch = analyseService.getAnalyseCache();
        //日
        ActivationInfo dayInfo = new ActivationInfo();
        dayInfo.setDateList(analyseCatch.getDayDateList());
        dayInfo.setNumList(analyseCatch.getDayDateRevList());
        dataRevActivation.getResultMap().put("day", dayInfo);
        //周
        ActivationInfo weekInfo = new ActivationInfo();
        weekInfo.setDateList(analyseCatch.getWeekDateList());
        weekInfo.setNumList(analyseCatch.getWeekDateRevList());
        dataRevActivation.getResultMap().put("week", weekInfo);
        //月
        ActivationInfo monthInfo = new ActivationInfo();
        monthInfo.setDateList(analyseCatch.getMonthDateList());
        monthInfo.setNumList(analyseCatch.getMonthDateRevList());
        dataRevActivation.getResultMap().put(MONTH, monthInfo);
        //季度
        ActivationInfo quarterInfo = new ActivationInfo();
        quarterInfo.setDateList(analyseCatch.getQuarterDateList());
        quarterInfo.setNumList(analyseCatch.getQuarterDateRevList());
        dataRevActivation.getResultMap().put(QUARTER, quarterInfo);
        //年
        ActivationInfo yearInfo = new ActivationInfo();
        yearInfo.setDateList(analyseCatch.getYearDateList());
        yearInfo.setNumList(analyseCatch.getYearDateRevList());
        dataRevActivation.getResultMap().put("year", yearInfo);

        return ResultUtils.success(dataRevActivation);
    }

    @GetMapping("/domain")
    @AuthCheck(mustRole = "admin")
    public ResultResponse<HashMap<String, Long>> getAnalyseDomainInfo(
            @RequestHeader("ACCESS-KEY") String accessKey) {
        HashMap<String, Long> domainMap = new HashMap<>();
        domainMap.put("军工", 100L);
        domainMap.put("人工智能", 90L);
        domainMap.put("计算机", 80L);
        domainMap.put("安防", 80L);
        domainMap.put("科研", 70L);
        domainMap.put("法律", 60L);
        domainMap.put("金融", 55L);
        domainMap.put("医疗", 50L);
        domainMap.put("质检", 40L);

        return ResultUtils.success(domainMap);
    }
}

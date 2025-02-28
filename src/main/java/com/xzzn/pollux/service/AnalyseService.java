package com.xzzn.pollux.service;


import com.xzzn.pollux.entity.ActiveInfo;
import com.xzzn.pollux.model.pojo.AnalyseCache;
import com.xzzn.pollux.service.impl.ActiveInfoServiceImpl;
import com.xzzn.pollux.utils.AnalyseUtils;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import javax.annotation.Resource;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

@Service
@Slf4j
public class AnalyseService {

    @Resource
    private ActiveInfoServiceImpl activeInfoService;

    private static final long TOTAL_USER_BASE = 20000L;
    private static final long ONLINE_USER_BASE = 200L;
    private static final long GENERATE_DATA_BASE = 1200000L;
    private static final long REVIEW_DATA_BASE = 1000000L;

    @Getter
    private AnalyseCache analyseCache;

    private final Random random = new Random();  // 声明为类的字段


    public long getOnlineUser() {
        return ONLINE_USER_BASE + random.nextInt(66);
    }

    @Scheduled(cron = "0 0 4 * * ?") // 每天凌晨4点执行
    public void autoIncrease() {
        ActiveInfo activeInfo = ActiveInfo.builder()
                .dateTime(LocalDate.now())
                .userActive((long) random.nextInt(600) + 1000)
                .dataGenerate((long) random.nextInt(15000) + 45000)
                .dataReview((long) random.nextInt(10000) + 40000)
                .build();
        boolean save = activeInfoService.save(activeInfo);
        if (save) {
            initCatch();
            log.info("****** AUTO-INCREASE ******");
        }
    }

    @PostConstruct
    public void initCatch() {
        //初始化cache
        analyseCache = new AnalyseCache();

        // 初始化数据总览
        long differDays = AnalyseUtils.getDifferDays();
        int deltaUser = 0;
        int deltaGenData = 0;
        int deltaRevData = 0;

        for (int i = 0; i < differDays; i++) {
            deltaUser += random.nextInt(10) + 10;
            deltaGenData += random.nextInt(1000) + 500;
            deltaRevData += random.nextInt(1000) + 300;
        }
        analyseCache.setTotalUser(TOTAL_USER_BASE + deltaUser);
        analyseCache.setGenerateData(GENERATE_DATA_BASE + deltaGenData);
        analyseCache.setReviewData(REVIEW_DATA_BASE + deltaRevData);

        // 初始化活跃度图表数据
        List<ActiveInfo> activeInfoList = activeInfoService.getAllActiveInfo();

        //更新“日”
        String[] days = AnalyseUtils.getLastTimeArray("day");
        analyseCache.setDayDateList(days);
        ArrayList<Long[]> dayRes = getAnalyseDateByTime(activeInfoList, days.length, 1);
        analyseCache.setDayUserActiveList(dayRes.get(0));
        analyseCache.setDayDateGenList(dayRes.get(1));
        analyseCache.setDayDateRevList(dayRes.get(2));

        //更新周
        String[] weeks = AnalyseUtils.getLastTimeArray("week");
        analyseCache.setWeekDateList(weeks);
        ArrayList<Long[]> weekRes = getAnalyseDateByTime(activeInfoList, weeks.length, 7);
        analyseCache.setWeekUserActiveList(weekRes.get(0));
        analyseCache.setWeekDateGenList(weekRes.get(1));
        analyseCache.setWeekDateRevList(weekRes.get(2));

        //更新月
        String[] months = AnalyseUtils.getLastTimeArray("month");
        analyseCache.setMonthDateList(months);
        ArrayList<Long[]> monthRes = getAnalyseDateByTime(activeInfoList, months.length, 30);
        analyseCache.setMonthUserActiveList(monthRes.get(0));
        analyseCache.setMonthDateGenList(monthRes.get(1));
        analyseCache.setMonthDateRevList(monthRes.get(2));

        //更新季度
        String[] quarters = AnalyseUtils.getLastTimeArray("quarter");
        analyseCache.setQuarterDateList(quarters);
        ArrayList<Long[]> quarterRes = getAnalyseDateByTime(activeInfoList, quarters.length, 90);
        analyseCache.setQuarterUserActiveList(quarterRes.get(0));
        analyseCache.setQuarterDateGenList(quarterRes.get(1));
        analyseCache.setQuarterDateRevList(quarterRes.get(2));

        //更新年
        String[] years = AnalyseUtils.getLastTimeArray("year");
        analyseCache.setYearDateList(years);
        ArrayList<Long[]> yearRes = getAnalyseDateByTime(activeInfoList, years.length, 365);
        analyseCache.setYearUserActiveList(yearRes.get(0));
        analyseCache.setYearDateGenList(yearRes.get(1));
        analyseCache.setYearDateRevList(yearRes.get(2));
    }

    private ArrayList<Long[]> getAnalyseDateByTime(List<ActiveInfo> activeInfoList, int size, int deltaCount) {
        Long[] user = new Long[size];
        Long[] dateGen = new Long[size];
        Long[] dateRev = new Long[size];

        int index = activeInfoList.size() - 1;
        for (int i = size - 1; i >= 0; i--) {
            long curUser = 0L;
            long curDateGen = 0L;
            long curDateRev = 0L;

            for (int j = 0; j < deltaCount; j++) {
                if (index - j < 0) {
                    break;
                }
                ActiveInfo activeInfo = activeInfoList.get(index - j);
                curUser += activeInfo.getUserActive();
                curDateGen += activeInfo.getDataGenerate();
                curDateRev += activeInfo.getDataReview();
            }

            user[size - 1 - i] = curUser;
            dateRev[size - 1 - i] = curDateRev;
            dateGen[size - 1 - i] = curDateGen;
            index -= deltaCount;
            if (index < 0)
                break;
        }

        ArrayList<Long[]> res = new ArrayList<>();
        AnalyseUtils.reverseArray(user);
        res.add(user);
        AnalyseUtils.reverseArray(dateGen);
        res.add(dateGen);
        AnalyseUtils.reverseArray(dateRev);
        res.add(dateRev);
        return res;
    }

}

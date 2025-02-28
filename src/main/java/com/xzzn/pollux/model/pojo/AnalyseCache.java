package com.xzzn.pollux.model.pojo;

import lombok.Data;

@Data
public class AnalyseCache {
    //数据总览数据
    private long totalUser;
    private long generateData;
    private long reviewData;

    //活跃度图表数据
    private String[] dayDateList;
    private Long[] dayUserActiveList;
    private Long[] dayDateGenList;
    private Long[] dayDateRevList;

    private String[] weekDateList;
    private Long[] weekUserActiveList;
    private Long[] weekDateGenList;
    private Long[] weekDateRevList;

    private String[] monthDateList;
    private Long[] monthUserActiveList;
    private Long[] monthDateGenList;
    private Long[] monthDateRevList;

    private String[] quarterDateList;
    private Long[] quarterUserActiveList;
    private Long[] quarterDateGenList;
    private Long[] quarterDateRevList;

    private String[] yearDateList;
    private Long[] yearUserActiveList;
    private Long[] yearDateGenList;
    private Long[] yearDateRevList;

}

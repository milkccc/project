package com.xzzn.pollux.utils;

import java.text.SimpleDateFormat;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.Arrays;
import java.util.Calendar;

public class AnalyseUtils {

    private AnalyseUtils() {
    }
    public static String[] getLastTimeArray(String dimension){
        String[] res = new String[0];
        switch (dimension){
            case "day":
                res = getLastTimeArray("MM.dd", Calendar.DAY_OF_MONTH, -1);
                break;
            case "week":
                res = getLastTimeArray("MM.dd",Calendar.WEEK_OF_YEAR, -1);
                break;
            case "month":
                res = getLastTimeArray("yyyy.MM",Calendar.MONTH, -1);
                break;
            case "quarter":
                res = getLastTimeArray("yyyy.MM",Calendar.MONTH, -3);
                break;
            case "year":
                res = getLastTimeArray("yyyy",Calendar.YEAR, -1);
                break;
            default:
                break;
        }
        return res;
    }

    public static String[] getLastTimeArray(String dateformat, int deltaTime, int deltaTimeCount) {
        String[] months = new String[10];
        Calendar calendar = Calendar.getInstance();
        Calendar endCalendar = Calendar.getInstance();

        //截止日期
        endCalendar.set(2023, Calendar.NOVEMBER, 2);

        SimpleDateFormat dateFormat = new SimpleDateFormat(dateformat);
        int size = 0;
        for (int i = 0; i < 10; i++) {
            if(calendar.before(endCalendar)){
                break;
            }
            months[i] = dateFormat.format(calendar.getTime());
            calendar.add(deltaTime, deltaTimeCount); // 在当前日期的基础上减去一个月
            size++;
        }
        months = Arrays.copyOfRange(months, 0, size);
        reverseArray(months);
        return months;
    }

    /**
     * 反转数组
     */
    public static <T> void  reverseArray(T[] array) {
        int start = 0;
        int end = array.length - 1;

        while (start < end) {
            T temp = array[start];
            array[start] = array[end];
            array[end] = temp;

            start++;
            end--;
        }
    }

    /**
     * 获取当前日期到截止日期的天数差
     */
    public static long getDifferDays(){
        LocalDate currentDate = LocalDate.now();
        LocalDate endDate = LocalDate.of(2023, 11, 2);

        return ChronoUnit.DAYS.between(endDate, currentDate);
    }


}

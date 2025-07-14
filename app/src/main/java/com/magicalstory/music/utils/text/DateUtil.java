package com.magicalstory.music.utils.text;

import android.text.TextUtils;

import androidx.annotation.IntRange;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.util.Pair;

import java.text.ParsePosition;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Random;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class DateUtil {
    static final String TAG = "DateUtil";
    //星期几与数值映射
    static public final Map<String, Integer> weekdayMap = new LinkedHashMap<>();
    //初始化星期映射
    static {
        weekdayMap.put("星期日", 1);
        weekdayMap.put("星期天", 1);
        weekdayMap.put("星期一", 2);
        weekdayMap.put("星期二", 3);
        weekdayMap.put("星期三", 4);
        weekdayMap.put("星期四", 5);
        weekdayMap.put("星期五", 6);
        weekdayMap.put("星期六", 7);
        weekdayMap.put("周日", 1);
        weekdayMap.put("周天", 1);
        weekdayMap.put("周一", 2);
        weekdayMap.put("周二", 3);
        weekdayMap.put("周三", 4);
        weekdayMap.put("周四", 5);
        weekdayMap.put("周五", 6);
        weekdayMap.put("周六", 7);
    }
    static public final String[] weekdayName = new String[]{"星期天", "星期一", "星期二", "星期三", "星期四", "星期五", "星期六"};
    static public final String[] weekdayShortName = new String[]{"周日", "周一", "周二", "周三", "周四", "周五", "周六"};
    static public final String[] englishMonthName = new String[]{"Jan.","Feb.","Mar.","Apr.","May.","Jun.","Jul.","Aug.","Sept.","Oct.","Nov.","Dec."};


    /**
     * 星期几转换为第几天
     * @param weekday 星期    例：星期天
     * @return 星期第几天
     */
    public static int weekdayToNumber(String weekday) {
        if (TextUtils.isEmpty(weekday) || !Pattern.matches("(星期[一二三四五六日天]|周[一二三四五六日天])", weekday)) {
            return -1;
        } else {
            Matcher matcher = Pattern.compile("(星期[一二三四五六日天]|周[一二三四五六日天])").matcher(weekday);
            if (matcher.find()) {
                Integer value = weekdayMap.get(matcher.group());
                return value == null ? -1 : value;
            }
            return -1;
        }
    }

    //获取星期字符串
    public static String getWeekdayString(@IntRange(from = 1, to = 7) int weekday, boolean isShort) {
        return isShort ? weekdayShortName[weekday - 1] : weekdayName[weekday - 1];
    }

    //获取英文月份
    public static String getMonthEnglish(int month) {
        return englishMonthName[month];
    }

    /**
     * 获取当前时间 - Calendar方式
     *
     * @return
     */
    public static String getCurrentTimeYMDHMS() {
        //获取当前时间
        Calendar c = Calendar.getInstance();//可以对每个时间域单独修改
        int year = c.get(Calendar.YEAR);
        int month = c.get(Calendar.MONTH);
        int date = c.get(Calendar.DATE);
        int hour = c.get(Calendar.HOUR_OF_DAY);
        int minute = c.get(Calendar.MINUTE);
        int second = c.get(Calendar.SECOND);

        return year + "-" + (month + 1) + "-" + date + " " + hour + ":" + minute + ":" + second;
    }

    /**
     * 获取当前时间 - Calendar方式
     *
     * @return
     */
    public static String getCurrentTimeYMD() {
        //获取当前时间
        Calendar c = Calendar.getInstance();//可以对每个时间域单独修改
        int year = c.get(Calendar.YEAR);
        int month = c.get(Calendar.MONTH);
        int date = c.get(Calendar.DATE);
        int hour = c.get(Calendar.HOUR_OF_DAY);
        int minute = c.get(Calendar.MINUTE);
        int second = c.get(Calendar.SECOND);

        return year + "-" + month + 1 + "-" + date;
    }

    //获取时间偏移量
    public static long getZoneOffset(Calendar calendar) {
        return calendar.get(Calendar.ZONE_OFFSET) + calendar.get(Calendar.DST_OFFSET);   //时区时间偏移量
    }

    //根据年月日返回Calendar
    public static Calendar getCalendar(int year, int month, int dayOfMonth) {
        Calendar calendar = Calendar.getInstance();
        calendar.clear();
        calendar.set(year, month - 1, dayOfMonth);

        return calendar;
    }

    //根据Date返回Calendar
    public static Calendar getCalendar(@Nullable Date date) {
        if (date == null) {
            return null;
        } else {
            Calendar calendar = Calendar.getInstance();
            calendar.setTime(date);
            return calendar;
        }
    }

    //根据Millis返回Calendar
    public static Calendar getCalendar(long millis) {
        Calendar calendar = Calendar.getInstance();
        calendar.setTimeInMillis(millis);
        return calendar;
    }

    //获取指定时间下一个小时的第一时刻
    public static Calendar getStartOfNextHour(Calendar calendar) {
        Calendar result = (Calendar) calendar.clone();
        result.set(Calendar.MINUTE, 0);
        result.set(Calendar.SECOND, 0);
        result.set(Calendar.MILLISECOND, 0);
        result.add(Calendar.HOUR_OF_DAY, 1);
        return result;
    }

    //获取时长的文字表示
    static public String getDurStr(long duration) {
        long second = duration / 1000;
        StringBuilder sb = new StringBuilder();
        //年
        if (second >= 31536000) {
            sb.append(second / 31536000).append("年");
        }
        //月
        if (second >= 2592000) {
            sb.append(second / 2592000).append("月");
        }
        //日
        if (second >= 86400) {
            sb.append(second / 86400).append("日");
        }
        //时
        if (second >= 3600) {
            sb.append(second / 3600).append("时");
        }
        //分
        if (second >= 60) {
            sb.append(second / 60).append("分");
        }
        //秒
        sb.append(second % 60).append("秒");
        return sb.toString();
    }


    //region 某天

    //获取当前时间，精确到分钟（秒和毫秒为0）
    public static Calendar getNow_CorrectMinute() {
        Calendar now = Calendar.getInstance();
        now.set(Calendar.SECOND, 0);
        now.set(Calendar.MILLISECOND, 0);

        return now;
    }

    //获取日期当天的第一时刻
    public static Calendar getStartOfDay(Date date) {
        Calendar calendar = getCalendar(date);
        Calendar temp = Calendar.getInstance();
        temp.clear();
        temp.set(calendar.get(Calendar.YEAR), calendar.get(Calendar.MONTH), calendar.get(Calendar.DAY_OF_MONTH));
        return temp;
    }

    //获取日期当天的第一时刻
    public static Calendar getStartOfDay(Calendar calendar) {
        Calendar temp = Calendar.getInstance();
        temp.clear();
        temp.set(calendar.get(Calendar.YEAR), calendar.get(Calendar.MONTH), calendar.get(Calendar.DAY_OF_MONTH));
        return temp;
    }

    //获取日期当天的第一时刻
    public static Calendar getStartOfDay(int year, int month, int dayOfMonth) {
        Calendar temp = Calendar.getInstance();
        temp.clear();
        temp.set(year, month, dayOfMonth);
        return temp;
    }

    //获取日期当天的最后一刻
    public static Calendar getEndOfDay(Date date) {
        Calendar calendar = getCalendar(date);
        Calendar temp = Calendar.getInstance();
        temp.clear();
        temp.set(calendar.get(Calendar.YEAR), calendar.get(Calendar.MONTH), calendar.get(Calendar.DAY_OF_MONTH), 23, 59, 59);
        return temp;
    }

    //获取日期当天的最后一刻
    public static Calendar getEndOfDay(Calendar calendar) {
        Calendar temp = Calendar.getInstance();
        temp.clear();
        temp.set(calendar.get(Calendar.YEAR), calendar.get(Calendar.MONTH), calendar.get(Calendar.DAY_OF_MONTH), 23, 59, 59);
        return temp;
    }

    //endregion

    //region 今天

    //获取今天第一时刻
    public static Calendar getStartOfToday() {
        Calendar today = Calendar.getInstance();
        Calendar calendar = (Calendar) today.clone();
        today.clear();
        today.set(calendar.get(Calendar.YEAR), calendar.get(Calendar.MONTH), calendar.get(Calendar.DAY_OF_MONTH));
        return today;
    }

    //获取今天最后一刻
    public static Calendar getEndOfToday() {
        Calendar today = Calendar.getInstance();
        Calendar calendar = (Calendar) today.clone();
        today.clear();
        today.set(calendar.get(Calendar.YEAR), calendar.get(Calendar.MONTH), calendar.get(Calendar.DAY_OF_MONTH), 23, 59, 59);
        return today;
    }

    //获取今天最后一分钟
    public static Calendar getEndOfToday_Minute() {
        Calendar today = Calendar.getInstance();
        today.set(Calendar.HOUR_OF_DAY, 23);
        today.set(Calendar.MINUTE, 59);
        today.set(Calendar.SECOND, 0);
        today.set(Calendar.MILLISECOND, 0);
        return today;
    }

    //根据时分获取今天的时间
    public static Calendar getTodayFromHM(@IntRange(from = 0, to = 23) int hourOfDay, @IntRange(from = 0, to = 59) int minute) {
        Calendar today = getStartOfToday();
        today.set(Calendar.HOUR_OF_DAY, hourOfDay);
        today.set(Calendar.MINUTE, minute);
        return today;
    }

    //endregion

    //region 明天

    //获取明天第一时刻
    public static Calendar getStartOfTomorrow() {
        Calendar tomorrow = Calendar.getInstance();
        tomorrow.add(Calendar.DAY_OF_MONTH, 1);
        Calendar calendar = (Calendar) tomorrow.clone();
        tomorrow.clear();
        tomorrow.set(calendar.get(Calendar.YEAR), calendar.get(Calendar.MONTH), calendar.get(Calendar.DAY_OF_MONTH));
        return tomorrow;
    }

    //获取明天第一时刻
    public static Calendar getStartOfTomorrow(Calendar calendar) {
        Calendar tomorrow = (Calendar) calendar.clone();
        tomorrow.add(Calendar.DAY_OF_MONTH, 1);
        Calendar result = (Calendar) tomorrow.clone();
        tomorrow.clear();
        tomorrow.set(result.get(Calendar.YEAR), result.get(Calendar.MONTH), result.get(Calendar.DAY_OF_MONTH));
        return tomorrow;
    }

    //endregion

    //获取昨天第一时刻
    public static Calendar getStartOfYesterday() {
        Calendar yesterday = Calendar.getInstance();
        yesterday.add(Calendar.DAY_OF_MONTH, -1);
        Calendar calendar = (Calendar) yesterday.clone();
        yesterday.clear();
        yesterday.set(calendar.get(Calendar.YEAR), calendar.get(Calendar.MONTH), calendar.get(Calendar.DAY_OF_MONTH));
        return yesterday;
    }

    //获取下一周第一时刻
    public static Calendar getStartOfNextWeek() {
        Calendar calendar = getStartOfToday();
        int addition = Math.abs(calendar.get(Calendar.DAY_OF_WEEK) - Calendar.SUNDAY);
        calendar.add(Calendar.DAY_OF_MONTH, addition == 0 ? 7 : addition);
        return calendar;
    }

    //获取两天相差天数
    public static int getIntervalOf2Days(Calendar start, Calendar end) {
        Calendar s = getStartOfDay(start);
        Calendar e = getStartOfDay(end);
        return (int) ((e.getTimeInMillis() - s.getTimeInMillis()) / (24 * 3600000));
    }

    //获取两时间相差分钟数
    public static int getIntervalOf2Time(Calendar start, Calendar end, boolean ignoreDate) {
        //忽略日期
        if (ignoreDate) {
            Calendar start2 = (Calendar) start.clone();
            start2.set(end.get(Calendar.YEAR), end.get(Calendar.MONTH), end.get(Calendar.DAY_OF_MONTH));
            return (int) Math.abs((end.getTimeInMillis() - start2.getTimeInMillis()) / 60000f);
        }
        return (int) Math.abs((end.getTimeInMillis() - start.getTimeInMillis()) / 60000f);
    }

    //获取本月第一时刻
    public static Calendar getStartOfThisMonth() {
        Calendar target = Calendar.getInstance();
        Calendar calendar = (Calendar) target.clone();
        target.clear();
        target.set(calendar.get(Calendar.YEAR), calendar.get(Calendar.MONTH), 1);
        return target;
    }

    //获取某月的上个月的第一时刻
    public static Calendar getStartOfMonthLast(int year, int month) {
        Calendar calendar = getStartOfMonth(year, month);
        calendar.add(Calendar.MONTH, -1);
        return calendar;
    }

    //获取某月第一时刻  month：1~12
    public static Calendar getStartOfMonth(int year, int month) {
        Calendar target = Calendar.getInstance();
        target.clear();
        target.set(year, month - 1, 1);
        return target;
    }

    //获取某月第一时刻
    public static Calendar getStartOfMonth(Calendar calendar) {
        Calendar target = (Calendar) calendar.clone();
        target.clear();
        target.set(calendar.get(Calendar.YEAR), calendar.get(Calendar.MONTH), 1);
        return target;
    }

    //获取某月最后一刻
    public static Calendar getEndOfMonth(int year, int month) {
        Calendar target = Calendar.getInstance();
        target.clear();
        target.set(year, month - 1, 1);
        target.add(Calendar.MONTH, 1);
        target.add(Calendar.SECOND, -1);
        return target;
    }

    //获取某月最后一刻
    public static Calendar getEndOfMonth(Calendar calendar) {
        Calendar target = (Calendar) calendar.clone();
        target.clear();
        target.set(calendar.get(Calendar.YEAR), calendar.get(Calendar.MONTH), calendar.get(Calendar.DAY_OF_MONTH));
        target.add(Calendar.MONTH, 1);
        target.add(Calendar.SECOND, -1);
        return target;
    }

    //获取上个月第一时刻
    public static Calendar getStartOfLastMonth() {
        Calendar target = Calendar.getInstance();
        Calendar calendar = (Calendar) target.clone();
        target.clear();
        target.set(calendar.get(Calendar.YEAR), calendar.get(Calendar.MONTH), 1);
        target.add(Calendar.MONTH, -1);
        return target;
    }

    //获取下个月第一时刻
    public static Calendar getStartOfNextMonth() {
        Calendar target = Calendar.getInstance();
        Calendar calendar = (Calendar) target.clone();
        target.clear();
        target.set(calendar.get(Calendar.YEAR), calendar.get(Calendar.MONTH), 1);
        target.add(Calendar.MONTH, 1);
        return target;
    }

    //获取今年第一时刻
    public static Calendar getStartOfYear() {
        Calendar target = Calendar.getInstance();
        Calendar calendar = (Calendar) target.clone();
        target.clear();
        target.set(calendar.get(Calendar.YEAR), 0, 1);
        return target;
    }

    //获取明年第一时刻
    public static Calendar getStartOfNextYear() {
        Calendar target = Calendar.getInstance();
        Calendar calendar = (Calendar) target.clone();
        target.clear();
        target.set(calendar.get(Calendar.YEAR) + 1, 0, 1);
        return target;
    }

    //是否为今天
    public static boolean isToday(@NonNull Date date) {
        return DateUtil.getStartOfToday().getTimeInMillis() <= date.getTime() && date.getTime() < DateUtil.getStartOfTomorrow().getTimeInMillis();
    }

    //是否为今天
    public static boolean isToday(@NonNull Calendar calendar) {
        return DateUtil.getStartOfToday().getTimeInMillis() <= calendar.getTimeInMillis() && calendar.getTimeInMillis() < DateUtil.getStartOfTomorrow().getTimeInMillis();
    }

    //是否为同一天
    public static boolean isSameDay(Calendar date1, Calendar date2) {
        return date1 != null && date2 != null
                && date1.get(Calendar.YEAR) == date2.get(Calendar.YEAR)
                && date1.get(Calendar.MONTH) == date2.get(Calendar.MONTH)
                && date1.get(Calendar.DAY_OF_MONTH) == date2.get(Calendar.DAY_OF_MONTH);
    }

    //是否为同一天
    public static boolean isSameDay(Date date1, Date date2) {
        return date1 != null && date2 != null
                && date1.getYear() == date2.getYear()
                && date1.getMonth() == date2.getMonth()
                && date1.getDate() == date2.getDate();
    }

    //是否为同一周
    public static boolean isSameWeek(Calendar date1, Calendar date2) {
        Pair<Calendar, Calendar> bound = DateUtil.getWeekBoundOfDate(date1);
        return bound.first.getTimeInMillis() <= date2.getTimeInMillis() && date2.getTimeInMillis() <= bound.second.getTimeInMillis();
    }

    //是否为同一个月
    public static boolean isSameMonth(Calendar date1, Calendar date2) {
        return date1.get(Calendar.YEAR) == date2.get(Calendar.YEAR)
                && date1.get(Calendar.MONTH) == date2.get(Calendar.MONTH);
    }

    //是否为同一年
    public static boolean isSameYear(Calendar date1, Calendar date2) {
        return date1.get(Calendar.YEAR) == date2.get(Calendar.YEAR);
    }

    //是否为同一年
    public static boolean isSameYear(Date date1, Date date2) {
        return date1.getYear() == date2.getYear();
    }

    //是否间隔至少一个完整周
    public static boolean isIncludeWholeWeek(Calendar date1, Calendar date2) {
        //确保date1日期在date2前
        if (!date1.before(date2)) {
            Calendar temp = (Calendar) date1.clone();
            date1.setTime(date2.getTime());
            date2.setTime(temp.getTime());
        }
        //date1和date2分别指向各自周的结尾和开头
        date1.add(Calendar.DAY_OF_WEEK, 7 - date1.get(Calendar.DAY_OF_WEEK));
        date2.add(Calendar.DAY_OF_WEEK, 1 - date2.get(Calendar.DAY_OF_WEEK));

        return date2.getTimeInMillis() - date1.getTimeInMillis() > 604800000;
    }

    //根据日期返回当周开始日期第一时刻和结束日期最后一刻     以周日为每周第一天
    public static Pair<Calendar, Calendar> getWeekBoundOfDate(Calendar date) {
        Calendar start = DateUtil.getStartOfDay(date);
        Calendar end = DateUtil.getEndOfDay(date);

        int dayOfWeek = date.get(Calendar.DAY_OF_WEEK); //当天为当周的第几天
        //当天不是第一天（周日）
        if (dayOfWeek != Calendar.SUNDAY) {
            start.add(Calendar.DAY_OF_WEEK, 1 - dayOfWeek);
        }
        //当天不是最后一天（周六）
        if (dayOfWeek != Calendar.SATURDAY) {
            end.add(Calendar.DAY_OF_WEEK, 7 - dayOfWeek);
        }

        return new Pair<>(start, end);
    }

    //根据日期返回当月开始日期第一时刻和结束日期最后一刻
    public static Pair<Calendar, Calendar> getMonthBoundOfDate(Calendar date) {
        Calendar start = DateUtil.getStartOfMonth(date);
        Calendar end = (Calendar) start.clone();
        end.add(Calendar.MONTH, 1);
        end.add(Calendar.SECOND, -1);

        return new Pair<>(start, end);
    }

    //根据毫秒数返回时分
    public static Pair<Integer, Integer> getHourMinute(long millis) {
        long totalSecond = millis / 60000;
        int hour = (int) (totalSecond / 60);
        int minute = (int) (totalSecond % 60);
        return new Pair<>(hour, minute);
    }

    //判断忽略部分因素时间是否相等
    public static boolean equalsNeglect(@NonNull Calendar date1, @NonNull Calendar date2
            , @IntRange(from = 0, to = Calendar.FIELD_COUNT) int... neglects) {
        if (date1 == null && date2 == null) {
            return true;
        } else if (date1 == null || date2 == null) {
            return false;
        }
        Calendar calendar1 = (Calendar) date1.clone();
        Calendar calendar2 = (Calendar) date2.clone();
        for (int neglect : neglects) {
            if (0 <= neglect && neglect <= Calendar.FIELD_COUNT) {
                calendar1.set(neglect, 0);
                calendar2.set(neglect, 0);
            }
        }
        return calendar1.getTimeInMillis() == calendar2.getTimeInMillis();
    }

    /**
     * 获取现在时间 - Date方式
     *
     * @return 返回时间类型 yyyy-MM-dd HH:mm:ss
     */
    public static Date getNowDate() {
        Date currentTime = new Date();
        SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        String dateString = formatter.format(currentTime);
        ParsePosition pos = new ParsePosition(8);
        Date currentTime_2 = formatter.parse(dateString, pos);
        return currentTime_2;
    }

    /**
     * 获取现在时间
     *
     * @return返回短时间格式 yyyy-MM-dd
     */
    public static Date getNowDateShort() {
        Date currentTime = new Date();
        SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd");
        String dateString = formatter.format(currentTime);
        ParsePosition pos = new ParsePosition(8);
        Date currentTime_2 = formatter.parse(dateString, pos);
        return currentTime_2;
    }

    /**
     * 获取现在时间
     *
     * @return返回字符串格式 yyyy-MM-dd HH:mm:ss
     */
    public static String getStringDate() {
        Date currentTime = new Date();
        SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        String dateString = formatter.format(currentTime);
        return dateString;
    }

    /**
     * 获取现在时间
     *
     * @return 返回短时间字符串格式yyyy-MM-dd
     */
    public static String getStringDateShort() {
        Date currentTime = new Date();
        SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd");
        String dateString = formatter.format(currentTime);
        return dateString;
    }

    /**
     * 获取时间 小时:分;秒 HH:mm:ss
     *
     * @return
     */
    public static String getTimeShort() {
        SimpleDateFormat formatter = new SimpleDateFormat("HH:mm:ss");
        Date currentTime = new Date();
        String dateString = formatter.format(currentTime);
        return dateString;
    }

    /**
     * 将长时间格式字符串转换为时间 yyyy-MM-dd HH:mm:ss
     *
     * @param strDate
     * @return
     */
    public static Date strToDateLong(String strDate) {
        SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        ParsePosition pos = new ParsePosition(0);
        Date strtodate = formatter.parse(strDate, pos);
        return strtodate;
    }

    /**
     * 将长时间格式时间转换为字符串 yyyy-MM-dd HH:mm:ss
     *
     * @param dateDate
     * @return
     */
    public static String dateToStrLong(Date dateDate) {
        SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        String dateString = formatter.format(dateDate);
        return dateString;
    }

    /**
     * 将短时间格式时间转换为字符串 yyyy-MM-dd
     *
     * @param dateDate
     * @return
     */
    public static String dateToStr(Date dateDate) {
        SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd");
        String dateString = formatter.format(dateDate);
        return dateString;
    }

    /**
     * 将短时间格式字符串转换为时间 yyyy-MM-dd
     *
     * @param strDate
     * @return
     */
    public static Date strToDate(String strDate) {
        SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd");
        ParsePosition pos = new ParsePosition(0);
        Date strtodate = formatter.parse(strDate, pos);
        return strtodate;
    }

    /**
     * 得到现在时间
     *
     * @return
     */
    public static Date getNow() {
        return new Date();
    }

    /**
     * 提取一个月中的最后一天
     *
     * @param day
     * @return
     */
    public static Date getLastDate(long day) {
        Date date = new Date();
        long date_3_hm = date.getTime() - 3600000 * 34 * day;
        Date date_3_hm_date = new Date(date_3_hm);
        return date_3_hm_date;
    }

    /**
     * 得到现在时间
     *
     * @return 字符串 yyyyMMdd HHmmss
     */
    public static String getStringToday() {
        Date currentTime = new Date();
        SimpleDateFormat formatter = new SimpleDateFormat("yyyyMMdd HHmmss");
        String dateString = formatter.format(currentTime);
        return dateString;
    }

    /**
     * 得到现在小时
     */
    public static String getHour() {
        Date currentTime = new Date();
        SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        String dateString = formatter.format(currentTime);
        String hour;
        hour = dateString.substring(11, 13);
        return hour;
    }

    /**
     * 得到现在分钟
     *
     * @return
     */
    public static String getTime() {
        Date currentTime = new Date();
        SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        String dateString = formatter.format(currentTime);
        String min;
        min = dateString.substring(14, 16);
        return min;
    }

    /**
     * 根据用户传入的时间表示格式，返回当前时间的格式 如果是yyyyMMdd，注意字母y不能大写。
     *
     * @param sformat yyyyMMddhhmmss
     * @return
     */
    public static String getUserDate(String sformat) {
        Date currentTime = new Date();
        SimpleDateFormat formatter = new SimpleDateFormat(sformat);
        String dateString = formatter.format(currentTime);
        return dateString;
    }

    /**
     * 二个小时时间间的差值,必须保证二个时间都是"HH:MM"的格式，返回字符型的分钟
     */
    public static String getTwoHour(String st1, String st2) {
        String[] kk = null;
        String[] jj = null;
        kk = st1.split(":");
        jj = st2.split(":");
        if (Integer.parseInt(kk[0]) < Integer.parseInt(jj[0]))
            return "0";
        else {
            double y = Double.parseDouble(kk[0]) + Double.parseDouble(kk[1]) / 60;
            double u = Double.parseDouble(jj[0]) + Double.parseDouble(jj[1]) / 60;
            if ((y - u) > 0)
                return y - u + "";
            else
                return "0";
        }
    }

    /**
     * 得到二个日期间的间隔天数
     */
    public static String getTwoDay(String sj1, String sj2) {
        SimpleDateFormat myFormatter = new SimpleDateFormat("yyyy-MM-dd");
        long day = 0;
        try {
            Date date = myFormatter.parse(sj1);
            Date mydate = myFormatter.parse(sj2);
            day = (date.getTime() - mydate.getTime()) / (24 * 60 * 60 * 1000);
        } catch (Exception e) {
            return "";
        }
        return day + "";
    }

    /**
     * 时间前推或后推分钟,其中JJ表示分钟.
     */
    public static String getPreTime(String sj1, String jj) {
        SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        String mydate1 = "";
        try {
            Date date1 = format.parse(sj1);
            long Time = (date1.getTime() / 1000) + Integer.parseInt(jj) * 60;
            date1.setTime(Time * 1000);
            mydate1 = format.format(date1);
        } catch (Exception e) {
        }
        return mydate1;
    }

    /**
     * 得到一个时间延后或前移几天的时间,nowdate为时间,delay为前移或后延的天数
     */
    public static String getNextDay(String nowdate, String delay) {
        try {
            SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd");
            String mdate = "";
            Date d = strToDate(nowdate);
            long myTime = (d.getTime() / 1000) + Integer.parseInt(delay) * 24 * 60 * 60;
            d.setTime(myTime * 1000);
            mdate = format.format(d);
            return mdate;
        } catch (Exception e) {
            return "";
        }
    }

    /**
     * 判断是否润年
     *
     * @param ddate
     * @return
     */
    public static boolean isLeapYear(String ddate) {

        /**
         * 详细设计： 1.被400整除是闰年，否则： 2.不能被4整除则不是闰年 3.能被4整除同时不能被100整除则是闰年
         * 3.能被4整除同时能被100整除则不是闰年
         */
        Date d = strToDate(ddate);
        GregorianCalendar gc = (GregorianCalendar) Calendar.getInstance();
        gc.setTime(d);
        int year = gc.get(Calendar.YEAR);
        if ((year % 400) == 0)
            return true;
        else if ((year % 4) == 0) {
            if ((year % 100) == 0)
                return false;
            else
                return true;
        } else
            return false;
    }

    /**
     * 返回美国时间格式 26 Apr 2006
     *
     * @param str
     * @return
     */
    public static String getEDate(String str) {
        SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd");
        ParsePosition pos = new ParsePosition(0);
        Date strtodate = formatter.parse(str, pos);
        String j = strtodate.toString();
        String[] k = j.split(" ");
        return k[2] + k[1].toUpperCase() + k[5].substring(2, 4);
    }

    /**
     * 获取一个月的最后一天
     *
     * @param dat
     * @return
     */
    public static String getEndDateOfMonth(String dat) {// yyyy-MM-dd
        String str = dat.substring(0, 8);
        String month = dat.substring(5, 7);
        int mon = Integer.parseInt(month);
        if (mon == 1 || mon == 3 || mon == 5 || mon == 7 || mon == 8 || mon == 10 || mon == 12) {
            str += "31";
        } else if (mon == 4 || mon == 6 || mon == 9 || mon == 11) {
            str += "30";
        } else {
            if (isLeapYear(dat)) {
                str += "29";
            } else {
                str += "28";
            }
        }
        return str;
    }

    /**
     * 判断二个时间是否在同一个周
     *
     * @param date1
     * @param date2
     * @return
     */
    public static boolean isSameWeekDates(Date date1, Date date2) {
        Calendar cal1 = Calendar.getInstance();
        Calendar cal2 = Calendar.getInstance();
        cal1.setTime(date1);
        cal2.setTime(date2);
        int subYear = cal1.get(Calendar.YEAR) - cal2.get(Calendar.YEAR);
        if (0 == subYear) {
            if (cal1.get(Calendar.WEEK_OF_YEAR) == cal2.get(Calendar.WEEK_OF_YEAR))
                return true;
        } else if (1 == subYear && 11 == cal2.get(Calendar.MONTH)) {
            // 如果12月的最后一周横跨来年第一周的话则最后一周即算做来年的第一周
            if (cal1.get(Calendar.WEEK_OF_YEAR) == cal2.get(Calendar.WEEK_OF_YEAR))
                return true;
        } else if (-1 == subYear && 11 == cal1.get(Calendar.MONTH)) {
            if (cal1.get(Calendar.WEEK_OF_YEAR) == cal2.get(Calendar.WEEK_OF_YEAR))
                return true;
        }
        return false;
    }

    /**
     * 产生周序列,即得到当前时间所在的年度是第几周
     *
     * @return
     */
    public static String getSeqWeek() {
        Calendar c = Calendar.getInstance(Locale.CHINA);
        String week = Integer.toString(c.get(Calendar.WEEK_OF_YEAR));
        if (week.length() == 1)
            week = "0" + week;
        String year = Integer.toString(c.get(Calendar.YEAR));
        return year + week;
    }

    /**
     * 获得一个日期所在的周的星期几的日期，如要找出2002年2月3日所在周的星期一是几号
     *
     * @param sdate
     * @param num
     * @return
     */
    public static String getWeek(String sdate, String num) {
        // 再转换为时间
        Date dd = strToDate(sdate);
        Calendar c = Calendar.getInstance();
        c.setTime(dd);
        if (num.equals("1")) // 返回星期一所在的日期
            c.set(Calendar.DAY_OF_WEEK, Calendar.MONDAY);
        else if (num.equals("2")) // 返回星期二所在的日期
            c.set(Calendar.DAY_OF_WEEK, Calendar.TUESDAY);
        else if (num.equals("3")) // 返回星期三所在的日期
            c.set(Calendar.DAY_OF_WEEK, Calendar.WEDNESDAY);
        else if (num.equals("4")) // 返回星期四所在的日期
            c.set(Calendar.DAY_OF_WEEK, Calendar.THURSDAY);
        else if (num.equals("5")) // 返回星期五所在的日期
            c.set(Calendar.DAY_OF_WEEK, Calendar.FRIDAY);
        else if (num.equals("6")) // 返回星期六所在的日期
            c.set(Calendar.DAY_OF_WEEK, Calendar.SATURDAY);
        else if (num.equals("0")) // 返回星期日所在的日期
            c.set(Calendar.DAY_OF_WEEK, Calendar.SUNDAY);
        return new SimpleDateFormat("yyyy-MM-dd").format(c.getTime());
    }

    /**
     * 根据一个日期，返回是星期几的字符串
     *
     * @param sdate
     * @return
     */
    public static String getWeek(String sdate) {
        // 再转换为时间
        Date date = strToDate(sdate);
        Calendar c = Calendar.getInstance();
        c.setTime(date);
        // int hour=c.get(Calendar.DAY_OF_WEEK);
        // hour中存的就是星期几了，其范围 1~7
        // 1=星期日 7=星期六，其他类推
        return new SimpleDateFormat("EEEE").format(c.getTime());
    }

    public static String getWeekStr(String sdate) {
        String str = "";
        str = getWeek(sdate);
        if ("1".equals(str)) {
            str = "星期日";
        } else if ("2".equals(str)) {
            str = "星期一";
        } else if ("3".equals(str)) {
            str = "星期二";
        } else if ("4".equals(str)) {
            str = "星期三";
        } else if ("5".equals(str)) {
            str = "星期四";
        } else if ("6".equals(str)) {
            str = "星期五";
        } else if ("7".equals(str)) {
            str = "星期六";
        }
        return str;
    }

    /**
     * 两个时间之间的天数
     *
     * @param date1
     * @param date2
     * @return
     */
    public static long getDays(String date1, String date2) {
        if (date1 == null || date1.equals(""))
            return 0;
        if (date2 == null || date2.equals(""))
            return 0;
        // 转换为标准时间
        SimpleDateFormat myFormatter = new SimpleDateFormat("yyyy-MM-dd");
        Date date = null;
        Date mydate = null;
        try {
            date = myFormatter.parse(date1);
            mydate = myFormatter.parse(date2);
        } catch (Exception e) {
        }
        long day = (date.getTime() - mydate.getTime()) / (24 * 60 * 60 * 1000);
        return day;
    }

    /**
     * 形成如下的日历 ， 根据传入的一个时间返回一个结构 星期日 星期一 星期二 星期三 星期四 星期五 星期六 下面是当月的各个时间
     * 此函数返回该日历第一行星期日所在的日期
     *
     * @param sdate
     * @return
     */
    public static String getNowMonth(String sdate) {
        // 取该时间所在月的一号
        sdate = sdate.substring(0, 8) + "01";

        // 得到这个月的1号是星期几
        Date date = strToDate(sdate);
        Calendar c = Calendar.getInstance();
        c.setTime(date);
        int u = c.get(Calendar.DAY_OF_WEEK);
        String newday = getNextDay(sdate, (1 - u) + "");
        return newday;
    }

    /**
     * 返回一个随机数
     *
     * @param i
     * @return
     */
    public static String getRandom(int i) {
        Random jjj = new Random();
        // int suiJiShu = jjj.nextInt(9);
        if (i == 0)
            return "";
        String jj = "";
        for (int k = 0; k < i; k++) {
            jj = jj + jjj.nextInt(9);
        }
        return jj;
    }



}
package com.magicalstory.music.utils.text;

import java.text.SimpleDateFormat;
import java.util.Date;

public class TimeUtils {
    
    private static final long ONE_MINUTE = 60 * 1000;
    private static final long ONE_HOUR = 60 * ONE_MINUTE;
    private static final long ONE_DAY = 24 * ONE_HOUR;
    private static final long ONE_WEEK = 7 * ONE_DAY;
    private static final long ONE_MONTH = 30 * ONE_DAY;
    private static final long ONE_YEAR = 12 * ONE_MONTH;
    
    /**
     * 将时间戳转换为"多久前"的格式
     * @param timestamp 时间戳（毫秒）
     * @return 格式化后的时间字符串
     */
    public static String getTimeAgo(long timestamp) {
        long now = System.currentTimeMillis();
        long diff = now - timestamp;
        
        if (diff < ONE_MINUTE) {
            return "刚刚";
        } else if (diff < ONE_HOUR) {
            return diff / ONE_MINUTE + "分钟前";
        } else if (diff < ONE_DAY) {
            return diff / ONE_HOUR + "小时前";
        } else if (diff < ONE_WEEK) {
            return diff / ONE_DAY + "天前";
        } else if (diff < ONE_MONTH) {
            return diff / ONE_WEEK + "周前";
        } else if (diff < ONE_YEAR) {
            return diff / ONE_MONTH + "个月前";
        } else {
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
            return sdf.format(new Date(timestamp));
        }
    }
    
    /**
     * 将时间（毫秒）格式化为时间字符串
     * @param milliseconds 时间（毫秒）
     * @return 格式化后的时间字符串，如 "3:45" 或 "1:23:45"
     */
    public static String formatTime(long milliseconds) {
        if (milliseconds < 0) {
            return "00:00";
        }
        
        long totalSeconds = milliseconds / 1000;
        long hours = totalSeconds / 3600;
        long minutes = (totalSeconds % 3600) / 60;
        long seconds = totalSeconds % 60;
        
        if (hours > 0) {
            return String.format("%d:%02d:%02d", hours, minutes, seconds);
        } else {
            return String.format("%d:%02d", minutes, seconds);
        }
    }
    
    /**
     * 将时间（毫秒）格式化为时间字符串（int参数版本）
     * @param milliseconds 时间（毫秒）
     * @return 格式化后的时间字符串，如 "3:45" 或 "1:23:45"
     */
    public static String formatTime(int milliseconds) {
        return formatTime((long) milliseconds);
    }
    
    /**
     * 将时长（毫秒）格式化为时长字符串
     * @param duration 时长（毫秒）
     * @return 格式化后的时长字符串，如 "3:45" 或 "1:23:45"
     */
    public static String formatDuration(long duration) {
        return formatTime(duration);
    }
} 
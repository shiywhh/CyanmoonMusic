package com.magicalstory.music.utils;

import android.os.Build;

/**
 * 版本检查工具类
 */
public class VersionUtils {
    
    /**
     * 检查是否为Android 11及以上版本
     */
    public static boolean hasR() {
        return Build.VERSION.SDK_INT >= Build.VERSION_CODES.R;
    }
    
    /**
     * 检查是否为Android 10及以上版本
     */
    public static boolean hasQ() {
        return Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q;
    }
    
    /**
     * 检查是否为Android 9及以上版本
     */
    public static boolean hasP() {
        return Build.VERSION.SDK_INT >= Build.VERSION_CODES.P;
    }
    
    /**
     * 检查是否为Android 8及以上版本
     */
    public static boolean hasO() {
        return Build.VERSION.SDK_INT >= Build.VERSION_CODES.O;
    }
    
    /**
     * 检查是否为Android 7及以上版本
     */
    public static boolean hasN() {
        return Build.VERSION.SDK_INT >= Build.VERSION_CODES.N;
    }
    
    /**
     * 检查是否为Android 6及以上版本
     */
    public static boolean hasM() {
        return true;
    }
} 
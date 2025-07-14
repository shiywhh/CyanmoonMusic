package com.magicalstory.music.utils.text;

/**
 * @Classname: NumberFormatter
 * @Auther: Created by 奇谈君 on 2023/8/16.
 * @Description:数值格式化
 */
public class NumberFormatter {
    public static String formatNumber(long number) {
        if (number >= 100000000) {         // 1亿及以上
            double num = number / 100000000.0;
            return String.format("%.2f亿", num);
        } else if (number >= 10000) {      // 1万~9999万
            double num = number / 10000.0;
            return String.format("%.2f万", num);
        } else {                           // 1万以下
            return String.valueOf(number);
        }
    }

    public static String formatNumber2(long number) {
        if (number < 1000) {
            return String.valueOf(number);
        } else if (number < 10000) {
            double num = number / 1000.0;
            return String.format("%.2fk", num);
        } else if (number < 1000000) {
            double num = number / 10000.0;
            return String.format("%.2fw", num);
        } else {
            double num = number / 1000000.0;
            return String.format("%.2fm", num);
        }
    }
}

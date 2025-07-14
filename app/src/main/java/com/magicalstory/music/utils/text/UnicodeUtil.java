package com.magicalstory.music.utils.text;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * @Classname: UnicodeUtil
 * @Auther: Created by 奇谈君 on 2022/2/15.
 * @Description:
 */
public class UnicodeUtil {

    /**
     * @param string
     * @return 转换之后的内容
     * @Title: unicodeDecode
     * @Description: unicode解码 将Unicode的编码转换为中文
     */
    public static String unicodeDecode(String string) {
        Pattern pattern = Pattern.compile("(\\\\u(\\p{XDigit}{4}))");
        Matcher matcher = pattern.matcher(string);
        char ch;
        while (matcher.find()) {
            ch = (char) Integer.parseInt(matcher.group(2), 16);
            string = string.replace(matcher.group(1), ch + "");
        }
        return string;
    }

    /**
     * 将utf-8的汉字转换成unicode格式汉字码
     *
     * @param string
     * @return
     */
    public static String stringToUnicode(String string) {

        StringBuffer unicode = new StringBuffer();
        for (int i = 0; i < string.length(); i++) {
            char c = string.charAt(i);
            unicode.append("\\u" + Integer.toHexString(c));
        }
        String str = unicode.toString();

        return str.replaceAll("\\\\", "0x");
    }

    /**
     * 将unicode的汉字码转换成utf-8格式的汉字
     *
     * @param unicode
     * @return
     */
    public static String unicodeToString(String unicode) {

        try {

            byte[] converttoBytes = unicode.getBytes("UTF-8");
            return new String(converttoBytes, "UTF-8");

        } catch (Exception e) {
            e.printStackTrace();
            return "";
        }

    }


}
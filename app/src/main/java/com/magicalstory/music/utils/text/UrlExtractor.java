package com.magicalstory.music.utils.text;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * @Classname: UrlExtractor
 * @Auther: Created by 奇谈君 on 2025/4/9.
 * @Description:
 */
public class UrlExtractor {

    /**
     * 提取字符串中的第一个网址
     *
     * @param text 包含网址的文本
     * @return 第一个网址，如果未找到则返回null
     */
    public static String extractFirstUrl(String text) {
        // 定义正则表达式匹配网址
        String regex = "http(s)?://\\S+";
        Pattern pattern = Pattern.compile(regex);
        Matcher matcher = pattern.matcher(text);

        if (matcher.find()) {
            return matcher.group(); // 返回第一个匹配的网址
        }
        return text; // 如果未找到匹配项，返回null
    }

}

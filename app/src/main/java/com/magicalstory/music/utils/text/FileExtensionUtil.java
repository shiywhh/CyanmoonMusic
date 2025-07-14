package com.magicalstory.music.utils.text;

/**
 * @Classname: FileExtensionUtil
 * @Auther: Created by 奇谈君 on 2024/6/11.
 * @Description:文件后缀名工具类
 */
public class FileExtensionUtil {

    public static String getFileExtension(String filename) {
        if (filename == null || filename.isEmpty()) {
            return "";
        }

        int dotIndex = filename.lastIndexOf('.');
        if (dotIndex == -1 || dotIndex == filename.length() - 1) {
            return "";
        }

        return filename.substring(dotIndex + 1);
    }

}

package com.magicalstory.music.utils.text;

import android.content.Context;
import android.graphics.Color;
import android.text.Spannable;
import android.text.SpannableString;
import android.text.Spanned;
import android.text.style.ForegroundColorSpan;
import android.util.TypedValue;
import android.widget.TextView;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.util.Locale;
import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class textUtils {

    //判断是否是中文环境
    public static boolean isZh(Context context) {
        Locale locale = context.getResources().getConfiguration().locale;
        String language = locale.getLanguage();
        if (language.endsWith("zh"))
            return true;
        else
            return false;
    }

    // 解析文件名字
    public static String getFileNameFromUrl(String url) {
        if (url == null) {
            return "";
        }
        int index = url.lastIndexOf('/');
        if (index != -1) {
            String name = extractFileName(url.substring(index + 1));
            return name.isEmpty() ? url : name;
        } else {
            return url;
        }
    }


    public static String extractUrls(String input) {
        // 定义URL正则表达式
        String urlRegex = "(https?://\\S+)";
        Pattern pattern = Pattern.compile(urlRegex);
        Matcher matcher = pattern.matcher(input);

        // 找到匹配的URL
        if (matcher.find()) {
            return matcher.group();
        }

        return "";
    }

    public static String extractFileName(String url) {
        try {
            // 去掉问号以及后面的内容
            int questionMarkIndex = url.indexOf("?");
            if (questionMarkIndex != -1) {
                url = url.substring(0, questionMarkIndex);
            }

            // 解码URL，以处理可能存在的URL编码
            String decodedUrl = URLDecoder.decode(url, "UTF-8");

            // 返回解码后的文件名
            return decodedUrl;
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }

        // 如果解析失败，返回null或者你认为合适的默认值
        return url;
    }

    public static String getFileExtension(String mimeType) {
        return MimeTypeUtil.getMIMEType(mimeType);

    }

    /**
     * 去除字符串的前后空格
     *
     * @param str 要处理的字符串
     * @return 去除前后空格后的字符串
     */
    public static String trimWhitespace(String str) {
        if (str == null) {
            return null;
        }
        return str.trim();
    }

    public static String sanitizeFilePath(String originalPath) {
        // 定义要替换的特殊字符
        String[] illegalChars = {"/", "\\", ":", "*", "?", "\"", "<", ">", "|", "\0"};

        // 遍历特殊字符数组，将每个特殊字符替换成下划线
        for (String illegalChar : illegalChars) {
            originalPath = originalPath.replace(illegalChar, "");
        }
        return originalPath;
    }


    /**
     * 去除字符串的前后空格和所有换行符
     *
     * @param str 要处理的字符串
     * @return 去除前后空格和换行符后的字符串
     */
    public static String trimWhitespaceAndNewLines(String str) {
        if (str == null) {
            return "";
        }
        return trimWhitespace(str);
    }

    //在光标处插入文本
    public static String insertTextAtCursor(String original, int cursorPosition, String insertion) {
        if (cursorPosition < 0 || cursorPosition > original.length()) {
            return original;
        }

        String prefix = original.substring(0, cursorPosition);
        String suffix = original.substring(cursorPosition);

        return prefix + insertion + suffix;
    }

    //格式化磁力
    public static String reformMagnet(String magnet) {
        return "magnet:?xt=urn:" + getSubString(magnet, "magnet:?xt=urn:", "&");
    }

    //判断是否为纯数字
    public static boolean isNumeric(String str) {
        Pattern pattern = Pattern.compile("[0-9]*");
        return pattern.matcher(str).matches();
    }

    /**
     * 判断字符串中是否包含中文
     *
     * @param str 待校验字符串
     * @return 是否为中文
     * @warn 不能校验是否为中文标点符号
     */
    public static boolean isContainChinese(String str) {
        Pattern p = Pattern.compile("[\u4e00-\u9fa5]");
        Matcher m = p.matcher(str);
        return m.find();
    }


    /**
     * @param str         全部的字符串
     * @param needHighStr 需要高亮的字符串
     * @return 这里默认高亮的是红色，需要自定义时，再添加重载的方法吧
     * 两个方法亲测都可以的，只是这个方法看着高大上一点
     */
    public static Spanned highStr2(String str, String needHighStr) {
        SpannableString s = new SpannableString(str);
        Pattern p = Pattern.compile(needHighStr);
        Matcher m = p.matcher(s);
        while (m.find()) {
            int start = m.start();
            int end = m.end();
            s.setSpan(new ForegroundColorSpan(Color.parseColor("#0081ff")), start, end, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        }
        return s;
    }

    /**
     * 取两个文本之间的文本值
     *
     * @param text  源文本 比如：欲取全文本为 12345
     * @param left  文本前面
     * @param right 后面文本
     * @return 返回 String
     */
    public static String getSubString(String text, String left, String right) {
        String result = "";
        int zLen;
        if (left == null || left.isEmpty()) {
            zLen = 0;
        } else {
            zLen = text.indexOf(left);
            if (zLen > -1) {
                zLen += left.length();
            } else {
                zLen = 0;
            }
        }
        int yLen = text.indexOf(right, zLen);
        if (yLen < 0 || right == null || right.isEmpty()) {
            yLen = text.length();
        }
        result = text.substring(zLen, yLen);
        return result;
    }

    /**
     * 传入txt路径读取txt文件
     *
     * @param txtPath
     * @return 返回读取到的内容
     */
    public static String readTxt(String txtPath) {
        File file = new File(txtPath);
        if (file.isFile() && file.exists()) {
            try {
                FileInputStream fileInputStream = new FileInputStream(file);
                InputStreamReader inputStreamReader = new InputStreamReader(fileInputStream);
                BufferedReader bufferedReader = new BufferedReader(inputStreamReader);

                StringBuffer sb = new StringBuffer();
                String text = null;
                while ((text = bufferedReader.readLine()) != null) {
                    sb.append(text);
                }
                fileInputStream.close();
                inputStreamReader.close();
                bufferedReader.close();
                return sb.toString();
            } catch (Exception e) {
                e.printStackTrace();
                return "";
            }
        }
        return "";
    }

    /**
     * 关键字高亮显示
     *
     * @param context 上下文
     * @param text    需要显示的文字
     * @param target  需要高亮的关键字
     * @param color   高亮颜色
     * @param start   头部增加高亮文字个数
     * @param end     尾部增加高亮文字个数
     * @return 处理完后的结果
     */
    public static SpannableString highlight(Context context, String text, String target,
                                            int color, int start, int end) {
        SpannableString spannableString = new SpannableString(text);
        Pattern pattern = Pattern.compile(target);
        Matcher matcher = pattern.matcher(text);
        while (matcher.find()) {
            ForegroundColorSpan span = new ForegroundColorSpan(color);
            spannableString.setSpan(span, matcher.start() - start, matcher.end() + end,
                    Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
        }
        return spannableString;
    }

    public static String getUrl(String content) {
        // Highlight URLs
        String regex = "(https?|ftp)://[-a-zA-Z0-9+&@#/%?=~_|!:,.;]*[-a-zA-Z0-9+&@#/%=~_|]";


        Pattern urlPattern = Pattern.compile(regex);
        Matcher matcher_url = urlPattern.matcher(content);

        if (matcher_url.find()) {
            return matcher_url.group(); // 获取匹配的链接文本
        } else {
            return "";
        }

    }

    public static String getFromAssets(Context context, String fileName) {
        try {
            InputStreamReader inputReader = new InputStreamReader(context.getResources().getAssets().open(fileName));
            BufferedReader bufReader = new BufferedReader(inputReader);
            String line = "";
            String Result = "";
            while ((line = bufReader.readLine()) != null) {
                Result += line;
            }
            inputReader.close();
            bufReader.close();
            return Result;
        } catch (Exception e) {
            e.printStackTrace();
            return "";
        }
    }

    /**
     * 替换文本中指定 start 到 end 索引之间的部分为 replacement 字符串。
     *
     * @param text        原始文本
     * @param start       开始索引（包括）
     * @param end         结束索引（不包括）
     * @param replacement 替换字符串
     * @return 替换后的文本
     */
    public static String replaceTextBetween(String text, int start, int end, String replacement) {
        if (text == null) {
            throw new IllegalArgumentException("Text cannot be null");
        }
        if (start < 0 || end > text.length() || start > end) {
            throw new IndexOutOfBoundsException("Invalid start or end index");
        }

        StringBuilder result = new StringBuilder(text);
        result.replace(start, end, replacement);
        return result.toString();
    }

    //写出文本文件
    public static void writeTxt(String path, String content) {

        if (!Objects.requireNonNull(new File(path).getParentFile()).exists()) {
            Objects.requireNonNull(new File(path).getParentFile()).mkdir();
        }

        FileOutputStream fileOutputStream = null;
        try {
            fileOutputStream = new FileOutputStream(path);
        } catch (FileNotFoundException e) {
            android.util.Log.d("Util", "e:" + e);
        }
        if (fileOutputStream != null) {
            try {
                fileOutputStream.write(content.getBytes());
                fileOutputStream.flush();
                fileOutputStream.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        } else {
            android.util.Log.d("Util", "空指针" + path);

        }

    }

    public static void adjustTitleWidth(Context context, TextView textView, String title, int width) {
        float totalWidth = 0;
        StringBuilder displayedTitle = new StringBuilder();
        float sp14 = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_SP, 10, context.getResources().getDisplayMetrics());
        float sp16 = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_SP, 16, context.getResources().getDisplayMetrics());

        for (int i = 0; i < title.length(); i++) {
            char c = title.charAt(i);
            // 判断是否为中文或全角字符
            if (isChineseOrFullWidth(c)) {
                totalWidth += sp16;
            } else {
                totalWidth += sp14;
            }

            // 如果超过指定宽度
            if (totalWidth > width) {
                if (i >= 3) {
                    displayedTitle.append(title.substring(0, i - 1)).append("...");
                } else {
                    displayedTitle.append("...");
                }

                textView.setText(displayedTitle.toString());
                return;
            }
        }

        // 未超过宽度，设置完整标题
        textView.setText(title);
    }

    // 判断是否为中文字符或全角符号
    private static boolean isChineseOrFullWidth(char c) {
        // 中文范围
        Character.UnicodeBlock ub = Character.UnicodeBlock.of(c);
        return (ub == Character.UnicodeBlock.CJK_UNIFIED_IDEOGRAPHS
                || ub == Character.UnicodeBlock.CJK_COMPATIBILITY_IDEOGRAPHS
                || ub == Character.UnicodeBlock.CJK_UNIFIED_IDEOGRAPHS_EXTENSION_A
                || ub == Character.UnicodeBlock.GENERAL_PUNCTUATION
                || ub == Character.UnicodeBlock.CJK_SYMBOLS_AND_PUNCTUATION
                || ub == Character.UnicodeBlock.HALFWIDTH_AND_FULLWIDTH_FORMS);
    }


}

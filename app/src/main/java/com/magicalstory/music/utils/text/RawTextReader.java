package com.magicalstory.music.utils.text;

/**
 * @Classname: RawTextReader
 * @Auther: Created by 奇谈君 on 2025/7/17.
 * @Description:
 */
import android.content.Context;
import android.content.res.Resources;
import android.util.Log;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

public class RawTextReader {

    /**
     * 从 res/raw 文件夹中读取文本文件并返回内容字符串
     *
     * @param context  上下文对象
     * @param resId    资源ID (例如: R.raw.myfile)
     * @return         文件内容字符串 | 读取失败返回空字符串
     */
    public static String getRawText(Context context, int resId) {
        InputStream inputStream = null;
        BufferedReader reader = null;
        StringBuilder stringBuilder = new StringBuilder();

        try {
            // 1. 通过资源ID获取输入流
            inputStream = context.getResources().openRawResource(resId);

            // 2. 使用字符流读取，避免字节到字符的转换问题
            reader = new BufferedReader(new InputStreamReader(inputStream));

            String line;
            // 3. 按行读取文本内容
            while ((line = reader.readLine()) != null) {
                stringBuilder.append(line);
                stringBuilder.append("\n");  // 保留换行符
            }

            // 4. 删除最后多余的换行符（如果需要保持原样可移除此部分）
            if (stringBuilder.length() > 0) {
                stringBuilder.deleteCharAt(stringBuilder.length() - 1);
            }

        } catch (Resources.NotFoundException e) {
            Log.e("RawTextReader", "文件未找到: " + e.getMessage());
        } catch (IOException e) {
            Log.e("RawTextReader", "读取错误: " + e.getMessage());
        } finally {
            // 5. 确保关闭资源
            try {
                if (reader != null) reader.close();
                if (inputStream != null) inputStream.close();
            } catch (IOException e) {
                Log.e("RawTextReader", "关闭资源失败: " + e.getMessage());
            }
        }

        return stringBuilder.toString();
    }
}
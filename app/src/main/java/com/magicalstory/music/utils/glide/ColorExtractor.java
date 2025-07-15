package com.magicalstory.music.utils.glide;

import android.graphics.Bitmap;
import android.graphics.Color;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.palette.graphics.Palette;
import java.util.List;

/**
 * 颜色提取工具类
 */
public class ColorExtractor {

    /**
     * 从专辑封面中提取主色调
     * @param bitmap 专辑封面图片
     * @return 主色调，如果提取失败返回默认颜色
     */
    public static int extractDominantColor(@Nullable Bitmap bitmap) {
        if (bitmap == null) {
            return Color.parseColor("#3353BE"); // 默认主色调
        }

        try {
            Palette palette = Palette.from(bitmap).generate();
            
            // 优先获取鲜艳色调
            Palette.Swatch vibrantSwatch = palette.getVibrantSwatch();
            if (vibrantSwatch != null) {
                return vibrantSwatch.getRgb();
            }
            
            // 其次获取柔和色调
            Palette.Swatch mutedSwatch = palette.getMutedSwatch();
            if (mutedSwatch != null) {
                return mutedSwatch.getRgb();
            }
            
            // 最后获取主色调
            Palette.Swatch dominantSwatch = palette.getDominantSwatch();
            if (dominantSwatch != null) {
                return dominantSwatch.getRgb();
            }
            
            // 如果都没有，从样本中取第一个
            List<Palette.Swatch> swatches = palette.getSwatches();
            if (!swatches.isEmpty()) {
                return swatches.get(0).getRgb();
            }
            
        } catch (Exception e) {
            e.printStackTrace();
        }
        
        return Color.parseColor("#3353BE"); // 默认主色调
    }

    /**
     * 从专辑封面中提取深色调
     * @param bitmap 专辑封面图片
     * @return 深色调，如果提取失败返回默认颜色
     */
    public static int extractDarkColor(@Nullable Bitmap bitmap) {
        if (bitmap == null) {
            return Color.parseColor("#1A237E"); // 默认深色调
        }

        try {
            Palette palette = Palette.from(bitmap).generate();
            
            // 优先获取深色鲜艳色调
            Palette.Swatch darkVibrantSwatch = palette.getDarkVibrantSwatch();
            if (darkVibrantSwatch != null) {
                return darkVibrantSwatch.getRgb();
            }
            
            // 其次获取深色柔和色调
            Palette.Swatch darkMutedSwatch = palette.getDarkMutedSwatch();
            if (darkMutedSwatch != null) {
                return darkMutedSwatch.getRgb();
            }
            
            // 最后获取主色调并调暗
            int dominantColor = extractDominantColor(bitmap);
            return darkenColor(dominantColor, 0.6f);
            
        } catch (Exception e) {
            e.printStackTrace();
        }
        
        return Color.parseColor("#1A237E"); // 默认深色调
    }

    /**
     * 调暗颜色
     * @param color 原始颜色
     * @param factor 调暗因子 (0.0-1.0)
     * @return 调暗后的颜色
     */
    public static int darkenColor(int color, float factor) {
        float[] hsv = new float[3];
        Color.colorToHSV(color, hsv);
        hsv[2] *= factor; // 调整亮度
        return Color.HSVToColor(hsv);
    }

    /**
     * 检查颜色是否为深色
     * @param color 要检查的颜色
     * @return 是否为深色
     */
    public static boolean isDarkColor(int color) {
        double darkness = 1 - (0.299 * Color.red(color) + 0.587 * Color.green(color) + 0.114 * Color.blue(color)) / 255;
        return darkness >= 0.5;
    }
} 
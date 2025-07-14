package com.magicalstory.music.utils.screen;

import android.content.Context;
import android.content.res.Configuration;
import android.graphics.Point;
import android.util.DisplayMetrics;

import androidx.appcompat.app.AppCompatActivity;

public class DensityUtil {

    /**
     * 根据手机的分辨率从 dp 的单位 转成为 px(像素)
     */
    public static int dip2px(Context context, float dpValue) {
        final float scale = context.getResources().getDisplayMetrics().density;
        return (int) (dpValue * scale + 0.5f);
    }
    public static int getStatusBarHeight(Context context) {
        int statusBarHeight = 0;
        int resourceId = context.getResources().getIdentifier("status_bar_height", "dimen", "android");
        if (resourceId > 0) {
            statusBarHeight = context.getResources().getDimensionPixelSize(resourceId);
        }
        return statusBarHeight;
    }

    public static int sp2px(Context var0, float var1) {
        float var2 = var0.getResources().getDisplayMetrics().scaledDensity;
        return (int) (var1 * var2 + 0.5F);
    }

    public static int px2sp(Context var0, float var1) {
        float var2 = var0.getResources().getDisplayMetrics().scaledDensity;
        return (int) (var1 / var2 + 0.5F);
    }

    /**
     * 根据手机的分辨率从 px(像素) 的单位 转成为 dp
     */
    public static int px2dip(Context context, float pxValue) {
        final float scale = context.getResources().getDisplayMetrics().density;
        return (int) (pxValue / scale + 0.5f);
    }

    //返回详情卡片封面的高宽
    public static int[] getCoverHeightAndWeight(Context context) {
        DisplayMetrics metrics = new DisplayMetrics();
        ((AppCompatActivity) context).getWindowManager().getDefaultDisplay().getMetrics(metrics);
        Point outSize = new Point();
        ((AppCompatActivity) context).getWindowManager().getDefaultDisplay().getRealSize(outSize);
        int x = outSize.x;
        int y = outSize.y;
        if (isScreenOriatationPortrait(context)) {
            if ((y / (float) x) > 1.8) {
                y = (int) (x * 1.3);
            } else {
                y = (int) (x * 1.10);
            }
        } else {
            if ((y / (float) x) > 0.6) {
                y = (int) (x * 0.3);
            } else {
                y = (int) (x * 0.3);
            }
        }


        return new int[]{x, y};
    }

    //返回裁剪封面的高宽
    public static int[] getCoverHeightAndWeight_ForCut(Context context) {
        DisplayMetrics metrics = new DisplayMetrics();
        ((AppCompatActivity) context).getWindowManager().getDefaultDisplay().getMetrics(metrics);
        Point outSize = new Point();
        ((AppCompatActivity) context).getWindowManager().getDefaultDisplay().getRealSize(outSize);
        int x = outSize.x;
        int y = outSize.y;
        if ((y / (float) x) > 1.8) {
            y = (int) (x * 1.3);
        } else {
            y = (int) (x * 1.10);
        }
        return isScreenOriatationPortrait(context) ? new int[]{x, y + DensityUtil.dip2px(context, 20)}
                : new int[]{y + DensityUtil.dip2px(context, 20), x};
    }


    //返回屏幕的高宽
    public static int[] getScreenHeightAndWeight(Context context) {
        DisplayMetrics metrics = new DisplayMetrics();
        ((AppCompatActivity) context).getWindowManager().getDefaultDisplay().getMetrics(metrics);
        Point outSize = new Point();
        ((AppCompatActivity) context).getWindowManager().getDefaultDisplay().getRealSize(outSize);
        int x = outSize.x;
        int y = outSize.y;
        return isScreenOriatationPortrait(context) ? new int[]{x, y} : new int[]{y, x};
    }


    //按照比例计算尺寸
    //fixedSize是固定的尺寸，比如屏幕宽度，targetSize是图片对应的固定宽度（长或宽），adaptiveSize就是返回另外一条边自适应缩放的px
    public static int[] calculateSizeByRrorate(int fixedSize, int targetSize, int adaptiveSize) {
        float ratio = (float) fixedSize / targetSize;
        int adaptiveSizeFinal = (int) (adaptiveSize * ratio);
        return new int[]{fixedSize, adaptiveSizeFinal};
    }

    //返回屏幕的高宽
    public static int[] getScreenHeightAndWeightForPad(Context context) {
        DisplayMetrics metrics = new DisplayMetrics();
        ((AppCompatActivity) context).getWindowManager().getDefaultDisplay().getMetrics(metrics);
        Point outSize = new Point();
        ((AppCompatActivity) context).getWindowManager().getDefaultDisplay().getRealSize(outSize);
        int x = outSize.x;
        int y = outSize.y;
        return new int[]{y, x};
    }

    /**
     * 返回当前屏幕是否为竖屏。
     *
     * @param context
     * @return 当且仅当当前屏幕为竖屏时返回true, 否则返回false。
     */
    public static boolean isScreenOriatationPortrait(Context context) {
        return context.getResources().getConfiguration().orientation == Configuration.ORIENTATION_PORTRAIT;
    }

    /**
     * 判断是否为平板
     *
     */
    public static boolean isPad(Context context) {
        return (context.getResources().getConfiguration().screenLayout & Configuration.SCREENLAYOUT_SIZE_MASK) >= Configuration.SCREENLAYOUT_SIZE_LARGE;
    }

    //判断是不是平板横屏
    public static boolean isPadHor(Context context) {
        return isPad(context) && !isScreenOriatationPortrait(context);
    }
}
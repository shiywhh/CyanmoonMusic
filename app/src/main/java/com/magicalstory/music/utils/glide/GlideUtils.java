package com.magicalstory.music.utils.glide;

/**
 * @Classname: GlideUtils
 * @Auther: Created by 奇谈君 on 2023/5/3.
 * @Description: glide工具类
 */
import android.content.Context;
import android.graphics.Bitmap;
import android.os.Build;
import android.os.StatFs;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.bumptech.glide.request.FutureTarget;

import java.io.File;
import java.util.concurrent.ExecutionException;

public class GlideUtils {

    /**
     * 获取Glide缓存大小
     *
     * @param context 上下文
     * @return Glide缓存大小，单位为字节
     */
    public static long getCacheSize(Context context) {
        try {
            File glideDir = Glide.getPhotoCacheDir(context);
            if (glideDir != null && glideDir.exists() && glideDir.isDirectory()) {
                long totalSize = 0;
                File[] files = glideDir.listFiles();
                if (files != null && files.length > 0) {
                    for (File file : files) {
                        totalSize += file.length();
                    }
                }
                return totalSize;
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return 0;
    }

    /**
     * 清除Glide全部缓存
     *
     * @param context 上下文
     */
    public static void clearAllCache(Context context) {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                Glide.get(context).clearDiskCache();
            }
            Glide.get(context).clearMemory();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * 获取手机剩余内存
     *
     * @param context 上下文
     * @return 手机剩余内存，单位为字节
     */
    public static long getAvailableMemory(Context context) {
        StatFs statFs = new StatFs(context.getCacheDir().getPath());
        long blockSize = statFs.getBlockSizeLong();
        long availableBlocks = statFs.getAvailableBlocksLong();
        return blockSize * availableBlocks;
    }
    
    /**
     * 从URL获取Bitmap对象
     *
     * @param context 上下文
     * @param url     图片URL
     * @return Bitmap对象，失败返回null
     */
    public static Bitmap getBitmapFromUrl(Context context, String url) {
        try {
            FutureTarget<Bitmap> futureTarget = Glide.with(context)
                    .asBitmap()
                    .load(url)
                    .diskCacheStrategy(DiskCacheStrategy.ALL) // 使用磁盘缓存
                    .submit();
            
            return futureTarget.get();
        } catch (ExecutionException | InterruptedException e) {
            e.printStackTrace();
            return null;
        }
    }
}

package com.magicalstory.music.utils.glide;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffXfermode;
import android.graphics.Rect;
import android.graphics.RectF;
import android.renderscript.Allocation;
import android.renderscript.Element;
import android.renderscript.RenderScript;
import android.renderscript.ScriptIntrinsicBlur;

/**
 * 高斯模糊工具类
 */
public class BlurUtils {

    /**
     * 使用RenderScript实现高斯模糊
     * @param context 上下文
     * @param bitmap 要模糊的图片
     * @param radius 模糊半径 (0.0f - 25.0f)
     * @return 模糊后的图片
     */
    public static Bitmap blur(Context context, Bitmap bitmap, float radius) {
        if (bitmap == null) return null;
        
        try {
            // 创建RenderScript上下文
            RenderScript rs = RenderScript.create(context);
            
            // 创建模糊脚本
            ScriptIntrinsicBlur blurScript = ScriptIntrinsicBlur.create(rs, Element.U8_4(rs));
            
            // 设置模糊半径
            blurScript.setRadius(Math.min(25.0f, Math.max(0.0f, radius)));
            
            // 创建输入和输出的Allocation
            Allocation inputAllocation = Allocation.createFromBitmap(rs, bitmap);
            Allocation outputAllocation = Allocation.createTyped(rs, inputAllocation.getType());
            
            // 执行模糊
            blurScript.setInput(inputAllocation);
            blurScript.forEach(outputAllocation);
            
            // 创建输出bitmap
            Bitmap blurredBitmap = Bitmap.createBitmap(bitmap.getWidth(), bitmap.getHeight(), Bitmap.Config.ARGB_8888);
            outputAllocation.copyTo(blurredBitmap);
            
            // 清理资源
            inputAllocation.destroy();
            outputAllocation.destroy();
            blurScript.destroy();
            rs.destroy();
            
            return blurredBitmap;
        } catch (Exception e) {
            e.printStackTrace();
            // 如果RenderScript失败，返回原始图片
            return bitmap;
        }
    }

    /**
     * 使用简单的方法实现高斯模糊（作为RenderScript的备用方案）
     * @param bitmap 要模糊的图片
     * @param radius 模糊半径
     * @return 模糊后的图片
     */
    public static Bitmap fastBlur(Bitmap bitmap, int radius) {
        if (bitmap == null) return null;
        
        try {
            Bitmap blurredBitmap = bitmap.copy(Bitmap.Config.ARGB_8888, true);
            
            if (radius < 1) {
                return blurredBitmap;
            }
            
            int w = blurredBitmap.getWidth();
            int h = blurredBitmap.getHeight();
            
            int[] pix = new int[w * h];
            blurredBitmap.getPixels(pix, 0, w, 0, 0, w, h);
            
            int wm = w - 1;
            int hm = h - 1;
            int wh = w * h;
            int div = radius + radius + 1;
            
            int r[] = new int[wh];
            int g[] = new int[wh];
            int b[] = new int[wh];
            int rsum, gsum, bsum, x, y, i, p, yp, yi, yw;
            int vmin[] = new int[Math.max(w, h)];
            
            int divsum = (div + 1) >> 1;
            divsum *= divsum;
            int dv[] = new int[256 * divsum];
            for (i = 0; i < 256 * divsum; i++) {
                dv[i] = (i / divsum);
            }
            
            yw = yi = 0;
            
            int[][] stack = new int[div][3];
            int stackpointer;
            int stackstart;
            int[] sir;
            int rbs;
            int r1 = radius + 1;
            int routsum, goutsum, boutsum;
            int rinsum, ginsum, binsum;
            
            for (y = 0; y < h; y++) {
                rinsum = ginsum = binsum = routsum = goutsum = boutsum = rsum = gsum = bsum = 0;
                for (i = -radius; i <= radius; i++) {
                    p = pix[yi + Math.min(wm, Math.max(i, 0))];
                    sir = stack[i + radius];
                    sir[0] = (p & 0xff0000) >> 16;
                    sir[1] = (p & 0x00ff00) >> 8;
                    sir[2] = (p & 0x0000ff);
                    rbs = r1 - Math.abs(i);
                    rsum += sir[0] * rbs;
                    gsum += sir[1] * rbs;
                    bsum += sir[2] * rbs;
                    if (i > 0) {
                        rinsum += sir[0];
                        ginsum += sir[1];
                        binsum += sir[2];
                    } else {
                        routsum += sir[0];
                        goutsum += sir[1];
                        boutsum += sir[2];
                    }
                }
                stackpointer = radius;
                
                for (x = 0; x < w; x++) {
                    
                    r[yi] = dv[rsum];
                    g[yi] = dv[gsum];
                    b[yi] = dv[bsum];
                    
                    rsum -= routsum;
                    gsum -= goutsum;
                    bsum -= boutsum;
                    
                    stackstart = stackpointer - radius + div;
                    sir = stack[stackstart % div];
                    
                    routsum -= sir[0];
                    goutsum -= sir[1];
                    boutsum -= sir[2];
                    
                    if (y == 0) {
                        vmin[x] = Math.min(x + radius + 1, wm);
                    }
                    p = pix[yw + vmin[x]];
                    
                    sir[0] = (p & 0xff0000) >> 16;
                    sir[1] = (p & 0x00ff00) >> 8;
                    sir[2] = (p & 0x0000ff);
                    
                    rinsum += sir[0];
                    ginsum += sir[1];
                    binsum += sir[2];
                    
                    rsum += rinsum;
                    gsum += ginsum;
                    bsum += binsum;
                    
                    stackpointer = (stackpointer + 1) % div;
                    sir = stack[(stackpointer) % div];
                    
                    routsum += sir[0];
                    goutsum += sir[1];
                    boutsum += sir[2];
                    
                    rinsum -= sir[0];
                    ginsum -= sir[1];
                    binsum -= sir[2];
                    
                    yi++;
                }
                yw += w;
            }
            for (x = 0; x < w; x++) {
                rinsum = ginsum = binsum = routsum = goutsum = boutsum = rsum = gsum = bsum = 0;
                yp = -radius * w;
                for (i = -radius; i <= radius; i++) {
                    yi = Math.max(0, yp) + x;
                    
                    sir = stack[i + radius];
                    
                    sir[0] = r[yi];
                    sir[1] = g[yi];
                    sir[2] = b[yi];
                    
                    rbs = r1 - Math.abs(i);
                    
                    rsum += r[yi] * rbs;
                    gsum += g[yi] * rbs;
                    bsum += b[yi] * rbs;
                    
                    if (i > 0) {
                        rinsum += sir[0];
                        ginsum += sir[1];
                        binsum += sir[2];
                    } else {
                        routsum += sir[0];
                        goutsum += sir[1];
                        boutsum += sir[2];
                    }
                    
                    if (i < hm) {
                        yp += w;
                    }
                }
                yi = x;
                stackpointer = radius;
                for (y = 0; y < h; y++) {
                    
                    pix[yi] = (0xff000000 & pix[yi]) | (dv[rsum] << 16) | (dv[gsum] << 8) | dv[bsum];
                    
                    rsum -= routsum;
                    gsum -= goutsum;
                    bsum -= boutsum;
                    
                    stackstart = stackpointer - radius + div;
                    sir = stack[stackstart % div];
                    
                    routsum -= sir[0];
                    goutsum -= sir[1];
                    boutsum -= sir[2];
                    
                    if (x == 0) {
                        vmin[y] = Math.min(y + r1, hm) * w;
                    }
                    p = x + vmin[y];
                    
                    sir[0] = r[p];
                    sir[1] = g[p];
                    sir[2] = b[p];
                    
                    rinsum += sir[0];
                    ginsum += sir[1];
                    binsum += sir[2];
                    
                    rsum += rinsum;
                    gsum += ginsum;
                    bsum += binsum;
                    
                    stackpointer = (stackpointer + 1) % div;
                    sir = stack[stackpointer];
                    
                    routsum += sir[0];
                    goutsum += sir[1];
                    boutsum += sir[2];
                    
                    rinsum -= sir[0];
                    ginsum -= sir[1];
                    binsum -= sir[2];
                    
                    yi += w;
                }
            }
            
            blurredBitmap.setPixels(pix, 0, w, 0, 0, w, h);
            return blurredBitmap;
        } catch (Exception e) {
            e.printStackTrace();
            return bitmap;
        }
    }

    /**
     * 缩放图片
     * @param bitmap 原始图片
     * @param scale 缩放比例
     * @return 缩放后的图片
     */
    public static Bitmap scaleBitmap(Bitmap bitmap, float scale) {
        if (bitmap == null) return null;
        
        int width = Math.round(bitmap.getWidth() * scale);
        int height = Math.round(bitmap.getHeight() * scale);
        
        if (width <= 0 || height <= 0) return bitmap;
        
        return Bitmap.createScaledBitmap(bitmap, width, height, true);
    }
} 
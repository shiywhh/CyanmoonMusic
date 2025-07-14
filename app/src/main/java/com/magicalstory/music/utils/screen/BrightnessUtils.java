package com.magicalstory.music.utils.screen;

import android.app.Activity;
import android.content.ContentResolver;
import android.content.Context;
import android.provider.Settings;
import android.view.Window;
import android.view.WindowManager;

/**
 * 屏幕亮度工具类
 */
public class BrightnessUtils {

    /**
     * 获取系统亮度（0-100）
     */
    public static int getBrightness(Context context) {
        int brightness = 0;
        try {
            brightness = Settings.System.getInt(context.getContentResolver(), Settings.System.SCREEN_BRIGHTNESS);
            // 转换为0-100的范围
            brightness = brightness * 100 / 255;
        } catch (Settings.SettingNotFoundException e) {
            e.printStackTrace();
        }
        return brightness;
    }

    /**
     * 设置系统亮度（0-100）
     */
    public static void setBrightness(Context context, int brightness) {
        // 检查是否有写入设置的权限
        if (!Settings.System.canWrite(context)) {
            return;
        }
        
        // 将0-100的亮度转换为系统的0-255
        int systemBrightness = brightness * 255 / 100;
        // 限制范围在0-255
        systemBrightness = Math.max(0, Math.min(255, systemBrightness));
        
        ContentResolver resolver = context.getContentResolver();
        try {
            // 修改系统亮度
            Settings.System.putInt(resolver, Settings.System.SCREEN_BRIGHTNESS, systemBrightness);
            
            // 如果是Activity，也修改当前窗口亮度
            if (context instanceof Activity) {
                setWindowBrightness((Activity) context, systemBrightness);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    
    /**
     * 设置窗口亮度
     */
    private static void setWindowBrightness(Activity activity, int brightness) {
        Window window = activity.getWindow();
        WindowManager.LayoutParams lp = window.getAttributes();
        lp.screenBrightness = brightness / 255f;
        window.setAttributes(lp);
    }
} 
package com.magicalstory.music.utils.app;

import android.content.Context;


/**
 * @Classname: ToastUtils
 * @Auther: Created by 奇谈君 on 2022/6/14.
 * @Description:
 */
public class ToastUtils {

    //显示toast
    public static void showToast(Context context, String content) {
        //ToastUtils.showToast(context, content);
        CustomToast.showToast(context,content,500);
    }
}

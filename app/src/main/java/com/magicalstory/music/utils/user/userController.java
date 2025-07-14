package com.magicalstory.music.utils.user;

import com.tencent.mmkv.MMKV;

/**
 * @Classname: userController
 * @Auther: Created by 奇谈君 on 2023/2/25.
 * @Description:用户系统控制器
 */
public class userController {

    public static String getUserName() {
        return MMKV.defaultMMKV().getString("userName", "");
    }

    public static String getUserUUID() {
        return MMKV.defaultMMKV().getString("uuid", "");
    }

    public static String getUserIcon() {
        return MMKV.defaultMMKV().getString("icon", "");
    }

    public static boolean isAdmin() {
        System.out.println("MMKV.defaultMMKV().getInt(\"admin\", 0) = " + MMKV.defaultMMKV().getInt("admin", 0));
        return MMKV.defaultMMKV().getInt("admin", 0) == 1;
    }


    public static long getVipTime() {
        return MMKV.defaultMMKV().getLong("vipTime", 0);
    }

    public static void setVipTime(long vipTime) {
        MMKV.defaultMMKV().putLong("vipTime", vipTime);
    }

    //是否已经登录了
    public static boolean hasLogin() {
        return !MMKV.defaultMMKV().decodeString("token", "##-##").equals("##-##");
    }


    public static void setToken(String token) {
        MMKV.defaultMMKV().encode("token", token);
    }

    //退出登录
    public static void loginout() {
        MMKV.defaultMMKV().remove("token");
        MMKV.defaultMMKV().remove("userName");
        MMKV.defaultMMKV().remove("icon");
        MMKV.defaultMMKV().remove("uuid");
        MMKV.defaultMMKV().remove("vipTime");
        MMKV.defaultMMKV().remove("admin");
        MMKV.defaultMMKV().remove("phoneNumber");
        MMKV.defaultMMKV().remove("MBBS_USER_TOKEN");

        MMKV.defaultMMKV().remove("favor_activetime");
        MMKV.defaultMMKV().remove("todo_active_time");
        MMKV.defaultMMKV().remove("asset_active_time");
        MMKV.defaultMMKV().remove("day_active_time");
        MMKV.defaultMMKV().remove("subscription_active_time");
        MMKV.defaultMMKV().remove("pomodoro_active_time");
        MMKV.defaultMMKV().remove("habit_active_time");
        MMKV.defaultMMKV().remove("menstrual_active_time");
    }


    public static boolean hasPhoneNumber() {
        return MMKV.defaultMMKV().containsKey("phoneNumber");
    }


    public static String getToken() {
        return MMKV.defaultMMKV().getString("token", "");
    }
}

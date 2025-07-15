package com.magicalstory.music;

import android.app.Application;

import com.magicalstory.music.utils.network.NetworkUtils;
import com.tencent.mmkv.BuildConfig;
import com.tencent.mmkv.MMKV;

import org.litepal.LitePal;



/**
 * @Classname: applicantion
 * @Auther: Created by 奇谈君 on 2025/7/13.
 * @Description:
 */
public class applicantion extends Application {
    @Override
    public void onCreate() {
        super.onCreate();
        MMKV.initialize(this);
        LitePal.initialize(this);
        NetworkUtils.initialize(this);
        //Fragmentation.builder()
        //        // show stack view. Mode: BUBBLE, SHAKE, NONE
        //        .stackViewMode(Fragmentation.BUBBLE)
        //        .debug(BuildConfig.DEBUG)
        //        .install();

    }
}

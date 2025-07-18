package com.magicalstory.music.dialog;

import android.content.Context;

import androidx.annotation.NonNull;

import com.google.android.material.bottomsheet.BottomSheetBehavior;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.magicalstory.music.utils.screen.DensityUtil;


/**
 * @Classname: bottomSheetDialog
 * @Auther: Created by 奇谈君 on 2022/10/9.
 * @Description: 解决横屏显示不全
 */
public class bottomSheetDialog extends BottomSheetDialog {
   public Context context;

    public bottomSheetDialog(@NonNull Context context) {
        super(context);
        this.context = context;

    }

    @Override

    protected void onStart() {
        super.onStart();
        if (!DensityUtil.isScreenOriatationPortrait(context)) {
            BottomSheetBehavior behavior = getBehavior();
            behavior.setState(BottomSheetBehavior.STATE_EXPANDED);
        }

    }
}

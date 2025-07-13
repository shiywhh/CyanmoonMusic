package com.magicalstory.music.utils.app;

/**
 * @Classname: CustomToast
 * @Auther: Created by 奇谈君 on 2024/7/8.
 * @Description:自定义toast
 */
// CustomToast.java
import android.content.Context;
import android.os.Handler;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.animation.AlphaAnimation;
import android.widget.TextView;
import android.widget.Toast;

import com.magicalstory.music.R;


public class CustomToast {
    public static void showToast(Context context, String message, int duration) {
        LayoutInflater inflater = LayoutInflater.from(context);
        View layout = inflater.inflate(R.layout.custom_toast, null);

        TextView text = layout.findViewById(R.id.toast_text);
        text.setText(message);

        Toast toast = new Toast(context);
        toast.setDuration(duration);
        toast.setView(layout);
        toast.setGravity(Gravity.CENTER, 0, 0);

        // Animation
        AlphaAnimation fadeIn = new AlphaAnimation(0.0f, 1.0f);
        fadeIn.setDuration(500); // Fade in duration
        fadeIn.setFillAfter(true);

        AlphaAnimation fadeOut = new AlphaAnimation(1.0f, 0.0f);
        fadeOut.setDuration(500); // Fade out duration
        fadeOut.setFillAfter(true);
        fadeOut.setStartOffset(duration - 500);

        layout.startAnimation(fadeIn);

        // Set the fade-out animation to start just before the toast disappears
        new Handler().postDelayed(() -> layout.startAnimation(fadeOut), duration - 500);

        toast.show();
    }
}

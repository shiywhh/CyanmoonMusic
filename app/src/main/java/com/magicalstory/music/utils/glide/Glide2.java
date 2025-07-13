package com.magicalstory.music.utils.glide;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.widget.ImageView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.DecodeFormat;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.bumptech.glide.load.model.GlideUrl;
import com.bumptech.glide.request.RequestOptions;
import com.bumptech.glide.request.target.CustomTarget;
import com.bumptech.glide.request.target.SimpleTarget;
import com.bumptech.glide.request.transition.Transition;
import com.bumptech.glide.signature.EmptySignature;
import com.bumptech.glide.util.Util;

import java.security.MessageDigest;

/**
 * @Classname: Glide2
 * @Auther: Created by 奇谈君 on 2024/5/22.
 * @Description:
 */
public class Glide2 {
    private static final int MAX_BITMAP_SIZE = 100 * 1024 * 1024; // 100 MB
    private static final String host = "https://cdn.magicalapk.com";
    public interface sizeChangeListener {
        void getSuccess(int height, int width);
    }

    public static void loadImage(Context context, ImageView view, Object url, int placeHolder) {
        if (context instanceof AppCompatActivity && ((AppCompatActivity) context).isDestroyed()) {
            return;
        }

        if (url instanceof String) {
            Glide.with(context).load(reformUrl((String) url)).apply(new RequestOptions().format(DecodeFormat.PREFER_ARGB_8888)).placeholder(placeHolder).error(placeHolder).into(view);
        } else {
            Glide.with(context).load(url).placeholder(placeHolder).apply(new RequestOptions().format(DecodeFormat.PREFER_ARGB_8888)).error(placeHolder).into(view);
        }
    }

    public static void loadImageWithSizeBack(Context context, ImageView view, Object url, int placeHolder, sizeChangeListener sizeChangeListener) {
        if (context instanceof AppCompatActivity && ((AppCompatActivity) context).isDestroyed()) {
            return;
        }

        Glide.with(context)
                .load(reformUrl((String) url))
                .apply(new RequestOptions().format(DecodeFormat.PREFER_ARGB_8888))
                .placeholder(placeHolder)
                .skipMemoryCache(true)
                .diskCacheStrategy(DiskCacheStrategy.NONE)
                .error(placeHolder)
                .into(new SimpleTarget<Drawable>() {
                    @Override
                    public void onResourceReady(Drawable resource, Transition<? super Drawable> transition) {
                        // 获取图片的宽高
                        int width = resource.getIntrinsicWidth();
                        int height = resource.getIntrinsicHeight();

                        // 如果是Bitmap类型的图片
                        if (resource instanceof BitmapDrawable) {
                            Bitmap bitmap = ((BitmapDrawable) resource).getBitmap();
                            width = bitmap.getWidth();
                            height = bitmap.getHeight();
                            sizeChangeListener.getSuccess(height, width);
                        } else {
                            sizeChangeListener.getSuccess(height, width);
                        }
                        view.setImageDrawable(resource);
                    }
                });

    }

    public static void loadImage(Context context, ImageView view, String url, int placeHolder, boolean skipMemoryCache) {
        if (context instanceof AppCompatActivity && ((AppCompatActivity) context).isDestroyed()) {
            return;
        }
        Glide.with(context).load(reformUrl(url)).skipMemoryCache(skipMemoryCache).apply(new RequestOptions().format(DecodeFormat.PREFER_ARGB_8888)).diskCacheStrategy(skipMemoryCache ? DiskCacheStrategy.NONE :
                DiskCacheStrategy.ALL).placeholder(placeHolder).error(placeHolder).into(view);
    }

    public static String reformUrl(String url) {
        if (url.startsWith("host")) {
            return url.replace("host", host);
        } else {
            return url;
        }
    }

    public static String replaceUrl(String url) {

        return url.replace("host", host);

    }

    public static void loadImage(Context context, ImageView view, String url, int placeHolder, int height, int width) {
        if (context instanceof AppCompatActivity && ((AppCompatActivity) context).isDestroyed()) {
            return;
        }

        int scale = 1;
        if (height > 5000 || width > 5000) {
            scale = 2;
        }
        while (width * height * 4 / scale > MAX_BITMAP_SIZE) {
            scale *= 2;
        }


        view.getLayoutParams().width = width;
        view.getLayoutParams().height = height;
        view.requestLayout();
        Glide.with(context).load(reformUrl(url)).placeholder(placeHolder).override(width / scale, height / scale).apply(new RequestOptions().format(DecodeFormat.PREFER_ARGB_8888)).error(placeHolder).into(view);
    }

    public static void loadImage(Context context, ImageView view, String url, int placeHolder, int height, int width, int scale) {
        if (context instanceof AppCompatActivity && ((AppCompatActivity) context).isDestroyed()) {
            return;
        }

        if (scale == 1) {
            if (height > 5000 || width > 5000) {
                scale = 2;
            }

            while (width * height * 4 / scale > MAX_BITMAP_SIZE) {
                scale *= 2;
            }
        }


        view.getLayoutParams().width = width;
        view.getLayoutParams().height = height;
        view.requestLayout();
        Glide.with(context).load(reformUrl(url)).placeholder(placeHolder).override(width / scale, height / scale).apply(new RequestOptions().format(DecodeFormat.PREFER_ARGB_8888)).error(placeHolder).into(view);
    }

    // 新添加的方法，根据固定宽度动态调整ImageView的高度
    public static void loadImageWithFixedWidth(Context context, ImageView view, String url, int placeHolder, int fixedWidth) {
        if (context instanceof AppCompatActivity && ((AppCompatActivity) context).isDestroyed()) {
            return;
        }
        Glide.with(context)
                .load(reformUrl(url))
                .placeholder(placeHolder).apply(new RequestOptions().format(DecodeFormat.PREFER_ARGB_8888))
                .error(placeHolder)
                .into(new CustomTarget<Drawable>() {
                    @Override
                    public void onResourceReady(@NonNull Drawable resource, @Nullable Transition<? super Drawable> transition) {
                        int originalWidth = resource.getIntrinsicWidth();
                        int originalHeight = resource.getIntrinsicHeight();
                        int newHeight = (int) ((float) fixedWidth / originalWidth * originalHeight);

                        view.getLayoutParams().width = fixedWidth;
                        view.getLayoutParams().height = newHeight;
                        view.requestLayout();
                        view.setImageDrawable(resource);
                    }

                    @Override
                    public void onLoadCleared(@Nullable Drawable placeholder) {
                        view.setImageDrawable(placeholder);
                    }
                });
    }

    private static String getGlide4_SafeKey(String url) {
        try {
            MessageDigest messageDigest = MessageDigest.getInstance("SHA-256");
            EmptySignature signature = EmptySignature.obtain();
            signature.updateDiskCacheKey(messageDigest);
            new GlideUrl(url).updateDiskCacheKey(messageDigest);
            String safeKey = Util.sha256BytesToHex(messageDigest.digest());
            return safeKey + ".0";
        } catch (Exception e) {
            return "";
        }
    }
}

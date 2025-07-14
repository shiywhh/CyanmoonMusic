package com.magicalstory.music.utils.network;

/**
 * @Classname: ImageDownloader
 * @Auther: Created by 奇谈君 on 2023/5/11.
 * @Description:图片下载器
 */
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

public class ImageDownloader {

    public interface DownloadListener {
        void onSuccess(File file);

        void onFailure(Exception e);
    }

    public static void downloadImage(String imageUrl, String savePath, final DownloadListener listener) {
        OkHttpClient client = new OkHttpClient();
        Request request = new Request.Builder()
                .url(imageUrl)
                .build();

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onResponse(Call call, Response response) throws IOException {
                if (response.isSuccessful()) {
                    FileOutputStream outputStream = null;
                    try {
                        File file = new File(savePath);
                        outputStream = new FileOutputStream(file);
                        outputStream.write(response.body().bytes());
                        outputStream.close();
                        listener.onSuccess(file);
                    } catch (Exception e) {
                        e.printStackTrace();
                        if (outputStream != null) {
                            outputStream.close();
                        }
                        listener.onFailure(e);
                    }
                } else {
                    listener.onFailure(new Exception("Download failed: " + response.code()));
                }
                response.close();
            }

            @Override
            public void onFailure(Call call, IOException e) {
                listener.onFailure(e);
            }
        });
    }

    public static void downloadImage(String imageUrl, String savePath, String referer, final DownloadListener listener) {
        OkHttpClient client = new OkHttpClient();
        Request request = new Request.Builder()
                .url(imageUrl)
                .addHeader("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/91.0.4472.124 Safari/537.36")
                .addHeader("Referer", referer)
                .build();

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onResponse(Call call, Response response) throws IOException {
                if (response.isSuccessful()) {
                    FileOutputStream outputStream = null;
                    try {
                        File file = new File(savePath);
                        outputStream = new FileOutputStream(file);
                        outputStream.write(response.body().bytes());
                        outputStream.close();
                        listener.onSuccess(file);
                    } catch (Exception e) {
                        e.printStackTrace();
                        if (outputStream != null) {
                            outputStream.close();
                        }
                        listener.onFailure(e);
                    }
                } else {
                    listener.onFailure(new Exception("Download failed: " + response.code()));
                }
                response.close();
            }

            @Override
            public void onFailure(Call call, IOException e) {
                listener.onFailure(e);
            }
        });
    }

}

package com.magicalstory.music.utils.file;

import android.app.Activity;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.provider.DocumentsContract;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;

import com.magicalstory.music.model.Playlist;
import com.magicalstory.music.model.Song;

import org.litepal.LitePal;

import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.util.List;

/**
 * SAF (Storage Access Framework) 工具类
 * 用于处理文件保存等操作
 */
public class SafUtils {
    private static final String TAG = "SafUtils";
    private static final int REQUEST_CREATE_DOCUMENT = 1001;

    /**
     * 保存播放列表到文件
     */
    public static void savePlaylistToFile(@NonNull Fragment fragment, @NonNull Playlist playlist) {
        // 创建保存文件的Intent
        Intent intent = new Intent(Intent.ACTION_CREATE_DOCUMENT);
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        intent.setType("text/plain");
        intent.putExtra(Intent.EXTRA_TITLE, playlist.getName() + ".m3u");

        // 使用Fragment的startActivityForResult
        fragment.startActivityForResult(intent, REQUEST_CREATE_DOCUMENT);
    }

    /**
     * 处理Activity结果
     */
    public static void handleActivityResult(Fragment fragment, int requestCode, int resultCode, Intent data, Playlist playlist) {
        if (requestCode == REQUEST_CREATE_DOCUMENT && resultCode == Activity.RESULT_OK && data != null) {
            Uri uri = data.getData();
            if (uri != null) {
                writePlaylistToUri(fragment.requireContext(), playlist, uri);
            }
        }
    }

    /**
     * 将播放列表写入到指定的URI
     */
    private static void writePlaylistToUri(Context context, Playlist playlist, Uri uri) {
        try {
            ContentResolver resolver = context.getContentResolver();
            
            // 获取播放列表中的歌曲
            List<Song> songs = playlist.getSongs();
            
            try (OutputStream outputStream = resolver.openOutputStream(uri);
                 Writer writer = new OutputStreamWriter(outputStream, "UTF-8")) {
                
                // 写入M3U文件头
                writer.write("#EXTM3U\n");
                writer.write("# Playlist: " + playlist.getName() + "\n");
                if (playlist.getDescription() != null && !playlist.getDescription().isEmpty()) {
                    writer.write("# Description: " + playlist.getDescription() + "\n");
                }
                writer.write("# Created: " + new java.util.Date(playlist.getCreatedTime()) + "\n\n");
                
                // 写入歌曲信息
                for (Song song : songs) {
                    // 写入扩展信息
                    writer.write("#EXTINF:" + (song.getDuration() / 1000) + "," + 
                               song.getArtist() + " - " + song.getTitle() + "\n");
                    
                    // 写入文件路径
                    writer.write(song.getPath() + "\n");
                }
                
                writer.flush();
                
                Log.d(TAG, "播放列表已保存到: " + uri);
                com.magicalstory.music.utils.app.ToastUtils.showToast(context, 
                    "播放列表已保存: " + playlist.getName() + ".m3u");
                
            } catch (IOException e) {
                Log.e(TAG, "保存播放列表失败", e);
                com.magicalstory.music.utils.app.ToastUtils.showToast(context, 
                    "保存失败: " + e.getMessage());
            }
            
        } catch (Exception e) {
            Log.e(TAG, "保存播放列表时发生错误", e);
            com.magicalstory.music.utils.app.ToastUtils.showToast(context, 
                "保存失败: " + e.getMessage());
        }
    }

    /**
     * 检查是否有写入外部存储的权限
     */
    public static boolean hasWritePermission(Context context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            // Android 11+ 使用SAF，不需要特殊权限
            return true;
        } else {
            // Android 10及以下需要WRITE_EXTERNAL_STORAGE权限
            return context.checkSelfPermission(android.Manifest.permission.WRITE_EXTERNAL_STORAGE) 
                   == android.content.pm.PackageManager.PERMISSION_GRANTED;
        }
    }
} 
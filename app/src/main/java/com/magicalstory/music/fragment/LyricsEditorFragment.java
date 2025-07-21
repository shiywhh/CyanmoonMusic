package com.magicalstory.music.fragment;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.media3.common.util.UnstableApi;
import androidx.navigation.Navigation;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import com.magicalstory.music.R;
import com.magicalstory.music.base.BaseFragment;
import com.magicalstory.music.databinding.FragmentLyricsEditorBinding;
import com.magicalstory.music.model.Song;
import com.magicalstory.music.utils.app.ToastUtils;
import com.magicalstory.music.utils.glide.GlideUtils;
import com.magicalstory.music.utils.lyrics.LyricsParser;
import com.magicalstory.music.model.LyricLine;
import com.magicalstory.music.dialog.dialogUtils;
import com.magicalstory.music.utils.network.NetUtils;
import com.magicalstory.music.utils.VersionUtils;
import com.magicalstory.music.utils.tag.TagWriter;

import org.jaudiotagger.tag.FieldKey;

import org.jaudiotagger.audio.AudioFileIO;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.Map;
import java.util.HashMap;

/**
 * 歌词编辑器Fragment
 * 用于编辑单首歌曲的歌词
 */
@UnstableApi
public class LyricsEditorFragment extends BaseFragment<FragmentLyricsEditorBinding> {

    private static final String TAG = "LyricsEditorFragment";
    private static final String ARG_SONG = "song";

    private Song currentSong;
    private ExecutorService executorService;
    private Handler mainHandler;
    private List<LyricLine> currentLyrics = new ArrayList<>();
    private ActivityResultLauncher<androidx.activity.result.IntentSenderRequest> writeRequestLauncher;

    public static LyricsEditorFragment newInstance(Song song) {
        LyricsEditorFragment fragment = new LyricsEditorFragment();
        Bundle args = new Bundle();
        args.putSerializable(ARG_SONG, song);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    protected FragmentLyricsEditorBinding getViewBinding(LayoutInflater inflater, ViewGroup container) {
        return FragmentLyricsEditorBinding.inflate(inflater, container, false);
    }

    @Override
    protected boolean usePersistentView() {
        return true;
    }

    @Override
    protected int getLayoutRes() {
        return R.layout.fragment_lyrics_editor;
    }

    @Override
    protected FragmentLyricsEditorBinding bindPersistentView(View view) {
        return FragmentLyricsEditorBinding.bind(view);
    }

    @Override
    protected void initViewForPersistentView() {
        super.initViewForPersistentView();
        // 初始化Handler和线程池
        mainHandler = new Handler(Looper.getMainLooper());
        executorService = Executors.newCachedThreadPool();

        // 初始化MediaStore写入请求启动器
        writeRequestLauncher = registerForActivityResult(
                new ActivityResultContracts.StartIntentSenderForResult(),
                result -> {
                    if (result.getResultCode() == Activity.RESULT_OK) {
                        // 写入成功，执行文件替换
                        performFileReplacement();
                    } else {
                        // 写入失败
                        ToastUtils.showToast(getContext(), "歌词保存失败");
                    }
                }
        );

        // 获取传递的歌曲数据
        Bundle args = getArguments();
        if (args != null) {
            currentSong = (Song) args.getSerializable(ARG_SONG);
        }

        if (currentSong == null) {
            ToastUtils.showToast(getContext(), "歌曲数据为空");
            Navigation.findNavController(requireView()).popBackStack();
            return;
        }

        // 加载歌曲数据
        loadSongData();
    }

    @Override
    protected void initListenerForPersistentView() {
        super.initListenerForPersistentView();

        // 返回按钮
        binding.toolbar.setNavigationOnClickListener(v -> {
            Navigation.findNavController(requireView()).popBackStack();
        });

        // 保存按钮
        binding.buttonSave.setOnClickListener(v -> {
            saveLyrics();
        });

        // 预览按钮
        binding.buttonPreview.setOnClickListener(v -> {
            previewLyrics();
        });

        // 清空按钮
        binding.buttonClear.setOnClickListener(v -> {
            clearLyrics();
        });

        binding.buttonSearch.setOnClickListener(v -> NetUtils.goUrl("https://cn.bing.com/search?q="+currentSong.getTitle()+" 歌词",context));
    }

    /**
     * 加载歌曲数据
     */
    private void loadSongData() {
        executorService.execute(() -> {
            try {
                // 读取现有歌词
                loadExistingLyrics();

                // 在主线程更新UI
                mainHandler.post(() -> {
                    updateUI();
                    loadSongCover();
                });

            } catch (Exception e) {
                Log.e(TAG, "加载歌曲数据失败", e);
                mainHandler.post(() -> {
                    ToastUtils.showToast(getContext(), "加载歌曲数据失败");
                });
            }
        });
    }

    /**
     * 读取现有歌词
     */
    private void loadExistingLyrics() {
        if (currentSong == null || currentSong.getPath() == null) {
            return;
        }

        try {
            // 从歌曲文件解析歌词
            List<LyricLine> lyrics = LyricsParser.parseLyricsFromSong(context, currentSong.getPath());
            
            mainHandler.post(() -> {
                currentLyrics.clear();
                currentLyrics.addAll(lyrics);
                
                // 将歌词转换为文本格式显示在编辑器中
                String lyricsText = convertLyricsToText(currentLyrics);
                binding.etLyrics.setText(lyricsText);
                
                Log.d(TAG, "加载现有歌词完成，共 " + lyrics.size() + " 行");
            });

        } catch (Exception e) {
            Log.e(TAG, "读取现有歌词失败", e);
        }
    }

    /**
     * 将歌词列表转换为文本格式
     */
    private String convertLyricsToText(List<LyricLine> lyrics) {
        if (lyrics == null || lyrics.isEmpty()) {
            return "";
        }

        StringBuilder sb = new StringBuilder();
        for (LyricLine line : lyrics) {
            if (line.getStartTime() > 0) {
                // 有时间标签的歌词
                long timeMs = line.getStartTime();
                long minutes = timeMs / (60 * 1000);
                long seconds = (timeMs % (60 * 1000)) / 1000;
                long centiseconds = (timeMs % 1000) / 10;
                
                sb.append(String.format("[%02d:%02d.%02d]", minutes, seconds, centiseconds));
            }
            sb.append(line.getContent()).append("\n");
        }
        
        return sb.toString();
    }

    /**
     * 更新UI
     */
    private void updateUI() {
        if (currentSong == null) {
            return;
        }

        // 设置歌曲标题
        binding.tvSongTitle.setText(currentSong.getTitle());

        // 设置艺术家和专辑信息
        String artistAlbum = currentSong.getArtist();
        if (currentSong.getAlbum() != null && !currentSong.getAlbum().isEmpty()) {
            artistAlbum += " • " + currentSong.getAlbum();
        }
        binding.tvSongArtist.setText(artistAlbum);
    }

    /**
     * 加载歌曲封面
     */
    private void loadSongCover() {
        if (currentSong == null) {
            return;
        }

        // 加载专辑封面
        GlideUtils.loadAlbumCover(requireContext(), currentSong.getAlbumId(), binding.ivSongCover);
    }

    /**
     * 保存歌词
     */
    private void saveLyrics() {
        if (currentSong == null) {
            ToastUtils.showToast(getContext(), "歌曲数据为空");
            return;
        }

        String lyricsText = binding.etLyrics.getText().toString().trim();
        if (TextUtils.isEmpty(lyricsText)) {
            ToastUtils.showToast(getContext(), "歌词内容不能为空");
            return;
        }

        // 解析歌词文本
        List<LyricLine> parsedLyrics = parseLyricsText(lyricsText);
        if (parsedLyrics.isEmpty()) {
            ToastUtils.showToast(getContext(), "歌词格式不正确");
            return;
        }

        // 构建歌词内容
        String lyricsContent = buildLyricsContent(parsedLyrics);

        // 根据Android版本选择写入方法
        if (VersionUtils.hasR()) {
            // Android 11及以上版本
            List<File> cacheFiles = writeLyricsToFileR(lyricsContent);
            if (!cacheFiles.isEmpty()) {
                // 处理缓存文件
                handleCacheFiles(cacheFiles);
            } else {
                ToastUtils.showToast(getContext(), "歌词保存失败");
            }
        } else {
            // Android 10及以下版本
            executorService.execute(() -> {
                try {
                    boolean success = writeLyricsToFileDirect(lyricsContent);
                    mainHandler.post(() -> {
                        if (success) {
                            ToastUtils.showToast(getContext(), "歌词保存成功");
                            // 通知FullPlayerFragment刷新歌词
                            notifyLyricsUpdated();
                            Navigation.findNavController(requireView()).popBackStack();
                        } else {
                            ToastUtils.showToast(getContext(), "歌词保存失败");
                        }
                    });
                } catch (Exception e) {
                    Log.e(TAG, "保存歌词失败", e);
                    mainHandler.post(() -> {
                        ToastUtils.showToast(getContext(), "保存歌词失败: " + e.getMessage());
                    });
                }
            });
        }
    }

    /**
     * 构建歌词内容
     */
    private String buildLyricsContent(List<LyricLine> lyrics) {
        StringBuilder lyricsContent = new StringBuilder();
        lyricsContent.append("[ti:").append(currentSong.getTitle()).append("]\n");
        if (currentSong.getArtist() != null && !currentSong.getArtist().isEmpty()) {
            lyricsContent.append("[ar:").append(currentSong.getArtist()).append("]\n");
        }
        if (currentSong.getAlbum() != null && !currentSong.getAlbum().isEmpty()) {
            lyricsContent.append("[al:").append(currentSong.getAlbum()).append("]\n");
        }
        lyricsContent.append("[by:MagicalMusic]\n\n");

        // 添加歌词内容
        for (LyricLine line : lyrics) {
            if (line.getStartTime() > 0) {
                long timeMs = line.getStartTime();
                long minutes = timeMs / (60 * 1000);
                long seconds = (timeMs % (60 * 1000)) / 1000;
                long centiseconds = (timeMs % 1000) / 10;
                
                lyricsContent.append(String.format("[%02d:%02d.%02d]", minutes, seconds, centiseconds));
            }
            lyricsContent.append(line.getContent()).append("\n");
        }

        return lyricsContent.toString();
    }

    /**
     * 解析歌词文本
     */
    private List<LyricLine> parseLyricsText(String lyricsText) {
        List<LyricLine> lyrics = new ArrayList<>();
        String[] lines = lyricsText.split("\n");
        
        for (String line : lines) {
            line = line.trim();
            if (line.isEmpty()) {
                continue;
            }

            // 检查是否包含时间标签 [mm:ss.xx]
            if (line.matches("\\[\\d{2}:\\d{2}\\.\\d{2}\\].*")) {
                try {
                    // 提取时间标签
                    String timeStr = line.substring(1, line.indexOf(']'));
                    String content = line.substring(line.indexOf(']') + 1).trim();
                    
                    if (!content.isEmpty()) {
                        String[] timeParts = timeStr.split(":");
                        String[] secondParts = timeParts[1].split("\\.");
                        
                        int minutes = Integer.parseInt(timeParts[0]);
                        int seconds = Integer.parseInt(secondParts[0]);
                        int centiseconds = Integer.parseInt(secondParts[1]);
                        
                        long timeMs = (minutes * 60 + seconds) * 1000 + centiseconds * 10;
                        lyrics.add(new LyricLine(timeMs, content));
                    }
                } catch (Exception e) {
                    Log.w(TAG, "解析时间标签失败: " + line);
                }
            } else {
                // 没有时间标签的纯文本歌词
                lyrics.add(new LyricLine(0, line));
            }
        }
        
        return lyrics;
    }

    /**
     * 直接写入歌词到音频文件（Android 10及以下版本）
     */
    private boolean writeLyricsToFileDirect(String lyricsContent) {
        if (currentSong == null || currentSong.getPath() == null) {
            return false;
        }

        try {
            File audioFile = new File(currentSong.getPath());
            if (!audioFile.exists()) {
                Log.e(TAG, "音频文件不存在: " + currentSong.getPath());
                return false;
            }

            // 读取音频文件
            org.jaudiotagger.audio.AudioFile file = org.jaudiotagger.audio.AudioFileIO.read(audioFile);
            org.jaudiotagger.tag.Tag tag = file.getTagOrCreateAndSetDefault();

            // 写入歌词到 LYRICS 字段
            tag.setField(FieldKey.LYRICS, lyricsContent);

            // 保存文件
            file.commit();

            Log.d(TAG, "歌词已写入音频文件: " + currentSong.getPath());
            return true;

        } catch (Exception e) {
            Log.e(TAG, "直接写入歌词失败", e);
            return false;
        }
    }

    /**
     * 写入歌词到音频文件（Android 11及以上版本）
     */
    private List<File> writeLyricsToFileR(String lyricsContent) {
        List<File> cacheFiles = new ArrayList<>();

        try {
            File originFile = new File(currentSong.getPath());
            File cacheFile = new File(requireContext().getCacheDir(), originFile.getName());
            cacheFiles.add(cacheFile);

            // 复制原文件到缓存
            try (java.io.InputStream input = new java.io.FileInputStream(originFile);
                 java.io.OutputStream output = new java.io.FileOutputStream(cacheFile)) {
                byte[] buffer = new byte[4096];
                int bytesRead;
                while ((bytesRead = input.read(buffer)) != -1) {
                    output.write(buffer, 0, bytesRead);
                }
            }

            // 读取缓存文件并修改标签
            org.jaudiotagger.audio.AudioFile file = org.jaudiotagger.audio.AudioFileIO.read(cacheFile);
            org.jaudiotagger.tag.Tag tag = file.getTagOrCreateAndSetDefault();

            // 写入歌词到 LYRICS 字段
            tag.setField(FieldKey.LYRICS, lyricsContent);

            // 保存缓存文件
            file.commit();

            Log.d(TAG, "歌词已写入缓存文件: " + cacheFile.getAbsolutePath());

        } catch (Exception e) {
            Log.e(TAG, "写入歌词到缓存文件失败", e);
        }

        return cacheFiles;
    }

    /**
     * 处理缓存文件（Android 11及以上版本）
     */
    private void handleCacheFiles(List<File> cacheFiles) {
        if (currentSong == null || cacheFiles.isEmpty()) {
            ToastUtils.showToast(getContext(), "歌词保存失败");
            return;
        }

        try {
            // 获取原文件路径
            String originalFilePath = currentSong.getPath();
            File originalFile = new File(originalFilePath);

            if (!originalFile.exists()) {
                ToastUtils.showToast(getContext(), "原文件不存在");
                return;
            }

            // 获取缓存文件（应该只有一个）
            File cacheFile = cacheFiles.get(0);
            if (!cacheFile.exists()) {
                ToastUtils.showToast(getContext(), "缓存文件不存在");
                return;
            }

            // 获取文件的MediaStore URI
            android.net.Uri mediaStoreUri = getMediaStoreUri(originalFilePath);
            if (mediaStoreUri == null) {
                ToastUtils.showToast(getContext(), "无法获取文件的MediaStore URI");
                return;
            }

            // 使用MediaStore.createWriteRequest创建写入请求
            android.app.PendingIntent pendingIntent = android.provider.MediaStore.createWriteRequest(
                    requireContext().getContentResolver(),
                    java.util.Collections.singletonList(mediaStoreUri)
            );

            try {
                // 使用新的ActivityResultLauncher启动写入请求
                androidx.activity.result.IntentSenderRequest request =
                        new androidx.activity.result.IntentSenderRequest.Builder(pendingIntent.getIntentSender())
                                .build();
                writeRequestLauncher.launch(request);
            } catch (Exception e) {
                Log.e(TAG, "启动写入请求失败", e);
                ToastUtils.showToast(getContext(), "启动写入请求失败");
            }

            // 打印原始数据到控制台
            System.out.println("已创建MediaStore写入请求:");
            System.out.println("原文件路径: " + originalFilePath);
            System.out.println("缓存文件路径: " + cacheFile.getAbsolutePath());

        } catch (Exception e) {
            Log.e(TAG, "处理缓存文件失败", e);
            ToastUtils.showToast(getContext(), "处理文件失败: " + e.getMessage());
        }
    }

    /**
     * 获取文件的MediaStore URI
     */
    private android.net.Uri getMediaStoreUri(String filePath) {
        try {
            // 查询MediaStore获取文件的URI
            String[] projection = {android.provider.MediaStore.Audio.Media._ID};
            String selection = android.provider.MediaStore.Audio.Media.DATA + "=?";
            String[] selectionArgs = {filePath};

            android.content.ContentResolver contentResolver = requireContext().getContentResolver();
            try (android.database.Cursor cursor = contentResolver.query(
                    android.provider.MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
                    projection,
                    selection,
                    selectionArgs,
                    null)) {

                if (cursor != null && cursor.moveToFirst()) {
                    long id = cursor.getLong(cursor.getColumnIndexOrThrow(android.provider.MediaStore.Audio.Media._ID));
                    return android.content.ContentUris.withAppendedId(
                            android.provider.MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
                            id
                    );
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "获取MediaStore URI失败", e);
        }
        return null;
    }

    /**
     * 预览歌词
     */
    private void previewLyrics() {
        String lyricsText = binding.etLyrics.getText().toString().trim();
        if (TextUtils.isEmpty(lyricsText)) {
            ToastUtils.showToast(getContext(), "没有歌词内容可预览");
            return;
        }

        List<LyricLine> parsedLyrics = parseLyricsText(lyricsText);
        if (parsedLyrics.isEmpty()) {
            ToastUtils.showToast(getContext(), "歌词格式不正确");
            return;
        }

        // 显示预览对话框
        StringBuilder previewText = new StringBuilder();
        for (LyricLine line : parsedLyrics) {
            if (line.getStartTime() > 0) {
                long timeMs = line.getStartTime();
                long minutes = timeMs / (60 * 1000);
                long seconds = (timeMs % (60 * 1000)) / 1000;
                previewText.append(String.format("[%02d:%02d] ", minutes, seconds));
            }
            previewText.append(line.getContent()).append("\n");
        }

        // 使用dialogUtils显示预览对话框
        com.magicalstory.music.dialog.dialogUtils.showAlertDialog(
                requireContext(),
                "歌词预览",
                previewText.toString(),
                "确定",
                null,
                null,
                true,
                new com.magicalstory.music.dialog.dialogUtils.onclick_with_dismiss() {
                    @Override
                    public void click_confirm() {
                        // 用户点击确定
                    }

                    @Override
                    public void click_cancel() {
                        // 不使用取消按钮
                    }

                    @Override
                    public void click_three() {
                        // 不使用第三个按钮
                    }

                    @Override
                    public void dismiss() {
                        // 对话框关闭
                    }
                }
        );
    }

    /**
     * 清空歌词
     */
    private void clearLyrics() {
        binding.etLyrics.setText("");
        currentLyrics.clear();
        ToastUtils.showToast(getContext(), "歌词已清空");
    }

    /**
     * 通知歌词已更新
     */
    private void notifyLyricsUpdated() {
        // 发送本地广播通知FullPlayerFragment刷新歌词
        Intent intent = new Intent("com.magicalstory.music.LYRICS_UPDATED");
        intent.putExtra("song_id", currentSong.getId());
        intent.putExtra("song_path", currentSong.getPath());
        androidx.localbroadcastmanager.content.LocalBroadcastManager.getInstance(requireContext()).sendBroadcast(intent);
        
        Log.d(TAG, "已发送歌词更新广播");
    }

    /**
     * 执行文件替换操作
     */
    private void performFileReplacement() {
        if (currentSong == null) {
            ToastUtils.showToast(getContext(), "歌词保存失败");
            return;
        }

        executorService.execute(() -> {
            try {
                // 获取原文件路径
                String originalFilePath = currentSong.getPath();
                File originalFile = new File(originalFilePath);

                // 获取缓存文件路径 - 使用getCacheDir()
                File cacheDir = requireContext().getCacheDir();
                File[] cacheFiles = cacheDir.listFiles((dir, name) -> name.endsWith(".mp3") || name.endsWith(".flac") || name.endsWith(".m4a"));

                if (cacheFiles == null || cacheFiles.length == 0) {
                    mainHandler.post(() -> ToastUtils.showToast(getContext(), "未找到缓存文件"));
                    return;
                }

                File cacheFile = cacheFiles[0]; // 使用第一个找到的缓存文件

                // 使用ContentResolver进行文件替换
                android.content.ContentResolver contentResolver = requireContext().getContentResolver();

                // 获取原文件的URI
                android.net.Uri originalUri = android.net.Uri.fromFile(originalFile);

                // 打开原文件进行写入
                try (java.io.OutputStream outputStream = contentResolver.openOutputStream(originalUri, "w")) {
                    if (outputStream != null) {
                        // 读取缓存文件并写入原文件
                        try (java.io.FileInputStream inputStream = new java.io.FileInputStream(cacheFile)) {
                            byte[] buffer = new byte[8192];
                            int bytesRead;
                            while ((bytesRead = inputStream.read(buffer)) != -1) {
                                outputStream.write(buffer, 0, bytesRead);
                            }
                        }

                        // 删除所有缓存文件
                        for (File cf : cacheFiles) {
                            try {
                                cf.delete();
                                System.out.println("已删除缓存文件: " + cf.getAbsolutePath());
                            } catch (Exception e) {
                                e.printStackTrace();
                            }
                        }

                        // 通知歌词已更新
                        mainHandler.post(() -> {
                            notifyLyricsUpdated();
                            ToastUtils.showToast(getContext(), "歌词保存成功");
                            Navigation.findNavController(requireView()).popBackStack();
                        });

                        // 打印原始数据到控制台
                        System.out.println("文件替换成功:");
                        System.out.println("原文件: " + originalFilePath);
                        System.out.println("缓存文件: " + cacheFile.getAbsolutePath());

                    } else {
                        mainHandler.post(() -> {
                            ToastUtils.showToast(getContext(), "无法写入原文件");
                        });
                    }
                }

            } catch (Exception e) {
                Log.e(TAG, "文件替换失败", e);
                mainHandler.post(() -> {
                    ToastUtils.showToast(getContext(), "文件替换失败: " + e.getMessage());
                });
            }
        });
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (executorService != null) {
            executorService.shutdown();
        }
        if (mainHandler != null) {
            mainHandler.removeCallbacksAndMessages(null);
        }
    }
} 
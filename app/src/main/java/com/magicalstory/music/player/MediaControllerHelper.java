package com.magicalstory.music.player;

import android.content.Context;
import android.media.session.PlaybackState;
import android.os.Bundle;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.media3.common.MediaItem;
import androidx.media3.common.Player;
import androidx.media3.common.PlaybackException;
import androidx.media3.common.util.UnstableApi;
import androidx.media3.session.MediaController;
import androidx.media3.session.SessionCommand;

import com.hjq.gson.factory.GsonFactory;
import com.magicalstory.music.R;
import com.magicalstory.music.model.Song;
import com.magicalstory.music.utils.app.ToastUtils;
import com.tencent.mmkv.MMKV;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * MediaController辅助类（单例模式）
 * 统一管理播放控制和播放状态监听
 */
@UnstableApi
public class MediaControllerHelper implements Player.Listener {

    private static final String TAG = "MediaControllerHelper";

    // 单例实例
    private static volatile MediaControllerHelper instance;
    private static final Object lock = new Object();

    // 自定义命令
    private static final String CUSTOM_COMMAND_TOGGLE_SHUFFLE = "TOGGLE_SHUFFLE";
    private static final String CUSTOM_COMMAND_TOGGLE_REPEAT = "TOGGLE_REPEAT";

    // 懒加载窗口大小配置
    private static final int WINDOW_SIZE = 3; // 当前播放歌曲 + 前后各1首
    private static final int PRELOAD_THRESHOLD = 1; // 当播放位置距离边界还有1首时开始预加载

    private MediaController mediaController;
    private final PlaylistManager playlistManager;

    // 播放状态监听器列表
    private final List<PlaybackStateListener> playbackStateListeners = new CopyOnWriteArrayList<>();

    // 懒加载相关字段
    private ArrayList<Song> fullPlaylist = new ArrayList<>(); // 完整播放列表
    private int currentPlaylistIndex = 0; // 当前播放在完整列表中的索引
    private int windowStart = 0; // 当前窗口在完整列表中的起始位置
    private int windowEnd = 0; // 当前窗口在完整列表中的结束位置
    private boolean isWindowLoading = false; // 是否正在加载窗口
    private Context context;

    /**
     * 获取单例实例（双重检查锁定）
     */
    public static MediaControllerHelper getInstance(@NonNull MediaController mediaController, Context context) {
        if (instance == null) {
            synchronized (lock) {
                if (instance == null) {
                    instance = new MediaControllerHelper(mediaController, context);
                }
            }
        }
        return instance;
    }

    public static MediaControllerHelper getInstance() {
        return instance;
    }

    /**
     * 重新初始化MediaController
     */
    public void reinitialize(@NonNull MediaController newMediaController) {
        // 清理旧的MediaController
        cleanupMediaController();

        this.mediaController = newMediaController;
        mediaController.addListener(this);

        Log.d(TAG, "重新初始化MediaController完成");
    }

    //保存播放列表到本地
    public void savePlayListJsonToLocal() {
        Song currentSong = getCurrentSong();
        String songTitle = currentSong != null ? currentSong.getTitle() : "未知歌曲";
        System.out.println("保存到本地：" + getCurrentIndex() + " " + songTitle);
        playlistManager.savePlayList(fullPlaylist, getCurrentIndex());
    }

    /**
     * 清理单例实例
     */
    public static void releaseInstance() {
        if (instance != null) {
            instance.cleanupInternal();
            instance = null;
        }
    }

    /**
     * 添加歌曲到播放列表的下一首位置
     * @param songs 要添加的歌曲列表
     */
    public void addSongsToPlayNext(@NonNull List<Song> songs) {
        if (songs.isEmpty()) {
            Log.w(TAG, "添加歌曲到下一首播放失败：歌曲列表为空");
            return;
        }

        try {
            Log.d(TAG, "添加歌曲到下一首播放，歌曲数量: " + songs.size());
            
            // 获取当前播放索引
            int currentIndex = getCurrentIndex();
            
            // 将新歌曲插入到当前播放歌曲的后面
            int insertIndex = currentIndex + 1;
            fullPlaylist.addAll(insertIndex, songs);
            
            // 更新当前播放索引（如果插入位置在当前播放位置之前，需要调整索引）
            if (insertIndex <= currentPlaylistIndex) {
                currentPlaylistIndex += songs.size();
            }
            
            // 更新窗口范围
            updateWindowAfterInsertion(insertIndex, songs.size());
            
            // 刷新播放列表
            refreshPlaylistAfterInsertion(insertIndex, songs.size());
            
            // 保存播放列表到本地
            savePlayListJsonToLocal();
            
            Log.d(TAG, "成功添加歌曲到下一首播放，当前播放索引: " + currentPlaylistIndex);
            
        } catch (Exception e) {
            Log.e(TAG, "添加歌曲到下一首播放时发生错误", e);
        }
    }

    /**
     * 添加歌曲到播放列表末尾
     * @param songs 要添加的歌曲列表
     */
    public void addSongsToPlaylist(@NonNull List<Song> songs) {
        if (songs == null || songs.isEmpty()) {
            Log.w(TAG, "添加歌曲到播放列表失败：歌曲列表为空");
            return;
        }

        try {
            Log.d(TAG, "添加歌曲到播放列表末尾，歌曲数量: " + songs.size());
            
            // 获取当前播放列表大小
            int originalSize = fullPlaylist.size();
            
            // 将新歌曲添加到播放列表末尾
            fullPlaylist.addAll(songs);
            
            // 更新窗口范围
            updateWindowAfterInsertion(originalSize, songs.size());
            
            // 刷新播放列表
            refreshPlaylistAfterInsertion(originalSize, songs.size());
            
            // 保存播放列表到本地
            savePlayListJsonToLocal();
            
            Log.d(TAG, "成功添加歌曲到播放列表末尾，播放列表大小: " + fullPlaylist.size());
            
        } catch (Exception e) {
            Log.e(TAG, "添加歌曲到播放列表时发生错误", e);
        }
    }

    /**
     * 插入歌曲后更新窗口范围
     */
    private void updateWindowAfterInsertion(int insertIndex, int insertCount) {
        // 如果插入位置在窗口范围内，需要调整窗口
        if (insertIndex <= windowEnd) {
            windowEnd += insertCount;
        }
        
        Log.d(TAG, "插入歌曲后更新窗口范围: [" + windowStart + ", " + windowEnd + "]");
    }

    /**
     * 插入歌曲后刷新播放列表
     */
    private void refreshPlaylistAfterInsertion(int insertIndex, int insertCount) {
        try {
            Log.d(TAG, "开始刷新播放列表（插入歌曲后），插入位置: " + insertIndex + ", 插入数量: " + insertCount);

            // 获取当前播放位置和媒体数量
            int currentMediaIndex = mediaController.getCurrentMediaItemIndex();
            int mediaItemCount = mediaController.getMediaItemCount();

            Log.d(TAG, "当前MediaIndex: " + currentMediaIndex + ", 媒体数量: " + mediaItemCount);

            // 移除除了当前播放媒体外的所有媒体
            while (mediaController.getMediaItemCount() > 1) {
                if (mediaController.getCurrentMediaItemIndex() == 0) {
                    // 如果当前播放的是第一个，移除最后一个
                    mediaController.removeMediaItem(mediaController.getMediaItemCount() - 1);
                } else {
                    // 否则移除第一个
                    mediaController.removeMediaItem(0);
                }
            }
            Log.d(TAG, "移除其他媒体后，剩余媒体数量: " + mediaController.getMediaItemCount());

            // 计算需要添加的前后歌曲索引
            int prevIndex = currentPlaylistIndex - 1;
            int nextIndex = currentPlaylistIndex + 1;

            // 添加前一首歌（如果存在）
            if (prevIndex >= 0 && prevIndex < fullPlaylist.size()) {
                Song prevSong = fullPlaylist.get(prevIndex);
                MediaItem prevMediaItem = playlistManager.createMediaItem(prevSong);
                if (prevMediaItem != null) {
                    mediaController.addMediaItem(0, prevMediaItem);
                    Log.d(TAG, "添加前一首歌: " + prevSong.getTitle() + " (索引: " + prevIndex + ")");
                }
            }

            // 添加后一首歌（如果存在）
            if (nextIndex >= 0 && nextIndex < fullPlaylist.size()) {
                Song nextSong = fullPlaylist.get(nextIndex);
                MediaItem nextMediaItem = playlistManager.createMediaItem(nextSong);
                if (nextMediaItem != null) {
                    mediaController.addMediaItem(nextMediaItem);
                    Log.d(TAG, "添加后一首歌: " + nextSong.getTitle() + " (索引: " + nextIndex + ")");
                }
            }

            // 刷新窗口范围
            windowStart = Math.max(0, currentPlaylistIndex - 1);
            windowEnd = Math.min(fullPlaylist.size() - 1, currentPlaylistIndex + 1);

            Log.d(TAG, "更新窗口范围: [" + windowStart + ", " + windowEnd + "]");
            Log.d(TAG, "最终媒体数量: " + mediaController.getMediaItemCount());

        } catch (Exception e) {
            Log.e(TAG, "插入歌曲后刷新播放列表时发生错误", e);
        }
    }

    /**
     * 私有构造函数
     */
    private MediaControllerHelper(@NonNull MediaController mediaController, Context context) {
        this.mediaController = mediaController;
        this.playlistManager = PlaylistManager.getInstance();
        this.context = context;
        // 添加播放监听器
        mediaController.addListener(this);

        Log.d(TAG, "MediaControllerHelper单例初始化完成，开始监听播放状态");
    }

    /**
     * 播放状态监听器接口
     */
    public interface PlaybackStateListener {
        /**
         * 播放状态改变
         */
        default void onPlaybackStateChanged(int playbackState) {
        }

        /**
         * 播放/暂停状态改变
         */
        default void onIsPlayingChanged(boolean isPlaying) {
        }

        /**
         * 媒体项切换
         */
        default void onMediaItemTransition(@Nullable MediaItem mediaItem, int reason) {
        }

        /**
         * 播放错误
         */
        default void onPlayerError(PlaybackException error) {
        }

        /**
         * 重复模式改变
         */
        default void onRepeatModeChanged(int repeatMode) {
        }

        /**
         * 随机模式改变
         */
        default void onShuffleModeEnabledChanged(boolean shuffleModeEnabled) {
        }

        /**
         * 播放进度更新（需要定时器触发）
         */
        default void onPositionChanged(long position, long duration) {
        }

        /**
         * 播放歌曲切换
         */
        default void songChange(Song newSong) {

        }

        default void stopPlay() {

        }

        default void progressInit(long dur, long progress) {

        }

        /**
         * 播放列表为空时的回调
         */
        default void onPlaylistEmpty() {

        }

    }

    /**
     * 添加播放状态监听器
     */
    public void addPlaybackStateListener(PlaybackStateListener listener) {
        if (listener != null && !playbackStateListeners.contains(listener)) {
            playbackStateListeners.add(listener);
            Log.d(TAG, "添加播放状态监听器: " + listener.getClass().getSimpleName());
        }
    }

    /**
     * 移除播放状态监听器
     */
    public void removePlaybackStateListener(PlaybackStateListener listener) {
        if (listener != null) {
            playbackStateListeners.remove(listener);
            Log.d(TAG, "移除播放状态监听器: " + listener.getClass().getSimpleName());
        }
    }

    /**
     * 清除所有播放状态监听器
     */
    public void clearPlaybackStateListeners() {
        playbackStateListeners.clear();
        Log.d(TAG, "清除所有播放状态监听器");
    }

    @Override
    public void onPlaybackStateChanged(int playbackState) {
        Log.d(TAG, "播放状态改变: " + playbackState);

        playlistManager.savePlayStatus(playbackState != PlaybackState.STATE_STOPPED);

        // 通知所有监听器
        for (PlaybackStateListener listener : playbackStateListeners) {
            try {
                listener.onPlaybackStateChanged(playbackState);
            } catch (Exception e) {
                Log.e(TAG, "播放状态监听器错误", e);
            }
        }
    }

    @Override
    public void onIsPlayingChanged(boolean isPlaying) {
        Log.d(TAG, "播放状态改变: " + isPlaying);
        playlistManager.savePlayStatus(isPlaying);
        // 通知所有监听器
        for (PlaybackStateListener listener : playbackStateListeners) {
            try {
                listener.onIsPlayingChanged(isPlaying);
            } catch (Exception e) {
                Log.e(TAG, "播放状态监听器错误", e);
            }
        }
    }

    @Override
    public void onMediaItemTransition(@Nullable MediaItem mediaItem, int reason) {
        Log.d(TAG, "媒体项切换: " + (mediaItem != null ? mediaItem.mediaId : "null"));

        // 更新当前播放索引
        updateCurrentPlaylistIndex();

        // 检查是否需要扩展播放窗口
        checkAndExpandWindow();


        // 通知所有监听器
        for (PlaybackStateListener listener : playbackStateListeners) {
            try {
                listener.onMediaItemTransition(mediaItem, reason);
                listener.songChange(getCurrentSong());
            } catch (Exception e) {
                Log.e(TAG, "媒体项切换监听器错误", e);
            }
        }
        savePlayListJsonToLocal();
    }

    @Override
    public void onPlayerError(PlaybackException error) {
        Log.e(TAG, "播放错误: " + error.getMessage(), error);

        // 通知所有监听器
        for (PlaybackStateListener listener : playbackStateListeners) {
            try {
                listener.onPlayerError(error);
            } catch (Exception e) {
                Log.e(TAG, "播放错误监听器错误", e);
            }
        }
    }

    @Override
    public void onRepeatModeChanged(int repeatMode) {
        Log.d(TAG, "重复模式改变: " + repeatMode);

        // 通知所有监听器
        for (PlaybackStateListener listener : playbackStateListeners) {
            try {
                listener.onRepeatModeChanged(repeatMode);
            } catch (Exception e) {
                Log.e(TAG, "重复模式监听器错误", e);
            }
        }
    }

    @Override
    public void onShuffleModeEnabledChanged(boolean shuffleModeEnabled) {
        Log.d(TAG, "随机模式改变: " + shuffleModeEnabled);

        // 通知所有监听器
        for (PlaybackStateListener listener : playbackStateListeners) {
            try {
                listener.onShuffleModeEnabledChanged(shuffleModeEnabled);
            } catch (Exception e) {
                Log.e(TAG, "随机模式监听器错误", e);
            }
        }
    }

    /**
     * 设置播放列表（懒加载优化版本）
     */
    public void setPlaylist(@NonNull List<Song> songs) {
        setPlaylist(songs, 0);
    }

    /**
     * 设置播放列表并指定起始索引（懒加载优化版本）
     */
    public void setPlaylist(@NonNull List<Song> songs, int startIndex) {
        long startTime = System.currentTimeMillis();
        Log.d(TAG, "设置播放列表（懒加载），歌曲数量: " + songs.size() + ", 起始索引: " + startIndex);

        try {
            if (songs.isEmpty()) {
                Log.w(TAG, "播放列表为空");
                return;
            }

            // 确保起始索引在有效范围内
            if (startIndex < 0 || startIndex >= songs.size()) {
                Log.w(TAG, "起始索引超出范围: " + startIndex + ", 使用默认值0");
                startIndex = 0;
            }

            // 保存完整播放列表
            fullPlaylist = new ArrayList<>(songs);
            currentPlaylistIndex = startIndex;

            Log.d(TAG, "完整播放列表大小: " + fullPlaylist.size() + ", 当前索引: " + currentPlaylistIndex);

            // 加载初始窗口
            loadInitialWindow(startIndex);

            long totalTime = System.currentTimeMillis() - startTime;
            Log.d(TAG, "懒加载播放列表设置完成，总耗时: " + totalTime + "ms");

        } catch (Exception e) {
            long totalTime = System.currentTimeMillis() - startTime;
            Log.e(TAG, "设置播放列表时发生错误，耗时: " + totalTime + "ms", e);
        }

        savePlayListJsonToLocal();
    }

    /**
     * 加载初始播放窗口
     */
    private void loadInitialWindow(int startIndex) {
        long startTime = System.currentTimeMillis();
        Log.d(TAG, "加载初始窗口，起始索引: " + startIndex);

        try {
            // 计算窗口范围
            windowStart = Math.max(0, startIndex - 1);
            windowEnd = Math.min(fullPlaylist.size() - 1, startIndex + 1);

            Log.d(TAG, "窗口范围: [" + windowStart + ", " + windowEnd + "]");

            // 创建窗口内的MediaItem列表
            List<MediaItem> windowMediaItems = new ArrayList<>();
            for (int i = windowStart; i <= windowEnd; i++) {
                Song song = fullPlaylist.get(i);
                MediaItem mediaItem = playlistManager.createMediaItem(song);
                if (mediaItem != null) {
                    windowMediaItems.add(mediaItem);
                    Log.d(TAG, "添加MediaItem到窗口: " + song.getTitle() + " (索引: " + i + ")");
                } else {
                    Log.w(TAG, "创建MediaItem失败: " + song.getTitle());
                }
            }

            if (!windowMediaItems.isEmpty()) {
                // 计算在窗口中的起始位置
                int windowStartIndex = startIndex - windowStart;

                Log.d(TAG, "设置MediaItems，窗口大小: " + windowMediaItems.size() + ", 窗口起始索引: " + windowStartIndex);

                // 设置媒体项列表到播放器
                mediaController.setMediaItems(windowMediaItems, windowStartIndex, 0);

                // 准备播放器
                mediaController.prepare();

                Log.d(TAG, "初始窗口加载完成，窗口范围: [" + windowStart + ", " + windowEnd + "]");
            } else {
                Log.w(TAG, "没有有效的MediaItem创建");
            }

            long totalTime = System.currentTimeMillis() - startTime;
            Log.d(TAG, "初始窗口加载耗时: " + totalTime + "ms");

        } catch (Exception e) {
            long totalTime = System.currentTimeMillis() - startTime;
            Log.e(TAG, "加载初始窗口时发生错误，耗时: " + totalTime + "ms", e);
        }
    }

    /**
     * 更新当前播放索引
     */
    private void updateCurrentPlaylistIndex() {
        try {
            int currentMediaIndex = mediaController.getCurrentMediaItemIndex();
            if (currentMediaIndex >= 0) {
                // 根据窗口位置计算在完整列表中的索引
                currentPlaylistIndex = windowStart + currentMediaIndex;
                Log.d(TAG, "更新当前播放索引: " + currentPlaylistIndex + " (MediaController索引: " + currentMediaIndex + ")");
            }
        } catch (Exception e) {
            Log.e(TAG, "更新当前播放索引时发生错误", e);
        }
    }

    /**
     * 检查并扩展播放窗口
     */
    private void checkAndExpandWindow() {
        if (isWindowLoading) {
            Log.d(TAG, "窗口正在加载中，跳过扩展检查");
            return;
        }

        try {
            int currentMediaIndex = mediaController.getCurrentMediaItemIndex();
            int mediaItemCount = mediaController.getMediaItemCount();

            Log.d(TAG, "检查窗口扩展，当前MediaIndex: " + currentMediaIndex + ", 总数: " + mediaItemCount);

            // 检查是否需要向前扩展
            if (currentMediaIndex <= PRELOAD_THRESHOLD && windowStart > 0) {
                Log.d(TAG, "需要向前扩展窗口");
                expandWindow(false); // 向前扩展
            }

            // 检查是否需要向后扩展
            if (currentMediaIndex >= mediaItemCount - PRELOAD_THRESHOLD - 1 && windowEnd < fullPlaylist.size() - 1) {
                Log.d(TAG, "需要向后扩展窗口");
                expandWindow(true); // 向后扩展
            }

        } catch (Exception e) {
            Log.e(TAG, "检查窗口扩展时发生错误", e);
        }
    }

    /**
     * 扩展播放窗口
     */
    private void expandWindow(boolean expandNext) {
        if (isWindowLoading) {
            Log.d(TAG, "窗口正在加载中，跳过扩展");
            return;
        }

        isWindowLoading = true;
        long startTime = System.currentTimeMillis();

        try {
            if (expandNext) {
                // 向后扩展
                int newEnd = Math.min(fullPlaylist.size() - 1, windowEnd + 1);
                if (newEnd > windowEnd) {
                    Song song = fullPlaylist.get(newEnd);
                    MediaItem mediaItem = playlistManager.createMediaItem(song);
                    if (mediaItem != null) {
                        // 添加到列表末尾
                        mediaController.addMediaItem(mediaItem);
                        windowEnd = newEnd;
                        Log.d(TAG, "向后扩展窗口，添加歌曲: " + song.getTitle() + " (索引: " + newEnd + ")");
                    }
                }
            } else {
                // 向前扩展
                int newStart = Math.max(0, windowStart - 1);
                if (newStart < windowStart) {
                    Song song = fullPlaylist.get(newStart);
                    MediaItem mediaItem = playlistManager.createMediaItem(song);
                    if (mediaItem != null) {
                        // 添加到列表开头
                        mediaController.addMediaItem(0, mediaItem);
                        windowStart = newStart;
                        Log.d(TAG, "向前扩展窗口，添加歌曲: " + song.getTitle() + " (索引: " + newStart + ")");
                    }
                }
            }

            Log.d(TAG, "窗口扩展完成，当前窗口范围: [" + windowStart + ", " + windowEnd + "]");

        } catch (Exception e) {
            Log.e(TAG, "扩展窗口时发生错误", e);
        } finally {
            isWindowLoading = false;
            long totalTime = System.currentTimeMillis() - startTime;
            Log.d(TAG, "窗口扩展耗时: " + totalTime + "ms");
        }
    }

    /**
     * 播放指定索引的歌曲（适配懒加载）
     */
    public void playAtIndex(int index) {
        try {
            if (index < 0 || index >= fullPlaylist.size()) {
                Log.w(TAG, "播放索引超出范围: " + index + ", 播放列表大小: " + fullPlaylist.size());
                return;
            }

            Log.d(TAG, "播放指定索引的歌曲: " + index);

            // 检查目标索引是否在当前窗口内
            if (index >= windowStart && index <= windowEnd) {
                // 在当前窗口内，直接播放
                int mediaIndex = index - windowStart;
                mediaController.seekTo(mediaIndex, 0);
                mediaController.play();
                Log.d(TAG, "在当前窗口内播放，MediaIndex: " + mediaIndex);
            } else {
                // 不在当前窗口内，重新加载窗口
                Log.d(TAG, "目标索引不在当前窗口内，重新加载窗口");
                loadInitialWindow(index);
            }

        } catch (Exception e) {
            Log.e(TAG, "播放指定索引歌曲时发生错误: " + index, e);
        }
    }

    /**
     * 切换随机播放模式
     */
    public void toggleShuffle() {
        try {
            SessionCommand command = new SessionCommand(CUSTOM_COMMAND_TOGGLE_SHUFFLE, Bundle.EMPTY);
            mediaController.sendCustomCommand(command, Bundle.EMPTY);

            Log.d(TAG, "Toggle shuffle command sent");
        } catch (Exception e) {
            Log.e(TAG, "Error toggling shuffle", e);
        }
    }

    /**
     * 切换重复播放模式
     */
    public void toggleRepeat() {
        try {
            SessionCommand command = new SessionCommand(CUSTOM_COMMAND_TOGGLE_REPEAT, Bundle.EMPTY);
            mediaController.sendCustomCommand(command, Bundle.EMPTY);

            Log.d(TAG, "Toggle repeat command sent");
        } catch (Exception e) {
            Log.e(TAG, "Error toggling repeat", e);
        }
    }

    /**
     * 设置重复播放模式
     */
    public void setRepeatMode(@Player.RepeatMode int repeatMode) {
        try {
            mediaController.setRepeatMode(repeatMode);

            Log.d(TAG, "Repeat mode set to: " + repeatMode);
        } catch (Exception e) {
            Log.e(TAG, "Error setting repeat mode", e);
        }
    }

    /**
     * 设置随机播放模式
     */
    public void setShuffleModeEnabled(boolean enabled) {
        try {
            mediaController.setShuffleModeEnabled(enabled);

            Log.d(TAG, "Shuffle mode set to: " + enabled);
        } catch (Exception e) {
            Log.e(TAG, "Error setting shuffle mode", e);
        }
    }

    /**
     * 清除播放列表
     */
    public void clearPlaylist() {
        try {
            mediaController.clearMediaItems();
            playlistManager.clearPlaylist();
            fullPlaylist.clear();
            currentPlaylistIndex = 0;
            windowStart = 0;
            windowEnd = 0;

            Log.d(TAG, "播放列表已清除");
        } catch (Exception e) {
            Log.e(TAG, "清除播放列表时发生错误", e);
        }
    }

    /**
     * 移除指定索引的歌曲
     */
    public void removeFromPlaylist(int index) {
        try {
            if (index >= 0 && index < fullPlaylist.size()) {
                Log.d(TAG, "删除播放列表中的歌曲，索引: " + index);
                
                // 记录被删除的歌曲
                Song removedSong = fullPlaylist.get(index);
                
                // 从完整播放列表中移除
                fullPlaylist.remove(index);
                
                // 更新当前播放索引
                boolean wasCurrentSongDeleted = (index == currentPlaylistIndex);
                
                if (wasCurrentSongDeleted) {
                    // 当前播放歌曲被删除
                    Log.d(TAG, "当前播放歌曲被删除");
                    
                    // 如果播放列表为空，停止播放
                    if (fullPlaylist.isEmpty()) {
                        Log.d(TAG, "播放列表为空，停止播放");
                        mediaController.stop();
                        currentPlaylistIndex = -1;
                        windowStart = 0;
                        windowEnd = 0;
                        return;
                    }
                    
                    // 如果删除的是最后一首，播放前一首
                    if (index >= fullPlaylist.size()) {
                        currentPlaylistIndex = fullPlaylist.size() - 1;
                    }
                    // 否则播放下一首（如果存在）
                    else {
                        currentPlaylistIndex = index;
                    }
                    
                    // 重新加载播放窗口
                    loadInitialWindow(currentPlaylistIndex);
                    
                } else {
                    // 其他歌曲被删除，更新索引
                    if (currentPlaylistIndex > index) {
                        currentPlaylistIndex--;
                    }
                    
                    // 重新构建播放窗口，参考reorderPlaylist的处理方式
                    updatePlaylistAfterRemoval(index);
                }
                
                // 保存播放列表到本地
                savePlayListJsonToLocal();
                
                Log.d(TAG, "歌曲删除完成，当前播放索引: " + currentPlaylistIndex + ", 播放列表大小: " + fullPlaylist.size());
                
            } else {
                Log.w(TAG, "Invalid index for removal: " + index);
            }
        } catch (Exception e) {
            Log.e(TAG, "Error removing song at index: " + index, e);
        }
    }

    /**
     * 重新排序播放列表（新方法，避免触发重新播放）
     * 使用replaceMediaItem来替换当前歌曲的上下两首歌，而不是重新加载整个窗口
     */
    public void reorderPlaylist(int fromIndex, int toIndex) {
        try {
            if (fromIndex >= 0 && fromIndex < fullPlaylist.size() &&
                    toIndex >= 0 && toIndex < fullPlaylist.size()) {

                Log.d(TAG, "重新排序播放列表: " + fromIndex + " -> " + toIndex);
                System.out.println("fromPosition = " + fromIndex);
                System.out.println("toPosition = " + toIndex);

                // 同步更新fullPlaylist
                Collections.swap(fullPlaylist, fromIndex, toIndex);

                // 更新当前播放索引
                if (currentPlaylistIndex == fromIndex) {
                    currentPlaylistIndex = toIndex;
                } else if (currentPlaylistIndex > fromIndex && currentPlaylistIndex <= toIndex) {
                    currentPlaylistIndex--;
                } else if (currentPlaylistIndex < fromIndex && currentPlaylistIndex >= toIndex) {
                    currentPlaylistIndex++;
                }
                System.out.println("最新的播放位置 = " + currentPlaylistIndex);
                System.out.println("mediaController.getMediaItemCount() = " + mediaController.getMediaItemCount());
                System.out.println("mediaController.getCurrentMediaItemIndex() = " + mediaController.getCurrentMediaItemIndex());

                updatePlaylistWithReplaceMediaItem(fromIndex, toIndex);

                // 保存播放列表到本地
                savePlayListJsonToLocal();

                Log.d(TAG, "播放列表重新排序完成: " + fromIndex + " -> " + toIndex);
            } else {
                Log.w(TAG, "Invalid indices for reorder: " + fromIndex + " -> " + toIndex);
            }
        } catch (Exception e) {
            Log.e(TAG, "Error reordering playlist", e);
        }
    }

    /**
     * 使用replaceMediaItem更新播放列表，避免重新加载整个窗口
     */
    private void updatePlaylistWithReplaceMediaItem(int fromIndex, int toIndex) {
        try {
            Log.d(TAG, "开始使用replaceMediaItem更新播放列表: " + fromIndex + " -> " + toIndex);

            // 获取当前播放位置和媒体数量
            int currentMediaIndex = mediaController.getCurrentMediaItemIndex();
            int mediaItemCount = mediaController.getMediaItemCount();

            Log.d(TAG, "当前MediaIndex: " + currentMediaIndex + ", 媒体数量: " + mediaItemCount);

            // 移除除了当前播放媒体外的所有媒体
            while (mediaController.getMediaItemCount() > 1) {
                if (mediaController.getCurrentMediaItemIndex() == 0) {
                    // 如果当前播放的是第一个，移除最后一个
                    mediaController.removeMediaItem(mediaController.getMediaItemCount() - 1);
                } else {
                    // 否则移除第一个
                    mediaController.removeMediaItem(0);
                }
            }
            Log.d(TAG, "移除其他媒体后，剩余媒体数量: " + mediaController.getMediaItemCount());

            // 计算需要添加的前后歌曲索引
            int prevIndex = currentPlaylistIndex - 1;
            int nextIndex = currentPlaylistIndex + 1;

            // 添加前一首歌（如果存在）
            if (prevIndex >= 0 && prevIndex < fullPlaylist.size()) {
                Song prevSong = fullPlaylist.get(prevIndex);
                MediaItem prevMediaItem = playlistManager.createMediaItem(prevSong);
                if (prevMediaItem != null) {
                    mediaController.addMediaItem(0, prevMediaItem);
                    Log.d(TAG, "添加前一首歌: " + prevSong.getTitle() + " (索引: " + prevIndex + ")");
                }
            }

            // 添加后一首歌（如果存在）
            if (nextIndex >= 0 && nextIndex < fullPlaylist.size()) {
                Song nextSong = fullPlaylist.get(nextIndex);
                MediaItem nextMediaItem = playlistManager.createMediaItem(nextSong);
                if (nextMediaItem != null) {
                    mediaController.addMediaItem(nextMediaItem);
                    Log.d(TAG, "添加后一首歌: " + nextSong.getTitle() + " (索引: " + nextIndex + ")");
                }
            }

            // 刷新窗口范围
            windowStart = Math.max(0, currentPlaylistIndex - 1);
            windowEnd = Math.min(fullPlaylist.size() - 1, currentPlaylistIndex + 1);

            Log.d(TAG, "更新窗口范围: [" + windowStart + ", " + windowEnd + "]");
            Log.d(TAG, "最终媒体数量: " + mediaController.getMediaItemCount());

        } catch (Exception e) {
            Log.e(TAG, "使用replaceMediaItem更新播放列表时发生错误", e);
        }
    }

    /**
     * 删除歌曲后更新播放列表，参考updatePlaylistWithReplaceMediaItem的处理方式
     */
    private void updatePlaylistAfterRemoval(int removedIndex) {
        try {
            Log.d(TAG, "开始更新播放列表（删除歌曲后），删除索引: " + removedIndex);

            // 如果removedIndex为-1，表示是外部删除（如从设备删除），不需要特殊处理
            if (removedIndex == -1) {
                Log.d(TAG, "外部删除，直接更新播放列表");
            }

            // 获取当前播放位置和媒体数量
            int currentMediaIndex = mediaController.getCurrentMediaItemIndex();
            int mediaItemCount = mediaController.getMediaItemCount();

            Log.d(TAG, "当前MediaIndex: " + currentMediaIndex + ", 媒体数量: " + mediaItemCount);

            // 移除除了当前播放媒体外的所有媒体
            while (mediaController.getMediaItemCount() > 1) {
                if (mediaController.getCurrentMediaItemIndex() == 0) {
                    // 如果当前播放的是第一个，移除最后一个
                    mediaController.removeMediaItem(mediaController.getMediaItemCount() - 1);
                } else {
                    // 否则移除第一个
                    mediaController.removeMediaItem(0);
                }
            }
            Log.d(TAG, "移除其他媒体后，剩余媒体数量: " + mediaController.getMediaItemCount());

            // 计算需要添加的前后歌曲索引
            int prevIndex = currentPlaylistIndex - 1;
            int nextIndex = currentPlaylistIndex + 1;

            // 添加前一首歌（如果存在）
            if (prevIndex >= 0 && prevIndex < fullPlaylist.size()) {
                Song prevSong = fullPlaylist.get(prevIndex);
                MediaItem prevMediaItem = playlistManager.createMediaItem(prevSong);
                if (prevMediaItem != null) {
                    mediaController.addMediaItem(0, prevMediaItem);
                    Log.d(TAG, "添加前一首歌: " + prevSong.getTitle() + " (索引: " + prevIndex + ")");
                }
            }

            // 添加后一首歌（如果存在）
            if (nextIndex >= 0 && nextIndex < fullPlaylist.size()) {
                Song nextSong = fullPlaylist.get(nextIndex);
                MediaItem nextMediaItem = playlistManager.createMediaItem(nextSong);
                if (nextMediaItem != null) {
                    mediaController.addMediaItem(nextMediaItem);
                    Log.d(TAG, "添加后一首歌: " + nextSong.getTitle() + " (索引: " + nextIndex + ")");
                }
            }

            // 刷新窗口范围
            windowStart = Math.max(0, currentPlaylistIndex - 1);
            windowEnd = Math.min(fullPlaylist.size() - 1, currentPlaylistIndex + 1);

            Log.d(TAG, "更新窗口范围: [" + windowStart + ", " + windowEnd + "]");
            Log.d(TAG, "最终媒体数量: " + mediaController.getMediaItemCount());

        } catch (Exception e) {
            Log.e(TAG, "删除歌曲后更新播放列表时发生错误", e);
        }
    }

    /**
     * 从设备删除歌曲后刷新播放列表
     * 检查并移除已删除的歌曲，更新播放列表
     * @param deletedSongIds 已删除歌曲的ID列表
     */
    public void refreshPlaylistAfterDeviceDeletion(List<Long> deletedSongIds) {
        if (deletedSongIds == null || deletedSongIds.isEmpty()) {
            Log.d(TAG, "没有需要处理的删除歌曲");
            return;
        }

        try {
            Log.d(TAG, "开始处理设备删除歌曲后的播放列表刷新，删除歌曲数量: " + deletedSongIds.size());
            Log.d(TAG, "当前播放列表大小: " + fullPlaylist.size() + ", 当前播放索引: " + currentPlaylistIndex);

            // 获取当前播放歌曲
            Song currentSong = getCurrentSong();
            boolean currentSongDeleted = false;
            
            Log.d(TAG, "当前播放歌曲: " + (currentSong != null ? currentSong.getTitle() : "null"));
            
            // 检查当前播放歌曲是否在删除列表中
            if (currentSong != null && deletedSongIds.contains(currentSong.getId())) {
                currentSongDeleted = true;
                Log.d(TAG, "当前播放歌曲已被删除: " + currentSong.getTitle());
            } else if (currentSong == null) {
                Log.d(TAG, "当前播放歌曲为null，尝试通过索引检查");
                // 如果当前播放歌曲为null，但当前索引有效，检查该索引的歌曲是否被删除
                if (currentPlaylistIndex >= 0 && currentPlaylistIndex < fullPlaylist.size()) {
                    Song songAtIndex = fullPlaylist.get(currentPlaylistIndex);
                    Log.d(TAG, "通过索引获取的歌曲: " + songAtIndex.getTitle() + " (ID: " + songAtIndex.getId() + ")");
                    if (deletedSongIds.contains(songAtIndex.getId())) {
                        currentSongDeleted = true;
                        Log.d(TAG, "通过索引检查发现当前播放歌曲已被删除: " + songAtIndex.getTitle());
                    } else {
                        Log.d(TAG, "通过索引检查发现当前播放歌曲未被删除: " + songAtIndex.getTitle());
                    }
                } else {
                    Log.d(TAG, "当前播放索引无效: " + currentPlaylistIndex + ", 播放列表大小: " + fullPlaylist.size());
                }
            } else {
                Log.d(TAG, "当前播放歌曲未被删除: " + currentSong.getTitle());
            }

            // 从完整播放列表中移除已删除的歌曲
            List<Song> songsToRemove = new ArrayList<>();
            for (Song song : fullPlaylist) {
                if (deletedSongIds.contains(song.getId())) {
                    songsToRemove.add(song);
                    Log.d(TAG, "找到要删除的歌曲: " + song.getTitle() + " (ID: " + song.getId() + ")");
                }
            }
            
            Log.d(TAG, "要删除的歌曲数量: " + songsToRemove.size());

            if (!songsToRemove.isEmpty()) {
                // 移除被删除的歌曲
                fullPlaylist.removeAll(songsToRemove);
                Log.d(TAG, "从完整播放列表中移除 " + songsToRemove.size() + " 首已删除的歌曲");

                // 如果当前播放歌曲被删除，参考removeFromPlaylist的处理逻辑
                if (currentSongDeleted) {
                    Log.d(TAG, "当前播放歌曲被删除，停止播放");
                    
                    // 停止播放
                    mediaController.pause();
                    
                    // 如果播放列表为空，停止播放并清空
                    if (fullPlaylist.isEmpty()) {
                        Log.d(TAG, "播放列表为空，停止播放");
                        mediaController.stop();
                        currentPlaylistIndex = -1;
                        windowStart = 0;
                        windowEnd = 0;
                        
                        // 通知播放停止
                        for (PlaybackStateListener listener : playbackStateListeners) {
                            listener.stopPlay();
                        }
                        
                        // 通知播放列表为空
                        for (PlaybackStateListener listener : playbackStateListeners) {
                            listener.onPlaylistEmpty();
                        }
                    } else {
                        // 播放列表不为空，选择新的播放位置
                        // 参考removeFromPlaylist的逻辑：如果删除的是最后一首，播放前一首，否则播放下一首
                        int newPlayIndex;
                        if (currentPlaylistIndex >= fullPlaylist.size()) {
                            // 如果当前索引超出范围，播放最后一首
                            newPlayIndex = fullPlaylist.size() - 1;
                        } else {
                            // 否则播放当前位置的歌曲（删除后，后面的歌曲会前移）
                            newPlayIndex = currentPlaylistIndex;
                        }
                        
                        Log.d(TAG, "选择新的播放位置: " + newPlayIndex);
                        currentPlaylistIndex = newPlayIndex;
                        
                        // 重新加载播放窗口
                        loadInitialWindow(currentPlaylistIndex);
                        
                        // 开始播放
                        mediaController.play();
                    }
                } else {
                    // 当前播放歌曲未被删除，需要调整索引
                    Log.d(TAG, "当前播放歌曲未被删除，调整播放索引");
                    
                    // 计算在当前播放索引之前被删除的歌曲数量
                    int deletedBeforeCurrent = 0;
                    for (Song removedSong : songsToRemove) {
                        // 这里需要更精确的计算，暂时使用简化版本
                        // 由于我们无法直接知道被删除歌曲在原始列表中的确切位置，
                        // 我们使用一个简化的方法来调整索引
                        if (currentPlaylistIndex > 0) {
                            // 简化处理：假设被删除的歌曲平均分布，调整索引
                            deletedBeforeCurrent = Math.min(deletedBeforeCurrent + 1, currentPlaylistIndex);
                        }
                    }
                    
                    // 调整当前播放索引
                    if (currentPlaylistIndex > 0) {
                        currentPlaylistIndex = Math.max(0, currentPlaylistIndex - deletedBeforeCurrent);
                    }
                    
                    Log.d(TAG, "调整后的当前播放索引: " + currentPlaylistIndex);
                    
                    // 更新播放窗口
                    updatePlaylistAfterRemoval(-1); // 使用-1表示外部删除
                }

                // 保存更新后的播放列表到本地
                savePlayListJsonToLocal();
            }

            Log.d(TAG, "设备删除歌曲后的播放列表刷新完成");

        } catch (Exception e) {
            Log.e(TAG, "处理设备删除歌曲后的播放列表刷新时发生错误", e);
        }
    }


    /**
     * 获取当前播放的歌曲
     */
    @Nullable
    public Song getCurrentSong() {
        try {
            MediaItem currentMediaItem = mediaController.getCurrentMediaItem();
            if (currentMediaItem != null) {
                return playlistManager.getSongFromMediaItem(currentMediaItem);
            }
        } catch (Exception e) {
            Log.e(TAG, "Error getting current song", e);
        }
        return null;
    }

    /**
     * 获取当前播放列表（返回完整列表）
     */
    @NonNull
    public List<Song> getPlaylist() {
        return new ArrayList<>(fullPlaylist);
    }

    /**
     * 获取播放列表大小（返回完整列表大小）
     */
    public int getPlaylistSize() {
        return fullPlaylist.size();
    }

    /**
     * 获取当前播放索引（返回在完整列表中的索引）
     */
    public int getCurrentIndex() {
        return currentPlaylistIndex;
    }

    /**
     * 获取指定索引的歌曲（从完整列表中获取）
     */
    @Nullable
    public Song getSongAtIndex(int index) {
        if (index >= 0 && index < fullPlaylist.size()) {
            return fullPlaylist.get(index);
        }
        return null;
    }

    /**
     * 是否正在播放
     */
    public boolean isPlaying() {
        try {
            return mediaController.isPlaying();
        } catch (Exception e) {
            Log.e(TAG, "Error checking if playing", e);
            return false;
        }
    }

    /**
     * 获取播放状态
     */
    public int getPlaybackState() {
        try {
            return mediaController.getPlaybackState();
        } catch (Exception e) {
            Log.e(TAG, "Error getting playback state", e);
            return Player.STATE_IDLE;
        }
    }

    /**
     * 获取重复播放模式
     */
    public int getRepeatMode() {
        try {
            return mediaController.getRepeatMode();
        } catch (Exception e) {
            Log.e(TAG, "Error getting repeat mode", e);
            return Player.REPEAT_MODE_OFF;
        }
    }

    /**
     * 获取随机播放模式
     */
    public boolean getShuffleModeEnabled() {
        try {
            return mediaController.getShuffleModeEnabled();
        } catch (Exception e) {
            Log.e(TAG, "Error getting shuffle mode", e);
            return false;
        }
    }

    /**
     * 获取当前播放进度
     */
    public long getCurrentPosition() {
        try {
            return mediaController.getCurrentPosition();
        } catch (Exception e) {
            Log.e(TAG, "Error getting current position", e);
            return 0;
        }
    }

    /**
     * 获取总时长
     */
    public long getDuration() {
        try {
            return mediaController.getDuration();
        } catch (Exception e) {
            Log.e(TAG, "Error getting duration", e);
            return 0;
        }
    }

    /**
     * 获取缓冲进度
     */
    public long getBufferedPosition() {
        try {
            return mediaController.getBufferedPosition();
        } catch (Exception e) {
            Log.e(TAG, "Error getting buffered position", e);
            return 0;
        }
    }

    /**
     * 是否有下一首
     */
    public boolean hasNext() {
        try {
            return mediaController.hasNextMediaItem();
        } catch (Exception e) {
            Log.e(TAG, "Error checking if has next", e);
            return false;
        }
    }

    /**
     * 是否有上一首
     */
    public boolean hasPrevious() {
        try {
            return mediaController.hasPreviousMediaItem();
        } catch (Exception e) {
            Log.e(TAG, "Error checking if has previous", e);
            return false;
        }
    }

    /**
     * 播放/暂停切换
     */
    public void togglePlayPause() {
        try {
            if (mediaController.isPlaying()) {
                mediaController.pause();
            } else {
                mediaController.play();
            }
        } catch (Exception e) {
            Log.e(TAG, "Error toggling play/pause", e);
        }
    }

    /**
     * 播放
     */
    public void play() {
        try {
            mediaController.play();
        } catch (Exception e) {
            Log.e(TAG, "Error playing", e);
        }
    }

    /**
     * 暂停
     */
    public void pause() {
        try {
            mediaController.pause();
        } catch (Exception e) {
            Log.e(TAG, "Error pausing", e);
        }
    }

    /**
     * 跳转到下一首
     */
    public void skipToNext() {
        try {
            if (currentPlaylistIndex == fullPlaylist.size() - 1) {
                ToastUtils.showToast(context, context.getResources().getString(R.string.songs_lastest));
                return;
            }
            mediaController.seekToNext();
            savePlayListJsonToLocal();
        } catch (Exception e) {
            Log.e(TAG, "Error skipping to next", e);
        }
    }

    /**
     * 跳转到上一首
     */
    public void skipToPrevious() {
        try {
            mediaController.seekToPrevious();
            savePlayListJsonToLocal();
        } catch (Exception e) {
            Log.e(TAG, "Error skipping to previous", e);
        }
    }

    /**
     * 快进
     */
    public void fastForward() {
        try {
            long currentPosition = mediaController.getCurrentPosition();
            long newPosition = currentPosition + 30000; // 快进30秒
            mediaController.seekTo(newPosition);
        } catch (Exception e) {
            Log.e(TAG, "Error fast forwarding", e);
        }
    }

    /**
     * 快退
     */
    public void rewind() {
        try {
            long currentPosition = mediaController.getCurrentPosition();
            long newPosition = Math.max(0, currentPosition - 30000); // 快退30秒
            mediaController.seekTo(newPosition);
        } catch (Exception e) {
            Log.e(TAG, "Error rewinding", e);
        }
    }

    /**
     * 跳转到指定进度
     */
    public void seekTo(long positionMs) {
        try {
            mediaController.seekTo(positionMs);
        } catch (Exception e) {
            Log.e(TAG, "Error seeking to position: " + positionMs, e);
        }
    }

    /**
     * 按百分比跳转
     */
    public void seekToPercentage(float percentage) {
        try {
            long duration = mediaController.getDuration();
            if (duration > 0) {
                long position = (long) (duration * percentage);
                mediaController.seekTo(position);
            }
        } catch (Exception e) {
            Log.e(TAG, "Error seeking to percentage: " + percentage, e);
        }
    }

    /**
     * 获取MediaController（内部使用）
     */
    public MediaController getMediaController() {
        return mediaController;
    }

    /**
     * 清理MediaController资源
     */
    private void cleanupMediaController() {
        if (mediaController != null) {
            // 移除播放监听器
            mediaController.removeListener(this);
            mediaController.release();
            Log.d(TAG, "清理MediaController资源");
        }
    }

    /**
     * 内部清理资源
     */
    private void cleanupInternal() {
        try {
            cleanupMediaController();
            clearPlaybackStateListeners();
            fullPlaylist.clear();

            if (playlistManager != null) {
                playlistManager.cleanup();
            }

            Log.d(TAG, "MediaControllerHelper单例已清理");
        } catch (Exception e) {
            Log.e(TAG, "清理资源时发生错误", e);
        }
    }

    /**
     * 清理资源（公开方法）
     */
    public void cleanup() {
        cleanupInternal();
    }

    public void notifyProgressBarInit(long dur, long progress) {
        for (PlaybackStateListener playbackStateListener : playbackStateListeners) {
            playbackStateListener.progressInit(dur, progress);
        }
    }

    public void notifyStopPlay() {
        for (PlaybackStateListener playbackStateListener : playbackStateListeners) {
            playbackStateListener.stopPlay();
        }
    }

    /**
     * 初始化播放列表并跳转到指定位置，设置进度但不开始播放
     * @param songs 播放列表
     * @param position 歌曲位置（索引）
     * @param progressMs 歌曲进度（毫秒）
     */
    public void initializePlaylistWithPosition(@NonNull List<Song> songs, int position, long progressMs) {
        long startTime = System.currentTimeMillis();
        Log.d(TAG, "初始化播放列表并设置位置，歌曲数量: " + songs.size() + ", 位置: " + position + ", 进度: " + progressMs + "ms");

        try {
            if (songs.isEmpty()) {
                Log.w(TAG, "播放列表为空");
                return;
            }

            // 确保位置索引在有效范围内
            if (position < 0 || position >= songs.size()) {
                Log.w(TAG, "位置索引超出范围: " + position + ", 使用默认值0");
                position = 0;
            }

            // 保存完整播放列表
            fullPlaylist = new ArrayList<>(songs);
            currentPlaylistIndex = position;

            Log.d(TAG, "完整播放列表大小: " + fullPlaylist.size() + ", 当前索引: " + currentPlaylistIndex);
            mediaController.setPlayWhenReady(false);

            // 加载初始窗口
            loadInitialWindow(position);

            // 等待播放器准备完成后设置进度
            if (mediaController.getPlaybackState() == Player.STATE_READY) {
                setPositionAndProgress(position, progressMs);
            } else {
                // 如果播放器还没准备好，延迟设置进度
                mediaController.addListener(new Player.Listener() {
                    @Override
                    public void onPlaybackStateChanged(int playbackState) {
                        if (playbackState == Player.STATE_READY) {
                            setPositionAndProgress(currentPlaylistIndex, progressMs);
                            mediaController.removeListener(this);
                        }
                    }
                });
            }

            long totalTime = System.currentTimeMillis() - startTime;
            Log.d(TAG, "播放列表初始化完成，总耗时: " + totalTime + "ms");

        } catch (Exception e) {
            long totalTime = System.currentTimeMillis() - startTime;
            Log.e(TAG, "初始化播放列表时发生错误，耗时: " + totalTime + "ms", e);
        }

        savePlayListJsonToLocal();
    }

    /**
     * 设置位置和进度但不开始播放
     * @param position 歌曲位置
     * @param progressMs 歌曲进度（毫秒）
     */
    private void setPositionAndProgress(int position, long progressMs) {
        try {
            Log.d(TAG, "设置位置和进度，位置: " + position + ", 进度: " + progressMs + "ms");

            // 检查目标索引是否在当前窗口内
            if (position >= windowStart && position <= windowEnd) {
                // 在当前窗口内，直接跳转
                int mediaIndex = position - windowStart;
                mediaController.seekTo(mediaIndex, progressMs);
                mediaController.addListener(new Player.Listener() {
                    @Override
                    public void onPlaybackStateChanged(int playbackState) {
                        //PlaybackState.STATE_PAUSED
                        System.out.println("播放状态转变 = " + playbackState);
                        mediaController.pause();
                        notifyProgressBarInit(mediaController.getDuration(), progressMs);
                        notifyStopPlay();
                        System.out.println("mediaController.getDuration() = " + mediaController.getDuration());
                        mediaController.removeListener(this);
                    }
                });


                Log.d(TAG, "在当前窗口内设置位置，MediaIndex: " + mediaIndex + ", 进度: " + progressMs + "ms");
            } else {
                // 不在当前窗口内，重新加载窗口
                Log.d(TAG, "目标位置不在当前窗口内，重新加载窗口");
                loadInitialWindow(position);

                // 延迟设置进度，等待窗口加载完成
                mediaController.addListener(new Player.Listener() {
                    @Override
                    public void onPlaybackStateChanged(int playbackState) {
                        System.out.println("playbackState = " + playbackState);
                        if (playbackState == Player.STATE_READY) {
                            int mediaIndex = position - windowStart;
                            mediaController.seekTo(mediaIndex, progressMs);
                            mediaController.pause();
                            notifyProgressBarInit(mediaController.getDuration(), progressMs);
                            System.out.println("mediaController.getDuration() = " + mediaController.getDuration());
                            Log.d(TAG, "窗口重新加载后设置位置，MediaIndex: " + mediaIndex + ", 进度: " + progressMs + "ms");
                        }
                    }
                });
            }


        } catch (Exception e) {
            Log.e(TAG, "设置位置和进度时发生错误", e);
        }
    }
}

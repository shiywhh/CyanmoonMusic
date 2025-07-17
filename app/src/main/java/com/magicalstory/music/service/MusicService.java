package com.magicalstory.music.service;

import android.app.PendingIntent;
import android.content.Intent;
import android.content.IntentFilter;

import android.net.Uri;
import android.os.Bundle;
import android.os.IBinder;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.media3.common.AudioAttributes;
import androidx.media3.common.C;
import androidx.media3.common.ForwardingPlayer;
import androidx.media3.common.MediaItem;
import androidx.media3.common.MediaMetadata;
import androidx.media3.common.PlaybackException;
import androidx.media3.common.Player;
import androidx.media3.common.util.UnstableApi;
import androidx.media3.exoplayer.ExoPlayer;
import androidx.media3.session.MediaSession;
import androidx.media3.session.MediaSessionService;
import androidx.media3.session.SessionCommand;
import androidx.media3.session.SessionCommands;
import androidx.media3.session.SessionError;
import androidx.media3.session.SessionResult;

import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.magicalstory.music.MainActivity;
import com.magicalstory.music.model.Song;
import com.magicalstory.music.utils.PlaybackStateManager;
import com.magicalstory.music.utils.PlaylistManager;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

/**
 * 音乐播放服务 - 完全按照Google Media3框架最佳实践实现
 * 
 * 该服务继承自MediaSessionService，遵循以下原则：
 * 1. 使用MediaSession处理播放控制
 * 2. 使用ExoPlayer作为底层播放器
 * 3. 支持媒体控制通知
 * 4. 正确处理音频焦点
 * 5. 支持后台播放
 */
@UnstableApi
public class MusicService extends MediaSessionService implements Player.Listener {
    
    private static final String TAG = "MusicService";
    
    // 广播常量
    public static final String ACTION_SONG_CHANGED = "com.magicalstory.music.ACTION_SONG_CHANGED";
    public static final String ACTION_PLAY_STATE_CHANGED = "com.magicalstory.music.ACTION_PLAY_STATE_CHANGED";
    
    // 自定义播放控制命令
    private static final String CUSTOM_COMMAND_TOGGLE_SHUFFLE = "TOGGLE_SHUFFLE";
    private static final String CUSTOM_COMMAND_TOGGLE_REPEAT = "TOGGLE_REPEAT";
    private static final String CUSTOM_COMMAND_SET_PLAYLIST = "SET_PLAYLIST";
    private static final String CUSTOM_COMMAND_PLAY_SONG_AT_INDEX = "PLAY_SONG_AT_INDEX";
    
    // 播放器和媒体会话
    private ExoPlayer player;
    private MediaSession mediaSession;
    private CustomPlayerWrapper playerWrapper;
    
    // 播放状态管理
    private PlaybackStateManager playbackStateManager;
    private PlaylistManager playlistManager;
    
    // 音频焦点管理已由Media3的AudioFocusManager处理，移除自定义处理
    
    @Override
    public void onCreate() {
        super.onCreate();
        Log.d(TAG, "MusicService开始创建");
        
        Log.d(TAG, "1. 初始化播放器");
        initializePlayer();
        
        Log.d(TAG, "2. 初始化媒体会话");
        initializeMediaSession();
        
        Log.d(TAG, "3. 初始化管理器");
        initializeManagers();
        
        Log.d(TAG, "MusicService创建完成");
    }
    
    /**
     * 初始化播放器
     */
    private void initializePlayer() {
        Log.d(TAG, "开始初始化播放器");
        
        // 创建音频属性
        AudioAttributes audioAttributes = new AudioAttributes.Builder()
                .setContentType(C.AUDIO_CONTENT_TYPE_MUSIC)
                .setUsage(C.USAGE_MEDIA)
                .build();
        
        Log.d(TAG, "音频属性创建完成");
        
        // 创建ExoPlayer
        player = new ExoPlayer.Builder(this)
                .setAudioAttributes(audioAttributes, true)
                .setHandleAudioBecomingNoisy(true)
                .build();
        
        Log.d(TAG, "ExoPlayer创建完成");
        
        // 添加播放器监听器
        player.addListener(this);
        Log.d(TAG, "播放器监听器添加完成");
        
        // 创建自定义播放器包装器
        playerWrapper = new CustomPlayerWrapper(player);
        Log.d(TAG, "自定义播放器包装器创建完成");
        
        Log.d(TAG, "播放器初始化完成");
    }
    

    
    /**
     * 初始化媒体会话
     */
    private void initializeMediaSession() {
        Log.d(TAG, "开始初始化媒体会话");
        
        // 创建用于启动Activity的PendingIntent
        PendingIntent sessionActivity = PendingIntent.getActivity(
                this,
                0,
                new Intent(this, MainActivity.class),
                PendingIntent.FLAG_IMMUTABLE | PendingIntent.FLAG_UPDATE_CURRENT
        );
        
        Log.d(TAG, "PendingIntent创建完成");
        
        // 创建MediaSession
        mediaSession = new MediaSession.Builder(this, playerWrapper)
                .setCallback(new MediaSessionCallback())
                .setSessionActivity(sessionActivity)
                .build();
        
        Log.d(TAG, "MediaSession创建完成");
        Log.d(TAG, "媒体会话初始化完成");
    }
    
    /**
     * 初始化管理器
     */
    private void initializeManagers() {
        Log.d(TAG, "开始初始化管理器");
        
        playbackStateManager = new PlaybackStateManager(this);
        Log.d(TAG, "PlaybackStateManager初始化完成");
        
        playlistManager = new PlaylistManager(this);
        Log.d(TAG, "PlaylistManager初始化完成");
        
        Log.d(TAG, "管理器初始化完成");
    }
    
    @Override
    public MediaSession onGetSession(MediaSession.ControllerInfo controllerInfo) {
        return mediaSession;
    }
    
    @Override
    public void onDestroy() {
        Log.d(TAG, "MusicService开始销毁");
        
        // 释放资源
        if (mediaSession != null) {
            Log.d(TAG, "释放MediaSession");
            mediaSession.release();
            mediaSession = null;
        }
        
        if (player != null) {
            Log.d(TAG, "释放播放器");
            player.removeListener(this);
            player.release();
            player = null;
        }
        

        
        super.onDestroy();
        Log.d(TAG, "MusicService销毁完成");
    }
    
    // ===========================================
    // MediaSession回调处理
    // ===========================================
    
    /**
     * MediaSession回调类，处理播放控制逻辑
     */
    private class MediaSessionCallback implements MediaSession.Callback {
        
        @Override
        public MediaSession.ConnectionResult onConnect(MediaSession session, 
                MediaSession.ControllerInfo controller) {
            
            // 构建可用的会话命令
            SessionCommands.Builder availableCommands = new SessionCommands.Builder();
            
            // 添加自定义命令
            availableCommands.add(new SessionCommand(CUSTOM_COMMAND_TOGGLE_SHUFFLE, Bundle.EMPTY));
            availableCommands.add(new SessionCommand(CUSTOM_COMMAND_TOGGLE_REPEAT, Bundle.EMPTY));
            availableCommands.add(new SessionCommand(CUSTOM_COMMAND_SET_PLAYLIST, Bundle.EMPTY));
            availableCommands.add(new SessionCommand(CUSTOM_COMMAND_PLAY_SONG_AT_INDEX, Bundle.EMPTY));
            
            return MediaSession.ConnectionResult.accept(
                    availableCommands.build(),
                    MediaSession.ConnectionResult.DEFAULT_PLAYER_COMMANDS
            );
        }
        
        @Override
        public ListenableFuture<SessionResult> onCustomCommand(
                MediaSession session,
                MediaSession.ControllerInfo controller,
                SessionCommand customCommand,
                Bundle args) {
            
            switch (customCommand.customAction) {
                case CUSTOM_COMMAND_TOGGLE_SHUFFLE:
                    return handleToggleShuffle();
                    
                case CUSTOM_COMMAND_TOGGLE_REPEAT:
                    return handleToggleRepeat();
                    
                case CUSTOM_COMMAND_SET_PLAYLIST:
                    return handleSetPlaylist(args);
                    
                case CUSTOM_COMMAND_PLAY_SONG_AT_INDEX:
                    return handlePlaySongAtIndex(args);
                    
                default:
                    return Futures.immediateFuture(
                            new SessionResult(SessionError.ERROR_NOT_SUPPORTED)
                    );
            }
        }
        
        @Override
        public ListenableFuture<List<MediaItem>> onAddMediaItems(
                MediaSession session,
                MediaSession.ControllerInfo controller,
                List<MediaItem> mediaItems) {
            
            // 处理添加媒体项到播放列表
            List<MediaItem> updatedMediaItems = playlistManager.addMediaItems(mediaItems);
            return Futures.immediateFuture(updatedMediaItems);
        }
        
        @Override
        public ListenableFuture<androidx.media3.session.MediaSession.MediaItemsWithStartPosition> onSetMediaItems(
                MediaSession session,
                MediaSession.ControllerInfo controller,
                List<MediaItem> mediaItems,
                int startIndex,
                long startPositionMs) {
            
            try {
                long time = System.currentTimeMillis();

                // 处理设置播放列表
                List<MediaItem> updatedMediaItems = playlistManager.setMediaItems(mediaItems);
                
                if (updatedMediaItems.isEmpty()) {
                    Log.w(TAG, "没有有效的MediaItem，返回空列表");
                    return Futures.immediateFuture(new androidx.media3.session.MediaSession.MediaItemsWithStartPosition(
                            new ArrayList<>(), 0, 0));
                }
                
                // 验证所有MediaItem都有有效的URI
                for (MediaItem item : updatedMediaItems) {
                    if (item.localConfiguration == null || item.localConfiguration.uri == null) {
                        Log.e(TAG, "MediaItem没有有效的URI: " + item.mediaId);
                    }
                }
                
                // 确保startIndex在有效范围内
                int finalStartIndex = Math.min(startIndex, updatedMediaItems.size() - 1);
                
                // 设置到播放器
                player.setMediaItems(updatedMediaItems, finalStartIndex, startPositionMs);
                
                return Futures.immediateFuture(new androidx.media3.session.MediaSession.MediaItemsWithStartPosition(
                        updatedMediaItems, finalStartIndex, startPositionMs));
            } catch (Exception e) {
                Log.e(TAG, "设置MediaItems时发生错误", e);
                // 返回空列表避免崩溃
                List<MediaItem> emptyList = new ArrayList<>();
                return Futures.immediateFuture(new androidx.media3.session.MediaSession.MediaItemsWithStartPosition(
                        emptyList, 0, 0));
            }
        }
        
        public ListenableFuture<SessionResult> onPlay(
                MediaSession session,
                MediaSession.ControllerInfo controller) {
            
            Log.d(TAG, "收到播放命令 - onPlay");
            Log.d(TAG, "当前播放器状态: " + player.getPlaybackState());
            Log.d(TAG, "当前播放: " + player.isPlaying());
            Log.d(TAG, "当前媒体项: " + (player.getCurrentMediaItem() != null ? 
                player.getCurrentMediaItem().mediaId : "null"));
            Log.d(TAG, "播放列表大小: " + player.getMediaItemCount());
            
            // 音频焦点由Media3的AudioFocusManager自动处理
            Log.d(TAG, "开始播放");
            player.setPlayWhenReady(true);
            return Futures.immediateFuture(new SessionResult(SessionResult.RESULT_SUCCESS));
        }
        
        public ListenableFuture<SessionResult> onPause(
                MediaSession session,
                MediaSession.ControllerInfo controller) {
            
            Log.d(TAG, "收到暂停命令 - onPause");
            Log.d(TAG, "当前播放器状态: " + player.getPlaybackState());
            Log.d(TAG, "当前播放: " + player.isPlaying());
            
            player.setPlayWhenReady(false);
            Log.d(TAG, "暂停命令执行完成");
            return Futures.immediateFuture(new SessionResult(SessionResult.RESULT_SUCCESS));
        }
        
        public ListenableFuture<SessionResult> onStop(
                MediaSession session,
                MediaSession.ControllerInfo controller) {
            
            Log.d(TAG, "收到停止命令 - onStop");
            Log.d(TAG, "当前播放器状态: " + player.getPlaybackState());
            Log.d(TAG, "当前播放: " + player.isPlaying());
            
            player.stop();
            // 音频焦点由Media3的AudioFocusManager自动处理
            Log.d(TAG, "停止命令执行完成");
            return Futures.immediateFuture(new SessionResult(SessionResult.RESULT_SUCCESS));
        }
        
        public ListenableFuture<SessionResult> onSeekTo(
                MediaSession session,
                MediaSession.ControllerInfo controller,
                long positionMs) {
            
            Log.d(TAG, "onSeekTo called: " + positionMs);
            player.seekTo(positionMs);
            return Futures.immediateFuture(new SessionResult(SessionResult.RESULT_SUCCESS));
        }
        
        public ListenableFuture<SessionResult> onSkipToNext(
                MediaSession session,
                MediaSession.ControllerInfo controller) {
            
            Log.d(TAG, "onSkipToNext called");
            player.seekToNext();
            return Futures.immediateFuture(new SessionResult(SessionResult.RESULT_SUCCESS));
        }
        
        public ListenableFuture<SessionResult> onSkipToPrevious(
                MediaSession session,
                MediaSession.ControllerInfo controller) {
            
            Log.d(TAG, "onSkipToPrevious called");
            player.seekToPrevious();
            return Futures.immediateFuture(new SessionResult(SessionResult.RESULT_SUCCESS));
        }
    }
    
    // ===========================================
    // 自定义命令处理
    // ===========================================
    
    /**
     * 处理切换随机播放
     */
    private ListenableFuture<SessionResult> handleToggleShuffle() {
        boolean shuffleEnabled = !player.getShuffleModeEnabled();
        player.setShuffleModeEnabled(shuffleEnabled);
        
        Log.d(TAG, "Shuffle mode: " + shuffleEnabled);
        
        // 通知播放状态变化
        playbackStateManager.notifyShuffleChanged(shuffleEnabled);
        
        return Futures.immediateFuture(new SessionResult(SessionResult.RESULT_SUCCESS));
    }
    
    /**
     * 处理切换重复播放
     */
    private ListenableFuture<SessionResult> handleToggleRepeat() {
        @Player.RepeatMode int currentMode = player.getRepeatMode();
        @Player.RepeatMode int newMode;
        
        switch (currentMode) {
            case Player.REPEAT_MODE_OFF:
                newMode = Player.REPEAT_MODE_ALL;
                break;
            case Player.REPEAT_MODE_ALL:
                newMode = Player.REPEAT_MODE_ONE;
                break;
            case Player.REPEAT_MODE_ONE:
            default:
                newMode = Player.REPEAT_MODE_OFF;
                break;
        }
        
        player.setRepeatMode(newMode);
        
        Log.d(TAG, "Repeat mode: " + newMode);
        
        // 通知播放状态变化
        playbackStateManager.notifyRepeatModeChanged(newMode);
        
        return Futures.immediateFuture(new SessionResult(SessionResult.RESULT_SUCCESS));
    }
    
    /**
     * 处理设置播放列表
     */
    private ListenableFuture<SessionResult> handleSetPlaylist(Bundle args) {
        // 这里可以从args中获取播放列表数据
        // 由于Bundle无法传递复杂对象，可以通过其他方式传递播放列表
        
        Log.d(TAG, "Set playlist requested");
        
        return Futures.immediateFuture(new SessionResult(SessionResult.RESULT_SUCCESS));
    }
    
    /**
     * 处理播放指定索引的歌曲
     */
    private ListenableFuture<SessionResult> handlePlaySongAtIndex(Bundle args) {
        int index = args.getInt("index", 0);
        
        if (index >= 0 && index < player.getMediaItemCount()) {
            player.seekTo(index, 0);
            player.setPlayWhenReady(true);
            
            Log.d(TAG, "Playing song at index: " + index);
            
            return Futures.immediateFuture(new SessionResult(SessionResult.RESULT_SUCCESS));
        } else {
            Log.w(TAG, "Invalid song index: " + index);
            return Futures.immediateFuture(new SessionResult(SessionError.ERROR_BAD_VALUE));
        }
    }
    

    
    // ===========================================
    // Player.Listener 回调
    // ===========================================
    
    @Override
    public void onPlaybackStateChanged(int playbackState) {
        Log.d(TAG, "播放状态变化: " + playbackState);
        
        switch (playbackState) {
            case Player.STATE_IDLE:
                Log.d(TAG, "播放器状态: 空闲");
                break;
            case Player.STATE_BUFFERING:
                Log.d(TAG, "播放器状态: 缓冲中");
                break;
            case Player.STATE_READY:
                Log.d(TAG, "播放器状态: 准备就绪");
                break;
            case Player.STATE_ENDED:
                Log.d(TAG, "播放器状态: 播放结束");
                break;
            default:
                Log.d(TAG, "播放器状态: 未知状态 " + playbackState);
                break;
        }
        
        // 通知播放状态变化
        playbackStateManager.notifyPlaybackStateChanged(playbackState);
    }
    
    @Override
    public void onIsPlayingChanged(boolean isPlaying) {
        Log.d(TAG, "播放状态变化: " + (isPlaying ? "正在播放" : "暂停"));
        
        // 通知播放状态变化
        playbackStateManager.notifyIsPlayingChanged(isPlaying);
    }
    
    @Override
    public void onMediaItemTransition(@Nullable MediaItem mediaItem, int reason) {
        String mediaId = mediaItem != null ? mediaItem.mediaId : "null";
        Log.d(TAG, "媒体项切换: " + mediaId + ", 原因: " + reason);
        
        switch (reason) {
            case Player.MEDIA_ITEM_TRANSITION_REASON_REPEAT:
                Log.d(TAG, "切换原因: 重复播放");
                break;
            case Player.MEDIA_ITEM_TRANSITION_REASON_AUTO:
                Log.d(TAG, "切换原因: 自动播放下一首");
                break;
            case Player.MEDIA_ITEM_TRANSITION_REASON_SEEK:
                Log.d(TAG, "切换原因: 跳转到指定位置");
                break;
            case Player.MEDIA_ITEM_TRANSITION_REASON_PLAYLIST_CHANGED:
                Log.d(TAG, "切换原因: 播放列表变化");
                break;
            default:
                Log.d(TAG, "切换原因: 未知原因 " + reason);
                break;
        }
        
        // 通知当前歌曲变化
        playbackStateManager.notifyCurrentMediaItemChanged(mediaItem);
    }
    
    @Override
    public void onPlayerError(PlaybackException error) {
        Log.e(TAG, "播放器错误: " + error.getMessage(), error);
        Log.e(TAG, "错误代码: " + error.errorCode);
        Log.e(TAG, "错误时间戳: " + error.timestampMs);
        
        if (error.getCause() != null) {
            Log.e(TAG, "错误原因: " + error.getCause().getMessage());
        }
        
        // 通知播放错误
        playbackStateManager.notifyPlayerError(error);
    }
    
    @Override
    public void onPositionDiscontinuity(Player.PositionInfo oldPosition, 
            Player.PositionInfo newPosition, int reason) {
        Log.d(TAG, "播放位置不连续: " + reason);
        Log.d(TAG, "旧位置: " + oldPosition.positionMs + ", 新位置: " + newPosition.positionMs);
        
        // 通知进度变化
        playbackStateManager.notifyPositionChanged(newPosition.positionMs);
    }
    
    // ===========================================
    // 自定义播放器包装器
    // ===========================================
    
    /**
     * 自定义播放器包装器，用于扩展ExoPlayer的功能
     */
    private class CustomPlayerWrapper extends ForwardingPlayer {
        
        public CustomPlayerWrapper(Player player) {
            super(player);
            Log.d(TAG, "CustomPlayerWrapper创建完成");
        }
        
        @Override
        public void play() {
            Log.d(TAG, "CustomPlayerWrapper.play() 被调用");
            // 音频焦点由Media3的AudioFocusManager自动处理
            Log.d(TAG, "调用父类play()");
            super.play();
        }
        
        @Override
        public void pause() {
            Log.d(TAG, "CustomPlayerWrapper.pause() 被调用");
            super.pause();
            // 音频焦点由Media3的AudioFocusManager自动处理
        }
        
        @Override
        public void stop() {
            Log.d(TAG, "CustomPlayerWrapper.stop() 被调用");
            super.stop();
            // 音频焦点由Media3的AudioFocusManager自动处理
        }
    }
    
    // ===========================================
    // 公共API方法 (供外部调用)
    // ===========================================
    
    /**
     * 设置播放列表
     */
    public void setPlaylist(List<Song> songs) {
        if (songs == null || songs.isEmpty()) {
            Log.w(TAG, "Empty playlist provided");
            return;
        }
        
        List<MediaItem> mediaItems = playlistManager.createMediaItems(songs);
        long time = System.currentTimeMillis();
        player.setMediaItems(mediaItems);
        long time2 = System.currentTimeMillis();
        Log.d(TAG, "设置播放列表耗时: " + (time2 - time) + "ms");
        Log.d(TAG, "Playlist set with " + songs.size() + " songs");
    }
    
    /**
     * 播放指定歌曲
     */
    public void playSong(Song song) {
        if (song == null) {
            Log.w(TAG, "Null song provided");
            return;
        }
        
        MediaItem mediaItem = playlistManager.createMediaItem(song);
        player.setMediaItem(mediaItem);
        player.prepare();
        player.play();
        
        Log.d(TAG, "Playing song: " + song.getTitle());
    }
    
    /**
     * 获取当前播放的歌曲
     */
    @Nullable
    public Song getCurrentSong() {
        MediaItem currentMediaItem = player.getCurrentMediaItem();
        if (currentMediaItem != null) {
            return playlistManager.getSongFromMediaItem(currentMediaItem);
        }
        return null;
    }
    
    /**
     * 获取MediaSession
     */
    @Nullable
    public MediaSession getMediaSession() {
        return mediaSession;
    }
} 
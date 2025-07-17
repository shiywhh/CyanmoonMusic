package com.magicalstory.music.utils;

import android.content.Context;
import android.content.Intent;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;
import androidx.media3.common.MediaItem;
import androidx.media3.common.PlaybackException;
import androidx.media3.common.Player;

import com.magicalstory.music.model.PlayHistory;
import com.magicalstory.music.model.Song;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * 播放状态管理器
 * 负责管理播放状态的变化和通知UI更新
 */
public class PlaybackStateManager {
    
    private static final String TAG = "PlaybackStateManager";
    
    // 广播Action
    public static final String ACTION_PLAYBACK_STATE_CHANGED = "com.magicalstory.music.PLAYBACK_STATE_CHANGED";
    public static final String ACTION_IS_PLAYING_CHANGED = "com.magicalstory.music.IS_PLAYING_CHANGED";
    public static final String ACTION_MEDIA_ITEM_CHANGED = "com.magicalstory.music.MEDIA_ITEM_CHANGED";
    public static final String ACTION_PLAYER_ERROR = "com.magicalstory.music.PLAYER_ERROR";
    public static final String ACTION_POSITION_CHANGED = "com.magicalstory.music.POSITION_CHANGED";
    public static final String ACTION_SHUFFLE_CHANGED = "com.magicalstory.music.SHUFFLE_CHANGED";
    public static final String ACTION_REPEAT_MODE_CHANGED = "com.magicalstory.music.REPEAT_MODE_CHANGED";
    
    // 广播Extra Keys
    public static final String EXTRA_PLAYBACK_STATE = "playback_state";
    public static final String EXTRA_IS_PLAYING = "is_playing";
    public static final String EXTRA_MEDIA_ID = "media_id";
    public static final String EXTRA_ERROR_MESSAGE = "error_message";
    public static final String EXTRA_POSITION = "position";
    public static final String EXTRA_SHUFFLE_ENABLED = "shuffle_enabled";
    public static final String EXTRA_REPEAT_MODE = "repeat_mode";
    
    private final Context context;
    private final LocalBroadcastManager broadcastManager;
    private final ExecutorService backgroundExecutor;
    private final Handler mainHandler;
    
    // 播放历史记录相关
    private long playStartTime;
    private long totalPlayTime;
    private Song currentSong;
    
    public PlaybackStateManager(@NonNull Context context) {
        this.context = context;
        this.broadcastManager = LocalBroadcastManager.getInstance(context);
        this.backgroundExecutor = Executors.newSingleThreadExecutor();
        this.mainHandler = new Handler(Looper.getMainLooper());
    }
    
    /**
     * 通知播放状态变化
     */
    public void notifyPlaybackStateChanged(@Player.State int playbackState) {
        Log.d(TAG, "Notifying playback state changed: " + playbackState);
        
        Intent intent = new Intent(ACTION_PLAYBACK_STATE_CHANGED);
        intent.putExtra(EXTRA_PLAYBACK_STATE, playbackState);
        broadcastManager.sendBroadcast(intent);
        
        // 处理播放历史记录
        if (playbackState == Player.STATE_READY) {
            startPlayTimeTracking();
        } else if (playbackState == Player.STATE_ENDED) {
            recordPlayHistory(true);
        }
    }
    
    /**
     * 通知播放/暂停状态变化
     */
    public void notifyIsPlayingChanged(boolean isPlaying) {
        Log.d(TAG, "Notifying is playing changed: " + isPlaying);
        
        Intent intent = new Intent(ACTION_IS_PLAYING_CHANGED);
        intent.putExtra(EXTRA_IS_PLAYING, isPlaying);
        broadcastManager.sendBroadcast(intent);
        
        // 处理播放时间跟踪
        if (isPlaying) {
            startPlayTimeTracking();
        } else {
            pausePlayTimeTracking();
        }
    }
    
    /**
     * 通知当前媒体项变化
     */
    public void notifyCurrentMediaItemChanged(@Nullable MediaItem mediaItem) {
        Log.d(TAG, "Notifying current media item changed: " + 
              (mediaItem != null ? mediaItem.mediaId : "null"));
        
        // 如果切换到新歌曲，先记录上一首的播放历史
        if (currentSong != null && mediaItem != null && 
            !String.valueOf(currentSong.getId()).equals(mediaItem.mediaId)) {
            recordPlayHistory(false);
        }
        
        // 更新当前歌曲
        updateCurrentSong(mediaItem);
        
        Intent intent = new Intent(ACTION_MEDIA_ITEM_CHANGED);
        intent.putExtra(EXTRA_MEDIA_ID, mediaItem != null ? mediaItem.mediaId : "");
        broadcastManager.sendBroadcast(intent);
    }
    
    /**
     * 通知播放错误
     */
    public void notifyPlayerError(@NonNull PlaybackException error) {
        Log.e(TAG, "Notifying player error: " + error.getMessage());
        
        Intent intent = new Intent(ACTION_PLAYER_ERROR);
        intent.putExtra(EXTRA_ERROR_MESSAGE, error.getMessage());
        broadcastManager.sendBroadcast(intent);
    }
    
    /**
     * 通知播放进度变化
     */
    public void notifyPositionChanged(long positionMs) {
        Intent intent = new Intent(ACTION_POSITION_CHANGED);
        intent.putExtra(EXTRA_POSITION, positionMs);
        broadcastManager.sendBroadcast(intent);
    }
    
    /**
     * 通知随机播放模式变化
     */
    public void notifyShuffleChanged(boolean shuffleEnabled) {
        Log.d(TAG, "Notifying shuffle changed: " + shuffleEnabled);
        
        Intent intent = new Intent(ACTION_SHUFFLE_CHANGED);
        intent.putExtra(EXTRA_SHUFFLE_ENABLED, shuffleEnabled);
        broadcastManager.sendBroadcast(intent);
    }
    
    /**
     * 通知重复播放模式变化
     */
    public void notifyRepeatModeChanged(@Player.RepeatMode int repeatMode) {
        Log.d(TAG, "Notifying repeat mode changed: " + repeatMode);
        
        Intent intent = new Intent(ACTION_REPEAT_MODE_CHANGED);
        intent.putExtra(EXTRA_REPEAT_MODE, repeatMode);
        broadcastManager.sendBroadcast(intent);
    }
    
    /**
     * 开始播放时间跟踪
     */
    private void startPlayTimeTracking() {
        if (playStartTime == 0) {
            playStartTime = System.currentTimeMillis();
        }
    }
    
    /**
     * 暂停播放时间跟踪
     */
    private void pausePlayTimeTracking() {
        if (playStartTime > 0) {
            totalPlayTime += System.currentTimeMillis() - playStartTime;
            playStartTime = 0;
        }
    }
    
    /**
     * 更新当前歌曲
     */
    private void updateCurrentSong(@Nullable MediaItem mediaItem) {
        if (mediaItem != null) {
            try {
                long songId = Long.parseLong(mediaItem.mediaId);
                currentSong = org.litepal.LitePal.find(Song.class, songId);
                // 重置播放时间跟踪
                playStartTime = 0;
                totalPlayTime = 0;
            } catch (NumberFormatException e) {
                Log.e(TAG, "Invalid media ID: " + mediaItem.mediaId, e);
                currentSong = null;
            }
        } else {
            currentSong = null;
        }
    }
    
    /**
     * 记录播放历史
     */
    private void recordPlayHistory(boolean isCompleted) {
        if (currentSong == null) return;
        
        // 更新播放时间
        pausePlayTimeTracking();
        
        final long songId = currentSong.getId();
        final long playTime = totalPlayTime;
        final long albumId = currentSong.getAlbumId();
        final long artistId = currentSong.getArtistId();
        final boolean completed = isCompleted;
        
        // 在后台线程中记录播放历史
        backgroundExecutor.execute(() -> {
            try {
                // 记录播放历史
                PlayHistory.recordPlay(songId, playTime, completed, completed ? 1.0 : 0.0);
                
                // 更新歌曲、专辑、艺术家的最后播放时间
                updateLastPlayedTime(songId, albumId, artistId);
                
                Log.d(TAG, "Play history recorded for song: " + currentSong.getTitle());
            } catch (Exception e) {
                Log.e(TAG, "Error recording play history", e);
            }
        });
    }
    
    /**
     * 更新最后播放时间
     */
    private void updateLastPlayedTime(long songId, long albumId, long artistId) {
        long currentTime = System.currentTimeMillis();
        
        try {
            // 更新歌曲最后播放时间
            Song song = org.litepal.LitePal.find(Song.class, songId);
            if (song != null) {
                song.setLastplayed(currentTime);
                song.saveThrows();
            }
            
            // 更新专辑最后播放时间
            com.magicalstory.music.model.Album album = org.litepal.LitePal
                    .where("albumId = ?", String.valueOf(albumId))
                    .findFirst(com.magicalstory.music.model.Album.class);
            if (album != null) {
                album.setLastplayed(currentTime);
                album.saveThrows();
            }
            
            // 更新艺术家最后播放时间
            com.magicalstory.music.model.Artist artist = org.litepal.LitePal
                    .where("artistId = ?", String.valueOf(artistId))
                    .findFirst(com.magicalstory.music.model.Artist.class);
            if (artist != null) {
                artist.setLastplayed(currentTime);
                artist.saveThrows();
            }
        } catch (Exception e) {
            Log.e(TAG, "Error updating last played time", e);
        }
    }

} 
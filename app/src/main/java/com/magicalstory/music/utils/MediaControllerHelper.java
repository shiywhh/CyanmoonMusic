package com.magicalstory.music.utils;

import android.content.Context;
import android.os.Bundle;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.media3.common.MediaItem;
import androidx.media3.common.Player;
import androidx.media3.common.util.UnstableApi;
import androidx.media3.session.MediaController;
import androidx.media3.session.SessionCommand;

import com.magicalstory.music.model.Song;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * MediaController辅助类
 * 提供便捷的播放控制方法和状态管理
 */
@UnstableApi
public class MediaControllerHelper {
    
    private static final String TAG = "MediaControllerHelper";
    
    // 自定义命令
    private static final String CUSTOM_COMMAND_TOGGLE_SHUFFLE = "TOGGLE_SHUFFLE";
    private static final String CUSTOM_COMMAND_TOGGLE_REPEAT = "TOGGLE_REPEAT";
    private static final String CUSTOM_COMMAND_SET_PLAYLIST = "SET_PLAYLIST";
    private static final String CUSTOM_COMMAND_PLAY_SONG_AT_INDEX = "PLAY_SONG_AT_INDEX";
    
    private final MediaController mediaController;
    private final PlaylistManager playlistManager;
    
    public MediaControllerHelper(@NonNull MediaController mediaController, Context context) {
        this.mediaController = mediaController;
        this.playlistManager = new PlaylistManager(context);
    }
    
    /**
     * 播放单个歌曲
     */
    public void playSong(@NonNull Song song) {
        long startTime = System.currentTimeMillis();
        Log.d(TAG, "播放单个歌曲: " + song.getTitle());
        Log.d(TAG, "歌曲路径: " + song.getPath());
        Log.d(TAG, "歌曲时长: " + song.getDuration());
        
        try {
            // 创建MediaItem
            long createMediaItemStart = System.currentTimeMillis();
            MediaItem mediaItem = playlistManager.createMediaItem(song);
            long createMediaItemEnd = System.currentTimeMillis();
            Log.d(TAG, "创建MediaItem耗时: " + (createMediaItemEnd - createMediaItemStart) + "ms");
            
            if (mediaItem != null) {
                Log.d(TAG, "MediaItem创建成功: " + mediaItem.mediaId);
                Log.d(TAG, "MediaItem URI: " + mediaItem.localConfiguration.uri);
                
                // 设置媒体项到播放器
                Log.d(TAG, "设置媒体项到播放器");
                long setMediaItemStart = System.currentTimeMillis();
                mediaController.setMediaItem(mediaItem);
                long setMediaItemEnd = System.currentTimeMillis();
                Log.d(TAG, "MediaController.setMediaItem耗时: " + (setMediaItemEnd - setMediaItemStart) + "ms");
                
                // 准备播放器
                Log.d(TAG, "准备播放器");
                long prepareStart = System.currentTimeMillis();
                mediaController.prepare();
                long prepareEnd = System.currentTimeMillis();
                Log.d(TAG, "MediaController.prepare耗时: " + (prepareEnd - prepareStart) + "ms");
                
                // 开始播放
                Log.d(TAG, "开始播放");
                long playStart = System.currentTimeMillis();
                mediaController.play();
                long playEnd = System.currentTimeMillis();
                Log.d(TAG, "MediaController.play耗时: " + (playEnd - playStart) + "ms");
                
                long totalTime = System.currentTimeMillis() - startTime;
                Log.d(TAG, "播放命令执行完成，总耗时: " + totalTime + "ms, 歌曲: " + song.getTitle());
            } else {
                Log.e(TAG, "MediaItem创建失败: " + song.getTitle());
            }
        } catch (Exception e) {
            long totalTime = System.currentTimeMillis() - startTime;
            Log.e(TAG, "播放歌曲时发生错误，耗时: " + totalTime + "ms, 歌曲: " + song.getTitle(), e);
        }
    }
    
    /**
     * 设置播放列表
     */
    public void setPlaylist(@NonNull List<Song> songs) {
        setPlaylist(songs, 0);
    }
    
    /**
     * 设置播放列表并指定起始索引
     */
    public void setPlaylist(@NonNull List<Song> songs, int startIndex) {
        long startTime = System.currentTimeMillis();
        Log.d(TAG, "设置播放列表，歌曲数量: " + songs.size() + ", 起始索引: " + startIndex);
        
        try {
            if (songs.isEmpty()) {
                Log.w(TAG, "播放列表为空");
                return;
            }
            
            // 确保起始索引在有效范围内
            long validationStart = System.currentTimeMillis();
            if (startIndex < 0 || startIndex >= songs.size()) {
                Log.w(TAG, "起始索引超出范围: " + startIndex + ", 使用默认值0");
                startIndex = 0;
            }
            long validationEnd = System.currentTimeMillis();
            
            if (validationEnd - validationStart > 1) {
                Log.d(TAG, "索引验证耗时: " + (validationEnd - validationStart) + "ms");
            }
            
            // 调用PlaylistManager创建MediaItem列表
            long playlistManagerStart = System.currentTimeMillis();
            List<MediaItem> mediaItems = playlistManager.setPlaylist(songs, startIndex);
            long playlistManagerEnd = System.currentTimeMillis();
            Log.d(TAG, "PlaylistManager创建MediaItem列表耗时: " + (playlistManagerEnd - playlistManagerStart) + "ms");
            
            if (!mediaItems.isEmpty()) {
                Log.d(TAG, "成功创建 " + mediaItems.size() + " 个MediaItem");
                
                // 确保startIndex在有效范围内
                int finalStartIndex = Math.min(startIndex, mediaItems.size() - 1);
                
                // 设置媒体项列表到播放器
                Log.d(TAG, "设置媒体项列表到播放器");
                long setMediaItemsStart = System.currentTimeMillis();
                mediaController.setMediaItems(mediaItems, finalStartIndex, 0);
                long setMediaItemsEnd = System.currentTimeMillis();
                Log.d(TAG, "MediaController.setMediaItems耗时: " + (setMediaItemsEnd - setMediaItemsStart) + "ms");
                
                // 准备播放器
                Log.d(TAG, "准备播放器");
                long prepareStart = System.currentTimeMillis();
                mediaController.prepare();
                long prepareEnd = System.currentTimeMillis();
                Log.d(TAG, "MediaController.prepare耗时: " + (prepareEnd - prepareStart) + "ms");
                
                long totalTime = System.currentTimeMillis() - startTime;
                Log.d(TAG, "播放列表设置完成，总耗时: " + totalTime + "ms");
            } else {
                Log.w(TAG, "没有有效的MediaItem创建");
            }
        } catch (Exception e) {
            long totalTime = System.currentTimeMillis() - startTime;
            Log.e(TAG, "设置播放列表时发生错误，耗时: " + totalTime + "ms", e);
        }
    }
    
    /**
     * 添加歌曲到播放列表
     */
    public void addToPlaylist(@NonNull Song song) {
        try {
            MediaItem mediaItem = playlistManager.createMediaItem(song);
            if (mediaItem != null) {
                mediaController.addMediaItem(mediaItem);
                
                Log.d(TAG, "Added song to playlist: " + song.getTitle());
            } else {
                Log.e(TAG, "Failed to create MediaItem for song: " + song.getTitle());
            }
        } catch (Exception e) {
            Log.e(TAG, "Error adding song to playlist: " + song.getTitle(), e);
        }
    }
    
    /**
     * 添加歌曲列表到播放列表
     */
    public void addToPlaylist(@NonNull List<Song> songs) {
        try {
            List<MediaItem> mediaItems = playlistManager.createMediaItems(songs);
            if (!mediaItems.isEmpty()) {
                mediaController.addMediaItems(mediaItems);
                
                Log.d(TAG, "Added " + mediaItems.size() + " songs to playlist");
            } else {
                Log.w(TAG, "No valid media items to add to playlist");
            }
        } catch (Exception e) {
            Log.e(TAG, "Error adding songs to playlist", e);
        }
    }
    
    /**
     * 播放指定索引的歌曲
     */
    public void playAtIndex(int index) {
        try {
            if (index >= 0 && index < mediaController.getMediaItemCount()) {
                // 确保要播放的MediaItem已经准备好
                //playlistManager.preloadNearbyItems(index);
                
                mediaController.seekTo(index, 0);
                mediaController.play();
                
                Log.d(TAG, "Playing song at index: " + index);
            } else {
                Log.w(TAG, "Invalid index: " + index + ", playlist size: " + mediaController.getMediaItemCount());
            }
        } catch (Exception e) {
            Log.e(TAG, "Error playing song at index: " + index, e);
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
            
            Log.d(TAG, "Playlist cleared");
        } catch (Exception e) {
            Log.e(TAG, "Error clearing playlist", e);
        }
    }
    
    /**
     * 移除指定索引的歌曲
     */
    public void removeFromPlaylist(int index) {
        try {
            if (index >= 0 && index < mediaController.getMediaItemCount()) {
                mediaController.removeMediaItem(index);
                
                Log.d(TAG, "Removed song at index: " + index);
            } else {
                Log.w(TAG, "Invalid index for removal: " + index);
            }
        } catch (Exception e) {
            Log.e(TAG, "Error removing song at index: " + index, e);
        }
    }
    
    /**
     * 移动播放列表中的歌曲
     */
    public void movePlaylistItem(int fromIndex, int toIndex) {
        try {
            if (fromIndex >= 0 && fromIndex < mediaController.getMediaItemCount() &&
                toIndex >= 0 && toIndex < mediaController.getMediaItemCount()) {
                
                mediaController.moveMediaItem(fromIndex, toIndex);
                
                Log.d(TAG, "Moved song from index " + fromIndex + " to " + toIndex);
            } else {
                Log.w(TAG, "Invalid indices for move: " + fromIndex + " -> " + toIndex);
            }
        } catch (Exception e) {
            Log.e(TAG, "Error moving song", e);
        }
    }
    
    // ===========================================
    // 状态查询方法
    // ===========================================
    
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
     * 获取当前播放列表
     */
    @NonNull
    public List<Song> getPlaylist() {
        try {
            List<Song> playlist = new ArrayList<>();
            
            for (int i = 0; i < mediaController.getMediaItemCount(); i++) {
                MediaItem mediaItem = mediaController.getMediaItemAt(i);
                Song song = playlistManager.getSongFromMediaItem(mediaItem);
                if (song != null) {
                    playlist.add(song);
                }
            }
            
            return playlist;
        } catch (Exception e) {
            Log.e(TAG, "Error getting playlist", e);
            return Collections.emptyList();
        }
    }
    
    /**
     * 获取播放列表大小
     */
    public int getPlaylistSize() {
        try {
            return mediaController.getMediaItemCount();
        } catch (Exception e) {
            Log.e(TAG, "Error getting playlist size", e);
            return 0;
        }
    }
    
    /**
     * 获取当前播放索引
     */
    public int getCurrentIndex() {
        try {
            return mediaController.getCurrentMediaItemIndex();
        } catch (Exception e) {
            Log.e(TAG, "Error getting current index", e);
            return -1;
        }
    }
    
    /**
     * 获取指定索引的歌曲
     */
    @Nullable
    public Song getSongAtIndex(int index) {
        try {
            if (index >= 0 && index < mediaController.getMediaItemCount()) {
                MediaItem mediaItem = mediaController.getMediaItemAt(index);
                return playlistManager.getSongFromMediaItem(mediaItem);
            }
        } catch (Exception e) {
            Log.e(TAG, "Error getting song at index: " + index, e);
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
    
    // ===========================================
    // 快捷播放控制方法
    // ===========================================
    
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
     * 跳转到下一首
     */
    public void skipToNext() {
        try {
            mediaController.seekToNext();
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
     * 清理资源
     */
    public void cleanup() {
        try {
            if (playlistManager != null) {
                playlistManager.cleanup();
            }
            Log.d(TAG, "MediaControllerHelper cleaned up");
        } catch (Exception e) {
            Log.e(TAG, "Error cleaning up", e);
        }
    }
} 
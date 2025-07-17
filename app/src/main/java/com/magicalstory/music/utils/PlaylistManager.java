package com.magicalstory.music.utils;

import android.content.Context;
import android.net.Uri;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.media3.common.MediaItem;
import androidx.media3.common.MediaMetadata;
import androidx.media3.common.util.UnstableApi;

import com.magicalstory.music.model.Song;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

/**
 * 简化后的播放列表管理器
 * 移除缓存机制，直接创建MediaItem
 */
@UnstableApi
public class PlaylistManager {
    
    private static final String TAG = "PlaylistManager";
    
    private final Context context;
    private final List<Song> currentPlaylist;
    private final Handler mainHandler;
    
    public PlaylistManager(@NonNull Context context) {
        this.context = context;
        this.currentPlaylist = new ArrayList<>();
        this.mainHandler = new Handler(Looper.getMainLooper());
    }
    
    /**
     * 设置播放列表
     */
    @NonNull
    public List<MediaItem> setPlaylist(@NonNull List<Song> songs, int startIndex) {
        long startTime = System.currentTimeMillis();
        Log.d(TAG, "设置播放列表，歌曲数量: " + songs.size() + ", 起始索引: " + startIndex);
        
        // 更新播放列表
        long updatePlaylistStart = System.currentTimeMillis();
        currentPlaylist.clear();
        currentPlaylist.addAll(songs);
        long updatePlaylistEnd = System.currentTimeMillis();
        Log.d(TAG, "更新播放列表耗时: " + (updatePlaylistEnd - updatePlaylistStart) + "ms");
        
        // 创建MediaItem列表
        long createMediaItemsStart = System.currentTimeMillis();
        List<MediaItem> mediaItems = new ArrayList<>();
        int validItemCount = 0;
        int skippedItemCount = 0;
        
        for (int i = 0; i < songs.size(); i++) {
            long itemStart = System.currentTimeMillis();
            Song song = songs.get(i);
            MediaItem mediaItem = createMediaItemSync(song);
            
            if (mediaItem != null) {
                mediaItems.add(mediaItem);
                validItemCount++;
            } else {
                Log.w(TAG, "跳过无效的歌曲: " + song.getTitle());
                skippedItemCount++;
                // 需要调整索引，因为跳过了一个歌曲
                if (i < startIndex) {
                    startIndex--;
                }
            }
            
            long itemEnd = System.currentTimeMillis();
            if (itemEnd - itemStart > 5) { // 只打印超过5ms的项目
                Log.d(TAG, "创建MediaItem[" + i + "]耗时: " + (itemEnd - itemStart) + "ms, 歌曲: " + song.getTitle());
            }
        }
        
        long createMediaItemsEnd = System.currentTimeMillis();
        Log.d(TAG, "创建MediaItem列表总耗时: " + (createMediaItemsEnd - createMediaItemsStart) + "ms");
        Log.d(TAG, "有效MediaItem数量: " + validItemCount + ", 跳过数量: " + skippedItemCount);
        
        long totalTime = System.currentTimeMillis() - startTime;
        Log.d(TAG, "播放列表设置完成，MediaItem数量: " + mediaItems.size() + ", 总耗时: " + totalTime + "ms");
        
        return mediaItems;
    }
    
    /**
     * 根据歌曲列表创建MediaItem列表（向后兼容）
     */
    @NonNull
    public List<MediaItem> createMediaItems(@NonNull List<Song> songs) {
        return setPlaylist(songs, 0);
    }
    
    /**
     * 获取指定索引的MediaItem
     */
    @Nullable
    public MediaItem getMediaItemAtIndex(int index) {
        if (index < 0 || index >= currentPlaylist.size()) {
            return null;
        }
        
        Song song = currentPlaylist.get(index);
        return createMediaItemSync(song);
    }
    
    /**
     * 同步创建MediaItem
     */
    @Nullable
    public MediaItem createMediaItemSync(@NonNull Song song) {
        long startTime = System.currentTimeMillis();
        MediaItem result = createMediaItemInternal(song);
        long endTime = System.currentTimeMillis();
        
        if (endTime - startTime > 10) { // 只打印超过10ms的创建操作
            Log.d(TAG, "同步创建MediaItem耗时: " + (endTime - startTime) + "ms, 歌曲: " + song.getTitle());
        }
        
        return result;
    }
    
    /**
     * 根据单个歌曲创建MediaItem（向后兼容）
     */
    @Nullable
    public MediaItem createMediaItem(@NonNull Song song) {
        return createMediaItemSync(song);
    }
    
    /**
     * 内部创建MediaItem的方法
     */
    @Nullable
    private MediaItem createMediaItemInternal(@NonNull Song song) {
        long startTime = System.currentTimeMillis();
        Log.d(TAG, "创建MediaItem: " + song.getTitle());
        
        try {
            // 验证歌曲文件
            long fileValidationStart = System.currentTimeMillis();
            if (song.getPath() == null || song.getPath().isEmpty()) {
                Log.w(TAG, "歌曲路径为空: " + song.getTitle());
                return null;
            }
            
            File songFile = new File(song.getPath());
            if (!songFile.exists()) {
                Log.w(TAG, "歌曲文件不存在: " + song.getPath());
                return null;
            }
            long fileValidationEnd = System.currentTimeMillis();
            
            if (fileValidationEnd - fileValidationStart > 3) {
                Log.d(TAG, "文件验证耗时: " + (fileValidationEnd - fileValidationStart) + "ms, 路径: " + song.getPath());
            }
            
            // 创建媒体元数据
            long metadataStart = System.currentTimeMillis();
            MediaMetadata.Builder metadataBuilder = new MediaMetadata.Builder()
                    .setTitle(song.getTitle())
                    .setArtist(song.getArtist())
                    .setAlbumTitle(song.getAlbum())
                    .setDisplayTitle(song.getTitle())
                    .setSubtitle(song.getArtist())
                    .setDescription(song.getAlbum());
            
            if (song.getTrack() > 0) {
                metadataBuilder.setTrackNumber(song.getTrack());
            }
            
            if (song.getDuration() > 0) {
                metadataBuilder.setDurationMs(song.getDuration());
            }
            
            if (song.getYear() > 0) {
                metadataBuilder.setReleaseYear(song.getYear());
            }
            long metadataEnd = System.currentTimeMillis();
            
            if (metadataEnd - metadataStart > 3) {
                Log.d(TAG, "创建元数据耗时: " + (metadataEnd - metadataStart) + "ms, 歌曲: " + song.getTitle());
            }
            
            // 创建MediaItem
            long mediaItemStart = System.currentTimeMillis();
            MediaItem mediaItem = new MediaItem.Builder()
                    .setMediaId(String.valueOf(song.getId()))
                    .setUri(song.getPath())
                    .setMediaMetadata(metadataBuilder.build())
                    .build();
            long mediaItemEnd = System.currentTimeMillis();
            
            if (mediaItemEnd - mediaItemStart > 3) {
                Log.d(TAG, "创建MediaItem对象耗时: " + (mediaItemEnd - mediaItemStart) + "ms, 歌曲: " + song.getTitle());
            }
            
            long totalTime = System.currentTimeMillis() - startTime;
            Log.d(TAG, "MediaItem创建成功，总耗时: " + totalTime + "ms, ID: " + mediaItem.mediaId);
            
            return mediaItem;
            
        } catch (Exception e) {
            long totalTime = System.currentTimeMillis() - startTime;
            Log.e(TAG, "创建MediaItem时发生错误，耗时: " + totalTime + "ms, 歌曲: " + song.getTitle(), e);
            return null;
        }
    }
    
    /**
     * 从MediaItem获取对应的Song对象
     */
    @Nullable
    public Song getSongFromMediaItem(@NonNull MediaItem mediaItem) {
        if (mediaItem.mediaId == null) {
            return null;
        }
        
        // 尝试从数据库查找
        try {
            long songId = Long.parseLong(mediaItem.mediaId);
            Song song = org.litepal.LitePal.find(Song.class, songId);
            return song;
        } catch (NumberFormatException e) {
            Log.e(TAG, "无效的媒体ID: " + mediaItem.mediaId, e);
        }
        
        return null;
    }
    
    /**
     * 添加媒体项到播放列表（兼容性方法）
     */
    @NonNull
    public List<MediaItem> addMediaItems(@NonNull List<MediaItem> mediaItems) {
        List<MediaItem> validMediaItems = new ArrayList<>();
        
        for (MediaItem mediaItem : mediaItems) {
            if (mediaItem != null && isValidMediaItem(mediaItem)) {
                validMediaItems.add(mediaItem);
                
                // 尝试从MediaItem获取Song信息
                Song song = getSongFromMediaItem(mediaItem);
                if (song != null) {
                    currentPlaylist.add(song);
                }
            }
        }
        
        Log.d(TAG, "添加 " + validMediaItems.size() + " 个有效媒体项");
        return validMediaItems;
    }
    
    /**
     * 设置播放列表媒体项（兼容性方法）
     */
    @NonNull
    public List<MediaItem> setMediaItems(@NonNull List<MediaItem> mediaItems) {
        // 清理旧数据
        currentPlaylist.clear();
        
        List<MediaItem> validMediaItems = new ArrayList<>();
        
        for (MediaItem mediaItem : mediaItems) {
            if (mediaItem != null && isValidMediaItem(mediaItem)) {
                validMediaItems.add(mediaItem);
                
                // 尝试从MediaItem获取Song信息
                Song song = getSongFromMediaItem(mediaItem);
                if (song != null) {
                    currentPlaylist.add(song);
                }
            }
        }
        
        Log.d(TAG, "设置 " + validMediaItems.size() + " 个有效媒体项");
        return validMediaItems;
    }
    
    /**
     * 验证MediaItem是否有效
     */
    private boolean isValidMediaItem(@NonNull MediaItem mediaItem) {
        if (mediaItem.mediaId == null || mediaItem.mediaId.isEmpty()) {
            Log.w(TAG, "MediaItem mediaId为空或无效");
            return false;
        }
        
        // 检查是否有本地配置
        if (mediaItem.localConfiguration == null || mediaItem.localConfiguration.uri == null) {
            Log.w(TAG, "MediaItem缺少本地配置或URI");
            return false;
        }
        
        // 验证文件是否存在
        String path = mediaItem.localConfiguration.uri.getPath();
        if (path != null) {
            File file = new File(path);
            if (!file.exists()) {
                Log.w(TAG, "MediaItem文件不存在: " + path);
                return false;
            }
        }
        
        return true;
    }
    
    /**
     * 获取当前播放列表
     */
    @NonNull
    public List<Song> getCurrentPlaylist() {
        return new ArrayList<>(currentPlaylist);
    }
    
    /**
     * 获取播放列表大小
     */
    public int getPlaylistSize() {
        return currentPlaylist.size();
    }
    
    /**
     * 根据索引获取歌曲
     */
    @Nullable
    public Song getSongAtIndex(int index) {
        if (index >= 0 && index < currentPlaylist.size()) {
            return currentPlaylist.get(index);
        }
        return null;
    }
    
    /**
     * 获取歌曲在播放列表中的索引
     */
    public int getSongIndex(@NonNull Song song) {
        return currentPlaylist.indexOf(song);
    }
    
    /**
     * 清除播放列表
     */
    public void clearPlaylist() {
        currentPlaylist.clear();
        Log.d(TAG, "播放列表已清空");
    }
    
    /**
     * 清理资源
     */
    public void cleanup() {
        currentPlaylist.clear();
        Log.d(TAG, "PlaylistManager已清理");
    }
} 
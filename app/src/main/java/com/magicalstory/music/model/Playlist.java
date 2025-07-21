package com.magicalstory.music.model;

import org.litepal.crud.LitePalSupport;

import java.util.ArrayList;
import java.util.List;

/**
 * 播放列表数据模型
 */
public class Playlist extends LitePalSupport {
    private long id;
    private String name;           // 播放列表名称
    private String description;    // 播放列表描述
    private long createdTime;      // 创建时间
    private long updatedTime;      // 更新时间
    private int songCount;         // 歌曲数量
    private String coverPath;      // 封面路径
    private boolean isSystemPlaylist; // 是否为系统播放列表（如收藏、最近播放等）
    private int playOrder;         // 播放顺序（0=顺序播放，1=随机播放，2=单曲循环）
    
    // 播放列表类型常量
    public static final int TYPE_NORMAL = 0;     // 普通播放列表
    public static final int TYPE_FAVORITES = 1;  // 收藏播放列表
    public static final int TYPE_RECENT = 2;     // 最近播放列表
    public static final int TYPE_MOST_PLAYED = 3; // 最多播放列表

    public Playlist() {
        this.createdTime = System.currentTimeMillis();
        this.updatedTime = this.createdTime;
        this.songCount = 0;
        this.isSystemPlaylist = false;
        this.playOrder = 0;
    }

    public Playlist(String name, String description) {
        this();
        this.name = name;
        this.description = description;
    }

    // Getters and Setters
    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
        this.updatedTime = System.currentTimeMillis();
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
        this.updatedTime = System.currentTimeMillis();
    }

    public long getCreatedTime() {
        return createdTime;
    }

    public void setCreatedTime(long createdTime) {
        this.createdTime = createdTime;
    }

    public long getUpdatedTime() {
        return updatedTime;
    }

    public void setUpdatedTime(long updatedTime) {
        this.updatedTime = updatedTime;
    }

    public int getSongCount() {
        return songCount;
    }

    public void setSongCount(int songCount) {
        this.songCount = songCount;
        this.updatedTime = System.currentTimeMillis();
    }

    public String getCoverPath() {
        return coverPath;
    }

    public void setCoverPath(String coverPath) {
        this.coverPath = coverPath;
        this.updatedTime = System.currentTimeMillis();
    }

    public boolean isSystemPlaylist() {
        return isSystemPlaylist;
    }

    public void setSystemPlaylist(boolean systemPlaylist) {
        isSystemPlaylist = systemPlaylist;
    }

    public int getPlayOrder() {
        return playOrder;
    }

    public void setPlayOrder(int playOrder) {
        this.playOrder = playOrder;
        this.updatedTime = System.currentTimeMillis();
    }

    /**
     * 获取播放列表中的歌曲
     */
    public List<Song> getSongs() {
        return PlaylistSong.getPlaylistSongs(this.id);
    }

    /**
     * 添加歌曲到播放列表
     */
    public void addSong(Song song) {
        PlaylistSong playlistSong = new PlaylistSong(this.id, song.getId());
        playlistSong.save();
        this.songCount++;
        this.updatedTime = System.currentTimeMillis();
        
        // 更新歌单封面为最新添加的歌曲的封面
        updatePlaylistCover(song);
        
        this.save();
    }

    /**
     * 更新歌单封面
     * 使用最新添加的歌曲的专辑封面作为歌单封面
     */
    private void updatePlaylistCover(Song song) {
        if (song != null && song.getAlbumId() > 0) {
            // 使用专辑封面路径作为歌单封面
            this.coverPath = "content://media/external/audio/albumart/" + song.getAlbumId();
        }
    }

    /**
     * 更新歌单封面为最新歌曲的封面
     */
    public void updateCoverToLatestSong() {
        Song latestSong = PlaylistSong.getLatestSongInPlaylist(this.id);
        if (latestSong != null) {
            updatePlaylistCover(latestSong);
            this.save();
        }
    }

    /**
     * 从播放列表移除歌曲
     */
    public void removeSong(Song song) {
        PlaylistSong.deletePlaylistSong(this.id, song.getId());
        this.songCount = Math.max(0, this.songCount - 1);
        this.updatedTime = System.currentTimeMillis();
        this.save();
    }

    /**
     * 检查歌曲是否在播放列表中
     */
    public boolean containsSong(Song song) {
        return PlaylistSong.containsPlaylistSong(this.id, song.getId());
    }

    /**
     * 创建默认的系统播放列表
     */
    public static void createDefaultPlaylists() {
        // 创建收藏播放列表
        Playlist favorites = new Playlist("我的收藏", "收藏的歌曲");
        favorites.setSystemPlaylist(true);
        favorites.save();

        // 创建最近播放列表
        Playlist recent = new Playlist("最近播放", "最近播放的歌曲");
        recent.setSystemPlaylist(true);
        recent.save();

        // 创建最多播放列表
        Playlist mostPlayed = new Playlist("最多播放", "播放次数最多的歌曲");
        mostPlayed.setSystemPlaylist(true);
        mostPlayed.save();
    }
} 
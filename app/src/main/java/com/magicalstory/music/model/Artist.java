package com.magicalstory.music.model;

import org.litepal.crud.LitePalSupport;

/**
 * 艺术家数据模型
 */
public class Artist extends LitePalSupport {
    private long id;
    private String artistName;      // 艺术家名称
    private int songCount;          // 歌曲数量
    private int albumCount;         // 专辑数量
    private long artistId;          // 系统艺术家ID
    private String coverUrl;        // 艺术家封面URL
    private boolean coverFetched;   // 是否已经尝试过获取封面
    private long lastplayed;        // 最后播放时间
    private long dateAdded;         // 添加时间

    public Artist() {}

    public Artist(String artistName, int songCount, int albumCount, long artistId) {
        this.artistName = artistName;
        this.songCount = songCount;
        this.albumCount = albumCount;
        this.artistId = artistId;
        this.coverFetched = false;
    }

    // Getters and Setters
    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public String getArtistName() {
        return artistName;
    }

    public void setArtistName(String artistName) {
        this.artistName = artistName;
    }

    public int getSongCount() {
        return songCount;
    }

    public void setSongCount(int songCount) {
        this.songCount = songCount;
    }

    public int getAlbumCount() {
        return albumCount;
    }

    public void setAlbumCount(int albumCount) {
        this.albumCount = albumCount;
    }

    public long getArtistId() {
        return artistId;
    }

    public void setArtistId(long artistId) {
        this.artistId = artistId;
    }

    public String getCoverUrl() {
        return coverUrl;
    }

    public void setCoverUrl(String coverUrl) {
        this.coverUrl = coverUrl;
    }

    public boolean isCoverFetched() {
        return coverFetched;
    }

    public void setCoverFetched(boolean coverFetched) {
        this.coverFetched = coverFetched;
    }

    public long getLastplayed() {
        return lastplayed;
    }

    public void setLastplayed(long lastplayed) {
        this.lastplayed = lastplayed;
    }

    public long getDateAdded() {
        return dateAdded;
    }

    public void setDateAdded(long dateAdded) {
        this.dateAdded = dateAdded;
    }
}
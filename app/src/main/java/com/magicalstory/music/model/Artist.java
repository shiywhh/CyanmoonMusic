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

    public Artist() {}

    public Artist(String artistName, int songCount, int albumCount, long artistId) {
        this.artistName = artistName;
        this.songCount = songCount;
        this.albumCount = albumCount;
        this.artistId = artistId;
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
}
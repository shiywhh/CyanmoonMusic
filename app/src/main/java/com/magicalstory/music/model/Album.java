package com.magicalstory.music.model;

import org.litepal.crud.LitePalSupport;

/**
 * 专辑数据模型
 */
public class Album extends LitePalSupport {
    private long id;
    private String albumName;       // 专辑名称
    private String artist;          // 艺术家
    private int songCount;          // 歌曲数量
    private long albumId;           // 系统专辑ID
    private String albumArt;        // 专辑封面路径
    private int year;               // 年份
    private long firstYear;         // 首次发行年份
    private long lastYear;          // 最后发行年份
    private long lastplayed;        // 最后播放时间

    public Album() {}

    public Album(String albumName, String artist, int songCount, long albumId) {
        this.albumName = albumName;
        this.artist = artist;
        this.songCount = songCount;
        this.albumId = albumId;
    }

    // Getters and Setters
    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public String getAlbumName() {
        return albumName;
    }

    public void setAlbumName(String albumName) {
        this.albumName = albumName;
    }

    public String getArtist() {
        return artist;
    }

    public void setArtist(String artist) {
        this.artist = artist;
    }

    public int getSongCount() {
        return songCount;
    }

    public void setSongCount(int songCount) {
        this.songCount = songCount;
    }

    public long getAlbumId() {
        return albumId;
    }

    public void setAlbumId(long albumId) {
        this.albumId = albumId;
    }

    public String getAlbumArt() {
        return albumArt;
    }

    public void setAlbumArt(String albumArt) {
        this.albumArt = albumArt;
    }

    public int getYear() {
        return year;
    }

    public void setYear(int year) {
        this.year = year;
    }

    public long getFirstYear() {
        return firstYear;
    }

    public void setFirstYear(long firstYear) {
        this.firstYear = firstYear;
    }

    public long getLastYear() {
        return lastYear;
    }

    public void setLastYear(long lastYear) {
        this.lastYear = lastYear;
    }

    public long getLastplayed() {
        return lastplayed;
    }

    public void setLastplayed(long lastplayed) {
        this.lastplayed = lastplayed;
    }
}
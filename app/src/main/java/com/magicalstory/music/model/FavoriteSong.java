package com.magicalstory.music.model;

import org.litepal.crud.LitePalSupport;

import java.util.Date;

/**
 * 收藏歌曲数据模型
 * 不直接存储歌曲信息，而是存储歌曲ID映射
 */
public class FavoriteSong extends LitePalSupport {
    
    private long id;
    private long songId;            // 歌曲ID，关联到Song表
    private long addTime;           // 添加时间戳
    private int sortOrder;          // 排序字段，用于用户自定义排序
    private Date createdAt;         // 创建时间
    private Date updatedAt;         // 更新时间

    public FavoriteSong() {
        this.createdAt = new Date();
        this.updatedAt = new Date();
        this.addTime = System.currentTimeMillis();
    }

    public FavoriteSong(long songId) {
        this();
        this.songId = songId;
    }

    public FavoriteSong(long songId, int sortOrder) {
        this(songId);
        this.sortOrder = sortOrder;
    }

    // Getters and Setters
    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public long getSongId() {
        return songId;
    }

    public void setSongId(long songId) {
        this.songId = songId;
    }

    public long getAddTime() {
        return addTime;
    }

    public void setAddTime(long addTime) {
        this.addTime = addTime;
    }

    public int getSortOrder() {
        return sortOrder;
    }

    public void setSortOrder(int sortOrder) {
        this.sortOrder = sortOrder;
        this.updatedAt = new Date();
    }

    public Date getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Date createdAt) {
        this.createdAt = createdAt;
    }

    public Date getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(Date updatedAt) {
        this.updatedAt = updatedAt;
    }

    @Override
    public String toString() {
        return "FavoriteSong{" +
                "id=" + id +
                ", songId=" + songId +
                ", addTime=" + addTime +
                ", sortOrder=" + sortOrder +
                ", createdAt=" + createdAt +
                ", updatedAt=" + updatedAt +
                '}';
    }
} 
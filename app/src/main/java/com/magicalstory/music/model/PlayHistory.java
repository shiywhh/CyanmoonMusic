package com.magicalstory.music.model;

import org.litepal.LitePal;
import org.litepal.crud.LitePalSupport;

import java.util.List;

/**
 * 播放历史数据模型
 */
public class PlayHistory extends LitePalSupport {
    private long id;
    private long songId;          // 歌曲ID
    private long playTime;        // 播放时间
    private long playDuration;    // 播放时长（毫秒）
    private int playCount;        // 播放次数
    private boolean isCompleted;  // 是否播放完成
    private long lastPlayTime;    // 最后播放时间
    private int skipCount;        // 跳过次数
    private double playProgress;  // 播放进度（0-1）

    public PlayHistory() {
        this.playTime = System.currentTimeMillis();
        this.lastPlayTime = this.playTime;
        this.playCount = 1;
        this.isCompleted = false;
        this.skipCount = 0;
        this.playProgress = 0.0;
    }

    public PlayHistory(long songId) {
        this();
        this.songId = songId;
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

    public long getPlayTime() {
        return playTime;
    }

    public void setPlayTime(long playTime) {
        this.playTime = playTime;
    }

    public long getPlayDuration() {
        return playDuration;
    }

    public void setPlayDuration(long playDuration) {
        this.playDuration = playDuration;
    }

    public int getPlayCount() {
        return playCount;
    }

    public void setPlayCount(int playCount) {
        this.playCount = playCount;
    }

    public boolean isCompleted() {
        return isCompleted;
    }

    public void setCompleted(boolean completed) {
        isCompleted = completed;
    }

    public long getLastPlayTime() {
        return lastPlayTime;
    }

    public void setLastPlayTime(long lastPlayTime) {
        this.lastPlayTime = lastPlayTime;
    }

    public int getSkipCount() {
        return skipCount;
    }

    public void setSkipCount(int skipCount) {
        this.skipCount = skipCount;
    }

    public double getPlayProgress() {
        return playProgress;
    }

    public void setPlayProgress(double playProgress) {
        this.playProgress = playProgress;
    }

    /**
     * 记录播放历史
     */
    public static void recordPlay(long songId, long playDuration, boolean isCompleted, double playProgress) {
        try {
            PlayHistory existingHistory = LitePal.where("songId = ?", String.valueOf(songId))
                    .findFirst(PlayHistory.class);
            
            if (existingHistory != null) {
                // 更新现有记录
                existingHistory.setPlayCount(existingHistory.getPlayCount() + 1);
                existingHistory.setLastPlayTime(System.currentTimeMillis());
                existingHistory.setPlayDuration(playDuration);
                existingHistory.setCompleted(isCompleted);
                existingHistory.setPlayProgress(playProgress);
                existingHistory.saveThrows();
            } else {
                // 创建新记录
                PlayHistory newHistory = new PlayHistory(songId);
                newHistory.setPlayDuration(playDuration);
                newHistory.setCompleted(isCompleted);
                newHistory.setPlayProgress(playProgress);
                newHistory.saveThrows();
            }
        } catch (Exception e) {
            android.util.Log.e("PlayHistory", "Error recording play history: " + e.getMessage(), e);
        }
    }

    /**
     * 记录跳过
     */
    public static void recordSkip(long songId) {
        try {
            PlayHistory existingHistory = LitePal.where("songId = ?", String.valueOf(songId))
                    .findFirst(PlayHistory.class);
            
            if (existingHistory != null) {
                existingHistory.setSkipCount(existingHistory.getSkipCount() + 1);
                existingHistory.setLastPlayTime(System.currentTimeMillis());
                existingHistory.saveThrows();
            } else {
                PlayHistory newHistory = new PlayHistory(songId);
                newHistory.setSkipCount(1);
                newHistory.saveThrows();
            }
        } catch (Exception e) {
            android.util.Log.e("PlayHistory", "Error recording skip: " + e.getMessage(), e);
        }
    }

    /**
     * 获取歌曲播放次数
     */
    public static int getSongPlayCount(long songId) {
        PlayHistory history = LitePal.where("songId = ?", String.valueOf(songId))
                .findFirst(PlayHistory.class);
        return history != null ? history.getPlayCount() : 0;
    }

    /**
     * 获取最近播放的歌曲
     */
    public static List<PlayHistory> getRecentPlayed(int limit) {
        return LitePal.order("lastPlayTime DESC")
                .limit(limit)
                .find(PlayHistory.class);
    }

    /**
     * 获取最多播放的歌曲
     */
    public static List<PlayHistory> getMostPlayed(int limit) {
        return LitePal.order("playCount DESC")
                .limit(limit)
                .find(PlayHistory.class);
    }

    /**
     * 获取播放历史总数
     */
    public static int getTotalPlayCount() {
        List<PlayHistory> histories = LitePal.findAll(PlayHistory.class);
        int totalCount = 0;
        for (PlayHistory history : histories) {
            totalCount += history.getPlayCount();
        }
        return totalCount;
    }

    /**
     * 清除播放历史
     */
    public static void clearPlayHistory() {
        LitePal.deleteAll(PlayHistory.class);
    }

    /**
     * 删除指定歌曲的播放历史
     */
    public static void deleteSongHistory(long songId) {
        LitePal.deleteAll(PlayHistory.class, "songId = ?", String.valueOf(songId));
    }

    /**
     * 获取歌曲对象
     */
    public Song getSong() {
        return LitePal.find(Song.class, this.songId);
    }

    /**
     * 获取播放完成率
     */
    public double getCompletionRate() {
        if (playCount == 0) return 0.0;
        return isCompleted ? 1.0 : playProgress;
    }

    /**
     * 更新播放进度
     */
    public void updatePlayProgress(double progress) {
        this.playProgress = progress;
        this.lastPlayTime = System.currentTimeMillis();
        
        // 如果播放进度超过80%，认为播放完成
        if (progress > 0.8) {
            this.isCompleted = true;
        }
        
        this.save();
    }
} 
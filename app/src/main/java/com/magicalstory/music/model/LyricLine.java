package com.magicalstory.music.model;

/**
 * 歌词行数据模型
 */
public class LyricLine {
    private long startTime; // 开始时间（毫秒）
    private String content; // 歌词内容
    
    public LyricLine() {
    }
    
    public LyricLine(long startTime, String content) {
        this.startTime = startTime;
        this.content = content;
    }
    
    public long getStartTime() {
        return startTime;
    }
    
    public void setStartTime(long startTime) {
        this.startTime = startTime;
    }
    
    public String getContent() {
        return content;
    }
    
    public void setContent(String content) {
        this.content = content;
    }
    
    @Override
    public String toString() {
        return "LyricLine{" +
                "startTime=" + startTime +
                ", content='" + content + '\'' +
                '}';
    }
} 
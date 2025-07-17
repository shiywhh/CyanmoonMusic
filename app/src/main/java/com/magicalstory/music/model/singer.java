package com.magicalstory.music.model;

import org.litepal.crud.LitePalSupport;

/**
 * 歌手封面模型
 */
public class singer extends LitePalSupport {
    private long id;
    private String singerName;

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public String getSingerName() {
        return singerName;
    }

    public void setSingerName(String singerName) {
        this.singerName = singerName;
    }
}
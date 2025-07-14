package com.magicalstory.music.model;

import org.litepal.LitePal;
import org.litepal.crud.LitePalSupport;

import java.util.ArrayList;
import java.util.List;

/**
 * 播放列表与歌曲关联表
 */
public class PlaylistSong extends LitePalSupport {
    private long id;
    private long playlistId;  // 播放列表ID
    private long songId;      // 歌曲ID
    private int position;     // 在播放列表中的位置
    private long addedTime;   // 添加时间

    public PlaylistSong() {
        this.addedTime = System.currentTimeMillis();
    }

    public PlaylistSong(long playlistId, long songId) {
        this();
        this.playlistId = playlistId;
        this.songId = songId;
    }

    // Getters and Setters
    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public long getPlaylistId() {
        return playlistId;
    }

    public void setPlaylistId(long playlistId) {
        this.playlistId = playlistId;
    }

    public long getSongId() {
        return songId;
    }

    public void setSongId(long songId) {
        this.songId = songId;
    }

    public int getPosition() {
        return position;
    }

    public void setPosition(int position) {
        this.position = position;
    }

    public long getAddedTime() {
        return addedTime;
    }

    public void setAddedTime(long addedTime) {
        this.addedTime = addedTime;
    }

    /**
     * 获取播放列表中的所有歌曲
     */
    public static List<Song> getPlaylistSongs(long playlistId) {
        List<Song> songs = new ArrayList<>();
        List<PlaylistSong> playlistSongs = LitePal.where("playlistId = ?", String.valueOf(playlistId))
                .order("position ASC, addedTime ASC")
                .find(PlaylistSong.class);
        
        for (PlaylistSong playlistSong : playlistSongs) {
            Song song = LitePal.find(Song.class, playlistSong.getSongId());
            if (song != null) {
                songs.add(song);
            }
        }
        return songs;
    }

    /**
     * 检查播放列表是否包含某首歌曲
     */
    public static boolean containsPlaylistSong(long playlistId, long songId) {
        return LitePal.where("playlistId = ? AND songId = ?", 
                String.valueOf(playlistId), String.valueOf(songId))
                .count(PlaylistSong.class) > 0;
    }

    /**
     * 删除播放列表中的歌曲
     */
    public static void deletePlaylistSong(long playlistId, long songId) {
        LitePal.deleteAll(PlaylistSong.class, "playlistId = ? AND songId = ?",
                String.valueOf(playlistId), String.valueOf(songId));
    }

    /**
     * 获取播放列表中歌曲的数量
     */
    public static int getPlaylistSongCount(long playlistId) {
        return LitePal.where("playlistId = ?", String.valueOf(playlistId)).count(PlaylistSong.class);
    }

    /**
     * 删除播放列表的所有歌曲
     */
    public static void deleteAllPlaylistSongs(long playlistId) {
        LitePal.deleteAll(PlaylistSong.class, "playlistId = ?", String.valueOf(playlistId));
    }

    /**
     * 更新播放列表中歌曲的位置
     */
    public static void updateSongPosition(long playlistId, long songId, int position) {
        PlaylistSong playlistSong = LitePal.where("playlistId = ? AND songId = ?",
                String.valueOf(playlistId), String.valueOf(songId))
                .findFirst(PlaylistSong.class);
        if (playlistSong != null) {
            playlistSong.setPosition(position);
            playlistSong.save();
        }
    }
} 
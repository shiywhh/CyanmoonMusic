package com.magicalstory.music.utils.query;

import com.magicalstory.music.model.Album;
import com.magicalstory.music.model.Artist;
import com.magicalstory.music.model.Song;

import org.litepal.LitePal;

import java.util.List;

/**
 * 音乐查询工具类
 * 用于查询专辑和艺人对应的所有歌曲
 */
public class MusicQueryUtils {

    /**
     * 查询专辑中的所有歌曲
     * @param album 专辑对象
     * @return 该专辑中的所有歌曲列表
     */
    public static List<Song> getSongsByAlbum(Album album) {
        if (album == null) {
            return null;
        }
        
        // 通过专辑名称和艺术家名称查询歌曲
        return LitePal.where("album = ? AND artist = ?", album.getAlbumName(), album.getArtist())
                .find(Song.class);
    }

    /**
     * 查询艺术家的所有歌曲
     * @param artist 艺术家对象
     * @return 该艺术家的所有歌曲列表
     */
    public static List<Song> getSongsByArtist(Artist artist) {
        if (artist == null) {
            return null;
        }
        
        // 通过艺术家名称查询歌曲
        return LitePal.where("artist = ?", artist.getArtistName())
                .find(Song.class);
    }

    /**
     * 查询多个专辑中的所有歌曲
     * @param albums 专辑列表
     * @return 所有专辑中的歌曲列表
     */
    public static List<Song> getSongsByAlbums(List<Album> albums) {
        if (albums == null || albums.isEmpty()) {
            return null;
        }
        
        List<Song> allSongs = new java.util.ArrayList<>();
        
        for (Album album : albums) {
            List<Song> albumSongs = getSongsByAlbum(album);
            if (albumSongs != null) {
                allSongs.addAll(albumSongs);
            }
        }
        
        return allSongs;
    }

    /**
     * 查询多个艺术家的所有歌曲
     * @param artists 艺术家列表
     * @return 所有艺术家的歌曲列表
     */
    public static List<Song> getSongsByArtists(List<Artist> artists) {
        if (artists == null || artists.isEmpty()) {
            return null;
        }
        
        List<Song> allSongs = new java.util.ArrayList<>();
        
        for (Artist artist : artists) {
            List<Song> artistSongs = getSongsByArtist(artist);
            if (artistSongs != null) {
                allSongs.addAll(artistSongs);
            }
        }
        
        return allSongs;
    }
} 
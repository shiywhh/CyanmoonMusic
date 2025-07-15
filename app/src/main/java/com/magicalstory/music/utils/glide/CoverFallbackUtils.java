package com.magicalstory.music.utils.glide;

import android.media.MediaMetadataRetriever;
import android.text.TextUtils;

import com.magicalstory.music.model.Album;
import com.magicalstory.music.model.Artist;
import com.magicalstory.music.model.Song;
import com.magicalstory.music.utils.query.MusicQueryUtils;

import org.litepal.LitePal;

import java.util.List;

/**
 * 封面回退工具类
 * 用于处理专辑和歌手的封面回退逻辑
 */
public class CoverFallbackUtils {

    /**
     * 为专辑设置回退封面
     * 如果专辑没有封面，则从专辑内的歌曲或对应歌手中查找封面
     * @param album 专辑对象
     * @return 是否设置了新的封面
     */
    public static boolean setAlbumFallbackCover(Album album) {
        if (album == null) {
            return false;
        }

        // 如果专辑已经有封面，不需要处理
        if (!TextUtils.isEmpty(album.getAlbumArt())) {
            return false;
        }

        // 1. 首先尝试从专辑内的歌曲中提取封面
        List<Song> albumSongs = MusicQueryUtils.getSongsByAlbum(album);
        if (albumSongs != null && !albumSongs.isEmpty()) {
            for (Song song : albumSongs) {
                String songCover = extractCoverFromSong(song);
                if (!TextUtils.isEmpty(songCover)) {
                    album.setAlbumArt(songCover);
                    album.save();
                    return true;
                }
            }
        }

        // 2. 如果歌曲中没有封面，尝试从对应的歌手中获取封面
        if (!TextUtils.isEmpty(album.getArtist())) {
            Artist artist = LitePal.where("artistName = ?", album.getArtist())
                    .findFirst(Artist.class);
            if (artist != null && !TextUtils.isEmpty(artist.getCoverUrl())) {
                album.setAlbumArt(artist.getCoverUrl());
                album.save();
                return true;
            }
        }

        return false;
    }

    /**
     * 为歌手设置回退封面
     * 如果歌手没有封面，则从歌手所在的歌曲或专辑中查找封面
     * @param artist 歌手对象
     * @return 是否设置了新的封面
     */
    public static boolean setArtistFallbackCover(Artist artist) {
        if (artist == null) {
            return false;
        }

        // 如果歌手已经有封面，不需要处理
        if (!TextUtils.isEmpty(artist.getCoverUrl())) {
            return false;
        }

        // 1. 首先尝试从歌手的歌曲中提取封面
        List<Song> artistSongs = MusicQueryUtils.getSongsByArtist(artist);
        if (artistSongs != null && !artistSongs.isEmpty()) {
            for (Song song : artistSongs) {
                String songCover = extractCoverFromSong(song);
                if (!TextUtils.isEmpty(songCover)) {
                    artist.setCoverUrl(songCover);
                    artist.setCoverFetched(true);
                    artist.save();
                    return true;
                }
            }
        }

        // 2. 如果歌曲中没有封面，尝试从歌手的专辑中获取封面
        List<Album> artistAlbums = LitePal.where("artist = ?", artist.getArtistName())
                .find(Album.class);
        if (artistAlbums != null && !artistAlbums.isEmpty()) {
            for (Album album : artistAlbums) {
                if (!TextUtils.isEmpty(album.getAlbumArt())) {
                    artist.setCoverUrl(album.getAlbumArt());
                    artist.setCoverFetched(true);
                    artist.save();
                    return true;
                }
            }
        }

        return false;
    }

    /**
     * 从歌曲文件中提取封面
     * @param song 歌曲对象
     * @return 封面URI或null
     */
    private static String extractCoverFromSong(Song song) {
        if (song == null || TextUtils.isEmpty(song.getPath())) {
            return null;
        }

        MediaMetadataRetriever retriever = null;
        try {
            retriever = new MediaMetadataRetriever();
            retriever.setDataSource(song.getPath());
            
            // 获取专辑封面
            byte[] albumArt = retriever.getEmbeddedPicture();
            if (albumArt != null && albumArt.length > 0) {
                // 如果歌曲有内嵌封面，返回专辑封面URI
                if (song.getAlbumId() > 0) {
                    return "content://media/external/audio/albumart/" + song.getAlbumId();
                } else {
                    // 如果没有albumId，则返回歌曲路径
                    return song.getPath();
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            if (retriever != null) {
                try {
                    retriever.release();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }

        return null;
    }

    /**
     * 批量为专辑设置回退封面
     * @param albums 专辑列表
     * @return 成功设置封面的数量
     */
    public static int setAlbumsFallbackCover(List<Album> albums) {
        if (albums == null || albums.isEmpty()) {
            return 0;
        }

        int successCount = 0;
        for (Album album : albums) {
            if (setAlbumFallbackCover(album)) {
                successCount++;
            }
        }

        return successCount;
    }

    /**
     * 批量为歌手设置回退封面
     * @param artists 歌手列表
     * @return 成功设置封面的数量
     */
    public static int setArtistsFallbackCover(List<Artist> artists) {
        if (artists == null || artists.isEmpty()) {
            return 0;
        }

        int successCount = 0;
        for (Artist artist : artists) {
            if (setArtistFallbackCover(artist)) {
                successCount++;
            }
        }

        return successCount;
    }

    /**
     * 检查专辑封面回退设置的情况
     * @param album 专辑对象
     * @return 封面信息字符串
     */
    public static String checkAlbumCoverStatus(Album album) {
        if (album == null) {
            return "专辑对象为空";
        }

        StringBuilder status = new StringBuilder();
        status.append("专辑: ").append(album.getAlbumName()).append("\n");
        status.append("歌手: ").append(album.getArtist()).append("\n");
        
        String albumArt = album.getAlbumArt();
        if (!TextUtils.isEmpty(albumArt)) {
            if (albumArt.startsWith("/") && albumArt.contains(".")) {
                status.append("封面类型: 从歌曲文件中提取\n");
                status.append("封面路径: ").append(albumArt).append("\n");
            } else if (albumArt.startsWith("http")) {
                status.append("封面类型: 网络URL\n");
                status.append("封面URL: ").append(albumArt).append("\n");
            } else if (albumArt.startsWith("content://media/external/audio/albumart/")) {
                status.append("封面类型: 系统专辑封面URI\n");
                status.append("封面URI: ").append(albumArt).append("\n");
            } else {
                status.append("封面类型: 其他格式\n");
                status.append("封面信息: ").append(albumArt).append("\n");
            }
        } else {
            status.append("封面类型: 无封面\n");
        }

        return status.toString();
    }

    /**
     * 检查歌手封面回退设置的情况
     * @param artist 歌手对象
     * @return 封面信息字符串
     */
    public static String checkArtistCoverStatus(Artist artist) {
        if (artist == null) {
            return "歌手对象为空";
        }

        StringBuilder status = new StringBuilder();
        status.append("歌手: ").append(artist.getArtistName()).append("\n");
        status.append("已尝试获取封面: ").append(artist.isCoverFetched() ? "是" : "否").append("\n");
        
        String coverUrl = artist.getCoverUrl();
        if (!TextUtils.isEmpty(coverUrl)) {
            if (coverUrl.startsWith("/") && coverUrl.contains(".")) {
                status.append("封面类型: 从歌曲文件中提取\n");
                status.append("封面路径: ").append(coverUrl).append("\n");
            } else if (coverUrl.startsWith("http")) {
                status.append("封面类型: 网络URL\n");
                status.append("封面URL: ").append(coverUrl).append("\n");
            } else {
                status.append("封面类型: 其他\n");
                status.append("封面信息: ").append(coverUrl).append("\n");
            }
        } else {
            status.append("封面类型: 无封面\n");
        }

        return status.toString();
    }
} 
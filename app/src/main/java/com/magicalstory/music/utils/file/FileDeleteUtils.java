package com.magicalstory.music.utils.file;

import android.app.Activity;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.provider.MediaStore;
import android.util.Log;
import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;
import androidx.media3.common.util.UnstableApi;

import com.magicalstory.music.model.Album;
import com.magicalstory.music.model.Artist;
import com.magicalstory.music.player.MediaControllerHelper;
import com.magicalstory.music.model.Song;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

/**
 * 文件删除工具类
 * 统一处理音乐文件的删除操作，支持Android 11+的MediaStore.createDeleteRequest
 * 参考RetroMusic的DeleteSongsDialog实现
 */
@UnstableApi
public class FileDeleteUtils {
    private static final String TAG = "FileDeleteUtils";
    public static final int DELETE_REQUEST_CODE = 1001;
    
    // 广播Action常量
    public static final String ACTION_REFRESH_MUSIC_LIST = "com.magicalstory.music.REFRESH_MUSIC_LIST";
    public static final String ACTION_DELETE_SONGS = "com.magicalstory.music.DELETE_SONGS";
    public static final String ACTION_DELETE_ALBUMS = "com.magicalstory.music.DELETE_ALBUMS";
    public static final String ACTION_DELETE_ARTISTS = "com.magicalstory.music.DELETE_ARTISTS";
    
    // 广播Extra常量
    public static final String EXTRA_DELETED_SONG_IDS = "deleted_song_ids";
    public static final String EXTRA_DELETED_ALBUM_IDS = "deleted_album_ids";
    public static final String EXTRA_DELETED_ARTIST_IDS = "deleted_artist_ids";

    /**
     * 删除歌曲文件（Android 11+使用MediaStore.createDeleteRequest）
     * 
     * @param fragment Fragment实例
     * @param songs 要删除的歌曲列表
     * @param deleteCallback 删除结果回调
     */
    public static void deleteSongsWithMediaStore(@NonNull Fragment fragment, 
                                               @NonNull List<Song> songs, 
                                               @NonNull DeleteCallback deleteCallback) {
        if (songs.isEmpty()) {
            deleteCallback.onDeleteFailed("没有歌曲需要删除");
            return;
        }

        try {
            // 构建需要删除的URI列表
            List<Uri> urisToDelete = new ArrayList<>();
            for (Song song : songs) {
                Uri uri = getSongFileUri(song);
                if (uri != null) {
                    urisToDelete.add(uri);
                }
            }

            if (urisToDelete.isEmpty()) {
                deleteCallback.onDeleteFailed("无法构建文件URI");
                return;
            }

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                // Android 11及以上版本使用MediaStore.createDeleteRequest()
                try {
                    android.app.PendingIntent deleteIntent = MediaStore.createDeleteRequest(
                            fragment.requireContext().getContentResolver(),
                            urisToDelete
                    );

                    // 启动删除请求
                    fragment.startIntentSenderForResult(
                            deleteIntent.getIntentSender(),
                            DELETE_REQUEST_CODE,
                            null,
                            0,
                            0,
                            0,
                            null
                    );

                } catch (Exception e) {
                    Log.e(TAG, "创建删除请求失败", e);
                    deleteCallback.onDeleteFailed("创建删除请求失败: " + e.getMessage());
                }
            } else {
                // Android 10及以下版本使用传统方式删除
                deleteSongsLegacy(fragment.requireContext(), songs, deleteCallback);
            }

        } catch (Exception e) {
            Log.e(TAG, "删除歌曲失败", e);
            deleteCallback.onDeleteFailed("删除失败: " + e.getMessage());
        }
    }

    /**
     * 处理删除结果（在Fragment的onActivityResult中调用）
     * 
     * @param fragment Fragment实例
     * @param requestCode 请求代码
     * @param resultCode 结果代码
     * @param data 返回数据
     * @param songsToDelete 要删除的歌曲列表
     * @param deleteCallback 删除结果回调
     */
    public static void handleDeleteResult(@NonNull Fragment fragment,
                                        int requestCode,
                                        int resultCode,
                                        Intent data,
                                        @NonNull List<Song> songsToDelete,
                                        @NonNull DeleteCallback deleteCallback) {
        if (requestCode == DELETE_REQUEST_CODE) {
            if (resultCode == Activity.RESULT_OK) {
                // 用户同意删除，从数据库中移除记录
                deleteSongsFromDatabase(songsToDelete);
                
                // 通知MediaControllerHelper刷新播放列表
                notifyMediaControllerHelperAfterDeletion(songsToDelete);
                
                // 发送广播通知其他组件刷新
                sendRefreshBroadcast(fragment.requireContext());
                
                // 发送删除歌曲广播
                sendDeleteSongsBroadcast(fragment.requireContext(), songsToDelete);
                
                // 调用成功回调
                deleteCallback.onDeleteSuccess(songsToDelete);
                
                Log.d(TAG, "删除成功，删除歌曲数量: " + songsToDelete.size());
            } else {
                // 用户取消删除
                deleteCallback.onDeleteFailed("用户取消删除");
            }
        }
    }

    /**
     * 传统方式删除文件（Android 10及以下版本）
     */
    private static void deleteSongsLegacy(@NonNull Context context, 
                                        @NonNull List<Song> songs, 
                                        @NonNull DeleteCallback deleteCallback) {
        ContentResolver resolver = context.getContentResolver();
        int deletedCount = 0;
        List<Song> successfullyDeletedSongs = new ArrayList<>();

        for (Song song : songs) {
            try {
                Uri uri = getSongFileUri(song);
                if (uri != null) {
                    int count = resolver.delete(uri, null, null);
                    if (count > 0) {
                        deletedCount++;
                        successfullyDeletedSongs.add(song);
                    }
                }
            } catch (SecurityException e) {
                Log.e(TAG, "删除文件权限不足: " + song.getTitle(), e);
            } catch (Exception e) {
                Log.e(TAG, "删除文件失败: " + song.getTitle(), e);
            }
        }

        if (deletedCount > 0) {
            // 从数据库中移除记录
            deleteSongsFromDatabase(successfullyDeletedSongs);
            
            // 通知MediaControllerHelper刷新播放列表
            notifyMediaControllerHelperAfterDeletion(successfullyDeletedSongs);
            
            // 发送广播通知其他组件刷新
            sendRefreshBroadcast(context);
            
            // 发送删除歌曲广播
            sendDeleteSongsBroadcast(context, successfullyDeletedSongs);
            
            deleteCallback.onDeleteSuccess(successfullyDeletedSongs);
        } else {
            deleteCallback.onDeleteFailed("没有文件被成功删除");
        }
    }

    /**
     * 获取歌曲文件的URI
     * 对于MediaStore删除请求，必须使用MediaStore ID构建URI
     */
    private static Uri getSongFileUri(@NonNull Song song) {
        try {
            // 对于MediaStore.createDeleteRequest，必须使用MediaStore ID构建URI
            // 不能使用文件路径构建的URI，因为MediaStore需要特定的ID引用
            if (song.getMediaStoreId() > 0) {
                return android.content.ContentUris.withAppendedId(
                        MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
                        song.getMediaStoreId()
                );
            }

            return null;
        } catch (Exception e) {
            Log.e(TAG, "构建歌曲URI失败: " + song.getTitle(), e);
            return null;
        }
    }

    /**
     * 从数据库中删除歌曲记录
     */
    public static void deleteSongsFromDatabase(@NonNull List<Song> songs) {
        try {
            for (Song song : songs) {
                // 删除歌曲记录
                org.litepal.LitePal.delete(Song.class, song.getId());
                
                // 删除相关的收藏记录
                org.litepal.LitePal.deleteAll(
                        com.magicalstory.music.model.FavoriteSong.class,
                        "songId = ?", 
                        String.valueOf(song.getId())
                );
                
                // 删除相关的播放历史记录
                org.litepal.LitePal.deleteAll(
                        com.magicalstory.music.model.PlayHistory.class,
                        "songId = ?", 
                        String.valueOf(song.getId())
                );
            }
            Log.d(TAG, "从数据库删除歌曲记录成功，删除数量: " + songs.size());
            
            // 删除完成后，刷新专辑和歌手的歌曲数量
            refreshAlbumAndArtistSongCounts(songs);
            
        } catch (Exception e) {
            Log.e(TAG, "从数据库删除歌曲记录失败", e);
        }
    }

    /**
     * 通知MediaControllerHelper刷新播放列表（删除歌曲后）
     * @param deletedSongs 已删除的歌曲列表
     */
    private static void notifyMediaControllerHelperAfterDeletion(List<Song> deletedSongs) {
        if (deletedSongs == null || deletedSongs.isEmpty()) {
            return;
        }
        try {
            // 获取MediaControllerHelper实例
            MediaControllerHelper controllerHelper = MediaControllerHelper.getInstance();
            if (controllerHelper != null) {
                // 提取已删除歌曲的ID列表
                List<Long> deletedSongIds = new ArrayList<>();
                for (Song song : deletedSongs) {
                    deletedSongIds.add(song.getId());
                }

                // 调用MediaControllerHelper的刷新方法
                controllerHelper.refreshPlaylistAfterDeviceDeletion(deletedSongIds);
                
                Log.d(TAG, "已通知MediaControllerHelper刷新播放列表，删除歌曲数量: " + deletedSongs.size());
            } else {
                Log.w(TAG, "MediaControllerHelper未初始化，无法刷新播放列表");
            }
        } catch (Exception e) {
            Log.e(TAG, "通知MediaControllerHelper刷新播放列表时发生错误: " + e.getMessage(), e);
        }
    }

    /**
     * 发送刷新广播
     * @param context 上下文
     */
    private static void sendRefreshBroadcast(Context context) {
        try {
            Intent refreshIntent = new Intent(ACTION_REFRESH_MUSIC_LIST);
            LocalBroadcastManager.getInstance(context).sendBroadcast(refreshIntent);
            Log.d(TAG, "已发送刷新音乐列表广播");
        } catch (Exception e) {
            Log.e(TAG, "发送刷新广播失败: " + e.getMessage(), e);
        }
    }

    /**
     * 发送删除歌曲广播
     * @param context 上下文
     * @param deletedSongs 已删除的歌曲列表
     */
    private static void sendDeleteSongsBroadcast(Context context, List<Song> deletedSongs) {
        try {
            Intent deleteIntent = new Intent(ACTION_DELETE_SONGS);
            List<Long> deletedSongIds = new ArrayList<>();
            for (Song song : deletedSongs) {
                deletedSongIds.add(song.getId());
            }
            deleteIntent.putExtra(EXTRA_DELETED_SONG_IDS, new ArrayList<>(deletedSongIds));
            LocalBroadcastManager.getInstance(context).sendBroadcast(deleteIntent);
            Log.d(TAG, "已发送删除歌曲广播，删除歌曲数量: " + deletedSongs.size());
        } catch (Exception e) {
            Log.e(TAG, "发送删除歌曲广播失败: " + e.getMessage(), e);
        }
    }

    /**
     * 发送删除专辑广播
     * @param context 上下文
     * @param deletedAlbums 已删除的专辑列表
     */
    private static void sendDeleteAlbumsBroadcast(Context context, List<Album> deletedAlbums) {
        try {
            Intent deleteIntent = new Intent(ACTION_DELETE_ALBUMS);
            List<Long> deletedAlbumIds = new ArrayList<>();
            for (Album album : deletedAlbums) {
                deletedAlbumIds.add(album.getId());
            }
            deleteIntent.putExtra(EXTRA_DELETED_ALBUM_IDS, new ArrayList<>(deletedAlbumIds));
            LocalBroadcastManager.getInstance(context).sendBroadcast(deleteIntent);
            Log.d(TAG, "已发送删除专辑广播，删除专辑数量: " + deletedAlbums.size());
        } catch (Exception e) {
            Log.e(TAG, "发送删除专辑广播失败: " + e.getMessage(), e);
        }
    }

    /**
     * 发送删除艺术家广播
     * @param context 上下文
     * @param deletedArtists 已删除的艺术家列表
     */
    private static void sendDeleteArtistsBroadcast(Context context, List<Artist> deletedArtists) {
        try {
            Intent deleteIntent = new Intent(ACTION_DELETE_ARTISTS);
            List<Long> deletedArtistIds = new ArrayList<>();
            for (Artist artist : deletedArtists) {
                deletedArtistIds.add(artist.getId());
            }
            deleteIntent.putExtra(EXTRA_DELETED_ARTIST_IDS, new ArrayList<>(deletedArtistIds));
            LocalBroadcastManager.getInstance(context).sendBroadcast(deleteIntent);
            Log.d(TAG, "已发送删除艺术家广播，删除艺术家数量: " + deletedArtists.size());
        } catch (Exception e) {
            Log.e(TAG, "发送删除艺术家广播失败: " + e.getMessage(), e);
        }
    }

    /**
     * 刷新专辑和歌手的歌曲数量
     * 在删除歌曲后调用，用于更新相关专辑和歌手的歌曲数量统计
     * 
     * @param deletedSongs 已删除的歌曲列表
     */
    public static void refreshAlbumAndArtistSongCounts(@NonNull List<Song> deletedSongs) {
        if (deletedSongs == null || deletedSongs.isEmpty()) {
            Log.d(TAG, "没有删除的歌曲，跳过刷新专辑和歌手歌曲数量");
            return;
        }

        // 在新线程中执行数据库操作
        new Thread(() -> {
            try {
                Log.d(TAG, "开始刷新专辑和歌手歌曲数量，删除歌曲数量: " + deletedSongs.size());
                
                // 收集需要更新的专辑和歌手信息
                java.util.Set<String> albumKeys = new java.util.HashSet<>();
                java.util.Set<String> artistNames = new java.util.HashSet<>();
                
                for (Song song : deletedSongs) {
                    if (song.getAlbum() != null && song.getArtist() != null) {
                        // 专辑键：专辑名_艺术家名
                        String albumKey = song.getAlbum() + "_" + song.getArtist();
                        albumKeys.add(albumKey);
                    }
                    
                    if (song.getArtist() != null) {
                        artistNames.add(song.getArtist());
                    }
                }

                // 更新专辑歌曲数量
                refreshAlbumSongCounts(albumKeys);
                
                // 更新歌手歌曲数量
                refreshArtistSongCounts(artistNames);
                
                Log.d(TAG, "专辑和歌手歌曲数量刷新完成");
                
            } catch (Exception e) {
                Log.e(TAG, "刷新专辑和歌手歌曲数量时发生错误: " + e.getMessage(), e);
            }
        }).start();
    }

    /**
     * 刷新专辑歌曲数量
     * 
     * @param albumKeys 需要更新的专辑键集合（专辑名_艺术家名）
     */
    private static void refreshAlbumSongCounts(java.util.Set<String> albumKeys) {
        if (albumKeys.isEmpty()) {
            return;
        }

        try {
            for (String albumKey : albumKeys) {
                String[] parts = albumKey.split("_", 2);
                if (parts.length == 2) {
                    String albumName = parts[0];
                    String artistName = parts[1];
                    
                    // 查询该专辑的歌曲数量
                    int songCount = org.litepal.LitePal.where(
                            "album = ? AND artist = ?", 
                            albumName, 
                            artistName
                    ).count(Song.class);
                    
                    // 更新专辑记录
                    List<com.magicalstory.music.model.Album> albums = org.litepal.LitePal.where(
                            "albumName = ? AND artist = ?", 
                            albumName, 
                            artistName
                    ).find(com.magicalstory.music.model.Album.class);
                    
                    for (com.magicalstory.music.model.Album album : albums) {
                        album.setSongCount(songCount);
                        album.save();
                    }
                    
                    Log.d(TAG, "更新专辑歌曲数量: " + albumName + " - " + artistName + " = " + songCount);
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "刷新专辑歌曲数量时发生错误: " + e.getMessage(), e);
        }
    }

    /**
     * 刷新歌手歌曲数量
     * 
     * @param artistNames 需要更新的歌手名称集合
     */
    private static void refreshArtistSongCounts(java.util.Set<String> artistNames) {
        if (artistNames.isEmpty()) {
            return;
        }

        try {
            for (String artistName : artistNames) {
                // 查询该歌手的歌曲数量
                int songCount = org.litepal.LitePal.where(
                        "artist = ?", 
                        artistName
                ).count(Song.class);
                
                // 查询该歌手的专辑数量（去重）
                List<Song> artistSongs = org.litepal.LitePal.where(
                        "artist = ?", 
                        artistName
                ).find(Song.class);
                
                java.util.Set<String> uniqueAlbums = new java.util.HashSet<>();
                for (Song song : artistSongs) {
                    if (song.getAlbum() != null) {
                        uniqueAlbums.add(song.getAlbum());
                    }
                }
                int albumCount = uniqueAlbums.size();
                
                // 更新歌手记录
                List<com.magicalstory.music.model.Artist> artists = org.litepal.LitePal.where(
                        "artistName = ?", 
                        artistName
                ).find(com.magicalstory.music.model.Artist.class);
                
                for (com.magicalstory.music.model.Artist artist : artists) {
                    artist.setSongCount(songCount);
                    artist.setAlbumCount(albumCount);
                    artist.save();
                }
                
                Log.d(TAG, "更新歌手歌曲数量: " + artistName + " = " + songCount + " 首歌曲, " + albumCount + " 张专辑");
            }
        } catch (Exception e) {
            Log.e(TAG, "刷新歌手歌曲数量时发生错误: " + e.getMessage(), e);
        }
    }

    /**
     * 删除回调接口
     */
    public interface DeleteCallback {
        /**
         * 删除成功
         * @param deletedSongs 已删除的歌曲列表
         */
        void onDeleteSuccess(List<Song> deletedSongs);

        /**
         * 删除失败
         * @param errorMessage 错误信息
         */
        void onDeleteFailed(String errorMessage);
    }

    /**
     * 删除专辑记录（当专辑没有歌曲时）
     * @param context 上下文
     * @param albums 要检查的专辑列表
     */
    public static void deleteEmptyAlbums(@NonNull Context context, @NonNull List<com.magicalstory.music.model.Album> albums) {
        if (albums == null || albums.isEmpty()) {
            return;
        }
        
        List<com.magicalstory.music.model.Album> deletedAlbums = new ArrayList<>();
        
        try {
            for (com.magicalstory.music.model.Album album : albums) {
                // 检查专辑是否还有歌曲
                List<com.magicalstory.music.model.Song> remainingSongs = 
                    com.magicalstory.music.utils.query.MusicQueryUtils.getSongsByAlbum(album);
                
                // 如果没有歌曲，删除专辑记录
                if (remainingSongs == null || remainingSongs.isEmpty()) {
                    org.litepal.LitePal.delete(com.magicalstory.music.model.Album.class, album.getId());
                    deletedAlbums.add(album);
                    Log.d(TAG, "删除空专辑: " + album.getAlbumName());
                }
            }
            
            // 如果有删除的专辑，发送广播
            if (!deletedAlbums.isEmpty()) {
                sendDeleteAlbumsBroadcast(context, deletedAlbums);
            }
        } catch (Exception e) {
            Log.e(TAG, "删除空专辑失败", e);
        }
    }

    /**
     * 删除艺术家记录（当艺术家没有歌曲时）
     * @param context 上下文
     * @param artists 要检查的艺术家列表
     */
    public static void deleteEmptyArtists(@NonNull Context context, @NonNull List<com.magicalstory.music.model.Artist> artists) {
        if (artists == null || artists.isEmpty()) {
            return;
        }
        
        List<com.magicalstory.music.model.Artist> deletedArtists = new ArrayList<>();
        
        try {
            for (com.magicalstory.music.model.Artist artist : artists) {
                // 检查艺术家是否还有歌曲
                List<com.magicalstory.music.model.Song> remainingSongs = 
                    com.magicalstory.music.utils.query.MusicQueryUtils.getSongsByArtist(artist);
                
                // 如果没有歌曲，删除艺术家记录
                if (remainingSongs == null || remainingSongs.isEmpty()) {
                    org.litepal.LitePal.delete(com.magicalstory.music.model.Artist.class, artist.getId());
                    deletedArtists.add(artist);
                    Log.d(TAG, "删除空艺术家: " + artist.getArtistName());
                }
            }
            
            // 如果有删除的艺术家，发送广播
            if (!deletedArtists.isEmpty()) {
                sendDeleteArtistsBroadcast(context, deletedArtists);
            }
        } catch (Exception e) {
            Log.e(TAG, "删除空艺术家失败", e);
        }
    }

    /**
     * 删除专辑记录（强制删除，不管是否还有歌曲）
     * @param context 上下文
     * @param albums 要删除的专辑列表
     */
    public static void deleteAlbums(@NonNull Context context, @NonNull List<com.magicalstory.music.model.Album> albums) {
        if (albums == null || albums.isEmpty()) {
            return;
        }
        
        try {
            for (com.magicalstory.music.model.Album album : albums) {
                org.litepal.LitePal.delete(com.magicalstory.music.model.Album.class, album.getId());
                Log.d(TAG, "删除专辑: " + album.getAlbumName());
            }
            
            // 发送删除专辑广播
            sendDeleteAlbumsBroadcast(context, albums);
        } catch (Exception e) {
            Log.e(TAG, "删除专辑失败", e);
        }
    }

    /**
     * 删除艺术家记录（强制删除，不管是否还有歌曲）
     * @param context 上下文
     * @param artists 要删除的艺术家列表
     */
    public static void deleteArtists(@NonNull Context context, @NonNull List<com.magicalstory.music.model.Artist> artists) {
        if (artists == null || artists.isEmpty()) {
            return;
        }
        
        try {
            for (com.magicalstory.music.model.Artist artist : artists) {
                org.litepal.LitePal.delete(com.magicalstory.music.model.Artist.class, artist.getId());
                Log.d(TAG, "删除艺术家: " + artist.getArtistName());
            }
            
            // 发送删除艺术家广播
            sendDeleteArtistsBroadcast(context, artists);
        } catch (Exception e) {
            Log.e(TAG, "删除艺术家失败", e);
        }
    }

} 
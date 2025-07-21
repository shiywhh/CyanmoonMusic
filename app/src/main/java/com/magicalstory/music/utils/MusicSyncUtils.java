package com.magicalstory.music.utils;

import android.content.ContentResolver;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.provider.MediaStore;
import android.text.TextUtils;
import android.util.Log;

import com.magicalstory.music.model.Album;
import com.magicalstory.music.model.Artist;
import com.magicalstory.music.model.Song;

import org.litepal.LitePal;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 音乐同步工具类
 * 实现增量同步功能，包括新增、删除、更新音乐文件
 */
public class MusicSyncUtils {
    private static final String TAG = "MusicSyncUtils";
    
    // 最小歌曲时长（毫秒），小于此时长的歌曲将被过滤
    private static final long MIN_SONG_DURATION = 60000; // 60秒

    /**
     * 同步结果统计
     */
    public static class SyncResult {
        public int addedSongs = 0;      // 新增歌曲数量
        public int deletedSongs = 0;    // 删除歌曲数量
        public int updatedSongs = 0;    // 更新歌曲数量
        public int addedAlbums = 0;     // 新增专辑数量
        public int deletedAlbums = 0;   // 删除专辑数量
        public int addedArtists = 0;    // 新增艺术家数量
        public int deletedArtists = 0;  // 删除艺术家数量
        public int updatedAlbums = 0;   // 更新专辑数量
        public int updatedArtists = 0;  // 更新艺术家数量
        
        @Override
        public String toString() {
            return String.format("同步完成 - 歌曲: +%d/-%d/更新%d, 专辑: +%d/-%d/更新%d, 艺术家: +%d/-%d/更新%d",
                    addedSongs, deletedSongs, updatedSongs,
                    addedAlbums, deletedAlbums, updatedAlbums,
                    addedArtists, deletedArtists, updatedArtists);
        }
    }

   public interface listener{
        void onSyncComplete(SyncResult result);
    }
    listener listener;

    /**
     * 开始音乐同步
     *
     * @param context 上下文
     */
    public static void syncMusicFiles(Context context, listener listener) {
        Log.d(TAG, "开始音乐文件同步");
        SyncResult result = new SyncResult();
        
        try {
            // 1. 获取手机中的音乐文件
            List<Song> deviceSongs = getDeviceMusicFiles(context);
            Log.d(TAG, "设备中发现 " + deviceSongs.size() + " 个音乐文件");
            
            // 2. 获取数据库中的音乐文件
            List<Song> dbSongs = LitePal.findAll(Song.class);
            Log.d(TAG, "数据库中有 " + dbSongs.size() + " 个音乐文件");
            
            // 3. 创建路径映射，用于快速查找
            Map<String, Song> deviceSongMap = new HashMap<>();
            Map<String, Song> dbSongMap = new HashMap<>();
            
            for (Song song : deviceSongs) {
                deviceSongMap.put(song.getPath(), song);
            }
            
            for (Song song : dbSongs) {
                dbSongMap.put(song.getPath(), song);
            }
            
            // 4. 处理新增和更新的歌曲
            List<Song> songsToAdd = new ArrayList<>();
            List<Song> songsToUpdate = new ArrayList<>();
            
            for (Song deviceSong : deviceSongs) {
                Song dbSong = dbSongMap.get(deviceSong.getPath());
                
                if (dbSong == null) {
                    // 新增歌曲
                    songsToAdd.add(deviceSong);
                    result.addedSongs++;
                    Log.d(TAG, "新增歌曲: " + deviceSong.getTitle() + " - " + deviceSong.getArtist());
                } else {
                    // 检查是否需要更新（根据修改时间）
                    if (deviceSong.getDateModified() > dbSong.getDateModified()) {
                        // 更新歌曲信息
                        updateSongFromDevice(dbSong, deviceSong);
                        songsToUpdate.add(dbSong);
                        result.updatedSongs++;
                        Log.d(TAG, "更新歌曲: " + deviceSong.getTitle() + " - " + deviceSong.getArtist());
                    }
                }
            }
            
            // 5. 处理删除的歌曲
            List<Song> songsToDelete = new ArrayList<>();
            for (Song dbSong : dbSongs) {
                if (!deviceSongMap.containsKey(dbSong.getPath())) {
                    songsToDelete.add(dbSong);
                    result.deletedSongs++;
                    Log.d(TAG, "删除歌曲: " + dbSong.getTitle() + " - " + dbSong.getArtist());
                }
            }
            
            // 6. 保存新增歌曲
            if (!songsToAdd.isEmpty()) {
                LitePal.saveAll(songsToAdd);
                Log.d(TAG, "保存了 " + songsToAdd.size() + " 首新歌曲");
            }
            
            // 7. 更新修改的歌曲
            if (!songsToUpdate.isEmpty()) {
                for (Song song : songsToUpdate) {
                    song.save();
                }
                Log.d(TAG, "更新了 " + songsToUpdate.size() + " 首歌曲");
            }
            
            // 8. 删除不存在的歌曲
            if (!songsToDelete.isEmpty()) {
                for (Song song : songsToDelete) {
                    song.delete();
                }
                Log.d(TAG, "删除了 " + songsToDelete.size() + " 首歌曲");
            }
            
            // 9. 同步专辑信息
            syncAlbums(result);
            
            // 10. 同步艺术家信息
            syncArtists(result);

            listener.onSyncComplete(result);

            Log.d(TAG, result.toString());
            
        } catch (Exception e) {
            Log.e(TAG, "音乐同步过程中出错", e);
        }

    }
    
    /**
     * 获取设备中的音乐文件
     */
    private static List<Song> getDeviceMusicFiles(Context context) {
        List<Song> songs = new ArrayList<>();
        ContentResolver contentResolver = context.getContentResolver();
        Uri uri = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI;
        
        String[] projection = {
                MediaStore.Audio.Media._ID,
                MediaStore.Audio.Media.TITLE,
                MediaStore.Audio.Media.ARTIST,
                MediaStore.Audio.Media.ALBUM,
                MediaStore.Audio.Media.DATA,
                MediaStore.Audio.Media.DURATION,
                MediaStore.Audio.Media.SIZE,
                MediaStore.Audio.Media.DISPLAY_NAME,
                MediaStore.Audio.Media.ALBUM_ID,
                MediaStore.Audio.Media.ARTIST_ID,
                MediaStore.Audio.Media.DATE_ADDED,
                MediaStore.Audio.Media.DATE_MODIFIED,
                MediaStore.Audio.Media.MIME_TYPE,
                MediaStore.Audio.Media.TRACK,
                MediaStore.Audio.Media.YEAR
        };
        
        String selection = MediaStore.Audio.Media.IS_MUSIC + " = 1 AND " +
                MediaStore.Audio.Media.DURATION + " > " + MIN_SONG_DURATION;
        
        String sortOrder = MediaStore.Audio.Media.TITLE + " ASC";
        
        Cursor cursor = contentResolver.query(uri, projection, selection, null, sortOrder);
        
        if (cursor != null) {
            try {
                while (cursor.moveToNext()) {
                    Song song = createSongFromCursor(cursor);
                    if (song != null && !TextUtils.isEmpty(song.getPath())) {
                        // 验证文件是否存在
                        File file = new File(song.getPath());
                        if (file.exists()) {
                            songs.add(song);
                        }
                    }
                }
            } finally {
                cursor.close();
            }
        }
        
        return songs;
    }
    
    /**
     * 从Cursor创建Song对象
     */
    private static Song createSongFromCursor(Cursor cursor) {
        try {
            Song song = new Song();
            
            // 保存MediaStore ID，用于删除文件
            song.setMediaStoreId(cursor.getLong(cursor.getColumnIndexOrThrow(MediaStore.Audio.Media._ID)));
            
            // 获取并处理标题，将<unknown>替换为unknown
            String title = cursor.getString(cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.TITLE));
            song.setTitle("<unknown>".equals(title) ? "unknown" : title);
            
            // 获取并处理艺术家，将<unknown>替换为unknown
            String artist = cursor.getString(cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ARTIST));
            song.setArtist("<unknown>".equals(artist) ? "unknown" : artist);
            
            // 获取并处理专辑，将<unknown>替换为unknown
            String album = cursor.getString(cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ALBUM));
            song.setAlbum("<unknown>".equals(album) ? "unknown" : album);
            
            song.setPath(cursor.getString(cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DATA)));
            song.setDuration(cursor.getLong(cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DURATION)));
            song.setSize(cursor.getLong(cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.SIZE)));
            song.setDisplayName(cursor.getString(cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DISPLAY_NAME)));
            song.setAlbumId(cursor.getLong(cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ALBUM_ID)));
            song.setArtistId(cursor.getLong(cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ARTIST_ID)));
            song.setDateAdded(cursor.getLong(cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DATE_ADDED)));
            song.setDateModified(cursor.getLong(cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DATE_MODIFIED)));
            song.setMimeType(cursor.getString(cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.MIME_TYPE)));
            song.setTrack(cursor.getInt(cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.TRACK)));
            song.setYear(cursor.getInt(cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.YEAR)));
            
            return song;
        } catch (Exception e) {
            Log.e(TAG, "创建Song对象时出错", e);
            return null;
        }
    }
    
    /**
     * 从设备歌曲更新数据库歌曲信息
     */
    private static void updateSongFromDevice(Song dbSong, Song deviceSong) {
        dbSong.setTitle(deviceSong.getTitle());
        dbSong.setArtist(deviceSong.getArtist());
        dbSong.setAlbum(deviceSong.getAlbum());
        dbSong.setDuration(deviceSong.getDuration());
        dbSong.setSize(deviceSong.getSize());
        dbSong.setDisplayName(deviceSong.getDisplayName());
        dbSong.setAlbumId(deviceSong.getAlbumId());
        dbSong.setArtistId(deviceSong.getArtistId());
        dbSong.setDateAdded(deviceSong.getDateAdded());
        dbSong.setDateModified(deviceSong.getDateModified());
        dbSong.setMimeType(deviceSong.getMimeType());
        dbSong.setTrack(deviceSong.getTrack());
        dbSong.setYear(deviceSong.getYear());
        dbSong.setMediaStoreId(deviceSong.getMediaStoreId());
    }
    
    /**
     * 同步专辑信息
     */
    private static void syncAlbums(SyncResult result) {
        Log.d(TAG, "开始同步专辑信息");
        
        // 获取所有歌曲
        List<Song> allSongs = LitePal.findAll(Song.class);
        
        // 按专辑分组统计
        Map<String, List<Song>> albumGroups = new HashMap<>();
        for (Song song : allSongs) {
            String albumKey = song.getAlbum() + "_" + song.getArtist();
            if (!albumGroups.containsKey(albumKey)) {
                albumGroups.put(albumKey, new ArrayList<>());
            }
            albumGroups.get(albumKey).add(song);
        }
        
        // 获取现有专辑
        List<Album> existingAlbums = LitePal.findAll(Album.class);
        Map<String, Album> existingAlbumMap = new HashMap<>();
        for (Album album : existingAlbums) {
            String key = album.getAlbumName() + "_" + album.getArtist();
            existingAlbumMap.put(key, album);
        }
        
        // 处理新增和更新的专辑
        for (Map.Entry<String, List<Song>> entry : albumGroups.entrySet()) {
            String albumKey = entry.getKey();
            List<Song> songs = entry.getValue();
            
            if (songs.isEmpty()) continue;
            
            Song firstSong = songs.get(0);
            Album existingAlbum = existingAlbumMap.get(albumKey);
            
            if (existingAlbum == null) {
                // 新增专辑
                Album newAlbum = new Album();
                newAlbum.setAlbumName(firstSong.getAlbum());
                newAlbum.setArtist(firstSong.getArtist());
                newAlbum.setAlbumId(firstSong.getAlbumId());
                newAlbum.setSongCount(songs.size());
                newAlbum.setYear(firstSong.getYear());
                // 设置专辑的添加时间为该专辑中最新歌曲的添加时间
                long latestDateAdded = 0;
                for (Song song : songs) {
                    if (song.getDateAdded() > latestDateAdded) {
                        latestDateAdded = song.getDateAdded();
                    }
                }
                newAlbum.setDateAdded(latestDateAdded);
                newAlbum.save();
                result.addedAlbums++;
                Log.d(TAG, "新增专辑: " + firstSong.getAlbum() + " - " + firstSong.getArtist());
            } else {
                // 检查是否需要更新
                boolean needUpdate = false;
                
                if (existingAlbum.getSongCount() != songs.size()) {
                    existingAlbum.setSongCount(songs.size());
                    needUpdate = true;
                }
                
                if (existingAlbum.getYear() != firstSong.getYear()) {
                    existingAlbum.setYear(firstSong.getYear());
                    needUpdate = true;
                }
                
                if (needUpdate) {
                    existingAlbum.save();
                    result.updatedAlbums++;
                    Log.d(TAG, "更新专辑: " + firstSong.getAlbum() + " - " + firstSong.getArtist());
                }
            }
        }
        
        // 处理删除的专辑
        for (Album album : existingAlbums) {
            String albumKey = album.getAlbumName() + "_" + album.getArtist();
            if (!albumGroups.containsKey(albumKey)) {
                album.delete();
                result.deletedAlbums++;
                Log.d(TAG, "删除专辑: " + album.getAlbumName() + " - " + album.getArtist());
            }
        }
    }
    
    /**
     * 同步艺术家信息
     */
    private static void syncArtists(SyncResult result) {
        Log.d(TAG, "开始同步艺术家信息");
        
        // 获取所有歌曲
        List<Song> allSongs = LitePal.findAll(Song.class);
        
        // 按艺术家分组统计
        Map<String, List<Song>> artistGroups = new HashMap<>();
        Map<String, List<String>> artistAlbumGroups = new HashMap<>();
        
        for (Song song : allSongs) {
            String artistName = song.getArtist();
            
            if (!artistGroups.containsKey(artistName)) {
                artistGroups.put(artistName, new ArrayList<>());
                artistAlbumGroups.put(artistName, new ArrayList<>());
            }
            
            artistGroups.get(artistName).add(song);
            
            String albumKey = song.getAlbum() + "_" + song.getArtist();
            if (!artistAlbumGroups.get(artistName).contains(albumKey)) {
                artistAlbumGroups.get(artistName).add(albumKey);
            }
        }
        
        // 获取现有艺术家
        List<Artist> existingArtists = LitePal.findAll(Artist.class);
        Map<String, Artist> existingArtistMap = new HashMap<>();
        for (Artist artist : existingArtists) {
            existingArtistMap.put(artist.getArtistName(), artist);
        }
        
        // 处理新增和更新的艺术家
        for (Map.Entry<String, List<Song>> entry : artistGroups.entrySet()) {
            String artistName = entry.getKey();
            List<Song> songs = entry.getValue();
            List<String> albums = artistAlbumGroups.get(artistName);
            
            if (songs.isEmpty()) continue;
            
            Song firstSong = songs.get(0);
            Artist existingArtist = existingArtistMap.get(artistName);
            
            if (existingArtist == null) {
                // 新增艺术家
                Artist newArtist = new Artist();
                newArtist.setArtistName(artistName);
                newArtist.setArtistId(firstSong.getArtistId());
                newArtist.setSongCount(songs.size());
                newArtist.setAlbumCount(albums.size());
                // 设置艺术家的添加时间为该艺术家中最新歌曲的添加时间
                long latestDateAdded = 0;
                for (Song song : songs) {
                    if (song.getDateAdded() > latestDateAdded) {
                        latestDateAdded = song.getDateAdded();
                    }
                }
                newArtist.setDateAdded(latestDateAdded);
                newArtist.save();
                result.addedArtists++;
                Log.d(TAG, "新增艺术家: " + artistName);
            } else {
                // 检查是否需要更新
                boolean needUpdate = false;
                
                if (existingArtist.getSongCount() != songs.size()) {
                    existingArtist.setSongCount(songs.size());
                    needUpdate = true;
                }
                
                if (existingArtist.getAlbumCount() != albums.size()) {
                    existingArtist.setAlbumCount(albums.size());
                    needUpdate = true;
                }
                
                if (needUpdate) {
                    existingArtist.save();
                    result.updatedArtists++;
                    Log.d(TAG, "更新艺术家: " + artistName);
                }
            }
        }
        
        // 处理删除的艺术家
        for (Artist artist : existingArtists) {
            if (!artistGroups.containsKey(artist.getArtistName())) {
                artist.delete();
                result.deletedArtists++;
                Log.d(TAG, "删除艺术家: " + artist.getArtistName());
            }
        }
    }
} 
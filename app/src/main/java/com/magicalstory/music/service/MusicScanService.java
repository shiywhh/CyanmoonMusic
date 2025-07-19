package com.magicalstory.music.service;

import android.app.Service;
import android.content.ContentResolver;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Binder;
import android.os.IBinder;
import android.provider.MediaStore;
import android.text.TextUtils;
import android.util.Log;

import androidx.annotation.Nullable;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import com.magicalstory.music.model.Album;
import com.magicalstory.music.model.Artist;
import com.magicalstory.music.model.Song;

import org.litepal.LitePal;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 音乐扫描服务
 */
public class MusicScanService extends Service {
    private static final String TAG = "MusicScanService";

    public static final String ACTION_SCAN_COMPLETE = "com.magicalstory.music.SCAN_COMPLETE";
    public static final String EXTRA_SCAN_COUNT = "scan_count";

    // 最小歌曲时长（毫秒），小于此时长的歌曲将被过滤
    private static final long MIN_SONG_DURATION = 60000; // 60秒

    private final IBinder binder = new MusicScanBinder();
    private boolean isScanning = false;

    public class MusicScanBinder extends Binder {
        public MusicScanService getService() {
            return MusicScanService.this;
        }
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return binder;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (!isScanning) {
            startMusicScan();
        }
        return START_NOT_STICKY;
    }

    /**
     * 开始扫描音乐
     */
    public void startMusicScan() {
        if (isScanning) {
            Log.d(TAG, "扫描已在进行中");
            return;
        }

        isScanning = true;
        Log.d(TAG, "开始扫描音乐文件");

        new Thread(() -> {
            int newSongCount = 0;
            try {
                newSongCount = scanMusicFiles();
                Log.d(TAG, "音乐扫描完成，新增歌曲: " + newSongCount);
            } catch (Exception e) {
                Log.e(TAG, "扫描音乐文件时出错", e);
                newSongCount = 0;
            } finally {
                // 无论扫描成功还是失败，都发送广播
                try {
                    Intent broadcastIntent = new Intent(ACTION_SCAN_COMPLETE);
                    broadcastIntent.putExtra(EXTRA_SCAN_COUNT, newSongCount);
                    // 使用LocalBroadcastManager发送广播
                    LocalBroadcastManager.getInstance(this).sendBroadcast(broadcastIntent);
                    Log.d(TAG, "发送本地广播: " + ACTION_SCAN_COMPLETE + ", 歌曲数量: " + newSongCount);
                } catch (Exception e) {
                    Log.e(TAG, "发送广播时出错", e);
                }

                isScanning = false;
                stopSelf();
            }
        }).start();
    }

    /**
     * 扫描音乐文件
     */
    private int scanMusicFiles() {
        Log.d(TAG, "开始扫描音乐文件");
        ContentResolver contentResolver = getContentResolver();
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
                MediaStore.Audio.Media.DURATION + " > " + MIN_SONG_DURATION; // 大于1分钟的音频文件

        String sortOrder = MediaStore.Audio.Media.TITLE + " ASC";

        List<Song> newSongs = new ArrayList<>();
        Map<String, Album> albumMap = new HashMap<>();
        Map<String, Artist> artistMap = new HashMap<>();

        // 清空现有数据
        Log.d(TAG, "清空现有数据");
        LitePal.deleteAll(Song.class);
        LitePal.deleteAll(Album.class);
        LitePal.deleteAll(Artist.class);

        Cursor cursor = contentResolver.query(uri, projection, selection, null, sortOrder);

        if (cursor != null) {
            Log.d(TAG, "查询到 " + cursor.getCount() + " 个音乐文件");
            try {
                while (cursor.moveToNext()) {
                    Song song = createSongFromCursor(cursor);
                    if (song != null && !TextUtils.isEmpty(song.getPath())) {
                        newSongs.add(song);

                        // 处理专辑信息
                        processAlbumInfo(song, albumMap);

                        // 处理艺术家信息
                        processArtistInfo(song, artistMap);
                    }
                }
            } finally {
                cursor.close();
            }
        } else {
            Log.w(TAG, "查询音乐文件失败，cursor为null");
        }

        // 批量保存到数据库
        Log.d(TAG, "保存数据到数据库");
        LitePal.saveAll(newSongs);
        LitePal.saveAll(new ArrayList<>(albumMap.values()));
        LitePal.saveAll(new ArrayList<>(artistMap.values()));

        Log.d(TAG, "扫描完成: 歌曲 " + newSongs.size() + ", 专辑 " + albumMap.size() + ", 艺术家 " + artistMap.size());

        return newSongs.size();
    }

    /**
     * 从Cursor创建Song对象
     */
    private Song createSongFromCursor(Cursor cursor) {
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
     * 处理专辑信息
     */
    private void processAlbumInfo(Song song, Map<String, Album> albumMap) {
        String albumKey = song.getAlbum() + "_" + song.getArtist();
        Album album = albumMap.get(albumKey);

        if (album == null) {
            album = new Album();
            album.setAlbumName(song.getAlbum());
            album.setArtist(song.getArtist());
            album.setAlbumId(song.getAlbumId());
            album.setSongCount(1);
            album.setYear(song.getYear());
            albumMap.put(albumKey, album);
        } else {
            album.setSongCount(album.getSongCount() + 1);
        }
    }

    /**
     * 处理艺术家信息
     */
    private void processArtistInfo(Song song, Map<String, Artist> artistMap) {
        String artistName = song.getArtist();
        Artist artist = artistMap.get(artistName);

        if (artist == null) {
            artist = new Artist();
            artist.setArtistName(artistName);
            artist.setArtistId(song.getArtistId());
            artist.setSongCount(1);
            artist.setAlbumCount(1);
            artistMap.put(artistName, artist);
        } else {
            artist.setSongCount(artist.getSongCount() + 1);
        }
    }

    /**
     * 是否正在扫描
     */
    public boolean isScanning() {
        return isScanning;
    }
}
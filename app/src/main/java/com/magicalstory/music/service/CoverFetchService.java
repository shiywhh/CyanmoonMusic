package com.magicalstory.music.service;

import android.app.IntentService;
import android.content.Context;
import android.content.Intent;
import android.text.TextUtils;
import android.util.Log;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.magicalstory.music.model.Album;
import com.magicalstory.music.model.Artist;
import com.magicalstory.music.utils.glide.CoverFallbackUtils;
import com.magicalstory.music.utils.network.NetUtils;

import org.litepal.LitePal;

import java.util.List;

import okhttp3.Response;

/**
 * 封面获取服务
 * 用于后台批量获取和处理专辑和歌手的封面
 */
public class CoverFetchService extends IntentService {
    
    private static final String TAG = "CoverFetchService";
    
    public static final String ACTION_FETCH_ALL_COVERS = "com.magicalstory.music.service.FETCH_ALL_COVERS";
    public static final String ACTION_FETCH_ALBUM_COVERS = "com.magicalstory.music.service.FETCH_ALBUM_COVERS";
    public static final String ACTION_FETCH_ARTIST_COVERS = "com.magicalstory.music.service.FETCH_ARTIST_COVERS";
    
    public CoverFetchService() {
        super("CoverFetchService");
    }
    
    /**
     * 启动批量获取所有封面的服务
     */
    public static void startFetchAllCovers(Context context) {
        Intent intent = new Intent(context, CoverFetchService.class);
        intent.setAction(ACTION_FETCH_ALL_COVERS);
        context.startService(intent);
    }
    
    /**
     * 启动批量获取专辑封面的服务
     */
    public static void startFetchAlbumCovers(Context context) {
        Intent intent = new Intent(context, CoverFetchService.class);
        intent.setAction(ACTION_FETCH_ALBUM_COVERS);
        context.startService(intent);
    }
    
    /**
     * 启动批量获取歌手封面的服务
     */
    public static void startFetchArtistCovers(Context context) {
        Intent intent = new Intent(context, CoverFetchService.class);
        intent.setAction(ACTION_FETCH_ARTIST_COVERS);
        context.startService(intent);
    }
    
    @Override
    protected void onHandleIntent(Intent intent) {
        if (intent != null) {
            final String action = intent.getAction();
            
            if (ACTION_FETCH_ALL_COVERS.equals(action)) {
                handleFetchAllCovers();
            } else if (ACTION_FETCH_ALBUM_COVERS.equals(action)) {
                handleFetchAlbumCovers();
            } else if (ACTION_FETCH_ARTIST_COVERS.equals(action)) {
                handleFetchArtistCovers();
            }
        }
    }
    
    /**
     * 处理获取所有封面的请求
     */
    private void handleFetchAllCovers() {
        Log.d(TAG, "开始批量获取所有封面...");
        
        // 获取数据库中所有的专辑
        List<Album> allAlbums = LitePal.findAll(Album.class);
        Log.d(TAG, "数据库中共有 " + allAlbums.size() + " 个专辑");
        
        // 获取数据库中所有的歌手
        List<Artist> allArtists = LitePal.findAll(Artist.class);
        Log.d(TAG, "数据库中共有 " + allArtists.size() + " 个歌手");
        
        // 先处理专辑封面
        batchProcessAlbumCovers(allAlbums);
        
        // 再处理歌手封面
        batchProcessArtistCovers(allArtists);
        
        Log.d(TAG, "批量获取所有封面完成");
    }
    
    /**
     * 处理获取专辑封面的请求
     */
    private void handleFetchAlbumCovers() {
        Log.d(TAG, "开始批量获取专辑封面...");
        
        List<Album> allAlbums = LitePal.findAll(Album.class);
        Log.d(TAG, "数据库中共有 " + allAlbums.size() + " 个专辑");
        
        batchProcessAlbumCovers(allAlbums);
        
        Log.d(TAG, "批量获取专辑封面完成");
    }
    
    /**
     * 处理获取歌手封面的请求
     */
    private void handleFetchArtistCovers() {
        Log.d(TAG, "开始批量获取歌手封面...");
        
        List<Artist> allArtists = LitePal.findAll(Artist.class);
        Log.d(TAG, "数据库中共有 " + allArtists.size() + " 个歌手");
        
        batchProcessArtistCovers(allArtists);
        
        Log.d(TAG, "批量获取歌手封面完成");
    }
    
    /**
     * 批量处理专辑封面
     */
    private void batchProcessAlbumCovers(List<Album> albums) {
        if (albums == null || albums.isEmpty()) {
            return;
        }
        
        int processedCount = 0;
        int successCount = 0;
        
        for (Album album : albums) {
            processedCount++;
            
            // 检查专辑是否已经有封面
            if (TextUtils.isEmpty(album.getAlbumArt())) {
                Log.d(TAG, "处理专辑封面: " + album.getAlbumName() + " - " + album.getArtist() + " (" + processedCount + "/" + albums.size() + ")");
                
                // 使用封面回退逻辑获取封面
                if (CoverFallbackUtils.setAlbumFallbackCover(album)) {
                    successCount++;
                    Log.d(TAG, "成功为专辑 " + album.getAlbumName() + " 设置封面");
                } else {
                    Log.d(TAG, "专辑 " + album.getAlbumName() + " 未找到可用封面");
                }
            } else {
                Log.d(TAG, "专辑 " + album.getAlbumName() + " 已有封面，跳过");
            }
            
            // 每处理10个专辑输出一次进度
            if (processedCount % 10 == 0) {
                Log.d(TAG, "专辑封面处理进度: " + processedCount + "/" + albums.size() + " (成功: " + successCount + ")");
            }
        }
        
        Log.d(TAG, "专辑封面处理完成: 处理了 " + processedCount + " 个专辑，成功设置 " + successCount + " 个封面");
    }
    
    /**
     * 批量处理歌手封面
     */
    private void batchProcessArtistCovers(List<Artist> artists) {
        if (artists == null || artists.isEmpty()) {
            return;
        }
        
        int processedCount = 0;
        int successCount = 0;
        int apiRequestCount = 0;
        
        for (Artist artist : artists) {
            processedCount++;
            
            // 检查歌手是否已经有封面或已经尝试过获取封面
            if (TextUtils.isEmpty(artist.getCoverUrl()) && !artist.isCoverFetched()) {
                Log.d(TAG, "处理歌手封面: " + artist.getArtistName() + " (" + processedCount + "/" + artists.size() + ")");
                
                // 首先尝试使用封面回退逻辑获取封面
                if (CoverFallbackUtils.setArtistFallbackCover(artist)) {
                    successCount++;
                    Log.d(TAG, "成功为歌手 " + artist.getArtistName() + " 设置回退封面");
                } else {
                    // 如果回退封面也没有，则尝试从API获取
                    if (fetchArtistCoverFromAPI(artist)) {
                        successCount++;
                        apiRequestCount++;
                        Log.d(TAG, "成功从API获取歌手 " + artist.getArtistName() + " 的封面");
                    } else {
                        Log.d(TAG, "歌手 " + artist.getArtistName() + " 未找到可用封面");
                    }
                }
            } else if (!TextUtils.isEmpty(artist.getCoverUrl())) {
                Log.d(TAG, "歌手 " + artist.getArtistName() + " 已有封面，跳过");
            } else {
                Log.d(TAG, "歌手 " + artist.getArtistName() + " 已尝试过获取封面，跳过");
            }
            
            // 每处理10个歌手输出一次进度
            if (processedCount % 10 == 0) {
                Log.d(TAG, "歌手封面处理进度: " + processedCount + "/" + artists.size() + " (成功: " + successCount + ", API请求: " + apiRequestCount + ")");
            }
            
            // 为了避免频繁请求API，每次API请求后稍作延迟
            if (apiRequestCount > 0 && processedCount % 5 == 0) {
                try {
                    Thread.sleep(200); // 延迟200ms
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    break;
                }
            }
        }
        
        Log.d(TAG, "歌手封面处理完成: 处理了 " + processedCount + " 个歌手，成功设置 " + successCount + " 个封面 (API请求: " + apiRequestCount + ")");
    }
    
    /**
     * 从API获取歌手封面
     */
    private boolean fetchArtistCoverFromAPI(Artist artist) {
        try {
            String artistName = artist.getArtistName();
            String url = "https://music.163.com/api/search/get/web?s=" +
                    java.net.URLEncoder.encode(artistName, "UTF-8") + "&type=100";

            Response response = NetUtils.getInstance().getDataSynFromNet(url);
            if (response != null && response.isSuccessful()) {
                String jsonResponse = response.body().string();
                return parseAndSaveArtistCover(artist, jsonResponse);
            } else {
                // 获取失败，标记为已尝试过
                artist.setCoverFetched(true);
                artist.save();
                return false;
            }
        } catch (Exception e) {
            Log.e(TAG, "从API获取歌手封面失败: " + e.getMessage(), e);
            // 异常情况下也标记为已尝试过
            artist.setCoverFetched(true);
            artist.save();
            return false;
        }
    }
    
    /**
     * 解析并保存艺术家封面
     */
    private boolean parseAndSaveArtistCover(Artist artist, String jsonResponse) {
        try {
            JsonObject jsonObject = JsonParser.parseString(jsonResponse).getAsJsonObject();
            if (jsonObject.has("code") && jsonObject.get("code").getAsInt() == 200 && jsonObject.has("result")) {
                JsonObject result = jsonObject.getAsJsonObject("result");
                if (result.has("artists")) {
                    JsonArray artists = result.getAsJsonArray("artists");
                    if (artists.size() > 0) {
                        JsonObject artistInfo = artists.get(0).getAsJsonObject();
                        if (artistInfo.has("picUrl")) {
                            String picUrl = artistInfo.get("picUrl").getAsString();
                            if (picUrl != null && !picUrl.isEmpty()) {
                                // 更新数据库
                                artist.setCoverUrl(picUrl);
                                artist.setCoverFetched(true);
                                artist.save();
                                return true;
                            }
                        }
                    }
                }
            }
            // 如果没有获取到封面，也标记为已尝试过
            artist.setCoverFetched(true);
            artist.save();
            return false;
        } catch (Exception e) {
            Log.e(TAG, "解析艺术家封面失败: " + e.getMessage(), e);
            // 异常情况下也标记为已尝试过
            artist.setCoverFetched(true);
            artist.save();
            return false;
        }
    }
} 
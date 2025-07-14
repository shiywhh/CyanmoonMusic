package com.magicalstory.music.utils.favorite;

import android.content.Context;
import android.widget.Toast;

import com.magicalstory.music.model.FavoriteSong;
import com.magicalstory.music.model.Song;

import org.litepal.LitePal;

import java.util.ArrayList;
import java.util.List;

/**
 * 收藏歌曲管理类
 */
public class FavoriteManager {
    
    private static FavoriteManager instance;
    private Context context;
    
    private FavoriteManager(Context context) {
        this.context = context.getApplicationContext();
    }
    
    public static FavoriteManager getInstance(Context context) {
        if (instance == null) {
            synchronized (FavoriteManager.class) {
                if (instance == null) {
                    instance = new FavoriteManager(context);
                }
            }
        }
        return instance;
    }
    
    /**
     * 添加歌曲到收藏
     */
    public boolean addToFavorite(long songId) {
        try {
            // 检查是否已经收藏
            if (isFavorite(songId)) {
                return false; // 已经收藏
            }
            
            // 获取当前最大排序值
            int maxSortOrder = getMaxSortOrder();
            
            // 创建收藏记录
            FavoriteSong favoriteSong = new FavoriteSong(songId, maxSortOrder + 1);
            return favoriteSong.save();
            
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }
    
    /**
     * 从收藏中移除歌曲
     */
    public boolean removeFromFavorite(long songId) {
        try {
            return LitePal.deleteAll(FavoriteSong.class, "songId = ?", String.valueOf(songId)) > 0;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }
    
    /**
     * 检查歌曲是否已收藏
     */
    public boolean isFavorite(long songId) {
        try {
            return LitePal.where("songId = ?", String.valueOf(songId))
                    .count(FavoriteSong.class) > 0;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }
    
    /**
     * 切换收藏状态
     */
    public boolean toggleFavorite(long songId) {
        if (isFavorite(songId)) {
            return removeFromFavorite(songId);
        } else {
            return addToFavorite(songId);
        }
    }
    
    /**
     * 获取所有收藏的歌曲ID
     */
    public List<Long> getAllFavoriteSongIds() {
        try {
            List<FavoriteSong> favoriteSongs = LitePal.order("sortOrder asc, addTime desc")
                    .find(FavoriteSong.class);
            
            List<Long> songIds = new ArrayList<>();
            for (FavoriteSong favoriteSong : favoriteSongs) {
                songIds.add(favoriteSong.getSongId());
            }
            return songIds;
        } catch (Exception e) {
            e.printStackTrace();
            return new ArrayList<>();
        }
    }
    
    /**
     * 获取收藏歌曲总数
     */
    public int getFavoriteCount() {
        try {
            return (int) LitePal.count(FavoriteSong.class);
        } catch (Exception e) {
            e.printStackTrace();
            return 0;
        }
    }
    
    /**
     * 获取当前最大排序值
     */
    private int getMaxSortOrder() {
        try {
            List<FavoriteSong> lastFavorite = LitePal.order("sortOrder desc")
                    .limit(1)
                    .find(FavoriteSong.class);
            
            if (lastFavorite.isEmpty()) {
                return 0;
            }
            
            return lastFavorite.get(0).getSortOrder();
        } catch (Exception e) {
            e.printStackTrace();
            return 0;
        }
    }
    
    /**
     * 更新收藏歌曲的排序
     */
    public boolean updateSortOrder(long songId, int newSortOrder) {
        try {
            FavoriteSong favoriteSong = LitePal.where("songId = ?", String.valueOf(songId))
                    .findFirst(FavoriteSong.class);
            
            if (favoriteSong != null) {
                favoriteSong.setSortOrder(newSortOrder);
                return favoriteSong.save();
            }
            
            return false;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }
    
    /**
     * 清空所有收藏
     */
    public boolean clearAllFavorites() {
        try {
            return LitePal.deleteAll(FavoriteSong.class) > 0;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }
    
    /**
     * 获取收藏的歌曲详情列表（需要结合Song表查询）
     */
    public List<FavoriteSong> getAllFavoriteDetails() {
        try {
            return LitePal.order("sortOrder asc, addTime desc")
                    .find(FavoriteSong.class);
        } catch (Exception e) {
            e.printStackTrace();
            return new ArrayList<>();
        }
    }
} 
package com.magicalstory.music.utils.playlist;

import android.content.Context;
import android.content.Intent;
import android.os.Handler;
import android.os.Looper;
import android.text.TextUtils;
import android.util.Log;

import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.magicalstory.music.dialog.dialogUtils;
import com.magicalstory.music.model.Playlist;
import com.magicalstory.music.model.Song;
import com.magicalstory.music.utils.app.ToastUtils;

import org.litepal.LitePal;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * 添加到歌单工具类
 * 支持批量添加歌曲到歌单，内部处理所有操作
 */
public class PlaylistAddUtils {
    
    private static final String TAG = "PlaylistAddUtils";
    private static final String REFRESH_PLAYLIST_ACTION = "com.magicalstory.music.REFRESH_PLAYLIST";
    
    private static final ExecutorService executorService = Executors.newCachedThreadPool();
    private static final Handler mainHandler = new Handler(Looper.getMainLooper());
    
    /**
     * 显示歌单选择对话框并添加歌曲
     * 
     * @param context 上下文
     * @param songs 要添加的歌曲列表
     */
    public static void showPlaylistSelectorDialog(Context context, List<Song> songs) {
        if (context == null || songs == null || songs.isEmpty()) {
            Log.w(TAG, "参数无效，无法显示歌单选择对话框");
            return;
        }
        
        // 在后台线程中加载歌单数据
        executorService.execute(() -> {
            try {
                // 获取所有非系统歌单
                List<Playlist> playlists = LitePal.where("isSystemPlaylist = ?", "0")
                        .order("updatedTime desc")
                        .find(Playlist.class);

                // 更新歌单的歌曲数量
                for (Playlist playlist : playlists) {
                    int songCount = com.magicalstory.music.model.PlaylistSong.getPlaylistSongCount(playlist.getId());
                    playlist.setSongCount(songCount);
                }

                // 在主线程中显示对话框
                mainHandler.post(() -> {
                    showPlaylistSelectorDialog(context, playlists, songs);
                });

            } catch (Exception e) {
                Log.e(TAG, "加载歌单数据失败: " + e.getMessage(), e);
                mainHandler.post(() -> {
                    ToastUtils.showToast(context, "加载歌单失败: " + e.getMessage());
                });
            }
        });
    }
    
    /**
     * 显示歌单选择对话框
     */
    private static void showPlaylistSelectorDialog(Context context, List<Playlist> playlists, List<Song> songs) {
        // 准备选项列表
        List<String> options = new ArrayList<>();
        options.add("新建歌单"); // 第一个选项是新建歌单
        
        // 添加现有歌单
        for (Playlist playlist : playlists) {
            options.add(playlist.getName() + " (" + playlist.getSongCount() + "首歌曲)");
        }

        // 转换为数组
        String[] items = options.toArray(new String[0]);

        // 使用Android自带的列表dialog
        new MaterialAlertDialogBuilder(context)
                .setTitle("选择歌单")
                .setItems(items, (dialog, which) -> {
                    if (which == 0) {
                        // 选择新建歌单
                        showCreatePlaylistDialog(context, songs);
                    } else {
                        // 选择现有歌单
                        Playlist selectedPlaylist = playlists.get(which - 1);
                        addSongsToPlaylist(context, selectedPlaylist, songs);
                    }
                })
                .setNegativeButton("取消", null)
                .show();
    }
    
    /**
     * 显示创建歌单对话框
     */
    private static void showCreatePlaylistDialog(Context context, List<Song> songs) {
        dialogUtils.getInstance().showInputDialog(
                context,
                "创建歌单",
                "歌单名称",
                "",
                false,
                "",
                new dialogUtils.InputDialogListener() {
                    @Override
                    public void onInputProvided(String playlistName) {
                        if (!TextUtils.isEmpty(playlistName.trim())) {
                            createPlaylistAndAddSongs(context, playlistName.trim(), songs);
                        } else {
                            ToastUtils.showToast(context, "歌单名称不能为空");
                        }
                    }

                    @Override
                    public void onCancel() {
                        // 用户取消创建
                    }
                }
        );
    }
    
    /**
     * 创建歌单并添加歌曲
     */
    private static void createPlaylistAndAddSongs(Context context, String playlistName, List<Song> songs) {
        executorService.execute(() -> {
            try {
                // 检查歌单名称是否已存在
                List<Playlist> existingPlaylists = LitePal.where("name = ?", playlistName).find(Playlist.class);
                if (!existingPlaylists.isEmpty()) {
                    mainHandler.post(() -> {
                        ToastUtils.showToast(context, "歌单名称已存在");
                    });
                    return;
                }

                // 创建新歌单
                Playlist newPlaylist = new Playlist(playlistName, "");
                newPlaylist.setSystemPlaylist(false);
                boolean saved = newPlaylist.save();

                if (saved) {
                    // 添加歌曲到新歌单
                    int addedCount = addSongsToPlaylistInternal(newPlaylist, songs);
                    
                    mainHandler.post(() -> {
                        if (addedCount > 0) {
                            ToastUtils.showToast(context, "歌单创建成功，" + addedCount + "首歌曲已添加");
                        } else {
                            ToastUtils.showToast(context, "歌单创建成功，但歌曲添加失败");
                        }
                        
                        // 通知PlaylistFragment刷新列表
                        notifyPlaylistFragmentRefresh(context);
                    });
                } else {
                    mainHandler.post(() -> {
                        ToastUtils.showToast(context, "歌单创建失败");
                    });
                }
            } catch (Exception e) {
                Log.e(TAG, "创建歌单失败: " + e.getMessage(), e);
                mainHandler.post(() -> {
                    ToastUtils.showToast(context, "创建歌单失败: " + e.getMessage());
                });
            }
        });
    }
    
    /**
     * 添加歌曲到指定歌单
     */
    private static void addSongsToPlaylist(Context context, Playlist playlist, List<Song> songs) {
        if (playlist == null || songs == null || songs.isEmpty()) {
            ToastUtils.showToast(context, "数据错误");
            return;
        }
        
        executorService.execute(() -> {
            try {
                int addedCount = addSongsToPlaylistInternal(playlist, songs);
                
                mainHandler.post(() -> {
                    if (addedCount > 0) {
                        ToastUtils.showToast(context, "已添加" + addedCount + "首歌曲到歌单: " + playlist.getName());
                    } else {
                        ToastUtils.showToast(context, "该歌曲已存在此歌单中");
                    }
                    
                    // 通知PlaylistFragment刷新列表
                    notifyPlaylistFragmentRefresh(context);
                });
                
            } catch (Exception e) {
                Log.e(TAG, "添加歌曲到歌单失败: " + e.getMessage(), e);
                mainHandler.post(() -> {
                    ToastUtils.showToast(context, "添加失败: " + e.getMessage());
                });
            }
        });
    }
    
    /**
     * 内部方法：添加歌曲到歌单
     * 
     * @param playlist 歌单
     * @param songs 歌曲列表
     * @return 实际添加的歌曲数量
     */
    private static int addSongsToPlaylistInternal(Playlist playlist, List<Song> songs) {
        int addedCount = 0;
        Song latestAddedSong = null; // 记录最新添加的歌曲，用于更新封面
        
        for (Song song : songs) {
            try {
                // 检查歌曲是否已在歌单中
                if (!playlist.containsSong(song)) {
                    // 添加歌曲到歌单
                    playlist.addSong(song);
                    addedCount++;
                    latestAddedSong = song; // 更新最新添加的歌曲
                }
            } catch (Exception e) {
                Log.e(TAG, "添加歌曲到歌单失败: " + song.getTitle() + ", 错误: " + e.getMessage(), e);
            }
        }
        
        // 如果有新歌曲添加，确保歌单封面更新为最新歌曲的封面
        if (addedCount > 0 && latestAddedSong != null) {
            try {
                playlist.updateCoverToLatestSong();
            } catch (Exception e) {
                Log.e(TAG, "更新歌单封面失败: " + e.getMessage(), e);
            }
        }
        
        return addedCount;
    }
    
    /**
     * 通知PlaylistFragment刷新列表
     */
    private static void notifyPlaylistFragmentRefresh(Context context) {
        try {
            // 发送广播通知PlaylistFragment刷新
            Intent intent = new Intent(REFRESH_PLAYLIST_ACTION);
            LocalBroadcastManager.getInstance(context).sendBroadcast(intent);
            
            Log.d(TAG, "已发送刷新歌单列表广播");
        } catch (Exception e) {
            Log.e(TAG, "发送刷新广播失败: " + e.getMessage(), e);
        }
    }
    
    /**
     * 直接添加歌曲到指定歌单（不显示对话框）
     * 
     * @param context 上下文
     * @param playlistId 歌单ID
     * @param songs 歌曲列表
     */
    public static void addSongsToPlaylistDirectly(Context context, long playlistId, List<Song> songs) {
        if (context == null || songs == null || songs.isEmpty()) {
            Log.w(TAG, "参数无效，无法添加歌曲到歌单");
            return;
        }
        
        executorService.execute(() -> {
            try {
                // 查找歌单
                Playlist playlist = LitePal.find(Playlist.class, playlistId);
                if (playlist == null) {
                    mainHandler.post(() -> {
                        ToastUtils.showToast(context, "歌单不存在");
                    });
                    return;
                }
                
                int addedCount = addSongsToPlaylistInternal(playlist, songs);
                
                mainHandler.post(() -> {
                    if (addedCount > 0) {
                        ToastUtils.showToast(context, "已添加" + addedCount + "首歌曲到歌单: " + playlist.getName());
                    } else {
                        ToastUtils.showToast(context, "该歌曲已存在此歌单中");
                    }
                    
                    // 通知PlaylistFragment刷新列表
                    notifyPlaylistFragmentRefresh(context);
                });
                
            } catch (Exception e) {
                Log.e(TAG, "直接添加歌曲到歌单失败: " + e.getMessage(), e);
                mainHandler.post(() -> {
                    ToastUtils.showToast(context, "添加失败: " + e.getMessage());
                });
            }
        });
    }
    
    /**
     * 清理资源
     */
    public static void shutdown() {
        if (executorService != null && !executorService.isShutdown()) {
            executorService.shutdown();
        }
    }
}
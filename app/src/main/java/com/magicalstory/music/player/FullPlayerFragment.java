package com.magicalstory.music.player;

import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.PorterDuff;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.ImageView;
import android.widget.SeekBar;
import android.widget.TextView;
import android.database.Cursor;
import android.media.MediaMetadataRetriever;
import android.provider.MediaStore;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.core.content.ContextCompat;
import androidx.media3.common.MediaItem;
import androidx.media3.common.PlaybackException;
import androidx.media3.common.Player;
import androidx.media3.common.util.UnstableApi;
import androidx.media3.session.MediaController;

import com.bumptech.glide.Glide;
import com.bumptech.glide.request.target.CustomTarget;
import com.bumptech.glide.request.transition.Transition;
import com.google.android.material.card.MaterialCardView;
import com.magicalstory.music.MainActivity;
import com.magicalstory.music.R;
import com.magicalstory.music.base.BaseFragment;
import com.magicalstory.music.databinding.FragmentFullPlayerBinding;
import com.magicalstory.music.model.Song;
import com.magicalstory.music.model.LyricLine;
import com.magicalstory.music.myView.LyricsView;
import com.magicalstory.music.utils.app.ToastUtils;
import com.magicalstory.music.utils.favorite.FavoriteManager;
import com.magicalstory.music.utils.glide.BlurUtils;
import com.magicalstory.music.utils.glide.ColorExtractor;
import com.magicalstory.music.utils.glide.GlideUtils;
import com.magicalstory.music.utils.lyrics.LyricsParser;
import com.magicalstory.music.utils.text.TimeUtils;
import com.magicalstory.music.utils.MediaControllerHelper;

/**
 * 全屏播放器Fragment - 使用MediaController
 */
@UnstableApi
public class FullPlayerFragment extends BaseFragment<FragmentFullPlayerBinding> implements Player.Listener {

    private static final String TAG = "FullPlayerFragment";

    // 背景类型常量
    private static final int BACKGROUND_TYPE_DEFAULT = 0;
    private static final int BACKGROUND_TYPE_SOLID = 1;
    private static final int BACKGROUND_TYPE_GRADIENT = 2;
    private static final int BACKGROUND_TYPE_BLUR = 3;

    // MediaController相关
    private MediaController mediaController;
    private MediaControllerHelper controllerHelper;

    private boolean isUserSeeking = false;
    private FavoriteManager favoriteManager;

    // 播放模式
    private int currentPlayMode = Player.REPEAT_MODE_OFF;

    // 背景类型
    private int currentBackgroundType = BACKGROUND_TYPE_GRADIENT;

    // 歌词相关
    private boolean isLyricsVisible = false;
    private List<LyricLine> currentLyrics = new ArrayList<>();
    private long currentSongId = -1; // 记录当前歌曲ID，用于判断是否需要重新加载歌词

    // 当前专辑封面和提取的颜色
    private Bitmap currentAlbumBitmap;
    private int dominantColor = Color.parseColor("#3353BE");
    private int darkColor = Color.parseColor("#1A237E");

    // 颜色资源记录
    private int defaultTextPrimaryColor;
    private int defaultTextSecondaryColor;
    private int defaultFavoriteColor;
    private int whiteColor = Color.WHITE;
    private int grayColor = Color.parseColor("#E0E0E0");

    // 进度更新相关
    private Handler progressUpdateHandler = new Handler(Looper.getMainLooper());
    private Runnable progressUpdateRunnable;

    @Override
    protected FragmentFullPlayerBinding getViewBinding(LayoutInflater inflater, ViewGroup container) {
        return FragmentFullPlayerBinding.inflate(inflater, container, false);
    }

    @Override
    protected void initView() {
        super.initView();
        favoriteManager = FavoriteManager.getInstance(context);

        // 初始化颜色资源
        initColors();

        // 初始化歌词View
        initLyricsView();

        // 设置点击事件
        setupClickListeners();

        // 设置进度条监听
        setupProgressListeners();

        // 设置默认状态
        updateDefaultState();
    }

    private void initColors() {
        defaultTextPrimaryColor = getResources().getColor(R.color.text_primary);
        defaultTextSecondaryColor = getResources().getColor(R.color.text_secondary);
        defaultFavoriteColor = getResources().getColor(R.color.favorite_color);
    }

    private void initLyricsView() {
        // 设置歌词点击监听器
        binding.lyricsView.setOnLyricClickListener((position, lyricLine) -> {
            if (mediaController != null) {
                // 跳转到歌词对应的播放位置
                mediaController.seekTo(lyricLine.getStartTime());

                // 如果当前是暂停状态，点击歌词后开始播放
                if (!mediaController.isPlaying()) {
                    mediaController.play();
                }
            }
        });
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        
        // 停止进度更新
        if (progressUpdateRunnable != null) {
            progressUpdateHandler.removeCallbacks(progressUpdateRunnable);
        }

        // 移除Player监听器
        if (mediaController != null) {
            mediaController.removeListener(this);
        }
    }

    // ===========================================
    // MediaController 设置
    // ===========================================

    /**
     * 设置MediaController
     */
    public void setMediaController(@Nullable MediaController mediaController) {
        Log.d(TAG, "设置MediaController: " + (mediaController != null ? "不为null" : "为null"));
        
        // 移除之前的监听器
        if (this.mediaController != null) {
            Log.d(TAG, "移除旧的MediaController监听器");
            this.mediaController.removeListener(this);
        }

        this.mediaController = mediaController;

        if (mediaController != null) {
            Log.d(TAG, "MediaController可用，开始初始化");
            
            // 添加监听器
            mediaController.addListener(this);
            Log.d(TAG, "已添加播放器监听器");
            
            // 创建辅助类
            controllerHelper = new MediaControllerHelper(mediaController, getContext());
            Log.d(TAG, "已创建MediaControllerHelper");
            
            // 记录当前状态
            Log.d(TAG, "当前播放状态: " + mediaController.getPlaybackState());
            Log.d(TAG, "当前播放: " + mediaController.isPlaying());
            Log.d(TAG, "当前媒体项: " + (mediaController.getCurrentMediaItem() != null ? 
                mediaController.getCurrentMediaItem().mediaId : "null"));
            Log.d(TAG, "播放列表大小: " + mediaController.getMediaItemCount());
            Log.d(TAG, "当前重复模式: " + mediaController.getRepeatMode());
            Log.d(TAG, "当前随机播放: " + mediaController.getShuffleModeEnabled());
            
            // 更新UI状态
            updatePlaybackState();
            updateCurrentSong();
            updatePlayModeButton(mediaController.getRepeatMode());
            
            Log.d(TAG, "MediaController设置完成");
        } else {
            Log.d(TAG, "MediaController为null，重置状态");
            controllerHelper = null;
            updateDefaultState();
        }
    }

    // ===========================================
    // Player.Listener 实现
    // ===========================================

    @Override
    public void onPlaybackStateChanged(int playbackState) {
        Log.d(TAG, "播放状态变化: " + playbackState);
        switch (playbackState) {
            case Player.STATE_IDLE:
                Log.d(TAG, "播放器状态: 空闲");
                break;
            case Player.STATE_BUFFERING:
                Log.d(TAG, "播放器状态: 缓冲中");
                break;
            case Player.STATE_READY:
                Log.d(TAG, "播放器状态: 准备就绪");
                break;
            case Player.STATE_ENDED:
                Log.d(TAG, "播放器状态: 播放结束");
                break;
            default:
                Log.d(TAG, "播放器状态: 未知状态 " + playbackState);
                break;
        }
        
        updatePlayButton(playbackState);
        
        if (playbackState == Player.STATE_READY || playbackState == Player.STATE_BUFFERING) {
            Log.d(TAG, "状态为准备就绪或缓冲中，开始进度更新");
            startProgressUpdates();
        } else {
            Log.d(TAG, "状态不为准备就绪或缓冲中，停止进度更新");
            stopProgressUpdates();
        }
    }

    @Override
    public void onIsPlayingChanged(boolean isPlaying) {
        Log.d(TAG, "播放状态变化: " + (isPlaying ? "正在播放" : "暂停"));
        updatePlayButton(isPlaying);
        
        if (isPlaying) {
            Log.d(TAG, "开始播放，启动进度更新");
            startProgressUpdates();
        } else {
            Log.d(TAG, "暂停播放，停止进度更新");
            stopProgressUpdates();
        }
    }

    @Override
    public void onMediaItemTransition(@Nullable MediaItem mediaItem, int reason) {
        String mediaId = mediaItem != null ? mediaItem.mediaId : "null";
        Log.d(TAG, "媒体项切换: " + mediaId + ", 原因: " + reason);
        
        switch (reason) {
            case Player.MEDIA_ITEM_TRANSITION_REASON_REPEAT:
                Log.d(TAG, "切换原因: 重复播放");
                break;
            case Player.MEDIA_ITEM_TRANSITION_REASON_AUTO:
                Log.d(TAG, "切换原因: 自动播放下一首");
                break;
            case Player.MEDIA_ITEM_TRANSITION_REASON_SEEK:
                Log.d(TAG, "切换原因: 跳转到指定位置");
                break;
            case Player.MEDIA_ITEM_TRANSITION_REASON_PLAYLIST_CHANGED:
                Log.d(TAG, "切换原因: 播放列表变化");
                break;
            default:
                Log.d(TAG, "切换原因: 未知原因 " + reason);
                break;
        }
        
        updateCurrentSong();
    }

    @Override
    public void onPlayerError(PlaybackException error) {
        Log.e(TAG, "播放器错误: " + error.getMessage(), error);
        Log.e(TAG, "错误代码: " + error.errorCode);
        Log.e(TAG, "错误时间戳: " + error.timestampMs);
        
        if (error.getCause() != null) {
            Log.e(TAG, "错误原因: " + error.getCause().getMessage());
        }
        
        updateDefaultState();
        
        // 显示错误提示
        String errorMessage = "播放失败: " + error.getMessage();
        ToastUtils.showToast(context, errorMessage);
    }

    @Override
    public void onRepeatModeChanged(int repeatMode) {
        Log.d(TAG, "重复模式变化: " + repeatMode);
        switch (repeatMode) {
            case Player.REPEAT_MODE_OFF:
                Log.d(TAG, "重复模式: 关闭");
                break;
            case Player.REPEAT_MODE_ONE:
                Log.d(TAG, "重复模式: 单曲循环");
                break;
            case Player.REPEAT_MODE_ALL:
                Log.d(TAG, "重复模式: 列表循环");
                break;
            default:
                Log.d(TAG, "重复模式: 未知模式 " + repeatMode);
                break;
        }
        currentPlayMode = repeatMode;
        updatePlayModeButton(repeatMode);
    }

    @Override
    public void onShuffleModeEnabledChanged(boolean shuffleModeEnabled) {
        Log.d(TAG, "随机播放模式变化: " + (shuffleModeEnabled ? "开启" : "关闭"));
        updateShuffleButton(shuffleModeEnabled);
    }

    // ===========================================
    // UI 更新方法
    // ===========================================

    /**
     * 更新播放状态
     */
    private void updatePlaybackState() {
        if (mediaController == null) {
            Log.w(TAG, "MediaController为null，无法更新播放状态");
            return;
        }
        
        Log.d(TAG, "更新播放状态");
        Log.d(TAG, "当前播放状态: " + mediaController.getPlaybackState());
        Log.d(TAG, "当前播放: " + mediaController.isPlaying());
        
        updatePlayButton(mediaController.isPlaying());
        
        if (mediaController.isPlaying()) {
            Log.d(TAG, "正在播放，启动进度更新");
            startProgressUpdates();
        } else {
            Log.d(TAG, "未在播放，停止进度更新");
            stopProgressUpdates();
        }
    }

    /**
     * 更新当前歌曲信息
     */
    private void updateCurrentSong() {
        if (controllerHelper == null) {
            Log.w(TAG, "MediaControllerHelper为null，无法更新当前歌曲");
            return;
        }

        Song currentSong = controllerHelper.getCurrentSong();
        if (currentSong != null) {
            Log.d(TAG, "更新当前歌曲: " + currentSong.getTitle());
            Log.d(TAG, "歌曲艺术家: " + currentSong.getArtist());
            Log.d(TAG, "歌曲专辑: " + currentSong.getAlbum());
            Log.d(TAG, "歌曲路径: " + currentSong.getPath());
            Log.d(TAG, "歌曲时长: " + currentSong.getDuration());
            
            // 更新歌曲信息
            updateSongInfo(currentSong);
            
            // 更新收藏按钮
            updateFavoriteButton();
            
            // 加载歌词
            loadLyrics(currentSong);
        } else {
            Log.w(TAG, "当前歌曲为null，使用默认状态");
            updateDefaultState();
        }
    }

    /**
     * 更新歌曲信息
     */
    private void updateSongInfo(Song song) {
        if (song == null) return;
        
        // 更新歌曲标题和艺术家
        binding.fullSongName.setText(song.getTitle());
        binding.fullSongArtist.setText(song.getArtist());
        
        // 更新专辑封面
        updateAlbumCover(song);
        
        // 更新时长
        binding.txtTotalTime.setText(TimeUtils.formatDuration(song.getDuration()));
        
        currentSongId = song.getId();
    }

    /**
     * 更新播放按钮
     */
    private void updatePlayButton(int playbackState) {
        switch (playbackState) {
            case Player.STATE_READY:
                if (mediaController != null && mediaController.isPlaying()) {
                    binding.btnPlayPause.setImageResource(R.drawable.ic_pause);
                } else {
                    binding.btnPlayPause.setImageResource(R.drawable.ic_play);
                }
                break;
            case Player.STATE_BUFFERING:
                binding.btnPlayPause.setImageResource(R.drawable.ic_pause);
                break;
            case Player.STATE_IDLE:
            case Player.STATE_ENDED:
            default:
                binding.btnPlayPause.setImageResource(R.drawable.ic_play);
                break;
        }
    }

    /**
     * 更新播放按钮（基于播放状态）
     */
    private void updatePlayButton(boolean isPlaying) {
        if (isPlaying) {
            binding.btnPlayPause.setImageResource(R.drawable.ic_pause);
        } else {
            binding.btnPlayPause.setImageResource(R.drawable.ic_play);
        }
    }

    /**
     * 更新播放模式按钮
     */
    private void updatePlayModeButton(int repeatMode) {
        currentPlayMode = repeatMode;
        
        switch (repeatMode) {
            case Player.REPEAT_MODE_OFF:
                binding.btnRepeat.setImageResource(R.drawable.ic_repeat);
                binding.btnRepeat.setColorFilter(defaultTextSecondaryColor);
                break;
            case Player.REPEAT_MODE_ONE:
                binding.btnRepeat.setImageResource(R.drawable.ic_repeat_one);
                binding.btnRepeat.setColorFilter(dominantColor);
                break;
            case Player.REPEAT_MODE_ALL:
                binding.btnRepeat.setImageResource(R.drawable.ic_repeat);
                binding.btnRepeat.setColorFilter(dominantColor);
                break;
        }
    }

    /**
     * 更新随机播放按钮
     */
    private void updateShuffleButton(boolean shuffleModeEnabled) {
        if (shuffleModeEnabled) {
            binding.btnShuffle.setColorFilter(dominantColor);
        } else {
            binding.btnShuffle.setColorFilter(defaultTextSecondaryColor);
        }
    }

    /**
     * 更新收藏按钮
     */
    private void updateFavoriteButton() {
        if (controllerHelper == null) return;

        Song currentSong = controllerHelper.getCurrentSong();
        if (currentSong != null) {
            boolean isFavorite = favoriteManager.isFavorite(currentSong.getId());
            if (isFavorite) {
                binding.btnFavorite.setImageResource(R.drawable.ic_favorite_on);
                binding.btnFavorite.setColorFilter(defaultFavoriteColor);
            } else {
                binding.btnFavorite.setImageResource(R.drawable.ic_favorite_off);
                binding.btnFavorite.setColorFilter(defaultTextSecondaryColor);
            }
        }
    }

    /**
     * 更新默认状态
     */
    private void updateDefaultState() {
        Log.d(TAG, "更新为默认状态");
        binding.fullSongName.setText("暂无歌曲");
        binding.fullSongArtist.setText("未知艺术家");
        binding.txtCurrentTime.setText("00:00");
        binding.txtTotalTime.setText("00:00");
        binding.seekBarProgress.setProgress(0);
        binding.btnPlayPause.setImageResource(R.drawable.ic_play);
        
        // 重置背景
        resetBackground();
        
        // 停止进度更新
        stopProgressUpdates();
        
        // 清空歌词
        currentLyrics.clear();
        if (isLyricsVisible) {
            binding.lyricsView.updateLyrics(currentLyrics);
        }
        
        currentSongId = -1;
        Log.d(TAG, "默认状态设置完成");
    }

    // ===========================================
    // 进度更新
    // ===========================================

    /**
     * 开始进度更新
     */
    private void startProgressUpdates() {
        if (progressUpdateRunnable == null) {
            progressUpdateRunnable = new Runnable() {
                @Override
                public void run() {
                    updateProgressFromPlayer();
                    progressUpdateHandler.postDelayed(this, 1000); // 每秒更新一次
                }
            };
        }
        
        progressUpdateHandler.removeCallbacks(progressUpdateRunnable);
        progressUpdateHandler.post(progressUpdateRunnable);
    }

    /**
     * 停止进度更新
     */
    private void stopProgressUpdates() {
        if (progressUpdateRunnable != null) {
            progressUpdateHandler.removeCallbacks(progressUpdateRunnable);
        }
    }

    /**
     * 从播放器更新进度
     */
    private void updateProgressFromPlayer() {
        if (mediaController == null || isUserSeeking) return;

        try {
            long currentPosition = mediaController.getCurrentPosition();
            long duration = mediaController.getDuration();
            
            if (duration > 0) {
                updateProgress((int) currentPosition, (int) duration);
            }
        } catch (Exception e) {
            Log.e(TAG, "Error updating progress from player", e);
        }
    }

    /**
     * 更新进度
     */
    private void updateProgress(int currentPosition, int duration) {
        if (isUserSeeking) return;

        // 更新进度条
        if (duration > 0) {
            int progress = (int) ((currentPosition * 100L) / duration);
            binding.seekBarProgress.setProgress(progress);
        }

        // 更新时间显示
        binding.txtCurrentTime.setText(TimeUtils.formatDuration(currentPosition));
        
        // 更新歌词高亮
        if (isLyricsVisible && !currentLyrics.isEmpty()) {
            binding.lyricsView.updateCurrentTime((long) currentPosition);
        }
    }

    /**
     * 更新专辑封面
     */
    private void updateAlbumCover(Song song) {
        if (song == null) return;
        
        // 加载专辑封面
        GlideUtils.loadAlbumCover(context, song.getAlbumId(), binding.fullSheetCover);
        
        // 使用Glide加载专辑封面并提取颜色
        Glide.with(context)
                .asBitmap()
                .load(GlideUtils.getAlbumCoverUri(song.getAlbumId()))
                .into(new CustomTarget<Bitmap>() {
                    @Override
                    public void onResourceReady(@NonNull Bitmap resource, @Nullable Transition<? super Bitmap> transition) {
                        currentAlbumBitmap = resource;
                        
                        // 提取颜色
                        dominantColor = ColorExtractor.extractDominantColor(resource);
                        darkColor = ColorExtractor.extractDarkColor(resource);
                        
                        // 更新背景罩层
                        updateBackgroundOverlay();
                        
                        // 更新按钮颜色
                        updateButtonColors();
                    }
                    
                    @Override
                    public void onLoadCleared(@Nullable android.graphics.drawable.Drawable placeholder) {
                        // 清理资源
                    }
                });
    }
    
    /**
     * 重置背景
     */
    private void resetBackground() {
        currentBackgroundType = BACKGROUND_TYPE_GRADIENT;
        dominantColor = Color.parseColor("#3353BE");
        darkColor = Color.parseColor("#1A237E");
        updateBackgroundOverlay();
    }
    


    // ===========================================
    // 点击事件设置
    // ===========================================

    /**
     * 设置点击事件
     */
    private void setupClickListeners() {
        // 播放/暂停按钮
        binding.btnPlayPause.setOnClickListener(v -> {
            Log.d(TAG, "播放/暂停按钮被点击");
            if (mediaController != null) {
                Log.d(TAG, "MediaController不为null，当前播放状态: " + mediaController.isPlaying());
                Log.d(TAG, "当前播放状态: " + mediaController.getPlaybackState());
                
                if (mediaController.isPlaying()) {
                    Log.d(TAG, "正在播放，执行暂停操作");
                    mediaController.pause();
                } else {
                    Log.d(TAG, "未在播放，执行播放操作");
                    Log.d(TAG, "当前媒体项: " + (mediaController.getCurrentMediaItem() != null ? 
                        mediaController.getCurrentMediaItem().mediaId : "null"));
                    Log.d(TAG, "播放列表大小: " + mediaController.getMediaItemCount());
                    
                    // 检查是否有媒体项
                    if (mediaController.getMediaItemCount() == 0) {
                        Log.w(TAG, "播放列表为空，无法播放");
                        ToastUtils.showToast(context, "播放列表为空");
                        return;
                    }
                    
                    // 检查当前媒体项
                    if (mediaController.getCurrentMediaItem() == null) {
                        Log.w(TAG, "当前媒体项为null，尝试播放第一个媒体项");
                        if (mediaController.getMediaItemCount() > 0) {
                            mediaController.seekTo(0, 0);
                        }
                    }
                    
                    mediaController.play();
                    Log.d(TAG, "播放命令已发送");
                }
            } else {
                Log.e(TAG, "MediaController为null，无法执行播放操作");
                ToastUtils.showToast(context, "播放器未准备好");
            }
        });

        // 上一首按钮
        binding.btnPrevious.setOnClickListener(v -> {
            Log.d(TAG, "上一首按钮被点击");
            if (mediaController != null) {
                Log.d(TAG, "执行上一首操作");
                mediaController.seekToPrevious();
            } else {
                Log.e(TAG, "MediaController为null，无法执行上一首操作");
            }
        });

        // 下一首按钮
        binding.btnNext.setOnClickListener(v -> {
            Log.d(TAG, "下一首按钮被点击");
            if (mediaController != null) {
                Log.d(TAG, "执行下一首操作");
                mediaController.seekToNext();
            } else {
                Log.e(TAG, "MediaController为null，无法执行下一首操作");
            }
        });

        // 随机播放按钮
        binding.btnShuffle.setOnClickListener(v -> {
            Log.d(TAG, "随机播放按钮被点击");
            if (mediaController != null) {
                boolean currentShuffle = mediaController.getShuffleModeEnabled();
                Log.d(TAG, "当前随机播放状态: " + currentShuffle + "，切换为: " + !currentShuffle);
                mediaController.setShuffleModeEnabled(!currentShuffle);
            } else {
                Log.e(TAG, "MediaController为null，无法切换随机播放");
            }
        });

        // 循环播放按钮
        binding.btnRepeat.setOnClickListener(v -> {
            Log.d(TAG, "循环播放按钮被点击");
            if (mediaController != null) {
                int currentRepeatMode = mediaController.getRepeatMode();
                Log.d(TAG, "当前循环播放模式: " + currentRepeatMode);
                switch (currentRepeatMode) {
                    case Player.REPEAT_MODE_OFF:
                        Log.d(TAG, "切换到单曲循环");
                        mediaController.setRepeatMode(Player.REPEAT_MODE_ONE);
                        break;
                    case Player.REPEAT_MODE_ONE:
                        Log.d(TAG, "切换到列表循环");
                        mediaController.setRepeatMode(Player.REPEAT_MODE_ALL);
                        break;
                    case Player.REPEAT_MODE_ALL:
                        Log.d(TAG, "切换到关闭循环");
                        mediaController.setRepeatMode(Player.REPEAT_MODE_OFF);
                        break;
                }
            } else {
                Log.e(TAG, "MediaController为null，无法切换循环播放");
            }
        });

        // 收藏按钮
        binding.btnFavorite.setOnClickListener(v -> {
            Log.d(TAG, "收藏按钮被点击");
            if (controllerHelper == null) {
                Log.e(TAG, "MediaControllerHelper为null，无法操作收藏");
                return;
            }

            Song currentSong = controllerHelper.getCurrentSong();
            if (currentSong != null) {
                Log.d(TAG, "当前歌曲: " + currentSong.getTitle());
                boolean success = favoriteManager.toggleFavorite(currentSong.getId());
                Log.d(TAG, "切换收藏状态结果: " + success);
                if (success) {
                    updateFavoriteButton();
                } else {
                    Log.e(TAG, "切换收藏状态失败");
                    ToastUtils.showToast(context, "操作失败");
                }
            } else {
                Log.w(TAG, "当前歌曲为null，无法操作收藏");
            }
        });

        // 歌词按钮
        binding.btnLyrics.setOnClickListener(v -> {
            Log.d(TAG, "歌词按钮被点击，当前歌词显示状态: " + isLyricsVisible);
            toggleLyrics();
        });

        // 睡眠模式按钮
        binding.btnSleepMode.setOnClickListener(v -> {
            Log.d(TAG, "睡眠模式按钮被点击");
            // 暂时只显示提示
            ToastUtils.showToast(context, "睡眠模式功能开发中");
        });

        // 播放列表按钮
        binding.btnPlaylist.setOnClickListener(v -> {
            Log.d(TAG, "播放列表按钮被点击");
            // 暂时只显示提示
            ToastUtils.showToast(context, "播放列表功能开发中");
        });

        // 更多按钮
        binding.btnMore.setOnClickListener(v -> {
            Log.d(TAG, "更多按钮被点击");
            // 暂时只显示提示
            ToastUtils.showToast(context, "更多功能开发中");
        });

        // 专辑封面点击事件
        binding.albumCoverFrame.setOnClickListener(v -> {
            Log.d(TAG, "专辑封面被点击，当前歌词显示状态: " + isLyricsVisible);
            if (isLyricsVisible) {
                Log.d(TAG, "歌词已显示，切换背景类型");
                // 如果歌词已显示，切换背景类型
                switchBackgroundType();
            } else {
                Log.d(TAG, "歌词未显示，显示歌词");
                // 如果歌词未显示，显示歌词
                toggleLyrics();
            }
        });
    }

    /**
     * 切换背景类型
     */
    private void switchBackgroundType() {
        currentBackgroundType = (currentBackgroundType + 1) % 4;
        updateBackgroundOverlay();
    }

    /**
     * 切换歌词显示/隐藏
     */
    private void toggleLyrics() {
        isLyricsVisible = !isLyricsVisible;
        updateLyricsVisibility();
        updateLyricsButton();
    }

    /**
     * 更新歌词显示状态
     */
    private void updateLyricsVisibility() {
        if (isLyricsVisible) {
            showLyrics();
        } else {
            hideLyrics();
        }
    }

    /**
     * 显示歌词
     */
    private void showLyrics() {
        // 加载动画
        Animation fadeInAnimation = AnimationUtils.loadAnimation(context, R.anim.fade_in);
        Animation fadeOutAnimation = AnimationUtils.loadAnimation(context, R.anim.fade_out);
        fadeInAnimation.setDuration(100);
        fadeOutAnimation.setDuration(100);

        // 设置动画监听器
        fadeOutAnimation.setAnimationListener(new Animation.AnimationListener() {
            @Override
            public void onAnimationStart(Animation animation) {}

            @Override
            public void onAnimationEnd(Animation animation) {
                // 淡出动画结束后，隐藏封面和信息，显示歌词
                binding.albumCoverFrame.setVisibility(View.INVISIBLE);
                binding.songInfoLayout.setVisibility(View.INVISIBLE);
                binding.lyricsView.setVisibility(View.VISIBLE);
                binding.lyricsView.startAnimation(fadeInAnimation);
                
                // 检查是否需要加载歌词
                binding.lyricsView.post(() -> {
                    if (controllerHelper != null) {
                        Song currentSong = controllerHelper.getCurrentSong();
                        if (currentSong != null && currentSong.getId() != currentSongId) {
                            // 如果是新歌曲，需要重新加载歌词
                            loadLyrics(currentSong);
                        }
                        // 如果是同一首歌，直接显示歌词，不需要重新加载
                    }
                });
            }

            @Override
            public void onAnimationRepeat(Animation animation) {}
        });

        // 开始淡出动画
        binding.albumCoverFrame.startAnimation(fadeOutAnimation);
        binding.songInfoLayout.startAnimation(fadeOutAnimation);
    }

    /**
     * 隐藏歌词
     */
    private void hideLyrics() {
        // 加载动画
        Animation fadeInAnimation = AnimationUtils.loadAnimation(context, R.anim.fade_in);
        Animation fadeOutAnimation = AnimationUtils.loadAnimation(context, R.anim.fade_out);
        fadeInAnimation.setDuration(100);
        fadeOutAnimation.setDuration(100);
        // 设置动画监听器
        fadeOutAnimation.setAnimationListener(new Animation.AnimationListener() {
            @Override
            public void onAnimationStart(Animation animation) {}

            @Override
            public void onAnimationEnd(Animation animation) {
                // 淡出动画结束后，隐藏歌词，显示封面和信息
                binding.lyricsView.setVisibility(View.GONE);
                binding.albumCoverFrame.setVisibility(View.VISIBLE);
                binding.songInfoLayout.setVisibility(View.VISIBLE);
                binding.albumCoverFrame.startAnimation(fadeInAnimation);
                binding.songInfoLayout.startAnimation(fadeInAnimation);
            }

            @Override
            public void onAnimationRepeat(Animation animation) {}
        });

        // 开始淡出动画
        binding.lyricsView.startAnimation(fadeOutAnimation);
    }

    /**
     * 更新歌词按钮状态
     */
    private void updateLyricsButton() {
        if (isLyricsVisible) {
            binding.btnLyrics.setImageResource(R.drawable.ic_lyrics_on);
        } else {
            binding.btnLyrics.setImageResource(R.drawable.ic_lyrics_off);
        }
    }

    /**
     * 加载歌词
     */
    private void loadLyrics(Song song) {
        if (controllerHelper == null) return;

        // 从歌曲文件解析歌词
        List<LyricLine> lyrics = LyricsParser.parseLyricsFromSong(context, song.getPath());

        // 如果没有找到歌词，显示"暂无歌词"
        if (lyrics.isEmpty()) {
            lyrics.add(new LyricLine(0, getString(R.string.no_lyrics)));
        }

        currentLyrics.clear();
        currentLyrics.addAll(lyrics);
        binding.lyricsView.setLyrics(currentLyrics);

        // 更新当前播放位置的歌词
        long currentPosition = controllerHelper.getCurrentPosition();
        binding.lyricsView.updateCurrentPosition(currentPosition);

        // 打印调试信息
        System.out.println("歌词加载完成，共 " + lyrics.size() + " 行");
    }


    /**
     * 更新背景罩层
     */
    private void updateBackgroundOverlay() {
        if (!(getActivity() instanceof MainActivity)) return;

        MainActivity mainActivity = (MainActivity) getActivity();
        Song currentSong = mainActivity.getCurrentSong();
        if (currentSong == null) {
            // 没有歌曲时显示渐变背景
            switch (currentBackgroundType) {
                case BACKGROUND_TYPE_DEFAULT:
                    binding.backgroundOverlay.setImageDrawable(null);
                    binding.backgroundOverlay.setBackground(null);
                    break;
                case BACKGROUND_TYPE_SOLID:
                    binding.backgroundOverlay.setImageDrawable(null);
                    binding.backgroundOverlay.setBackgroundColor(dominantColor);
                    break;
                case BACKGROUND_TYPE_GRADIENT:
                    binding.backgroundOverlay.setImageDrawable(null);
                    // 使用默认渐变色
                    GradientDrawable gradientDrawable = new GradientDrawable(
                            GradientDrawable.Orientation.TOP_BOTTOM,
                            new int[]{darkColor, Color.BLACK}
                    );
                    binding.backgroundOverlay.setBackground(gradientDrawable);
                    break;
                case BACKGROUND_TYPE_BLUR:
                    binding.backgroundOverlay.setImageDrawable(null);
                    binding.backgroundOverlay.setBackground(null);
                    break;
            }
            updateUIColors();
            return;
        }

        switch (currentBackgroundType) {
            case BACKGROUND_TYPE_DEFAULT:
                binding.backgroundOverlay.setImageDrawable(null);
                binding.backgroundOverlay.setBackground(null);
                break;
            case BACKGROUND_TYPE_SOLID:
                updateSolidBackground(currentSong);
                break;
            case BACKGROUND_TYPE_GRADIENT:
                updateGradientBackground(currentSong);
                break;
            case BACKGROUND_TYPE_BLUR:
                updateBlurBackground(currentSong);
                break;
        }

        updateUIColors();

    }

    /**
     * 更新UI颜色
     */
    private void updateUIColors() {
        boolean isDefault = currentBackgroundType == BACKGROUND_TYPE_DEFAULT;
        int textPrimaryColor = isDefault ? defaultTextPrimaryColor : whiteColor;
        int textSecondaryColor = isDefault ? defaultTextSecondaryColor : grayColor;

        // 更新歌曲名称和艺术家颜色
        binding.fullSongName.setTextColor(textPrimaryColor);
        binding.fullSongArtist.setTextColor(textPrimaryColor);

        // 更新音质信息颜色
        binding.txtFormat.setTextColor(textPrimaryColor);
        binding.txtBitrate.setTextColor(textPrimaryColor);
        binding.txtSampleRate.setTextColor(textPrimaryColor);

        // 更新时间显示颜色
        binding.txtCurrentTime.setTextColor(textSecondaryColor);
        binding.txtTotalTime.setTextColor(textSecondaryColor);

        // 更新质量信息分隔符颜色
        ViewGroup qualityLayout = binding.qualityLayout;
        for (int i = 0; i < qualityLayout.getChildCount(); i++) {
            View child = qualityLayout.getChildAt(i);
            if (child instanceof TextView) {
                TextView textView = (TextView) child;
                if ("•".equals(textView.getText().toString())) {
                    textView.setTextColor(textPrimaryColor);
                }
            }
        }

        // 更新SeekBar颜色
        updateSeekBarColors(isDefault);

        // 更新按钮颜色
        updateButtonColors();
    }

    /**
     * 更新SeekBar颜色
     * @param isDefault 是否是默认背景
     */
    private void updateSeekBarColors(boolean isDefault) {
        if (isDefault) {
            // 默认背景下保持原有颜色
            binding.seekBarProgress.getProgressDrawable().setColorFilter(null);
            binding.seekBarProgress.getThumb().setColorFilter(null);
        } else {
            // 非默认背景下设置为白色
            binding.seekBarProgress.getProgressDrawable().setColorFilter(whiteColor,
                    android.graphics.PorterDuff.Mode.SRC_IN);
            binding.seekBarProgress.getThumb().setColorFilter(whiteColor,
                    android.graphics.PorterDuff.Mode.SRC_IN);
        }
    }

    /**
     * 更新按钮颜色
     */
    private void updateButtonColors() {
        boolean isDefault = currentBackgroundType == BACKGROUND_TYPE_DEFAULT;
        int buttonNormalColor = isDefault ? defaultTextSecondaryColor : whiteColor;
        int buttonActiveColor = isDefault ? defaultTextPrimaryColor : whiteColor;

        if (isDefault) {
            binding.btnPlayPause.setColorFilter(whiteColor);
        } else if (currentBackgroundType == BACKGROUND_TYPE_SOLID) {
            binding.btnPlayPause.setColorFilter(dominantColor);
        } else {
            binding.btnPlayPause.setColorFilter(darkColor);

        }


        binding.btnPlayBackground.setCardBackgroundColor(isDefault ? getResources().getColor(R.color.md_theme_primary) : whiteColor);


        binding.btnPrevious.setColorFilter(buttonNormalColor);
        binding.btnNext.setColorFilter(buttonNormalColor);

        // 更新随机播放按钮
        if (mediaController != null && mediaController.getShuffleModeEnabled()) {
            binding.btnShuffle.setColorFilter(buttonActiveColor);
            binding.btnShuffle.setAlpha(1.0f); // 随机播放开启时设置正常透明度
        } else {
            binding.btnShuffle.setColorFilter(buttonNormalColor);
            binding.btnShuffle.setAlpha(0.5f); // 随机播放未开启时设置较低透明度
        }

        // 更新循环播放按钮
        if (currentPlayMode == Player.REPEAT_MODE_ONE || currentPlayMode == Player.REPEAT_MODE_ALL) {
            binding.btnRepeat.setColorFilter(buttonActiveColor);
            binding.btnRepeat.setAlpha(1.0f); // 循环播放开启时设置正常透明度
        } else {
            binding.btnRepeat.setColorFilter(buttonNormalColor);
            binding.btnRepeat.setAlpha(0.5f); // 循环播放未开启时设置较低透明度
        }

        // 更新收藏按钮
        if (controllerHelper != null) {
            Song currentSong = controllerHelper.getCurrentSong();
            if (currentSong != null && favoriteManager.isFavorite(currentSong.getId())) {
                binding.btnFavorite.setColorFilter(isDefault ? defaultFavoriteColor : Color.parseColor("#FF6B9D"));
            } else {
                binding.btnFavorite.setColorFilter(buttonNormalColor);
            }
        }

        // 更新底部控制按钮
        binding.btnLyrics.setColorFilter(buttonNormalColor);
        binding.btnSleepMode.setColorFilter(buttonNormalColor);
        binding.btnPlaylist.setColorFilter(buttonNormalColor);
        binding.btnMore.setColorFilter(buttonNormalColor);
    }

    /**
     * 更新纯色背景
     */
    private void updateSolidBackground(Song song) {
        binding.backgroundOverlay.setImageDrawable(null);
        binding.backgroundOverlay.setBackgroundColor(dominantColor);
    }

    /**
     * 更新渐变背景
     */
    private void updateGradientBackground(Song song) {
        binding.backgroundOverlay.setImageDrawable(null);

        // 创建渐变drawable
        GradientDrawable gradientDrawable = new GradientDrawable(
                GradientDrawable.Orientation.TOP_BOTTOM,
                new int[]{darkColor, Color.BLACK}
        );

        binding.backgroundOverlay.setBackground(gradientDrawable);
    }

    /**
     * 更新模糊背景
     */
    private void updateBlurBackground(Song song) {
        binding.backgroundOverlay.setBackground(null);

        if (currentAlbumBitmap != null) {
            // 先缩放图片以提高性能
            Bitmap scaledBitmap = BlurUtils.scaleBitmap(currentAlbumBitmap, 0.25f);

            // 应用高斯模糊
            Bitmap blurredBitmap = BlurUtils.blur(context, scaledBitmap, 20.0f);

            if (blurredBitmap != null) {
                // 让模糊背景更暗
                Bitmap darkenedBitmap = darkenBitmap(blurredBitmap);
                binding.backgroundOverlay.setImageBitmap(darkenedBitmap);
            } else {
                // 如果模糊失败，使用fastBlur作为备用方案
                blurredBitmap = BlurUtils.fastBlur(scaledBitmap, 15);
                if (blurredBitmap != null) {
                    // 让模糊背景更暗
                    Bitmap darkenedBitmap = darkenBitmap(blurredBitmap);
                    binding.backgroundOverlay.setImageBitmap(darkenedBitmap);
                }
            }
        } else {
            // 如果没有专辑封面，使用默认封面
            GlideUtils.loadAlbumCover(context, song.getAlbumId(), binding.backgroundOverlay);
        }
    }

    /**
     * 让图片变暗
     */
    private Bitmap darkenBitmap(Bitmap bitmap) {
        if (bitmap == null) return null;

        // 创建可变的Bitmap副本
        Bitmap darkBitmap = bitmap.copy(Bitmap.Config.ARGB_8888, true);

        // 创建一个半透明的黑色罩层
        Canvas canvas = new Canvas(darkBitmap);
        Paint paint = new Paint();
        paint.setColor(Color.BLACK);
        paint.setAlpha(120); // 透明度：120/255 ≈ 47%
        canvas.drawRect(0, 0, darkBitmap.getWidth(), darkBitmap.getHeight(), paint);

        return darkBitmap;
    }

    private void setupProgressListeners() {
        // SeekBar进度监听
        binding.seekBarProgress.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                if (fromUser && mediaController != null) {
                    long duration = mediaController.getDuration();
                    if (duration > 0) {
                        long position = (long) ((float) progress / 100 * duration);
                        mediaController.seekTo(position);
                    }
                }
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
                isUserSeeking = true;
            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                isUserSeeking = false;
            }
        });
    }

    @Override
    public void onStart() {
        super.onStart();
        // 注册MediaController监听器
        if (mediaController != null) {
            mediaController.addListener(this);
        }
    }

    @Override
    public void onStop() {
        super.onStop();
        // 移除MediaController监听器
        if (mediaController != null) {
            mediaController.removeListener(this);
        }
    }

    /**
     * 开始播放歌曲
     */
    public void playSong(Song song) {
        // 这个方法应该通过MainActivity调用，不应该直接在Fragment中创建MediaItem
        // 委托给MainActivity处理
        if (getActivity() instanceof MainActivity) {
            ((MainActivity) getActivity()).playSong(song);
        }
    }

    /**
     * 设置播放列表
     */
    public void setPlaylist(java.util.List<Song> songs) {
        // 委托给MainActivity处理
        if (getActivity() instanceof MainActivity) {
            ((MainActivity) getActivity()).setPlaylist(songs);
        }
    }

    /**
     * 获取当前播放状态
     */
    public boolean isPlaying() {
        if (mediaController != null) {
            return mediaController.isPlaying();
        }
        return false;
    }

    /**
     * 获取当前播放的歌曲
     */
    public Song getCurrentSong() {
        if (controllerHelper != null) {
            return controllerHelper.getCurrentSong();
        }
        return null;
    }

    /**
     * 设置播放模式
     */
    public void setPlayMode(int playMode) {
        if (mediaController != null) {
            mediaController.setRepeatMode(playMode);
        }
    }

    /**
     * 获取播放模式
     */
    public int getPlayMode() {
        if (mediaController != null) {
            return mediaController.getRepeatMode();
        }
        return Player.REPEAT_MODE_OFF;
    }

    @Override
    public boolean autoHideBottomNavigation() {
        return false;
    }
} 
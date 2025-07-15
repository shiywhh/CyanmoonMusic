package com.magicalstory.music.player;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.PorterDuff;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.SeekBar;
import android.widget.TextView;
import android.database.Cursor;
import android.media.MediaMetadataRetriever;
import android.provider.MediaStore;

import java.io.File;


import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import com.bumptech.glide.Glide;
import com.bumptech.glide.request.target.CustomTarget;
import com.bumptech.glide.request.transition.Transition;
import com.google.android.material.card.MaterialCardView;
import com.magicalstory.music.MainActivity;
import com.magicalstory.music.R;
import com.magicalstory.music.base.BaseFragment;
import com.magicalstory.music.databinding.FragmentFullPlayerBinding;
import com.magicalstory.music.model.Song;
import com.magicalstory.music.service.MusicService;
import com.magicalstory.music.utils.app.ToastUtils;
import com.magicalstory.music.utils.favorite.FavoriteManager;
import com.magicalstory.music.utils.glide.BlurUtils;
import com.magicalstory.music.utils.glide.ColorExtractor;
import com.magicalstory.music.utils.glide.GlideUtils;
import com.magicalstory.music.utils.text.TimeUtils;

/**
 * 全屏播放器Fragment
 */
public class FullPlayerFragment extends BaseFragment<FragmentFullPlayerBinding> {

    private static final String TAG = "FullPlayerFragment";

    // 背景类型常量
    private static final int BACKGROUND_TYPE_DEFAULT = 0;
    private static final int BACKGROUND_TYPE_SOLID = 1;
    private static final int BACKGROUND_TYPE_GRADIENT = 2;
    private static final int BACKGROUND_TYPE_BLUR = 3;

    private boolean isUserSeeking = false;
    private FavoriteManager favoriteManager;

    // 播放模式
    private int currentPlayMode = MusicService.PLAY_MODE_SEQUENCE;

    // 背景类型
    private int currentBackgroundType = BACKGROUND_TYPE_GRADIENT;

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

    private BroadcastReceiver musicReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (action == null) return;

            switch (action) {
                case MusicService.ACTION_PLAY_STATE_CHANGED:
                    int playState = intent.getIntExtra("play_state", MusicService.STATE_IDLE);
                    updatePlayButton(playState);
                    break;
                case MusicService.ACTION_SONG_CHANGED:
                    updateSongInfo();
                    break;
                case MusicService.ACTION_PROGRESS_UPDATED:
                    if (!isUserSeeking) {
                        int currentPosition = intent.getIntExtra("current_position", 0);
                        int duration = intent.getIntExtra("duration", 0);
                        updateProgress(currentPosition, duration);
                    }
                    break;
                case MusicService.ACTION_PLAY_MODE_CHANGED:
                    int playMode = intent.getIntExtra("play_mode", MusicService.PLAY_MODE_SEQUENCE);
                    updatePlayModeButton(playMode);
                    break;
            }
        }
    };



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

    @Override
    public void onDestroyView() {
        super.onDestroyView();
    }


    private void setupClickListeners() {
        // 播放/暂停按钮
        binding.btnPlayPause.setOnClickListener(v -> {
            if (getActivity() instanceof MainActivity) {
                MainActivity mainActivity = (MainActivity) getActivity();
                mainActivity.playOrPause();
            }
        });

        // 上一首按钮
        binding.btnPrevious.setOnClickListener(v -> {
            if (getActivity() instanceof MainActivity) {
                MainActivity mainActivity = (MainActivity) getActivity();
                mainActivity.previous();
            }
        });

        // 下一首按钮
        binding.btnNext.setOnClickListener(v -> {
            if (getActivity() instanceof MainActivity) {
                MainActivity mainActivity = (MainActivity) getActivity();
                mainActivity.next();
            }
        });

        // 随机播放按钮
        binding.btnShuffle.setOnClickListener(v -> {
            if (getActivity() instanceof MainActivity) {
                MainActivity mainActivity = (MainActivity) getActivity();
                // 切换随机播放模式
                if (currentPlayMode == MusicService.PLAY_MODE_RANDOM) {
                    // 如果当前是随机播放，切换回顺序播放
                    mainActivity.setPlayMode(MusicService.PLAY_MODE_SEQUENCE);
                } else {
                    // 否则切换到随机播放
                    mainActivity.setPlayMode(MusicService.PLAY_MODE_RANDOM);
                }
            }
        });

        // 循环播放按钮
        binding.btnRepeat.setOnClickListener(v -> {
            if (getActivity() instanceof MainActivity) {
                MainActivity mainActivity = (MainActivity) getActivity();
                // 循环播放模式：顺序播放 -> 单曲循环 -> 列表循环 -> 顺序播放
                switch (currentPlayMode) {
                    case MusicService.PLAY_MODE_SEQUENCE:
                        currentPlayMode = MusicService.PLAY_MODE_SINGLE;
                        break;
                    case MusicService.PLAY_MODE_SINGLE:
                        currentPlayMode = MusicService.PLAY_MODE_LOOP;
                        break;
                    case MusicService.PLAY_MODE_LOOP:
                        currentPlayMode = MusicService.PLAY_MODE_SEQUENCE;
                        break;
                }
                mainActivity.setPlayMode(currentPlayMode);
            }
        });

        // 收藏按钮
        binding.btnFavorite.setOnClickListener(v -> {
            if (getActivity() instanceof MainActivity) {
                MainActivity mainActivity = (MainActivity) getActivity();
                Song currentSong = mainActivity.getCurrentSong();
                if (currentSong != null) {
                    boolean success = favoriteManager.toggleFavorite(currentSong.getId());
                    if (success) {
                        updateFavoriteButton();
                    } else {
                        ToastUtils.showToast(context, "操作失败");
                    }
                }
            }
        });

        // 歌词按钮
        binding.btnLyrics.setOnClickListener(v -> {
            // 暂时只显示提示

        });

        // 睡眠模式按钮
        binding.btnSleepMode.setOnClickListener(v -> {
            // 暂时只显示提示

        });

        // 播放列表按钮
        binding.btnPlaylist.setOnClickListener(v -> {
            // 暂时只显示提示

        });

        // 更多按钮
        binding.btnMore.setOnClickListener(v -> {
            // 暂时只显示提示

        });

        // 专辑封面点击事件
        binding.albumCoverFrame.setOnClickListener(v -> {
            // 切换背景类型
            switchBackgroundType();
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
        if (currentPlayMode == MusicService.PLAY_MODE_RANDOM) {
            binding.btnShuffle.setColorFilter(buttonActiveColor);
            binding.btnShuffle.setAlpha(1.0f); // 随机播放开启时设置正常透明度
        } else {
            binding.btnShuffle.setColorFilter(buttonNormalColor);
            binding.btnShuffle.setAlpha(0.5f); // 随机播放未开启时设置较低透明度
        }

        // 更新循环播放按钮
        if (currentPlayMode == MusicService.PLAY_MODE_SINGLE || currentPlayMode == MusicService.PLAY_MODE_LOOP) {
            binding.btnRepeat.setColorFilter(buttonActiveColor);
            binding.btnRepeat.setAlpha(1.0f); // 循环播放开启时设置正常透明度
        } else {
            binding.btnRepeat.setColorFilter(buttonNormalColor);
            binding.btnRepeat.setAlpha(0.5f); // 循环播放未开启时设置较低透明度
        }

        // 更新收藏按钮
        if (getActivity() instanceof MainActivity) {
            MainActivity mainActivity = (MainActivity) getActivity();
            Song currentSong = mainActivity.getCurrentSong();
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
                if (fromUser && getActivity() instanceof MainActivity) {
                    MainActivity mainActivity = (MainActivity) getActivity();
                    int duration = mainActivity.getDuration();
                    if (duration > 0) {
                        int position = (int) ((float) progress / 100 * duration);
                        binding.txtCurrentTime.setText(TimeUtils.formatTime(position));
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
                if (getActivity() instanceof MainActivity) {
                    MainActivity mainActivity = (MainActivity) getActivity();
                    int duration = mainActivity.getDuration();
                    if (duration > 0) {
                        int position = (int) ((float) seekBar.getProgress() / 100 * duration);
                        mainActivity.seekTo(position);
                    }
                }
            }
        });
    }

    @Override
    public void onStart() {
        super.onStart();
        registerMusicReceiver();
    }

    @Override
    public void onStop() {
        super.onStop();
        unregisterMusicReceiver();
    }

    private void registerMusicReceiver() {
        IntentFilter filter = new IntentFilter();
        filter.addAction(MusicService.ACTION_PLAY_STATE_CHANGED);
        filter.addAction(MusicService.ACTION_SONG_CHANGED);
        filter.addAction(MusicService.ACTION_PROGRESS_UPDATED);
        filter.addAction(MusicService.ACTION_PLAY_MODE_CHANGED);
        LocalBroadcastManager.getInstance(context).registerReceiver(musicReceiver, filter);
    }

    private void unregisterMusicReceiver() {
        LocalBroadcastManager.getInstance(context).unregisterReceiver(musicReceiver);
    }

    private void updateUI() {
        if (getActivity() instanceof MainActivity) {
            MainActivity mainActivity = (MainActivity) getActivity();
            updateSongInfo();
            updatePlayButton(mainActivity.getPlayState());
            updatePlayModeButton(mainActivity.getPlayMode());
            updateProgress(mainActivity.getCurrentPosition(), mainActivity.getDuration());
            updateFavoriteButton();
        }
    }

    private void updateSongInfo() {
        if (!(getActivity() instanceof MainActivity)) return;
        
        MainActivity mainActivity = (MainActivity) getActivity();
        Song currentSong = mainActivity.getCurrentSong();
        if (currentSong != null) {
            binding.fullSongName.setText(currentSong.getTitle());
            binding.fullSongArtist.setText(currentSong.getArtist());

            // 加载封面并提取颜色
            loadAlbumCoverAndExtractColors(currentSong);

            // 更新时长
            binding.txtTotalTime.setText(TimeUtils.formatTime(currentSong.getDuration()));

            // 更新收藏状态
            updateFavoriteButton();

            // 更新音质信息（这里可以从歌曲信息中获取）
            updateQualityInfo(currentSong);
        } else {
            updateDefaultState();
        }
    }

    /**
     * 加载专辑封面并提取颜色
     */
    private void loadAlbumCoverAndExtractColors(Song song) {
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
                    }

                    @Override
                    public void onLoadCleared(@Nullable android.graphics.drawable.Drawable placeholder) {
                        // 清理资源
                    }
                });
    }

    private void updatePlayButton(int playState) {
        switch (playState) {
            case MusicService.STATE_PLAYING:
                binding.btnPlayPause.setImageResource(R.drawable.ic_pause);
                break;
            case MusicService.STATE_PAUSED:
            case MusicService.STATE_STOPPED:
            case MusicService.STATE_IDLE:
                binding.btnPlayPause.setImageResource(R.drawable.ic_play);
                break;
            case MusicService.STATE_PREPARING:
                binding.btnPlayPause.setImageResource(R.drawable.ic_pause);
                // 可以显示加载动画
                break;
            case MusicService.STATE_ERROR:
                binding.btnPlayPause.setImageResource(R.drawable.ic_play);
                break;
        }
    }

    private void updatePlayModeButton(int playMode) {
        currentPlayMode = playMode;
        switch (playMode) {
            case MusicService.PLAY_MODE_SEQUENCE:
                binding.btnRepeat.setImageResource(R.drawable.ic_repeat);
                binding.btnRepeat.setAlpha(0.5f); // 未开启时设置较低透明度
                break;
            case MusicService.PLAY_MODE_SINGLE:
                binding.btnRepeat.setImageResource(R.drawable.ic_repeat_one);
                binding.btnRepeat.setAlpha(1.0f); // 开启时设置正常透明度
                break;
            case MusicService.PLAY_MODE_LOOP:
                binding.btnRepeat.setImageResource(R.drawable.ic_repeat);
                binding.btnRepeat.setAlpha(1.0f); // 开启时设置正常透明度
                break;
            case MusicService.PLAY_MODE_RANDOM:
                binding.btnRepeat.setImageResource(R.drawable.ic_repeat);
                binding.btnRepeat.setAlpha(0.5f); // 随机播放时重复播放按钮设置较低透明度
                break;
        }
        
        // 更新随机播放按钮的透明度
        if (playMode == MusicService.PLAY_MODE_RANDOM) {
            binding.btnShuffle.setAlpha(1.0f); // 随机播放开启时设置正常透明度
        } else {
            binding.btnShuffle.setAlpha(0.5f); // 随机播放未开启时设置较低透明度
        }
        
        // 更新按钮颜色
        updateButtonColors();
    }

    private void updateFavoriteButton() {
        if (getActivity() instanceof MainActivity) {
            MainActivity mainActivity = (MainActivity) getActivity();
            Song currentSong = mainActivity.getCurrentSong();
            if (currentSong != null) {
                boolean isFavorite = favoriteManager.isFavorite(currentSong.getId());
                if (isFavorite) {
                    binding.btnFavorite.setImageResource(R.drawable.ic_favorite_on);
                } else {
                    binding.btnFavorite.setImageResource(R.drawable.ic_favorite_off);
                }
                // 更新按钮颜色
                updateButtonColors();
            }
        }
    }

    private void updateProgress(int currentPosition, int duration) {
        if (duration > 0) {
            int progress = (int) ((float) currentPosition / duration * 100);
            binding.seekBarProgress.setProgress(progress);
        }

        binding.txtCurrentTime.setText(TimeUtils.formatTime(currentPosition));
        binding.txtTotalTime.setText(TimeUtils.formatTime(duration));
    }

    private void updateQualityInfo(Song song) {
        if (song == null) {
            binding.txtFormat.setText(getString(R.string.unknown_format));
            binding.txtBitrate.setText(getString(R.string.unknown_bitrate));
            binding.txtSampleRate.setText(getString(R.string.unknown_sample_rate));
            return;
        }

        // 检查歌曲文件是否存在
        String songPath = song.getPath();
        if (songPath == null || songPath.isEmpty()) {
            binding.txtFormat.setText(getString(R.string.song_not_found));
            binding.txtBitrate.setText("");
            binding.txtSampleRate.setText("");
            return;
        }

        File songFile = new File(songPath);
        if (!songFile.exists()) {
            binding.txtFormat.setText(getString(R.string.song_not_found));
            binding.txtBitrate.setText("");
            binding.txtSampleRate.setText("");
            return;
        }

        // 尝试获取音质信息
        try {
            MediaMetadataRetriever retriever = new MediaMetadataRetriever();
            retriever.setDataSource(songPath);

            // 获取文件格式
            String mimeType = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_MIMETYPE);
            String format = getFormatFromMimeType(mimeType);

            // 获取比特率
            String bitrateStr = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_BITRATE);
            String bitrate = formatBitrate(bitrateStr);

            // 获取采样率
            String sampleRateStr = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_SAMPLERATE);
            String sampleRate = formatSampleRate(sampleRateStr);

            // 设置显示
            binding.txtFormat.setText(format);
            binding.txtBitrate.setText(bitrate);
            binding.txtSampleRate.setText(sampleRate);

            retriever.release();

        } catch (Exception e) {
            // 如果获取音质信息失败，可能是文件损坏
            binding.txtFormat.setText(getString(R.string.song_corrupted));
            binding.txtBitrate.setText("");
            binding.txtSampleRate.setText("");
        }
    }

    /**
     * 从MIME类型获取格式名称
     */
    private String getFormatFromMimeType(String mimeType) {
        if (mimeType == null) return getString(R.string.unknown_format);

        switch (mimeType.toLowerCase()) {
            case "audio/mp3":
            case "audio/mpeg":
                return "MP3";
            case "audio/x-wav":
                return "WAV";
            case "audio/flac":
                return "FLAC";
            case "audio/mp4":
            case "audio/aac":
                return "AAC";
            case "audio/ogg":
                return "OGG";
            case "audio/wav":
                return "WAV";
            case "audio/x-ms-wma":
                return "WMA";
            case "audio/3gpp":
                return "3GP";
            case "audio/amr":
                return "AMR";
            default:
                return getString(R.string.unknown_format);
        }
    }

    /**
     * 格式化比特率显示
     */
    private String formatBitrate(String bitrateStr) {
        if (bitrateStr == null || bitrateStr.isEmpty()) {
            return getString(R.string.unknown_bitrate);
        }

        try {
            long bitrate = Long.parseLong(bitrateStr);
            if (bitrate > 0) {
                // 转换为kb/s
                long kbps = bitrate / 1000;
                return kbps + " kb/s";
            }
        } catch (NumberFormatException e) {
            // 忽略错误
        }

        return getString(R.string.unknown_bitrate);
    }

    /**
     * 格式化采样率显示
     */
    private String formatSampleRate(String sampleRateStr) {
        if (sampleRateStr == null || sampleRateStr.isEmpty()) {
            return getString(R.string.unknown_sample_rate);
        }

        try {
            long sampleRate = Long.parseLong(sampleRateStr);
            if (sampleRate > 0) {
                // 转换为kHz
                if (sampleRate >= 1000) {
                    double kHz = sampleRate / 1000.0;
                    return String.format("%.1f kHz", kHz);
                } else {
                    return sampleRate + " Hz";
                }
            }
        } catch (NumberFormatException e) {
            // 忽略错误
        }

        return getString(R.string.unknown_sample_rate);
    }

    private void updateDefaultState() {
        binding.fullSongName.setText("未播放");
        binding.fullSongArtist.setText("选择歌曲开始播放");
        binding.btnPlayPause.setImageResource(R.drawable.ic_play);
        binding.fullSheetCover.setImageResource(R.drawable.place_holder_album);
        binding.txtCurrentTime.setText("0:00");
        binding.txtTotalTime.setText("0:00");
        binding.seekBarProgress.setProgress(0);

        // 重置收藏按钮
        binding.btnFavorite.setImageResource(R.drawable.ic_favorite_off);
        binding.btnFavorite.setColorFilter(getResources().getColor(R.color.text_secondary));

        // 重置音质信息
        updateQualityInfo(null);

        // 重置背景
        currentBackgroundType = BACKGROUND_TYPE_GRADIENT;
        updateBackgroundOverlay();
    }

    /**
     * 开始播放歌曲
     */
    public void playSong(Song song) {
        if (getActivity() instanceof MainActivity) {
            MainActivity mainActivity = (MainActivity) getActivity();
            mainActivity.playSong(song);
        }
    }

    /**
     * 设置播放列表
     */
    public void setPlaylist(java.util.List<Song> songs) {
        if (getActivity() instanceof MainActivity) {
            MainActivity mainActivity = (MainActivity) getActivity();
            mainActivity.setPlaylist(songs);
        }
    }

    /**
     * 获取当前播放状态
     */
    public boolean isPlaying() {
        if (getActivity() instanceof MainActivity) {
            MainActivity mainActivity = (MainActivity) getActivity();
            return mainActivity.isPlaying();
        }
        return false;
    }

    /**
     * 获取当前播放的歌曲
     */
    public Song getCurrentSong() {
        if (getActivity() instanceof MainActivity) {
            MainActivity mainActivity = (MainActivity) getActivity();
            return mainActivity.getCurrentSong();
        }
        return null;
    }

    /**
     * 设置播放模式
     */
    public void setPlayMode(int playMode) {
        if (getActivity() instanceof MainActivity) {
            MainActivity mainActivity = (MainActivity) getActivity();
            mainActivity.setPlayMode(playMode);
        }
    }

    /**
     * 获取播放模式
     */
    public int getPlayMode() {
        if (getActivity() instanceof MainActivity) {
            MainActivity mainActivity = (MainActivity) getActivity();
            return mainActivity.getPlayMode();
        }
        return MusicService.PLAY_MODE_SEQUENCE;
    }

    @Override
    public boolean autoHideBottomNavigation() {
        return false;
    }
} 
package com.magicalstory.music.player;

import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.IBinder;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import com.google.android.material.card.MaterialCardView;
import com.magicalstory.music.R;
import com.magicalstory.music.databinding.FragmentFullPlayerBinding;
import com.magicalstory.music.model.Song;
import com.magicalstory.music.service.MusicService;
import com.magicalstory.music.utils.favorite.FavoriteManager;
import com.magicalstory.music.utils.glide.GlideUtils;
import com.magicalstory.music.utils.text.TimeUtils;

/**
 * 全屏播放器Fragment
 */
public class FullPlayerFragment extends Fragment {

    private static final String TAG = "FullPlayerFragment";

    private FragmentFullPlayerBinding binding;

    private MusicService musicService;
    private boolean isServiceBound = false;
    private boolean isUserSeeking = false;
    private FavoriteManager favoriteManager;

    // 播放模式
    private int currentPlayMode = MusicService.PLAY_MODE_SEQUENCE;
    private boolean isShuffleMode = false;

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

    private ServiceConnection serviceConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            MusicService.MusicBinder binder = (MusicService.MusicBinder) service;
            musicService = binder.getService();
            isServiceBound = true;
            updateUI();
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            musicService = null;
            isServiceBound = false;
        }
    };

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = FragmentFullPlayerBinding.inflate(inflater, container, false);
        favoriteManager = FavoriteManager.getInstance(getContext());
        initViews();
        return binding.getRoot();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }

    private void initViews() {
        // 设置点击事件
        setupClickListeners();

        // 设置进度条监听
        setupProgressListeners();

        // 设置默认状态
        updateDefaultState();
    }

    private void setupClickListeners() {
        // 播放/暂停按钮
        binding.btnPlayPause.setOnClickListener(v -> {
            if (musicService != null) {
                if (musicService.isPlaying()) {
                    musicService.pause();
                } else {
                    musicService.play();
                }
            }
        });

        // 上一首按钮
        binding.btnPrevious.setOnClickListener(v -> {
            if (musicService != null) {
                musicService.playPrevious();
            }
        });

        // 下一首按钮
        binding.btnNext.setOnClickListener(v -> {
            if (musicService != null) {
                musicService.playNext();
            }
        });

        // 随机播放按钮
        binding.btnShuffle.setOnClickListener(v -> {
            if (musicService != null) {
                isShuffleMode = !isShuffleMode;
                updateShuffleButton();
                // 这里可以设置随机播放逻辑
                Toast.makeText(getContext(), isShuffleMode ? "随机播放已开启" : "随机播放已关闭", Toast.LENGTH_SHORT).show();
            }
        });

        // 循环播放按钮
        binding.btnRepeat.setOnClickListener(v -> {
            if (musicService != null) {
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
                musicService.setPlayMode(currentPlayMode);
                updatePlayModeButton(currentPlayMode);
            }
        });

        // 收藏按钮
        binding.btnFavorite.setOnClickListener(v -> {
            if (musicService != null) {
                Song currentSong = musicService.getCurrentSong();
                if (currentSong != null) {
                    boolean success = favoriteManager.toggleFavorite(currentSong.getId());
                    if (success) {
                        updateFavoriteButton();
                        boolean isFavorite = favoriteManager.isFavorite(currentSong.getId());
                        Toast.makeText(getContext(), isFavorite ? "已添加到收藏" : "已从收藏移除", Toast.LENGTH_SHORT).show();
                    }
                }
            }
        });

        // 歌词按钮
        binding.btnLyrics.setOnClickListener(v -> {
            // 暂时只显示提示
            Toast.makeText(getContext(), "歌词功能即将推出", Toast.LENGTH_SHORT).show();
        });

        // 睡眠模式按钮
        binding.btnSleepMode.setOnClickListener(v -> {
            // 暂时只显示提示
            Toast.makeText(getContext(), "睡眠模式功能即将推出", Toast.LENGTH_SHORT).show();
        });

        // 播放列表按钮
        binding.btnPlaylist.setOnClickListener(v -> {
            // 暂时只显示提示
            Toast.makeText(getContext(), "播放列表功能即将推出", Toast.LENGTH_SHORT).show();
        });

        // 更多按钮
        binding.btnMore.setOnClickListener(v -> {
            // 暂时只显示提示
            Toast.makeText(getContext(), "更多功能即将推出", Toast.LENGTH_SHORT).show();
        });

        // 专辑封面点击事件
        binding.albumCoverFrame.setOnClickListener(v -> {
            // 可以添加封面点击效果或功能
        });
    }

    private void setupProgressListeners() {
        // SeekBar进度监听
        binding.seekBarProgress.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                if (fromUser && musicService != null) {
                    int duration = musicService.getDuration();
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
                if (musicService != null) {
                    int duration = musicService.getDuration();
                    if (duration > 0) {
                        int position = (int) ((float) seekBar.getProgress() / 100 * duration);
                        musicService.seekTo(position);
                    }
                }
            }
        });
    }

    @Override
    public void onStart() {
        super.onStart();
        bindMusicService();
        registerMusicReceiver();
    }

    @Override
    public void onStop() {
        super.onStop();
        unbindMusicService();
        unregisterMusicReceiver();
    }

    private void bindMusicService() {
        Intent intent = new Intent(getContext(), MusicService.class);
        getContext().bindService(intent, serviceConnection, Context.BIND_AUTO_CREATE);
    }

    private void unbindMusicService() {
        if (isServiceBound) {
            getContext().unbindService(serviceConnection);
            isServiceBound = false;
        }
    }

    private void registerMusicReceiver() {
        IntentFilter filter = new IntentFilter();
        filter.addAction(MusicService.ACTION_PLAY_STATE_CHANGED);
        filter.addAction(MusicService.ACTION_SONG_CHANGED);
        filter.addAction(MusicService.ACTION_PROGRESS_UPDATED);
        filter.addAction(MusicService.ACTION_PLAY_MODE_CHANGED);
        LocalBroadcastManager.getInstance(getContext()).registerReceiver(musicReceiver, filter);
    }

    private void unregisterMusicReceiver() {
        LocalBroadcastManager.getInstance(getContext()).unregisterReceiver(musicReceiver);
    }

    private void updateUI() {
        if (musicService != null) {
            updateSongInfo();
            updatePlayButton(musicService.getPlayState());
            updatePlayModeButton(musicService.getPlayMode());
            updateProgress(musicService.getCurrentPosition(), musicService.getDuration());
            updateFavoriteButton();
        }
    }

    private void updateSongInfo() {
        if (musicService == null) return;

        Song currentSong = musicService.getCurrentSong();
        if (currentSong != null) {
            binding.fullSongName.setText(currentSong.getTitle());
            binding.fullSongArtist.setText(currentSong.getArtist());

            // 加载封面
            GlideUtils.loadAlbumCover(getContext(), currentSong.getAlbumId(), binding.fullSheetCover);

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
                binding.btnRepeat.setColorFilter(getResources().getColor(R.color.gray));
                break;
            case MusicService.PLAY_MODE_SINGLE:
                binding.btnRepeat.setImageResource(R.drawable.ic_repeat_one);
                binding.btnRepeat.setColorFilter(getResources().getColor(R.color.primary_color));
                break;
            case MusicService.PLAY_MODE_LOOP:
                binding.btnRepeat.setImageResource(R.drawable.ic_repeat);
                binding.btnRepeat.setColorFilter(getResources().getColor(R.color.primary_color));
                break;
        }
    }

    private void updateShuffleButton() {
        if (isShuffleMode) {
            binding.btnShuffle.setColorFilter(getResources().getColor(R.color.primary_color));
        } else {
            binding.btnShuffle.setColorFilter(getResources().getColor(R.color.gray));
        }
    }

    private void updateFavoriteButton() {
        if (musicService != null) {
            Song currentSong = musicService.getCurrentSong();
            if (currentSong != null) {
                boolean isFavorite = favoriteManager.isFavorite(currentSong.getId());
                if (isFavorite) {
                    binding.btnFavorite.setImageResource(R.drawable.ic_favorite_on);
                    binding.btnFavorite.setColorFilter(getResources().getColor(R.color.favorite_color));
                } else {
                    binding.btnFavorite.setImageResource(R.drawable.ic_favorite_off);
                    binding.btnFavorite.setColorFilter(getResources().getColor(R.color.gray));
                }
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
        // 这里可以根据歌曲信息显示音质信息
        // 目前显示默认值，可以根据实际需求修改
        binding.txtFormat.setText("FLAC");
        binding.txtBitrate.setText("848 kb/s");
        binding.txtSampleRate.setText("44.1 kHz");
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
        binding.btnFavorite.setColorFilter(getResources().getColor(R.color.gray));
    }

    /**
     * 开始播放歌曲
     */
    public void playSong(Song song) {
        if (musicService != null) {
            musicService.playSong(song);
        } else {
            // 如果服务没有绑定，启动服务
            Intent intent = new Intent(getContext(), MusicService.class);
            getContext().startService(intent);
            bindMusicService();
        }
    }

    /**
     * 设置播放列表
     */
    public void setPlaylist(java.util.List<Song> songs) {
        if (musicService != null) {
            musicService.setPlaylist(songs);
        }
    }

    /**
     * 获取当前播放状态
     */
    public boolean isPlaying() {
        return musicService != null && musicService.isPlaying();
    }

    /**
     * 获取当前播放的歌曲
     */
    public Song getCurrentSong() {
        return musicService != null ? musicService.getCurrentSong() : null;
    }

    /**
     * 设置播放模式
     */
    public void setPlayMode(int playMode) {
        if (musicService != null) {
            musicService.setPlayMode(playMode);
        }
    }

    /**
     * 获取播放模式
     */
    public int getPlayMode() {
        return musicService != null ? musicService.getPlayMode() : MusicService.PLAY_MODE_SEQUENCE;
    }
} 
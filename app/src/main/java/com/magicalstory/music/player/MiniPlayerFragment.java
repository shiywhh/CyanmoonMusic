package com.magicalstory.music.player;

import android.animation.ObjectAnimator;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.PagerSnapHelper;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.imageview.ShapeableImageView;
import com.magicalstory.music.MainActivity;
import com.magicalstory.music.R;
import com.magicalstory.music.base.BaseFragment;
import com.magicalstory.music.databinding.FragmentMiniPlayerBinding;
import com.magicalstory.music.model.Song;
import com.magicalstory.music.service.MusicService;
import com.magicalstory.music.utils.glide.GlideUtils;
import com.magicalstory.music.utils.text.TimeUtils;

import java.util.ArrayList;
import java.util.List;

/**
 * Mini播放器Fragment
 */
public class MiniPlayerFragment extends BaseFragment<FragmentMiniPlayerBinding> {

    private static final String TAG = "MiniPlayerFragment";
    private static final int PLAY_DELAY_MS = 300; // 延迟播放时间（毫秒）

    // RecyclerView 相关
    private MiniPlayerAdapter miniPlayerAdapter;
    private LinearLayoutManager layoutManager;
    private PagerSnapHelper pagerSnapHelper;
    private List<Song> playlist = new ArrayList<>();
    private int currentPosition = 0;
    private boolean isUserScrolling = false;

    // 进度条相关
    private ProgressBar progressBar;
    private ObjectAnimator progressAnimator;
    private int currentProgress = 0;

    // 延迟播放相关
    private Handler playDelayHandler = new Handler(Looper.getMainLooper());
    private Runnable pendingPlayRunnable;

    private BroadcastReceiver musicReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (action == null) return;

            switch (action) {
                case MusicService.ACTION_PLAY_STATE_CHANGED:
                    int playState = intent.getIntExtra("play_state", MusicService.STATE_IDLE);
                    updatePlayButton(playState);
                    updateProgressBarVisibility(playState);
                    break;
                case MusicService.ACTION_SONG_CHANGED:
                    updateSongInfo();
                    break;
                case MusicService.ACTION_PROGRESS_UPDATED:
                    // 更新进度条
                    int currentPosition = intent.getIntExtra("current_position", 0);
                    int duration = intent.getIntExtra("duration", 1);
                    int progress = (int) (((float) currentPosition / duration) * 100);
                    updateProgress(progress);
                    break;
            }
        }
    };

    @Override
    protected FragmentMiniPlayerBinding getViewBinding(LayoutInflater inflater, ViewGroup container) {
        return FragmentMiniPlayerBinding.inflate(inflater, container, false);
    }

    @Override
    protected void initView() {
        super.initView();

        // 初始化进度条
        progressBar = binding.miniProgressBar;

        // 初始化 RecyclerView
        setupRecyclerView();

        // 设置点击事件
        binding.miniPlayPause.setOnClickListener(v -> {
            if (getActivity() instanceof MainActivity) {
                MainActivity mainActivity = (MainActivity) getActivity();
                mainActivity.playOrPause();
            }
        });

        // 设置默认状态
        updateDefaultState();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();

        // 清理延迟播放任务
        if (pendingPlayRunnable != null) {
            playDelayHandler.removeCallbacks(pendingPlayRunnable);
        }

        // 清理进度条动画
        if (progressAnimator != null && progressAnimator.isRunning()) {
            progressAnimator.cancel();
        }
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
        LocalBroadcastManager.getInstance(context).registerReceiver(musicReceiver, filter);
    }

    private void unregisterMusicReceiver() {
        LocalBroadcastManager.getInstance(context).unregisterReceiver(musicReceiver);
    }


    private void updateSongInfo() {
        if (!(getActivity() instanceof MainActivity)) return;

        MainActivity mainActivity = (MainActivity) getActivity();
        Song currentSong = mainActivity.getCurrentSong();

        if (currentSong != null) {
            // 更新播放列表
            List<Song> currentPlaylist = mainActivity.getPlaylist();
            if (currentPlaylist != null && !currentPlaylist.isEmpty()) {
                updatePlaylist(currentPlaylist);

                // 找到当前歌曲在播放列表中的位置
                int position = currentPlaylist.indexOf(currentSong);
                System.out.println("position = " + position);
                System.out.println("currentPosition = " + currentPosition);
                if (position != -1 && position != currentPosition) {
                    currentPosition = position;

                    miniPlayerAdapter.setCurrentPosition(position);
                    binding.miniPlayerContainer.post(() -> {
                        scrollToPosition(position);
                    });

                    // 歌曲切换时立即重置进度条
                    resetProgress();
                }
            }
        } else {
            updateDefaultState();
        }
    }

    private void updatePlayButton(int playState) {
        switch (playState) {
            case MusicService.STATE_PLAYING:
                binding.miniPlayPause.setImageResource(R.drawable.ic_pause);
                break;
            case MusicService.STATE_PAUSED:
            case MusicService.STATE_STOPPED:
            case MusicService.STATE_IDLE:
                binding.miniPlayPause.setImageResource(R.drawable.ic_play);
                break;
            case MusicService.STATE_PREPARING:
                binding.miniPlayPause.setImageResource(R.drawable.ic_pause);
                break;
            case MusicService.STATE_ERROR:
                binding.miniPlayPause.setImageResource(R.drawable.ic_play);
                break;
        }
    }

    private void updateDefaultState() {
        binding.miniPlayPause.setImageResource(R.drawable.ic_play);

        // 清空播放列表
        playlist.clear();
        if (miniPlayerAdapter != null) {
            miniPlayerAdapter.updatePlaylist(playlist);
        }

        // 隐藏进度条并重置进度
        if (progressBar != null) {
            progressBar.setVisibility(View.GONE);
            resetProgress();
        }

        currentPosition = 0;
    }

    /**
     * 更新进度条
     */
    private void updateProgress(int progress) {
        if (progressBar != null) {
            // 使用动画平滑更新进度条
            animateProgressTo(progress);
        }
    }

    /**
     * 重置进度条（立即置零）
     */
    private void resetProgress() {
        if (progressBar != null) {
            // 取消当前动画
            if (progressAnimator != null && progressAnimator.isRunning()) {
                progressAnimator.cancel();
            }

            // 立即将进度条置零
            currentProgress = 0;
            progressBar.setProgress(0);
        }
    }

    /**
     * 平滑动画到指定进度
     */
    private void animateProgressTo(int targetProgress) {
        if (progressBar == null || targetProgress == currentProgress) {
            return;
        }

        // 取消当前动画
        if (progressAnimator != null && progressAnimator.isRunning()) {
            progressAnimator.cancel();
        }

        // 创建新的动画
        progressAnimator = ObjectAnimator.ofInt(progressBar, "progress", currentProgress, targetProgress);
        progressAnimator.setDuration(200); // 200ms的平滑动画
        progressAnimator.addUpdateListener(animator -> {
            currentProgress = (int) animator.getAnimatedValue();
        });
        progressAnimator.start();

        // 更新当前进度值
        currentProgress = targetProgress;
    }

    /**
     * 更新进度条可见性
     */
    private void updateProgressBarVisibility(int playState) {
        if (progressBar == null) return;

        switch (playState) {
            case MusicService.STATE_PLAYING:
            case MusicService.STATE_PAUSED:
                progressBar.setVisibility(View.VISIBLE);
                break;
            case MusicService.STATE_STOPPED:
            case MusicService.STATE_IDLE:
            case MusicService.STATE_ERROR:
                progressBar.setVisibility(View.GONE);
                resetProgress();
                break;
            case MusicService.STATE_PREPARING:
                progressBar.setVisibility(View.VISIBLE);
                break;
        }
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

        // 更新本地播放列表
        updatePlaylist(songs);
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

    @Override
    public boolean autoHideBottomNavigation() {
        return false;
    }

    /**
     * 设置 RecyclerView
     */
    private void setupRecyclerView() {
        // 设置布局管理器
        layoutManager = new LinearLayoutManager(context, LinearLayoutManager.HORIZONTAL, false);
        binding.miniPlayerRecyclerView.setLayoutManager(layoutManager);

        // 设置适配器
        miniPlayerAdapter = new MiniPlayerAdapter(context, playlist);
        miniPlayerAdapter.setOnSongChangeListener(new MiniPlayerAdapter.OnSongChangeListener() {
            @Override
            public void onSongChanged(Song song, int position) {
                onSongChangedByUser(song, position);
            }

            @Override
            public void onSongItemClicked(Song song, int position) {
                MiniPlayerFragment.this.onSongItemClicked(song, position);
            }
        });
        binding.miniPlayerRecyclerView.setAdapter(miniPlayerAdapter);

        // 设置PagerSnapHelper以实现分页效果
        pagerSnapHelper = new PagerSnapHelper();
        pagerSnapHelper.attachToRecyclerView(binding.miniPlayerRecyclerView);

        // 设置滚动监听器
        binding.miniPlayerRecyclerView.addOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrollStateChanged(@NonNull RecyclerView recyclerView, int newState) {
                super.onScrollStateChanged(recyclerView, newState);

                if (newState == RecyclerView.SCROLL_STATE_IDLE) {
                    // 滚动停止时，检查当前位置
                    View snapView = pagerSnapHelper.findSnapView(layoutManager);
                    if (snapView != null) {
                        int position = binding.miniPlayerRecyclerView.getChildAdapterPosition(snapView);
                        if (position != RecyclerView.NO_POSITION && position != currentPosition) {
                            // 用户滑动切换了歌曲
                            isUserScrolling = true;
                            currentPosition = position;
                            miniPlayerAdapter.setCurrentPosition(position);

                            // 取消之前的延迟播放任务
                            if (pendingPlayRunnable != null) {
                                playDelayHandler.removeCallbacks(pendingPlayRunnable);
                            }

                            // 延迟播放新歌曲
                            if (position < playlist.size()) {
                                Song newSong = playlist.get(position);
                                pendingPlayRunnable = () -> {
                                    if (getActivity() instanceof MainActivity) {
                                        MainActivity mainActivity = (MainActivity) getActivity();
                                        mainActivity.playSong(newSong);
                                    }
                                    isUserScrolling = false;
                                };
                                playDelayHandler.postDelayed(pendingPlayRunnable, PLAY_DELAY_MS);
                            } else {
                                isUserScrolling = false;
                            }
                        }
                    }
                }
            }
        });
    }

    /**
     * 用户通过滑动切换歌曲时的回调
     */
    private void onSongChangedByUser(Song song, int position) {
        if (!isUserScrolling && getActivity() instanceof MainActivity) {
            MainActivity mainActivity = (MainActivity) getActivity();
            mainActivity.playSong(song);
        }
    }

    /**
     * 用户点击歌曲item时的回调
     */
    private void onSongItemClicked(Song song, int position) {
        // 展开bottomsheet
        if (getActivity() instanceof MainActivity) {
            MainActivity mainActivity = (MainActivity) getActivity();
            mainActivity.expandBottomSheet();
        }
    }

    /**
     * 更新播放列表
     */
    public void updatePlaylist(List<Song> newPlaylist) {
        this.playlist.clear();
        if (newPlaylist != null) {
            this.playlist.addAll(newPlaylist);
        }

        if (miniPlayerAdapter != null) {
            miniPlayerAdapter.updatePlaylist(this.playlist);
        }
    }

    /**
     * 滚动到指定位置（直接跳转）
     */
    private void scrollToPosition(int position) {
        if (!isUserScrolling) {
            binding.miniPlayerRecyclerView.scrollToPosition(position);
        }
    }
} 
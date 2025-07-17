package com.magicalstory.music.player;

import android.animation.ObjectAnimator;
import android.graphics.Color;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ProgressBar;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.media3.common.MediaItem;
import androidx.media3.common.PlaybackException;
import androidx.media3.common.Player;
import androidx.media3.common.util.UnstableApi;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.PagerSnapHelper;
import androidx.recyclerview.widget.RecyclerView;

import com.magicalstory.music.MainActivity;
import com.magicalstory.music.R;
import com.magicalstory.music.base.BaseFragment;
import com.magicalstory.music.databinding.FragmentMiniPlayerBinding;
import com.magicalstory.music.model.Song;

import java.util.ArrayList;
import java.util.List;

/**
 * Mini播放器Fragment - 使用MediaControllerHelper统一管理播放状态
 */
@UnstableApi
public class MiniPlayerFragment extends BaseFragment<FragmentMiniPlayerBinding> {

    private static final String TAG = "MiniPlayerFragment";
    private static final int PLAY_DELAY_MS = 100; // 延迟播放时间（毫秒）

    // MediaControllerHelper相关
    private MediaControllerHelper controllerHelper;

    // 播放状态监听器
    private final MediaControllerHelper.PlaybackStateListener playbackStateListener = new MediaControllerHelper.PlaybackStateListener() {
        @Override
        public void onPlaybackStateChanged(int playbackState) {
            Log.d(TAG, "MiniPlayerFragment收到播放状态改变: " + playbackState);
            updatePlaybackState();
        }

        @Override
        public void onIsPlayingChanged(boolean isPlaying) {
            Log.d(TAG, "MiniPlayerFragment收到播放状态改变: " + isPlaying);
            updatePlayButton(isPlaying);
        }

        @Override
        public void onMediaItemTransition(@Nullable MediaItem mediaItem, int reason) {
            Log.d(TAG, "MiniPlayerFragment收到媒体项切换: " + (mediaItem != null ? mediaItem.mediaId : "null"));
            updateCurrentSong();
        }

        @Override
        public void onPlayerError(PlaybackException error) {
            Log.e(TAG, "MiniPlayerFragment收到播放错误: " + error.getMessage(), error);
            // 可以在这里处理错误，比如显示错误信息
        }

        @Override
        public void onPositionChanged(long position, long duration) {
            System.out.println("position = " + position);
            updateProgress(position, duration);
        }

        /**
         * 更新播放进度
         */
        private void updateProgress(long position, long duration) {
            if (duration > 0) {
                int progress = (int) (((float) position / duration) * 100);
                MiniPlayerFragment.this.updateProgress(progress);
            }
        }

        @Override
        public void progressInit(long dur, long progress) {
            binding.miniProgressBar.setVisibility(View.VISIBLE);
            int progress1 = (int) (((float) progress / dur) * 100);
            MiniPlayerFragment.this.updateProgress(progress1);
        }
    };

    // RecyclerView 相关
    private MiniPlayerAdapter miniPlayerAdapter;
    private LinearLayoutManager layoutManager;
    private PagerSnapHelper pagerSnapHelper;
    private final List<Song> playlist = new ArrayList<>();
    private int currentPosition = 0;
    private boolean isUserScrolling = false;

    // 进度条相关
    private ProgressBar progressBar;
    private ObjectAnimator progressAnimator;
    private int currentProgress = 0;

    // 延迟播放相关
    private Handler playDelayHandler = new Handler(Looper.getMainLooper());
    private Runnable pendingPlayRunnable;

    // 进度更新相关
    private Handler progressUpdateHandler = new Handler(Looper.getMainLooper());
    private Runnable progressUpdateRunnable;

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
            if (controllerHelper != null) {
                controllerHelper.togglePlayPause();
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

        // 清理进度更新任务
        if (progressUpdateRunnable != null) {
            progressUpdateHandler.removeCallbacks(progressUpdateRunnable);
        }

        // 清理进度条动画
        if (progressAnimator != null && progressAnimator.isRunning()) {
            progressAnimator.cancel();
        }

        // 移除播放状态监听器
        if (controllerHelper != null) {
            controllerHelper.removePlaybackStateListener(playbackStateListener);
        }
    }


    /**
     * 设置MediaControllerHelper
     */
    public void setMediaControllerHelper(@Nullable MediaControllerHelper controllerHelper) {
        // 移除之前的监听器
        if (this.controllerHelper != null) {
            this.controllerHelper.removePlaybackStateListener(playbackStateListener);
        }

        this.controllerHelper = controllerHelper;

        if (controllerHelper != null) {
            // 添加播放状态监听器
            controllerHelper.addPlaybackStateListener(playbackStateListener);

            // 更新UI状态
            updatePlaybackState();
            updateCurrentSong();

            Log.d(TAG, "MediaControllerHelper set successfully");
        } else {
            updateDefaultState();
            Log.d(TAG, "MediaController removed");
        }
    }


    /**
     * 播放状态改变处理
     */
    public void onPlaybackStateChanged(int playbackState) {
        Log.d(TAG, "Playback state changed: " + playbackState);
        updatePlayButton(playbackState);
        updateProgressBarVisibility(playbackState);

        if (playbackState == Player.STATE_READY || playbackState == Player.STATE_BUFFERING) {
            startProgressUpdates();
        } else {
            stopProgressUpdates();
        }
    }

    /**
     * 播放状态改变处理
     */
    public void onIsPlayingChanged(boolean isPlaying) {
        Log.d(TAG, "Is playing changed: " + isPlaying);
        updatePlayButton(isPlaying);

        if (isPlaying) {
            startProgressUpdates();
        } else {
            stopProgressUpdates();
        }
    }

    /**
     * 媒体项切换处理
     */
    public void onMediaItemTransition(@Nullable MediaItem mediaItem, int reason) {
        Log.d(TAG, "Media item transition: " + (mediaItem != null ? mediaItem.mediaId : "null"));
        updateCurrentSong();
        resetProgress();
    }

    /**
     * 播放错误处理
     */
    public void onPlayerError(PlaybackException error) {
        Log.e(TAG, "Player error: " + error.getMessage(), error);
        updateDefaultState();
    }

    /**
     * 重复模式改变处理
     */
    public void onRepeatModeChanged(int repeatMode) {
        Log.d(TAG, "Repeat mode changed: " + repeatMode);
    }

    /**
     * 随机模式改变处理
     */
    public void onShuffleModeEnabledChanged(boolean shuffleModeEnabled) {
        Log.d(TAG, "Shuffle mode changed: " + shuffleModeEnabled);
    }


    /**
     * 更新播放状态
     */
    private void updatePlaybackState() {
        if (controllerHelper == null) return;

        updatePlayButton(controllerHelper.isPlaying());
        updateProgressBarVisibility(controllerHelper.getPlaybackState());

        if (controllerHelper.isPlaying()) {
            startProgressUpdates();
        } else {
            stopProgressUpdates();
        }
    }

    /**
     * 更新当前歌曲信息
     */
    private void updateCurrentSong() {
        if (controllerHelper == null) return;

        Song currentSong = controllerHelper.getCurrentSong();
        List<Song> currentPlaylist = controllerHelper.getPlaylist();

        if (currentSong != null && !currentPlaylist.isEmpty()) {

            System.out.println("currentSong.getTitle() = " + currentSong.getTitle());

            // 更新播放列表
            updatePlaylist(currentPlaylist);

            // 找到当前歌曲在播放列表中的位置
            int position = controllerHelper.getCurrentIndex();
            System.out.println("position = " + position);
            if (position != -1 && position != currentPosition) {
                currentPosition = position;

                miniPlayerAdapter.setCurrentPosition(position);
                binding.miniPlayerContainer.post(() -> {
                    scrollToPosition(position);
                });

                // 歌曲切换时立即重置进度条
                resetProgress();
            }
        } else {
            updateDefaultState();
        }
    }

    /**
     * 更新播放按钮
     */
    private void updatePlayButton(int playbackState) {
        switch (playbackState) {
            case Player.STATE_READY:
                if (controllerHelper != null && controllerHelper.isPlaying()) {
                    binding.miniPlayPause.setImageResource(R.drawable.ic_pause);
                } else {
                    binding.miniPlayPause.setImageResource(R.drawable.ic_play);
                }
                break;
            case Player.STATE_BUFFERING:
                binding.miniPlayPause.setImageResource(R.drawable.ic_pause);
                break;
            case Player.STATE_IDLE:
            case Player.STATE_ENDED:
            default:
                binding.miniPlayPause.setImageResource(R.drawable.ic_play);
                break;
        }
    }

    /**
     * 更新播放按钮（基于播放状态）
     */
    private void updatePlayButton(boolean isPlaying) {
        if (isPlaying) {
            binding.miniPlayPause.setImageResource(R.drawable.ic_pause);
        } else {
            binding.miniPlayPause.setImageResource(R.drawable.ic_play);
        }
    }

    /**
     * 更新默认状态
     */
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

        stopProgressUpdates();
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
    private void updateProgressBarVisibility(int playbackState) {
        if (progressBar == null) return;

        switch (playbackState) {
            case Player.STATE_READY:
            case Player.STATE_BUFFERING:
                progressBar.setVisibility(View.VISIBLE);
                break;
            case Player.STATE_IDLE:
            case Player.STATE_ENDED:
            default:
                progressBar.setVisibility(View.GONE);
                resetProgress();
                break;
        }
    }

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
    public void updateProgressFromPlayer() {
        if (controllerHelper == null) return;
        binding.miniProgressBar.setVisibility(View.VISIBLE);

        try {
            long currentPosition = controllerHelper.getCurrentPosition();
            System.out.println("currentPosition = " + currentPosition);
            System.out.println("controllerHelper.getDuration() = " + controllerHelper.getDuration());
            long duration = controllerHelper.getDuration();
            if (duration > 0) {
                int progress = (int) (((float) currentPosition / duration) * 100);
                updateProgress(progress);
            }
        } catch (Exception e) {
            Log.e(TAG, "Error updating progress from player", e);
        }
    }


    /**
     * 设置播放列表
     */
    public void setPlaylist(List<Song> songs) {
        if (controllerHelper != null) {
            controllerHelper.setPlaylist(songs);
        }

        // 更新本地播放列表
        updatePlaylist(songs);
    }

    /**
     * 获取当前播放状态
     */
    public boolean isPlaying() {
        return controllerHelper != null && controllerHelper.isPlaying();
    }

    /**
     * 获取当前播放的歌曲
     */
    public Song getCurrentSong() {
        return controllerHelper != null ? controllerHelper.getCurrentSong() : null;
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

                                pendingPlayRunnable = () -> {
                                    if (controllerHelper != null) {
                                        controllerHelper.playAtIndex(currentPosition);
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
        binding.miniPlayerRecyclerView.setVisibility(View.VISIBLE);
        binding.itemPlaceholder.setVisibility(View.GONE);
        binding.miniPlayPause.setVisibility(View.VISIBLE);
        binding.miniPlayerRecyclerView.setBackgroundColor(Color.TRANSPARENT);
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
package com.magicalstory.music.service;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.os.Binder;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.util.Log;
import android.widget.RemoteViews;

import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import com.magicalstory.music.MainActivity;
import com.magicalstory.music.R;
import com.magicalstory.music.model.PlayHistory;
import com.magicalstory.music.model.Song;
import com.magicalstory.music.utils.glide.GlideUtils;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.io.File;

/**
 * 音乐播放服务
 */
public class MusicService extends Service implements MediaPlayer.OnPreparedListener, 
        MediaPlayer.OnCompletionListener, MediaPlayer.OnErrorListener {

    private static final String TAG = "MusicService";
    private static final String CHANNEL_ID = "music_channel";
    private static final int NOTIFICATION_ID = 1001;

    // 播放状态
    public static final int STATE_IDLE = 0;
    public static final int STATE_PREPARING = 1;
    public static final int STATE_PLAYING = 2;
    public static final int STATE_PAUSED = 3;
    public static final int STATE_STOPPED = 4;
    public static final int STATE_ERROR = 5;

    // 播放模式
    public static final int PLAY_MODE_SEQUENCE = 0;  // 顺序播放
    public static final int PLAY_MODE_LOOP = 1;      // 列表循环
    public static final int PLAY_MODE_SINGLE = 2;    // 单曲循环
    public static final int PLAY_MODE_RANDOM = 3;    // 随机播放

    // 广播Action
    public static final String ACTION_PLAY_STATE_CHANGED = "com.magicalstory.music.PLAY_STATE_CHANGED";
    public static final String ACTION_SONG_CHANGED = "com.magicalstory.music.SONG_CHANGED";
    public static final String ACTION_PLAY_MODE_CHANGED = "com.magicalstory.music.PLAY_MODE_CHANGED";
    public static final String ACTION_PROGRESS_UPDATED = "com.magicalstory.music.PROGRESS_UPDATED";

    // 通知Action
    public static final String ACTION_PLAY_PAUSE = "com.magicalstory.music.PLAY_PAUSE";
    public static final String ACTION_PREVIOUS = "com.magicalstory.music.PREVIOUS";
    public static final String ACTION_NEXT = "com.magicalstory.music.NEXT";
    public static final String ACTION_STOP = "com.magicalstory.music.STOP";

    private volatile MediaPlayer mediaPlayer;
    private List<Song> playlist;
    private volatile Song currentSong;
    private volatile int currentIndex = -1;
    private volatile int playState = STATE_IDLE;
    private int playMode = PLAY_MODE_SEQUENCE;
    private boolean isShuffleEnabled = false;
    private List<Integer> shuffleList;
    private Random random;

    private NotificationManager notificationManager;
    private Handler mainHandler;
    private Handler progressHandler;
    private Runnable progressRunnable;
    private AudioManager audioManager;
    private AudioManager.OnAudioFocusChangeListener audioFocusChangeListener;

    private volatile long playStartTime;
    private volatile long totalPlayTime;
    
    // 后台任务执行器
    private ExecutorService backgroundExecutor;
    private ExecutorService mediaPlayerExecutor;
    
    // ANR保护
    private static final int MAX_HANDLER_EXECUTION_TIME = 3000; // 3秒
    private Handler anrWatchdog;
    private Future<?> currentPlayTask;

    // Binder for activity communication
    private final IBinder binder = new MusicBinder();

    public class MusicBinder extends Binder {
        public MusicService getService() {
            return MusicService.this;
        }
    }

    @Override
    public void onCreate() {
        super.onCreate();
        Log.d(TAG, "Service created");
        
        initializeService();
        createNotificationChannel();
        setupProgressHandler();
        setupAudioFocus();
        registerNotificationReceiver();
        
        // 立即启动前台服务以确保服务在后台运行
        showDefaultNotification();
    }

    private void initializeService() {
        playlist = new ArrayList<>();
        random = new Random();
        shuffleList = new ArrayList<>();
        notificationManager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
        audioManager = (AudioManager) getSystemService(AUDIO_SERVICE);
        mainHandler = new Handler(Looper.getMainLooper());
        backgroundExecutor = Executors.newCachedThreadPool();
        mediaPlayerExecutor = Executors.newSingleThreadExecutor();
        
        // 初始化ANR保护
        anrWatchdog = new Handler(Looper.getMainLooper());
    }

    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    CHANNEL_ID,
                    "Music Player",
                    NotificationManager.IMPORTANCE_LOW
            );
            channel.setDescription("Music playback notifications");
            channel.setShowBadge(false);
            notificationManager.createNotificationChannel(channel);
        }
    }

    private void setupProgressHandler() {
        progressHandler = new Handler(Looper.getMainLooper());
        progressRunnable = new Runnable() {
            private int lastBroadcastPosition = -1;
            
            @Override
            public void run() {
                try {
                    if (mediaPlayer != null && mediaPlayer.isPlaying()) {
                        int currentPosition = mediaPlayer.getCurrentPosition();
                        int duration = mediaPlayer.getDuration();
                        
                        // 避免频繁广播，只在位置有显著变化时发送
                        if (currentPosition >= 0 && duration > 0 && 
                            Math.abs(currentPosition - lastBroadcastPosition) > 500) { // 每500ms发送一次
                            
                            Intent intent = new Intent(ACTION_PROGRESS_UPDATED);
                            intent.putExtra("current_position", currentPosition);
                            intent.putExtra("duration", duration);
                            LocalBroadcastManager.getInstance(MusicService.this).sendBroadcast(intent);
                            
                            lastBroadcastPosition = currentPosition;
                        }
                        
                        // 每秒更新一次
                        progressHandler.postDelayed(this, 1000);
                    }
                } catch (Exception e) {
                    Log.e(TAG, "Error updating progress", e);
                    // 发生错误时停止进度更新
                    stopProgressUpdates();
                }
            }
        };
    }

    private void setupAudioFocus() {
        audioFocusChangeListener = focusChange -> {
            mainHandler.post(() -> {
                try {
                    switch (focusChange) {
                        case AudioManager.AUDIOFOCUS_LOSS:
                            // 永久失去焦点，暂停播放
                            pause();
                            break;
                        case AudioManager.AUDIOFOCUS_LOSS_TRANSIENT:
                            // 暂时失去焦点，暂停播放
                            pause();
                            break;
                        case AudioManager.AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK:
                            // 降低音量
                            if (mediaPlayer != null) {
                                mediaPlayer.setVolume(0.3f, 0.3f);
                            }
                            break;
                        case AudioManager.AUDIOFOCUS_GAIN:
                            // 重新获得焦点，恢复播放
                            if (mediaPlayer != null) {
                                mediaPlayer.setVolume(1.0f, 1.0f);
                            }
                            break;
                    }
                } catch (Exception e) {
                    Log.e(TAG, "Error handling audio focus change", e);
                }
            });
        };
    }

    private void registerNotificationReceiver() {
        IntentFilter filter = new IntentFilter();
        filter.addAction(ACTION_PLAY_PAUSE);
        filter.addAction(ACTION_PREVIOUS);
        filter.addAction(ACTION_NEXT);
        filter.addAction(ACTION_STOP);
        
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            registerReceiver(notificationReceiver, filter, Context.RECEIVER_NOT_EXPORTED);
        } else {
            registerReceiver(notificationReceiver, filter);
        }
    }

    private final BroadcastReceiver notificationReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (action == null) return;

            mainHandler.post(() -> {
                try {
                    switch (action) {
                        case ACTION_PLAY_PAUSE:
                            if (isPlaying()) {
                                pause();
                            } else {
                                play();
                            }
                            break;
                        case ACTION_PREVIOUS:
                            playPrevious();
                            break;
                        case ACTION_NEXT:
                            playNext();
                            break;
                        case ACTION_STOP:
                            stop();
                            break;
                    }
                } catch (Exception e) {
                    Log.e(TAG, "Error handling notification action", e);
                }
            });
        }
    };

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return binder;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.d(TAG, "Service started with START_STICKY");
        // 确保显示前台通知
        if (currentSong == null) {
            showDefaultNotification();
        } else {
            updateNotification();
        }
        return START_STICKY; // 服务被杀死后会重新创建
    }

    // 播放控制方法
    public void play() {
        // 避免在主线程中进行重复的任务提交
        if (mainHandler.getLooper().getThread() == Thread.currentThread()) {
            // 如果已经在主线程中，直接执行
            playInternal();
        } else {
            // 在后台线程中，通过Handler切换到主线程
            mainHandler.post(this::playInternal);
        }
    }

    private void playInternal() {
        try {
            if (currentSong == null) {
                if (!playlist.isEmpty()) {
                    playSong(0);
                }
                return;
            }

            if (mediaPlayer == null) {
                // 在后台线程中初始化MediaPlayer
                backgroundExecutor.execute(() -> {
                    initializeMediaPlayer();
                    mainHandler.post(() -> {
                        if (playState == STATE_PAUSED) {
                            resumePlayback();
                        } else if (playState == STATE_IDLE || playState == STATE_STOPPED) {
                            playSong(currentIndex);
                        }
                    });
                });
                return;
            }

            if (playState == STATE_PAUSED) {
                resumePlayback();
            } else if (playState == STATE_IDLE || playState == STATE_STOPPED) {
                playSong(currentIndex);
            }
        } catch (Exception e) {
            Log.e(TAG, "Error in play()", e);
            playState = STATE_ERROR;
            sendPlayStateChangedBroadcast();
        }
    }

    private void resumePlayback() {
        try {
            if (requestAudioFocus()) {
                mediaPlayer.start();
                playState = STATE_PLAYING;
                playStartTime = System.currentTimeMillis();
                startProgressUpdates();
                updateNotification();
                sendPlayStateChangedBroadcast();
            }
        } catch (Exception e) {
            Log.e(TAG, "Error resuming playback", e);
            playState = STATE_ERROR;
            sendPlayStateChangedBroadcast();
        }
    }

    public void pause() {
        // 避免在主线程中进行重复的任务提交
        if (mainHandler.getLooper().getThread() == Thread.currentThread()) {
            pauseInternal();
        } else {
            mainHandler.post(this::pauseInternal);
        }
    }

    private void pauseInternal() {
        try {
            if (mediaPlayer != null && mediaPlayer.isPlaying()) {
                mediaPlayer.pause();
                playState = STATE_PAUSED;
                
                // 在后台线程中更新播放时间和记录
                backgroundExecutor.execute(() -> {
                    updatePlayTime();
                    // 主线程操作
                    mainHandler.post(() -> {
                        stopProgressUpdates();
                        updateNotification(); // 保持前台服务，只更新通知
                        sendPlayStateChangedBroadcast();
                        abandonAudioFocus();
                    });
                });
            }
        } catch (Exception e) {
            Log.e(TAG, "Error in pause()", e);
        }
    }

    public void stop() {
        // 避免在主线程中进行重复的任务提交
        if (mainHandler.getLooper().getThread() == Thread.currentThread()) {
            stopInternal();
        } else {
            mainHandler.post(this::stopInternal);
        }
    }

    private void stopInternal() {
        try {
            if (mediaPlayer != null) {
                if (mediaPlayer.isPlaying()) {
                    mediaPlayer.stop();
                }
                
                // 在后台线程中处理耗时操作
                backgroundExecutor.execute(() -> {
                    updatePlayTime();
                    recordPlayHistory();
                    
                    // 主线程操作
                    mainHandler.post(() -> {
                        playState = STATE_STOPPED;
                        stopProgressUpdates();
                        updateNotification();
                        sendPlayStateChangedBroadcast();
                        abandonAudioFocus();
                    });
                });
            }
        } catch (Exception e) {
            Log.e(TAG, "Error in stop()", e);
        }
    }

    /**
     * 停止音乐播放但保持服务运行
     */
    public void stopPlayback() {
        mainHandler.post(() -> {
            try {
                // 取消当前的播放任务
                if (currentPlayTask != null && !currentPlayTask.isDone()) {
                    currentPlayTask.cancel(true);
                }
                
                if (mediaPlayer != null) {
                    if (mediaPlayer.isPlaying()) {
                        mediaPlayer.stop();
                    }
                    mediaPlayer.reset();
                }
                
                updatePlayTime();
                recordPlayHistory();
                
                currentSong = null;
                currentIndex = -1;
                playState = STATE_STOPPED;
                
                stopProgressUpdates();
                showDefaultNotification(); // 显示默认通知而不是停止前台服务
                sendPlayStateChangedBroadcast();
                sendSongChangedBroadcast();
                abandonAudioFocus();
            } catch (Exception e) {
                Log.e(TAG, "Error in stopPlayback()", e);
            }
        });
    }

    public void seekTo(int position) {
        mainHandler.post(() -> {
            try {
                if (mediaPlayer != null && playState != STATE_IDLE) {
                    mediaPlayer.seekTo(position);
                    // 更新播放历史进度
                    if (currentSong != null) {
                        double progress = (double) position / mediaPlayer.getDuration();
                        updatePlayHistoryProgress(progress);
                    }
                }
            } catch (Exception e) {
                Log.e(TAG, "Error in seekTo()", e);
            }
        });
    }

    public void playSong(int index) {
        if (index < 0 || index >= playlist.size()) return;
        
        // 取消之前的播放任务
        if (currentPlayTask != null && !currentPlayTask.isDone()) {
            currentPlayTask.cancel(true);
        }
        
        currentPlayTask = mediaPlayerExecutor.submit(() -> {
            try {
                // 保存当前播放记录
                if (currentSong != null) {
                    recordPlayHistory();
                }

                currentIndex = index;
                Song songToPlay = playlist.get(index);
                
                // 检查文件是否存在 - 在后台线程中进行
                if (songToPlay == null || songToPlay.getPath() == null) {
                    Log.e(TAG, "Current song or path is null");
                    mainHandler.post(() -> {
                        playState = STATE_ERROR;
                        sendPlayStateChangedBroadcast();
                    });
                    return;
                }
                
                File songFile = new File(songToPlay.getPath());
                if (!songFile.exists()) {
                    Log.e(TAG, "Song file does not exist: " + songToPlay.getPath());
                    mainHandler.post(() -> {
                        playState = STATE_ERROR;
                        sendPlayStateChangedBroadcast();
                        // 自动播放下一首
                        playNext();
                    });
                    return;
                }
                
                // 在主线程中更新状态和设置 MediaPlayer
                mainHandler.post(() -> {
                    try {
                        currentSong = songToPlay;
                        
                        if (mediaPlayer != null) {
                            mediaPlayer.reset();
                        } else {
                            initializeMediaPlayer();
                        }

                        playState = STATE_PREPARING;
                        playStartTime = System.currentTimeMillis();
                        totalPlayTime = 0;
                        
                        // 在后台线程中设置数据源
                        mediaPlayerExecutor.submit(() -> {
                            try {
                                mediaPlayer.setDataSource(currentSong.getPath());
                                mediaPlayer.prepareAsync();
                                
                                mainHandler.post(() -> {
                                    sendSongChangedBroadcast();
                                });
                            } catch (IOException e) {
                                Log.e(TAG, "Error setting data source", e);
                                mainHandler.post(() -> {
                                    playState = STATE_ERROR;
                                    sendPlayStateChangedBroadcast();
                                    // 自动播放下一首
                                    playNext();
                                });
                            } catch (Exception e) {
                                Log.e(TAG, "Unexpected error preparing media player", e);
                                mainHandler.post(() -> {
                                    playState = STATE_ERROR;
                                    sendPlayStateChangedBroadcast();
                                });
                            }
                        });
                    } catch (Exception e) {
                        Log.e(TAG, "Error in playSong main thread", e);
                        playState = STATE_ERROR;
                        sendPlayStateChangedBroadcast();
                    }
                });
            } catch (Exception e) {
                Log.e(TAG, "Error in playSong background thread", e);
                mainHandler.post(() -> {
                    playState = STATE_ERROR;
                    sendPlayStateChangedBroadcast();
                });
            }
        });
    }

    public void playSong(Song song) {
        if (song == null) return;
        
        int index = playlist.indexOf(song);
        if (index >= 0) {
            playSong(index);
        } else {
            // 如果歌曲不在当前播放列表中，创建新的播放列表
            playlist.clear();
            playlist.add(song);
            updateShuffleList();
            playSong(0);
        }
    }

    public void playNext() {
        if (playlist.isEmpty()) return;

        int nextIndex = getNextIndex();
        if (nextIndex >= 0) {
            playSong(nextIndex);
        }
    }

    public void playPrevious() {
        if (playlist.isEmpty()) return;

        int prevIndex = getPreviousIndex();
        if (prevIndex >= 0) {
            playSong(prevIndex);
        }
    }

    // 播放列表管理
    public void setPlaylist(List<Song> songs) {
        playlist.clear();
        if (songs != null) {
            playlist.addAll(songs);
        }
        updateShuffleList();
    }

    public void addToPlaylist(Song song) {
        if (song != null) {
            playlist.add(song);
            updateShuffleList();
        }
    }

    public void removeFromPlaylist(int index) {
        if (index >= 0 && index < playlist.size()) {
            playlist.remove(index);
            if (index == currentIndex) {
                if (playlist.isEmpty()) {
                    stop();
                    currentSong = null;
                    currentIndex = -1;
                } else {
                    currentIndex = Math.min(currentIndex, playlist.size() - 1);
                    playSong(currentIndex);
                }
            } else if (index < currentIndex) {
                currentIndex--;
            }
            updateShuffleList();
        }
    }

    public void clearPlaylist() {
        stop();
        playlist.clear();
        currentSong = null;
        currentIndex = -1;
        updateShuffleList();
    }

    // 播放模式管理
    public void setPlayMode(int mode) {
        playMode = mode;
        if (mode == PLAY_MODE_RANDOM) {
            isShuffleEnabled = true;
            updateShuffleList();
        } else {
            isShuffleEnabled = false;
        }
        sendPlayModeChangedBroadcast();
    }

    private void updateShuffleList() {
        shuffleList.clear();
        for (int i = 0; i < playlist.size(); i++) {
            shuffleList.add(i);
        }
        Collections.shuffle(shuffleList, random);
    }

    private int getNextIndex() {
        if (playlist.isEmpty()) return -1;

        switch (playMode) {
            case PLAY_MODE_SEQUENCE:
                return (currentIndex + 1) % playlist.size();
            case PLAY_MODE_LOOP:
                return (currentIndex + 1) % playlist.size();
            case PLAY_MODE_SINGLE:
                return currentIndex;
            case PLAY_MODE_RANDOM:
                if (shuffleList.isEmpty()) {
                    updateShuffleList();
                }
                int currentShuffleIndex = shuffleList.indexOf(currentIndex);
                return shuffleList.get((currentShuffleIndex + 1) % shuffleList.size());
            default:
                return (currentIndex + 1) % playlist.size();
        }
    }

    private int getPreviousIndex() {
        if (playlist.isEmpty()) return -1;

        switch (playMode) {
            case PLAY_MODE_SEQUENCE:
                return currentIndex > 0 ? currentIndex - 1 : playlist.size() - 1;
            case PLAY_MODE_LOOP:
                return currentIndex > 0 ? currentIndex - 1 : playlist.size() - 1;
            case PLAY_MODE_SINGLE:
                return currentIndex;
            case PLAY_MODE_RANDOM:
                if (shuffleList.isEmpty()) {
                    updateShuffleList();
                }
                int currentShuffleIndex = shuffleList.indexOf(currentIndex);
                return shuffleList.get(currentShuffleIndex > 0 ? currentShuffleIndex - 1 : shuffleList.size() - 1);
            default:
                return currentIndex > 0 ? currentIndex - 1 : playlist.size() - 1;
        }
    }

    // MediaPlayer回调 - 这些回调已经在主线程中执行
    @Override
    public void onPrepared(MediaPlayer mp) {
        try {
            if (requestAudioFocus()) {
                mp.start();
                playState = STATE_PLAYING;
                startProgressUpdates();
                updateNotification();
                sendPlayStateChangedBroadcast();
            } else {
                Log.w(TAG, "Failed to request audio focus");
                playState = STATE_ERROR;
                sendPlayStateChangedBroadcast();
            }
        } catch (Exception e) {
            Log.e(TAG, "Error starting media player", e);
            playState = STATE_ERROR;
            sendPlayStateChangedBroadcast();
        }
    }

    @Override
    public void onCompletion(MediaPlayer mp) {
        try {
            recordPlayHistory();
            
            if (playMode == PLAY_MODE_SINGLE) {
                // 单曲循环
                playSong(currentIndex);
            } else {
                // 播放下一首
                playNext();
            }
        } catch (Exception e) {
            Log.e(TAG, "Error in onCompletion", e);
            playState = STATE_ERROR;
            sendPlayStateChangedBroadcast();
        }
    }

    @Override
    public boolean onError(MediaPlayer mp, int what, int extra) {
        Log.e(TAG, "MediaPlayer error: what=" + what + ", extra=" + extra);
        playState = STATE_ERROR;
        sendPlayStateChangedBroadcast();
        return true;
    }

    // 辅助方法
    private void initializeMediaPlayer() {
        try {
            // 确保在后台线程中创建MediaPlayer
            if (mediaPlayer != null) {
                try {
                    mediaPlayer.release();
                } catch (Exception e) {
                    Log.e(TAG, "Error releasing previous media player", e);
                }
            }
            
            mediaPlayer = new MediaPlayer();
            mediaPlayer.setOnPreparedListener(this);
            mediaPlayer.setOnCompletionListener(this);
            mediaPlayer.setOnErrorListener(this);
            mediaPlayer.setAudioStreamType(AudioManager.STREAM_MUSIC);
            
            // 设置异步模式，避免阻塞主线程
            mediaPlayer.setLooping(false);
            
            Log.d(TAG, "MediaPlayer initialized successfully");
        } catch (Exception e) {
            Log.e(TAG, "Error initializing media player", e);
            mediaPlayer = null;
        }
    }

    private boolean requestAudioFocus() {
        try {
            int result = audioManager.requestAudioFocus(
                    audioFocusChangeListener,
                    AudioManager.STREAM_MUSIC,
                    AudioManager.AUDIOFOCUS_GAIN
            );
            return result == AudioManager.AUDIOFOCUS_REQUEST_GRANTED;
        } catch (Exception e) {
            Log.e(TAG, "Error requesting audio focus", e);
            return false;
        }
    }

    private void abandonAudioFocus() {
        try {
            audioManager.abandonAudioFocus(audioFocusChangeListener);
        } catch (Exception e) {
            Log.e(TAG, "Error abandoning audio focus", e);
        }
    }

    private void startProgressUpdates() {
        progressHandler.removeCallbacks(progressRunnable);
        progressHandler.post(progressRunnable);
    }

    private void stopProgressUpdates() {
        progressHandler.removeCallbacks(progressRunnable);
    }

    private void updatePlayTime() {
        if (playStartTime > 0) {
            totalPlayTime += System.currentTimeMillis() - playStartTime;
            playStartTime = 0;
        }
    }
    
    /**
     * ANR保护：监控主线程任务执行时间
     */
    private void executeWithAnrProtection(Runnable task, String taskName) {
        // 记录开始时间
        long startTime = System.currentTimeMillis();
        
        // 启动监控
        anrWatchdog.postDelayed(() -> {
            long duration = System.currentTimeMillis() - startTime;
            if (duration > MAX_HANDLER_EXECUTION_TIME) {
                Log.w(TAG, "Potential ANR detected: " + taskName + " took " + duration + "ms");
            }
        }, MAX_HANDLER_EXECUTION_TIME);
        
        try {
            task.run();
        } catch (Exception e) {
            Log.e(TAG, "Error executing task: " + taskName, e);
        }
    }

    private void recordPlayHistory() {
        if (currentSong != null && backgroundExecutor != null) {
            updatePlayTime();
            final long songId = currentSong.getId();
            final long playTime = totalPlayTime;
            final boolean isCompleted = mediaPlayer != null && 
                    mediaPlayer.getCurrentPosition() >= mediaPlayer.getDuration() * 0.8;
            final double progress = mediaPlayer != null ? 
                    (double) mediaPlayer.getCurrentPosition() / mediaPlayer.getDuration() : 0.0;
            final long albumId = currentSong.getAlbumId();
            final long artistId = currentSong.getArtistId();
            
            // 在后台线程中处理数据库操作
            backgroundExecutor.execute(() -> {
                try {
                    PlayHistory.recordPlay(songId, playTime, isCompleted, progress);
                    
                    // 更新Album和Artist的lastplayed字段
                    updateAlbumAndArtistLastPlayedAsync(albumId, artistId);
                } catch (Exception e) {
                    Log.e(TAG, "Error recording play history", e);
                }
            });
        }
    }

    /**
     * 更新Album和Artist的lastplayed字段（异步版本）
     */
    private void updateAlbumAndArtistLastPlayedAsync(long albumId, long artistId) {
        long currentTime = System.currentTimeMillis();
        
        // 更新Album的lastplayed字段
        try {
            com.magicalstory.music.model.Album album = org.litepal.LitePal.where("albumId = ?", 
                String.valueOf(albumId)).findFirst(com.magicalstory.music.model.Album.class);
            if (album != null) {
                album.setLastplayed(currentTime);
                album.saveThrows();
            }
        } catch (Exception e) {
            Log.e(TAG, "Error updating album lastplayed: " + e.getMessage(), e);
        }
        
        // 更新Artist的lastplayed字段
        try {
            com.magicalstory.music.model.Artist artist = org.litepal.LitePal.where("artistId = ?", 
                String.valueOf(artistId)).findFirst(com.magicalstory.music.model.Artist.class);
            if (artist != null) {
                artist.setLastplayed(currentTime);
                artist.saveThrows();
            }
        } catch (Exception e) {
            Log.e(TAG, "Error updating artist lastplayed: " + e.getMessage(), e);
        }
    }

    private void updatePlayHistoryProgress(double progress) {
        if (currentSong != null && backgroundExecutor != null) {
            final long songId = currentSong.getId();
            final double finalProgress = progress;
            
            // 在后台线程中处理数据库操作
            backgroundExecutor.execute(() -> {
                try {
                    PlayHistory existingHistory = org.litepal.LitePal.where("songId = ?", 
                            String.valueOf(songId)).findFirst(PlayHistory.class);
                    if (existingHistory != null) {
                        existingHistory.updatePlayProgress(finalProgress);
                    }
                } catch (Exception e) {
                    Log.e(TAG, "Error updating play history progress", e);
                }
            });
        }
    }

    // 通知管理
    private void updateNotification() {
        backgroundExecutor.execute(() -> {
            try {
                if (currentSong == null) {
                    // 如果没有歌曲，显示默认通知
                    mainHandler.post(() -> showDefaultNotification());
                    return;
                }

                Intent intent = new Intent(this, MainActivity.class);
                PendingIntent pendingIntent = PendingIntent.getActivity(
                        this, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

                NotificationCompat.Builder builder = new NotificationCompat.Builder(this, CHANNEL_ID)
                        .setSmallIcon(R.drawable.ic_music_note)
                        .setContentTitle(currentSong.getTitle())
                        .setContentText(currentSong.getArtist())
                        .setSubText(currentSong.getAlbum())
                        .setContentIntent(pendingIntent)
                        .setOngoing(playState == STATE_PLAYING)
                        .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
                        .setPriority(NotificationCompat.PRIORITY_LOW)
                        .setOnlyAlertOnce(true)
                        .addAction(R.drawable.ic_skip_previous, "Previous", 
                                createNotificationAction(ACTION_PREVIOUS))
                        .addAction(playState == STATE_PLAYING ? R.drawable.ic_pause : R.drawable.ic_play, 
                                playState == STATE_PLAYING ? "Pause" : "Play",
                                createNotificationAction(ACTION_PLAY_PAUSE))
                        .addAction(R.drawable.ic_skip_next, "Next", 
                                createNotificationAction(ACTION_NEXT));

                // 添加Media Style支持
                androidx.media.app.NotificationCompat.MediaStyle mediaStyle = 
                        new androidx.media.app.NotificationCompat.MediaStyle()
                                .setShowActionsInCompactView(0, 1, 2); // 显示所有三个按钮
                
                builder.setStyle(mediaStyle);

                Notification notification = builder.build();
                
                mainHandler.post(() -> {
                    try {
                        startForeground(NOTIFICATION_ID, notification);
                    } catch (Exception e) {
                        Log.e(TAG, "Error starting foreground", e);
                    }
                });
            } catch (Exception e) {
                Log.e(TAG, "Error updating notification", e);
            }
        });
    }

    private void showDefaultNotification() {
        try {
            Intent intent = new Intent(this, MainActivity.class);
            PendingIntent pendingIntent = PendingIntent.getActivity(
                    this, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

            NotificationCompat.Builder builder = new NotificationCompat.Builder(this, CHANNEL_ID)
                    .setSmallIcon(R.drawable.ic_music_note)
                    .setContentTitle("音乐播放器")
                    .setContentText("点击打开音乐播放器")
                    .setContentIntent(pendingIntent)
                    .setOngoing(false)
                    .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
                    .setPriority(NotificationCompat.PRIORITY_LOW)
                    .setOnlyAlertOnce(true);

            Notification notification = builder.build();
            startForeground(NOTIFICATION_ID, notification);
        } catch (Exception e) {
            Log.e(TAG, "Error showing default notification", e);
        }
    }

    private PendingIntent createNotificationAction(String action) {
        Intent intent = new Intent(action);
        return PendingIntent.getBroadcast(this, 0, intent, 
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
    }

    // 广播发送
    private void sendPlayStateChangedBroadcast() {
        try {
            Intent intent = new Intent(ACTION_PLAY_STATE_CHANGED);
            intent.putExtra("play_state", playState);
            LocalBroadcastManager.getInstance(this).sendBroadcast(intent);
        } catch (Exception e) {
            Log.e(TAG, "Error sending play state broadcast", e);
        }
    }

    private void sendSongChangedBroadcast() {
        try {
            Intent intent = new Intent(ACTION_SONG_CHANGED);
            intent.putExtra("song_id", currentSong != null ? currentSong.getId() : -1);
            LocalBroadcastManager.getInstance(this).sendBroadcast(intent);
        } catch (Exception e) {
            Log.e(TAG, "Error sending song changed broadcast", e);
        }
    }

    private void sendPlayModeChangedBroadcast() {
        try {
            Intent intent = new Intent(ACTION_PLAY_MODE_CHANGED);
            intent.putExtra("play_mode", playMode);
            LocalBroadcastManager.getInstance(this).sendBroadcast(intent);
        } catch (Exception e) {
            Log.e(TAG, "Error sending play mode broadcast", e);
        }
    }

    // Getter方法
    public boolean isPlaying() {
        return playState == STATE_PLAYING;
    }

    public boolean isPaused() {
        return playState == STATE_PAUSED;
    }

    public Song getCurrentSong() {
        return currentSong;
    }

    public int getCurrentPosition() {
        try {
            return mediaPlayer != null ? mediaPlayer.getCurrentPosition() : 0;
        } catch (Exception e) {
            Log.e(TAG, "Error getting current position", e);
            return 0;
        }
    }

    public int getDuration() {
        try {
            return mediaPlayer != null ? mediaPlayer.getDuration() : 0;
        } catch (Exception e) {
            Log.e(TAG, "Error getting duration", e);
            return 0;
        }
    }

    public int getPlayState() {
        return playState;
    }

    public int getPlayMode() {
        return playMode;
    }

    public List<Song> getPlaylist() {
        return new ArrayList<>(playlist);
    }

    public int getCurrentIndex() {
        return currentIndex;
    }

    /**
     * 关闭音乐服务
     */
    public void shutdown() {
        stop();
        stopSelf();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        Log.d(TAG, "Service destroyed");
        
        try {
            recordPlayHistory();
            stopProgressUpdates();
            
            // 取消所有正在进行的任务
            if (currentPlayTask != null && !currentPlayTask.isDone()) {
                currentPlayTask.cancel(true);
            }
            
            if (mediaPlayer != null) {
                if (mediaPlayer.isPlaying()) {
                    mediaPlayer.stop();
                }
                mediaPlayer.release();
                mediaPlayer = null;
            }
            
            abandonAudioFocus();
            
            try {
                unregisterReceiver(notificationReceiver);
            } catch (Exception e) {
                Log.e(TAG, "Error unregistering receiver", e);
            }
            
            // 关闭后台任务执行器
            if (backgroundExecutor != null) {
                backgroundExecutor.shutdown();
                backgroundExecutor = null;
            }
            
            if (mediaPlayerExecutor != null) {
                mediaPlayerExecutor.shutdown();
                mediaPlayerExecutor = null;
            }
            
            // 清理ANR watchdog
            if (anrWatchdog != null) {
                anrWatchdog.removeCallbacksAndMessages(null);
                anrWatchdog = null;
            }
            
            // 停止前台服务
            stopForeground(true);
        } catch (Exception e) {
            Log.e(TAG, "Error in onDestroy", e);
        }
    }
} 
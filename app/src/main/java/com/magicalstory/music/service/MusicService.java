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

    private MediaPlayer mediaPlayer;
    private List<Song> playlist;
    private Song currentSong;
    private int currentIndex = -1;
    private int playState = STATE_IDLE;
    private int playMode = PLAY_MODE_SEQUENCE;
    private boolean isShuffleEnabled = false;
    private List<Integer> shuffleList;
    private Random random;

    private NotificationManager notificationManager;
    private Handler progressHandler;
    private Runnable progressRunnable;
    private AudioManager audioManager;
    private AudioManager.OnAudioFocusChangeListener audioFocusChangeListener;

    private long playStartTime;
    private long totalPlayTime;

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
            @Override
            public void run() {
                if (mediaPlayer != null && mediaPlayer.isPlaying()) {
                    int currentPosition = mediaPlayer.getCurrentPosition();
                    int duration = mediaPlayer.getDuration();
                    
                    // 发送进度更新广播
                    Intent intent = new Intent(ACTION_PROGRESS_UPDATED);
                    intent.putExtra("current_position", currentPosition);
                    intent.putExtra("duration", duration);
                    LocalBroadcastManager.getInstance(MusicService.this).sendBroadcast(intent);
                    
                    // 每秒更新一次
                    progressHandler.postDelayed(this, 1000);
                }
            }
        };
    }

    private void setupAudioFocus() {
        audioFocusChangeListener = focusChange -> {
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
        if (currentSong == null) {
            if (!playlist.isEmpty()) {
                playSong(0);
            }
            return;
        }

        if (mediaPlayer == null) {
            initializeMediaPlayer();
        }

        if (playState == STATE_PAUSED) {
            if (requestAudioFocus()) {
                mediaPlayer.start();
                playState = STATE_PLAYING;
                playStartTime = System.currentTimeMillis();
                startProgressUpdates();
                updateNotification();
                sendPlayStateChangedBroadcast();
            }
        } else if (playState == STATE_IDLE || playState == STATE_STOPPED) {
            playSong(currentIndex);
        }
    }

    public void pause() {
        if (mediaPlayer != null && mediaPlayer.isPlaying()) {
            mediaPlayer.pause();
            playState = STATE_PAUSED;
            updatePlayTime();
            stopProgressUpdates();
            updateNotification(); // 保持前台服务，只更新通知
            sendPlayStateChangedBroadcast();
            abandonAudioFocus();
        }
    }

    public void stop() {
        if (mediaPlayer != null) {
            if (mediaPlayer.isPlaying()) {
                mediaPlayer.stop();
            }
            updatePlayTime();
            recordPlayHistory();
            playState = STATE_STOPPED;
            stopProgressUpdates();
            updateNotification();
            sendPlayStateChangedBroadcast();
            abandonAudioFocus();
        }
    }

    /**
     * 停止音乐播放但保持服务运行
     */
    public void stopPlayback() {
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
    }

    public void seekTo(int position) {
        if (mediaPlayer != null && playState != STATE_IDLE) {
            mediaPlayer.seekTo(position);
            // 更新播放历史进度
            if (currentSong != null) {
                double progress = (double) position / mediaPlayer.getDuration();
                updatePlayHistoryProgress(progress);
            }
        }
    }

    public void playSong(int index) {
        if (index < 0 || index >= playlist.size()) return;

        // 保存当前播放记录
        if (currentSong != null) {
            recordPlayHistory();
        }

        currentIndex = index;
        currentSong = playlist.get(index);
        
        if (mediaPlayer != null) {
            mediaPlayer.reset();
        } else {
            initializeMediaPlayer();
        }

        playState = STATE_PREPARING;
        playStartTime = System.currentTimeMillis();
        totalPlayTime = 0;
        
        try {
            mediaPlayer.setDataSource(currentSong.getPath());
            mediaPlayer.prepareAsync();
            sendSongChangedBroadcast();
        } catch (IOException e) {
            Log.e(TAG, "Error setting data source", e);
            playState = STATE_ERROR;
            sendPlayStateChangedBroadcast();
        }
    }

    public void playSong(Song song) {
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
        playlist.addAll(songs);
        updateShuffleList();
    }

    public void addToPlaylist(Song song) {
        playlist.add(song);
        updateShuffleList();
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

    // MediaPlayer回调
    @Override
    public void onPrepared(MediaPlayer mp) {
        if (requestAudioFocus()) {
            mp.start();
            playState = STATE_PLAYING;
            startProgressUpdates();
            updateNotification();
            sendPlayStateChangedBroadcast();
        }
    }

    @Override
    public void onCompletion(MediaPlayer mp) {
        recordPlayHistory();
        
        if (playMode == PLAY_MODE_SINGLE) {
            // 单曲循环
            playSong(currentIndex);
        } else {
            // 播放下一首
            playNext();
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
        mediaPlayer = new MediaPlayer();
        mediaPlayer.setOnPreparedListener(this);
        mediaPlayer.setOnCompletionListener(this);
        mediaPlayer.setOnErrorListener(this);
        mediaPlayer.setAudioStreamType(AudioManager.STREAM_MUSIC);
    }

    private boolean requestAudioFocus() {
        int result = audioManager.requestAudioFocus(
                audioFocusChangeListener,
                AudioManager.STREAM_MUSIC,
                AudioManager.AUDIOFOCUS_GAIN
        );
        return result == AudioManager.AUDIOFOCUS_REQUEST_GRANTED;
    }

    private void abandonAudioFocus() {
        audioManager.abandonAudioFocus(audioFocusChangeListener);
    }

    private void startProgressUpdates() {
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

    private void recordPlayHistory() {
        if (currentSong != null) {
            updatePlayTime();
            boolean isCompleted = mediaPlayer != null && 
                    mediaPlayer.getCurrentPosition() >= mediaPlayer.getDuration() * 0.8;
            double progress = mediaPlayer != null ? 
                    (double) mediaPlayer.getCurrentPosition() / mediaPlayer.getDuration() : 0.0;
            
            PlayHistory.recordPlay(currentSong.getId(), totalPlayTime, isCompleted, progress);
        }
    }

    private void updatePlayHistoryProgress(double progress) {
        if (currentSong != null) {
            PlayHistory existingHistory = org.litepal.LitePal.where("songId = ?", 
                    String.valueOf(currentSong.getId())).findFirst(PlayHistory.class);
            if (existingHistory != null) {
                existingHistory.updatePlayProgress(progress);
            }
        }
    }

    // 通知管理
    private void updateNotification() {
        if (currentSong == null) {
            // 如果没有歌曲，显示默认通知
            showDefaultNotification();
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
        startForeground(NOTIFICATION_ID, notification);
    }

    private void showDefaultNotification() {
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
    }

    private PendingIntent createNotificationAction(String action) {
        Intent intent = new Intent(action);
        return PendingIntent.getBroadcast(this, 0, intent, 
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
    }

    // 广播发送
    private void sendPlayStateChangedBroadcast() {
        Intent intent = new Intent(ACTION_PLAY_STATE_CHANGED);
        intent.putExtra("play_state", playState);
        LocalBroadcastManager.getInstance(this).sendBroadcast(intent);
    }

    private void sendSongChangedBroadcast() {
        Intent intent = new Intent(ACTION_SONG_CHANGED);
        intent.putExtra("song_id", currentSong != null ? currentSong.getId() : -1);
        LocalBroadcastManager.getInstance(this).sendBroadcast(intent);
    }

    private void sendPlayModeChangedBroadcast() {
        Intent intent = new Intent(ACTION_PLAY_MODE_CHANGED);
        intent.putExtra("play_mode", playMode);
        LocalBroadcastManager.getInstance(this).sendBroadcast(intent);
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
        return mediaPlayer != null ? mediaPlayer.getCurrentPosition() : 0;
    }

    public int getDuration() {
        return mediaPlayer != null ? mediaPlayer.getDuration() : 0;
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
        
        recordPlayHistory();
        stopProgressUpdates();
        
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
        
        // 停止前台服务
        stopForeground(true);
    }
} 
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
import android.widget.ImageButton;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import com.google.android.material.imageview.ShapeableImageView;
import com.magicalstory.music.R;
import com.magicalstory.music.databinding.FragmentMiniPlayerBinding;
import com.magicalstory.music.model.Song;
import com.magicalstory.music.service.MusicService;
import com.magicalstory.music.utils.glide.GlideUtils;
import com.magicalstory.music.utils.text.TimeUtils;

/**
 * Mini播放器Fragment
 */
public class MiniPlayerFragment extends Fragment {

    private static final String TAG = "MiniPlayerFragment";

    private FragmentMiniPlayerBinding binding;

    private MusicService musicService;
    private boolean isServiceBound = false;

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
                    // Mini player不需要显示进度
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
        binding = FragmentMiniPlayerBinding.inflate(inflater, container, false);
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
        binding.miniPlayPause.setOnClickListener(v -> {
            if (musicService != null) {
                if (musicService.isPlaying()) {
                    musicService.pause();
                } else {
                    musicService.play();
                }
            }
        });

        // 设置默认状态
        updateDefaultState();
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
        LocalBroadcastManager.getInstance(getContext()).registerReceiver(musicReceiver, filter);
    }

    private void unregisterMusicReceiver() {
        LocalBroadcastManager.getInstance(getContext()).unregisterReceiver(musicReceiver);
    }

    private void updateUI() {
        if (musicService != null) {
            updateSongInfo();
            updatePlayButton(musicService.getPlayState());
        }
    }

    private void updateSongInfo() {
        if (musicService == null) return;

        Song currentSong = musicService.getCurrentSong();
        if (currentSong != null) {
            binding.miniSongName.setText(currentSong.getTitle());
            binding.miniArtistName.setText(currentSong.getArtist());
            
            // 加载封面
            GlideUtils.loadAlbumCover(getContext(), currentSong.getAlbumId(), binding.miniCoverImage);
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
        binding.miniSongName.setText("未播放");
        binding.miniArtistName.setText("选择歌曲开始播放");
        binding.miniPlayPause.setImageResource(R.drawable.ic_play);
        binding.miniCoverImage.setImageResource(R.drawable.place_holder_album);
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
} 
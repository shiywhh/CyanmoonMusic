package com.magicalstory.music.fragment;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.res.ColorStateList;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.widget.Toolbar;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;
import androidx.navigation.Navigation;
import androidx.palette.graphics.Palette;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.bumptech.glide.Glide;
import com.bumptech.glide.request.target.CustomTarget;
import com.bumptech.glide.request.transition.Transition;
import com.google.android.material.appbar.AppBarLayout;
import com.google.android.material.button.MaterialButton;
import com.magicalstory.music.MainActivity;
import com.magicalstory.music.R;
import com.magicalstory.music.base.BaseFragment;
import com.magicalstory.music.databinding.FragmentArtistDetailBinding;
import com.magicalstory.music.adapter.AlbumHorizontalAdapter;
import com.magicalstory.music.adapter.SongVerticalAdapter;
import com.magicalstory.music.model.Album;
import com.magicalstory.music.model.Artist;
import com.magicalstory.music.model.Song;
import com.magicalstory.music.service.MusicService;
import com.magicalstory.music.utils.app.ToastUtils;
import com.magicalstory.music.utils.glide.Glide2;
import com.magicalstory.music.utils.screen.DensityUtil;

import org.litepal.LitePal;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * 歌手详情Fragment
 */
public class ArtistDetailFragment extends BaseFragment<FragmentArtistDetailBinding> {

    private static final String ARG_ARTIST_NAME = "artist_name";

    private Artist currentArtist;
    private List<Song> popularSongs;
    private List<Album> artistAlbums;
    
    private SongVerticalAdapter songAdapter;
    private AlbumHorizontalAdapter albumAdapter;
    
    private ExecutorService executorService;
    private Handler mainHandler;
    
    // 颜色相关
    private int primaryColor = Color.parseColor("#6200EE");
    private int onPrimaryColor = Color.WHITE;
    
    // 广播接收器
    private BroadcastReceiver musicServiceReceiver;

    public static ArtistDetailFragment newInstance(String artistName) {
        ArtistDetailFragment fragment = new ArtistDetailFragment();
        Bundle args = new Bundle();
        args.putString(ARG_ARTIST_NAME, artistName);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    protected FragmentArtistDetailBinding getViewBinding(LayoutInflater inflater, ViewGroup container) {
        return FragmentArtistDetailBinding.inflate(inflater, container, false);
    }

    @Override
    protected boolean usePersistentView() {
        return true;
    }

    @Override
    protected int getLayoutRes() {
        return R.layout.fragment_artist_detail;
    }

    @Override
    protected FragmentArtistDetailBinding bindPersistentView(View view) {
        return FragmentArtistDetailBinding.bind(view);
    }

    @Override
    protected void initViewForPersistentView() {
        super.initViewForPersistentView();
        
        // 初始化Handler和线程池
        mainHandler = new Handler(Looper.getMainLooper());
        executorService = Executors.newCachedThreadPool();
        
        // 设置menu选项
        setHasOptionsMenu(true);
        
        // 初始化RecyclerView
        initRecyclerViews();
        
        // 获取传递的参数
        Bundle args = getArguments();
        if (args != null) {
            String artistName = args.getString(ARG_ARTIST_NAME);
            
            // 加载歌手详情
            loadArtistDetail(artistName);
        }
    }

    @Override
    protected void initView() {
        super.initView();
        // 每次视图创建时需要执行的初始化代码
    }

    @Override
    protected void initListenerForPersistentView() {
        super.initListenerForPersistentView();

        binding.toolbar.setOnMenuItemClickListener(item -> {
            int itemId = item.getItemId();

            if (itemId == R.id.action_shuffle_play) {
                // 随机播放
                binding.btnShufflePlay.performClick();
                return true;
            } else if (itemId == R.id.action_play_next) {
                // 下一首播放
                addToPlayNext();
                return true;
            } else if (itemId == R.id.action_add_to_playlist) {
                // 添加到播放列表
                addToPlaylist();
                return true;
            }

            return false;
        });

        // 返回按钮
        binding.toolbar.setNavigationOnClickListener(v -> {
            Navigation.findNavController(requireView()).popBackStack();
        });

        int albumTitleY = DensityUtil.getScreenHeightAndWeight(context)[0];
        binding.nestedScrollView.setOnScrollChangeListener((View.OnScrollChangeListener) (v, scrollX, scrollY, oldScrollX, oldScrollY) -> {
            if (Math.abs(scrollY) > albumTitleY) {
                if (currentArtist != null) {
                    binding.toolbar.setTitle(currentArtist.getArtistName());
                }
            } else {
                // 否则隐藏toolbar标题
                binding.toolbar.setTitle("");
            }
        });


        
        // 全部播放按钮
        binding.btnPlayAll.setOnClickListener(v -> {
            if (popularSongs != null && !popularSongs.isEmpty()) {
                if (getActivity() instanceof MainActivity mainActivity) {
                    mainActivity.setPlaylist(popularSongs);
                    mainActivity.playSong(popularSongs.get(0));
                }
            }
        });
        
        // 随机播放按钮
        binding.btnShufflePlay.setOnClickListener(v -> {
            if (popularSongs != null && !popularSongs.isEmpty()) {
                if (getActivity() instanceof MainActivity mainActivity) {
                    List<Song> shuffledSongs = new ArrayList<>(popularSongs);
                    Collections.shuffle(shuffledSongs);
                    mainActivity.setPlaylist(shuffledSongs);
                    mainActivity.playSong(shuffledSongs.get(0));
                }
            }
        });
        
        // 查看全部歌曲
        binding.btnViewAllSongs.setOnClickListener(v -> {
            Bundle bundle = new Bundle();
            bundle.putString("dataType", "artist");
            bundle.putString("artistName", currentArtist.getArtistName());
            Navigation.findNavController(requireView()).navigate(R.id.action_artist_detail_to_recent_songs, bundle);
        });
        
        // 查看全部专辑
        binding.btnViewAllAlbums.setOnClickListener(v -> {
            Bundle bundle = new Bundle();
            bundle.putString("artistName", currentArtist.getArtistName());
            Navigation.findNavController(requireView()).navigate(R.id.action_artist_detail_to_albums, bundle);
        });
    }

    @Override
    protected void initListener() {
        super.initListener();
        // 每次视图创建时需要执行的监听器初始化代码
    }

    /**
     * 初始化RecyclerView
     */
    private void initRecyclerViews() {
        // 热门歌曲列表
        binding.rvPopularSongs.setLayoutManager(new LinearLayoutManager(getContext()));
        songAdapter = new SongVerticalAdapter(getContext(), new ArrayList<>());
        binding.rvPopularSongs.setAdapter(songAdapter);
        
        // 专辑列表
        binding.rvAlbums.setLayoutManager(
                new LinearLayoutManager(getContext(), LinearLayoutManager.HORIZONTAL, false));
        albumAdapter = new AlbumHorizontalAdapter(getContext(), new ArrayList<>());
        albumAdapter.setOnItemClickListener((album, position) -> {
            // 跳转到专辑详情
            Bundle bundle = new Bundle();
            bundle.putLong("album_id", album.getAlbumId());
            bundle.putString("artist_name", album.getArtist());
            bundle.putString("album_name", album.getAlbumName());
            Navigation.findNavController(requireView()).navigate(R.id.action_artist_detail_to_album_detail, bundle);
        });
        binding.rvAlbums.setAdapter(albumAdapter);
        
        // 注册MusicService广播接收器
        registerMusicServiceReceiver();
        
        // 获取当前播放歌曲并设置到适配器
        updateCurrentPlayingSong();
    }

    /**
     * 加载歌手详情
     */
    private void loadArtistDetail(String artistName) {
        binding.progressBar.setVisibility(View.VISIBLE);
        
        executorService.execute(() -> {
            try {
                // 查询歌手信息
                currentArtist = LitePal.where("artistName = ?", artistName).findFirst(Artist.class);
                
                if (currentArtist == null) {
                    // 如果数据库中没有歌手信息，创建一个基本的歌手对象
                    currentArtist = new Artist();
                    currentArtist.setArtistName(artistName);
                }
                
                // 查询歌手的热门歌曲（按播放次数排序，取前10首）
                popularSongs = LitePal.where("artist = ?", artistName)
                        .order("dateAdded desc")
                        .limit(10)
                        .find(Song.class);
                
                // 更新歌手歌曲数量
                if (popularSongs != null) {
                    currentArtist.setSongCount(popularSongs.size());
                }
                
                // 查询歌手的专辑
                artistAlbums = LitePal.where("artist = ?", artistName)
                        .limit(10)
                        .find(Album.class);
                
                // 在主线程更新UI
                mainHandler.post(() -> {
                    updateUI();
                    binding.progressBar.setVisibility(View.GONE);
                });
                
            } catch (Exception e) {
                e.printStackTrace();
                mainHandler.post(() -> {
                    binding.progressBar.setVisibility(View.GONE);
                    ToastUtils.showToast(getContext(), "加载歌手详情失败");
                });
            }
        });
    }

    /**
     * 更新UI
     */
    private void updateUI() {
        if (currentArtist == null) return;
        
        // 设置歌手名称
        binding.tvArtistName.setText(currentArtist.getArtistName());
        
        // 设置歌曲数量
        int totalSongCount = LitePal.where("artist = ?", currentArtist.getArtistName()).count(Song.class);
        binding.tvSongCount.setText(totalSongCount + " 首歌曲");
        
        // 加载歌手头像并提取颜色
        loadArtistAvatar();
        
        // 更新热门歌曲列表
        updatePopularSongsList();
        
        // 更新专辑列表
        updateAlbumsList();
    }

    /**
     * 加载歌手头像
     */
    private void loadArtistAvatar() {
        if (currentArtist.getCoverUrl() != null && !currentArtist.getCoverUrl().isEmpty()) {
            Glide.with(this)
                    .asBitmap()
                    .load(currentArtist.getCoverUrl())
                    .placeholder(R.drawable.place_holder_artist)
                    .error(R.drawable.place_holder_artist)
                    .into(new CustomTarget<Bitmap>() {
                        @Override
                        public void onResourceReady(@NonNull Bitmap resource, @Nullable Transition<? super Bitmap> transition) {
                            if (binding == null) {
                                return;
                            }
                            binding.ivArtistAvatar.setImageBitmap(resource);
                            
                            // 使用Palette进行取色
                            Palette.from(resource).generate(palette -> {
                                if (palette != null) {
                                    applyPaletteColors(palette);
                                }
                            });
                        }

                        @Override
                        public void onLoadCleared(@Nullable Drawable placeholder) {
                            if (binding == null) {
                                return;
                            }
                            binding.ivArtistAvatar.setImageDrawable(placeholder);
                            // 使用默认颜色
                            applyDefaultColors();
                        }
                    });
        } else {
            // 使用默认头像
            binding.ivArtistAvatar.setImageResource(R.drawable.place_holder_artist);
            applyDefaultColors();
        }
    }

    /**
     * 更新热门歌曲列表
     */
    private void updatePopularSongsList() {
        if (popularSongs != null && !popularSongs.isEmpty()) {
            songAdapter.updateData(popularSongs);
            binding.layoutPopularSongs.setVisibility(View.VISIBLE);
            
            // 获取歌手的所有歌曲数量
            int totalSongCount = LitePal.where("artist = ?", currentArtist.getArtistName()).count(Song.class);
            if (totalSongCount > 10) {
                binding.btnViewAllSongs.setVisibility(View.VISIBLE);
            } else {
                binding.btnViewAllSongs.setVisibility(View.GONE);
            }
        } else {
            binding.layoutPopularSongs.setVisibility(View.GONE);
        }
    }

    /**
     * 更新专辑列表
     */
    private void updateAlbumsList() {
        if (artistAlbums != null && !artistAlbums.isEmpty()) {
            albumAdapter.updateData(artistAlbums);
            binding.layoutAlbums.setVisibility(View.VISIBLE);
            
            // 查询歌手的所有专辑数量
            int totalAlbumCount = LitePal.where("artist = ?", currentArtist.getArtistName()).count(Album.class);
            if (totalAlbumCount > artistAlbums.size()) {
                binding.btnViewAllAlbums.setVisibility(View.VISIBLE);
            } else {
                binding.btnViewAllAlbums.setVisibility(View.GONE);
            }
        } else {
            binding.layoutAlbums.setVisibility(View.GONE);
        }
    }

    /**
     * 应用调色板颜色
     */
    private void applyPaletteColors(Palette palette) {
        // 获取主要颜色
        int vibrantColor = palette.getVibrantColor(primaryColor);
        int darkVibrantColor = palette.getDarkVibrantColor(primaryColor);
        int mutedColor = palette.getMutedColor(primaryColor);
        
        // 选择最适合的颜色
        primaryColor = vibrantColor != primaryColor ? vibrantColor : 
                      (darkVibrantColor != primaryColor ? darkVibrantColor : mutedColor);
        
        // 应用颜色到按钮
        updateButtonColors();
        
        // 创建渐变罩层
        createGradientOverlay();
    }

    /**
     * 应用默认颜色
     */
    private void applyDefaultColors() {
        primaryColor = Color.parseColor("#6200EE");
        updateButtonColors();
    }

    /**
     * 更新按钮颜色
     */
    private void updateButtonColors() {
        // 全部播放按钮 - 填充样式
        MaterialButton playAllButton = (MaterialButton) binding.btnPlayAll;
        playAllButton.setBackgroundTintList(ColorStateList.valueOf(primaryColor));
        playAllButton.setTextColor(onPrimaryColor);
        playAllButton.setIconTint(ColorStateList.valueOf(onPrimaryColor));
        
        // 随机播放按钮 - 边框样式
        MaterialButton shufflePlayButton = (MaterialButton) binding.btnShufflePlay;
        shufflePlayButton.setBackgroundTintList(ColorStateList.valueOf(Color.TRANSPARENT));
        shufflePlayButton.setTextColor(primaryColor);
        shufflePlayButton.setIconTint(ColorStateList.valueOf(primaryColor));
        shufflePlayButton.setStrokeColor(ColorStateList.valueOf(primaryColor));
        
        // 查看全部按钮颜色
        binding.btnViewAllSongs.setTextColor(primaryColor);
        binding.btnViewAllAlbums.setTextColor(primaryColor);
    }

    /**
     * 创建渐变罩层
     */
    private void createGradientOverlay() {
        GradientDrawable gradientDrawable = new GradientDrawable(
                GradientDrawable.Orientation.BOTTOM_TOP,
                new int[]{primaryColor, Color.TRANSPARENT}
        );
        
        binding.gradientOverlay.setBackground(gradientDrawable);
        binding.gradientOverlay.setVisibility(View.VISIBLE);
    }

    /**
     * 注册MusicService广播接收器
     */
    private void registerMusicServiceReceiver() {
        musicServiceReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                String action = intent.getAction();
                if (MusicService.ACTION_SONG_CHANGED.equals(action)) {
                    // 歌曲发生变化时更新适配器
                    long songId = intent.getLongExtra("song_id", -1);
                    if (songAdapter != null) {
                        songAdapter.setCurrentPlayingSongId(songId);
                    }
                } else if (MusicService.ACTION_PLAY_STATE_CHANGED.equals(action)) {
                    // 播放状态变化时也更新当前播放歌曲状态
                    updateCurrentPlayingSong();
                }
            }
        };

        // 注册广播接收器
        IntentFilter filter = new IntentFilter();
        filter.addAction(MusicService.ACTION_SONG_CHANGED);
        filter.addAction(MusicService.ACTION_PLAY_STATE_CHANGED);
        LocalBroadcastManager.getInstance(requireContext()).registerReceiver(musicServiceReceiver, filter);
    }

    /**
     * 更新当前播放歌曲的状态
     */
    private void updateCurrentPlayingSong() {
        if (context instanceof MainActivity mainActivity) {
            Song currentSong = mainActivity.getCurrentSong();
            if (currentSong != null && songAdapter != null) {
                songAdapter.setCurrentPlayingSongId(currentSong.getId());
            }
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        
        // 取消注册广播接收器
        if (musicServiceReceiver != null) {
            LocalBroadcastManager.getInstance(requireContext()).unregisterReceiver(musicServiceReceiver);
            musicServiceReceiver = null;
        }
        
        // 清理ExecutorService
        if (executorService != null) {
            executorService.shutdown();
            executorService = null;
        }
        
        // 清理Handler
        if (mainHandler != null) {
            mainHandler.removeCallbacksAndMessages(null);
            mainHandler = null;
        }
    }

    /**
     * 添加到播放队列的下一首位置
     */
    private void addToPlayNext() {
        if (popularSongs != null && !popularSongs.isEmpty()) {
            if (getActivity() instanceof MainActivity mainActivity) {
                // 添加到播放队列的下一首位置
                // 这里需要实现将歌手歌曲添加到播放队列的下一首位置
                // 由于当前项目中没有看到具体的播放队列管理，先用Toast提示
                ToastUtils.showToast(getContext(), "已添加到播放队列的下一首");
            }
        } else {
            ToastUtils.showToast(getContext(), "没有歌曲可添加");
        }
    }

    /**
     * 添加到播放列表
     */
    private void addToPlaylist() {
        if (popularSongs != null && !popularSongs.isEmpty()) {
            // 这里可以实现添加到播放列表的功能
            // 可以弹出一个对话框让用户选择现有播放列表或创建新的播放列表
            // 由于当前项目中没有看到具体的播放列表管理，先用Toast提示
            ToastUtils.showToast(getContext(), "已添加到播放列表");
        } else {
            ToastUtils.showToast(getContext(), "没有歌曲可添加");
        }
    }

    @Override
    public boolean autoHideBottomNavigation() {
        return true;
    }
} 
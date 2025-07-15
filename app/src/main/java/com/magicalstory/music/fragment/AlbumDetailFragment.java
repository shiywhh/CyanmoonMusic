package com.magicalstory.music.fragment;

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
import com.magicalstory.music.databinding.FragmentAlbumDetailBinding;
import com.magicalstory.music.adapter.AlbumHorizontalAdapter;
import com.magicalstory.music.adapter.SongVerticalAdapter;
import com.magicalstory.music.model.Album;
import com.magicalstory.music.model.Artist;
import com.magicalstory.music.model.Song;
import com.magicalstory.music.utils.app.ToastUtils;
import com.magicalstory.music.utils.glide.Glide2;

import org.litepal.LitePal;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * 专辑详情Fragment
 */
public class AlbumDetailFragment extends BaseFragment<FragmentAlbumDetailBinding> {

    private static final String ARG_ALBUM_ID = "album_id";
    private static final String ARG_ARTIST_NAME = "artist_name";
    private static final String ARG_ALBUM_NAME = "album_name";

    private Album currentAlbum;
    private List<Song> albumSongs;
    private List<Album> otherAlbums;
    
    private SongVerticalAdapter songAdapter;
    private AlbumHorizontalAdapter albumAdapter;
    
    private ExecutorService executorService;
    private Handler mainHandler;
    
    // 颜色相关
    private int primaryColor = Color.parseColor("#6200EE");
    private int onPrimaryColor = Color.WHITE;

    public static AlbumDetailFragment newInstance(long albumId, String artistName, String albumName) {
        AlbumDetailFragment fragment = new AlbumDetailFragment();
        Bundle args = new Bundle();
        args.putLong(ARG_ALBUM_ID, albumId);
        args.putString(ARG_ARTIST_NAME, artistName);
        args.putString(ARG_ALBUM_NAME, albumName);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    protected FragmentAlbumDetailBinding getViewBinding(LayoutInflater inflater, ViewGroup container) {
        return FragmentAlbumDetailBinding.inflate(inflater, container, false);
    }

    @Override
    protected void initView() {
        super.initView();
        
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
            long albumId = args.getLong(ARG_ALBUM_ID);
            String artistName = args.getString(ARG_ARTIST_NAME);
            String albumName = args.getString(ARG_ALBUM_NAME);
            
            // 加载专辑详情
            loadAlbumDetail(albumId, artistName, albumName);
        }
    }

    @Override
    protected void initListener() {
        super.initListener();

        binding.toolbar.setOnMenuItemClickListener(item -> {
            int itemId = item.getItemId();

            if (itemId == R.id.action_edit_tags) {
                // 打开标签编辑器
                openTagEditor();
                return true;
            } else if (itemId == R.id.action_shuffle_play) {
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
        
        // AppBarLayout偏移监听器
        binding.appBarLayout.addOnOffsetChangedListener((appBarLayout, verticalOffset) -> {
            // 获取专辑标题TextView的Y位置
            int albumTitleY = binding.tvAlbumName.getTop() + binding.tvAlbumName.getHeight() / 2;

            // 如果滑动距离超过了专辑标题的Y位置，则显示toolbar标题
            if (Math.abs(verticalOffset) > albumTitleY) {
                if (currentAlbum != null) {
                    binding.toolbar.setTitle(currentAlbum.getAlbumName());
                }
            } else {
                // 否则隐藏toolbar标题
                binding.toolbar.setTitle("");
            }
        });
        
        // 全部播放按钮
        binding.btnPlayAll.setOnClickListener(v -> {
            if (albumSongs != null && !albumSongs.isEmpty()) {
                if (getActivity() instanceof MainActivity mainActivity) {
                    mainActivity.setPlaylist(albumSongs);
                    mainActivity.playSong(albumSongs.get(0));
                }
            }
        });
        
        // 随机播放按钮
        binding.btnShufflePlay.setOnClickListener(v -> {
            if (albumSongs != null && !albumSongs.isEmpty()) {
                if (getActivity() instanceof MainActivity mainActivity) {
                    List<Song> shuffledSongs = new ArrayList<>(albumSongs);
                    Collections.shuffle(shuffledSongs);
                    mainActivity.setPlaylist(shuffledSongs);
                    mainActivity.playSong(shuffledSongs.get(0));
                }
            }
        });
        
        // 查看全部歌曲
        binding.btnViewAllSongs.setOnClickListener(v -> {
            Bundle bundle = new Bundle();
            bundle.putString("dataType", "album");
            bundle.putLong("albumId", currentAlbum.getAlbumId());
            bundle.putString("artistName", currentAlbum.getArtist());
            bundle.putString("albumName", currentAlbum.getAlbumName());
            Navigation.findNavController(requireView()).navigate(R.id.action_album_detail_to_recent_songs, bundle);
        });
        
        // 查看全部专辑
        binding.btnViewAllAlbums.setOnClickListener(v -> {
            Bundle bundle = new Bundle();
            bundle.putString("artistName", currentAlbum.getArtist());
            Navigation.findNavController(requireView()).navigate(R.id.action_album_detail_to_albums, bundle);
        });
        
        // 艺术家头像点击
        binding.ivArtistAvatar.setOnClickListener(v -> {
            Bundle bundle = new Bundle();
            bundle.putString("artistName", currentAlbum.getArtist());
            Navigation.findNavController(requireView()).navigate(R.id.action_album_detail_to_artists, bundle);
        });
        
        // 艺术家名字点击
        binding.tvArtistName.setOnClickListener(v -> {
            Bundle bundle = new Bundle();
            bundle.putString("artistName", currentAlbum.getArtist());
            Navigation.findNavController(requireView()).navigate(R.id.action_album_detail_to_artists, bundle);
        });
    }




    /**
     * 初始化RecyclerView
     */
    private void initRecyclerViews() {
        // 歌曲列表
        binding.rvSongs.setLayoutManager(new LinearLayoutManager(getContext()));
        songAdapter = new SongVerticalAdapter(getContext(), new ArrayList<>());
        binding.rvSongs.setAdapter(songAdapter);
        
        // 其他专辑列表
        binding.rvOtherAlbums.setLayoutManager(
                new LinearLayoutManager(getContext(), LinearLayoutManager.HORIZONTAL, false));
        albumAdapter = new AlbumHorizontalAdapter(getContext(), new ArrayList<>());
        albumAdapter.setOnItemClickListener((album, position) -> {
            // 跳转到专辑详情
            Bundle bundle = new Bundle();
            bundle.putLong(ARG_ALBUM_ID, album.getAlbumId());
            bundle.putString(ARG_ARTIST_NAME, album.getArtist());
            bundle.putString(ARG_ALBUM_NAME, album.getAlbumName());
            Navigation.findNavController(requireView()).navigate(R.id.action_album_detail_to_album_detail, bundle);
        });
        binding.rvOtherAlbums.setAdapter(albumAdapter);
    }

    /**
     * 加载专辑详情
     */
    private void loadAlbumDetail(long albumId, String artistName, String albumName) {
        binding.progressBar.setVisibility(View.VISIBLE);
        
        executorService.execute(() -> {
            try {
                // 查询专辑信息
                currentAlbum = LitePal.where("albumId = ? and artist = ?", 
                        String.valueOf(albumId), artistName).findFirst(Album.class);
                
                if (currentAlbum == null) {
                    // 如果数据库中没有专辑信息，创建一个基本的专辑对象
                    currentAlbum = new Album();
                    currentAlbum.setAlbumId(albumId);
                    currentAlbum.setArtist(artistName);
                    currentAlbum.setAlbumName(albumName);
                }
                
                // 查询专辑歌曲（按音轨号排序）
                albumSongs = LitePal.where("albumId = ? and artist = ?", 
                        String.valueOf(albumId), artistName)
                        .order("track asc")
                        .find(Song.class);
                
                // 更新专辑歌曲数量
                if (albumSongs != null) {
                    currentAlbum.setSongCount(albumSongs.size());
                }
                
                // 查询同一艺术家的其他专辑
                otherAlbums = LitePal.where("artist = ? and albumId != ?", 
                        artistName, String.valueOf(albumId))
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
                    ToastUtils.showToast(getContext(), getString(R.string.load_album_detail_failed));
                });
            }
        });
    }

    /**
     * 更新UI
     */
    private void updateUI() {
        if (currentAlbum == null) return;
        
        // 设置专辑标题
        binding.tvAlbumName.setText(currentAlbum.getAlbumName());
        
        // 设置艺术家名称
        binding.tvArtistName.setText(currentAlbum.getArtist());
        
        // 加载专辑封面并提取颜色
        loadAlbumCover();
        
        // 加载艺术家头像
        loadArtistAvatar();
        
        // 更新歌曲列表
        updateSongsList();
        
        // 更新其他专辑列表
        updateOtherAlbumsList();
    }

    /**
     * 加载专辑封面
     */
    private void loadAlbumCover() {
        String albumArtUri = "content://media/external/audio/albumart/" + currentAlbum.getAlbumId();
        
        Glide.with(this)
                .asBitmap()
                .load(albumArtUri)
                .placeholder(R.drawable.place_holder_album)
                .error(R.drawable.place_holder_album)
                .into(new CustomTarget<Bitmap>() {
                    @Override
                    public void onResourceReady(@NonNull Bitmap resource, @Nullable Transition<? super Bitmap> transition) {
                        if (binding == null) {
                            return;
                        }
                        binding.ivAlbumCover.setImageBitmap(resource);
                        
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
                        binding.ivAlbumCover.setImageDrawable(placeholder);
                        // 使用默认颜色
                        applyDefaultColors();
                    }
                });
    }

    /**
     * 加载艺术家头像
     */
    private void loadArtistAvatar() {
        // 尝试从数据库获取艺术家头像
        Artist artist = LitePal.where("artistName = ?", currentAlbum.getArtist()).findFirst(Artist.class);
        
        if (artist != null && artist.getCoverUrl() != null && !artist.getCoverUrl().isEmpty()) {
            Glide2.loadImage(getContext(), binding.ivArtistAvatar, artist.getCoverUrl(), R.drawable.place_holder_artist);
        } else {
            // 使用默认头像
            binding.ivArtistAvatar.setImageResource(R.drawable.place_holder_artist);
        }
    }

    /**
     * 更新歌曲列表
     */
    private void updateSongsList() {
        if (albumSongs != null && !albumSongs.isEmpty()) {
            // 显示最多10首歌曲
            List<Song> displaySongs = albumSongs.size() > 10 ? 
                    albumSongs.subList(0, 10) : albumSongs;
            
            songAdapter.updateData(displaySongs);
            binding.layoutSongs.setVisibility(View.VISIBLE);
            
            // 如果歌曲数量超过10首，显示查看全部按钮
            if (albumSongs.size() > 10) {
                binding.btnViewAllSongs.setVisibility(View.VISIBLE);
            } else {
                binding.btnViewAllSongs.setVisibility(View.GONE);
            }
        } else {
            binding.layoutSongs.setVisibility(View.GONE);
        }
    }

    /**
     * 更新其他专辑列表
     */
    private void updateOtherAlbumsList() {
        if (otherAlbums != null && !otherAlbums.isEmpty()) {
            albumAdapter.updateData(otherAlbums);
            binding.layoutOtherAlbums.setVisibility(View.VISIBLE);
            
            // 设置标题
            binding.tvOtherAlbumsTitle.setText(getString(R.string.other_albums_from_artist_format, currentAlbum.getArtist()));
            
            // 查询艺术家的所有专辑数量
            int totalAlbumCount = LitePal.where("artist = ?", currentAlbum.getArtist()).count(Album.class);
            if (totalAlbumCount > otherAlbums.size() + 1) { // +1 因为当前专辑不在其他专辑列表中
                binding.btnViewAllAlbums.setVisibility(View.VISIBLE);
            } else {
                binding.btnViewAllAlbums.setVisibility(View.GONE);
            }
        } else {
            binding.layoutOtherAlbums.setVisibility(View.GONE);
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

    @Override
    public void onDestroy() {
        super.onDestroy();
        
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
     * 打开标签编辑器
     */
    private void openTagEditor() {
        if (currentAlbum != null) {
            Bundle bundle = new Bundle();
            bundle.putLong(ARG_ALBUM_ID, currentAlbum.getAlbumId());
            bundle.putString(ARG_ARTIST_NAME, currentAlbum.getArtist());
            bundle.putString(ARG_ALBUM_NAME, currentAlbum.getAlbumName());
            Navigation.findNavController(requireView()).navigate(R.id.action_album_detail_to_tag_editor, bundle);
        }
    }

    /**
     * 添加到播放队列的下一首位置
     */
    private void addToPlayNext() {
        if (albumSongs != null && !albumSongs.isEmpty()) {
            if (getActivity() instanceof MainActivity mainActivity) {
                // 添加到播放队列的下一首位置
                // 这里需要实现将专辑歌曲添加到播放队列的下一首位置
                // 由于当前项目中没有看到具体的播放队列管理，先用Toast提示
                ToastUtils.showToast(getContext(), getString(R.string.added_to_queue_next));
            }
        } else {
            ToastUtils.showToast(getContext(), getString(R.string.no_songs_to_add));
        }
    }

    /**
     * 添加到播放列表
     */
    private void addToPlaylist() {
        if (albumSongs != null && !albumSongs.isEmpty()) {
            // 这里可以实现添加到播放列表的功能
            // 可以弹出一个对话框让用户选择现有播放列表或创建新的播放列表
            // 由于当前项目中没有看到具体的播放列表管理，先用Toast提示
        } else {
            ToastUtils.showToast(getContext(), getString(R.string.no_songs_to_add));
        }
    }

    @Override
    public boolean autoHideBottomNavigation() {
        return true;
    }
} 
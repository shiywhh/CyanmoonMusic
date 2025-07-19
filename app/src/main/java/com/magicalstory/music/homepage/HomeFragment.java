package com.magicalstory.music.homepage;

import android.Manifest;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.os.Build;
import android.os.Bundle;
import android.os.IBinder;
import android.text.TextUtils;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.os.Handler;
import android.os.Looper;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;
import androidx.media3.common.util.UnstableApi;
import androidx.navigation.Navigation;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.google.gson.reflect.TypeToken;
import com.hjq.gson.factory.GsonFactory;
import com.magicalstory.music.R;
import com.google.android.material.search.SearchView;
import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipGroup;
import com.magicalstory.music.MainActivity;
import com.magicalstory.music.base.BaseFragment;
import com.magicalstory.music.databinding.FragmentHomeBinding;
import com.magicalstory.music.dialog.dialogUtils;
import com.magicalstory.music.player.MediaControllerHelper;
import com.magicalstory.music.player.PlaylistManager;
import com.magicalstory.music.service.MusicScanService;
import com.magicalstory.music.utils.app.ToastUtils;
import com.magicalstory.music.adapter.SongHorizontalAdapter;
import com.magicalstory.music.adapter.AlbumHorizontalAdapter;
import com.magicalstory.music.adapter.ArtistHorizontalAdapter;
import com.magicalstory.music.adapter.SearchResultAdapter;
import com.magicalstory.music.model.Song;
import com.magicalstory.music.model.Album;
import com.magicalstory.music.model.Artist;
import com.magicalstory.music.model.Playlist;
import com.magicalstory.music.utils.network.NetUtils;
import com.google.gson.JsonObject;
import com.google.gson.JsonArray;
import com.google.gson.JsonParser;

import org.litepal.LitePal;

import java.lang.reflect.Type;
import java.util.List;
import java.util.ArrayList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import okhttp3.Response;

import com.magicalstory.music.model.FavoriteSong;
import com.magicalstory.music.utils.glide.ColorExtractor;
import com.magicalstory.music.utils.glide.CoverFallbackUtils;
import com.magicalstory.music.service.CoverFetchService;
import com.magicalstory.music.utils.screen.DensityUtil;
import com.magicalstory.music.utils.sync.MusicSyncUtils;
import com.tencent.mmkv.MMKV;

@UnstableApi
public class HomeFragment extends BaseFragment<FragmentHomeBinding> {

    private MusicScanService musicScanService;
    private boolean serviceBound = false;

    // 权限请求启动器
    private ActivityResultLauncher<String[]> permissionLauncher;

    // 适配器
    private SongHorizontalAdapter songsLatestAddedAdapter;
    private AlbumHorizontalAdapter recentAlbumsAdapter;
    private ArtistHorizontalAdapter recentArtistsAdapter;
    private SongHorizontalAdapter myFavoritesAdapter;
    private SongHorizontalAdapter randomRecommendationsAdapter;

    // 预设的gradient drawable列表 - 改为动态生成
    private List<GradientDrawable> songsLatestAddedGradients;
    // 预设的颜色值数组 - 改为动态生成
    private List<Integer> songsLatestAddedColors;

    // 搜索相关
    private SearchResultAdapter searchResultAdapter;
    private String currentSearchQuery = "";
    private String currentSearchType = "all"; // all, songs, album, artist, playlist

    // 网络请求相关
    private ExecutorService executorService;
    private Handler mainHandler;

    // 扫描完成广播接收器
    private final BroadcastReceiver scanCompleteReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            android.util.Log.d("HomeFragment", "收到广播: " + intent.getAction());
            if (MusicScanService.ACTION_SCAN_COMPLETE.equals(intent.getAction())) {
                int scanCount = intent.getIntExtra(MusicScanService.EXTRA_SCAN_COUNT, 0);
                android.util.Log.d("HomeFragment", "扫描完成广播，新增歌曲数量: " + scanCount);
                ToastUtils.showToast(getContext(), "扫描完成，新增 " + scanCount + " 首歌曲");
                // 扫描完成后重新加载歌曲并显示布局
                reloadMusicDataAndShowLayout();
            }
        }
    };

    // 删除事件广播接收器
    private final BroadcastReceiver deleteEventReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            android.util.Log.d("HomeFragment", "收到删除广播: " + intent.getAction());

            if (com.magicalstory.music.utils.file.FileDeleteUtils.ACTION_DELETE_SONGS.equals(intent.getAction())) {
                // 处理删除歌曲事件
                List<Long> deletedSongIds = intent.getParcelableArrayListExtra(
                        com.magicalstory.music.utils.file.FileDeleteUtils.EXTRA_DELETED_SONG_IDS, Long.class);
                if (deletedSongIds != null && !deletedSongIds.isEmpty()) {
                    handleSongsDeleted(deletedSongIds);
                }
            } else if (com.magicalstory.music.utils.file.FileDeleteUtils.ACTION_DELETE_ALBUMS.equals(intent.getAction())) {
                // 处理删除专辑事件
                List<Long> deletedAlbumIds = intent.getParcelableArrayListExtra(
                        com.magicalstory.music.utils.file.FileDeleteUtils.EXTRA_DELETED_ALBUM_IDS, Long.class);
                if (deletedAlbumIds != null && !deletedAlbumIds.isEmpty()) {
                    handleAlbumsDeleted(deletedAlbumIds);
                }
            } else if (com.magicalstory.music.utils.file.FileDeleteUtils.ACTION_DELETE_ARTISTS.equals(intent.getAction())) {
                // 处理删除艺术家事件
                List<Long> deletedArtistIds = intent.getParcelableArrayListExtra(
                        com.magicalstory.music.utils.file.FileDeleteUtils.EXTRA_DELETED_ARTIST_IDS, Long.class);
                if (deletedArtistIds != null && !deletedArtistIds.isEmpty()) {
                    handleArtistsDeleted(deletedArtistIds);
                }
            }
        }
    };

    // 服务连接
    private final ServiceConnection serviceConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            MusicScanService.MusicScanBinder binder = (MusicScanService.MusicScanBinder) service;
            musicScanService = binder.getService();
            serviceBound = true;
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            serviceBound = false;
        }
    };

    @Override
    protected FragmentHomeBinding getViewBinding(LayoutInflater inflater, ViewGroup container) {
        return FragmentHomeBinding.inflate(inflater, container, false);
    }

    @Override
    protected boolean usePersistentView() {
        return true;
    }

    @Override
    protected int getLayoutRes() {
        return R.layout.fragment_home;
    }

    @Override
    protected FragmentHomeBinding bindPersistentView(View view) {
        return FragmentHomeBinding.bind(view);
    }

    @Override
    protected void initViewForPersistentView() {
        super.initViewForPersistentView();
        // 初始化权限请求启动器
        initPermissionLauncher();
        // 初始化视图
        setupSearchView();
        // 初始化RecyclerView
        initRecyclerViews();
        // 初始化搜索相关
        initSearchComponents();
        // 先检查权限，但不立即查询数据库
        checkPermissionAndShowUI();
    }

    @Override
    protected void initDataForPersistentView() {
        super.initDataForPersistentView();
        // 初始化Handler和线程池
        mainHandler = new Handler(Looper.getMainLooper());
        executorService = Executors.newCachedThreadPool();

        // 初始化预设的gradient drawable列表 - 改为空列表，等待动态生成
        songsLatestAddedGradients = new ArrayList<>();
        songsLatestAddedColors = new ArrayList<>();

        // 绑定服务
        Intent serviceIntent = new Intent(getContext(), MusicScanService.class);
        context.bindService(serviceIntent, serviceConnection, Context.BIND_AUTO_CREATE);

        // 注册广播接收器
        registerScanCompleteReceiver();
        registerDeleteEventReceiver();

        // 现在线程池和Handler已经初始化，可以检查权限并更新UI
        checkMusicPermissionAndUpdateUI();
    }

    /**
     * 基于歌曲列表动态生成渐变和颜色
     */
    private void generateGradientsAndColorsFromSongs(List<Song> songs) {
        if (songs == null || songs.isEmpty()) {
            android.util.Log.d("HomeFragment", "歌曲列表为空，跳过颜色生成");
            return;
        }

        android.util.Log.d("HomeFragment", "开始为 " + songs.size() + " 首歌曲生成渐变和颜色");

        songsLatestAddedGradients.clear();
        songsLatestAddedColors.clear();

        // 为每首歌曲生成基于专辑封面的颜色
        for (int i = 0; i < songs.size(); i++) {
            Song song = songs.get(i);
            // 构建专辑封面URI
            String albumArtUri = null;
            if (song.getAlbumId() > 0) {
                albumArtUri = "content://media/external/audio/albumart/" + song.getAlbumId();
            }

            android.util.Log.d("HomeFragment", "处理歌曲[" + i + "]: " + song.getTitle() +
                    ", 专辑ID: " + song.getAlbumId() +
                    ", URI: " + albumArtUri);

            // 异步加载专辑封面并提取颜色，传入索引确保顺序一致
            loadAlbumArtAndExtractColor(albumArtUri, song, i);
        }
    }

    /**
     * 异步加载专辑封面并提取颜色
     */
    private void loadAlbumArtAndExtractColor(String albumArtUri, Song song, int index) {

        try {
            // 使用Glide加载专辑封面
            android.graphics.Bitmap bitmap = null;
            if (albumArtUri != null) {
                try {
                    bitmap = com.bumptech.glide.Glide.with(getContext())
                            .asBitmap()
                            .load(albumArtUri)
                            .submit()
                            .get();

                    // 打印原始数据到控制台
                    android.util.Log.d("HomeFragment", "成功加载专辑封面[" + index + "]: " + song.getTitle() +
                            ", URI: " + albumArtUri +
                            ", Bitmap: " + (bitmap != null ? bitmap.getWidth() + "x" + bitmap.getHeight() : "null"));
                } catch (Exception e) {
                    android.util.Log.e("HomeFragment", "加载专辑封面失败[" + index + "]: " + e.getMessage(), e);
                }
            } else {
                android.util.Log.d("HomeFragment", "歌曲[" + index + "] " + song.getTitle() + " 没有专辑封面URI");
            }

            // 提取主色调
            int dominantColor = ColorExtractor.extractDominantColor(bitmap);

            // 打印提取的颜色信息到控制台
            android.util.Log.d("HomeFragment", "歌曲[" + index + "] " + song.getTitle() +
                    " 提取的主色调: #" + String.format("%06X", (0xFFFFFF & dominantColor)) +
                    ", 颜色值: " + dominantColor);

            // 创建渐变drawable
            GradientDrawable gradientDrawable = new GradientDrawable(
                    GradientDrawable.Orientation.LEFT_RIGHT,
                    new int[]{dominantColor, android.graphics.Color.TRANSPARENT}
            );

            // 在主线程中更新UI，确保顺序一致
            if (mainHandler != null) {
                mainHandler.post(() -> {
                    // 确保列表大小足够，如果不够则填充到指定索引
                    while (songsLatestAddedGradients.size() <= index) {
                        songsLatestAddedGradients.add(null);
                    }
                    while (songsLatestAddedColors.size() <= index) {
                        songsLatestAddedColors.add(null);
                    }

                    // 在指定索引位置设置渐变和颜色
                    songsLatestAddedGradients.set(index, gradientDrawable);
                    songsLatestAddedColors.set(index, dominantColor);

                    android.util.Log.d("HomeFragment", "已设置渐变和颜色[" + index + "]，当前完成数量: " +
                            getCompletedCount() + "/" + songsLatestAddedAdapter.getItemCount());

                    // 如果所有歌曲的颜色都已提取完成，更新适配器
                    if (getCompletedCount() == songsLatestAddedAdapter.getItemCount()) {
                        android.util.Log.d("HomeFragment", "所有歌曲颜色提取完成，更新适配器");
                        songsLatestAddedAdapter.updateData(
                                songsLatestAddedAdapter.getSongList(),
                                songsLatestAddedGradients,
                                songsLatestAddedColors
                        );
                    }
                });
            }
        } catch (Exception e) {
            android.util.Log.e("HomeFragment", "提取颜色失败[" + index + "]: " + e.getMessage(), e);

            // 如果提取失败，使用默认颜色
            if (mainHandler != null) {
                mainHandler.post(() -> {
                    int defaultColor = android.graphics.Color.parseColor("#3353BE");
                    android.util.Log.d("HomeFragment", "使用默认颜色[" + index + "]: #3353BE");

                    GradientDrawable defaultGradient = new GradientDrawable(
                            GradientDrawable.Orientation.LEFT_RIGHT,
                            new int[]{defaultColor, android.graphics.Color.TRANSPARENT}
                    );

                    // 确保列表大小足够，如果不够则填充到指定索引
                    while (songsLatestAddedGradients.size() <= index) {
                        songsLatestAddedGradients.add(null);
                    }
                    while (songsLatestAddedColors.size() <= index) {
                        songsLatestAddedColors.add(null);
                    }

                    // 在指定索引位置设置默认渐变和颜色
                    songsLatestAddedGradients.set(index, defaultGradient);
                    songsLatestAddedColors.set(index, defaultColor);

                    // 如果所有歌曲的颜色都已提取完成，更新适配器
                    if (getCompletedCount() == songsLatestAddedAdapter.getItemCount()) {
                        android.util.Log.d("HomeFragment", "所有歌曲颜色提取完成（包含默认颜色），更新适配器");
                        songsLatestAddedAdapter.updateData(
                                songsLatestAddedAdapter.getSongList(),
                                songsLatestAddedGradients,
                                songsLatestAddedColors
                        );
                    }
                });
            }
        }

    }

    /**
     * 获取已完成的颜色提取数量
     */
    private int getCompletedCount() {
        int count = 0;
        for (int i = 0; i < songsLatestAddedGradients.size(); i++) {
            if (songsLatestAddedGradients.get(i) != null && songsLatestAddedColors.get(i) != null) {
                count++;
            }
        }
        return count;
    }

    @Override
    protected void initListenerForPersistentView() {
        super.initListenerForPersistentView();


        // 扫描按钮点击事件
        binding.buttonScan.setOnClickListener(v -> {
            requestMusicPermissionAndScan();
        });


        // 最近添加的歌曲标题点击事件
        binding.itemHeaderSongsLastestAdded.setOnClickListener(v -> {
            // 跳转到最近添加的歌曲页面
            navigateToRecentSongs();
        });

        // 最近播放专辑标题点击事件
        binding.itemHeaderRecentAlbums.setOnClickListener(v -> {
            // 跳转到专辑页面
            navigateToAlbums();
        });

        // 最近听过的艺术家标题点击事件
        binding.itemHeaderRecentArtists.setOnClickListener(v -> {
            // 跳转到艺术家页面
            navigateToArtists();
        });

        // 我的收藏标题点击事件
        binding.itemHeaderMyFavorites.setOnClickListener(v -> {
            // 跳转到我的收藏页面
            navigateToMyFavorites();
        });

        // 随机推荐标题点击事件
        binding.itemHeaderRandomRecommendations.setOnClickListener(v -> {
            // 跳转到随机推荐页面
            navigateToRandomRecommendations();
        });

        // 播放历史点击事件
        binding.layoutPlayHistory.setOnClickListener(v -> {
            // 跳转到播放历史页面
            navigateToPlayHistory();
        });

        // 我的收藏点击事件
        binding.layoutMyFavoritesTop.setOnClickListener(v -> {
            // 跳转到我的收藏页面
            navigateToMyFavorites();
        });

        // 最常播放点击事件
        binding.layoutMostPlayed.setOnClickListener(v -> {
            // 跳转到最常播放页面
            navigateToMostPlayed();
        });

        // 随机播放点击事件
        binding.layoutRandomPlay.setOnClickListener(v -> {
            // 开始随机播放
            startRandomPlay();
        });

        if (hasMusicPermission()) {
            //MusicSyncUtils.syncMusicFiles(context);
        }
    }

    /**
     * 初始化搜索相关组件
     */
    private void initSearchComponents() {
        // 初始化搜索结果RecyclerView
        binding.searchLayout.recyclerviewResult.setLayoutManager(
                new LinearLayoutManager(getContext(), LinearLayoutManager.VERTICAL, false));
        searchResultAdapter = new SearchResultAdapter(getContext());

        // 设置搜索结果点击事件
        searchResultAdapter.setOnSongClickListener((song, position) -> {
            // 播放选中的歌曲
            if (getActivity() instanceof MainActivity mainActivity) {
                List<Song> allSongs = searchResultAdapter.getAllSongs();
                mainActivity.playFromPlaylist(allSongs, position);
            }
        });

        searchResultAdapter.setOnAlbumClickListener((album, position) -> {
            // 跳转到专辑详情页面
            Bundle bundle = new Bundle();
            bundle.putLong("album_id", album.getAlbumId());
            bundle.putString("artist_name", album.getArtist());
            bundle.putString("album_name", album.getAlbumName());
            Navigation.findNavController(requireView()).navigate(R.id.action_home_to_album_detail, bundle);
        });

        searchResultAdapter.setOnArtistClickListener((artist, position) -> {
            // 跳转到艺术家详情页面
            Bundle bundle = new Bundle();
            bundle.putLong("artist_id", artist.getArtistId());
            bundle.putString("artist_name", artist.getArtistName());
            Navigation.findNavController(requireView()).navigate(R.id.action_home_to_artist_detail, bundle);
        });

        binding.searchLayout.recyclerviewResult.setAdapter(searchResultAdapter);

        // 初始化搜索界面状态 - 默认显示placeholder，隐藏其他
        binding.searchLayout.layoutEmpty.setVisibility(View.GONE);
        binding.searchLayout.placeholder.setVisibility(View.VISIBLE);

        // 设置搜索监听器
        binding.openSearchView.addTransitionListener((searchView, previousState, newState) -> {
            if (newState == SearchView.TransitionState.SHOWING) {
                binding.searchLayout.getRoot().setVisibility(View.VISIBLE);
                // SearchView展开时隐藏BottomNavigationView
                hideBottomNavigation();
            } else if (newState == SearchView.TransitionState.HIDING) {
                // SearchView收起时显示BottomNavigationView
                showBottomNavigation();
            }
        });

        // 设置搜索文本变化监听器
        binding.openSearchView.getEditText().addTextChangedListener(new android.text.TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
            }

            @Override
            public void afterTextChanged(android.text.Editable s) {
                currentSearchQuery = s.toString().trim();
                android.util.Log.d("HomeFragment", "搜索文本变化: " + currentSearchQuery);
                if (!TextUtils.isEmpty(currentSearchQuery)) {
                    performSearch(currentSearchQuery, currentSearchType);
                } else {
                    // 清空搜索结果，显示placeholder
                    searchResultAdapter.updateSearchResults(new ArrayList<>(), new ArrayList<>(), new ArrayList<>());
                    binding.searchLayout.layoutEmpty.setVisibility(View.GONE);
                    binding.searchLayout.placeholder.setVisibility(View.VISIBLE);
                }
            }
        });

        // 设置Chip点击监听器
        binding.searchLayout.chipGroup.setOnCheckedStateChangeListener((group, checkedIds) -> {
            if (!checkedIds.isEmpty()) {
                Chip selectedChip = group.findViewById(checkedIds.get(0));
                if (selectedChip != null) {
                    String chipText = selectedChip.getText().toString();
                    updateSearchType(chipText);
                }
            }
        });
    }

    /**
     * 更新搜索类型
     */
    private void updateSearchType(String chipText) {
        String newSearchType;
        switch (chipText) {
            case "全部":
                newSearchType = "all";
                break;
            case "歌曲":
                newSearchType = "songs";
                break;
            case "专辑":
                newSearchType = "album";
                break;
            case "艺术家":
                newSearchType = "artist";
                break;
            case "播放列表":
                newSearchType = "playlist";
                break;
            default:
                newSearchType = "all";
                break;
        }

        android.util.Log.d("HomeFragment", "搜索类型切换: " + chipText + " -> " + newSearchType);

        if (!currentSearchType.equals(newSearchType)) {
            currentSearchType = newSearchType;
            if (!TextUtils.isEmpty(currentSearchQuery)) {
                performSearch(currentSearchQuery, currentSearchType);
            }
        }
    }

    /**
     * 执行搜索
     */
    private void performSearch(String query, String searchType) {
        android.util.Log.d("HomeFragment", "开始搜索: " + query + ", 类型: " + searchType);

        if (executorService == null) {
            executorService = Executors.newCachedThreadPool();
        }

        executorService.execute(() -> {
            try {
                List<Song> songs = new ArrayList<>();
                List<Album> albums = new ArrayList<>();
                List<Artist> artists = new ArrayList<>();

                switch (searchType) {
                    case "all":
                        // 搜索全部类型
                        songs = searchAllSongs(query);
                        albums = searchAllAlbums(query);
                        artists = searchAllArtists(query);
                        break;
                    case "songs":
                        songs = searchSongs(query);
                        break;
                    case "album":
                        albums = searchAlbums(query);
                        break;
                    case "artist":
                        artists = searchArtists(query);
                        break;
                    case "playlist":
                        songs = searchByPlaylist(query);
                        break;
                }

                // 创建final变量用于lambda表达式
                final List<Song> finalSongs = songs;
                final List<Album> finalAlbums = albums;
                final List<Artist> finalArtists = artists;

                // 在主线程中更新UI
                if (mainHandler != null) {
                    mainHandler.post(() -> {
                        if (searchType.equals("all")) {
                            // 全部搜索：显示所有类型结果
                            searchResultAdapter.updateSearchResults(finalSongs, finalAlbums, finalArtists);
                        } else {
                            // 单一类型搜索
                            switch (searchType) {
                                case "songs":
                                    searchResultAdapter.updateSingleTypeResults("songs", finalSongs);
                                    break;
                                case "album":
                                    searchResultAdapter.updateSingleTypeResults("album", finalAlbums);
                                    break;
                                case "artist":
                                    searchResultAdapter.updateSingleTypeResults("artist", finalArtists);
                                    break;
                                case "playlist":
                                    searchResultAdapter.updateSingleTypeResults("songs", finalSongs);
                                    break;
                            }
                        }

                        // 检查是否有搜索结果
                        boolean hasResults = !finalSongs.isEmpty() || !finalAlbums.isEmpty() || !finalArtists.isEmpty();
                        if (hasResults) {
                            binding.searchLayout.layoutEmpty.setVisibility(View.GONE);
                            binding.searchLayout.placeholder.setVisibility(View.GONE);
                        } else {
                            binding.searchLayout.layoutEmpty.setVisibility(View.VISIBLE);
                            binding.searchLayout.placeholder.setVisibility(View.GONE);
                        }
                    });
                }
            } catch (Exception e) {
                android.util.Log.e("HomeFragment", "搜索失败: " + e.getMessage(), e);
                if (mainHandler != null) {
                    mainHandler.post(() -> {
                        searchResultAdapter.updateSearchResults(new ArrayList<>(), new ArrayList<>(), new ArrayList<>());
                        binding.searchLayout.layoutEmpty.setVisibility(View.VISIBLE);
                        binding.searchLayout.placeholder.setVisibility(View.GONE);
                    });
                }
            }
        });
    }

    /**
     * 搜索全部歌曲（包括标题、艺术家、专辑匹配）
     */
    private List<Song> searchAllSongs(String query) {
        List<Song> results = new ArrayList<>();

        // 搜索歌曲标题
        List<Song> titleResults = LitePal.where("title like ?", "%" + query + "%").find(Song.class);
        results.addAll(titleResults);

        // 搜索艺术家
        List<Song> artistResults = LitePal.where("artist like ?", "%" + query + "%").find(Song.class);
        for (Song song : artistResults) {
            if (!results.contains(song)) {
                results.add(song);
            }
        }

        // 搜索专辑
        List<Song> albumResults = LitePal.where("album like ?", "%" + query + "%").find(Song.class);
        for (Song song : albumResults) {
            if (!results.contains(song)) {
                results.add(song);
            }
        }

        return results;
    }

    /**
     * 搜索全部专辑
     */
    private List<Album> searchAllAlbums(String query) {
        return LitePal.where("albumName like ?", "%" + query + "%").find(Album.class);
    }

    /**
     * 搜索全部艺术家
     */
    private List<Artist> searchAllArtists(String query) {
        return LitePal.where("artistName like ?", "%" + query + "%").find(Artist.class);
    }

    /**
     * 搜索歌曲（包括标题、艺术家、专辑匹配）
     */
    private List<Song> searchSongs(String query) {
        List<Song> results = new ArrayList<>();

        // 搜索歌曲标题
        List<Song> titleResults = LitePal.where("title like ?", "%" + query + "%").find(Song.class);
        results.addAll(titleResults);

        // 搜索艺术家
        List<Song> artistResults = LitePal.where("artist like ?", "%" + query + "%").find(Song.class);
        for (Song song : artistResults) {
            if (!results.contains(song)) {
                results.add(song);
            }
        }

        // 搜索专辑
        List<Song> albumResults = LitePal.where("album like ?", "%" + query + "%").find(Song.class);
        for (Song song : albumResults) {
            if (!results.contains(song)) {
                results.add(song);
            }
        }

        return results;
    }

    /**
     * 搜索专辑
     */
    private List<Album> searchAlbums(String query) {
        return LitePal.where("albumName like ?", "%" + query + "%").find(Album.class);
    }

    /**
     * 搜索艺术家
     */
    private List<Artist> searchArtists(String query) {
        return LitePal.where("artistName like ?", "%" + query + "%").find(Artist.class);
    }

    /**
     * 按播放列表搜索
     */
    private List<Song> searchByPlaylist(String query) {
        List<Song> results = new ArrayList<>();

        // 先查找匹配的播放列表
        List<Playlist> playlists = LitePal.where("name like ?", "%" + query + "%").find(Playlist.class);

        for (Playlist playlist : playlists) {
            // 查找播放列表中的歌曲
            List<com.magicalstory.music.model.PlaylistSong> playlistSongs =
                    LitePal.where("playlistId = ?", String.valueOf(playlist.getId())).find(com.magicalstory.music.model.PlaylistSong.class);

            for (com.magicalstory.music.model.PlaylistSong playlistSong : playlistSongs) {
                Song song = LitePal.find(Song.class, playlistSong.getSongId());
                if (song != null && !results.contains(song)) {
                    results.add(song);
                }
            }
        }

        return results;
    }

    private void setupSearchView() {
        binding.openSearchView.getEditText().setGravity(Gravity.CENTER_VERTICAL);
        binding.openSearchView.getEditText().setPadding(0, DensityUtil.dip2px(context, 6), 0, 0);
    }

    private void hideBottomNavigation() {
        if (getActivity() instanceof MainActivity) {
            MainActivity mainActivity = (MainActivity) getActivity();
            mainActivity.hideBottomNavigation();
        }
    }

    private void showBottomNavigation() {
        if (getActivity() instanceof MainActivity) {
            MainActivity mainActivity = (MainActivity) getActivity();
            mainActivity.showBottomNavigation();
        }
    }

    /**
     * 检查搜索框是否展开
     * @return true表示展开，false表示收起
     */
    public boolean isSearchViewExpanded() {
        return binding != null && binding.openSearchView != null &&
                binding.openSearchView.getCurrentTransitionState() == SearchView.TransitionState.SHOWN;
    }

    /**
     * 关闭搜索框
     * @return true表示成功关闭，false表示搜索框未展开
     */
    public boolean closeSearchView() {
        if (binding != null && binding.openSearchView != null && isSearchViewExpanded()) {
            binding.openSearchView.hide();
            return true;
        }
        return false;
    }

    /**
     * 初始化权限请求启动器
     */
    private void initPermissionLauncher() {
        permissionLauncher = registerForActivityResult(
                new ActivityResultContracts.RequestMultiplePermissions(),
                result -> {
                    boolean allGranted = true;
                    for (Boolean granted : result.values()) {
                        if (!granted) {
                            allGranted = false;
                            break;
                        }
                    }

                    if (allGranted) {
                        // 权限已授予，开始扫描
                        startMusicScan();
                    } else {
                        // 权限被拒绝
                        ToastUtils.showToast(getContext(), "需要存储权限才能扫描音乐文件");
                    }

                    // 权限授予后重新加载歌曲并显示布局
                    reloadMusicDataAndShowLayout();
                }
        );
    }

    /**
     * 检查权限并显示初始UI状态（不查询数据库）
     */
    private void checkPermissionAndShowUI() {
        boolean hasPermission = hasMusicPermission();

        if (!hasPermission) {
            // 没有权限，直接显示空布局
            binding.layoutEmpty.setVisibility(View.VISIBLE);
            hideAllMusicLists();
        } else {
            // 有权限，先显示loading状态，等待数据库查询
            binding.progressBar.setVisibility(View.VISIBLE);
            changeContentLayoutVisable(false);
            binding.layoutEmpty.setVisibility(View.GONE);
        }
    }

    /**
     * 检查音乐权限并更新UI
     */
    private void checkMusicPermissionAndUpdateUI() {
        boolean hasPermission = hasMusicPermission();

        if (!hasPermission) {
            // 没有权限，直接显示空布局
            binding.layoutEmpty.setVisibility(View.VISIBLE);
            hideAllMusicLists();
            notifyHideSplashScreen();
            return;
        }

        // 有权限，异步检查数据库中是否有音乐数据
        checkMusicDataAsync();
    }

    /**
     * 异步检查音乐数据并更新UI
     */
    private void checkMusicDataAsync() {
        // 在后台线程中检查数据库
        executorService.execute(() -> {
            try {
                // 检查数据库中是否有音乐数据
                boolean hasMusicData = LitePal.count("song") > 0;

                // 回到主线程更新UI
                mainHandler.post(() -> {
                    if (hasMusicData) {
                        // 有音乐数据，隐藏空布局，加载音乐数据
                        binding.layoutEmpty.setVisibility(View.GONE);
                        loadMusicData();
                    } else {
                        // 没有音乐数据，显示空布局，隐藏其他列表
                        binding.layoutEmpty.setVisibility(View.VISIBLE);
                        hideAllMusicLists();
                        // 隐藏loading状态
                        hideLoading();
                    }
                });
            } catch (Exception e) {
                android.util.Log.e("HomeFragment", "Error checking music data: " + e.getMessage(), e);
                // 发生错误时，回到主线程显示空布局
                mainHandler.post(() -> {
                    binding.layoutEmpty.setVisibility(View.VISIBLE);
                    hideAllMusicLists();
                    hideLoading();
                });
            }
        });
    }

    /**
     * 检查是否有音乐权限
     */
    private boolean hasMusicPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            // Android 13+ 使用 READ_MEDIA_AUDIO
            return ContextCompat.checkSelfPermission(getContext(),
                    Manifest.permission.READ_MEDIA_AUDIO) == PackageManager.PERMISSION_GRANTED;
        } else {
            // Android 12 及以下使用 READ_EXTERNAL_STORAGE
            return ContextCompat.checkSelfPermission(getContext(),
                    Manifest.permission.READ_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED;
        }
    }

    /**
     * 请求音乐权限并扫描
     */
    private void requestMusicPermissionAndScan() {
        if (hasMusicPermission()) {
            // 已有权限，直接开始扫描
            startMusicScan();
        } else {
            // 请求权限
            String[] permissions;
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                permissions = new String[]{Manifest.permission.READ_MEDIA_AUDIO};
            } else {
                permissions = new String[]{Manifest.permission.READ_EXTERNAL_STORAGE};
            }
            permissionLauncher.launch(permissions);
        }
    }

    /**
     * 开始音乐扫描
     */
    private void startMusicScan() {
        if (serviceBound && musicScanService != null) {
            if (!musicScanService.isScanning()) {
                musicScanService.startMusicScan();
            }
        } else {
            // 服务未绑定，直接启动服务
            Intent serviceIntent = new Intent(getContext(), MusicScanService.class);
            getContext().startService(serviceIntent);
        }
    }

    /**
     * 重新加载歌曲数据并显示布局
     */
    private void reloadMusicDataAndShowLayout() {
        // 显示loading状态
        binding.progressBar.setVisibility(View.VISIBLE);
        changeContentLayoutVisable(false);
        binding.layoutEmpty.setVisibility(View.GONE);

        // 在后台线程中重新加载数据
        if (executorService == null) {
            executorService = Executors.newCachedThreadPool();
        }

        executorService.execute(() -> {
            try {
                // 检查数据库中是否有音乐数据
                boolean hasMusicData = LitePal.count("song") > 0;

                // 回到主线程更新UI
                if (mainHandler != null) {
                    mainHandler.post(() -> {
                        if (hasMusicData) {
                            // 有音乐数据，隐藏空布局，加载音乐数据
                            binding.layoutEmpty.setVisibility(View.GONE);
                            loadMusicData();
                        } else {
                            // 没有音乐数据，显示空布局，隐藏其他列表
                            binding.layoutEmpty.setVisibility(View.VISIBLE);
                            hideAllMusicLists();
                            // 隐藏loading状态
                            hideLoading();
                        }
                    });
                }
            } catch (Exception e) {
                android.util.Log.e("HomeFragment", "Error reloading music data: " + e.getMessage(), e);
                // 发生错误时，回到主线程显示空布局
                if (mainHandler != null) {
                    mainHandler.post(() -> {
                        binding.layoutEmpty.setVisibility(View.VISIBLE);
                        hideAllMusicLists();
                        hideLoading();

                    });
                }
            }
        });
    }

    //隐藏加载
    private void hideLoading() {
        notifyHideSplashScreen();
        changeContentLayoutVisable(true);
    }

    //通知隐藏闪屏
    private void notifyHideSplashScreen() {
        binding.progressBar.setVisibility(View.GONE);
        if (getActivity() instanceof MainActivity mainActivity) {
            mainActivity.hideSplashScreen();
        }
    }

    /**
     * 隐藏所有音乐列表
     */
    private void hideAllMusicLists() {
        binding.layoutSongsLastestAdded.setVisibility(View.GONE);
        binding.layoutRecentAlbums.setVisibility(View.GONE);
        binding.layoutRecentArtists.setVisibility(View.GONE);
        binding.layoutMyFavorites.setVisibility(View.GONE);
        binding.layoutRandomRecommendations.setVisibility(View.GONE);
    }

    /**
     * 初始化RecyclerView
     */
    private void initRecyclerViews() {
        // 最近添加的歌曲 - 使用空的渐变和颜色列表，等待动态生成
        binding.rvSongsLastestAdded.setLayoutManager(
                new LinearLayoutManager(getContext(), LinearLayoutManager.HORIZONTAL, false));
        songsLatestAddedAdapter = new SongHorizontalAdapter(getContext(), new ArrayList<>(), new ArrayList<>(), new ArrayList<>());
        binding.rvSongsLastestAdded.setAdapter(songsLatestAddedAdapter);

        // 最近播放的专辑
        binding.rvRecentAlbums.setLayoutManager(
                new LinearLayoutManager(getContext(), LinearLayoutManager.HORIZONTAL, false));
        recentAlbumsAdapter = new AlbumHorizontalAdapter(getContext(), new ArrayList<>());
        recentAlbumsAdapter.setOnItemClickListener((album, position) -> {
            // 跳转到专辑详情页面
            Bundle bundle = new Bundle();
            bundle.putLong("album_id", album.getAlbumId());
            bundle.putString("artist_name", album.getArtist());
            bundle.putString("album_name", album.getAlbumName());
            Navigation.findNavController(requireView()).navigate(R.id.action_home_to_album_detail, bundle);
        });
        binding.rvRecentAlbums.setAdapter(recentAlbumsAdapter);

        // 最近听过的艺术家
        binding.rvRecentArtists.setLayoutManager(
                new LinearLayoutManager(getContext(), LinearLayoutManager.HORIZONTAL, false));
        recentArtistsAdapter = new ArtistHorizontalAdapter(getContext(), new ArrayList<>(), this);
        binding.rvRecentArtists.setAdapter(recentArtistsAdapter);

        // 我的收藏 - 使用方形布局，不传入颜色值数组
        binding.rvMyFavorites.setLayoutManager(
                new LinearLayoutManager(getContext(), LinearLayoutManager.HORIZONTAL, false));
        myFavoritesAdapter = new SongHorizontalAdapter(getContext(), new ArrayList<>(), true);
        // 为调试添加点击监听器
        myFavoritesAdapter.setOnItemClickListener((song, position) -> {
            System.out.println("我的收藏点击: " + song.getTitle());
        });
        binding.rvMyFavorites.setAdapter(myFavoritesAdapter);

        // 随机推荐 - 使用方形布局，不传入颜色值数组
        binding.rvRandomRecommendations.setLayoutManager(
                new LinearLayoutManager(getContext(), LinearLayoutManager.HORIZONTAL, false));
        randomRecommendationsAdapter = new SongHorizontalAdapter(getContext(), new ArrayList<>(), true);
        // 为调试添加点击监听器
        randomRecommendationsAdapter.setOnItemClickListener((song, position) -> {
            System.out.println("随机推荐点击: " + song.getTitle());
        });
        binding.rvRandomRecommendations.setAdapter(randomRecommendationsAdapter);


        //binding.rvMyFavorites.setLayoutFrozen(true);
    }

    /**
     * 加载音乐数据
     */
    private void loadMusicData() {
        // 在后台线程中进行数据库查询，避免ANR
        if (executorService == null) {
            executorService = Executors.newCachedThreadPool();
        }

        // 启动后台服务批量获取所有专辑和歌手的封面
        CoverFetchService.startFetchAllCovers(getContext());

        executorService.execute(() -> {
            try {
                // 加载最近添加的歌曲（按添加时间倒序，取前10首）
                List<Song> latestSongs = LitePal.order("dateAdded desc").limit(10).find(Song.class);

                // 加载最近播放专辑（按lastplayed倒序排列，取前10个）
                List<Album> recentAlbums = LitePal.order("lastplayed desc").limit(10).find(Album.class);

                // 为专辑设置回退封面
                if (recentAlbums != null && !recentAlbums.isEmpty()) {
                    int albumCoverCount = CoverFallbackUtils.setAlbumsFallbackCover(recentAlbums);
                    if (albumCoverCount > 0) {
                        android.util.Log.d("HomeFragment", "为 " + albumCoverCount + " 个专辑设置了回退封面");
                    }
                }

                // 加载最近听过的艺术家（按lastplayed倒序排列，取前10个）
                List<Artist> recentArtists = LitePal.order("lastplayed desc").limit(10).find(Artist.class);

                if (recentArtists != null && !recentArtists.isEmpty()) {
                    int artistCoverCount = CoverFallbackUtils.setArtistsFallbackCover(recentArtists);
                    if (artistCoverCount > 0) {
                        android.util.Log.d("HomeFragment", "为 " + artistCoverCount + " 个艺术家设置了回退封面");
                    }
                }

                // 加载我的收藏（从FavoriteSong表查询真正的收藏歌曲）
                List<FavoriteSong> favoriteSongList = LitePal.order("addTime desc").limit(10).find(FavoriteSong.class);
                List<Song> favoriteSongs = new ArrayList<>();
                if (favoriteSongList != null && !favoriteSongList.isEmpty()) {
                    for (FavoriteSong favoriteSong : favoriteSongList) {
                        // 根据songId查询对应的Song对象
                        Song song = LitePal.find(Song.class, favoriteSong.getSongId());
                        if (song != null) {
                            favoriteSongs.add(song);
                        }
                    }
                }

                // 加载随机推荐（随机获取10首歌曲）
                List<Song> randomSongs = LitePal.order("random()").limit(10).find(Song.class);

                // 在主线程中更新UI
                if (mainHandler != null) {
                    mainHandler.post(() -> {
                        try {
                            // 更新最近添加的歌曲
                            if (latestSongs != null && !latestSongs.isEmpty()) {
                                songsLatestAddedAdapter.updateData(latestSongs);
                                binding.layoutSongsLastestAdded.setVisibility(View.VISIBLE);

                                // 基于歌曲列表动态生成渐变和颜色
                                new Thread() {
                                    @Override
                                    public void run() {
                                        super.run();
                                        generateGradientsAndColorsFromSongs(latestSongs);
                                    }
                                }.start();
                            } else {
                                binding.layoutSongsLastestAdded.setVisibility(View.GONE);
                            }

                            // 更新最近播放专辑
                            if (recentAlbums != null && !recentAlbums.isEmpty()) {
                                recentAlbumsAdapter.updateData(recentAlbums);
                                binding.layoutRecentAlbums.setVisibility(View.VISIBLE);
                            } else {
                                binding.layoutRecentAlbums.setVisibility(View.GONE);
                            }

                            // 更新最近听过的艺术家
                            if (recentArtists != null && !recentArtists.isEmpty()) {
                                recentArtistsAdapter.updateData(recentArtists);
                                binding.layoutRecentArtists.setVisibility(View.VISIBLE);
                            } else {
                                binding.layoutRecentArtists.setVisibility(View.GONE);
                            }

                            // 更新我的收藏
                            if (!favoriteSongs.isEmpty()) {
                                System.out.println("加载收藏歌曲数量: " + favoriteSongs.size());
                                myFavoritesAdapter.updateData(favoriteSongs);
                                binding.layoutMyFavorites.setVisibility(View.VISIBLE);
                            } else {
                                binding.layoutMyFavorites.setVisibility(View.GONE);
                            }

                            // 更新随机推荐
                            if (randomSongs != null && !randomSongs.isEmpty()) {
                                randomRecommendationsAdapter.updateData(randomSongs);
                                binding.layoutRandomRecommendations.setVisibility(View.VISIBLE);
                            } else {
                                binding.layoutRandomRecommendations.setVisibility(View.GONE);
                            }


                            binding.openSearchView.postDelayed(() -> {
                                hideLoading();
                                changeContentLayoutVisable(true);
                            }, 0);

                        } catch (Exception e) {
                            android.util.Log.e("HomeFragment", "Error updating UI: " + e.getMessage(), e);
                        }
                    });
                }
            } catch (Exception e) {
                android.util.Log.e("HomeFragment", "Error loading music data: " + e.getMessage(), e);
            }
        });
    }

    private void changeContentLayoutVisable(boolean visiable) {
        if (!visiable) {
            binding.layoutRandomRecommendations.setVisibility(View.INVISIBLE);
            binding.layoutMyFavorites.setVisibility(View.INVISIBLE);
            binding.layoutRecentArtists.setVisibility(View.INVISIBLE);
            binding.layoutRecentAlbums.setVisibility(View.INVISIBLE);
            binding.layoutSongsLastestAdded.setVisibility(View.INVISIBLE);
        } else {
            if (recentAlbumsAdapter != null && recentAlbumsAdapter.getItemCount() != 0) {
                binding.layoutRecentAlbums.setVisibility(View.VISIBLE);
            }
            if (recentArtistsAdapter != null && recentArtistsAdapter.getItemCount() != 0) {
                binding.layoutRecentArtists.setVisibility(View.VISIBLE);
            }
            if (myFavoritesAdapter != null && myFavoritesAdapter.getItemCount() != 0) {
                binding.layoutMyFavorites.setVisibility(View.VISIBLE);
            }
            if (randomRecommendationsAdapter != null && randomRecommendationsAdapter.getItemCount() != 0) {
                binding.layoutRandomRecommendations.setVisibility(View.VISIBLE);
            }
            if (songsLatestAddedAdapter != null && songsLatestAddedAdapter.getItemCount() != 0) {
                binding.layoutSongsLastestAdded.setVisibility(View.VISIBLE);
            }

        }
    }

    /**
     * 跳转到最近添加的歌曲页面
     */
    private void navigateToRecentSongs() {
        // 使用Navigation组件进行跳转，动画已在nav_graph.xml中配置
        Navigation.findNavController(requireView()).navigate(R.id.action_home_to_recent_songs);
    }

    /**
     * 跳转到专辑页面
     */
    private void navigateToAlbums() {
        // 使用Navigation组件进行跳转，动画已在nav_graph.xml中配置
        Navigation.findNavController(requireView()).navigate(R.id.action_home_to_albums);
    }

    /**
     * 跳转到艺术家页面
     */
    private void navigateToArtists() {
        // 使用Navigation组件进行跳转，动画已在nav_graph.xml中配置
        Navigation.findNavController(requireView()).navigate(R.id.action_home_to_artists);
    }

    /**
     * 跳转到我的收藏页面
     */
    private void navigateToMyFavorites() {
        // 使用Navigation组件进行跳转，传递数据类型参数
        android.os.Bundle bundle = new android.os.Bundle();
        bundle.putString("dataType", "favorite");
        Navigation.findNavController(requireView()).navigate(R.id.action_home_to_recent_songs, bundle);
    }

    /**
     * 跳转到随机推荐页面
     */
    private void navigateToRandomRecommendations() {
        // 使用Navigation组件进行跳转，传递数据类型参数
        android.os.Bundle bundle = new android.os.Bundle();
        bundle.putString("dataType", "random");
        Navigation.findNavController(requireView()).navigate(R.id.action_home_to_recent_songs, bundle);
    }

    /**
     * 跳转到播放历史页面
     */
    private void navigateToPlayHistory() {
        // 使用Navigation组件进行跳转，传递数据类型参数
        android.os.Bundle bundle = new android.os.Bundle();
        bundle.putString("dataType", "history");
        Navigation.findNavController(requireView()).navigate(R.id.action_home_to_recent_songs, bundle);
    }

    /**
     * 跳转到最常播放页面
     */
    private void navigateToMostPlayed() {
        // 使用Navigation组件进行跳转，传递数据类型参数
        android.os.Bundle bundle = new android.os.Bundle();
        bundle.putString("dataType", "most_played");
        Navigation.findNavController(requireView()).navigate(R.id.action_home_to_recent_songs, bundle);
    }

    /**
     * 开始随机播放
     */
    private void startRandomPlay() {
        // 在后台线程中进行数据库查询
        if (executorService == null) {
            executorService = Executors.newCachedThreadPool();
        }

        executorService.execute(() -> {
            try {
                // 从数据库随机获取歌曲
                List<Song> randomSongs = LitePal.order("random()").find(Song.class);

                // 在主线程中处理播放
                if (mainHandler != null) {
                    mainHandler.post(() -> {
                        if (randomSongs != null && !randomSongs.isEmpty()) {
                            // 将随机歌曲列表设置为播放列表并播放第一首
                            if (getActivity() instanceof MainActivity mainActivity) {
                                mainActivity.playFromPlaylist(randomSongs, 0);
                            }
                        } else {
                            com.magicalstory.music.utils.app.ToastUtils.showToast(getContext(),
                                    "没有可播放的歌曲");
                        }
                    });
                }
            } catch (Exception e) {
                android.util.Log.e("HomeFragment", "Error starting random play: " + e.getMessage(), e);
                // 在主线程中显示错误信息
                if (mainHandler != null) {
                    mainHandler.post(() -> {
                        com.magicalstory.music.utils.app.ToastUtils.showToast(getContext(),
                                "随机播放失败");
                    });
                }
            }
        });
    }

    @Override
    public void onResume() {
        super.onResume();
        showBottomNavigation();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();

        // 注销广播接收器
        try {
            if (getContext() != null) {
                LocalBroadcastManager.getInstance(getContext()).unregisterReceiver(scanCompleteReceiver);
                LocalBroadcastManager.getInstance(getContext()).unregisterReceiver(deleteEventReceiver);
                android.util.Log.d("HomeFragment", "广播接收器注销成功");
            }
        } catch (Exception e) {
            android.util.Log.e("HomeFragment", "注销广播接收器失败: " + e.getMessage(), e);
        }

        // 解绑服务
        if (serviceBound) {
            getContext().unbindService(serviceConnection);
            serviceBound = false;
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

        // 清理渐变和颜色列表
        if (songsLatestAddedGradients != null) {
            songsLatestAddedGradients.clear();
            songsLatestAddedGradients = null;
        }
        if (songsLatestAddedColors != null) {
            songsLatestAddedColors.clear();
            songsLatestAddedColors = null;
        }

        // 释放适配器资源
        if (songsLatestAddedAdapter != null) {
            songsLatestAddedAdapter.release();
            songsLatestAddedAdapter = null;
        }
        if (myFavoritesAdapter != null) {
            myFavoritesAdapter.release();
            myFavoritesAdapter = null;
        }
        if (randomRecommendationsAdapter != null) {
            randomRecommendationsAdapter.release();
            randomRecommendationsAdapter = null;
        }

        // 清理其他适配器
        recentAlbumsAdapter = null;
        recentArtistsAdapter = null;
    }

    @Override
    public boolean autoHideBottomNavigation() {
        return false;
    }

    /**
     * 注册扫描完成广播接收器
     */
    private void registerScanCompleteReceiver() {
        try {
            IntentFilter filter = new IntentFilter(MusicScanService.ACTION_SCAN_COMPLETE);
            LocalBroadcastManager.getInstance(getContext()).registerReceiver(scanCompleteReceiver, filter);
            android.util.Log.d("HomeFragment", "扫描完成广播接收器注册成功");
        } catch (Exception e) {
            android.util.Log.e("HomeFragment", "注册扫描完成广播接收器失败: " + e.getMessage(), e);
        }
    }

    /**
     * 注册删除事件广播接收器
     */
    private void registerDeleteEventReceiver() {
        try {
            IntentFilter filter = new IntentFilter();
            filter.addAction(com.magicalstory.music.utils.file.FileDeleteUtils.ACTION_DELETE_SONGS);
            filter.addAction(com.magicalstory.music.utils.file.FileDeleteUtils.ACTION_DELETE_ALBUMS);
            filter.addAction(com.magicalstory.music.utils.file.FileDeleteUtils.ACTION_DELETE_ARTISTS);
            LocalBroadcastManager.getInstance(getContext()).registerReceiver(deleteEventReceiver, filter);
            android.util.Log.d("HomeFragment", "删除事件广播接收器注册成功");
        } catch (Exception e) {
            android.util.Log.e("HomeFragment", "注册删除事件广播接收器失败: " + e.getMessage(), e);
        }
    }

    /**
     * 处理歌曲删除事件
     * @param deletedSongIds 已删除的歌曲ID列表
     */
    private void handleSongsDeleted(List<Long> deletedSongIds) {
        android.util.Log.d("HomeFragment", "处理歌曲删除事件，删除歌曲数量: " + deletedSongIds.size());

        // 从各个适配器中移除被删除的歌曲
        if (songsLatestAddedAdapter != null) {
            songsLatestAddedAdapter.removeSongsByIds(deletedSongIds);
            updateGroupVisibility(songsLatestAddedAdapter, binding.layoutSongsLastestAdded);
        }

        if (myFavoritesAdapter != null) {
            myFavoritesAdapter.removeSongsByIds(deletedSongIds);
            updateGroupVisibility(myFavoritesAdapter, binding.layoutMyFavorites);
        }

        if (randomRecommendationsAdapter != null) {
            randomRecommendationsAdapter.removeSongsByIds(deletedSongIds);
            updateGroupVisibility(randomRecommendationsAdapter, binding.layoutRandomRecommendations);
        }

        // 检查是否需要显示空布局
        checkAndShowEmptyLayout();
    }

    /**
     * 处理专辑删除事件
     * @param deletedAlbumIds 已删除的专辑ID列表
     */
    private void handleAlbumsDeleted(List<Long> deletedAlbumIds) {
        android.util.Log.d("HomeFragment", "处理专辑删除事件，删除专辑数量: " + deletedAlbumIds.size());

        // 从专辑适配器中移除被删除的专辑
        if (recentAlbumsAdapter != null) {
            recentAlbumsAdapter.removeAlbumsByIds(deletedAlbumIds);
            updateGroupVisibility(recentAlbumsAdapter, binding.layoutRecentAlbums);
        }

        // 检查是否需要显示空布局
        checkAndShowEmptyLayout();
    }

    /**
     * 处理艺术家删除事件
     * @param deletedArtistIds 已删除的艺术家ID列表
     */
    private void handleArtistsDeleted(List<Long> deletedArtistIds) {
        android.util.Log.d("HomeFragment", "处理艺术家删除事件，删除艺术家数量: " + deletedArtistIds.size());

        // 从艺术家适配器中移除被删除的艺术家
        if (recentArtistsAdapter != null) {
            recentArtistsAdapter.removeArtistsByIds(deletedArtistIds);
            updateGroupVisibility(recentArtistsAdapter, binding.layoutRecentArtists);
        }

        // 检查是否需要显示空布局
        checkAndShowEmptyLayout();
    }

    /**
     * 更新组可见性
     * @param adapter 适配器
     * @param groupLayout 组布局
     */
    private void updateGroupVisibility(androidx.recyclerview.widget.RecyclerView.Adapter adapter, View groupLayout) {
        if (adapter.getItemCount() == 0) {
            groupLayout.setVisibility(View.GONE);
            android.util.Log.d("HomeFragment", "隐藏空组: " + groupLayout.getId());
        } else {
            groupLayout.setVisibility(View.VISIBLE);
        }
    }

    /**
     * 检查并显示空布局
     */
    private void checkAndShowEmptyLayout() {
        // 检查所有组是否都为空
        boolean allGroupsEmpty = true;

        if (songsLatestAddedAdapter != null && songsLatestAddedAdapter.getItemCount() > 0) {
            allGroupsEmpty = false;
        }
        if (recentAlbumsAdapter != null && recentAlbumsAdapter.getItemCount() > 0) {
            allGroupsEmpty = false;
        }
        if (recentArtistsAdapter != null && recentArtistsAdapter.getItemCount() > 0) {
            allGroupsEmpty = false;
        }
        if (myFavoritesAdapter != null && myFavoritesAdapter.getItemCount() > 0) {
            allGroupsEmpty = false;
        }
        if (randomRecommendationsAdapter != null && randomRecommendationsAdapter.getItemCount() > 0) {
            allGroupsEmpty = false;
        }

        if (allGroupsEmpty) {
            // 显示空布局
            binding.layoutEmpty.setVisibility(View.VISIBLE);
            android.util.Log.d("HomeFragment", "所有组都为空，显示空布局");
        } else {
            // 隐藏空布局
            binding.layoutEmpty.setVisibility(View.GONE);
        }
    }

    /**
     * 刷新音乐列表
     */
    @Override
    protected void onRefreshMusicList() {
        // 重新加载音乐数据
        refreshFragmentAsync();
    }

    /**
     * 在后台线程执行刷新操作
     */
    @Override
    protected void performRefreshInBackground() {
        try {
            // 重新加载音乐数据
            // 这里会重新查询数据库获取最新的歌曲和专辑信息
            loadMusicData();

            // 打印原始数据到控制台
            System.out.println("HomeFragment后台刷新完成 - 已重新查询数据库获取最新数据");

        } catch (Exception e) {
            e.printStackTrace();
            System.out.println("HomeFragment后台刷新失败: " + e.getMessage());
        }
    }

    /**
     * 在主线程更新UI
     */
    @Override
    protected void updateUIAfterRefresh() {
        try {
            // 更新UI显示
            if (binding != null) {
                // 重新加载数据并更新各个适配器
                // 这里不需要手动调用notifyDataSetChanged，因为loadMusicData()会重新设置数据
                System.out.println("HomeFragment UI更新完成 - 已刷新所有适配器数据");
            }

        } catch (Exception e) {
            e.printStackTrace();
            System.out.println("HomeFragment UI更新失败: " + e.getMessage());
        }
    }

}
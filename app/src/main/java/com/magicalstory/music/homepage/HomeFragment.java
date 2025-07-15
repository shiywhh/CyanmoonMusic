package com.magicalstory.music.homepage;

import android.Manifest;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.os.IBinder;
import android.text.TextUtils;
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
import androidx.navigation.Navigation;

import com.magicalstory.music.R;
import com.google.android.material.search.SearchView;
import com.magicalstory.music.MainActivity;
import com.magicalstory.music.base.BaseFragment;
import com.magicalstory.music.databinding.FragmentHomeBinding;
import com.magicalstory.music.service.MusicScanService;
import com.magicalstory.music.utils.app.ToastUtils;
import com.magicalstory.music.adapter.SongHorizontalAdapter;
import com.magicalstory.music.adapter.AlbumHorizontalAdapter;
import com.magicalstory.music.adapter.ArtistHorizontalAdapter;
import com.magicalstory.music.model.Song;
import com.magicalstory.music.model.Album;
import com.magicalstory.music.model.Artist;
import com.magicalstory.music.utils.network.NetUtils;
import com.google.gson.JsonObject;
import com.google.gson.JsonArray;
import com.google.gson.JsonParser;

import androidx.recyclerview.widget.LinearLayoutManager;

import org.litepal.LitePal;

import java.util.List;
import java.util.ArrayList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import okhttp3.Response;

import com.magicalstory.music.model.FavoriteSong;
import com.magicalstory.music.utils.glide.CoverFallbackUtils;
import com.magicalstory.music.service.CoverFetchService;

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


    // 网络请求相关
    private ExecutorService executorService;
    private Handler mainHandler;

    // 扫描完成广播接收器
    private final BroadcastReceiver scanCompleteReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (MusicScanService.ACTION_SCAN_COMPLETE.equals(intent.getAction())) {
                int scanCount = intent.getIntExtra(MusicScanService.EXTRA_SCAN_COUNT, 0);
                ToastUtils.showToast(getContext(), "扫描完成，新增 " + scanCount + " 首歌曲");
                checkMusicPermissionAndUpdateUI();
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
    protected void initView() {
        super.initView();
        // 初始化权限请求启动器
        initPermissionLauncher();
        // 初始化视图
        setupSearchView();
        // 初始化RecyclerView
        initRecyclerViews();
        // 检查权限并更新UI
        checkMusicPermissionAndUpdateUI();
    }

    @Override
    protected void initData() {
        super.initData();
        
        // 初始化Handler和线程池
        mainHandler = new Handler(Looper.getMainLooper());
        executorService = Executors.newCachedThreadPool();
        
        // 注册广播接收器
        IntentFilter filter = new IntentFilter(MusicScanService.ACTION_SCAN_COMPLETE);
        ContextCompat.registerReceiver(context, scanCompleteReceiver, filter, ContextCompat.RECEIVER_NOT_EXPORTED);

        // 绑定服务
        Intent serviceIntent = new Intent(getContext(), MusicScanService.class);
        context.bindService(serviceIntent, serviceConnection, Context.BIND_AUTO_CREATE);
    }

    @Override
    protected void initListener() {
        super.initListener();
        // SearchBar和SearchView已通过layout_anchor自动关联，无需手动设置点击监听

        // 扫描按钮点击事件
        binding.buttonScan.setOnClickListener(v -> requestMusicPermissionAndScan());

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
    }

    private void setupSearchView() {
        // 设置SearchView的展开和收起监听
        binding.openSearchView.addTransitionListener((searchView, previousState, newState) -> {
            if (newState == SearchView.TransitionState.SHOWING) {
                // SearchView展开时隐藏BottomNavigationView
                hideBottomNavigation();
            } else if (newState == SearchView.TransitionState.HIDING) {
                // SearchView收起时显示BottomNavigationView
                showBottomNavigation();
            }
        });
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

                    // 更新UI
                    checkMusicPermissionAndUpdateUI();
                }
        );
    }

    /**
     * 检查音乐权限并更新UI
     */
    private void checkMusicPermissionAndUpdateUI() {
        boolean hasPermission = hasMusicPermission();
        boolean hasMusicData = false;

        if (hasPermission) {
            // 检查数据库中是否有音乐数据
            hasMusicData = LitePal.count("song") > 0;
        }

        // 根据权限和数据情况显示不同的UI
        if (!hasPermission || !hasMusicData) {
            // 显示空布局，隐藏其他列表
            binding.layoutEmpty.setVisibility(View.VISIBLE);
            hideAllMusicLists();
        } else {
            // 隐藏空布局，显示音乐列表
            binding.layoutEmpty.setVisibility(View.GONE);
            loadMusicData();
        }
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
                ToastUtils.showToast(getContext(), "开始扫描音乐文件...");
                musicScanService.startMusicScan();
            } else {
                ToastUtils.showToast(getContext(), "正在扫描中，请稍候...");
            }
        } else {
            // 服务未绑定，直接启动服务
            Intent serviceIntent = new Intent(getContext(), MusicScanService.class);
            getContext().startService(serviceIntent);
            ToastUtils.showToast(getContext(), "开始扫描音乐文件...");
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
        // 最近添加的歌曲
        binding.rvSongsLastestAdded.setLayoutManager(
                new LinearLayoutManager(getContext(), LinearLayoutManager.HORIZONTAL, false));
        songsLatestAddedAdapter = new SongHorizontalAdapter(getContext(), new ArrayList<>());
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
        recentArtistsAdapter = new ArtistHorizontalAdapter(getContext(), new ArrayList<>());
        binding.rvRecentArtists.setAdapter(recentArtistsAdapter);

        // 我的收藏 - 使用方形布局
        binding.rvMyFavorites.setLayoutManager(
                new LinearLayoutManager(getContext(), LinearLayoutManager.HORIZONTAL, false));
        myFavoritesAdapter = new SongHorizontalAdapter(getContext(), new ArrayList<>(), true);
        // 为调试添加点击监听器
        myFavoritesAdapter.setOnItemClickListener((song, position) -> {
            System.out.println("我的收藏点击: " + song.getTitle());
        });
        binding.rvMyFavorites.setAdapter(myFavoritesAdapter);

        // 随机推荐 - 使用方形布局
        binding.rvRandomRecommendations.setLayoutManager(
                new LinearLayoutManager(getContext(), LinearLayoutManager.HORIZONTAL, false));
        randomRecommendationsAdapter = new SongHorizontalAdapter(getContext(), new ArrayList<>(), true);
        // 为调试添加点击监听器
        randomRecommendationsAdapter.setOnItemClickListener((song, position) -> {
            System.out.println("随机推荐点击: " + song.getTitle());
        });
        binding.rvRandomRecommendations.setAdapter(randomRecommendationsAdapter);
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
                
                // 加载最近播放专辑（取前10个）
                List<Album> recentAlbums = LitePal.limit(10).find(Album.class);
                
                // 为专辑设置回退封面
                if (recentAlbums != null && !recentAlbums.isEmpty()) {
                    int albumCoverCount = CoverFallbackUtils.setAlbumsFallbackCover(recentAlbums);
                    if (albumCoverCount > 0) {
                        android.util.Log.d("HomeFragment", "为 " + albumCoverCount + " 个专辑设置了回退封面");
                    }
                }
                
                // 加载最近听过的艺术家（取前10个）
                List<Artist> recentArtists = LitePal.limit(10).find(Artist.class);
                
                // 为歌手设置回退封面
                if (recentArtists != null && !recentArtists.isEmpty()) {
                    int artistCoverCount = CoverFallbackUtils.setArtistsFallbackCover(recentArtists);
                    if (artistCoverCount > 0) {
                        android.util.Log.d("HomeFragment", "为 " + artistCoverCount + " 个歌手设置了回退封面");
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

                                // 获取艺术家封面
                                for (Artist artist : recentArtists) {
                                    fetchArtistCover(artist);
                                }
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
     * 获取艺术家封面
     */
    private void fetchArtistCover(Artist artist) {
        // 如果已经尝试过获取封面，则不再重复获取
        if (artist.isCoverFetched()) {
            return;
        }

        if (executorService == null) {
            executorService = Executors.newCachedThreadPool();
        }
        if (mainHandler == null) {
            mainHandler = new Handler(Looper.getMainLooper());
        }

        executorService.execute(() -> {
            try {
                String artistName = artist.getArtistName();
                String url = "https://music.163.com/api/search/get/web?s=" +
                        java.net.URLEncoder.encode(artistName, "UTF-8") + "&type=100";

                Response response = NetUtils.getInstance().getDataSynFromNet(url);
                if (response != null && response.isSuccessful()) {
                    String jsonResponse = response.body().string();
                    parseAndSaveArtistCover(artist, jsonResponse);
                } else {
                    // 获取失败，尝试使用回退封面
                    if (CoverFallbackUtils.setArtistFallbackCover(artist)) {
                        // 成功设置了回退封面，通知UI更新
                        mainHandler.post(() -> {
                            if (recentArtistsAdapter != null) {
                                recentArtistsAdapter.notifyDataSetChanged();
                            }
                        });
                    } else {
                        // 回退封面也没有，标记为已尝试过
                        artist.setCoverFetched(true);
                        artist.save();
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
                // 异常情况下尝试使用回退封面
                if (CoverFallbackUtils.setArtistFallbackCover(artist)) {
                    // 成功设置了回退封面，通知UI更新
                    mainHandler.post(() -> {
                        if (recentArtistsAdapter != null) {
                            recentArtistsAdapter.notifyDataSetChanged();
                        }
                    });
                } else {
                    // 回退封面也没有，标记为已尝试过
                    artist.setCoverFetched(true);
                    artist.save();
                }
            }
        });
    }

    /**
     * 解析并保存艺术家封面
     */
    private void parseAndSaveArtistCover(Artist artist, String jsonResponse) {
        try {
            JsonObject jsonObject = JsonParser.parseString(jsonResponse).getAsJsonObject();
            if (jsonObject.has("code") && jsonObject.get("code").getAsInt() == 200 && jsonObject.has("result")) {
                JsonObject result = jsonObject.getAsJsonObject("result");
                if (result.has("artists")) {
                    JsonArray artists = result.getAsJsonArray("artists");
                    if (artists.size() > 0) {
                        JsonObject artistInfo = artists.get(0).getAsJsonObject();
                        if (artistInfo.has("picUrl")) {
                            String picUrl = artistInfo.get("picUrl").getAsString();
                            if (picUrl != null && !picUrl.isEmpty()) {
                                // 更新数据库
                                artist.setCoverUrl(picUrl);
                                artist.setCoverFetched(true);
                                artist.save();

                                // 回到主线程更新UI
                                mainHandler.post(() -> {
                                    if (recentArtistsAdapter != null) {
                                        recentArtistsAdapter.notifyDataSetChanged();
                                    }
                                });
                                return;
                            }
                        }
                    }
                }
            }
            // 如果没有获取到封面，尝试使用回退封面
            if (CoverFallbackUtils.setArtistFallbackCover(artist)) {
                // 成功设置了回退封面，通知UI更新
                mainHandler.post(() -> {
                    if (recentArtistsAdapter != null) {
                        recentArtistsAdapter.notifyDataSetChanged();
                    }
                });
            } else {
                // 回退封面也没有，标记为已尝试过
                artist.setCoverFetched(true);
                artist.save();
            }
        } catch (Exception e) {
            e.printStackTrace();
            // 异常情况下尝试使用回退封面
            if (CoverFallbackUtils.setArtistFallbackCover(artist)) {
                // 成功设置了回退封面，通知UI更新
                mainHandler.post(() -> {
                    if (recentArtistsAdapter != null) {
                        recentArtistsAdapter.notifyDataSetChanged();
                    }
                });
            } else {
                // 回退封面也没有，标记为已尝试过
                artist.setCoverFetched(true);
                artist.save();
            }
        }
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
        if (scanCompleteReceiver != null) {
            getContext().unregisterReceiver(scanCompleteReceiver);
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
            executorService = null;
        }
    }


    @Override
    public boolean autoHideBottomNavigation() {
        return false;
    }
}
package com.magicalstory.music.homepage;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.TextUtils;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.media3.common.util.UnstableApi;
import androidx.navigation.Navigation;
import androidx.recyclerview.widget.GridLayoutManager;

import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipGroup;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.search.SearchView;
import com.magicalstory.music.R;
import com.magicalstory.music.adapter.PlaylistGridAdapter;
import com.magicalstory.music.adapter.SearchResultAdapter;
import com.magicalstory.music.base.BaseFragment;
import com.magicalstory.music.databinding.FragmentPlaylistBinding;
import com.magicalstory.music.dialog.dialogUtils;
import com.magicalstory.music.dialog.bottomMenuDialog.bottomDialogMenu;
import com.magicalstory.music.dialog.bottomMenuDialog.bottomMenusDialog;
import com.magicalstory.music.fragment.SongsListFragment;
import com.magicalstory.music.model.Album;
import com.magicalstory.music.model.Artist;
import com.magicalstory.music.model.Playlist;
import com.magicalstory.music.model.Song;
import com.magicalstory.music.utils.app.ToastUtils;
import com.magicalstory.music.utils.screen.DensityUtil;

import org.litepal.LitePal;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * 播放列表Fragment
 */
@UnstableApi
public class PlaylistFragment extends BaseFragment<FragmentPlaylistBinding> {

    // 适配器
    private PlaylistGridAdapter playlistAdapter;
    private SearchResultAdapter searchResultAdapter;

    // 搜索相关
    private String currentSearchQuery = "";
    private String currentSearchType = "all"; // all, songs, album, artist, playlist

    // 排序相关
    private String currentSortType = "name"; // name, songCount, createTime
    private String currentSortOrder = "asc"; // asc, desc

    // 网络请求相关
    private ExecutorService executorService;
    private Handler mainHandler;

    // 刷新广播接收器
    private final BroadcastReceiver refreshReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if ("com.magicalstory.music.REFRESH_PLAYLIST".equals(intent.getAction())) {
                android.util.Log.d("PlaylistFragment", "收到刷新歌单列表广播");
                // 重新加载歌单数据
                loadPlaylistData();
            }
        }
    };

    @Override
    protected FragmentPlaylistBinding getViewBinding(LayoutInflater inflater, ViewGroup container) {
        return FragmentPlaylistBinding.inflate(inflater, container, false);
    }

    @Override
    protected boolean usePersistentView() {
        return true;
    }

    @Override
    protected int getLayoutRes() {
        return R.layout.fragment_playlist;
    }

    @Override
    protected FragmentPlaylistBinding bindPersistentView(View view) {
        return FragmentPlaylistBinding.bind(view);
    }

    @Override
    protected void initViewForPersistentView() {
        super.initViewForPersistentView();
        // 初始化视图
        setupSearchView();
        // 初始化RecyclerView
        initRecyclerViews();
        // 初始化搜索相关
        initSearchComponents();
        // 初始化排序按钮
        initSortButton();
        // 初始化随机播放按钮
        initRandomPlayButton();
        // 加载歌单数据
        loadPlaylistData();
    }

    @Override
    protected void initDataForPersistentView() {
        super.initDataForPersistentView();
        // 初始化Handler和线程池
        mainHandler = new Handler(Looper.getMainLooper());
        executorService = Executors.newCachedThreadPool();
    }

    @Override
    protected void initListenerForPersistentView() {
        super.initListenerForPersistentView();

        // 添加歌单按钮点击事件
        binding.fabAddPlaylist.setOnClickListener(v -> {
            showCreatePlaylistDialog();
        });

        // 注册刷新广播接收器
        androidx.localbroadcastmanager.content.LocalBroadcastManager.getInstance(requireContext())
                .registerReceiver(refreshReceiver, new IntentFilter("com.magicalstory.music.REFRESH_PLAYLIST"));
    }

    /**
     * 初始化搜索相关组件
     */
    private void initSearchComponents() {
        // 初始化搜索结果RecyclerView
        binding.searchLayout.recyclerviewResult.setLayoutManager(
                new androidx.recyclerview.widget.LinearLayoutManager(getContext(), 
                        androidx.recyclerview.widget.LinearLayoutManager.VERTICAL, false));
        searchResultAdapter = new SearchResultAdapter(getContext());

        // 设置搜索结果点击事件
        searchResultAdapter.setOnSongClickListener((song, position) -> {
            // 播放选中的歌曲
            if (getActivity() instanceof com.magicalstory.music.MainActivity mainActivity) {
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
            Navigation.findNavController(requireView()).navigate(R.id.action_playlist_to_album_detail, bundle);
        });

        searchResultAdapter.setOnArtistClickListener((artist, position) -> {
            // 跳转到艺术家详情页面
            Bundle bundle = new Bundle();
            bundle.putLong("artist_id", artist.getArtistId());
            bundle.putString("artist_name", artist.getArtistName());
            Navigation.findNavController(requireView()).navigate(R.id.action_playlist_to_artist_detail, bundle);
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
                android.util.Log.d("PlaylistFragment", "搜索文本变化: " + currentSearchQuery);
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

        android.util.Log.d("PlaylistFragment", "搜索类型切换: " + chipText + " -> " + newSearchType);

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
        android.util.Log.d("PlaylistFragment", "开始搜索: " + query + ", 类型: " + searchType);

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
                android.util.Log.e("PlaylistFragment", "搜索失败: " + e.getMessage(), e);
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
        if (getActivity() instanceof com.magicalstory.music.MainActivity) {
            com.magicalstory.music.MainActivity mainActivity = (com.magicalstory.music.MainActivity) getActivity();
            mainActivity.hideBottomNavigation();
        }
    }

    private void showBottomNavigation() {
        if (getActivity() instanceof com.magicalstory.music.MainActivity) {
            com.magicalstory.music.MainActivity mainActivity = (com.magicalstory.music.MainActivity) getActivity();
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
     * 初始化RecyclerView
     */
    private void initRecyclerViews() {
        // 歌单列表 - 使用网格布局，2列
        binding.rvPlaylists.setLayoutManager(new GridLayoutManager(getContext(), 2));
        playlistAdapter = new PlaylistGridAdapter(getContext(), new ArrayList<>());
        
        // 设置歌单点击事件
        playlistAdapter.setOnItemClickListener((playlist, position) -> {
            // 跳转到歌单详情页面
            navigateToPlaylistSongs(playlist);
        });


        binding.rvPlaylists.setAdapter(playlistAdapter);
    }

    /**
     * 跳转到歌单歌曲列表
     */
    private void navigateToPlaylistSongs(Playlist playlist) {
        Bundle bundle = new Bundle();
        bundle.putLong("playlist_id", playlist.getId());
        bundle.putString("playlist_name", playlist.getName());
        bundle.putString("dataType", "playlist");
        Navigation.findNavController(requireView()).navigate(R.id.action_playlist_to_recent_songs, bundle);
    }

    /**
     * 加载歌单数据
     */
    private void loadPlaylistData() {
        // 显示进度条
        binding.progressBar.setVisibility(View.VISIBLE);
        binding.rvPlaylists.setVisibility(View.GONE);
        binding.layoutEmpty.setVisibility(View.GONE);

        // 在后台线程中查询数据
        if (executorService == null) {
            executorService = Executors.newCachedThreadPool();
        }

        executorService.execute(() -> {
            try {
                // 根据当前排序类型构建查询
                String orderBy = getOrderByClause();
                List<Playlist> playlists = LitePal.where("isSystemPlaylist = ?", "0")
                        .order(orderBy)
                        .find(Playlist.class);

                // 更新歌单的歌曲数量
                for (Playlist playlist : playlists) {
                    int songCount = com.magicalstory.music.model.PlaylistSong.getPlaylistSongCount(playlist.getId());
                    playlist.setSongCount(songCount);
                }

                // 如果按歌曲数量排序，需要重新排序
                if ("songCount".equals(currentSortType)) {
                    sortPlaylistsBySongCount(playlists);
                }

                // 在主线程中更新UI
                if (mainHandler != null) {
                    mainHandler.post(() -> {
                        binding.progressBar.setVisibility(View.GONE);

                        if (playlists != null && !playlists.isEmpty()) {
                            playlistAdapter.updateData(playlists);
                            binding.rvPlaylists.setVisibility(View.VISIBLE);
                            binding.layoutEmpty.setVisibility(View.GONE);
                        } else {
                            binding.rvPlaylists.setVisibility(View.GONE);
                            binding.layoutEmpty.setVisibility(View.VISIBLE);
                        }
                    });
                }
            } catch (Exception e) {
                android.util.Log.e("PlaylistFragment", "加载歌单数据失败: " + e.getMessage(), e);
                if (mainHandler != null) {
                    mainHandler.post(() -> {
                        binding.progressBar.setVisibility(View.GONE);
                        binding.rvPlaylists.setVisibility(View.GONE);
                        binding.layoutEmpty.setVisibility(View.VISIBLE);
                    });
                }
            }
        });
    }

    /**
     * 获取排序子句
     */
    private String getOrderByClause() {
        String orderBy;
        switch (currentSortType) {
            case "name":
                orderBy = "name " + currentSortOrder;
                break;
            case "createTime":
                orderBy = "createdTime " + currentSortOrder;
                break;
            case "songCount":
                // 歌曲数量排序需要特殊处理，先按默认排序
                orderBy = "updatedTime desc";
                break;
            default:
                orderBy = "name " + currentSortOrder;
                break;
        }
        return orderBy;
    }

    /**
     * 按歌曲数量排序歌单
     */
    private void sortPlaylistsBySongCount(List<Playlist> playlists) {
        if ("asc".equals(currentSortOrder)) {
            playlists.sort((p1, p2) -> Integer.compare(p1.getSongCount(), p2.getSongCount()));
        } else {
            playlists.sort((p1, p2) -> Integer.compare(p2.getSongCount(), p1.getSongCount()));
        }
    }

    /**
     * 显示创建歌单对话框
     */
    private void showCreatePlaylistDialog() {
        dialogUtils.getInstance().showInputDialog(
                getContext(),
                "创建歌单",
                "歌单名称",
                "",
                false,
                "",
                new dialogUtils.InputDialogListener() {
                    @Override
                    public void onInputProvided(String playlistName) {
                        if (!TextUtils.isEmpty(playlistName.trim())) {
                            createPlaylist(playlistName.trim());
                        } else {
                            ToastUtils.showToast(getContext(), "歌单名称不能为空");
                        }
                    }

                    @Override
                    public void onCancel() {
                        // 用户取消创建
                    }
                }
        );
    }

    /**
     * 创建歌单
     */
    private void createPlaylist(String playlistName) {
        // 检查歌单名称是否已存在
        List<Playlist> existingPlaylists = LitePal.where("name = ?", playlistName).find(Playlist.class);
        if (!existingPlaylists.isEmpty()) {
            ToastUtils.showToast(getContext(), "歌单名称已存在");
            return;
        }

        // 在后台线程中创建歌单
        if (executorService == null) {
            executorService = Executors.newCachedThreadPool();
        }

        executorService.execute(() -> {
            try {
                // 创建新歌单
                Playlist newPlaylist = new Playlist(playlistName, "");
                newPlaylist.setSystemPlaylist(false);
                boolean saved = newPlaylist.save();

                if (mainHandler != null) {
                    mainHandler.post(() -> {
                        if (saved) {
                            ToastUtils.showToast(getContext(), "歌单创建成功");
                            // 重新加载歌单数据
                            loadPlaylistData();
                        } else {
                            ToastUtils.showToast(getContext(), "歌单创建失败");
                        }
                    });
                }
            } catch (Exception e) {
                android.util.Log.e("PlaylistFragment", "创建歌单失败: " + e.getMessage(), e);
                if (mainHandler != null) {
                    mainHandler.post(() -> {
                        ToastUtils.showToast(getContext(), "创建歌单失败: " + e.getMessage());
                    });
                }
            }
        });
    }

    /**
     * 初始化排序按钮
     */
    private void initSortButton() {
        binding.buttonSort.setOnClickListener(v -> {
            showSortDialog();
        });
    }

    /**
     * 初始化随机播放按钮
     */
    private void initRandomPlayButton() {
        binding.buttonRandom.setOnClickListener(v -> {
            startRandomPlay();
        });
    }

    /**
     * 显示排序对话框
     */
    private void showSortDialog() {
        ArrayList<bottomDialogMenu> sortOptions = new ArrayList<>();
        
        // 添加排序选项
        sortOptions.add(new bottomDialogMenu(getString(R.string.sort_by_name), currentSortType.equals("name")));
        sortOptions.add(new bottomDialogMenu(getString(R.string.sort_by_song_count), currentSortType.equals("songCount")));
        sortOptions.add(new bottomDialogMenu(getString(R.string.sort_by_create_time), currentSortType.equals("createTime")));

        bottomMenusDialog sortDialog = new bottomMenusDialog(
                getContext(),
                sortOptions,
                getCurrentSortDisplayName(),
                getString(R.string.sort_by),
                new bottomMenusDialog.listener() {
                    @Override
                    public void onMenuClick(bottomDialogMenu menu) {
                        String selectedSort = menu.getTitle();
                        updateSortType(selectedSort);
                    }
                }
        );
        sortDialog.show();
    }

    /**
     * 获取当前排序方式的显示名称
     */
    private String getCurrentSortDisplayName() {
        switch (currentSortType) {
            case "name":
                return getString(R.string.sort_by_name);
            case "songCount":
                return getString(R.string.sort_by_song_count);
            case "createTime":
                return getString(R.string.sort_by_create_time);
            default:
                return getString(R.string.sort_by_name);
        }
    }

    /**
     * 更新排序类型
     */
    private void updateSortType(String sortType) {
        String newSortType;
        switch (sortType) {
            case "名称":
            case "Name":
                newSortType = "name";
                break;
            case "歌曲数量":
            case "Song Count":
                newSortType = "songCount";
                break;
            case "创建时间":
            case "Create Time":
                newSortType = "createTime";
                break;
            default:
                newSortType = "name";
                break;
        }

        if (!currentSortType.equals(newSortType)) {
            currentSortType = newSortType;
            // 更新排序按钮显示
            updateSortButtonDisplay();
            // 重新加载歌单数据
            loadPlaylistData();
        }
    }

    /**
     * 更新排序按钮显示
     */
    private void updateSortButtonDisplay() {
        String displayText;
        int iconRes;
        switch (currentSortType) {
            case "name":
                displayText = getString(R.string.sort_by_name);
                iconRes = R.drawable.ic_up;
                break;
            case "songCount":
                displayText = getString(R.string.sort_by_song_count);
                iconRes = R.drawable.ic_up;
                break;
            case "createTime":
                displayText = getString(R.string.sort_by_create_time);
                iconRes = R.drawable.ic_up;
                break;
            default:
                displayText = getString(R.string.sort_by_name);
                iconRes = R.drawable.ic_up;
                break;
        }
        binding.buttonSort.setText(displayText);
        binding.buttonSort.setIconResource(iconRes);
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
                // 获取所有非系统歌单
                List<Playlist> playlists = LitePal.where("isSystemPlaylist = ?", "0")
                        .find(Playlist.class);
                
                List<Song> allPlaylistSongs = new ArrayList<>();
                
                // 从所有歌单中获取歌曲
                for (Playlist playlist : playlists) {
                    List<Song> playlistSongs = com.magicalstory.music.model.PlaylistSong.getPlaylistSongs(playlist.getId());
                    allPlaylistSongs.addAll(playlistSongs);
                }
                
                // 随机打乱歌曲列表
                java.util.Collections.shuffle(allPlaylistSongs);

                // 在主线程中处理播放
                if (mainHandler != null) {
                    mainHandler.post(() -> {
                        if (allPlaylistSongs != null && !allPlaylistSongs.isEmpty()) {
                            // 将随机歌曲列表设置为播放列表并播放第一首
                            if (getActivity() instanceof com.magicalstory.music.MainActivity mainActivity) {
                                mainActivity.playFromPlaylist(allPlaylistSongs, 0);
                            }
                        } else {
                            ToastUtils.showToast(getContext(), getString(R.string.no_songs_to_play));
                        }
                    });
                }
            } catch (Exception e) {
                android.util.Log.e("PlaylistFragment", "Error starting random play: " + e.getMessage(), e);
                // 在主线程中显示错误信息
                if (mainHandler != null) {
                    mainHandler.post(() -> {
                        ToastUtils.showToast(getContext(), getString(R.string.random_play_failed));
                    });
                }
            }
        });
    }

    @Override
    public boolean autoHideBottomNavigation() {
        return false;
    }

    /**
     * 刷新音乐列表
     */
    @Override
    protected void onRefreshMusicList() {
        // 重新加载歌单数据
        refreshFragmentAsync();
    }

    /**
     * 在后台线程执行刷新操作
     */
    @Override
    protected void performRefreshInBackground() {
        try {
            // 重新加载歌单数据
            loadPlaylistData();
            
            // 打印原始数据到控制台
            System.out.println("PlaylistFragment后台刷新完成");
            
        } catch (Exception e) {
            e.printStackTrace();
            System.out.println("PlaylistFragment后台刷新失败: " + e.getMessage());
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
                // 重新加载歌单数据
                loadPlaylistData();
            }
            
            System.out.println("PlaylistFragment UI更新完成");
            
        } catch (Exception e) {
            e.printStackTrace();
            System.out.println("PlaylistFragment UI更新失败: " + e.getMessage());
        }
    }


    @Override
    public void onResume() {
        super.onResume();
        showBottomNavigation();
    }


    @Override
    protected FloatingActionButton getFab() {
        return binding.fabAddPlaylist;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        
        // 注销广播接收器
        try {
            androidx.localbroadcastmanager.content.LocalBroadcastManager.getInstance(requireContext())
                    .unregisterReceiver(refreshReceiver);
        } catch (Exception e) {
            android.util.Log.e("PlaylistFragment", "注销广播接收器失败: " + e.getMessage(), e);
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
}
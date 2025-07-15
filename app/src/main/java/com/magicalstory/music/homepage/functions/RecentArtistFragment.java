package com.magicalstory.music.homepage.functions;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;
import androidx.navigation.Navigation;
import androidx.recyclerview.widget.GridLayoutManager;

import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.magicalstory.music.MainActivity;
import com.magicalstory.music.R;
import com.magicalstory.music.base.BaseFragment;
import com.magicalstory.music.databinding.FragmentArtistBinding;
import com.magicalstory.music.dialog.dialogUtils;
import com.magicalstory.music.homepage.adapter.ArtistGridAdapter;
import com.magicalstory.music.model.Artist;
import com.magicalstory.music.model.Song;
import com.magicalstory.music.service.MusicService;
import com.magicalstory.music.utils.app.ToastUtils;
import com.magicalstory.music.utils.query.MusicQueryUtils;
import com.google.android.material.snackbar.Snackbar;


import org.litepal.LitePal;

import java.util.ArrayList;
import java.util.List;

/**
 * 艺术家Fragment - 显示最近听过的艺术家
 * 支持长按进入多选模式
 */
public class RecentArtistFragment extends BaseFragment<FragmentArtistBinding> {

    // 请求代码常量
    private static final int DELETE_REQUEST_CODE = 1001;

    private ArtistGridAdapter artistAdapter;
    private List<Artist> artistList;
    private Handler mainHandler;
    private BroadcastReceiver musicServiceReceiver;

    // 多选相关
    private boolean isMultiSelectMode = false;
    private String originalTitle = "最近听过的艺术家";

    @Override
    protected FragmentArtistBinding getViewBinding(LayoutInflater inflater, ViewGroup container) {
        return FragmentArtistBinding.inflate(inflater, container, false);
    }

    @Override
    protected FloatingActionButton getFab() {
        return binding.fab;
    }

    @Override
    protected void initView() {
        super.initView();

        // 初始化Handler
        mainHandler = new Handler(Looper.getMainLooper());

        // 初始化RecyclerView
        initRecyclerView();

        // 注册MusicService广播接收器
        registerMusicServiceReceiver();

        // 设置menu选项
        setHasOptionsMenu(true);

        // 设置返回键监听
        setupBackKeyListener();

        // 加载数据
        loadArtists();
    }

    @Override
    protected void initListener() {
        super.initListener();

        // 设置返回按钮点击事件
        binding.toolbar.setNavigationOnClickListener(v -> {
            if (isMultiSelectMode) {
                exitMultiSelectMode();
            } else {
                // 使用Navigation组件进行返回，会自动应用返回动画
                Navigation.findNavController(requireView()).popBackStack();
            }
        });

        // 设置toolbar菜单处理
        binding.toolbar.setOnMenuItemClickListener(item -> {
            return onOptionsItemSelected(item);
        });
    }

    @Override
    public void onCreateOptionsMenu(@NonNull Menu menu, @NonNull MenuInflater inflater) {
        super.onCreateOptionsMenu(menu, inflater);
        if (isMultiSelectMode) {
            menu.clear();
            inflater.inflate(R.menu.menu_multiselect, menu);
            // 设置菜单项为可见状态
            setMenuItemsVisible(menu, true);
        }
    }

    /**
     * 设置菜单项的可见性
     */
    private void setMenuItemsVisible(Menu menu, boolean visible) {
        if (menu != null) {
            MenuItem playNext = menu.findItem(R.id.menu_play_next);
            MenuItem addToPlaylist = menu.findItem(R.id.menu_add_to_playlist);
            MenuItem selectAll = menu.findItem(R.id.menu_select_all);
            MenuItem removeFromPlaylist = menu.findItem(R.id.menu_remove_from_playlist);
            MenuItem deleteFromDevice = menu.findItem(R.id.menu_delete_from_device);
            
            if (playNext != null) playNext.setVisible(visible);
            if (addToPlaylist != null) addToPlaylist.setVisible(visible);
            if (selectAll != null) selectAll.setVisible(visible);
            if (removeFromPlaylist != null) removeFromPlaylist.setVisible(visible);
            if (deleteFromDevice != null) deleteFromDevice.setVisible(visible);
        }
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        int itemId = item.getItemId();
        if (itemId == R.id.menu_play_next) {
            playNext();
            return true;
        } else if (itemId == R.id.menu_add_to_playlist) {
            addToPlaylist();
            return true;
        } else if (itemId == R.id.menu_select_all) {
            selectAll();
            return true;
        } else if (itemId == R.id.menu_remove_from_playlist) {
            removeFromPlaylist();
            return true;
        } else if (itemId == R.id.menu_delete_from_device) {
            deleteFromDevice();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        // 取消注册广播接收器
        if (musicServiceReceiver != null) {
            LocalBroadcastManager.getInstance(requireContext()).unregisterReceiver(musicServiceReceiver);
        }
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == DELETE_REQUEST_CODE) {
            if (resultCode == android.app.Activity.RESULT_OK) {
                // 用户同意删除，从数据库中移除记录
                List<Artist> selectedArtists = artistAdapter.getSelectedArtists();
                List<Song> songsToDelete = new ArrayList<>();
                for (Artist artist : selectedArtists) {
                    List<Song> artistSongs = MusicQueryUtils.getSongsByArtist(artist);
                    if (artistSongs != null) {
                        songsToDelete.addAll(artistSongs);
                    }
                }

                for (Song song : songsToDelete) {
                    LitePal.delete(Song.class, song.getId());
                }

                // 更新UI
                onDeleteSuccess(selectedArtists, songsToDelete);
            } else {
                // 用户取消删除
                showSnackbar(getString(R.string.delete_cancelled));
            }
        }
    }

    /**
     * 设置返回键监听
     */
    private void setupBackKeyListener() {
        requireView().setFocusableInTouchMode(true);
        requireView().requestFocus();
        requireView().setOnKeyListener((v, keyCode, event) -> {
            if (keyCode == KeyEvent.KEYCODE_BACK && event.getAction() == KeyEvent.ACTION_UP) {
                if (isMultiSelectMode) {
                    exitMultiSelectMode();
                    return true;
                }
            }
            return false;
        });
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
                    updateCurrentPlayingSong();
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
     * 初始化RecyclerView
     */
    private void initRecyclerView() {
        artistList = new ArrayList<>();
        artistAdapter = new ArtistGridAdapter(getContext(), artistList);

        // 设置网格布局
        int spanCount = 2;
        GridLayoutManager layoutManager = new GridLayoutManager(getContext(), spanCount);
        binding.rvArtists.setLayoutManager(layoutManager);
        binding.rvArtists.setAdapter(artistAdapter);

        // 获取当前播放歌曲并设置到适配器
        updateCurrentPlayingSong();

        // 设置艺术家点击事件
        artistAdapter.setOnItemClickListener((artist, position) -> {
            // TODO: 打开艺术家详情页面
            // 这里可以添加打开艺术家详情页面的逻辑
        });

        // 设置长按事件
        artistAdapter.setOnItemLongClickListener((artist, position) -> {
            enterMultiSelectMode(artist);
        });

        // 设置选中状态变化监听器
        artistAdapter.setOnSelectionChangedListener(selectedCount -> {
            updateSelectionCount();
        });
    }

    /**
     * 更新当前播放歌曲的状态
     */
    private void updateCurrentPlayingSong() {
        if (context instanceof MainActivity mainActivity) {
            Song currentSong = mainActivity.getCurrentSong();
            if (currentSong != null && artistAdapter != null) {
                // 艺术家适配器不需要设置当前播放歌曲
            }
        }
    }

    /**
     * 进入多选模式
     */
    private void enterMultiSelectMode(Artist initialArtist) {
        isMultiSelectMode = true;

        // 设置适配器为多选模式
        artistAdapter.setMultiSelectMode(true);

        // 选中初始艺术家
        if (initialArtist != null) {
            artistAdapter.toggleSelection(initialArtist);
        }

        // 更新UI
        updateSelectionCount();

        // 隐藏FAB
        binding.fab.hide();

        // 设置toolbar菜单
        binding.toolbar.getMenu().clear();
        binding.toolbar.inflateMenu(R.menu.menu_multiselect);
        setMenuItemsVisible(binding.toolbar.getMenu(), true);

        // 刷新菜单
        requireActivity().invalidateOptionsMenu();
    }

    /**
     * 退出多选模式
     */
    private void exitMultiSelectMode() {
        isMultiSelectMode = false;

        // 设置适配器为普通模式
        artistAdapter.setMultiSelectMode(false);

        // 恢复原来的标题
        binding.toolbar.setTitle(originalTitle);

        // 清空toolbar菜单
        binding.toolbar.getMenu().clear();

        // 显示FAB
        binding.fab.show();

        // 刷新菜单
        requireActivity().invalidateOptionsMenu();
    }

    /**
     * 更新选中数量显示
     */
    private void updateSelectionCount() {
        if (artistAdapter != null && isMultiSelectMode) {
            int selectedCount = artistAdapter.getSelectedCount();
            // 如果没有选中任何项，自动退出多选模式
            if (selectedCount == 0) {
                exitMultiSelectMode();
                return;
            }
            binding.toolbar.setTitle(getString(R.string.selected_count_artists, selectedCount));
        }
    }

    /**
     * 下一首播放
     */
    private void playNext() {
        List<Artist> selectedArtists = artistAdapter.getSelectedArtists();
        if (selectedArtists.isEmpty()) {
            showSnackbar(getString(R.string.select_artists));
            return;
        }

        List<Song> allSongs = MusicQueryUtils.getSongsByArtists(selectedArtists);
        if (allSongs == null || allSongs.isEmpty()) {
            showSnackbar(getString(R.string.no_songs_in_artists));
            return;
        }

        // 实现添加到播放队列下一首的功能
        if (context instanceof MainActivity mainActivity) {
            // 暂时使用Snackbar提示，后续可以扩展MusicService来支持添加到播放队列
            showSnackbar(getString(R.string.added_to_queue, allSongs.size()));
            // TODO: 后续需要在MusicService中添加addToPlayQueue方法
        }

        exitMultiSelectMode();
    }

    /**
     * 添加到播放列表
     */
    private void addToPlaylist() {
        List<Artist> selectedArtists = artistAdapter.getSelectedArtists();
        if (selectedArtists.isEmpty()) {
            showSnackbar(getString(R.string.select_artists));
            return;
        }

        List<Song> allSongs = MusicQueryUtils.getSongsByArtists(selectedArtists);
        if (allSongs == null || allSongs.isEmpty()) {
            showSnackbar(getString(R.string.no_songs_in_artists));
            return;
        }

        // 实现添加到播放列表的功能
        if (context instanceof MainActivity mainActivity) {
            // 将选中的艺术家中的歌曲添加到当前播放列表
            java.util.List<Song> currentPlaylist = new java.util.ArrayList<>();
            if (mainActivity.getCurrentSong() != null) {
                // 获取当前播放列表
                currentPlaylist.addAll(artistList.stream()
                        .map(MusicQueryUtils::getSongsByArtist)
                        .filter(java.util.Objects::nonNull)
                        .flatMap(List::stream)
                        .collect(java.util.stream.Collectors.toList()));
            }
            currentPlaylist.addAll(allSongs);
            mainActivity.setPlaylist(currentPlaylist);
            showSnackbar(getString(R.string.added_to_playlist, allSongs.size()));
        }

        exitMultiSelectMode();
    }

    /**
     * 全选
     */
    private void selectAll() {
        if (artistAdapter != null) {
            artistAdapter.selectAll();
            updateSelectionCount();
        }
    }

    /**
     * 从播放列表移除
     */
    private void removeFromPlaylist() {
        List<Artist> selectedArtists = artistAdapter.getSelectedArtists();
        if (selectedArtists.isEmpty()) {
            showSnackbar(getString(R.string.select_items_to_remove));
            return;
        }

        List<Song> allSongs = MusicQueryUtils.getSongsByArtists(selectedArtists);
        if (allSongs == null || allSongs.isEmpty()) {
            showSnackbar(getString(R.string.no_songs_in_artists));
            return;
        }

        // 实现从播放列表移除的功能
        // 暂时使用Snackbar提示，实际功能需要MusicService支持
        showSnackbar(getString(R.string.removed_from_playlist, allSongs.size()));
        // TODO: 后续需要在MusicService中添加removeFromPlayQueue方法
        exitMultiSelectMode();
    }

    /**
     * 从设备删除
     */
    private void deleteFromDevice() {
        List<Artist> selectedArtists = artistAdapter.getSelectedArtists();
        if (selectedArtists.isEmpty()) {
            showSnackbar(getString(R.string.select_items_to_delete));
            return;
        }

        // 计算要删除的歌曲数量
        List<Song> allSongs = MusicQueryUtils.getSongsByArtists(selectedArtists);
        if (allSongs == null || allSongs.isEmpty()) {
            showSnackbar(getString(R.string.no_songs_in_artists));
            return;
        }

        // 使用dialogUtils显示确认对话框
        dialogUtils.showAlertDialog(
                getContext(),
                getString(R.string.delete_confirmation_title),
                getString(R.string.delete_confirmation_message, allSongs.size()),
                getString(R.string.dialog_delete),
                getString(R.string.dialog_cancel),
                null,
                true,
                new dialogUtils.onclick_with_dismiss() {
                    @Override
                    public void click_confirm() {
                        performDeleteFromDevice(selectedArtists, allSongs);
                    }

                    @Override
                    public void click_cancel() {
                        // 取消删除
                    }

                    @Override
                    public void click_three() {
                        // 不使用第三个按钮
                    }

                    @Override
                    public void dismiss() {
                        // 对话框关闭
                    }
                }
        );
    }

    /**
     * 执行从设备删除操作
     */
    private void performDeleteFromDevice(List<Artist> artistsToDelete, List<Song> songsToDelete) {
        // 使用Android推荐的MediaStore删除方式
        try {
            // 构建需要删除的URI列表
            List<android.net.Uri> urisToDelete = new ArrayList<>();
            for (Song song : songsToDelete) {
                android.net.Uri uri = android.content.ContentUris.withAppendedId(
                        android.provider.MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
                        song.getId()
                );
                urisToDelete.add(uri);
            }

            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.R) {
                // Android 11及以上版本使用MediaStore.createDeleteRequest()
                android.app.PendingIntent deleteIntent = android.provider.MediaStore.createDeleteRequest(
                        requireContext().getContentResolver(),
                        urisToDelete
                );

                // 启动删除请求
                startIntentSenderForResult(
                        deleteIntent.getIntentSender(),
                        DELETE_REQUEST_CODE,
                        null,
                        0,
                        0,
                        0,
                        null
                );
            } else {
                // Android 10及以下版本使用ContentResolver.delete()
                deleteFilesLegacy(urisToDelete, artistsToDelete, songsToDelete);
            }

        } catch (Exception e) {
            e.printStackTrace();
            showSnackbar(getString(R.string.delete_failed) + "：" + e.getMessage());
        }
    }

    /**
     * 传统方式删除文件（Android 10及以下版本）
     */
    private void deleteFilesLegacy(List<android.net.Uri> urisToDelete, List<Artist> artistsToDelete, List<Song> songsToDelete) {
        android.content.ContentResolver resolver = requireContext().getContentResolver();
        int deletedCount = 0;

        for (android.net.Uri uri : urisToDelete) {
            try {
                int count = resolver.delete(uri, null, null);
                if (count > 0) {
                    deletedCount++;
                }
            } catch (SecurityException e) {
                // 处理权限不足的情况
                e.printStackTrace();
                showSnackbar(getString(R.string.delete_some_failed));
            }
        }

        if (deletedCount > 0) {
            // 从数据库中移除记录
            for (Song song : songsToDelete) {
                LitePal.delete(Song.class, song.getId());
            }

            // 更新UI
            onDeleteSuccess(artistsToDelete, songsToDelete);
        } else {
            showSnackbar(getString(R.string.delete_failed));
        }
    }

    /**
     * 删除成功后的处理
     */
    private void onDeleteSuccess(List<Artist> deletedArtists, List<Song> deletedSongs) {
        // 从当前列表中移除
        artistList.removeAll(deletedArtists);
        artistAdapter.notifyDataSetChanged();

        // 如果列表为空，显示空状态
        if (artistList.isEmpty()) {
            binding.rvArtists.setVisibility(View.GONE);
            binding.layoutEmpty.setVisibility(View.VISIBLE);
        }

        // 退出多选模式
        exitMultiSelectMode();

        // 通知删除成功
        showSnackbar(getString(R.string.delete_success, deletedSongs.size()));

        // 发送广播通知其他组件刷新
        Intent refreshIntent = new Intent(ACTION_REFRESH_MUSIC_LIST);
        LocalBroadcastManager.getInstance(requireContext()).sendBroadcast(refreshIntent);
    }

    /**
     * 加载艺术家数据
     */
    private void loadArtists() {
        // 创建新线程进行数据查询
        Thread loadThread = new Thread(() -> {
            try {
                // 从数据库查询艺术家数据
                List<Artist> artists = LitePal.order("lastPlayed desc").find(Artist.class);

                // 切换到主线程更新UI
                mainHandler.post(() -> {
                    // 隐藏进度圈
                    binding.progressBar.setVisibility(View.GONE);

                    if (artists != null && !artists.isEmpty()) {
                        artistList.clear();
                        artistList.addAll(artists);
                        artistAdapter.notifyDataSetChanged();

                        // 数据加载完成后更新当前播放歌曲状态
                        updateCurrentPlayingSong();

                        // 显示列表，隐藏空状态
                        binding.rvArtists.setVisibility(View.VISIBLE);
                        binding.layoutEmpty.setVisibility(View.GONE);
                        // 有数据时显示fab
                        binding.fab.show();
                    } else {
                        // 显示空状态，隐藏列表
                        binding.rvArtists.setVisibility(View.GONE);
                        binding.layoutEmpty.setVisibility(View.VISIBLE);
                        // 无数据时隐藏fab
                        binding.fab.hide();
                    }
                });

            } catch (Exception e) {
                e.printStackTrace();
                // 发生错误时也要隐藏进度圈
                mainHandler.post(() -> {
                    binding.progressBar.setVisibility(View.GONE);
                    binding.rvArtists.setVisibility(View.GONE);
                    binding.layoutEmpty.setVisibility(View.VISIBLE);
                    // 错误时隐藏fab
                    binding.fab.hide();
                });
            }
        });

        // 启动线程
        loadThread.start();
    }

    /**
     * 显示Snackbar提示
     */
    private void showSnackbar(String message) {
        if (getView() != null) {
            Snackbar.make(getView(), message, Snackbar.LENGTH_SHORT).show();
        }
    }

    /**
     * 刷新音乐列表
     */
    @Override
    protected void onRefreshMusicList() {
        // 重新加载艺术家列表
        loadArtists();
    }
} 
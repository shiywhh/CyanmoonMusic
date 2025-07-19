package com.magicalstory.music.fragment;

import static java.lang.Thread.sleep;

import android.content.Intent;
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
import androidx.media3.common.util.UnstableApi;
import androidx.navigation.Navigation;
import androidx.recyclerview.widget.GridLayoutManager;

import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.magicalstory.music.MainActivity;
import com.magicalstory.music.R;
import com.magicalstory.music.base.BaseFragment;
import com.magicalstory.music.databinding.FragmentAlbumBinding;
import com.magicalstory.music.dialog.dialogUtils;
import com.magicalstory.music.adapter.AlbumGridAdapter;
import com.magicalstory.music.model.Album;
import com.magicalstory.music.model.Song;
import com.magicalstory.music.player.MediaControllerHelper;
import com.magicalstory.music.utils.query.MusicQueryUtils;
import com.magicalstory.music.utils.app.ToastUtils;
import com.magicalstory.music.utils.file.FileDeleteUtils;
import com.google.android.material.snackbar.Snackbar;


import org.litepal.LitePal;

import java.util.ArrayList;
import java.util.List;

/**
 * 专辑Fragment - 显示最近播放的专辑
 * 支持长按进入多选模式
 */
@UnstableApi
public class AlbumListFragment extends BaseFragment<FragmentAlbumBinding> {

    // 请求代码常量
    private static final int DELETE_REQUEST_CODE = FileDeleteUtils.DELETE_REQUEST_CODE;

    private AlbumGridAdapter albumAdapter;
    private List<Album> albumList;
    private Handler mainHandler;

    private MediaControllerHelper controllerHelper;
    private final MediaControllerHelper.PlaybackStateListener playbackStateListener = new MediaControllerHelper.PlaybackStateListener() {
        @Override
        public void songChange(Song newSong) {
            // 更新当前播放歌曲的状态
            updateCurrentPlayingSong();
        }
    };

    // 多选相关
    private boolean isMultiSelectMode = false;
    private String originalTitle = "最近播放专辑";

    @Override
    protected FragmentAlbumBinding getViewBinding(LayoutInflater inflater, ViewGroup container) {
        return FragmentAlbumBinding.inflate(inflater, container, false);
    }

    @Override
    protected boolean usePersistentView() {
        return true;
    }

    @Override
    protected int getLayoutRes() {
        return R.layout.fragment_album;
    }

    private void initControllerHelper() {
        controllerHelper = MediaControllerHelper.getInstance();
        controllerHelper.addPlaybackStateListener(playbackStateListener);
    }

    @Override
    protected FragmentAlbumBinding bindPersistentView(View view) {
        return FragmentAlbumBinding.bind(view);
    }

    @Override
    protected FloatingActionButton getFab() {
        return binding.fab;
    }

    @Override
    protected void initViewForPersistentView() {
        super.initViewForPersistentView();

        // 初始化Handler
        mainHandler = new Handler(Looper.getMainLooper());

        // 初始化RecyclerView
        initRecyclerView();

        // 设置menu选项
        setHasOptionsMenu(true);

        // 设置返回键监听
        setupBackKeyListener();

        // 初始化toolbar菜单
        binding.toolbar.inflateMenu(R.menu.menu_songs_list);
        setNormalMenuItemsVisible(binding.toolbar.getMenu(), true);

        // 加载数据
        loadAlbums();

        initControllerHelper();
    }

    @Override
    protected void initListenerForPersistentView() {
        super.initListenerForPersistentView();

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
        binding.toolbar.setOnMenuItemClickListener(item -> onOptionsItemSelected(item));

        // 设置FAB点击事件 - 播放所有专辑的歌曲
        binding.fab.setOnClickListener(v -> {
            playAllAlbumSongs();
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
        } else {
            // 非多选模式下显示随机播放、下一首播放、添加到播放列表菜单
            menu.clear();
            inflater.inflate(R.menu.menu_songs_list, menu);
            // 设置菜单项为可见状态
            setNormalMenuItemsVisible(menu, true);
        }
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        int itemId = item.getItemId();

        if (isMultiSelectMode) {
            // 多选模式下的菜单处理
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
        } else {
            // 普通模式下的菜单处理
            if (itemId == R.id.action_shuffle_play) {
                shufflePlay();
                return true;
            } else if (itemId == R.id.action_play_next) {
                addToPlayNext();
                return true;
            } else if (itemId == R.id.action_add_to_playlist) {
                addToPlaylistNormal();
                return true;
            }
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();

        if (controllerHelper != null) {
            controllerHelper.removePlaybackStateListener(playbackStateListener);
        }
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == DELETE_REQUEST_CODE) {
            List<Album> selectedAlbums = albumAdapter.getSelectedAlbums();
            List<Song> songsToDelete = new ArrayList<>();
            for (Album album : selectedAlbums) {
                List<Song> albumSongs = MusicQueryUtils.getSongsByAlbum(album);
                if (albumSongs != null) {
                    songsToDelete.addAll(albumSongs);
                }
            }

            // 使用FileDeleteUtils处理删除结果
            FileDeleteUtils.handleDeleteResult(this, requestCode, resultCode, data, songsToDelete, new FileDeleteUtils.DeleteCallback() {
                @Override
                public void onDeleteSuccess(List<Song> deletedSongs) {
                    // 删除空专辑记录（只删除没有歌曲的专辑）
                    FileDeleteUtils.deleteEmptyAlbums(context,selectedAlbums);

                    // 更新UI
                    handleDeleteSuccess(selectedAlbums, deletedSongs);
                }

                @Override
                public void onDeleteFailed(String errorMessage) {
                    showSnackbar(getString(R.string.delete_failed) + "：" + errorMessage);
                }
            });
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
     * 初始化RecyclerView
     */
    private void initRecyclerView() {
        albumList = new ArrayList<>();
        albumAdapter = new AlbumGridAdapter(getContext(), albumList);

        // 设置网格布局
        int spanCount = 2;
        GridLayoutManager layoutManager = new GridLayoutManager(getContext(), spanCount);
        binding.rvAlbums.setLayoutManager(layoutManager);
        binding.rvAlbums.setAdapter(albumAdapter);


        // 设置专辑点击事件
        albumAdapter.setOnItemClickListener((album, position) -> {
            // 跳转到专辑详情页面
            Bundle bundle = new Bundle();
            bundle.putLong("album_id", album.getAlbumId());
            bundle.putString("artist_name", album.getArtist());
            bundle.putString("album_name", album.getAlbumName());
            Navigation.findNavController(requireView()).navigate(R.id.action_albums_to_album_detail, bundle);
        });

        // 设置长按事件
        albumAdapter.setOnItemLongClickListener((album, position) -> {
            enterMultiSelectMode(album);
        });

        // 设置选中状态变化监听器
        albumAdapter.setOnSelectionChangedListener(selectedCount -> {
            updateSelectionCount();
        });
    }


    /**
     * 进入多选模式
     */
    private void enterMultiSelectMode(Album initialAlbum) {
        isMultiSelectMode = true;

        // 设置适配器为多选模式
        albumAdapter.setMultiSelectMode(true);

        // 选中初始专辑
        if (initialAlbum != null) {
            albumAdapter.toggleSelection(initialAlbum);
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
     * 设置多选模式菜单项的可见性
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

    /**
     * 设置普通模式菜单项的可见性
     */
    private void setNormalMenuItemsVisible(Menu menu, boolean visible) {
        if (menu != null) {
            MenuItem shufflePlay = menu.findItem(R.id.action_shuffle_play);
            MenuItem playNext = menu.findItem(R.id.action_play_next);
            MenuItem addToPlaylist = menu.findItem(R.id.action_add_to_playlist);

            if (shufflePlay != null) shufflePlay.setVisible(visible);
            if (playNext != null) playNext.setVisible(visible);
            if (addToPlaylist != null) addToPlaylist.setVisible(visible);
        }
    }

    /**
     * 退出多选模式
     */
    private void exitMultiSelectMode() {
        isMultiSelectMode = false;

        // 设置适配器为普通模式
        albumAdapter.setMultiSelectMode(false);

        // 恢复原来的标题
        binding.toolbar.setTitle(originalTitle);

        // 清空toolbar菜单并重新设置普通模式菜单
        binding.toolbar.getMenu().clear();
        binding.toolbar.inflateMenu(R.menu.menu_songs_list);
        setNormalMenuItemsVisible(binding.toolbar.getMenu(), true);

        // 显示FAB
        binding.fab.show();

        // 刷新菜单
        requireActivity().invalidateOptionsMenu();
    }

    /**
     * 更新选中数量显示
     */
    private void updateSelectionCount() {
        if (albumAdapter != null && isMultiSelectMode) {
            int selectedCount = albumAdapter.getSelectedCount();
            // 如果没有选中任何项，自动退出多选模式
            if (selectedCount == 0) {
                exitMultiSelectMode();
                return;
            }
            binding.toolbar.setTitle(getString(R.string.selected_count_albums, selectedCount));
        }
    }

    /**
     * 更新当前播放歌曲的状态
     */
    private void updateCurrentPlayingSong() {
        if (albumAdapter != null) {
            albumAdapter.notifyDataSetChanged();
        }
    }

    /**
     * 下一首播放
     */
    private void playNext() {
        List<Album> selectedAlbums = albumAdapter.getSelectedAlbums();
        if (selectedAlbums.isEmpty()) {
            showSnackbar(getString(R.string.select_albums));
            return;
        }

        List<Song> allSongs = MusicQueryUtils.getSongsByAlbums(selectedAlbums);
        if (allSongs == null || allSongs.isEmpty()) {
            showSnackbar(getString(R.string.no_songs_in_albums));
            return;
        }

        // 实现添加到播放队列下一首的功能
        if (getActivity() instanceof MainActivity mainActivity) {
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
        List<Album> selectedAlbums = albumAdapter.getSelectedAlbums();
        if (selectedAlbums.isEmpty()) {
            showSnackbar(getString(R.string.select_albums));
            return;
        }

        List<Song> allSongs = MusicQueryUtils.getSongsByAlbums(selectedAlbums);
        if (allSongs == null || allSongs.isEmpty()) {
            showSnackbar(getString(R.string.no_songs_in_albums));
            return;
        }

        // 实现添加到播放列表的功能
        if (getActivity() instanceof MainActivity mainActivity) {
            // 将选中的专辑中的歌曲添加到当前播放列表
            java.util.List<Song> currentPlaylist = new java.util.ArrayList<>();
            if (mainActivity.getCurrentSong() != null) {
                // 获取当前播放列表
                currentPlaylist.addAll(albumList.stream()
                        .map(MusicQueryUtils::getSongsByAlbum)
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
        if (albumAdapter != null) {
            albumAdapter.selectAll();
            updateSelectionCount();
        }
    }

    /**
     * 从播放列表移除
     */
    private void removeFromPlaylist() {
        List<Album> selectedAlbums = albumAdapter.getSelectedAlbums();
        if (selectedAlbums.isEmpty()) {
            showSnackbar(getString(R.string.select_items_to_remove));
            return;
        }

        List<Song> allSongs = MusicQueryUtils.getSongsByAlbums(selectedAlbums);
        if (allSongs == null || allSongs.isEmpty()) {
            showSnackbar(getString(R.string.no_songs_in_albums));
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
        List<Album> selectedAlbums = albumAdapter.getSelectedAlbums();
        if (selectedAlbums.isEmpty()) {
            showSnackbar(getString(R.string.select_items_to_delete));
            return;
        }

        // 计算要删除的歌曲数量
        List<Song> allSongs = MusicQueryUtils.getSongsByAlbums(selectedAlbums);
        
        if (allSongs == null || allSongs.isEmpty()) {
            // 如果没有歌曲，直接删除专辑记录
            // 直接删除专辑记录
            FileDeleteUtils.deleteAlbums(context,selectedAlbums);
            // 更新UI
            handleDeleteSuccess(selectedAlbums, new ArrayList<>());
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
                        performDeleteFromDevice(selectedAlbums, allSongs);
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
    private void performDeleteFromDevice(List<Album> albumsToDelete, List<Song> songsToDelete) {
        // 使用FileDeleteUtils统一处理删除操作
        FileDeleteUtils.deleteSongsWithMediaStore(this, songsToDelete, new FileDeleteUtils.DeleteCallback() {
            @Override
            public void onDeleteSuccess(List<Song> deletedSongs) {
                // 删除空专辑记录（只删除没有歌曲的专辑）
                FileDeleteUtils.deleteEmptyAlbums(context,albumsToDelete);

                // 更新UI
                handleDeleteSuccess(albumsToDelete, deletedSongs);
            }

            @Override
            public void onDeleteFailed(String errorMessage) {
                showSnackbar(getString(R.string.delete_failed) + "：" + errorMessage);
            }
        });
    }

    /**
     * 删除成功后的处理
     */
    private void handleDeleteSuccess(List<Album> deletedAlbums, List<Song> deletedSongs) {
        // 退出多选模式
        exitMultiSelectMode();

        // 通知删除成功
        if (deletedSongs.isEmpty()) {
            showSnackbar("已删除 " + deletedAlbums.size() + " 个专辑记录");
        } else {
            showSnackbar(getString(R.string.delete_success, deletedSongs.size()));
        }

        // 重新加载数据以反映数据库变化
        loadAlbums();

        // 发送广播通知其他组件刷新
        Intent refreshIntent = new Intent(FileDeleteUtils.ACTION_REFRESH_MUSIC_LIST);
        LocalBroadcastManager.getInstance(requireContext()).sendBroadcast(refreshIntent);
    }

    /**
     * 加载专辑数据
     */
    private void loadAlbums() {
        // 创建新线程进行数据查询
        Thread loadThread = new Thread(() -> {
            try {
                // 从数据库查询专辑数据
                List<Album> albums;

                // 检查是否有传递的艺术家名称
                Bundle arguments = getArguments();
                if (arguments != null && arguments.containsKey("artistName")) {
                    String artistName = arguments.getString("artistName");
                    // 查询特定艺术家的专辑
                    albums = LitePal.where("artist = ?", artistName)
                            .order("lastPlayed desc")
                            .find(Album.class);

                    // 更新标题
                    mainHandler.post(() -> {
                        binding.toolbar.setTitle(artistName + " 的专辑");
                    });
                } else {
                    // 查询所有专辑
                    albums = LitePal.order("lastPlayed desc").find(Album.class);
                }

                sleep(200);

                // 切换到主线程更新UI
                mainHandler.post(() -> {
                    // 隐藏进度圈
                    binding.progressBar.setVisibility(View.GONE);

                    if (albums != null && !albums.isEmpty()) {
                        albumList.clear();
                        albumList.addAll(albums);
                        albumAdapter.notifyDataSetChanged();


                        // 显示列表，隐藏空状态
                        binding.rvAlbums.setVisibility(View.VISIBLE);
                        binding.layoutEmpty.setVisibility(View.GONE);
                        binding.fab.setVisibility(View.VISIBLE);
                        // 有数据时显示fab
                        binding.fab.show();

                        // 延迟禁用动画，确保首屏动画完成
                        mainHandler.postDelayed(() -> {
                            if (albumAdapter != null) {
                                albumAdapter.disableLoadAnimation();
                            }
                        }, 1500); // 1.5秒后禁用动画
                    } else {
                        // 显示空状态，隐藏列表
                        binding.rvAlbums.setVisibility(View.GONE);
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
                    binding.rvAlbums.setVisibility(View.GONE);
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
        ToastUtils.showToast(context, message);
    }

    /**
     * 刷新音乐列表
     */
    @Override
    protected void onRefreshMusicList() {
        // 重新加载专辑列表，并重置动画状态
        if (albumAdapter != null) {
            albumAdapter.updateData(albumList);
        }
        loadAlbums();
    }

    /**
     * 随机播放（普通模式）
     */
    private void shufflePlay() {
        if (albumList == null || albumList.isEmpty()) {
            ToastUtils.showToast(getContext(), getString(R.string.no_albums_to_play));
            return;
        }

        // 在后台线程查询所有专辑的歌曲
        new Thread(() -> {
            try {
                List<Song> allSongs = new ArrayList<>();

                // 遍历所有专辑，获取每个专辑的歌曲
                for (Album album : albumList) {
                    List<Song> albumSongs = LitePal.where("albumId = ? and artist = ?",
                                    String.valueOf(album.getAlbumId()), album.getArtist())
                            .order("track asc")
                            .find(Song.class);
                    if (albumSongs != null) {
                        allSongs.addAll(albumSongs);
                    }
                }

                // 在主线程更新UI并播放
                mainHandler.post(() -> {
                    if (!allSongs.isEmpty()) {
                        if (getActivity() instanceof MainActivity mainActivity) {
                            // 随机打乱歌曲顺序
                            java.util.Collections.shuffle(allSongs);
                            mainActivity.playFromPlaylist(allSongs, 0);
                        }
                    } else {
                        ToastUtils.showToast(getContext(), getString(R.string.no_songs_to_play));
                    }
                });

            } catch (Exception e) {
                e.printStackTrace();
                mainHandler.post(() -> {
                    ToastUtils.showToast(getContext(), "播放失败：" + e.getMessage());
                });
            }
        }).start();
    }

    /**
     * 添加到播放队列的下一首位置（普通模式）
     */
    private void addToPlayNext() {
        if (albumList == null || albumList.isEmpty()) {
            ToastUtils.showToast(getContext(), getString(R.string.no_albums_to_add));
            return;
        }

        // 在后台线程查询所有专辑的歌曲
        new Thread(() -> {
            try {
                List<Song> allSongs = new ArrayList<>();

                // 遍历所有专辑，获取每个专辑的歌曲
                for (Album album : albumList) {
                    List<Song> albumSongs = LitePal.where("albumId = ? and artist = ?",
                                    String.valueOf(album.getAlbumId()), album.getArtist())
                            .order("track asc")
                            .find(Song.class);
                    if (albumSongs != null) {
                        allSongs.addAll(albumSongs);
                    }
                }

                // 在主线程更新UI并添加到播放队列
                mainHandler.post(() -> {
                    if (!allSongs.isEmpty()) {
                        // 获取MediaControllerHelper实例
                        MediaControllerHelper controllerHelper = MediaControllerHelper.getInstance();
                        if (controllerHelper != null) {
                            // 添加歌曲到下一首播放位置
                            controllerHelper.addSongsToPlayNext(allSongs);
                            ToastUtils.showToast(getContext(), getString(R.string.added_to_queue_next));
                        } else {
                            ToastUtils.showToast(getContext(), "播放控制器未初始化");
                        }
                    } else {
                        ToastUtils.showToast(getContext(), getString(R.string.no_songs_to_add));
                    }
                });

            } catch (Exception e) {
                e.printStackTrace();
                mainHandler.post(() -> {
                    ToastUtils.showToast(getContext(), "添加失败：" + e.getMessage());
                });
            }
        }).start();
    }

    /**
     * 添加到播放列表（普通模式）
     */
    private void addToPlaylistNormal() {
        if (albumList == null || albumList.isEmpty()) {
            ToastUtils.showToast(getContext(), getString(R.string.no_albums_to_add));
            return;
        }

        // 在后台线程查询所有专辑的歌曲
        new Thread(() -> {
            try {
                List<Song> allSongs = new ArrayList<>();

                // 遍历所有专辑，获取每个专辑的歌曲
                for (Album album : albumList) {
                    List<Song> albumSongs = LitePal.where("albumId = ? and artist = ?",
                                    String.valueOf(album.getAlbumId()), album.getArtist())
                            .order("track asc")
                            .find(Song.class);
                    if (albumSongs != null) {
                        allSongs.addAll(albumSongs);
                    }
                }

                // 在主线程更新UI并添加到播放列表
                mainHandler.post(() -> {
                    if (!allSongs.isEmpty()) {
                        // 获取MediaControllerHelper实例
                        MediaControllerHelper controllerHelper = MediaControllerHelper.getInstance();
                        if (controllerHelper != null) {
                            // 添加歌曲到播放列表末尾
                            controllerHelper.addSongsToPlaylist(allSongs);
                            ToastUtils.showToast(getContext(), getString(R.string.added_to_playlist));
                        } else {
                            ToastUtils.showToast(getContext(), "播放控制器未初始化");
                        }
                    } else {
                        ToastUtils.showToast(getContext(), getString(R.string.no_songs_to_add));
                    }
                });

            } catch (Exception e) {
                e.printStackTrace();
                mainHandler.post(() -> {
                    ToastUtils.showToast(getContext(), "添加失败：" + e.getMessage());
                });
            }
        }).start();
    }

    /**
     * 播放所有专辑的歌曲
     */
    private void playAllAlbumSongs() {
        if (albumList == null || albumList.isEmpty()) {
            showSnackbar("没有可播放的专辑");
            return;
        }

        // 在后台线程查询所有专辑的歌曲
        new Thread(() -> {
            try {
                List<Song> allSongs = new ArrayList<>();

                // 遍历所有专辑，获取每个专辑的歌曲
                for (Album album : albumList) {
                    List<Song> albumSongs = LitePal.where("albumId = ? and artist = ?",
                                    String.valueOf(album.getAlbumId()), album.getArtist())
                            .order("track asc")
                            .find(Song.class);
                    if (albumSongs != null) {
                        allSongs.addAll(albumSongs);
                    }
                }

                // 在主线程更新UI并播放
                mainHandler.post(() -> {
                    if (!allSongs.isEmpty()) {
                        if (getActivity() instanceof MainActivity mainActivity) {
                            mainActivity.playFromPlaylist(allSongs, 0);
                            showSnackbar("开始播放所有专辑歌曲，共 " + allSongs.size() + " 首");
                        }
                    } else {
                        showSnackbar("没有找到可播放的歌曲");
                    }
                });

            } catch (Exception e) {
                e.printStackTrace();
                mainHandler.post(() -> {
                    showSnackbar("播放失败：" + e.getMessage());
                });
            }
        }).start();
    }
}  
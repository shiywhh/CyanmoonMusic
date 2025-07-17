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
    private static final int DELETE_REQUEST_CODE = 1001;

    private AlbumGridAdapter albumAdapter;
    private List<Album> albumList;
    private Handler mainHandler;

    private MediaControllerHelper controllerHelper;
    private final MediaControllerHelper.PlaybackStateListener playbackStateListener = new MediaControllerHelper.PlaybackStateListener() {
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

        if (controllerHelper != null) {
            controllerHelper.removePlaybackStateListener(playbackStateListener);
        }
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == DELETE_REQUEST_CODE) {
            if (resultCode == android.app.Activity.RESULT_OK) {
                // 用户同意删除，从数据库中移除记录
                List<Album> selectedAlbums = albumAdapter.getSelectedAlbums();
                List<Song> songsToDelete = new ArrayList<>();
                for (Album album : selectedAlbums) {
                    List<Song> albumSongs = MusicQueryUtils.getSongsByAlbum(album);
                    if (albumSongs != null) {
                        songsToDelete.addAll(albumSongs);
                    }
                }

                for (Song song : songsToDelete) {
                    LitePal.delete(Song.class, song.getId());
                }

                // 更新UI
                onDeleteSuccess(selectedAlbums, songsToDelete);
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

        // 刷新菜单
        requireActivity().invalidateOptionsMenu();
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
        if (context instanceof MainActivity mainActivity) {
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
            showSnackbar(getString(R.string.no_songs_in_albums));
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
                deleteFilesLegacy(urisToDelete, albumsToDelete, songsToDelete);
            }

        } catch (Exception e) {
            e.printStackTrace();
            showSnackbar(getString(R.string.delete_failed) + "：" + e.getMessage());
        }
    }

    /**
     * 传统方式删除文件（Android 10及以下版本）
     */
    private void deleteFilesLegacy(List<android.net.Uri> urisToDelete, List<Album> albumsToDelete, List<Song> songsToDelete) {
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
            onDeleteSuccess(albumsToDelete, songsToDelete);
        } else {
            showSnackbar(getString(R.string.delete_failed));
        }
    }

    /**
     * 删除成功后的处理
     */
    private void onDeleteSuccess(List<Album> deletedAlbums, List<Song> deletedSongs) {
        // 从当前列表中移除
        albumList.removeAll(deletedAlbums);
        albumAdapter.notifyDataSetChanged();

        // 如果列表为空，显示空状态
        if (albumList.isEmpty()) {
            binding.rvAlbums.setVisibility(View.GONE);
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
        if (getView() != null) {
            Snackbar.make(getView(), message, Snackbar.LENGTH_SHORT).show();
        }
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
                            mainActivity.playFromPlaylist(allSongs,0);
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
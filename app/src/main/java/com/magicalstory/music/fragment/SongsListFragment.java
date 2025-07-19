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
import androidx.media3.common.util.UnstableApi;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.navigation.Navigation;

import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.magicalstory.music.MainActivity;
import com.magicalstory.music.R;
import com.magicalstory.music.base.BaseFragment;
import com.magicalstory.music.databinding.FragmentRecentSongsBinding;
import com.magicalstory.music.dialog.dialogUtils;
import com.magicalstory.music.adapter.SongVerticalAdapter;
import com.magicalstory.music.model.Song;
import com.google.android.material.snackbar.Snackbar;
import com.magicalstory.music.player.MediaControllerHelper;
import com.magicalstory.music.utils.app.ToastUtils;
import com.magicalstory.music.utils.file.FileDeleteUtils;

import org.litepal.LitePal;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * 歌曲列表Fragment
 * 可以显示不同类型的歌曲列表（最近添加、我的收藏、随机推荐）
 */
@UnstableApi
public class SongsListFragment extends BaseFragment<FragmentRecentSongsBinding> {

    // 数据类型常量
    private static final String DATA_TYPE_RECENT = "recent";
    private static final String DATA_TYPE_FAVORITE = "favorite";
    private static final String DATA_TYPE_RANDOM = "random";
    private static final String DATA_TYPE_ALBUM = "album";
    private static final String DATA_TYPE_ARTIST = "artist";
    private static final String DATA_TYPE_HISTORY = "history";
    private static final String DATA_TYPE_MOST_PLAYED = "most_played";

    // 请求代码常量
    private static final int CLEAR_LIST_REQUEST_CODE = 1002;

    private SongVerticalAdapter songAdapter;
    private List<Song> songList;
    private Handler mainHandler;
    private String dataType;

    // 多选相关
    private boolean isMultiSelectMode = false;
    private String originalTitle;

    private MediaControllerHelper controllerHelper;
    private final MediaControllerHelper.PlaybackStateListener playbackStateListener = new MediaControllerHelper.PlaybackStateListener() {
        @Override
        public void songChange(Song newSong) {
            System.out.println("newSong.getTitle() = " + newSong.getTitle());
            // 更新当前播放歌曲的状态
            updateCurrentPlayingSong();
        }

        @Override
        public void stopPlay() {
            songAdapter.setCurrentPlayingSongId(0);
            songAdapter.notifyDataSetChanged();
        }
    };

    @Override
    protected FragmentRecentSongsBinding getViewBinding(LayoutInflater inflater, ViewGroup container) {
        return FragmentRecentSongsBinding.inflate(inflater, container, false);
    }

    @Override
    protected boolean usePersistentView() {
        return true;
    }

    @Override
    protected int getLayoutRes() {
        return R.layout.fragment_recent_songs;
    }

    @Override
    protected FragmentRecentSongsBinding bindPersistentView(View view) {
        return FragmentRecentSongsBinding.bind(view);
    }

    @Override
    protected FloatingActionButton getFab() {
        return binding.fab;
    }

    @Override
    protected void initViewForPersistentView() {
        super.initViewForPersistentView();

        // 获取参数
        Bundle arguments = getArguments();
        if (arguments != null) {
            dataType = arguments.getString("dataType", DATA_TYPE_RECENT);
        } else {
            dataType = DATA_TYPE_RECENT;
        }

        // 根据数据类型设置标题
        setupTitle();

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
        loadSongsByType();

        initControllerHelper();
    }

    private void initControllerHelper() {
        controllerHelper = MediaControllerHelper.getInstance();
        controllerHelper.addPlaybackStateListener(playbackStateListener);
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

        // 设置FAB点击事件 - 播放所有歌曲
        binding.fab.setOnClickListener(v -> {
            playAllSongs();
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
            MenuItem deleteSelectedSongs = menu.findItem(R.id.menu_delete_selected_songs);

            if (playNext != null) playNext.setVisible(visible);
            if (addToPlaylist != null) addToPlaylist.setVisible(visible);
            if (selectAll != null) selectAll.setVisible(visible);
            if (removeFromPlaylist != null) removeFromPlaylist.setVisible(visible);
            if (deleteFromDevice != null) deleteFromDevice.setVisible(visible);

            // 根据数据类型决定是否显示删除所选歌曲选项
            boolean showDeleteSelected = (DATA_TYPE_FAVORITE.equals(dataType) ||
                    DATA_TYPE_HISTORY.equals(dataType) ||
                    DATA_TYPE_MOST_PLAYED.equals(dataType));
            if (deleteSelectedSongs != null) deleteSelectedSongs.setVisible(visible && showDeleteSelected);
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
            MenuItem clearList = menu.findItem(R.id.action_clear_list);

            if (shufflePlay != null) shufflePlay.setVisible(visible);
            if (playNext != null) playNext.setVisible(visible);
            if (addToPlaylist != null) addToPlaylist.setVisible(visible);

            // 根据数据类型决定是否显示清空列表选项
            boolean showClearList = (DATA_TYPE_FAVORITE.equals(dataType) ||
                    DATA_TYPE_HISTORY.equals(dataType) ||
                    DATA_TYPE_MOST_PLAYED.equals(dataType));
            if (clearList != null) clearList.setVisible(visible && showClearList);
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
            } else if (itemId == R.id.menu_delete_selected_songs) {
                deleteSelectedSongs();
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
            } else if (itemId == R.id.action_clear_list) {
                clearSongList();
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

        // 处理删除结果
        if (requestCode == FileDeleteUtils.DELETE_REQUEST_CODE) {
            List<Song> selectedSongs = songAdapter.getSelectedSongs();
            FileDeleteUtils.handleDeleteResult(this, requestCode, resultCode, data, selectedSongs, new FileDeleteUtils.DeleteCallback() {
                @Override
                public void onDeleteSuccess(List<Song> deletedSongs) {
                    // 更新UI
                    handleDeleteSuccess(deletedSongs);
                }

                @Override
                public void onDeleteFailed(String errorMessage) {
                    showSnackbar(errorMessage);
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
     * 根据数据类型设置标题
     */
    private void setupTitle() {
        String title;
        switch (dataType) {
            case DATA_TYPE_FAVORITE:
                title = "我的收藏";
                break;
            case DATA_TYPE_RANDOM:
                title = "随机推荐";
                break;
            case DATA_TYPE_HISTORY:
                title = "播放历史";
                break;
            case DATA_TYPE_MOST_PLAYED:
                title = "最常播放";
                break;
            case DATA_TYPE_ALBUM:
                // 从参数获取专辑名称
                Bundle arguments = getArguments();
                String albumName = arguments != null ? arguments.getString("albumName") : "专辑";
                title = albumName;
                break;
            case DATA_TYPE_ARTIST:
                // 从参数获取艺术家名称
                Bundle artistArguments = getArguments();
                String artistName = artistArguments != null ? artistArguments.getString("artistName") : "艺术家";
                title = artistName + " 的歌曲";
                break;
            case DATA_TYPE_RECENT:
            default:
                title = "最近添加的歌曲";
                break;
        }
        originalTitle = title;
        binding.toolbar.setTitle(title);
    }

    /**
     * 初始化RecyclerView
     */
    private void initRecyclerView() {
        songList = new ArrayList<>();
        songAdapter = new SongVerticalAdapter(getContext(), songList);

        binding.rvRecentSongs.setLayoutManager(new LinearLayoutManager(getContext()));
        binding.rvRecentSongs.setAdapter(songAdapter);

        // 获取当前播放歌曲并设置到适配器
        updateCurrentPlayingSong();

        // 设置歌曲点击事件
        songAdapter.setOnItemClickListener((song, position) -> {
            // TODO: 播放歌曲
            //songAdapter.setCurrentPlayingSongId(song.getId());
            //songAdapter.notifyItemChanged(position);
        });

        // 设置长按事件
        songAdapter.setOnItemLongClickListener((song, position) -> {
            enterMultiSelectMode(song);
        });

        // 设置选中状态变化监听器
        songAdapter.setOnSelectionChangedListener(selectedCount -> {
            updateSelectionCount();
        });
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

    /**
     * 进入多选模式
     */
    private void enterMultiSelectMode(Song initialSong) {
        isMultiSelectMode = true;

        // 设置适配器为多选模式
        songAdapter.setMultiSelectMode(true);

        // 选中初始歌曲
        if (initialSong != null) {
            songAdapter.toggleSelection(initialSong);
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
        songAdapter.setMultiSelectMode(false);

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
        if (songAdapter != null && isMultiSelectMode) {
            int selectedCount = songAdapter.getSelectedCount();
            // 如果没有选中任何项，自动退出多选模式
            if (selectedCount == 0) {
                exitMultiSelectMode();
                return;
            }
            binding.toolbar.setTitle(getString(R.string.selected_count_songs, selectedCount));
        }
    }

    /**
     * 随机播放（普通模式）
     */
    private void shufflePlay() {
        if (songList == null || songList.isEmpty()) {
            ToastUtils.showToast(getContext(), getString(R.string.no_songs_to_play));
            return;
        }

        if (getActivity() instanceof MainActivity mainActivity) {
            List<Song> shuffledSongs = new ArrayList<>(songList);
            Collections.shuffle(shuffledSongs);
            mainActivity.playFromPlaylist(shuffledSongs, 0);
        }
    }

    /**
     * 添加到播放队列的下一首位置（普通模式）
     */
    private void addToPlayNext() {
        if (songList == null || songList.isEmpty()) {
            ToastUtils.showToast(getContext(), getString(R.string.no_songs_to_add));
            return;
        }

        // 获取MediaControllerHelper实例
        MediaControllerHelper controllerHelper = MediaControllerHelper.getInstance();
        if (controllerHelper != null) {
            // 添加歌曲到下一首播放位置
            controllerHelper.addSongsToPlayNext(songList);
            ToastUtils.showToast(getContext(), getString(R.string.added_to_queue_next));
        } else {
            ToastUtils.showToast(getContext(), "播放控制器未初始化");
        }
    }

    /**
     * 添加到播放列表（普通模式）
     */
    private void addToPlaylistNormal() {
        if (songList == null || songList.isEmpty()) {
            ToastUtils.showToast(getContext(), getString(R.string.no_songs_to_add));
            return;
        }

        // 获取MediaControllerHelper实例
        MediaControllerHelper controllerHelper = MediaControllerHelper.getInstance();
        if (controllerHelper != null) {
            // 添加歌曲到播放列表末尾
            controllerHelper.addSongsToPlaylist(songList);
            ToastUtils.showToast(getContext(), getString(R.string.added_to_playlist));
        } else {
            ToastUtils.showToast(getContext(), "播放控制器未初始化");
        }
    }

    /**
     * 下一首播放（多选模式）
     */
    private void playNext() {
        List<Song> selectedSongs = songAdapter.getSelectedSongs();
        if (selectedSongs.isEmpty()) {
            showSnackbar(getString(R.string.select_songs));
            return;
        }

        // 获取MediaControllerHelper实例
        MediaControllerHelper controllerHelper = MediaControllerHelper.getInstance();
        if (controllerHelper != null) {
            // 添加选中的歌曲到下一首播放位置
            controllerHelper.addSongsToPlayNext(selectedSongs);
            ToastUtils.showToast(getContext(), getString(R.string.added_to_queue_next));
        } else {
            ToastUtils.showToast(getContext(), "播放控制器未初始化");
        }

        exitMultiSelectMode();
    }

    /**
     * 添加到播放列表（多选模式）
     */
    private void addToPlaylist() {
        List<Song> selectedSongs = songAdapter.getSelectedSongs();
        if (selectedSongs.isEmpty()) {
            showSnackbar(getString(R.string.select_songs));
            return;
        }

        // 获取MediaControllerHelper实例
        MediaControllerHelper controllerHelper = MediaControllerHelper.getInstance();
        if (controllerHelper != null) {
            // 添加选中的歌曲到播放列表末尾
            controllerHelper.addSongsToPlaylist(selectedSongs);
            ToastUtils.showToast(getContext(), getString(R.string.added_to_playlist));
        } else {
            ToastUtils.showToast(getContext(), "播放控制器未初始化");
        }

        exitMultiSelectMode();
    }

    /**
     * 全选
     */
    private void selectAll() {
        if (songAdapter != null) {
            songAdapter.selectAll();
            updateSelectionCount();
        }
    }

    /**
     * 从播放列表移除
     */
    private void removeFromPlaylist() {
        List<Song> selectedSongs = songAdapter.getSelectedSongs();
        if (selectedSongs.isEmpty()) {
            showSnackbar(getString(R.string.select_items_to_remove));
            return;
        }

        // 获取MediaControllerHelper实例
        MediaControllerHelper controllerHelper = MediaControllerHelper.getInstance();
        if (controllerHelper != null) {
            // 从播放列表中移除选中的歌曲
            List<Song> currentPlaylist = controllerHelper.getPlaylist();
            for (Song selectedSong : selectedSongs) {
                for (int i = 0; i < currentPlaylist.size(); i++) {
                    if (currentPlaylist.get(i).getId() == selectedSong.getId()) {
                        controllerHelper.removeFromPlaylist(i);
                        break;
                    }
                }
            }
            ToastUtils.showToast(getContext(), getString(R.string.removed_from_playlist, selectedSongs.size()));
        } else {
            ToastUtils.showToast(getContext(), "播放控制器未初始化");
        }

        exitMultiSelectMode();
    }

    /**
     * 从设备删除
     */
    private void deleteFromDevice() {
        List<Song> selectedSongs = songAdapter.getSelectedSongs();
        if (selectedSongs.isEmpty()) {
            showSnackbar(getString(R.string.select_items_to_delete));
            return;
        }

        // 使用dialogUtils显示确认对话框
        dialogUtils.showAlertDialog(
                getContext(),
                getString(R.string.delete_confirmation_title),
                getString(R.string.delete_confirmation_message, selectedSongs.size()),
                getString(R.string.dialog_delete),
                getString(R.string.dialog_cancel),
                null,
                true,
                new dialogUtils.onclick_with_dismiss() {
                    @Override
                    public void click_confirm() {
                        performDeleteFromDevice(selectedSongs);
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
     * 删除所选歌曲（从收藏、历史、最常播放中删除）
     */
    private void deleteSelectedSongs() {
        List<Song> selectedSongs = songAdapter.getSelectedSongs();
        if (selectedSongs.isEmpty()) {
            showSnackbar(getString(R.string.select_items_to_delete));
            return;
        }

        // 使用dialogUtils显示确认对话框
        dialogUtils.showAlertDialog(
                getContext(),
                getString(R.string.delete_records_confirmation_title),
                getString(R.string.delete_records_confirmation_message, selectedSongs.size()),
                getString(R.string.dialog_delete),
                getString(R.string.dialog_cancel),
                null,
                true,
                new dialogUtils.onclick_with_dismiss() {
                    @Override
                    public void click_confirm() {
                        performDeleteSelectedSongs(selectedSongs);
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
     * 清空歌曲列表
     */
    private void clearSongList() {
        if (songList == null || songList.isEmpty()) {
            showSnackbar("列表已经是空的");
            return;
        }

        // 使用dialogUtils显示确认对话框
        dialogUtils.showAlertDialog(
                getContext(),
                getString(R.string.clear_records_confirmation_title),
                getString(R.string.clear_records_confirmation_message),
                getString(R.string.dialog_delete),
                getString(R.string.dialog_cancel),
                null,
                true,
                new dialogUtils.onclick_with_dismiss() {
                    @Override
                    public void click_confirm() {
                        performClearSongList();
                    }

                    @Override
                    public void click_cancel() {
                        // 取消清空
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
    private void performDeleteFromDevice(List<Song> songsToDelete) {
        // 使用FileDeleteUtils统一处理删除操作
        FileDeleteUtils.deleteSongsWithMediaStore(this, songsToDelete, new FileDeleteUtils.DeleteCallback() {
            @Override
            public void onDeleteSuccess(List<Song> deletedSongs) {
                // 更新UI
                handleDeleteSuccess(deletedSongs);
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
    private void handleDeleteSuccess(List<Song> deletedSongs) {
        // 从当前列表中移除
        songList.removeAll(deletedSongs);
        songAdapter.notifyDataSetChanged();

        // 如果列表为空，显示空状态
        if (songList.isEmpty()) {
            binding.rvRecentSongs.setVisibility(View.GONE);
            binding.layoutEmpty.setVisibility(View.VISIBLE);
        }

        // 退出多选模式
        exitMultiSelectMode();

        // 通知删除成功
        showSnackbar(getString(R.string.delete_success, deletedSongs.size()));
    }

    /**
     * 执行删除所选歌曲操作
     */
    private void performDeleteSelectedSongs(List<Song> songsToDelete) {
        try {
            // 根据数据类型执行不同的删除操作
            switch (dataType) {
                case DATA_TYPE_FAVORITE:
                    // 从收藏中删除
                    for (Song song : songsToDelete) {
                        LitePal.deleteAll(com.magicalstory.music.model.FavoriteSong.class,
                                "songId = ?", String.valueOf(song.getId()));
                    }
                    showSnackbar(getString(R.string.delete_from_favorites_success, songsToDelete.size()));
                    break;

                case DATA_TYPE_HISTORY:
                    // 从播放历史中删除
                    for (Song song : songsToDelete) {
                        LitePal.deleteAll(com.magicalstory.music.model.PlayHistory.class,
                                "songId = ?", String.valueOf(song.getId()));
                    }
                    showSnackbar(getString(R.string.delete_from_history_success, songsToDelete.size()));
                    break;

                case DATA_TYPE_MOST_PLAYED:
                    // 从最常播放中删除
                    for (Song song : songsToDelete) {
                        LitePal.deleteAll(com.magicalstory.music.model.PlayHistory.class,
                                "songId = ?", String.valueOf(song.getId()));
                    }
                    showSnackbar(getString(R.string.delete_from_most_played_success, songsToDelete.size()));
                    break;
            }

            // 从当前列表中移除
            songList.removeAll(songsToDelete);
            songAdapter.notifyDataSetChanged();

            // 如果列表为空，显示空状态
            if (songList.isEmpty()) {
                binding.rvRecentSongs.setVisibility(View.GONE);
                binding.layoutEmpty.setVisibility(View.VISIBLE);
            }

            // 退出多选模式
            exitMultiSelectMode();

            FileDeleteUtils.deleteSongsFromDatabase(songsToDelete);
        } catch (Exception e) {
            e.printStackTrace();
            showSnackbar("删除失败：" + e.getMessage());
        }
    }

    /**
     * 执行清空歌曲列表操作
     */
    private void performClearSongList() {
        try {
            // 根据数据类型执行不同的清空操作
            switch (dataType) {
                case DATA_TYPE_FAVORITE:
                    // 清空收藏
                    LitePal.deleteAll(com.magicalstory.music.model.FavoriteSong.class);
                    showSnackbar("已清空收藏列表");
                    break;

                case DATA_TYPE_HISTORY:
                    // 清空播放历史
                    LitePal.deleteAll(com.magicalstory.music.model.PlayHistory.class);
                    showSnackbar("已清空播放历史");
                    break;

                case DATA_TYPE_MOST_PLAYED:
                    // 清空最常播放（实际上也是清空播放历史）
                    LitePal.deleteAll(com.magicalstory.music.model.PlayHistory.class);
                    showSnackbar("已清空最常播放列表");
                    break;
            }

            // 清空当前列表
            songList.clear();
            songAdapter.notifyDataSetChanged();

            // 显示空状态
            binding.rvRecentSongs.setVisibility(View.GONE);
            binding.layoutEmpty.setVisibility(View.VISIBLE);

        } catch (Exception e) {
            e.printStackTrace();
            showSnackbar("清空失败：" + e.getMessage());
        }
    }

    /**
     * 根据数据类型加载歌曲
     */
    private void loadSongsByType() {

        // 创建新线程进行数据查询
        Thread loadThread = new Thread(() -> {
            try {
                List<Song> songs;

                // 根据数据类型查询不同的数据
                switch (dataType) {
                    case DATA_TYPE_FAVORITE:
                        // 我的收藏 - 从FavoriteSong表查询真正的收藏歌曲
                        List<com.magicalstory.music.model.FavoriteSong> favoriteSongList =
                                LitePal.order("addTime desc").find(com.magicalstory.music.model.FavoriteSong.class);
                        songs = new ArrayList<>();
                        if (favoriteSongList != null && !favoriteSongList.isEmpty()) {
                            for (com.magicalstory.music.model.FavoriteSong favoriteSong : favoriteSongList) {
                                // 根据songId查询对应的Song对象
                                Song song = LitePal.find(Song.class, favoriteSong.getSongId());
                                if (song != null) {
                                    songs.add(song);
                                }
                            }
                        }
                        break;
                    case DATA_TYPE_RANDOM:
                        // 随机推荐
                        songs = LitePal.order("random()").find(Song.class);
                        break;
                    case DATA_TYPE_HISTORY:
                        // 播放历史 - 从PlayHistory表查询播放历史，按播放时间倒序
                        List<com.magicalstory.music.model.PlayHistory> playHistoryList =
                                LitePal.order("lastPlayTime desc").find(com.magicalstory.music.model.PlayHistory.class);
                        songs = new ArrayList<>();
                        if (playHistoryList != null && !playHistoryList.isEmpty()) {
                            for (com.magicalstory.music.model.PlayHistory playHistory : playHistoryList) {
                                // 根据songId查询对应的Song对象
                                Song song = LitePal.find(Song.class, playHistory.getSongId());
                                if (song != null) {
                                    songs.add(song);
                                }
                            }
                        }
                        break;
                    case DATA_TYPE_MOST_PLAYED:
                        // 最常播放 - 从PlayHistory表查询播放次数最多的歌曲
                        List<com.magicalstory.music.model.PlayHistory> mostPlayedList =
                                LitePal.order("playCount desc").find(com.magicalstory.music.model.PlayHistory.class);
                        songs = new ArrayList<>();
                        if (mostPlayedList != null && !mostPlayedList.isEmpty()) {
                            for (com.magicalstory.music.model.PlayHistory playHistory : mostPlayedList) {
                                // 根据songId查询对应的Song对象
                                Song song = LitePal.find(Song.class, playHistory.getSongId());
                                if (song != null) {
                                    songs.add(song);
                                }
                            }
                        }
                        break;
                    case DATA_TYPE_ALBUM:
                        // 专辑歌曲 - 根据专辑ID和艺术家查询
                        Bundle arguments = getArguments();
                        if (arguments != null) {
                            long albumId = arguments.getLong("albumId");
                            String artistName = arguments.getString("artistName");
                            songs = LitePal.where("albumId = ? and artist = ?",
                                            String.valueOf(albumId), artistName)
                                    .order("track asc")
                                    .find(Song.class);
                        } else {
                            songs = new ArrayList<>();
                        }
                        break;
                    case DATA_TYPE_ARTIST:
                        // 艺术家歌曲 - 根据艺术家名称查询，按添加时间排序
                        Bundle artistArguments = getArguments();
                        if (artistArguments != null) {
                            String artistName = artistArguments.getString("artistName");
                            songs = LitePal.where("artist = ?", artistName)
                                    .order("dateAdded desc")
                                    .find(Song.class);
                        } else {
                            songs = new ArrayList<>();
                        }
                        break;
                    case DATA_TYPE_RECENT:
                    default:
                        // 最近添加的歌曲，按添加时间倒序排列
                        songs = LitePal.order("dateAdded desc").find(Song.class);
                        break;
                }

                sleep(200);


                // 切换到主线程更新UI
                mainHandler.post(() -> {
                    // 隐藏进度圈
                    binding.progressBar.setVisibility(View.GONE);

                    if (songs != null && !songs.isEmpty()) {
                        songList.clear();
                        songList.addAll(songs);
                        songAdapter.notifyDataSetChanged();

                        // 数据加载完成后更新当前播放歌曲状态
                        updateCurrentPlayingSong();

                        // 显示列表，隐藏空状态
                        binding.rvRecentSongs.setVisibility(View.VISIBLE);
                        binding.layoutEmpty.setVisibility(View.GONE);
                        // 有数据时显示fab
                        binding.fab.setVisibility(View.VISIBLE);
                        // 有数据时显示fab
                        binding.fab.show();
                    } else {
                        // 显示空状态，隐藏列表
                        binding.rvRecentSongs.setVisibility(View.GONE);
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
                    binding.rvRecentSongs.setVisibility(View.GONE);
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
        // 重新加载歌曲列表
        loadSongsByType();
    }

    /**
     * 播放所有歌曲
     */
    private void playAllSongs() {
        if (songList == null || songList.isEmpty()) {
            showSnackbar("没有可播放的歌曲");
            return;
        }

        // 将当前歌曲列表设置为播放列表并播放第一首
        if (getActivity() instanceof MainActivity mainActivity) {
            mainActivity.playFromPlaylist(songList, 0);
        }
    }
}
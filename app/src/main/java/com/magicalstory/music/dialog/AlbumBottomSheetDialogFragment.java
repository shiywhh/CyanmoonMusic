package com.magicalstory.music.dialog;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import androidx.core.content.FileProvider;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.media3.common.util.UnstableApi;

import com.google.android.material.bottomsheet.BottomSheetDialogFragment;
import com.magicalstory.music.MainActivity;
import com.magicalstory.music.R;
import com.magicalstory.music.databinding.BottomSheetAlbumBinding;
import com.magicalstory.music.model.Album;
import com.magicalstory.music.model.Song;
import com.magicalstory.music.player.MediaControllerHelper;
import com.magicalstory.music.utils.app.ToastUtils;
import com.magicalstory.music.utils.file.FileDeleteUtils;
import com.magicalstory.music.utils.glide.GlideUtils;
import com.magicalstory.music.utils.query.MusicQueryUtils;
import com.magicalstory.music.dialog.dialogUtils;

import org.litepal.LitePal;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

/**
 * 专辑底部弹出窗口Fragment
 * 显示专辑的更多操作选项
 */
@UnstableApi
public class AlbumBottomSheetDialogFragment extends BottomSheetDialogFragment {

    private static final String TAG = "AlbumBottomSheetDialogFragment";
    private static final String ARG_ALBUM_ID = "album_id";
    private static final String ARG_ALBUM_NAME = "album_name";
    private static final String ARG_ARTIST = "artist";
    private static final String ARG_SONG_COUNT = "song_count";
    private static final String ARG_ALBUM_ART = "album_art";

    private BottomSheetAlbumBinding binding;
    private Album album;
    private MediaControllerHelper controllerHelper;

    /**
     * 创建新的实例
     */
    public static AlbumBottomSheetDialogFragment newInstance(Album album) {
        AlbumBottomSheetDialogFragment fragment = new AlbumBottomSheetDialogFragment();
        Bundle args = new Bundle();
        args.putLong(ARG_ALBUM_ID, album.getAlbumId());
        args.putString(ARG_ALBUM_NAME, album.getAlbumName());
        args.putString(ARG_ARTIST, album.getArtist());
        args.putInt(ARG_SONG_COUNT, album.getSongCount());
        args.putString(ARG_ALBUM_ART, album.getAlbumArt());
        fragment.setArguments(args);
        return fragment;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        // 使用ViewBinding创建视图
        binding = BottomSheetAlbumBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // 从Bundle中重建专辑对象
        if (getArguments() != null) {
            Bundle args = getArguments();
            album = new Album();
            album.setAlbumId(args.getLong(ARG_ALBUM_ID, 0));
            album.setAlbumName(args.getString(ARG_ALBUM_NAME, ""));
            album.setArtist(args.getString(ARG_ARTIST, ""));
            album.setSongCount(args.getInt(ARG_SONG_COUNT, 0));
            album.setAlbumArt(args.getString(ARG_ALBUM_ART, ""));
        }

        // 初始化MediaControllerHelper
        initControllerHelper();

        // 设置专辑信息
        setupAlbumInfo();

        // 设置点击事件
        setupClickListeners();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        // 处理删除结果
        if (requestCode == FileDeleteUtils.DELETE_REQUEST_CODE) {
            List<Song> songsToDelete = MusicQueryUtils.getSongsByAlbum(album);

            FileDeleteUtils.handleDeleteResult(
                    this,
                    requestCode,
                    resultCode,
                    data,
                    songsToDelete,
                    new FileDeleteUtils.DeleteCallback() {
                        @Override
                        public void onDeleteSuccess(List<Song> deletedSongs) {
                            // 删除成功
                            ToastUtils.showToast(requireContext(), "删除成功");
                            Log.d(TAG, "删除成功，删除歌曲数量: " + deletedSongs.size());
                        }

                        @Override
                        public void onDeleteFailed(String errorMessage) {
                            // 删除失败
                            ToastUtils.showToast(requireContext(), "删除失败: " + errorMessage);
                            Log.e(TAG, "删除失败: " + errorMessage);
                        }
                    }
            );
        }
    }

    /**
     * 初始化MediaControllerHelper
     */
    private void initControllerHelper() {
        controllerHelper = MediaControllerHelper.getInstance();
    }

    /**
     * 设置MediaControllerHelper
     */
    public void setMediaControllerHelper(MediaControllerHelper controllerHelper) {
        this.controllerHelper = controllerHelper;
    }

    /**
     * 设置专辑信息
     */
    private void setupAlbumInfo() {
        if (album == null) {
            Log.w(TAG, "专辑数据为null");
            return;
        }

        // 设置专辑名称
        binding.tvAlbumName.setText(album.getAlbumName());

        // 设置艺术家和歌曲数量信息
        String artistSongCount = album.getArtist();
        if (album.getSongCount() > 0) {
            artistSongCount += " • " + album.getSongCount() + "首歌曲";
        }
        binding.tvAlbumArtist.setText(artistSongCount);

        // 加载专辑封面
        GlideUtils.loadAlbumCover(requireContext(), album.getAlbumId(), binding.ivAlbumCover);
    }

    /**
     * 设置点击事件
     */
    private void setupClickListeners() {
        // 下一首播放
        binding.llPlayNext.setOnClickListener(v -> {
            Log.d(TAG, "下一首播放被点击");
            handlePlayNext();
            dismiss();
        });

        // 添加到播放列表
        binding.llAddToPlaylist.setOnClickListener(v -> {
            Log.d(TAG, "添加到播放列表被点击");
            handleAddToPlaylist();
            dismiss();
        });

        // 添加到歌单
        binding.llAddToPlaylistMenu.setOnClickListener(v -> {
            Log.d(TAG, "添加到歌单被点击");
            handleAddToPlaylistMenu();
            dismiss();
        });

        // 查看艺术家
        binding.llViewArtist.setOnClickListener(v -> {
            Log.d(TAG, "查看艺术家被点击");
            handleViewArtist();
            dismiss();
        });

        // 标签编辑器
        binding.llTagEditor.setOnClickListener(v -> {
            Log.d(TAG, "标签编辑器被点击");
            handleTagEditor();
            dismiss();
        });

        // 分享
        binding.llShare.setOnClickListener(v -> {
            Log.d(TAG, "分享被点击");
            handleShare();
            dismiss();
        });

        // 从设备上删除
        binding.llDelete.setOnClickListener(v -> {
            Log.d(TAG, "从设备上删除被点击");
            handleDelete();
            dismiss();
        });
    }

    /**
     * 处理下一首播放
     */
    private void handlePlayNext() {
        if (album == null) {
            ToastUtils.showToast(requireContext(), "专辑数据为空");
            return;
        }

        if (controllerHelper == null) {
            ToastUtils.showToast(requireContext(), "播放控制器未初始化");
            return;
        }

        // 获取专辑中的所有歌曲
        List<Song> albumSongs = MusicQueryUtils.getSongsByAlbum(album);
        if (albumSongs == null || albumSongs.isEmpty()) {
            ToastUtils.showToast(requireContext(), "专辑中没有歌曲");
            return;
        }

        // 添加到下一首播放
        controllerHelper.addSongsToPlayNext(albumSongs);
        ToastUtils.showToast(requireContext(), "已添加到下一首播放");

        Log.d(TAG, "专辑歌曲已添加到下一首播放: " + album.getAlbumName() + ", 歌曲数量: " + albumSongs.size());
    }

    /**
     * 处理添加到播放列表
     */
    private void handleAddToPlaylist() {
        if (album == null) {
            ToastUtils.showToast(requireContext(), "专辑数据为空");
            return;
        }

        if (controllerHelper == null) {
            ToastUtils.showToast(requireContext(), "播放控制器未初始化");
            return;
        }

        // 获取专辑中的所有歌曲
        List<Song> albumSongs = MusicQueryUtils.getSongsByAlbum(album);
        if (albumSongs == null || albumSongs.isEmpty()) {
            ToastUtils.showToast(requireContext(), "专辑中没有歌曲");
            return;
        }

        // 添加到播放列表末尾
        controllerHelper.addSongsToPlaylist(albumSongs);
        ToastUtils.showToast(requireContext(), "已添加到播放列表");

        Log.d(TAG, "专辑歌曲已添加到播放列表: " + album.getAlbumName() + ", 歌曲数量: " + albumSongs.size());
    }

    /**
     * 处理查看艺术家
     */
    private void handleViewArtist() {
        if (album == null) {
            ToastUtils.showToast(requireContext(), "专辑数据为空");
            return;
        }

        String artistName = album.getArtist();
        if (artistName == null || artistName.isEmpty()) {
            ToastUtils.showToast(requireContext(), "无法获取艺术家信息");
            return;
        }

        // 跳转到艺术家详情页面
        if (getActivity() instanceof MainActivity mainActivity) {
            mainActivity.navigateToArtistDetail(artistName);
        } else {
            ToastUtils.showToast(requireContext(), "无法跳转到艺术家详情");
        }

        Log.d(TAG, "跳转到艺术家详情: " + artistName);
    }

    /**
     * 处理标签编辑器
     */
    private void handleTagEditor() {
        if (album == null) {
            ToastUtils.showToast(requireContext(), "专辑数据为空");
            return;
        }

        // 获取专辑中的第一首歌曲用于标签编辑
        List<Song> albumSongs = MusicQueryUtils.getSongsByAlbum(album);
        if (albumSongs == null || albumSongs.isEmpty()) {
            ToastUtils.showToast(requireContext(), "专辑中没有歌曲");
            return;
        }

        // 跳转到歌曲标签编辑器（使用第一首歌曲）
        if (getActivity() instanceof MainActivity mainActivity) {
            mainActivity.navigateToSongTagEditor(albumSongs.get(0));
        } else {
            ToastUtils.showToast(requireContext(), "无法跳转到标签编辑器");
        }

        Log.d(TAG, "跳转到歌曲标签编辑器: " + album.getAlbumName());
    }

    /**
     * 处理分享
     */
    private void handleShare() {
        if (album == null) {
            ToastUtils.showToast(requireContext(), "专辑数据为空");
            return;
        }

        // 获取专辑中的所有歌曲
        List<Song> albumSongs = MusicQueryUtils.getSongsByAlbum(album);
        if (albumSongs == null || albumSongs.isEmpty()) {
            ToastUtils.showToast(requireContext(), "专辑中没有歌曲");
            return;
        }

        try {
            // 创建分享意图
            Intent shareIntent = new Intent(Intent.ACTION_SEND_MULTIPLE);
            shareIntent.setType("audio/*");

            // 准备要分享的文件URI列表
            ArrayList<Uri> songUris = new ArrayList<>();

            for (Song song : albumSongs) {
                String filePath = song.getPath();
                if (filePath != null && !filePath.isEmpty()) {
                    File songFile = new File(filePath);
                    if (songFile.exists()) {
                        // 使用FileProvider获取文件的URI
                        Uri songUri = androidx.core.content.FileProvider.getUriForFile(
                                requireContext(),
                                requireContext().getPackageName() + ".fileprovider",
                                songFile
                        );
                        songUris.add(songUri);
                    }
                }
            }

            if (songUris.isEmpty()) {
                ToastUtils.showToast(requireContext(), "没有可分享的歌曲文件");
                return;
            }

            // 设置分享的文件
            shareIntent.putParcelableArrayListExtra(Intent.EXTRA_STREAM, songUris);

            // 设置分享标题
            String shareTitle = "分享专辑: " + album.getAlbumName();
            if (album.getArtist() != null && !album.getArtist().isEmpty()) {
                shareTitle += " - " + album.getArtist();
            }
            shareIntent.putExtra(Intent.EXTRA_SUBJECT, shareTitle);

            // 启动分享
            startActivity(Intent.createChooser(shareIntent, "分享专辑歌曲"));

            Log.d(TAG, "分享专辑歌曲: " + album.getAlbumName() + ", 歌曲数量: " + songUris.size());

        } catch (Exception e) {
            Log.e(TAG, "分享专辑歌曲时发生错误", e);
            ToastUtils.showToast(requireContext(), "分享失败: " + e.getMessage());
        }
    }

    /**
     * 处理添加到歌单
     */
    private void handleAddToPlaylistMenu() {
        if (album == null) {
            ToastUtils.showToast(requireContext(), "专辑数据为空");
            return;
        }

        // 获取专辑中的所有歌曲
        List<Song> albumSongs = MusicQueryUtils.getSongsByAlbum(album);
        if (albumSongs == null || albumSongs.isEmpty()) {
            ToastUtils.showToast(requireContext(), "专辑中没有歌曲");
            return;
        }

        // 使用PlaylistAddUtils显示歌单选择对话框
        com.magicalstory.music.utils.playlist.PlaylistAddUtils.showPlaylistSelectorDialog(requireContext(), albumSongs);

        Log.d(TAG, "显示歌单选择对话框: " + album.getAlbumName() + ", 歌曲数量: " + albumSongs.size());
    }

    /**
     * 处理从设备上删除
     */
    private void handleDelete() {
        if (album == null) {
            ToastUtils.showToast(requireContext(), "专辑数据为空");
            return;
        }

        // 获取专辑中的所有歌曲
        List<Song> albumSongs = MusicQueryUtils.getSongsByAlbum(album);

        if (albumSongs == null || albumSongs.isEmpty()) {
            // 如果没有歌曲，直接删除专辑记录
            ToastUtils.showToast(requireContext(), "专辑中没有歌曲");
            return;
        }

        // 使用dialogUtils显示确认对话框
        dialogUtils.showAlertDialog(
                requireContext(),
                "确认删除",
                "确定要从设备上删除专辑 \"" + album.getAlbumName() + "\" 及其 " + albumSongs.size() + " 首歌曲吗？\n\n此操作将永久删除文件，无法恢复。",
                "删除",
                "取消",
                null,
                true,
                new dialogUtils.onclick_with_dismiss() {
                    @Override
                    public void click_confirm() {
                        performDeleteFromDevice(albumSongs);
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

        Log.d(TAG, "显示删除确认对话框: " + album.getAlbumName());
    }

    /**
     * 执行从设备删除操作
     */
    private void performDeleteFromDevice(List<Song> songsToDelete) {
        FileDeleteUtils.deleteSongsWithMediaStore(
                this,
                songsToDelete,
                new FileDeleteUtils.DeleteCallback() {
                    @Override
                    public void onDeleteSuccess(List<Song> deletedSongs) {
                        // 删除成功
                        ToastUtils.showToast(requireContext(), "删除成功");
                        Log.d(TAG, "删除成功，删除歌曲数量: " + deletedSongs.size());
                    }

                    @Override
                    public void onDeleteFailed(String errorMessage) {
                        // 删除失败
                        ToastUtils.showToast(requireContext(), "删除失败: " + errorMessage);
                        Log.e(TAG, "删除失败: " + errorMessage);
                    }
                }
        );
    }
} 
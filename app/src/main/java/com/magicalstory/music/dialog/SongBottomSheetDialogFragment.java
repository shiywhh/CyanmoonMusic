package com.magicalstory.music.dialog;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.media3.common.util.UnstableApi;

import com.google.android.material.bottomsheet.BottomSheetDialogFragment;
import com.magicalstory.music.MainActivity;
import com.magicalstory.music.R;
import com.magicalstory.music.databinding.BottomSheetSongBinding;
import com.magicalstory.music.model.Song;
import com.magicalstory.music.player.MediaControllerHelper;
import com.magicalstory.music.utils.app.ToastUtils;
import com.magicalstory.music.utils.file.FileDeleteUtils;
import com.magicalstory.music.utils.glide.GlideUtils;
import com.magicalstory.music.utils.text.TimeUtils;
import com.magicalstory.music.dialog.dialogUtils;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

/**
 * 歌曲底部弹出窗口Fragment
 * 显示歌曲的更多操作选项
 */
@UnstableApi
public class SongBottomSheetDialogFragment extends BottomSheetDialogFragment {

    private static final String TAG = "SongBottomSheetDialogFragment";
    private static final String ARG_SONG = "song";

    private BottomSheetSongBinding binding;
    private Song song;
    private MediaControllerHelper controllerHelper;

    /**
     * 创建新的实例
     */
    public static SongBottomSheetDialogFragment newInstance(Song song) {
        SongBottomSheetDialogFragment fragment = new SongBottomSheetDialogFragment();
        Bundle args = new Bundle();
        args.putSerializable(ARG_SONG, song);
        fragment.setArguments(args);
        return fragment;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        // 使用ViewBinding创建视图
        binding = BottomSheetSongBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // 获取传入的歌曲数据
        if (getArguments() != null) {
            song = (Song) getArguments().getSerializable(ARG_SONG);
        }

        // 初始化MediaControllerHelper
        initControllerHelper();

        // 设置歌曲信息
        setupSongInfo();

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
            List<Song> songsToDelete = new ArrayList<>();
            if (song != null) {
                songsToDelete.add(song);
            }
            
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
     * 设置歌曲信息
     */
    private void setupSongInfo() {
        if (song == null) {
            Log.w(TAG, "歌曲数据为null");
            return;
        }

        // 设置歌曲标题
        binding.tvSongTitle.setText(song.getTitle());

        // 设置艺术家和专辑信息
        String artistAlbum = song.getArtist();
        if (song.getAlbum() != null && !song.getAlbum().isEmpty()) {
            artistAlbum += " • " + song.getAlbum();
        }
        binding.tvSongArtist.setText(artistAlbum);

        // 加载专辑封面
        GlideUtils.loadAlbumCover(requireContext(), song.getAlbumId(), binding.ivAlbumCover);
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

        // 查看专辑
        binding.llViewAlbum.setOnClickListener(v -> {
            Log.d(TAG, "查看专辑被点击");
            handleViewAlbum();
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

        // 编辑歌词
        binding.llEditLyrics.setOnClickListener(v -> {
            Log.d(TAG, "编辑歌词被点击");
            handleEditLyrics();
            dismiss();
        });

        // 详细信息
        binding.llDetails.setOnClickListener(v -> {
            Log.d(TAG, "详细信息被点击");
            handleDetails();
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
        if (song == null) {
            ToastUtils.showToast(requireContext(), "歌曲数据为空");
            return;
        }

        if (controllerHelper == null) {
            ToastUtils.showToast(requireContext(), "播放控制器未初始化");
            return;
        }

        // 创建包含当前歌曲的列表
        List<Song> songsToAdd = new ArrayList<>();
        songsToAdd.add(song);

        // 添加到下一首播放
        controllerHelper.addSongsToPlayNext(songsToAdd);
        ToastUtils.showToast(requireContext(), "已添加到下一首播放");
        
        Log.d(TAG, "歌曲已添加到下一首播放: " + song.getTitle());
    }

    /**
     * 处理添加到播放列表
     */
    private void handleAddToPlaylist() {
        if (song == null) {
            ToastUtils.showToast(requireContext(), "歌曲数据为空");
            return;
        }

        if (controllerHelper == null) {
            ToastUtils.showToast(requireContext(), "播放控制器未初始化");
            return;
        }

        // 创建包含当前歌曲的列表
        List<Song> songsToAdd = new ArrayList<>();
        songsToAdd.add(song);

        // 添加到播放列表末尾
        controllerHelper.addSongsToPlaylist(songsToAdd);
        ToastUtils.showToast(requireContext(), "已添加到播放列表");
        
        Log.d(TAG, "歌曲已添加到播放列表: " + song.getTitle());
    }

    /**
     * 处理查看专辑
     */
    private void handleViewAlbum() {
        if (song == null) {
            ToastUtils.showToast(requireContext(), "歌曲数据为空");
            return;
        }

        String albumName = song.getAlbum();
        String artistName = song.getArtist();
        long albumId = song.getAlbumId();
        
        if (albumName == null || albumName.isEmpty()) {
            ToastUtils.showToast(requireContext(), "无法获取专辑信息");
            return;
        }
        
        if (artistName == null || artistName.isEmpty()) {
            ToastUtils.showToast(requireContext(), "无法获取艺术家信息");
            return;
        }

        // 跳转到专辑详情页面
        if (getActivity() instanceof MainActivity mainActivity) {
            mainActivity.navigateToAlbumDetail(albumName, artistName, albumId);
        } else {
            ToastUtils.showToast(requireContext(), "无法跳转到专辑详情");
        }
        
        Log.d(TAG, "跳转到专辑详情: " + albumName + ", 艺术家: " + artistName + ", 专辑ID: " + albumId);
    }

    /**
     * 处理查看艺术家
     */
    private void handleViewArtist() {
        if (song == null) {
            ToastUtils.showToast(requireContext(), "歌曲数据为空");
            return;
        }

        String artistName = song.getArtist();
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
        if (song == null) {
            ToastUtils.showToast(requireContext(), "歌曲数据为空");
            return;
        }

        // 跳转到歌曲标签编辑器
        if (getActivity() instanceof MainActivity mainActivity) {
            mainActivity.navigateToSongTagEditor(song);
        } else {
            ToastUtils.showToast(requireContext(), "无法跳转到标签编辑器");
        }
        
        Log.d(TAG, "跳转到歌曲标签编辑器: " + song.getTitle());
    }

    /**
     * 处理编辑歌词
     */
    private void handleEditLyrics() {
        // 暂时不实现，显示提示信息
        ToastUtils.showToast(requireContext(), "编辑歌词功能暂未实现");
        Log.d(TAG, "编辑歌词功能暂未实现");
    }

    /**
     * 处理详细信息
     */
    private void handleDetails() {
        if (song == null) {
            ToastUtils.showToast(requireContext(), "歌曲数据为空");
            return;
        }

        // 构建详细信息文本
        StringBuilder details = new StringBuilder();
        details.append("标题: ").append(song.getTitle()).append("\n\n");
        
        if (song.getArtist() != null && !song.getArtist().isEmpty()) {
            details.append("艺术家: ").append(song.getArtist()).append("\n\n");
        }
        
        if (song.getAlbum() != null && !song.getAlbum().isEmpty()) {
            details.append("专辑: ").append(song.getAlbum()).append("\n\n");
        }
        
        if (song.getPath() != null && !song.getPath().isEmpty()) {
            details.append("路径: ").append(song.getPath()).append("\n\n");
        }
        
        if (song.getDuration() > 0) {
            details.append("时长: ").append(TimeUtils.formatDuration(song.getDuration())).append("\n\n");
        }
        
        if (song.getSize() > 0) {
            details.append("文件大小: ").append(formatFileSize(song.getSize())).append("\n\n");
        }

        // 使用dialogUtils显示详细信息对话框
        dialogUtils.showAlertDialog(
                requireContext(),
                "歌曲详细信息",
                details.toString(),
                "确定",
                null,
                null,
                true,
                new dialogUtils.onclick_with_dismiss() {
                    @Override
                    public void click_confirm() {
                        // 用户点击确定
                    }

                    @Override
                    public void click_cancel() {
                        // 不使用取消按钮
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
        
        Log.d(TAG, "显示歌曲详细信息: " + song.getTitle());
    }

    /**
     * 处理分享
     */
    private void handleShare() {
        if (song == null) {
            ToastUtils.showToast(requireContext(), "歌曲数据为空");
            return;
        }

        try {
            // 创建分享意图
            Intent shareIntent = new Intent(Intent.ACTION_SEND);
            shareIntent.setType("audio/*");
            
            // 设置分享内容
            String shareText = "分享歌曲: " + song.getTitle();
            if (song.getArtist() != null && !song.getArtist().isEmpty()) {
                shareText += " - " + song.getArtist();
            }
            
            shareIntent.putExtra(Intent.EXTRA_TEXT, shareText);
            
            // 如果有文件路径，添加文件URI
            if (song.getPath() != null && !song.getPath().isEmpty()) {
                File songFile = new File(song.getPath());
                if (songFile.exists()) {
                    Uri fileUri = androidx.core.content.FileProvider.getUriForFile(
                            requireContext(),
                            requireContext().getPackageName() + ".fileprovider",
                            songFile
                    );
                    shareIntent.putExtra(Intent.EXTRA_STREAM, fileUri);
                    shareIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
                }
            }
            
            // 启动分享
            startActivity(Intent.createChooser(shareIntent, "分享歌曲"));
            
            Log.d(TAG, "分享歌曲: " + song.getTitle());
            
        } catch (Exception e) {
            Log.e(TAG, "分享歌曲时发生错误", e);
            ToastUtils.showToast(requireContext(), "分享失败: " + e.getMessage());
        }
    }

    /**
     * 处理从设备上删除
     */
    private void handleDelete() {
        if (song == null) {
            ToastUtils.showToast(requireContext(), "歌曲数据为空");
            return;
        }

        // 创建包含当前歌曲的列表
        List<Song> songsToDelete = new ArrayList<>();
        songsToDelete.add(song);

        // 使用dialogUtils显示确认对话框
        dialogUtils.showAlertDialog(
                requireContext(),
                "确认删除",
                "确定要从设备上删除歌曲 \"" + song.getTitle() + "\" 吗？\n\n此操作将永久删除文件，无法恢复。",
                "删除",
                "取消",
                null,
                true,
                new dialogUtils.onclick_with_dismiss() {
                    @Override
                    public void click_confirm() {
                        performDeleteFromDevice(songsToDelete);
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
        
        Log.d(TAG, "显示删除确认对话框: " + song.getTitle());
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

    /**
     * 格式化文件大小
     */
    private String formatFileSize(long sizeBytes) {
        if (sizeBytes < 1024) {
            return sizeBytes + " B";
        } else if (sizeBytes < 1024 * 1024) {
            return String.format("%.1f KB", sizeBytes / 1024.0);
        } else {
            return String.format("%.1f MB", sizeBytes / (1024.0 * 1024.0));
        }
    }
} 
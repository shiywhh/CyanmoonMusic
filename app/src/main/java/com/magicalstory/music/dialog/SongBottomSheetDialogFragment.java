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
import com.magicalstory.music.utils.glide.GlideUtils;

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

    // 回调接口
    public interface OnSongActionListener {
        /**
         * 下一首播放
         */
        void onPlayNext(Song song);

        /**
         * 添加到播放列表
         */
        void onAddToPlaylist(Song song);

        /**
         * 查看专辑
         */
        void onViewAlbum(Song song);

        /**
         * 查看艺术家
         */
        void onViewArtist(Song song);

        /**
         * 标签编辑器
         */
        void onTagEditor(Song song);

        /**
         * 编辑歌词
         */
        void onEditLyrics(Song song);

        /**
         * 详细信息
         */
        void onDetails(Song song);

        /**
         * 分享
         */
        void onShare(Song song);

        /**
         * 从设备上删除
         */
        void onDelete(Song song);
    }

    private OnSongActionListener actionListener;

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
     * 设置歌曲操作监听器
     */
    public void setOnSongActionListener(OnSongActionListener listener) {
        this.actionListener = listener;
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
            if (actionListener != null) {
                actionListener.onPlayNext(song);
            } else {
                handlePlayNext();
            }
            dismiss();
        });

        // 添加到播放列表
        binding.llAddToPlaylist.setOnClickListener(v -> {
            Log.d(TAG, "添加到播放列表被点击");
            if (actionListener != null) {
                actionListener.onAddToPlaylist(song);
            } else {
                handleAddToPlaylist();
            }
            dismiss();
        });

        // 查看专辑
        binding.llViewAlbum.setOnClickListener(v -> {
            Log.d(TAG, "查看专辑被点击");
            if (actionListener != null) {
                actionListener.onViewAlbum(song);
            } else {
                handleViewAlbum();
            }
            dismiss();
        });

        // 查看艺术家
        binding.llViewArtist.setOnClickListener(v -> {
            Log.d(TAG, "查看艺术家被点击");
            if (actionListener != null) {
                actionListener.onViewArtist(song);
            } else {
                handleViewArtist();
            }
            dismiss();
        });

        // 标签编辑器
        binding.llTagEditor.setOnClickListener(v -> {
            Log.d(TAG, "标签编辑器被点击");
            if (actionListener != null) {
                actionListener.onTagEditor(song);
            } else {
                handleTagEditor();
            }
            dismiss();
        });

        // 编辑歌词
        binding.llEditLyrics.setOnClickListener(v -> {
            Log.d(TAG, "编辑歌词被点击");
            if (actionListener != null) {
                actionListener.onEditLyrics(song);
            } else {
                handleEditLyrics();
            }
            dismiss();
        });

        // 详细信息
        binding.llDetails.setOnClickListener(v -> {
            Log.d(TAG, "详细信息被点击");
            if (actionListener != null) {
                actionListener.onDetails(song);
            } else {
                handleDetails();
            }
            dismiss();
        });

        // 分享
        binding.llShare.setOnClickListener(v -> {
            Log.d(TAG, "分享被点击");
            if (actionListener != null) {
                actionListener.onShare(song);
            } else {
                handleShare();
            }
            dismiss();
        });

        // 从设备上删除
        binding.llDelete.setOnClickListener(v -> {
            Log.d(TAG, "从设备上删除被点击");
            if (actionListener != null) {
                actionListener.onDelete(song);
            } else {
                handleDelete();
            }
            dismiss();
        });
    }

    // 默认处理方法的实现
    private void handlePlayNext() {
        if (controllerHelper != null) {
            List<Song> songs = new ArrayList<>();
            songs.add(song);
            controllerHelper.addSongsToPlayNext(songs);
            ToastUtils.showToast(requireContext(), "已添加到下一首播放");
        }
    }

    private void handleAddToPlaylist() {
        ToastUtils.showToast(requireContext(), "添加到播放列表功能开发中");
    }

    private void handleViewAlbum() {
        ToastUtils.showToast(requireContext(), "查看专辑功能开发中");
    }

    private void handleViewArtist() {
        if (song.getArtist() != null && !song.getArtist().isEmpty()) {
            if (getActivity() instanceof MainActivity mainActivity) {
                mainActivity.navigateToArtistDetail(song.getArtist());
            }
        } else {
            ToastUtils.showToast(requireContext(), "无法获取艺术家信息");
        }
    }

    private void handleTagEditor() {
        ToastUtils.showToast(requireContext(), "标签编辑器功能开发中");
    }

    private void handleEditLyrics() {
        ToastUtils.showToast(requireContext(), "编辑歌词功能开发中");
    }

    private void handleDetails() {
        ToastUtils.showToast(requireContext(), "详细信息功能开发中");
    }

    private void handleShare() {
        try {
            Intent shareIntent = new Intent(Intent.ACTION_SEND);
            shareIntent.setType("audio/*");
            shareIntent.putExtra(Intent.EXTRA_STREAM, Uri.parse("file://" + song.getPath()));
            shareIntent.putExtra(Intent.EXTRA_SUBJECT, song.getTitle());
            shareIntent.putExtra(Intent.EXTRA_TEXT, song.getTitle() + " - " + song.getArtist());
            startActivity(Intent.createChooser(shareIntent, "分享音乐"));
        } catch (Exception e) {
            Log.e(TAG, "分享失败", e);
            ToastUtils.showToast(requireContext(), "分享失败");
        }
    }

    private void handleDelete() {
        // 显示确认删除对话框
        new androidx.appcompat.app.AlertDialog.Builder(requireContext())
                .setTitle("确认删除")
                .setMessage("确定要从设备上删除这首歌曲吗？此操作不可撤销。")
                .setPositiveButton("删除", (dialog, which) -> {
                    try {
                        File file = new File(song.getPath());
                        if (file.exists() && file.delete()) {
                            ToastUtils.showToast(requireContext(), "删除成功");
                            // 这里可以通知数据库更新
                        } else {
                            ToastUtils.showToast(requireContext(), "删除失败");
                        }
                    } catch (Exception e) {
                        Log.e(TAG, "删除文件失败", e);
                        ToastUtils.showToast(requireContext(), "删除失败");
                    }
                })
                .setNegativeButton("取消", null)
                .show();
    }
} 
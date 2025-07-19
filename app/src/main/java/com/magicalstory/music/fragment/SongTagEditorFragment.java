package com.magicalstory.music.fragment;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.media3.common.util.UnstableApi;
import androidx.navigation.Navigation;

import com.bumptech.glide.Glide;
import com.bumptech.glide.request.target.CustomTarget;
import com.bumptech.glide.request.transition.Transition;
import com.magicalstory.music.R;
import com.magicalstory.music.base.BaseFragment;
import com.magicalstory.music.databinding.FragmentSongTagEditorBinding;
import com.magicalstory.music.model.Album;
import com.magicalstory.music.model.Song;
import com.magicalstory.music.utils.VersionUtils;
import com.magicalstory.music.utils.app.ToastUtils;
import com.magicalstory.music.utils.glide.GlideUtils;
import com.magicalstory.music.utils.tag.TagWriter;

import org.jaudiotagger.audio.AudioFile;
import org.jaudiotagger.audio.AudioFileIO;
import org.jaudiotagger.audio.exceptions.CannotReadException;
import org.jaudiotagger.audio.exceptions.CannotWriteException;
import org.jaudiotagger.audio.exceptions.InvalidAudioFrameException;
import org.jaudiotagger.audio.exceptions.ReadOnlyFileException;
import org.jaudiotagger.tag.FieldDataInvalidException;
import org.jaudiotagger.tag.FieldKey;
import org.jaudiotagger.tag.Tag;
import org.jaudiotagger.tag.images.AndroidArtwork;
import org.jaudiotagger.tag.images.Artwork;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * 歌曲标签编辑器Fragment
 * 用于编辑单首歌曲的标签信息
 */
@UnstableApi
public class SongTagEditorFragment extends BaseFragment<FragmentSongTagEditorBinding> {

    private static final String TAG = "SongTagEditorFragment";
    private static final String ARG_SONG = "song";

    private Song currentSong;
    private ExecutorService executorService;
    private Handler mainHandler;
    private Uri selectedImageUri;


    private ActivityResultLauncher<Intent> imagePickerLauncher;
    private ActivityResultLauncher<androidx.activity.result.IntentSenderRequest> writeRequestLauncher;
    private boolean hasCustomCover = false; // 标记是否有自定义封面

    public static SongTagEditorFragment newInstance(Song song) {
        SongTagEditorFragment fragment = new SongTagEditorFragment();
        Bundle args = new Bundle();
        args.putSerializable(ARG_SONG, song);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    protected FragmentSongTagEditorBinding getViewBinding(LayoutInflater inflater, ViewGroup container) {
        return FragmentSongTagEditorBinding.inflate(inflater, container, false);
    }

    @Override
    protected boolean usePersistentView() {
        return true;
    }

    @Override
    protected int getLayoutRes() {
        return R.layout.fragment_song_tag_editor;
    }

    @Override
    protected FragmentSongTagEditorBinding bindPersistentView(View view) {
        return FragmentSongTagEditorBinding.bind(view);
    }

    @Override
    protected void initViewForPersistentView() {
        super.initViewForPersistentView();
        // 初始化Handler和线程池
        mainHandler = new Handler(Looper.getMainLooper());
        executorService = Executors.newCachedThreadPool();

        // 初始化图片选择器
        initImagePicker();

        // 获取传递的歌曲数据
        Bundle args = getArguments();
        if (args != null) {
            currentSong = (Song) args.getSerializable(ARG_SONG);
        }

        if (currentSong == null) {
            ToastUtils.showToast(getContext(), "歌曲数据为空");
            Navigation.findNavController(requireView()).popBackStack();
            return;
        }

        // 加载歌曲数据
        loadSongData();
    }

    @Override
    protected void initListenerForPersistentView() {
        super.initListenerForPersistentView();

        // 返回按钮
        binding.toolbar.setNavigationOnClickListener(v -> {
            Navigation.findNavController(requireView()).popBackStack();
        });

        // 歌曲封面点击选择图片
        binding.ivSongCover.setOnClickListener(v -> {
            openImagePicker();
        });

        // 更换封面按钮
        binding.addCover.setOnClickListener(v -> {
            openImagePicker();
        });

        // 移除封面按钮
        binding.removeCover.setOnClickListener(v -> {
            removeSongCover();
        });

        // 保存按钮
        binding.buttonSave.setOnClickListener(v -> {
            saveTags();
        });
    }

    /**
     * 初始化图片选择器
     */
    private void initImagePicker() {
        imagePickerLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == Activity.RESULT_OK && result.getData() != null) {
                        selectedImageUri = result.getData().getData();
                        if (selectedImageUri != null) {
                            loadSelectedImage();
                            hasCustomCover = true;
                            updateCoverButtons();
                        }
                    }
                }
        );

        // 初始化MediaStore写入请求启动器
        writeRequestLauncher = registerForActivityResult(
                new ActivityResultContracts.StartIntentSenderForResult(),
                result -> {
                    if (result.getResultCode() == Activity.RESULT_OK) {
                        // 写入成功，执行文件替换
                        performFileReplacement();
                    } else {
                        // 写入失败
                        ToastUtils.showToast(getContext(), getString(R.string.save_tags_failed));
                    }
                }
        );
    }

    /**
     * 更新封面按钮状态
     */
    private void updateCoverButtons() {
        binding.removeCover.setEnabled(hasCustomCover);
    }

    /**
     * 移除歌曲封面
     */
    private void removeSongCover() {
        // 清除选中的图片
        selectedImageUri = null;
        hasCustomCover = false;

        // 重新加载默认封面
        loadSongCover();

        // 更新按钮状态
        updateCoverButtons();

        ToastUtils.showToast(getContext(), getString(R.string.cover_removed));
    }

    /**
     * 加载歌曲数据
     */
    private void loadSongData() {
        executorService.execute(() -> {
            try {
                // 读取音频文件的标签信息
                loadAudioTags();

                // 在主线程更新UI
                mainHandler.post(() -> {
                    updateUI();
                    loadSongCover();
                    updateCoverButtons();
                });

            } catch (Exception e) {
                Log.e(TAG, "加载歌曲数据失败", e);
                mainHandler.post(() -> {
                    ToastUtils.showToast(getContext(), "加载歌曲数据失败");
                });
            }
        });
    }

    /**
     * 读取音频文件的标签信息
     */
    private void loadAudioTags() {
        if (currentSong == null || currentSong.getPath() == null) {
            return;
        }

        try {
            File audioFile = new File(currentSong.getPath());
            if (!audioFile.exists()) {
                Log.w(TAG, "音频文件不存在: " + currentSong.getPath());
                return;
            }

            AudioFile file = AudioFileIO.read(audioFile);
            Tag tag = file.getTag();

            if (tag != null) {
                // 读取标签信息并设置到输入框
                mainHandler.post(() -> {
                    binding.etTitle.setText(tag.getFirst(FieldKey.TITLE));
                    binding.etArtist.setText(tag.getFirst(FieldKey.ARTIST));
                    binding.etAlbum.setText(tag.getFirst(FieldKey.ALBUM));
                    binding.etAlbumArtist.setText(tag.getFirst(FieldKey.ALBUM_ARTIST));
                    binding.etComposer.setText(tag.getFirst(FieldKey.COMPOSER));
                    binding.etGenre.setText(tag.getFirst(FieldKey.GENRE));
                    binding.etYear.setText(tag.getFirst(FieldKey.YEAR));
                    binding.etTrackNumber.setText(tag.getFirst(FieldKey.TRACK));
                    binding.etDiscNumber.setText(tag.getFirst(FieldKey.DISC_NO));
                    binding.etTrackTotal.setText(tag.getFirst(FieldKey.TRACK_TOTAL));
                    binding.etDiscTotal.setText(tag.getFirst(FieldKey.DISC_TOTAL));
                });
            }

        } catch (CannotReadException | InvalidAudioFrameException e) {
            Log.e(TAG, "读取音频文件标签失败", e);
        } catch (Exception e) {
            Log.e(TAG, "读取音频文件失败", e);
        }
    }

    /**
     * 更新UI
     */
    private void updateUI() {
        if (currentSong == null) {
            return;
        }

        // 设置歌曲标题
        binding.tvSongTitle.setText(currentSong.getTitle());

        // 设置艺术家和专辑信息
        String artistAlbum = currentSong.getArtist();
        if (currentSong.getAlbum() != null && !currentSong.getAlbum().isEmpty()) {
            artistAlbum += " • " + currentSong.getAlbum();
        }
        binding.tvSongArtist.setText(artistAlbum);

        // 设置输入框的默认值
        if (TextUtils.isEmpty(binding.etTitle.getText())) {
            binding.etTitle.setText(currentSong.getTitle());
        }
        if (TextUtils.isEmpty(binding.etArtist.getText())) {
            binding.etArtist.setText(currentSong.getArtist());
        }
        if (TextUtils.isEmpty(binding.etAlbum.getText())) {
            binding.etAlbum.setText(currentSong.getAlbum());
        }
        if (TextUtils.isEmpty(binding.etGenre.getText())) {
            binding.etGenre.setText(currentSong.getGenre());
        }
        if (TextUtils.isEmpty(binding.etYear.getText()) && currentSong.getYear() > 0) {
            binding.etYear.setText(String.valueOf(currentSong.getYear()));
        }
        if (TextUtils.isEmpty(binding.etTrackNumber.getText()) && currentSong.getTrack() > 0) {
            binding.etTrackNumber.setText(String.valueOf(currentSong.getTrack()));
        }
    }

    /**
     * 加载歌曲封面
     */
    private void loadSongCover() {
        if (currentSong == null) {
            return;
        }

        // 加载专辑封面
        GlideUtils.loadAlbumCover(requireContext(), currentSong.getAlbumId(), binding.ivSongCover);
    }

    /**
     * 打开图片选择器
     */
    private void openImagePicker() {
        Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
        intent.setType("image/*");
        intent.addCategory(Intent.CATEGORY_OPENABLE);

        try {
            imagePickerLauncher.launch(Intent.createChooser(intent, getString(R.string.select_image)));
        } catch (Exception e) {
            Log.e(TAG, "打开图片选择器失败", e);
            ToastUtils.showToast(getContext(), getString(R.string.permission_required));
        }
    }

    /**
     * 加载选中的图片
     */
    private void loadSelectedImage() {
        if (selectedImageUri == null) {
            return;
        }

        Glide.with(requireContext())
                .asBitmap()
                .load(selectedImageUri)
                .into(new CustomTarget<Bitmap>() {
                    @Override
                    public void onResourceReady(@NonNull Bitmap resource, @Nullable Transition<? super Bitmap> transition) {
                        binding.ivSongCover.setImageBitmap(resource);
                    }

                    @Override
                    public void onLoadCleared(@Nullable Drawable placeholder) {
                        // 清除加载
                    }
                });
    }

    /**
     * 保存标签
     */
    private void saveTags() {
        if (currentSong == null) {
            ToastUtils.showToast(getContext(), "歌曲数据为空");
            return;
        }

        // 收集表单数据
        String title = binding.etTitle.getText().toString().trim();
        String artist = binding.etArtist.getText().toString().trim();
        String album = binding.etAlbum.getText().toString().trim();
        String albumArtist = binding.etAlbumArtist.getText().toString().trim();
        String composer = binding.etComposer.getText().toString().trim();
        String genre = binding.etGenre.getText().toString().trim();
        String year = binding.etYear.getText().toString().trim();
        String trackNumber = binding.etTrackNumber.getText().toString().trim();
        String discNumber = binding.etDiscNumber.getText().toString().trim();
        String trackTotal = binding.etTrackTotal.getText().toString().trim();
        String discTotal = binding.etDiscTotal.getText().toString().trim();

        // 验证必填字段
        if (TextUtils.isEmpty(title)) {
            ToastUtils.showToast(getContext(), "标题不能为空");
            return;
        }

        // 构建标签映射
        Map<FieldKey, String> fieldKeyValueMap = new HashMap<>();
        fieldKeyValueMap.put(FieldKey.TITLE, title);
        fieldKeyValueMap.put(FieldKey.ARTIST, artist);
        fieldKeyValueMap.put(FieldKey.ALBUM, album);
        fieldKeyValueMap.put(FieldKey.ALBUM_ARTIST, albumArtist);
        fieldKeyValueMap.put(FieldKey.COMPOSER, composer);
        fieldKeyValueMap.put(FieldKey.GENRE, genre);
        fieldKeyValueMap.put(FieldKey.YEAR, year);
        fieldKeyValueMap.put(FieldKey.TRACK, trackNumber);
        fieldKeyValueMap.put(FieldKey.DISC_NO, discNumber);
        fieldKeyValueMap.put(FieldKey.TRACK_TOTAL, trackTotal);
        fieldKeyValueMap.put(FieldKey.DISC_TOTAL, discTotal);

        // 处理封面
        TagWriter.ArtworkInfo artworkInfo = null;
        if (hasCustomCover && selectedImageUri != null) {
            try {
                Bitmap bitmap = Glide.with(requireContext())
                        .asBitmap()
                        .load(selectedImageUri)
                        .submit()
                        .get();
                artworkInfo = new TagWriter.ArtworkInfo(currentSong.getAlbumId(), bitmap);
            } catch (Exception e) {
                Log.e(TAG, "处理封面失败", e);
            }
        } else if (!hasCustomCover) {
            // 移除封面
            artworkInfo = new TagWriter.ArtworkInfo(currentSong.getAlbumId(), null);
        }

        // 创建文件路径列表
        List<String> filePaths = new ArrayList<>();
        filePaths.add(currentSong.getPath());

        // 创建标签信息对象
        TagWriter.AudioTagInfo audioTagInfo = new TagWriter.AudioTagInfo(
                filePaths,
                fieldKeyValueMap,
                artworkInfo
        );

        // 根据Android版本选择写入方法
        if (VersionUtils.hasR()) {
            // Android 11及以上版本
            List<File> cacheFiles = TagWriter.writeTagsToFilesR(requireContext(), audioTagInfo);
            if (!cacheFiles.isEmpty()) {
                // 处理缓存文件
                handleCacheFiles(cacheFiles);
            } else {
                ToastUtils.showToast(getContext(), getString(R.string.save_tags_failed));
            }
        } else {
            // Android 10及以下版本
            executorService.execute(() -> {
                try {
                    TagWriter.writeTagsToFiles(requireContext(), audioTagInfo);
                    mainHandler.post(() -> {
                        // 通知所有fragment刷新
                        notifyAllFragmentsRefresh();
                        ToastUtils.showToast(getContext(), getString(R.string.tags_saved_successfully));
                        Navigation.findNavController(requireView()).popBackStack();
                    });
                } catch (Exception e) {
                    Log.e(TAG, "保存标签失败", e);
                    mainHandler.post(() -> {
                        ToastUtils.showToast(getContext(), getString(R.string.save_tags_failed));
                    });
                }
            });
        }
    }

    /**
     * 执行文件替换操作
     */
    private void performFileReplacement() {
        if (currentSong == null) {
            ToastUtils.showToast(getContext(), getString(R.string.save_tags_failed));
            return;
        }

        executorService.execute(() -> {
            try {
                // 获取原文件路径
                String originalFilePath = currentSong.getPath();
                File originalFile = new File(originalFilePath);

                // 获取缓存文件路径 - 使用getCacheDir()
                File cacheDir = requireContext().getCacheDir();
                File[] cacheFiles = cacheDir.listFiles((dir, name) -> name.endsWith(".mp3") || name.endsWith(".flac") || name.endsWith(".m4a"));

                if (cacheFiles == null || cacheFiles.length == 0) {
                    mainHandler.post(() -> ToastUtils.showToast(getContext(), "未找到缓存文件"));
                    return;
                }

                File cacheFile = cacheFiles[0]; // 使用第一个找到的缓存文件

                // 使用ContentResolver进行文件替换
                android.content.ContentResolver contentResolver = requireContext().getContentResolver();

                // 获取原文件的URI
                android.net.Uri originalUri = android.net.Uri.fromFile(originalFile);

                // 打开原文件进行写入
                try (java.io.OutputStream outputStream = contentResolver.openOutputStream(originalUri, "w")) {
                    if (outputStream != null) {
                        // 读取缓存文件并写入原文件
                        try (java.io.FileInputStream inputStream = new java.io.FileInputStream(cacheFile)) {
                            byte[] buffer = new byte[8192];
                            int bytesRead;
                            while ((bytesRead = inputStream.read(buffer)) != -1) {
                                outputStream.write(buffer, 0, bytesRead);
                            }
                        }

                        // 删除所有缓存文件
                        for (File cf : cacheFiles) {
                            try {
                                cf.delete();
                                System.out.println("已删除缓存文件: " + cf.getAbsolutePath());
                            } catch (Exception e) {
                                e.printStackTrace();
                            }
                        }

                        // 更新本地数据库
                        mainHandler.post(() -> {
                            updateLocalDatabaseAfterWrite();
                            // 通知所有fragment刷新
                            notifyAllFragmentsRefresh();
                            ToastUtils.showToast(getContext(), getString(R.string.tags_saved_successfully));
                            Navigation.findNavController(requireView()).popBackStack();
                        });

                        // 打印原始数据到控制台
                        System.out.println("文件替换成功:");
                        System.out.println("原文件: " + originalFilePath);
                        System.out.println("缓存文件: " + cacheFile.getAbsolutePath());

                    } else {
                        mainHandler.post(() -> {
                            ToastUtils.showToast(getContext(), "无法写入原文件");
                        });
                    }
                }

            } catch (Exception e) {
                Log.e(TAG, "文件替换失败", e);
                mainHandler.post(() -> {
                    ToastUtils.showToast(getContext(), "文件替换失败: " + e.getMessage());
                });
            }
        });
    }

    /**
     * 获取文件的MediaStore URI
     */
    private android.net.Uri getMediaStoreUri(String filePath) {
        try {
            // 查询MediaStore获取文件的URI
            String[] projection = {android.provider.MediaStore.Audio.Media._ID};
            String selection = android.provider.MediaStore.Audio.Media.DATA + "=?";
            String[] selectionArgs = {filePath};

            android.content.ContentResolver contentResolver = requireContext().getContentResolver();
            try (android.database.Cursor cursor = contentResolver.query(
                    android.provider.MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
                    projection,
                    selection,
                    selectionArgs,
                    null)) {

                if (cursor != null && cursor.moveToFirst()) {
                    long id = cursor.getLong(cursor.getColumnIndexOrThrow(android.provider.MediaStore.Audio.Media._ID));
                    return android.content.ContentUris.withAppendedId(
                            android.provider.MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
                            id
                    );
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "获取MediaStore URI失败", e);
        }
        return null;
    }

    /**
     * 处理缓存文件（Android 11及以上版本）
     * 使用MediaStore.createWriteRequest自动替换文件
     */
    private void handleCacheFiles(List<File> cacheFiles) {
        if (currentSong == null || cacheFiles.isEmpty()) {
            ToastUtils.showToast(getContext(), getString(R.string.save_tags_failed));
            return;
        }

        try {
            // 获取原文件路径
            String originalFilePath = currentSong.getPath();
            File originalFile = new File(originalFilePath);

            if (!originalFile.exists()) {
                ToastUtils.showToast(getContext(), "原文件不存在");
                return;
            }

            // 获取缓存文件（应该只有一个）
            File cacheFile = cacheFiles.get(0);
            if (!cacheFile.exists()) {
                ToastUtils.showToast(getContext(), "缓存文件不存在");
                return;
            }

            // 获取文件的MediaStore URI
            android.net.Uri mediaStoreUri = getMediaStoreUri(originalFilePath);
            if (mediaStoreUri == null) {
                ToastUtils.showToast(getContext(), "无法获取文件的MediaStore URI");
                return;
            }

            // 使用MediaStore.createWriteRequest创建写入请求
            android.app.PendingIntent pendingIntent = android.provider.MediaStore.createWriteRequest(
                    requireContext().getContentResolver(),
                    java.util.Collections.singletonList(mediaStoreUri)
            );

            try {
                // 使用新的ActivityResultLauncher启动写入请求
                androidx.activity.result.IntentSenderRequest request =
                        new androidx.activity.result.IntentSenderRequest.Builder(pendingIntent.getIntentSender())
                                .build();
                writeRequestLauncher.launch(request);
            } catch (Exception e) {
                Log.e(TAG, "启动写入请求失败", e);
                ToastUtils.showToast(getContext(), "启动写入请求失败");
            }

            // 打印原始数据到控制台
            System.out.println("已创建MediaStore写入请求:");
            System.out.println("原文件路径: " + originalFilePath);
            System.out.println("缓存文件路径: " + cacheFile.getAbsolutePath());

        } catch (Exception e) {
            Log.e(TAG, "处理缓存文件失败", e);
            ToastUtils.showToast(getContext(), "处理文件失败: " + e.getMessage());
        }
    }

    /**
     * 写入成功后更新本地数据库
     */
    private void updateLocalDatabaseAfterWrite() {
        if (currentSong == null) {
            return;
        }

        executorService.execute(() -> {
            try {
                // 更新歌曲信息
                String title = binding.etTitle.getText().toString().trim();
                String artist = binding.etArtist.getText().toString().trim();
                String album = binding.etAlbum.getText().toString().trim();
                String albumArtist = binding.etAlbumArtist.getText().toString().trim();
                String composer = binding.etComposer.getText().toString().trim();
                String genre = binding.etGenre.getText().toString().trim();
                String yearStr = binding.etYear.getText().toString().trim();
                String trackStr = binding.etTrackNumber.getText().toString().trim();
                String discStr = binding.etDiscNumber.getText().toString().trim();

                currentSong.setTitle(title);
                currentSong.setArtist(artist);
                currentSong.setAlbum(album);
                currentSong.setGenre(genre);

                if (!TextUtils.isEmpty(yearStr)) {
                    try {
                        currentSong.setYear(Integer.parseInt(yearStr));
                    } catch (NumberFormatException e) {
                        Log.w(TAG, "年份格式错误: " + yearStr);
                    }
                }

                if (!TextUtils.isEmpty(trackStr)) {
                    try {
                        currentSong.setTrack(Integer.parseInt(trackStr));
                    } catch (NumberFormatException e) {
                        Log.w(TAG, "曲目号格式错误: " + trackStr);
                    }
                }

                // 保存到数据库
                currentSong.save();

                // 如果专辑信息发生变化，可能需要更新专辑表
                if (!TextUtils.isEmpty(album) && !TextUtils.isEmpty(artist)) {
                    updateAlbumInfo(album, artist, genre, yearStr);
                }

                // 打印原始数据到控制台
                System.out.println("已更新歌曲信息到数据库:");
                System.out.println("标题: " + currentSong.getTitle());
                System.out.println("艺术家: " + currentSong.getArtist());
                System.out.println("专辑: " + currentSong.getAlbum());
                System.out.println("专辑艺术家: " + albumArtist);
                System.out.println("作曲家: " + composer);
                System.out.println("流派: " + currentSong.getGenre());
                System.out.println("年份: " + currentSong.getYear());
                System.out.println("曲目号: " + currentSong.getTrack());
                System.out.println("碟片号: " + discStr);

                // 通知所有fragment刷新
                mainHandler.post(() -> {
                    notifyAllFragmentsRefresh();
                });

            } catch (Exception e) {
                Log.e(TAG, "更新数据库失败", e);
                System.out.println("更新数据库失败: " + e.getMessage());
            }
        });
    }

    /**
     * 更新专辑信息
     */
    private void updateAlbumInfo(String albumName, String artist, String genre, String yearStr) {
        try {
            // 查找或创建专辑记录
            Album album = org.litepal.LitePal.where("albumName = ? and artist = ?", 
                    albumName, artist).findFirst(Album.class);
            
            if (album == null) {
                // 创建新专辑记录
                album = new Album();
                album.setAlbumName(albumName);
                album.setArtist(artist);
                album.setAlbumId(currentSong.getAlbumId());
            }
            
            // 更新专辑信息
            if (!TextUtils.isEmpty(genre)) {
                album.setGenre(genre);
            }
            
            if (!TextUtils.isEmpty(yearStr)) {
                try {
                    album.setYear(Integer.parseInt(yearStr));
                } catch (NumberFormatException e) {
                    Log.w(TAG, "专辑年份格式错误: " + yearStr);
                }
            }
            
            // 计算专辑中的歌曲数量
            int songCount = org.litepal.LitePal.where("album = ? and artist = ?", 
                    albumName, artist).count(Song.class);
            album.setSongCount(songCount);
            
            album.save();
            
            System.out.println("已更新专辑信息: " + albumName + " - " + artist);
            
        } catch (Exception e) {
            Log.e(TAG, "更新专辑信息失败", e);
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (executorService != null) {
            executorService.shutdown();
        }
        if (mainHandler != null) {
            mainHandler.removeCallbacksAndMessages(null);
        }
    }

} 
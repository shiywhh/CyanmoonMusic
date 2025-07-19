package com.magicalstory.music.fragment;

import android.app.Activity;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.provider.MediaStore;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.media3.common.util.UnstableApi;
import androidx.navigation.Navigation;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.bumptech.glide.Glide;
import com.bumptech.glide.request.target.CustomTarget;
import com.bumptech.glide.request.transition.Transition;
import com.magicalstory.music.R;
import com.magicalstory.music.adapter.SongVerticalAdapter;
import com.magicalstory.music.base.BaseFragment;
import com.magicalstory.music.databinding.FragmentTagEditorBinding;
import com.magicalstory.music.model.Album;
import com.magicalstory.music.model.Song;
import com.magicalstory.music.utils.VersionUtils;
import com.magicalstory.music.utils.app.ToastUtils;
import com.magicalstory.music.utils.tag.TagWriter;
import com.magicalstory.music.utils.text.TimeUtils;

import org.litepal.LitePal;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * 标签编辑器Fragment
 */
@UnstableApi
public class TagEditorFragment extends BaseFragment<FragmentTagEditorBinding> {

    private static final String ARG_ALBUM_ID = "album_id";
    private static final String ARG_ARTIST_NAME = "artist_name";
    private static final String ARG_ALBUM_NAME = "album_name";

    private Album currentAlbum;

    private ExecutorService executorService;
    private Handler mainHandler;
    private List<Song> albumSongs;
    private Uri selectedImageUri;
    private ActivityResultLauncher<Intent> imagePickerLauncher;
    private ActivityResultLauncher<androidx.activity.result.IntentSenderRequest> writeRequestLauncher;
    private boolean hasCustomCover = false; // 标记是否有自定义封面

    public static TagEditorFragment newInstance(long albumId, String artistName, String albumName) {
        TagEditorFragment fragment = new TagEditorFragment();
        Bundle args = new Bundle();
        args.putLong(ARG_ALBUM_ID, albumId);
        args.putString(ARG_ARTIST_NAME, artistName);
        args.putString(ARG_ALBUM_NAME, albumName);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    protected FragmentTagEditorBinding getViewBinding(LayoutInflater inflater, ViewGroup container) {
        return FragmentTagEditorBinding.inflate(inflater, container, false);
    }

    @Override
    protected boolean usePersistentView() {
        return true;
    }

    @Override
    protected int getLayoutRes() {
        return R.layout.fragment_tag_editor;
    }

    @Override
    protected FragmentTagEditorBinding bindPersistentView(View view) {
        return FragmentTagEditorBinding.bind(view);
    }

    @Override
    protected void initViewForPersistentView() {
        super.initViewForPersistentView();

        // 初始化Handler和线程池
        mainHandler = new Handler(Looper.getMainLooper());
        executorService = Executors.newCachedThreadPool();

        // 初始化图片选择器
        initImagePicker();

        // 获取传递的参数并加载数据
        Bundle args = getArguments();
        if (args != null) {
            long albumId = args.getLong(ARG_ALBUM_ID);
            String artistName = args.getString(ARG_ARTIST_NAME);
            String albumName = args.getString(ARG_ALBUM_NAME);

            loadAlbumData(albumId, artistName, albumName);
        }
    }

    @Override
    protected void initListenerForPersistentView() {
        super.initListenerForPersistentView();

        // 返回按钮
        binding.toolbar.setNavigationOnClickListener(v -> {
            Navigation.findNavController(requireView()).popBackStack();
        });

        // 专辑封面点击选择图片
        binding.ivAlbumCover.setOnClickListener(v -> {
            openImagePicker();
        });

        // 更换封面按钮
        binding.addCover.setOnClickListener(v -> {
            openImagePicker();
        });

        // 移除封面按钮
        binding.removeCover.setOnClickListener(v -> {
            removeAlbumCover();
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
                        // 写入成功，更新本地数据库
                        updateLocalDatabaseAfterWrite();
                        // 执行文件替换操作
                        performFileReplacement();
                    } else {
                        // 写入失败，尝试直接文件替换
                        ToastUtils.showToast(getContext(), "MediaStore写入失败，尝试直接文件替换");
                        performFileReplacement();
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
     * 移除专辑封面
     */
    private void removeAlbumCover() {
        // 清除选中的图片
        selectedImageUri = null;
        hasCustomCover = false;
        
        // 重新加载默认封面
        loadAlbumCover();
        
        // 更新按钮状态
        updateCoverButtons();
        
        // 删除已保存的封面文件
        executorService.execute(() -> {
            try {
                File albumArtDir = new File(requireContext().getFilesDir(), "album_art");
                if (albumArtDir.exists()) {
                    File albumArtFile = new File(albumArtDir, currentAlbum.getAlbumId() + ".jpg");
                    if (albumArtFile.exists()) {
                        albumArtFile.delete();
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        });
        
        ToastUtils.showToast(getContext(), getString(R.string.cover_removed));
    }

    /**
     * 加载专辑数据
     */
    private void loadAlbumData(long albumId, String artistName, String albumName) {
        executorService.execute(() -> {
            try {
                // 查询专辑信息
                currentAlbum = LitePal.where("albumId = ? and artist = ?",
                        String.valueOf(albumId), artistName).findFirst(Album.class);

                if (currentAlbum == null) {
                    // 创建基本的专辑对象
                    currentAlbum = new Album();
                    currentAlbum.setAlbumId(albumId);
                    currentAlbum.setArtist(artistName);
                    currentAlbum.setAlbumName(albumName);
                }

                // 查询专辑歌曲
                albumSongs = LitePal.where("albumId = ? and artist = ?",
                                String.valueOf(albumId), artistName)
                        .order("track asc")
                        .find(Song.class);

                // 检查是否有自定义封面
                checkCustomCover();

                // 计算总时长
                if (albumSongs != null) {
                    long totalDuration = 0;
                    for (Song song : albumSongs) {
                        totalDuration += song.getDuration();
                    }
                    currentAlbum.setSongCount(albumSongs.size());
                }

                // 在主线程更新UI
                mainHandler.post(this::updateUI);

            } catch (Exception e) {
                e.printStackTrace();
                mainHandler.post(() -> {
                    ToastUtils.showToast(getContext(), "加载专辑数据失败");
                });
            }
        });
    }

    /**
     * 检查是否有自定义封面
     */
    private void checkCustomCover() {
        try {
            File albumArtDir = new File(requireContext().getFilesDir(), "album_art");
            if (albumArtDir.exists()) {
                File albumArtFile = new File(albumArtDir, currentAlbum.getAlbumId() + ".jpg");
                hasCustomCover = albumArtFile.exists();
            }
        } catch (Exception e) {
            e.printStackTrace();
            hasCustomCover = false;
        }
    }

    /**
     * 更新UI
     */
    private void updateUI() {
        if (currentAlbum == null) return;

        // 设置专辑信息
        binding.tvAlbumName.setText(currentAlbum.getAlbumName());

        // 设置歌曲数量和时长
        String songCountText = albumSongs != null ?
                albumSongs.size() + " 首歌曲" : "0 首歌曲";

        if (albumSongs != null && !albumSongs.isEmpty()) {
            long totalDuration = 0;
            for (Song song : albumSongs) {
                totalDuration += song.getDuration();
            }
            String durationText = TimeUtils.formatDuration(totalDuration);
            songCountText += " • 时长 " + durationText;
        }

        binding.tvSongCount.setText(songCountText);

        // 加载专辑封面
        loadAlbumCover();

        // 更新封面按钮状态
        updateCoverButtons();

        // 填充编辑框
        binding.etAlbumName.setText(currentAlbum.getAlbumName());
        binding.etAlbumArtist.setText(currentAlbum.getArtist());
        binding.etGenre.setText(currentAlbum.getGenre());
        binding.etYear.setText(currentAlbum.getYear() > 0 ? String.valueOf(currentAlbum.getYear()) : "");
        binding.etTrackTotal.setText(currentAlbum.getSongCount() > 0 ? String.valueOf(currentAlbum.getSongCount()) : "");
        binding.etDiscTotal.setText("1"); // 默认为1
    }

    /**
     * 加载专辑封面
     */
    private void loadAlbumCover() {
        if (hasCustomCover) {
            // 加载自定义封面
            File albumArtDir = new File(requireContext().getFilesDir(), "album_art");
            File albumArtFile = new File(albumArtDir, currentAlbum.getAlbumId() + ".jpg");
            
            Glide.with(this)
                    .load(albumArtFile)
                    .placeholder(R.drawable.place_holder_album)
                    .error(R.drawable.place_holder_album)
                    .into(binding.ivAlbumCover);
        } else {
            // 加载系统封面
            String albumArtUri = "content://media/external/audio/albumart/" + currentAlbum.getAlbumId();

            Glide.with(this)
                    .load(albumArtUri)
                    .placeholder(R.drawable.place_holder_album)
                    .error(R.drawable.place_holder_album)
                    .into(binding.ivAlbumCover);
        }
    }

    /**
     * 打开图片选择器
     */
    private void openImagePicker() {
        Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
        intent.setType("image/*");
        imagePickerLauncher.launch(Intent.createChooser(intent, getString(R.string.select_image)));
    }

    /**
     * 加载选中的图片
     */
    private void loadSelectedImage() {
        if (selectedImageUri == null) return;

        Glide.with(this)
                .asBitmap()
                .load(selectedImageUri)
                .into(new CustomTarget<Bitmap>() {
                    @Override
                    public void onResourceReady(@NonNull Bitmap resource, @Nullable Transition<? super Bitmap> transition) {
                        binding.ivAlbumCover.setImageBitmap(resource);
                    }

                    @Override
                    public void onLoadCleared(@Nullable Drawable placeholder) {
                        // 可选择处理
                    }
                });
    }

    /**
     * 保存标签
     */
    private void saveTags() {
        if (currentAlbum == null) return;

        // 获取编辑框中的值
        String albumName = binding.etAlbumName.getText().toString().trim();
        String albumArtist = binding.etAlbumArtist.getText().toString().trim();
        String genre = binding.etGenre.getText().toString().trim();
        String yearStr = binding.etYear.getText().toString().trim();
        String trackTotalStr = binding.etTrackTotal.getText().toString().trim();
        String discTotalStr = binding.etDiscTotal.getText().toString().trim();

        // 验证必填字段
        if (TextUtils.isEmpty(albumName)) {
            ToastUtils.showToast(getContext(), "专辑名称不能为空");
            return;
        }

        if (TextUtils.isEmpty(albumArtist)) {
            ToastUtils.showToast(getContext(), "艺术家名称不能为空");
            return;
        }

        // 更新专辑信息
        currentAlbum.setAlbumName(albumName);
        currentAlbum.setArtist(albumArtist);
        currentAlbum.setGenre(genre);

        // 解析年份
        if (!TextUtils.isEmpty(yearStr)) {
            try {
                int year = Integer.parseInt(yearStr);
                currentAlbum.setYear(year);
            } catch (NumberFormatException e) {
                // 年份格式错误，使用默认值
                currentAlbum.setYear(0);
            }
        }

        // 解析音轨总数
        if (!TextUtils.isEmpty(trackTotalStr)) {
            try {
                int trackTotal = Integer.parseInt(trackTotalStr);
                currentAlbum.setSongCount(trackTotal);
            } catch (NumberFormatException e) {
                // 使用实际歌曲数量
                currentAlbum.setSongCount(albumSongs != null ? albumSongs.size() : 0);
            }
        }

        // 保存专辑信息到数据库
        boolean albumSaved = saveAlbumToDatabase();

        // 保存专辑封面
        boolean coverSaved = saveAlbumCover();

        // 使用新的标签写入系统
        if (albumSaved && coverSaved) {
            writeValuesToFiles(albumName, albumArtist, genre);
        } else {
            ToastUtils.showToast(getContext(), getString(R.string.save_tags_failed));
        }
    }

    /**
     * 写入标签到文件
     */
    private void writeValuesToFiles(String albumName, String albumArtist, String genre) {
        if (albumSongs == null || albumSongs.isEmpty()) {
            ToastUtils.showToast(getContext(), getString(R.string.tags_saved_successfully));
            Navigation.findNavController(requireView()).popBackStack();
            return;
        }

        // 构建字段键值映射
        Map<org.jaudiotagger.tag.FieldKey, String> fieldKeyValueMap = new HashMap<>();
        fieldKeyValueMap.put(org.jaudiotagger.tag.FieldKey.ALBUM, albumName);
        fieldKeyValueMap.put(org.jaudiotagger.tag.FieldKey.ARTIST, albumArtist);
        if (!TextUtils.isEmpty(genre)) {
            fieldKeyValueMap.put(org.jaudiotagger.tag.FieldKey.GENRE, genre);
        }

        // 获取文件路径列表
        List<String> songPaths = new ArrayList<>();
        for (Song song : albumSongs) {
            if (song.getPath() != null && !song.getPath().isEmpty()) {
                songPaths.add(song.getPath());
            }
        }

        if (songPaths.isEmpty()) {
            ToastUtils.showToast(getContext(), getString(R.string.save_tags_failed));
            return;
        }

        // 处理封面
        TagWriter.ArtworkInfo artworkInfo = null;
        if (selectedImageUri != null || hasCustomCover) {
            Bitmap artwork = null;
            if (selectedImageUri != null) {
                // 从选中的图片获取Bitmap
                try {
                    artwork = Glide.with(this)
                            .asBitmap()
                            .load(selectedImageUri)
                            .submit()
                            .get();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            } else {
                // 从已保存的封面文件获取Bitmap
                try {
                    File albumArtDir = new File(requireContext().getFilesDir(), "album_art");
                    File albumArtFile = new File(albumArtDir, currentAlbum.getAlbumId() + ".jpg");
                    if (albumArtFile.exists()) {
                        artwork = Glide.with(this)
                                .asBitmap()
                                .load(albumArtFile)
                                .submit()
                                .get();
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
            artworkInfo = new TagWriter.ArtworkInfo(currentAlbum.getAlbumId(), artwork);
        }

        // 创建音频标签信息
        TagWriter.AudioTagInfo audioTagInfo = new TagWriter.AudioTagInfo(
                songPaths, fieldKeyValueMap, artworkInfo
        );

        // 打印原始数据到控制台
        System.out.println("准备写入标签 - 专辑: " + albumName + 
                         ", 艺术家: " + albumArtist + 
                         ", 流派: " + genre + 
                         ", 歌曲数量: " + songPaths.size() + 
                         ", 有封面: " + (artworkInfo != null && artworkInfo.getArtwork() != null));

        // 根据Android版本选择写入方式
        if (VersionUtils.hasR()) {
            // Android 11及以上版本
            executorService.execute(() -> {
                try {
                    List<File> cacheFiles = TagWriter.writeTagsToFilesR(requireContext(), audioTagInfo);
                    
                    if (!cacheFiles.isEmpty()) {
                        mainHandler.post(() -> {
                            handleCacheFiles(cacheFiles);
                        });
                    } else {
                        mainHandler.post(() -> {
                            // 通知所有fragment刷新
                            notifyAllFragmentsRefresh();
                            ToastUtils.showToast(getContext(), getString(R.string.tags_saved_successfully));
                            Navigation.findNavController(requireView()).popBackStack();
                        });
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                    mainHandler.post(() -> {
                        ToastUtils.showToast(getContext(), getString(R.string.save_tags_failed));
                    });
                }
            });
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
                    e.printStackTrace();
                    mainHandler.post(() -> {
                        ToastUtils.showToast(getContext(), getString(R.string.save_tags_failed));
                    });
                }
            });
        }
    }

    /**
     * 处理缓存文件（Android 11及以上版本）
     * 使用MediaStore.createWriteRequest自动替换文件
     */
    private void handleCacheFiles(List<File> cacheFiles) {
        if (albumSongs == null || cacheFiles.isEmpty()) {
            ToastUtils.showToast(getContext(), getString(R.string.save_tags_failed));
            return;
        }

        try {
            // 构建需要写入的URI列表
            List<Uri> songUris = new ArrayList<>();
            for (Song song : albumSongs) {
                if (song.getMediaStoreId() > 0) {
                    Uri songUri = android.content.ContentUris.withAppendedId(
                            MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
                            song.getMediaStoreId()
                    );
                    songUris.add(songUri);
                }
            }

            if (!songUris.isEmpty()) {
                try {
                    android.app.PendingIntent writeIntent = MediaStore.createWriteRequest(
                            requireContext().getContentResolver(),
                            songUris
                    );
                    writeRequestLauncher.launch(
                            new androidx.activity.result.IntentSenderRequest.Builder(writeIntent.getIntentSender()).build()
                    );
                } catch (Exception e) {
                    e.printStackTrace();
                    ToastUtils.showToast(getContext(), "创建写入请求失败: " + e.getMessage());
                }
            } else {
                // 如果没有MediaStore ID，尝试使用文件路径
                performFileReplacement();
            }

            // 打印原始数据到控制台
            System.out.println("已创建MediaStore写入请求:");
            System.out.println("歌曲数量: " + songUris.size());
            System.out.println("缓存文件数量: " + cacheFiles.size());

        } catch (Exception e) {
            e.printStackTrace();
            ToastUtils.showToast(getContext(), "处理文件失败: " + e.getMessage());
        }
    }

    /**
     * 执行文件替换操作
     */
    private void performFileReplacement() {
        if (albumSongs == null || albumSongs.isEmpty()) {
            ToastUtils.showToast(getContext(), getString(R.string.save_tags_failed));
            return;
        }

        executorService.execute(() -> {
            try {
                // 获取缓存文件路径 - 使用getCacheDir()
                File cacheDir = requireContext().getCacheDir();
                File[] cacheFiles = cacheDir.listFiles((dir, name) -> name.endsWith(".mp3") || name.endsWith(".flac") || name.endsWith(".m4a"));

                if (cacheFiles == null || cacheFiles.length == 0) {
                    mainHandler.post(() -> ToastUtils.showToast(getContext(), "未找到缓存文件"));
                    return;
                }

                // 使用ContentResolver进行文件替换
                android.content.ContentResolver contentResolver = requireContext().getContentResolver();
                int successCount = 0;

                for (Song song : albumSongs) {
                    if (song.getPath() == null || song.getPath().isEmpty()) {
                        continue;
                    }

                    File originalFile = new File(song.getPath());
                    if (!originalFile.exists()) {
                        continue;
                    }

                    // 查找对应的缓存文件
                    File cacheFile = null;
                    for (File cf : cacheFiles) {
                        if (cf.getName().equals(originalFile.getName())) {
                            cacheFile = cf;
                            break;
                        }
                    }

                    if (cacheFile == null || !cacheFile.exists()) {
                        continue;
                    }

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
                            successCount++;

                            // 打印原始数据到控制台
                            System.out.println("文件替换成功: " + originalFile.getAbsolutePath());
                        }
                    } catch (Exception e) {
                        System.out.println("文件替换失败: " + originalFile.getAbsolutePath() + " - " + e.getMessage());
                    }
                }

                // 删除所有缓存文件
                for (File cacheFile : cacheFiles) {
                    try {
                        cacheFile.delete();
                        System.out.println("已删除缓存文件: " + cacheFile.getAbsolutePath());
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }

                // 更新本地数据库
                mainHandler.post(() -> {
                    updateLocalDatabaseAfterWrite();
                    ToastUtils.showToast(getContext(), getString(R.string.tags_saved_successfully));
                    Navigation.findNavController(requireView()).popBackStack();
                });

                // 打印原始数据到控制台
                System.out.println("文件替换完成，成功替换: " + successCount + " 个文件");

            } catch (Exception e) {
                e.printStackTrace();
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
            e.printStackTrace();
        }
        return null;
    }

    /**
     * 保存专辑信息到数据库
     */
    private boolean saveAlbumToDatabase() {
        try {
            // 查找现有专辑记录
            Album existingAlbum = LitePal.where("albumId = ?",
                    String.valueOf(currentAlbum.getAlbumId())).findFirst(Album.class);

            if (existingAlbum != null) {
                // 更新现有记录
                existingAlbum.setAlbumName(currentAlbum.getAlbumName());
                existingAlbum.setArtist(currentAlbum.getArtist());
                existingAlbum.setGenre(currentAlbum.getGenre());
                existingAlbum.setYear(currentAlbum.getYear());
                existingAlbum.setSongCount(currentAlbum.getSongCount());
                return existingAlbum.save();
            } else {
                // 创建新记录
                return currentAlbum.save();
            }
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    /**
     * 写入成功后更新本地数据库
     */
    private void updateLocalDatabaseAfterWrite() {
        try {
            // 获取编辑框中的值
            String albumName = binding.etAlbumName.getText().toString().trim();
            String albumArtist = binding.etAlbumArtist.getText().toString().trim();
            String genre = binding.etGenre.getText().toString().trim();
            String yearStr = binding.etYear.getText().toString().trim();
            String trackTotalStr = binding.etTrackTotal.getText().toString().trim();

            // 更新专辑信息
            if (currentAlbum != null) {
                currentAlbum.setAlbumName(albumName);
                currentAlbum.setArtist(albumArtist);
                currentAlbum.setGenre(genre);
                
                if (!TextUtils.isEmpty(yearStr)) {
                    try {
                        currentAlbum.setYear(Integer.parseInt(yearStr));
                    } catch (NumberFormatException e) {
                        currentAlbum.setYear(0);
                    }
                }
                
                if (!TextUtils.isEmpty(trackTotalStr)) {
                    try {
                        currentAlbum.setSongCount(Integer.parseInt(trackTotalStr));
                    } catch (NumberFormatException e) {
                        currentAlbum.setSongCount(albumSongs != null ? albumSongs.size() : 0);
                    }
                }
                
                currentAlbum.save();
            }

            // 更新歌曲信息
            if (albumSongs != null) {
                for (Song song : albumSongs) {
                    song.setAlbum(albumName);
                    song.setArtist(albumArtist);
                    if (!TextUtils.isEmpty(genre)) {
                        song.setGenre(genre);
                    }
                    
                    // 更新歌曲的专辑ID相关信息
                    if (currentAlbum != null) {
                        song.setAlbumId(currentAlbum.getAlbumId());
                    }
                    
                    song.save();
                }
            }

            // 打印原始数据到控制台
            System.out.println("文件替换成功，已更新本地数据库:");
            System.out.println("专辑信息 - 名称: " + albumName + 
                             ", 艺术家: " + albumArtist + 
                             ", 流派: " + genre + 
                             ", 年份: " + (currentAlbum != null ? currentAlbum.getYear() : 0) +
                             ", 歌曲数量: " + (albumSongs != null ? albumSongs.size() : 0));

            // 通知所有fragment刷新
            notifyAllFragmentsRefresh();

        } catch (Exception e) {
            e.printStackTrace();
            System.out.println("更新数据库失败: " + e.getMessage());
        }
    }

    /**
     * 保存专辑封面
     */
    private boolean saveAlbumCover() {
        if (selectedImageUri == null && !hasCustomCover) {
            return true; // 没有选择新图片且没有自定义封面，认为成功
        }

        try {
            // 获取应用的私有存储目录
            File albumArtDir = new File(requireContext().getFilesDir(), "album_art");
            if (!albumArtDir.exists()) {
                albumArtDir.mkdirs();
            }

            // 创建专辑封面文件
            File albumArtFile = new File(albumArtDir, currentAlbum.getAlbumId() + ".jpg");

            // 如果有新选择的图片，复制到专辑封面文件
            if (selectedImageUri != null) {
                try (InputStream inputStream = requireContext().getContentResolver().openInputStream(selectedImageUri);
                     OutputStream outputStream = new FileOutputStream(albumArtFile)) {

                    if (inputStream != null) {
                        byte[] buffer = new byte[4096];
                        int bytesRead;
                        while ((bytesRead = inputStream.read(buffer)) != -1) {
                            outputStream.write(buffer, 0, bytesRead);
                        }
                        outputStream.flush();
                    }
                }
            }

            return albumArtFile.exists();
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();

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
package com.magicalstory.music.fragment;

import android.app.Activity;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.drawable.Drawable;
import android.net.Uri;
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
import com.magicalstory.music.utils.app.ToastUtils;
import com.magicalstory.music.utils.text.TimeUtils;

import org.litepal.LitePal;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * 标签编辑器Fragment
 */
public class TagEditorFragment extends BaseFragment<FragmentTagEditorBinding> {

    private static final String ARG_ALBUM_ID = "album_id";
    private static final String ARG_ARTIST_NAME = "artist_name";
    private static final String ARG_ALBUM_NAME = "album_name";

    private Album currentAlbum;
    private List<Song> albumSongs;
    private SongVerticalAdapter songAdapter;
    
    private ExecutorService executorService;
    private Handler mainHandler;
    
    private Uri selectedImageUri;
    private ActivityResultLauncher<Intent> imagePickerLauncher;

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
    protected void initView() {
        super.initView();
        
        // 初始化Handler和线程池
        mainHandler = new Handler(Looper.getMainLooper());
        executorService = Executors.newCachedThreadPool();
        
        // 初始化图片选择器
        initImagePicker();
        
        // 初始化歌曲列表
        initSongsList();
        
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
    protected void initListener() {
        super.initListener();
        
        // 返回按钮
        binding.toolbar.setNavigationOnClickListener(v -> {
            Navigation.findNavController(requireView()).popBackStack();
        });
        
        // 专辑封面点击选择图片
        binding.ivAlbumCover.setOnClickListener(v -> {
            openImagePicker();
        });
        
        // 保存按钮
        binding.fabSave.setOnClickListener(v -> {
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
                        }
                    }
                }
        );
    }

    /**
     * 初始化歌曲列表
     */
    private void initSongsList() {
        binding.rvSongs.setLayoutManager(new LinearLayoutManager(getContext()));
        songAdapter = new SongVerticalAdapter(getContext(), new ArrayList<>());
        binding.rvSongs.setAdapter(songAdapter);
    }

    /**
     * 加载专辑数据
     */
    private void loadAlbumData(long albumId, String artistName, String albumName) {
        binding.progressBar.setVisibility(View.VISIBLE);
        
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
                
                // 计算总时长
                if (albumSongs != null) {
                    long totalDuration = 0;
                    for (Song song : albumSongs) {
                        totalDuration += song.getDuration();
                    }
                    currentAlbum.setSongCount(albumSongs.size());
                }
                
                // 在主线程更新UI
                mainHandler.post(() -> {
                    updateUI();
                    binding.progressBar.setVisibility(View.GONE);
                });
                
            } catch (Exception e) {
                e.printStackTrace();
                mainHandler.post(() -> {
                    binding.progressBar.setVisibility(View.GONE);
                    ToastUtils.showToast(getContext(), "加载专辑数据失败");
                });
            }
        });
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
        
        // 填充编辑框
        binding.etAlbumName.setText(currentAlbum.getAlbumName());
        binding.etAlbumArtist.setText(currentAlbum.getArtist());
        binding.etGenre.setText(currentAlbum.getGenre());
        binding.etYear.setText(currentAlbum.getYear() > 0 ? String.valueOf(currentAlbum.getYear()) : "");
        binding.etTrackTotal.setText(currentAlbum.getSongCount() > 0 ? String.valueOf(currentAlbum.getSongCount()) : "");
        binding.etDiscTotal.setText("1"); // 默认为1
        
        // 更新歌曲列表
        if (albumSongs != null) {
            songAdapter.updateData(albumSongs);
        }
    }

    /**
     * 加载专辑封面
     */
    private void loadAlbumCover() {
        String albumArtUri = "content://media/external/audio/albumart/" + currentAlbum.getAlbumId();
        
        Glide.with(this)
                .load(albumArtUri)
                .placeholder(R.drawable.place_holder_album)
                .error(R.drawable.place_holder_album)
                .into(binding.ivAlbumCover);
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
        
        binding.progressBar.setVisibility(View.VISIBLE);
        
        executorService.execute(() -> {
            try {
                // 获取编辑框中的值
                String albumName = binding.etAlbumName.getText().toString().trim();
                String albumArtist = binding.etAlbumArtist.getText().toString().trim();
                String genre = binding.etGenre.getText().toString().trim();
                String yearStr = binding.etYear.getText().toString().trim();
                String trackTotalStr = binding.etTrackTotal.getText().toString().trim();
                String discTotalStr = binding.etDiscTotal.getText().toString().trim();
                
                // 验证必填字段
                if (TextUtils.isEmpty(albumName)) {
                    mainHandler.post(() -> {
                        binding.progressBar.setVisibility(View.GONE);
                        ToastUtils.showToast(getContext(), "专辑名称不能为空");
                    });
                    return;
                }
                
                if (TextUtils.isEmpty(albumArtist)) {
                    mainHandler.post(() -> {
                        binding.progressBar.setVisibility(View.GONE);
                        ToastUtils.showToast(getContext(), "艺术家名称不能为空");
                    });
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
                
                // 更新相关歌曲的标签
                boolean songsSaved = updateSongsTags(albumName, albumArtist, genre);
                
                // 保存专辑封面
                boolean coverSaved = saveAlbumCover();
                
                // 在主线程显示结果
                mainHandler.post(() -> {
                    binding.progressBar.setVisibility(View.GONE);
                    if (albumSaved && songsSaved && coverSaved) {
                        ToastUtils.showToast(getContext(), getString(R.string.tags_saved_successfully));
                        Navigation.findNavController(requireView()).popBackStack();
                    } else {
                        ToastUtils.showToast(getContext(), getString(R.string.save_tags_failed));
                    }
                });
                
            } catch (Exception e) {
                e.printStackTrace();
                mainHandler.post(() -> {
                    binding.progressBar.setVisibility(View.GONE);
                    ToastUtils.showToast(getContext(), getString(R.string.save_tags_failed));
                });
            }
        });
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
     * 更新歌曲标签
     */
    private boolean updateSongsTags(String albumName, String albumArtist, String genre) {
        try {
            if (albumSongs == null || albumSongs.isEmpty()) {
                return true;
            }
            
            // 使用 ContentResolver 更新媒体库中的歌曲信息
            ContentResolver contentResolver = requireContext().getContentResolver();
            
            for (Song song : albumSongs) {
                ContentValues values = new ContentValues();
                values.put(MediaStore.Audio.Media.ALBUM, albumName);
                values.put(MediaStore.Audio.Media.ARTIST, albumArtist);
                if (!TextUtils.isEmpty(genre)) {
                    values.put(MediaStore.Audio.Media.GENRE, genre);
                }
                
                // 更新媒体库
                contentResolver.update(
                        MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
                        values,
                        MediaStore.Audio.Media._ID + " = ?",
                        new String[]{String.valueOf(song.getId())}
                );
                
                // 更新本地数据库
                song.setAlbum(albumName);
                song.setArtist(albumArtist);
                if (!TextUtils.isEmpty(genre)) {
                    song.setGenre(genre);
                }
                song.save();
            }
            
            return true;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    /**
     * 保存专辑封面
     */
    private boolean saveAlbumCover() {
        if (selectedImageUri == null) {
            return true; // 没有选择新图片，认为成功
        }
        
        try {
            // 获取应用的私有存储目录
            File albumArtDir = new File(requireContext().getFilesDir(), "album_art");
            if (!albumArtDir.exists()) {
                albumArtDir.mkdirs();
            }
            
            // 创建专辑封面文件
            File albumArtFile = new File(albumArtDir, currentAlbum.getAlbumId() + ".jpg");
            
            // 复制选中的图片到专辑封面文件
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

    @Override
    public boolean autoHideBottomNavigation() {
        return true;
    }
} 
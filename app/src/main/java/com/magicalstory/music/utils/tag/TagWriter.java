package com.magicalstory.music.utils.tag;

import android.content.Context;
import android.graphics.Bitmap;
import android.os.Build;
import android.util.Log;

import androidx.annotation.NonNull;

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
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * 音频标签写入工具类
 */
public class TagWriter {
    private static final String TAG = "TagWriter";
    private static final ExecutorService executorService = Executors.newCachedThreadPool();

    /**
     * 音频标签信息类
     */
    public static class AudioTagInfo {
        private final List<String> filePaths;
        private final java.util.Map<org.jaudiotagger.tag.FieldKey, String> fieldKeyValueMap;
        private final ArtworkInfo artworkInfo;

        public AudioTagInfo(List<String> filePaths,
                            java.util.Map<org.jaudiotagger.tag.FieldKey, String> fieldKeyValueMap,
                            ArtworkInfo artworkInfo) {
            this.filePaths = filePaths;
            this.fieldKeyValueMap = fieldKeyValueMap;
            this.artworkInfo = artworkInfo;
        }

        public List<String> getFilePaths() {
            return filePaths;
        }

        public java.util.Map<org.jaudiotagger.tag.FieldKey, String> getFieldKeyValueMap() {
            return fieldKeyValueMap;
        }

        public ArtworkInfo getArtworkInfo() {
            return artworkInfo;
        }
    }

    /**
     * 封面信息类
     */
    public static class ArtworkInfo {
        private final long albumId;
        private final Bitmap artwork;

        public ArtworkInfo(long albumId, Bitmap artwork) {
            this.albumId = albumId;
            this.artwork = artwork;
        }

        public long getAlbumId() {
            return albumId;
        }

        public Bitmap getArtwork() {
            return artwork;
        }
    }


    /**
     * 写入标签到文件（Android 11及以上版本）
     */
    public static List<File> writeTagsToFilesR(Context context, AudioTagInfo info) {
        List<File> cacheFiles = new java.util.ArrayList<>();

        try {
            Artwork artwork = null;
            File albumArtFile = null;

            // 处理封面
            if (info.getArtworkInfo() != null && info.getArtworkInfo().getArtwork() != null) {
                try {
                    albumArtFile = createAlbumArtFile(context);
                    info.getArtworkInfo().getArtwork().compress(
                            Bitmap.CompressFormat.JPEG,
                            100,
                            new java.io.FileOutputStream(albumArtFile)
                    );
                    artwork = AndroidArtwork.createArtworkFromFile(albumArtFile);
                } catch (IOException e) {
                    Log.e(TAG, "创建封面文件失败", e);
                }
            }

            boolean wroteArtwork = false;
            boolean deletedArtwork = false;

            // 处理每个音频文件
            for (String filePath : info.getFilePaths()) {
                try {
                    File originFile = new File(filePath);
                    File cacheFile = new File(context.getCacheDir(), originFile.getName());
                    cacheFiles.add(cacheFile);

                    // 复制原文件到缓存
                    try (java.io.InputStream input = new java.io.FileInputStream(originFile);
                         java.io.OutputStream output = new java.io.FileOutputStream(cacheFile)) {
                        byte[] buffer = new byte[4096];
                        int bytesRead;
                        while ((bytesRead = input.read(buffer)) != -1) {
                            output.write(buffer, 0, bytesRead);
                        }
                    }

                    // 读取音频文件并修改标签
                    AudioFile audioFile = AudioFileIO.read(cacheFile);
                    Tag tag = audioFile.getTagOrCreateAndSetDefault();

                    // 写入字段信息
                    if (info.getFieldKeyValueMap() != null) {
                        for (java.util.Map.Entry<org.jaudiotagger.tag.FieldKey, String> entry : info.getFieldKeyValueMap().entrySet()) {
                            try {
                                org.jaudiotagger.tag.FieldKey key = entry.getKey();
                                String newValue = entry.getValue();
                                String currentValue = tag.getFirst(key);

                                if (!newValue.equals(currentValue)) {
                                    if (newValue.isEmpty()) {
                                        tag.deleteField(key);
                                    } else {
                                        tag.setField(key, newValue);
                                    }
                                }
                            } catch (FieldDataInvalidException e) {
                                Log.e(TAG, "字段数据无效: " + entry.getKey(), e);
                                return new java.util.ArrayList<>();
                            } catch (Exception e) {
                                Log.e(TAG, "写入字段失败: " + entry.getKey(), e);
                            }
                        }
                    }

                    // 处理封面
                    if (info.getArtworkInfo() != null) {
                        if (info.getArtworkInfo().getArtwork() == null) {
                            tag.deleteArtworkField();
                            deletedArtwork = true;
                        } else if (artwork != null) {
                            tag.deleteArtworkField();
                            tag.setField(artwork);
                            wroteArtwork = true;
                        }
                    }

                    audioFile.commit();

                    // 打印原始数据到控制台
                    System.out.println("已写入标签到缓存文件: " + cacheFile.getAbsolutePath());

                } catch (CannotReadException | IOException | CannotWriteException |
                         ReadOnlyFileException | InvalidAudioFrameException e) {
                    Log.e(TAG, "处理音频文件失败: " + filePath, e);
                }
            }

            // 处理封面数据库
            if (wroteArtwork) {
                insertAlbumArt(context, info.getArtworkInfo().getAlbumId(), albumArtFile.getPath());
            } else if (deletedArtwork) {
                deleteAlbumArt(context, info.getArtworkInfo().getAlbumId());
            }

        } catch (Exception e) {
            Log.e(TAG, "写入标签失败", e);
        }

        return cacheFiles;
    }

    /**
     * 写入标签到文件（Android 10及以下版本）
     */
    public static void writeTagsToFiles(Context context, AudioTagInfo info) {
        try {
            Artwork artwork = null;
            File albumArtFile = null;

            // 处理封面
            if (info.getArtworkInfo() != null && info.getArtworkInfo().getArtwork() != null) {
                try {
                    albumArtFile = createAlbumArtFile(context);
                    info.getArtworkInfo().getArtwork().compress(
                            Bitmap.CompressFormat.JPEG,
                            100,
                            new java.io.FileOutputStream(albumArtFile)
                    );
                    artwork = AndroidArtwork.createArtworkFromFile(albumArtFile);
                } catch (IOException e) {
                    Log.e(TAG, "创建封面文件失败", e);
                }
            }

            boolean wroteArtwork = false;
            boolean deletedArtwork = false;

            // 处理每个音频文件
            for (String filePath : info.getFilePaths()) {
                try {
                    AudioFile audioFile = AudioFileIO.read(new File(filePath));
                    Tag tag = audioFile.getTagOrCreateAndSetDefault();

                    // 写入字段信息
                    if (info.getFieldKeyValueMap() != null) {
                        for (java.util.Map.Entry<org.jaudiotagger.tag.FieldKey, String> entry : info.getFieldKeyValueMap().entrySet()) {
                            try {
                                org.jaudiotagger.tag.FieldKey key = entry.getKey();
                                String newValue = entry.getValue();
                                String currentValue = tag.getFirst(key);

                                if (!newValue.equals(currentValue)) {
                                    if (newValue.isEmpty()) {
                                        tag.deleteField(key);
                                    } else {
                                        tag.setField(key, newValue);
                                    }
                                }
                            } catch (FieldDataInvalidException e) {
                                Log.e(TAG, "字段数据无效: " + entry.getKey(), e);
                                return;
                            } catch (Exception e) {
                                Log.e(TAG, "写入字段失败: " + entry.getKey(), e);
                            }
                        }
                    }

                    // 处理封面
                    if (info.getArtworkInfo() != null) {
                        if (info.getArtworkInfo().getArtwork() == null) {
                            tag.deleteArtworkField();
                            deletedArtwork = true;
                        } else if (artwork != null) {
                            tag.deleteArtworkField();
                            tag.setField(artwork);
                            wroteArtwork = true;
                        }
                    }

                    audioFile.commit();

                    // 打印原始数据到控制台
                    System.out.println("已写入标签到文件: " + filePath);

                } catch (CannotReadException | IOException | CannotWriteException |
                         ReadOnlyFileException | InvalidAudioFrameException e) {
                    Log.e(TAG, "处理音频文件失败: " + filePath, e);
                }
            }

            // 处理封面数据库
            if (wroteArtwork) {
                insertAlbumArt(context, info.getArtworkInfo().getAlbumId(), albumArtFile.getPath());
            } else if (deletedArtwork) {
                deleteAlbumArt(context, info.getArtworkInfo().getAlbumId());
            }

            // 扫描文件
            scan(context, info.getFilePaths());

        } catch (Exception e) {
            Log.e(TAG, "写入标签失败", e);
        }
    }

    /**
     * 创建专辑封面文件
     */
    private static File createAlbumArtFile(Context context) throws IOException {
        File albumArtDir = new File(context.getCacheDir(), "album_art");
        if (!albumArtDir.exists()) {
            albumArtDir.mkdirs();
        }
        return new File(albumArtDir, "temp_cover.jpg");
    }

    /**
     * 插入专辑封面到数据库
     */
    private static void insertAlbumArt(Context context, long albumId, String albumArtPath) {
        try {
            // 这里可以添加将封面信息保存到数据库的逻辑
            Log.d(TAG, "插入专辑封面: albumId=" + albumId + ", path=" + albumArtPath);
        } catch (Exception e) {
            Log.e(TAG, "插入专辑封面失败", e);
        }
    }

    /**
     * 从数据库删除专辑封面
     */
    private static void deleteAlbumArt(Context context, long albumId) {
        try {
            // 这里可以添加从数据库删除封面信息的逻辑
            Log.d(TAG, "删除专辑封面: albumId=" + albumId);
        } catch (Exception e) {
            Log.e(TAG, "删除专辑封面失败", e);
        }
    }

    /**
     * 扫描文件
     */
    private static void scan(Context context, List<String> filePaths) {
        try {
            // 这里可以添加文件扫描的逻辑，通知系统媒体库更新
            Log.d(TAG, "扫描文件: " + filePaths.size() + " 个文件");
        } catch (Exception e) {
            Log.e(TAG, "扫描文件失败", e);
        }
    }
} 
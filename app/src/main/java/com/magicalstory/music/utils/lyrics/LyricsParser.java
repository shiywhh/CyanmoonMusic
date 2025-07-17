package com.magicalstory.music.utils.lyrics;

import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.provider.MediaStore;
import android.util.Log;

import com.magicalstory.music.model.LyricLine;

import org.jaudiotagger.audio.AudioFile;
import org.jaudiotagger.audio.AudioFileIO;
import org.jaudiotagger.tag.FieldKey;
import org.jaudiotagger.tag.Tag;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 歌词解析工具类
 */
public class LyricsParser {

    private static final String TAG = "LyricsParser";

    /**
     * 从歌曲文件路径解析歌词
     */
    public static List<LyricLine> parseLyricsFromSong(Context context, String songPath) {
        List<LyricLine> lyrics = new ArrayList<>();

        if (songPath == null || songPath.isEmpty()) {
            Log.d(TAG, "歌曲路径为空");
            return lyrics;
        }

        try {
            Log.d(TAG, "开始解析歌词，歌曲路径: " + songPath);

            // 1. 首先尝试从歌曲文件本身提取内嵌歌词
            lyrics = extractLyricsFromAudioFile(context, songPath);
            Log.d(TAG, "从音频文件提取歌词结果: " + lyrics.size() + " 行");

            // 2. 如果歌曲文件没有内嵌歌词，尝试查找同名的.lrc文件
            if (lyrics.isEmpty()) {
                lyrics = parseLrcFile(context, songPath);
                Log.d(TAG, "从LRC文件提取歌词结果: " + lyrics.size() + " 行");
            }

            // 3. 如果还是没有歌词，可以在这里添加其他歌词提取方法
            // 比如从网络下载歌词等

            Log.d(TAG, "最终歌词解析结果: " + lyrics.size() + " 行");
        } catch (Exception e) {
            Log.e(TAG, "解析歌词失败: " + e.getMessage());
        }

        return lyrics;
    }


    /**
     * 从音频文件中提取内嵌歌词
     */
    private static List<LyricLine> extractLyricsFromAudioFile(Context context, String songPath) {
        List<LyricLine> lyrics = new ArrayList<>();

        try {
            File tempFile = new File(songPath);

            Log.d(TAG, "临时文件创建成功: " + tempFile.getAbsolutePath() + ", 文件大小: " + tempFile.length() + " bytes");
            AudioFile file = AudioFileIO.read(tempFile);
            Tag tag = file.getTag();

            if (tag != null) {
                Log.d(TAG, "音频文件标签读取成功，开始提取歌词字段");
                System.out.println("tag = " + tag);
                // 尝试获取各种可能的歌词字段
                String[] possibleLyricFields = {
                        "USLT",  // Unsynchronized Lyrics
                        "SYLT",  // Synchronized Lyrics
                        "LYRICS",
                        "lyrics",
                        "Lyrics",
                        "UNSYNCEDLYRICS",
                        "unsyncedlyrics",
                        "UnsyncedLyrics",
                        "SYNCEDLYRICS",
                        "syncedlyrics",
                        "SyncedLyrics",
                        "COMMENT",
                        "comment",
                        "Comment"
                };

                for (String fieldName : possibleLyricFields) {
                    try {
                        String lyricsText = tag.getFirst(fieldName);
                        if (lyricsText != null && !lyricsText.trim().isEmpty()) {
                            Log.d(TAG, "找到歌词字段: " + fieldName + ", 内容长度: " + lyricsText.length());
                            List<LyricLine> parsedLyrics = parseLyricsText(lyricsText);
                            if (!parsedLyrics.isEmpty()) {
                                lyrics.addAll(parsedLyrics);
                                Log.d(TAG, "从字段 " + fieldName + " 提取到歌词，共 " + parsedLyrics.size() + " 行");
                                break;
                            } else {
                                Log.d(TAG, "字段 " + fieldName + " 内容不为空但解析后为空");
                            }
                        }
                    } catch (Exception e) {
                        // 忽略单个字段的错误
                        Log.d(TAG, "字段 " + fieldName + " 提取失败: " + e.getMessage());
                    }
                }

                // 如果没有找到歌词，尝试获取评论字段作为歌词
                if (lyrics.isEmpty()) {
                    try {
                        String comment = tag.getFirst(FieldKey.COMMENT);
                        if (comment != null && !comment.trim().isEmpty()) {
                            Log.d(TAG, "尝试从评论字段提取歌词，评论长度: " + comment.length());
                            // 检查评论是否包含歌词格式
                            if (comment.contains("[") && comment.contains("]")) {
                                List<LyricLine> parsedLyrics = parseLyricsText(comment);
                                if (!parsedLyrics.isEmpty()) {
                                    lyrics.addAll(parsedLyrics);
                                    Log.d(TAG, "从评论字段提取到歌词，共 " + parsedLyrics.size() + " 行");
                                }
                            }
                        }
                    } catch (Exception e) {
                        Log.d(TAG, "从评论字段提取歌词失败: " + e.getMessage());
                    }
                }
            } else {
                Log.d(TAG, "音频文件没有标签信息");
            }

        } catch (Exception e) {
            Log.e(TAG, "从音频文件提取歌词失败: " + e.getMessage(), e);
        }

        return lyrics;
    }

    /**
     * 根据文件路径获取音频文件的URI
     */
    private static Uri getAudioUriFromPath(Context context, String songPath) {
        try {
            // 从MediaStore查询文件
            String[] projection = {MediaStore.Audio.Media._ID};
            String selection = MediaStore.Audio.Media.DATA + "=?";
            String[] selectionArgs = {songPath};

            Cursor cursor = context.getContentResolver().query(
                    MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
                    projection,
                    selection,
                    selectionArgs,
                    null
            );

            if (cursor != null && cursor.moveToFirst()) {
                long id = cursor.getLong(cursor.getColumnIndexOrThrow(MediaStore.Audio.Media._ID));
                cursor.close();
                return Uri.withAppendedPath(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, String.valueOf(id));
            }

            if (cursor != null) {
                cursor.close();
            }

            Log.d(TAG, "在MediaStore中未找到文件: " + songPath);
            return null;
        } catch (Exception e) {
            Log.e(TAG, "获取音频URI失败: " + e.getMessage());
            return null;
        }
    }

    /**
     * 从URI创建临时文件
     */
    private static File createTempFileFromUri(Context context, Uri uri) {
        try {
            InputStream inputStream = context.getContentResolver().openInputStream(uri);
            if (inputStream == null) {
                Log.d(TAG, "无法打开音频文件流");
                return null;
            }

            // 根据原始文件路径确定正确的扩展名
            String originalPath = uri.toString();
            String extension = ".mp3"; // 默认扩展名

            // 尝试从URI中获取原始文件名
            try {
                Cursor cursor = context.getContentResolver().query(uri, null, null, null, null);
                if (cursor != null && cursor.moveToFirst()) {
                    int displayNameIndex = cursor.getColumnIndex(MediaStore.MediaColumns.DISPLAY_NAME);
                    if (displayNameIndex >= 0) {
                        String displayName = cursor.getString(displayNameIndex);
                        if (displayName != null && displayName.contains(".")) {
                            extension = "." + displayName.substring(displayName.lastIndexOf(".") + 1);
                        }
                    }
                    cursor.close();
                }
            } catch (Exception e) {
                Log.d(TAG, "无法获取原始文件名，使用默认扩展名: " + e.getMessage());
            }

            // 创建临时文件，使用正确的扩展名
            File tempFile = File.createTempFile("audio_", extension, context.getCacheDir());
            java.io.FileOutputStream outputStream = new java.io.FileOutputStream(tempFile);

            byte[] buffer = new byte[8192];
            int bytesRead;
            while ((bytesRead = inputStream.read(buffer)) != -1) {
                outputStream.write(buffer, 0, bytesRead);
            }

            inputStream.close();
            outputStream.close();

            Log.d(TAG, "创建临时文件成功: " + tempFile.getAbsolutePath() + ", 扩展名: " + extension);
            return tempFile;
        } catch (Exception e) {
            Log.e(TAG, "创建临时文件失败: " + e.getMessage());
            return null;
        }
    }

    /**
     * 解析.lrc文件
     */
    private static List<LyricLine> parseLrcFile(Context context, String songPath) {
        List<LyricLine> lyrics = new ArrayList<>();

        try {
            // 构造.lrc文件路径
            String lrcPath = songPath.substring(0, songPath.lastIndexOf('.')) + ".lrc";

            // 尝试通过ContentResolver访问LRC文件
            Uri lrcUri = getLrcFileUri(context, lrcPath);
            if (lrcUri == null) {
                Log.d(TAG, "未找到LRC文件: " + lrcPath);
                return lyrics;
            }

            // 使用ContentResolver读取LRC文件
            InputStream inputStream = context.getContentResolver().openInputStream(lrcUri);
            if (inputStream == null) {
                Log.d(TAG, "无法打开LRC文件流");
                return lyrics;
            }

            BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream));
            String line;
            Pattern timePattern = Pattern.compile("\\[(\\d{2}):(\\d{2})\\.(\\d{2})\\](.*)");

            while ((line = reader.readLine()) != null) {
                Matcher matcher = timePattern.matcher(line);
                if (matcher.find()) {
                    int minutes = Integer.parseInt(matcher.group(1));
                    int seconds = Integer.parseInt(matcher.group(2));
                    int centiseconds = Integer.parseInt(matcher.group(3));
                    String content = matcher.group(4).trim();

                    if (!content.isEmpty()) {
                        long timeMs = (minutes * 60 + seconds) * 1000 + centiseconds * 10;
                        lyrics.add(new LyricLine(timeMs, content));
                    }
                }
            }

            reader.close();
            inputStream.close();

        } catch (IOException e) {
            Log.e(TAG, "解析.lrc文件失败: " + e.getMessage());
        }

        return lyrics;
    }

    /**
     * 获取LRC文件的URI
     */
    private static Uri getLrcFileUri(Context context, String lrcPath) {
        try {
            // 尝试直接通过文件路径获取URI
            File lrcFile = new File(lrcPath);
            if (lrcFile.exists()) {
                // 直接使用文件路径，如果应用有权限的话
                try {
                    return Uri.fromFile(lrcFile);
                } catch (Exception e) {
                    Log.d(TAG, "无法直接获取文件URI: " + e.getMessage());
                }
            }

            // 如果直接路径不存在，尝试在MediaStore中查找
            String[] projection = {MediaStore.Files.FileColumns._ID};
            String selection = MediaStore.Files.FileColumns.DATA + "=?";
            String[] selectionArgs = {lrcPath};

            Cursor cursor = context.getContentResolver().query(
                    MediaStore.Files.getContentUri("external"),
                    projection,
                    selection,
                    selectionArgs,
                    null
            );

            if (cursor != null && cursor.moveToFirst()) {
                long id = cursor.getLong(cursor.getColumnIndexOrThrow(MediaStore.Files.FileColumns._ID));
                cursor.close();
                return Uri.withAppendedPath(MediaStore.Files.getContentUri("external"), String.valueOf(id));
            }

            if (cursor != null) {
                cursor.close();
            }

            return null;
        } catch (Exception e) {
            Log.e(TAG, "获取LRC文件URI失败: " + e.getMessage());
            return null;
        }
    }


    /**
     * 解析歌词文本
     */
    private static List<LyricLine> parseLyricsText(String lyricsText) {
        List<LyricLine> lyrics = new ArrayList<>();

        try {
            String[] lines = lyricsText.split("\n");
            Pattern timePattern = Pattern.compile("\\[(\\d{2}):(\\d{2})\\.(\\d{2})\\](.*)");

            for (String line : lines) {
                line = line.trim();
                if (line.isEmpty()) {
                    continue;
                }

                Matcher matcher = timePattern.matcher(line);
                if (matcher.find()) {
                    int minutes = Integer.parseInt(matcher.group(1));
                    int seconds = Integer.parseInt(matcher.group(2));
                    int centiseconds = Integer.parseInt(matcher.group(3));
                    String content = matcher.group(4).trim();

                    if (!content.isEmpty()) {
                        long timeMs = (minutes * 60 + seconds) * 1000 + centiseconds * 10;
                        lyrics.add(new LyricLine(timeMs, content));
                    }
                } else {
                    // 如果没有时间标签，可能是纯文本歌词
                    // 跳过一些常见的元数据行
                    if (!line.startsWith("[ti:") && !line.startsWith("[ar:") &&
                            !line.startsWith("[al:") && !line.startsWith("[by:") &&
                            !line.startsWith("[offset:") && !line.startsWith("[length:")) {

                        String content = line.trim();
                        if (!content.isEmpty()) {
                            lyrics.add(new LyricLine(0, content));
                        }
                    }
                }
            }

            Log.d(TAG, "解析歌词文本成功，共 " + lyrics.size() + " 行");
        } catch (Exception e) {
            Log.e(TAG, "解析歌词文本失败: " + e.getMessage());
        }

        return lyrics;
    }
} 
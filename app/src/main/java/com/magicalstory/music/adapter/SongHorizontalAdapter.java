package com.magicalstory.music.adapter;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.GradientDrawable;
import android.os.Handler;
import android.os.Looper;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.media3.common.util.UnstableApi;
import androidx.palette.graphics.Palette;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.resource.bitmap.RoundedCorners;
import com.bumptech.glide.request.RequestOptions;
import com.bumptech.glide.request.target.CustomTarget;
import com.bumptech.glide.request.transition.Transition;
import com.magicalstory.music.MainActivity;
import com.magicalstory.music.R;
import com.magicalstory.music.databinding.ItemSongHorizontalBinding;
import com.magicalstory.music.databinding.ItemSongSquareBinding;
import com.magicalstory.music.model.Song;
import com.magicalstory.music.utils.glide.Glide2;

import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * 歌曲横向滑动列表适配器
 */
@UnstableApi
public class SongHorizontalAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

    // ViewType常量
    private static final int VIEW_TYPE_HORIZONTAL = 0;
    private static final int VIEW_TYPE_SQUARE = 1;

    private Context context;
    private List<Song> songList;
    private List<GradientDrawable> gradientDrawables;
    private List<Integer> colors;
    private OnItemClickListener onItemClickListener;
    private boolean useSquareLayout = false; // 默认使用横向布局

    // 线程池和主线程Handler
    private ExecutorService executorService;
    private Handler mainHandler;

    public interface OnItemClickListener {
        void onItemClick(Song song, int position);
    }

    public SongHorizontalAdapter(Context context, List<Song> songList) {
        this.context = context;
        this.songList = songList;
        this.executorService = Executors.newCachedThreadPool();
        this.mainHandler = new Handler(Looper.getMainLooper());
    }

    public SongHorizontalAdapter(Context context, List<Song> songList, boolean useSquareLayout) {
        this.context = context;
        this.songList = songList;
        this.useSquareLayout = useSquareLayout;
        this.executorService = Executors.newCachedThreadPool();
        this.mainHandler = new Handler(Looper.getMainLooper());
    }

    public SongHorizontalAdapter(Context context, List<Song> songList, List<GradientDrawable> gradientDrawables) {
        this.context = context;
        this.songList = songList;
        this.gradientDrawables = gradientDrawables;
        this.executorService = Executors.newCachedThreadPool();
        this.mainHandler = new Handler(Looper.getMainLooper());
    }

    public SongHorizontalAdapter(Context context, List<Song> songList, List<GradientDrawable> gradientDrawables, boolean useSquareLayout) {
        this.context = context;
        this.songList = songList;
        this.gradientDrawables = gradientDrawables;
        this.useSquareLayout = useSquareLayout;
        this.executorService = Executors.newCachedThreadPool();
        this.mainHandler = new Handler(Looper.getMainLooper());
    }

    public SongHorizontalAdapter(Context context, List<Song> songList, List<GradientDrawable> gradientDrawables, List<Integer> colors) {
        this.context = context;
        this.songList = songList;
        this.gradientDrawables = gradientDrawables;
        this.colors = colors;
        this.executorService = Executors.newCachedThreadPool();
        this.mainHandler = new Handler(Looper.getMainLooper());
    }

    public void setOnItemClickListener(OnItemClickListener listener) {
        this.onItemClickListener = listener;
    }

    @Override
    public int getItemViewType(int position) {
        return useSquareLayout ? VIEW_TYPE_SQUARE : VIEW_TYPE_HORIZONTAL;
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        if (viewType == VIEW_TYPE_SQUARE) {
            ItemSongSquareBinding binding = ItemSongSquareBinding.inflate(
                    LayoutInflater.from(context), parent, false);
            return new SquareViewHolder(binding);
        } else {
            ItemSongHorizontalBinding binding = ItemSongHorizontalBinding.inflate(
                    LayoutInflater.from(context), parent, false);
            return new ViewHolder(binding);
        }
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        Song song = songList.get(position);
        if (holder instanceof SquareViewHolder squareHolder) {
            // 设置歌曲标题
            squareHolder.binding.tvTitle.setText(song.getTitle());

            // 设置艺术家
            squareHolder.binding.tvArtist.setText(song.getArtist());

            // 加载专辑封面
            loadAlbumArtSquare(squareHolder.binding.cover, song);

            // 设置点击事件
            squareHolder.binding.cover.setOnClickListener(v -> {
                if (onItemClickListener != null) {
                    onItemClickListener.onItemClick(song, position);
                }

                // 播放歌曲
                if (context instanceof MainActivity mainActivity) {
                    mainActivity.playFromPlaylist(songList, position);
                }
            });
        } else if (holder instanceof ViewHolder horizontalHolder) {
            // 设置歌曲标题
            horizontalHolder.binding.tvTitle.setText(song.getTitle());

            // 设置艺术家
            horizontalHolder.binding.tvArtist.setText(song.getArtist());

            // 加载专辑封面
            loadAlbumArt(horizontalHolder.binding.cover, song, horizontalHolder, position);

            // 设置点击事件
            horizontalHolder.itemView.setOnClickListener(v -> {
                if (onItemClickListener != null) {
                    onItemClickListener.onItemClick(song, position);
                }

                // 播放歌曲
                if (context instanceof MainActivity mainActivity) {
                    mainActivity.playFromPlaylist(songList, position);
                }
            });
        }
    }

    private void loadAlbumArt(ImageView imageView, Song song, ViewHolder holder, int position) {
        // 构建专辑封面URI
        String albumArtUri = null;
        if (song.getAlbumId() > 0) {
            albumArtUri = "content://media/external/audio/albumart/" + song.getAlbumId();
        }

        // 使用预创建的gradient drawable（只有在有gradientDrawables时才设置）
        if (gradientDrawables != null && position < gradientDrawables.size()) {
            holder.binding.gradientOverlay.setBackground(gradientDrawables.get(position));
            holder.binding.gradientOverlay2.setBackground(gradientDrawables.get(position));
            holder.binding.gradientOverlay3.setBackground(gradientDrawables.get(position));

            // 设置卡片背景颜色（只有在有colors时才设置）
            if (colors != null && position < colors.size()) {
                holder.binding.getRoot().setCardBackgroundColor(colors.get(position));
            }
        }

        Glide2.loadImage(context, imageView, gradientDrawables.isEmpty() ? R.drawable.place_holder_song : albumArtUri, R.drawable.place_holder_song);

    }

    private void loadAlbumArtSquare(ImageView imageView, Song song) {
        // 构建专辑封面URI
        String albumArtUri = null;
        if (song.getAlbumId() > 0) {
            albumArtUri = "content://media/external/audio/albumart/" + song.getAlbumId();
        }
        Glide2.loadImage(context, imageView, albumArtUri, R.drawable.place_holder_song);
    }


    @Override
    public int getItemCount() {
        return songList != null ? songList.size() : 0;
    }

    public void updateData(List<Song> newSongList) {
        this.songList = newSongList;
        notifyDataSetChanged();
    }

    public void updateData(List<Song> newSongList, List<GradientDrawable> newGradientDrawables) {
        this.songList = newSongList;
        this.gradientDrawables = newGradientDrawables;
        notifyDataSetChanged();
    }

    public void updateData(List<Song> newSongList, List<GradientDrawable> newGradientDrawables, List<Integer> newColors) {
        this.songList = newSongList;
        this.gradientDrawables = newGradientDrawables;
        this.colors = newColors;
        notifyDataSetChanged();
    }

    /**
     * 获取当前的歌曲列表
     * @return 歌曲列表
     */
    public List<Song> getSongList() {
        return songList;
    }

    // 释放资源
    public void release() {
        if (executorService != null && !executorService.isShutdown()) {
            executorService.shutdown();
        }
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        ItemSongHorizontalBinding binding;

        public ViewHolder(@NonNull ItemSongHorizontalBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }
    }

    static class SquareViewHolder extends RecyclerView.ViewHolder {
        ItemSongSquareBinding binding;

        public SquareViewHolder(@NonNull ItemSongSquareBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }
    }
}
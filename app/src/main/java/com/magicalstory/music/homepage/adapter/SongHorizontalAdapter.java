package com.magicalstory.music.homepage.adapter;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.GradientDrawable;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.palette.graphics.Palette;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.resource.bitmap.RoundedCorners;
import com.bumptech.glide.request.RequestOptions;
import com.bumptech.glide.request.target.CustomTarget;
import com.bumptech.glide.request.transition.Transition;
import com.magicalstory.music.R;
import com.magicalstory.music.databinding.ItemSongHorizontalBinding;
import com.magicalstory.music.databinding.ItemSongSquareBinding;
import com.magicalstory.music.model.Song;
import com.magicalstory.music.utils.glide.Glide2;

import java.util.List;

/**
 * 歌曲横向滑动列表适配器
 */
public class SongHorizontalAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {
    
    // ViewType常量
    private static final int VIEW_TYPE_HORIZONTAL = 0;
    private static final int VIEW_TYPE_SQUARE = 1;
    
    private Context context;
    private List<Song> songList;
    private OnItemClickListener onItemClickListener;
    private boolean useSquareLayout = false; // 默认使用横向布局
    
    public interface OnItemClickListener {
        void onItemClick(Song song, int position);
    }
    
    public SongHorizontalAdapter(Context context, List<Song> songList) {
        this.context = context;
        this.songList = songList;
    }
    
    public SongHorizontalAdapter(Context context, List<Song> songList, boolean useSquareLayout) {
        this.context = context;
        this.songList = songList;
        this.useSquareLayout = useSquareLayout;
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
        if (holder instanceof SquareViewHolder) {
            SquareViewHolder squareHolder = (SquareViewHolder) holder;
            // 设置歌曲标题
            squareHolder.binding.tvTitle.setText(song.getTitle());
            
            // 设置艺术家
            squareHolder.binding.tvArtist.setText(song.getArtist());
            
            // 加载专辑封面
            loadAlbumArtSquare(squareHolder.binding.cover, song);
            
            // 设置点击事件
            squareHolder.itemView.setOnClickListener(v -> {
                if (onItemClickListener != null) {
                    onItemClickListener.onItemClick(song, position);
                }
            });
        } else if (holder instanceof ViewHolder) {
            ViewHolder horizontalHolder = (ViewHolder) holder;
            // 设置歌曲标题
            horizontalHolder.binding.tvTitle.setText(song.getTitle());
            
            // 设置艺术家
            horizontalHolder.binding.tvArtist.setText(song.getArtist());
            
            // 加载专辑封面
            loadAlbumArt(horizontalHolder.binding.cover, song, horizontalHolder);
            
            // 设置点击事件
            horizontalHolder.itemView.setOnClickListener(v -> {
                if (onItemClickListener != null) {
                    onItemClickListener.onItemClick(song, position);
                }
            });
        }
    }
    
    private void loadAlbumArt(ImageView imageView, Song song, ViewHolder holder) {
        // 构建专辑封面URI
        String albumArtUri = null;
        if (song.getAlbumId() > 0) {
            albumArtUri = "content://media/external/audio/albumart/" + song.getAlbumId();
        }

        // 使用Glide加载图片并在完成后进行Palette取色
        Glide.with(context)
                .asBitmap()
                .load(albumArtUri)
                .placeholder(R.drawable.place_holder_song)
                .error(R.drawable.place_holder_song)
                .into(new CustomTarget<Bitmap>() {
                    @Override
                    public void onResourceReady(@NonNull Bitmap resource, @Nullable Transition<? super Bitmap> transition) {
                        imageView.setImageBitmap(resource);

                        
                        // 使用Palette进行取色
                        Palette.from(resource).generate(palette -> {
                            if (palette != null) {
                                applyPaletteColors(palette, holder);
                            }
                        });
                    }

                    @Override
                    public void onLoadCleared(@Nullable Drawable placeholder) {
                        imageView.setImageDrawable(placeholder);
                        // 加载失败时使用默认颜色
                        applyDefaultColors(holder);
                    }
                });
    }
    
    private void loadAlbumArtSquare(ImageView imageView, Song song) {
        // 构建专辑封面URI
        String albumArtUri = null;
        if (song.getAlbumId() > 0) {
            albumArtUri = "content://media/external/audio/albumart/" + song.getAlbumId();
        }
        Glide2.loadImage(context, imageView, albumArtUri, R.drawable.place_holder_song);
    }
    
    private void applyPaletteColors(Palette palette, ViewHolder holder) {
        // 获取深色调色板颜色
        int darkVibrantColor = palette.getDarkVibrantColor(Color.parseColor("#2C2C2C"));
        int darkMutedColor = palette.getDarkMutedColor(Color.parseColor("#1A1A1A"));
        
        // 设置CardView背景为深色
        holder.binding.getRoot().setCardBackgroundColor(darkMutedColor);
        
        // 创建渐变罩层
        createGradientOverlay(holder, darkVibrantColor);
    }
    
    private void applyDefaultColors(ViewHolder holder) {
        // 默认深色配色
        int defaultDarkColor = Color.parseColor("#2C2C2C");
        int defaultBackgroundColor = Color.parseColor("#1A1A1A");
        
        holder.binding.getRoot().setCardBackgroundColor(defaultBackgroundColor);
        holder.binding.tvTitle.setTextColor(defaultDarkColor);
        holder.binding.tvArtist.setTextColor(defaultDarkColor);
        
        createGradientOverlay(holder, defaultDarkColor);
    }
    
    private void createGradientOverlay(ViewHolder holder, int color) {
        // 创建左右渐变drawable
        GradientDrawable gradientDrawable = new GradientDrawable(
                GradientDrawable.Orientation.LEFT_RIGHT,
                new int[]{color, Color.TRANSPARENT}
        );
        
        // 应用渐变背景并显示罩层
        holder.binding.gradientOverlay.setBackground(gradientDrawable);
        holder.binding.gradientOverlay.setVisibility(View.VISIBLE);
    }

    
    @Override
    public int getItemCount() {
        return songList != null ? songList.size() : 0;
    }
    
    public void updateData(List<Song> newSongList) {
        this.songList = newSongList;
        notifyDataSetChanged();
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
package com.magicalstory.music.player;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.magicalstory.music.R;
import com.magicalstory.music.databinding.ItemMiniPlayerBinding;
import com.magicalstory.music.model.Song;
import com.magicalstory.music.utils.glide.GlideUtils;

import java.util.List;

/**
 * MiniPlayer适配器，用于左右滑动切换歌曲
 */
public class MiniPlayerAdapter extends RecyclerView.Adapter<MiniPlayerAdapter.ViewHolder> {

    private Context context;
    private List<Song> playlist;
    private OnSongChangeListener onSongChangeListener;
    private int currentPosition = 0;

    public interface OnSongChangeListener {
        void onSongChanged(Song song, int position);
        void onSongItemClicked(Song song, int position);
    }

    public MiniPlayerAdapter(Context context, List<Song> playlist) {
        this.context = context;
        this.playlist = playlist;
    }

    public void setOnSongChangeListener(OnSongChangeListener listener) {
        this.onSongChangeListener = listener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        ItemMiniPlayerBinding binding = ItemMiniPlayerBinding.inflate(
                LayoutInflater.from(context), parent, false);
        return new ViewHolder(binding);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Song song = playlist.get(position);
        
        // 设置歌曲信息
        holder.binding.songName.setText(song.getTitle());
        holder.binding.artistName.setText(song.getArtist());
        
        // 加载封面
        GlideUtils.loadAlbumCover(context, song.getAlbumId(), holder.binding.coverImage);
        
        // 设置渐变动画（当前播放的歌曲高亮显示）
        if (position == currentPosition) {
            holder.itemView.setAlpha(1.0f);
            holder.itemView.setScaleX(1.0f);
            holder.itemView.setScaleY(1.0f);
        } else {
            holder.itemView.setAlpha(0.7f);
            holder.itemView.setScaleX(0.95f);
            holder.itemView.setScaleY(0.95f);
        }
        
        // 设置点击事件
        holder.itemView.setOnClickListener(v -> {
            if (onSongChangeListener != null) {
                onSongChangeListener.onSongItemClicked(song, position);
            }
        });
    }

    @Override
    public int getItemCount() {
        return playlist != null ? playlist.size() : 0;
    }

    /**
     * 更新播放列表
     */
    public void updatePlaylist(List<Song> newPlaylist) {
        this.playlist = newPlaylist;
        notifyDataSetChanged();
    }

    /**
     * 设置当前播放位置
     */
    public void setCurrentPosition(int position) {
        this.currentPosition = position;
        notifyDataSetChanged();
    }

    /**
     * 获取当前播放位置
     */
    public int getCurrentPosition() {
        return currentPosition;
    }

    /**
     * 获取当前播放歌曲
     */
    public Song getCurrentSong() {
        if (playlist != null && currentPosition >= 0 && currentPosition < playlist.size()) {
            return playlist.get(currentPosition);
        }
        return null;
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        ItemMiniPlayerBinding binding;

        ViewHolder(ItemMiniPlayerBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }
    }
} 
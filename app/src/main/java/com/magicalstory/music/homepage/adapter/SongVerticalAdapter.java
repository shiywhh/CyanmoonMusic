package com.magicalstory.music.homepage.adapter;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.magicalstory.music.R;
import com.magicalstory.music.databinding.ItemSongVerticalBinding;
import com.magicalstory.music.model.Song;
import com.magicalstory.music.utils.glide.Glide2;

import java.util.List;

/**
 * 歌曲垂直列表适配器
 * 用于显示歌曲的垂直列表
 */
public class SongVerticalAdapter extends RecyclerView.Adapter<SongVerticalAdapter.ViewHolder> {

    private Context context;
    private List<Song> songList;
    private OnItemClickListener onItemClickListener;

    public interface OnItemClickListener {
        void onItemClick(Song song, int position);
    }

    public SongVerticalAdapter(Context context, List<Song> songList) {
        this.context = context;
        this.songList = songList;
    }

    public void setOnItemClickListener(OnItemClickListener listener) {
        this.onItemClickListener = listener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        ItemSongVerticalBinding binding = ItemSongVerticalBinding.inflate(
                LayoutInflater.from(context), parent, false);
        return new ViewHolder(binding);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Song song = songList.get(position);

        // 设置歌曲标题
        holder.binding.tvTitle.setText(song.getTitle());

        // 设置艺术家和专辑信息
        String artistAlbum = song.getArtist();
        if (song.getAlbum() != null && !song.getAlbum().isEmpty()) {
            artistAlbum += " • " + formatDuration(song.getDuration());
        }
        holder.binding.tvArtistDur.setText(artistAlbum);


        // 加载专辑封面
        loadAlbumArt(holder.binding.ivCover, song);

        // 设置点击事件
        holder.itemView.setOnClickListener(v -> {
            if (onItemClickListener != null) {
                onItemClickListener.onItemClick(song, position);
            }
        });
    }

    @Override
    public int getItemCount() {
        return songList != null ? songList.size() : 0;
    }

    /**
     * 加载专辑封面
     */
    private void loadAlbumArt(android.widget.ImageView imageView, Song song) {
        String albumArtUri = null;
        if (song.getAlbumId() > 0) {
            albumArtUri = "content://media/external/audio/albumart/" + song.getAlbumId();
        }
        Glide2.loadImage(context, imageView, albumArtUri, R.drawable.place_holder_song);
    }

    /**
     * 格式化时长
     */
    private String formatDuration(long duration) {
        if (duration <= 0) {
            return "0:00";
        }

        long seconds = duration / 1000;
        long minutes = seconds / 60;
        seconds = seconds % 60;

        return String.format("%d:%02d", minutes, seconds);
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        ItemSongVerticalBinding binding;

        public ViewHolder(@NonNull ItemSongVerticalBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }
    }
}
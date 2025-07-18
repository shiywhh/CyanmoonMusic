package com.magicalstory.music.adapter;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.magicalstory.music.R;
import com.magicalstory.music.model.Song;
import com.magicalstory.music.utils.glide.GlideUtils;
import com.magicalstory.music.utils.text.TimeUtils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * 播放列表适配器
 */
public class PlaylistAdapter extends RecyclerView.Adapter<PlaylistAdapter.PlaylistViewHolder> {

    private final Context context;
    private final List<Song> songs;
    private int currentPlayingIndex = -1;
    private OnItemClickListener onItemClickListener;
    private OnItemMoreClickListener onItemMoreClickListener;
    private OnDragListener onDragListener;
    private OnItemDeleteListener onItemDeleteListener;

    public PlaylistAdapter(Context context, List<Song> songs) {
        this.context = context;
        this.songs = new ArrayList<>(songs);
    }

    @NonNull
    @Override
    public PlaylistViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_playlist_song, parent, false);
        return new PlaylistViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull PlaylistViewHolder holder, int position) {
        Song song = songs.get(position);
        holder.bind(song, position);
    }

    @Override
    public int getItemCount() {
        return songs.size();
    }

    /**
     * 设置当前播放索引
     */
    public void setCurrentPlayingIndex(int index) {
        int oldIndex = currentPlayingIndex;
        currentPlayingIndex = index;

        // 更新旧位置
        if (oldIndex >= 0 && oldIndex < songs.size()) {
            notifyItemChanged(oldIndex);
        }

        // 更新新位置
        if (index >= 0 && index < songs.size()) {
            notifyItemChanged(index);
        }
    }

    /**
     * 移动歌曲项
     */
    public void moveItem(int fromPosition, int toPosition) {
        if (fromPosition < toPosition) {
            for (int i = fromPosition; i < toPosition; i++) {
                Collections.swap(songs, i, i + 1);
            }
        } else {
            for (int i = fromPosition; i > toPosition; i--) {
                Collections.swap(songs, i, i - 1);
            }
        }

        // 更新当前播放索引
        if (currentPlayingIndex == fromPosition) {
            currentPlayingIndex = toPosition;
        } else if (currentPlayingIndex > fromPosition && currentPlayingIndex <= toPosition) {
            currentPlayingIndex--;
        } else if (currentPlayingIndex < fromPosition && currentPlayingIndex >= toPosition) {
            currentPlayingIndex++;
        }

        notifyItemMoved(fromPosition, toPosition);
    }

    /**
     * 删除歌曲项
     */
    public void removeItem(int position) {
        if (position >= 0 && position < songs.size()) {
            Song removedSong = songs.remove(position);
            notifyItemRemoved(position);

            // 更新当前播放索引
            if (currentPlayingIndex > position) {
                currentPlayingIndex--;
            } else if (currentPlayingIndex == position) {
                currentPlayingIndex = -1; // 当前播放歌曲被删除
            }

            // 通知删除监听器
            if (onItemDeleteListener != null) {
                onItemDeleteListener.onItemDeleted(position, removedSong);
            }
        }
    }

    /**
     * 获取当前歌曲列表
     */
    public List<Song> getSongs() {
        return new ArrayList<>(songs);
    }

    /**
     * 更新歌曲列表
     */
    public void updateSongs(List<Song> newSongs) {
        songs.clear();
        songs.addAll(newSongs);
        notifyDataSetChanged();
    }

    /**
     * 获取当前播放索引
     */
    public int getCurrentPlayingIndex() {
        return currentPlayingIndex;
    }

    /**
     * 设置项点击监听器
     */
    public void setOnItemClickListener(OnItemClickListener listener) {
        this.onItemClickListener = listener;
    }

    /**
     * 设置更多按钮点击监听器
     */
    public void setOnItemMoreClickListener(OnItemMoreClickListener listener) {
        this.onItemMoreClickListener = listener;
    }

    /**
     * 设置拖动监听器
     */
    public void setOnDragListener(OnDragListener listener) {
        this.onDragListener = listener;
    }

    /**
     * 设置删除监听器
     */
    public void setOnItemDeleteListener(OnItemDeleteListener listener) {
        this.onItemDeleteListener = listener;
    }

    /**
     * 项点击监听器接口
     */
    public interface OnItemClickListener {
        void onItemClick(int position, Song song);
    }

    /**
     * 更多按钮点击监听器接口
     */
    public interface OnItemMoreClickListener {
        void onMoreClick(int position, Song song, View view);
    }

    /**
     * 拖动监听器接口
     */
    public interface OnDragListener {
        void onStartDrag(PlaylistViewHolder viewHolder);
    }

    /**
     * 删除监听器接口
     */
    public interface OnItemDeleteListener {
        void onItemDeleted(int position, Song song);
    }

    public class PlaylistViewHolder extends RecyclerView.ViewHolder {
        private final ImageView ivCover;
        private final TextView tvTitle;
        private final TextView tvArtistDur;
        private final ImageButton ivMore;
        private final ImageView ivDragHandle;
        private final View viewPlayingIndicator;

        public PlaylistViewHolder(@NonNull View itemView) {
            super(itemView);
            ivCover = itemView.findViewById(R.id.iv_cover);
            tvTitle = itemView.findViewById(R.id.tv_title);
            tvArtistDur = itemView.findViewById(R.id.tv_artist_dur);
            ivMore = itemView.findViewById(R.id.iv_more);
            ivDragHandle = itemView.findViewById(R.id.iv_drag_handle);
            viewPlayingIndicator = itemView.findViewById(R.id.view_playing_indicator);
        }

        public void bind(Song song, int position) {
            // 设置歌曲信息
            tvTitle.setText(song.getTitle());
            String artistDur = song.getArtist() + " • " + TimeUtils.formatDuration(song.getDuration());
            tvArtistDur.setText(artistDur);

            // 加载专辑封面
            GlideUtils.loadAlbumCover(context, song.getAlbumId(), ivCover);

            // 设置播放指示器
            if (position == currentPlayingIndex) {
                viewPlayingIndicator.setVisibility(View.VISIBLE);
            } else {
                viewPlayingIndicator.setVisibility(View.GONE);
            }

            // 设置点击事件
            itemView.setOnClickListener(v -> {
                if (onItemClickListener != null) {
                    onItemClickListener.onItemClick(position, song);
                }
            });

            // 设置更多按钮点击事件
            ivMore.setOnClickListener(v -> {
                if (onItemMoreClickListener != null) {
                    onItemMoreClickListener.onMoreClick(position, song, v);
                }
            });

            // 设置拖动图标触摸事件
            ivDragHandle.setOnTouchListener((v, event) -> {
                if (event.getAction() == MotionEvent.ACTION_DOWN && onDragListener != null) {
                    onDragListener.onStartDrag(this);
                    return true;
                }
                return false;
            });
        }

        /**
         * 获取拖动图标
         */
        public ImageView getDragHandle() {
            return ivDragHandle;
        }
    }
} 
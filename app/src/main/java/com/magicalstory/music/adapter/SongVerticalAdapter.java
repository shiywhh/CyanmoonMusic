package com.magicalstory.music.adapter;

import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.DecelerateInterpolator;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.media3.common.util.UnstableApi;
import androidx.recyclerview.widget.RecyclerView;

import com.magicalstory.music.MainActivity;
import com.magicalstory.music.R;
import com.magicalstory.music.databinding.ItemSongVerticalBinding;
import com.magicalstory.music.model.Song;
import com.magicalstory.music.utils.glide.Glide2;

import java.util.ArrayList;
import java.util.List;

/**
 * 歌曲垂直列表适配器
 * 用于显示歌曲的垂直列表
 */
@UnstableApi
public class SongVerticalAdapter extends RecyclerView.Adapter<SongVerticalAdapter.ViewHolder> {

    private Context context;
    private List<Song> songList;
    private OnItemClickListener onItemClickListener;
    private OnItemLongClickListener onItemLongClickListener;
    private OnSelectionChangedListener onSelectionChangedListener;
    private long currentPlayingSongId = -1; // 当前播放歌曲的ID

    // 多选相关
    private boolean isMultiSelectMode = false;
    private List<Song> selectedSongs = new ArrayList<>();

    // 动画相关
    private boolean isFirstLoad = false;
    private static final int ANIMATION_DURATION = 100; // 动画持续时间(毫秒)
    private static final int ANIMATION_DELAY = 30; // 每个item之间的延迟时间(毫秒)
    private static final int MAX_ANIMATED_ITEMS = 12; // 最多为多少个item执行动画(首屏)

    public interface OnItemClickListener {
        void onItemClick(Song song, int position);
    }

    public interface OnItemLongClickListener {
        void onItemLongClick(Song song, int position);
    }

    public interface OnSelectionChangedListener {
        void onSelectionChanged(int selectedCount);
    }

    public SongVerticalAdapter(Context context, List<Song> songList) {
        this.context = context;
        this.songList = songList;
    }

    public void setOnItemClickListener(OnItemClickListener listener) {
        this.onItemClickListener = listener;
    }

    public void setOnItemLongClickListener(OnItemLongClickListener listener) {
        this.onItemLongClickListener = listener;
    }

    public void setOnSelectionChangedListener(OnSelectionChangedListener listener) {
        this.onSelectionChangedListener = listener;
    }

    /**
     * 设置当前播放歌曲的ID
     */
    public void setCurrentPlayingSongId(long songId) {
        this.currentPlayingSongId = songId;
        notifyDataSetChanged();
    }

    /**
     * 获取当前播放歌曲的ID
     */
    public long getCurrentPlayingSongId() {
        return currentPlayingSongId;
    }

    /**
     * 设置多选模式
     */
    public void setMultiSelectMode(boolean multiSelectMode) {
        this.isMultiSelectMode = multiSelectMode;
        if (!multiSelectMode) {
            selectedSongs.clear();
        }
        notifyDataSetChanged();
    }

    /**
     * 获取多选模式状态
     */
    public boolean isMultiSelectMode() {
        return isMultiSelectMode;
    }

    /**
     * 切换歌曲选中状态
     */
    public void toggleSelection(Song song) {
        if (selectedSongs.contains(song)) {
            selectedSongs.remove(song);
        } else {
            selectedSongs.add(song);
        }
        notifyDataSetChanged();

        // 通知选中状态变化
        if (onSelectionChangedListener != null) {
            onSelectionChangedListener.onSelectionChanged(selectedSongs.size());
        }
    }

    /**
     * 全选
     */
    public void selectAll() {
        selectedSongs.clear();
        selectedSongs.addAll(songList);
        notifyDataSetChanged();

        // 通知选中状态变化
        if (onSelectionChangedListener != null) {
            onSelectionChangedListener.onSelectionChanged(selectedSongs.size());
        }
    }

    /**
     * 取消全选
     */
    public void clearSelection() {
        selectedSongs.clear();
        notifyDataSetChanged();
    }

    /**
     * 获取选中的歌曲列表
     */
    public List<Song> getSelectedSongs() {
        return new ArrayList<>(selectedSongs);
    }

    /**
     * 获取选中的歌曲数量
     */
    public int getSelectedCount() {
        return selectedSongs.size();
    }

    /**
     * 检查歌曲是否被选中
     */
    public boolean isSelected(Song song) {
        return selectedSongs.contains(song);
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

        // 设置背景和文字颜色
        boolean isCurrentPlaying = song.getId() == currentPlayingSongId;
        boolean isSelected = isMultiSelectMode && isSelected(song);

        // 设置多选背景
        if (isMultiSelectMode) {
            holder.binding.viewMultiselectBackground.setVisibility(isSelected ? View.VISIBLE : View.GONE);
        } else {
            holder.binding.viewMultiselectBackground.setVisibility(View.GONE);
        }

        if (isCurrentPlaying) {
            // 当前播放状态：使用主题背景
            holder.itemView.setBackgroundColor(ContextCompat.getColor(context, R.color.md_theme_primaryContainer));
            holder.binding.tvTitle.setTextColor(ContextCompat.getColor(context, R.color.md_theme_onPrimaryContainer));
            holder.binding.tvArtistDur.setTextColor(ContextCompat.getColor(context, R.color.md_theme_onPrimaryContainer));
        } else {
            // 默认状态
            holder.itemView.setBackgroundColor(ContextCompat.getColor(context, android.R.color.transparent));
            holder.binding.tvTitle.setTextColor(ContextCompat.getColor(context, R.color.text_primary));
            holder.binding.tvArtistDur.setTextColor(ContextCompat.getColor(context, R.color.text_secondary));
        }

        // 加载专辑封面
        loadAlbumArt(holder.binding.ivCover, song);

        // 设置点击事件
        holder.itemView.setOnClickListener(v -> {
            if (isMultiSelectMode) {
                // 多选模式：切换选中状态
                toggleSelection(song);
            } else {
                // 普通模式：播放歌曲
                if (onItemClickListener != null) {
                    onItemClickListener.onItemClick(song, position);
                }
                System.out.println("点击播放音乐");
                // 播放歌曲 - 优化后的播放逻辑
                if (context instanceof MainActivity mainActivity) {
                    mainActivity.playFromPlaylist(songList, position);
                }
            }
        });

        // 设置长按事件
        holder.itemView.setOnLongClickListener(v -> {
            if (!isMultiSelectMode && onItemLongClickListener != null) {
                onItemLongClickListener.onItemLongClick(song, position);
                return true;
            }
            return false;
        });

        // 执行加载动画（仅首次加载的首屏项目）
        if (isFirstLoad && position < MAX_ANIMATED_ITEMS) {
            animateItem(holder.itemView, position);
        } else {
            // 重置视图状态，避免复用时显示异常
            holder.itemView.setAlpha(1f);
            holder.itemView.setTranslationY(0f);
        }
    }

    /**
     * 为item执行渐变动画
     */
    private void animateItem(View view, int position) {
        // 设置初始状态：透明度为0，稍微向下偏移
        view.setAlpha(0f);
        view.setTranslationY(50f);

        // 创建透明度动画
        ObjectAnimator alphaAnimator = ObjectAnimator.ofFloat(view, "alpha", 0f, 1f);
        alphaAnimator.setDuration(ANIMATION_DURATION);

        // 创建位移动画
        ObjectAnimator translationAnimator = ObjectAnimator.ofFloat(view, "translationY", 50f, 0f);
        translationAnimator.setDuration(ANIMATION_DURATION);

        // 创建动画集合
        AnimatorSet animatorSet = new AnimatorSet();
        animatorSet.playTogether(alphaAnimator, translationAnimator);
        animatorSet.setInterpolator(new DecelerateInterpolator());

        // 设置延迟时间，让每个item按顺序出现
        animatorSet.setStartDelay((long) position * ANIMATION_DELAY);

        // 开始动画
        animatorSet.start();
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

    /**
     * 更新数据
     */
    public void updateData(List<Song> newSongList) {
        this.songList = newSongList;
        // 重置首次加载标志，允许重新执行动画

        notifyDataSetChanged();
    }

    /**
     * 获取歌曲列表
     */
    public List<Song> getSongList() {
        return songList != null ? songList : new ArrayList<>();
    }

    /**
     * 禁用后续的加载动画
     */
    public void disableLoadAnimation() {
        this.isFirstLoad = false;
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        ItemSongVerticalBinding binding;

        public ViewHolder(@NonNull ItemSongVerticalBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }
    }
}
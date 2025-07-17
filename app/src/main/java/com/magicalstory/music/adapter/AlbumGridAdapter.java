package com.magicalstory.music.adapter;

import android.animation.AnimatorInflater;
import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.content.Context;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.DecelerateInterpolator;
import android.widget.ImageView;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.resource.bitmap.RoundedCorners;
import com.bumptech.glide.request.RequestOptions;
import com.magicalstory.music.MainActivity;
import com.magicalstory.music.R;
import com.magicalstory.music.databinding.ItemAlbumGridBinding;
import com.magicalstory.music.model.Album;
import com.magicalstory.music.model.Song;

import org.litepal.LitePal;

import java.util.ArrayList;
import java.util.List;

/**
 * 专辑宫格双列布局适配器
 */
public class AlbumGridAdapter extends RecyclerView.Adapter<AlbumGridAdapter.ViewHolder> {
    
    private static final String TAG = "AlbumGridAdapter";
    
    private Context context;
    private List<Album> albumList;
    private OnItemClickListener onItemClickListener;
    private OnItemLongClickListener onItemLongClickListener;
    private OnSelectionChangedListener onSelectionChangedListener;
    
    // 多选相关
    private boolean isMultiSelectMode = false;
    private List<Album> selectedAlbums = new ArrayList<>();
    
    // 动画相关
    private boolean isFirstLoad = false;
    private static final int ANIMATION_DURATION = 100; // 动画持续时间(毫秒)
    private static final int ANIMATION_DELAY = 30; // 每个item之间的延迟时间(毫秒)
    private static final int MAX_ANIMATED_ITEMS = 8; // 最多为多少个item执行动画(首屏)
    
    public interface OnItemClickListener {
        void onItemClick(Album album, int position);
    }
    
    public interface OnItemLongClickListener {
        void onItemLongClick(Album album, int position);
    }
    
    public interface OnSelectionChangedListener {
        void onSelectionChanged(int selectedCount);
    }
    
    public AlbumGridAdapter(Context context, List<Album> albumList) {
        this.context = context;
        this.albumList = albumList;
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
     * 设置多选模式
     */
    public void setMultiSelectMode(boolean multiSelectMode) {
        this.isMultiSelectMode = multiSelectMode;
        if (!multiSelectMode) {
            selectedAlbums.clear();
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
     * 切换专辑选中状态
     */
    public void toggleSelection(Album album) {
        if (selectedAlbums.contains(album)) {
            selectedAlbums.remove(album);
        } else {
            selectedAlbums.add(album);
        }
        notifyDataSetChanged();
        
        // 通知选中状态变化
        if (onSelectionChangedListener != null) {
            onSelectionChangedListener.onSelectionChanged(selectedAlbums.size());
        }
    }
    
    /**
     * 全选
     */
    public void selectAll() {
        selectedAlbums.clear();
        selectedAlbums.addAll(albumList);
        notifyDataSetChanged();
        
        // 通知选中状态变化
        if (onSelectionChangedListener != null) {
            onSelectionChangedListener.onSelectionChanged(selectedAlbums.size());
        }
    }
    
    /**
     * 取消全选
     */
    public void clearSelection() {
        selectedAlbums.clear();
        notifyDataSetChanged();
    }
    
    /**
     * 获取选中的专辑列表
     */
    public List<Album> getSelectedAlbums() {
        return new ArrayList<>(selectedAlbums);
    }
    
    /**
     * 获取选中的专辑数量
     */
    public int getSelectedCount() {
        return selectedAlbums.size();
    }
    
    /**
     * 检查专辑是否被选中
     */
    public boolean isSelected(Album album) {
        return selectedAlbums.contains(album);
    }
    
    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        ItemAlbumGridBinding binding = ItemAlbumGridBinding.inflate(
                LayoutInflater.from(context), parent, false);
        
        // 调整item宽度以适应宫格布局
        ViewGroup.LayoutParams layoutParams = binding.getRoot().getLayoutParams();
        layoutParams.width = ViewGroup.LayoutParams.MATCH_PARENT;
        binding.getRoot().setLayoutParams(layoutParams);
        
        return new ViewHolder(binding);
    }
    
    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Album album = albumList.get(position);
        
        // 设置专辑名称
        holder.binding.tvAlbumName.setText(album.getAlbumName());
        
        // 设置艺术家
        holder.binding.tvArtist.setText(album.getArtist());
        
        // 设置背景和文字颜色
        boolean isSelected = isMultiSelectMode && isSelected(album);
        
        // 设置多选背景
        if (isMultiSelectMode) {
            holder.binding.viewMultiselectBackground.setVisibility(isSelected ? View.VISIBLE : View.GONE);
        } else {
            holder.binding.viewMultiselectBackground.setVisibility(View.GONE);
        }
        
        // 设置文字颜色
        holder.binding.tvAlbumName.setTextColor(ContextCompat.getColor(context, R.color.text_primary));
        holder.binding.tvArtist.setTextColor(ContextCompat.getColor(context, R.color.text_secondary));
        
        // 加载专辑封面
        loadAlbumArt(holder.binding.ivCover, album);

        holder.binding.btnPlay.setOnClickListener(v -> {
            //在这里添加播放专辑音乐
            if (context instanceof MainActivity) {
                MainActivity mainActivity = (MainActivity) context;
                playAlbumSongs(mainActivity, album);
            }
        });
        // 设置点击事件
        holder.itemView.setOnClickListener(v -> {
            if (isMultiSelectMode) {
                // 多选模式：切换选中状态
                toggleSelection(album);
            } else {
                // 普通模式：执行点击事件
                if (onItemClickListener != null) {
                    onItemClickListener.onItemClick(album, position);
                }
            }
        });
        
        // 设置长按事件
        holder.itemView.setOnLongClickListener(v -> {
            if (!isMultiSelectMode && onItemLongClickListener != null) {
                onItemLongClickListener.onItemLongClick(album, position);
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
        animatorSet.setStartDelay(position * ANIMATION_DELAY);
        
        // 开始动画
        animatorSet.start();
    }
    
    private void loadAlbumArt(ImageView imageView, Album album) {
        // 构建专辑封面URI
        String albumArtUri = null;
        if (album.getAlbumId() > 0) {
            albumArtUri = "content://media/external/audio/albumart/" + album.getAlbumId();
        }
        
        RequestOptions options = new RequestOptions()
                .transform(new RoundedCorners(16))
                .placeholder(R.drawable.place_holder_album)
                .error(R.drawable.place_holder_album);
        
        if (!TextUtils.isEmpty(albumArtUri)) {
            Glide.with(context)
                    .load(albumArtUri)
                    .apply(options)
                    .into(imageView);
        } else {
            // 没有封面时显示默认图标
            Glide.with(context)
                    .load(R.drawable.ic_album)
                    .apply(options)
                    .into(imageView);
        }
    }
    
    /**
     * 播放专辑歌曲
     */
    private void playAlbumSongs(MainActivity mainActivity, Album album) {
        // 根据专辑ID和艺术家名称查询歌曲
        List<Song> albumSongs = LitePal.where("albumId = ? and artist = ?", 
                String.valueOf(album.getAlbumId()), album.getArtist())
                .order("track asc")
                .find(Song.class);
        
        if (albumSongs != null && !albumSongs.isEmpty()) {
            Log.d(TAG, "播放专辑: " + album.getAlbumName() + ", 歌曲数量: " + albumSongs.size());
            mainActivity.playFromPlaylist(albumSongs, 0); // 播放专辑的第一首歌曲
        }
    }
    
    @Override
    public int getItemCount() {
        return albumList != null ? albumList.size() : 0;
    }
    
    public void updateData(List<Album> newAlbumList) {
        this.albumList = newAlbumList;
        // 重置首次加载标志，允许重新执行动画
        
        notifyDataSetChanged();
    }
    
    /**
     * 禁用后续的加载动画
     */
    public void disableLoadAnimation() {
        this.isFirstLoad = false;
    }
    
    static class ViewHolder extends RecyclerView.ViewHolder {
        ItemAlbumGridBinding binding;
        
        public ViewHolder(@NonNull ItemAlbumGridBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }
    }
} 
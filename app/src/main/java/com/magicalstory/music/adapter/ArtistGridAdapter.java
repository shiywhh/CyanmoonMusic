package com.magicalstory.music.adapter;

import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.DecelerateInterpolator;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.media3.common.util.UnstableApi;
import androidx.recyclerview.widget.RecyclerView;
import androidx.fragment.app.Fragment;
import androidx.navigation.Navigation;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.resource.bitmap.RoundedCorners;
import com.bumptech.glide.request.RequestOptions;
import com.magicalstory.music.R;
import com.magicalstory.music.databinding.ItemArtistGridBinding;
import com.magicalstory.music.model.Artist;

import java.util.ArrayList;
import java.util.List;

/**
 * 艺术家宫格双列布局适配器
 */
@UnstableApi
public class ArtistGridAdapter extends RecyclerView.Adapter<ArtistGridAdapter.ViewHolder> {
    
    private Context context;
    private List<Artist> artistList;
    private OnItemClickListener onItemClickListener;
    private OnItemLongClickListener onItemLongClickListener;
    private OnSelectionChangedListener onSelectionChangedListener;
    private Fragment fragment;
    
    // 多选相关
    private boolean isMultiSelectMode = false;
    private List<Artist> selectedArtists = new ArrayList<>();
    
    // 动画相关
    private boolean isFirstLoad = false;
    private static final int ANIMATION_DURATION = 100; // 动画持续时间(毫秒)
    private static final int ANIMATION_DELAY = 30; // 每个item之间的延迟时间(毫秒)
    private static final int MAX_ANIMATED_ITEMS = 8; // 最多为多少个item执行动画(首屏)
    
    public interface OnItemClickListener {
        void onItemClick(Artist artist, int position);
    }
    
    public interface OnItemLongClickListener {
        void onItemLongClick(Artist artist, int position);
    }
    
    public interface OnSelectionChangedListener {
        void onSelectionChanged(int selectedCount);
    }
    
    public ArtistGridAdapter(Context context, List<Artist> artistList) {
        this.context = context;
        this.artistList = artistList;
    }
    
    public ArtistGridAdapter(Context context, List<Artist> artistList, Fragment fragment) {
        this.context = context;
        this.artistList = artistList;
        this.fragment = fragment;
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
            selectedArtists.clear();
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
     * 切换艺术家选中状态
     */
    public void toggleSelection(Artist artist) {
        if (selectedArtists.contains(artist)) {
            selectedArtists.remove(artist);
        } else {
            selectedArtists.add(artist);
        }
        notifyDataSetChanged();
        
        // 通知选中状态变化
        if (onSelectionChangedListener != null) {
            onSelectionChangedListener.onSelectionChanged(selectedArtists.size());
        }
    }
    
    /**
     * 全选
     */
    public void selectAll() {
        selectedArtists.clear();
        selectedArtists.addAll(artistList);
        notifyDataSetChanged();
        
        // 通知选中状态变化
        if (onSelectionChangedListener != null) {
            onSelectionChangedListener.onSelectionChanged(selectedArtists.size());
        }
    }
    
    /**
     * 取消全选
     */
    public void clearSelection() {
        selectedArtists.clear();
        notifyDataSetChanged();
    }
    
    /**
     * 获取选中的艺术家列表
     */
    public List<Artist> getSelectedArtists() {
        return new ArrayList<>(selectedArtists);
    }
    
    /**
     * 获取选中的艺术家数量
     */
    public int getSelectedCount() {
        return selectedArtists.size();
    }
    
    /**
     * 检查艺术家是否被选中
     */
    public boolean isSelected(Artist artist) {
        return selectedArtists.contains(artist);
    }
    
    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        ItemArtistGridBinding binding = ItemArtistGridBinding.inflate(
                LayoutInflater.from(context), parent, false);
        
        return new ViewHolder(binding);
    }
    
    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Artist artist = artistList.get(position);
        
        // 设置艺术家名称
        holder.binding.tvArtistName.setText(artist.getArtistName());
        
        // 设置歌曲数量
        String songCount = artist.getSongCount() + " 首歌曲";
        holder.binding.tvSongCount.setText(songCount);
        
        // 设置背景和文字颜色
        boolean isSelected = isMultiSelectMode && isSelected(artist);
        
        // 设置多选背景
        if (isMultiSelectMode) {
            holder.binding.viewMultiselectBackground.setVisibility(isSelected ? View.VISIBLE : View.GONE);
        } else {
            holder.binding.viewMultiselectBackground.setVisibility(View.GONE);
        }
        
        // 设置文字颜色
        holder.binding.tvArtistName.setTextColor(ContextCompat.getColor(context, R.color.text_primary));
        holder.binding.tvSongCount.setTextColor(ContextCompat.getColor(context, R.color.text_secondary));
        
        // 加载艺术家头像
        loadArtistAvatar(holder.binding.ivAvatar, artist);
        
        // 设置点击事件
        holder.itemView.setOnClickListener(v -> {
            if (isMultiSelectMode) {
                // 多选模式：切换选中状态
                toggleSelection(artist);
            } else {
                // 普通模式：执行点击事件
                if (onItemClickListener != null) {
                    onItemClickListener.onItemClick(artist, position);
                } else {
                    // 默认跳转到歌手详情页面
                    navigateToArtistDetail(artist);
                }
            }
        });
        
        // 设置长按事件
        holder.itemView.setOnLongClickListener(v -> {
            if (!isMultiSelectMode && onItemLongClickListener != null) {
                onItemLongClickListener.onItemLongClick(artist, position);
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
    
    private void loadArtistAvatar(com.google.android.material.imageview.ShapeableImageView imageView, Artist artist) {
        RequestOptions options = new RequestOptions()
                .transform(new RoundedCorners(16))
                .placeholder(R.drawable.place_holder_artist)
                .error(R.drawable.place_holder_artist);

        // 如果艺术家有封面URL，则加载网络图片，否则使用默认图标
        if (artist.getCoverUrl() != null && !artist.getCoverUrl().isEmpty()) {
            Glide.with(context)
                    .load(artist.getCoverUrl())
                    .apply(options)
                    .into(imageView);
        } else {
            Glide.with(context)
                    .load(R.drawable.place_holder_artist)
                    .apply(options)
                    .into(imageView);
        }
    }
    
    @Override
    public int getItemCount() {
        return artistList != null ? artistList.size() : 0;
    }
    
    public void updateData(List<Artist> newArtistList) {
        this.artistList = newArtistList;
        // 重置首次加载标志，允许重新执行动画
        
        notifyDataSetChanged();
    }
    
    /**
     * 禁用后续的加载动画
     */
    public void disableLoadAnimation() {
        this.isFirstLoad = false;
    }
    
    /**
     * 跳转到歌手详情页面
     */
    private void navigateToArtistDetail(Artist artist) {
        if (fragment != null && fragment.getView() != null) {
            Bundle bundle = new Bundle();
            bundle.putString("artist_name", artist.getArtistName());
            Navigation.findNavController(fragment.requireView()).navigate(R.id.action_artists_to_artist_detail, bundle);
        }
    }
    
    static class ViewHolder extends RecyclerView.ViewHolder {
        ItemArtistGridBinding binding;
        
        public ViewHolder(@NonNull ItemArtistGridBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }
    }
} 
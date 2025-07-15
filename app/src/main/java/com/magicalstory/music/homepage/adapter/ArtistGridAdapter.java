package com.magicalstory.music.homepage.adapter;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

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
public class ArtistGridAdapter extends RecyclerView.Adapter<ArtistGridAdapter.ViewHolder> {
    
    private Context context;
    private List<Artist> artistList;
    private OnItemClickListener onItemClickListener;
    private OnItemLongClickListener onItemLongClickListener;
    private OnSelectionChangedListener onSelectionChangedListener;
    
    // 多选相关
    private boolean isMultiSelectMode = false;
    private List<Artist> selectedArtists = new ArrayList<>();
    
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
        notifyDataSetChanged();
    }
    
    static class ViewHolder extends RecyclerView.ViewHolder {
        ItemArtistGridBinding binding;
        
        public ViewHolder(@NonNull ItemArtistGridBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }
    }
} 
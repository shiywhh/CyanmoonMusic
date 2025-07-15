package com.magicalstory.music.homepage.adapter;

import android.content.Context;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.resource.bitmap.RoundedCorners;
import com.bumptech.glide.request.RequestOptions;
import com.magicalstory.music.R;
import com.magicalstory.music.databinding.ItemAlbumGridBinding;
import com.magicalstory.music.databinding.ItemAlbumHorizontalBinding;
import com.magicalstory.music.model.Album;

import java.util.ArrayList;
import java.util.List;

/**
 * 专辑宫格双列布局适配器
 */
public class AlbumGridAdapter extends RecyclerView.Adapter<AlbumGridAdapter.ViewHolder> {
    
    private Context context;
    private List<Album> albumList;
    private OnItemClickListener onItemClickListener;
    private OnItemLongClickListener onItemLongClickListener;
    private OnSelectionChangedListener onSelectionChangedListener;
    
    // 多选相关
    private boolean isMultiSelectMode = false;
    private List<Album> selectedAlbums = new ArrayList<>();
    
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
    
    @Override
    public int getItemCount() {
        return albumList != null ? albumList.size() : 0;
    }
    
    public void updateData(List<Album> newAlbumList) {
        this.albumList = newAlbumList;
        notifyDataSetChanged();
    }
    
    static class ViewHolder extends RecyclerView.ViewHolder {
        ItemAlbumGridBinding binding;
        
        public ViewHolder(@NonNull ItemAlbumGridBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }
    }
} 
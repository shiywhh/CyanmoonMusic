package com.magicalstory.music.homepage.adapter;

import android.content.Context;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.ViewGroup;
import android.widget.ImageView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.resource.bitmap.RoundedCorners;
import com.bumptech.glide.request.RequestOptions;
import com.magicalstory.music.R;
import com.magicalstory.music.databinding.ItemAlbumGridBinding;
import com.magicalstory.music.databinding.ItemAlbumHorizontalBinding;
import com.magicalstory.music.model.Album;

import java.util.List;

/**
 * 专辑宫格双列布局适配器
 */
public class AlbumGridAdapter extends RecyclerView.Adapter<AlbumGridAdapter.ViewHolder> {
    
    private Context context;
    private List<Album> albumList;
    private OnItemClickListener onItemClickListener;
    
    public interface OnItemClickListener {
        void onItemClick(Album album, int position);
    }
    
    public AlbumGridAdapter(Context context, List<Album> albumList) {
        this.context = context;
        this.albumList = albumList;
    }
    
    public void setOnItemClickListener(OnItemClickListener listener) {
        this.onItemClickListener = listener;
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
        
        // 加载专辑封面
        loadAlbumArt(holder.binding.ivCover, album);
        
        // 设置点击事件
        holder.itemView.setOnClickListener(v -> {
            if (onItemClickListener != null) {
                onItemClickListener.onItemClick(album, position);
            }
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
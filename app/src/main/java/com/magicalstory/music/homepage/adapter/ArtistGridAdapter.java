package com.magicalstory.music.homepage.adapter;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.resource.bitmap.RoundedCorners;
import com.bumptech.glide.request.RequestOptions;
import com.magicalstory.music.R;
import com.magicalstory.music.databinding.ItemArtistGridBinding;
import com.magicalstory.music.model.Artist;

import java.util.List;

/**
 * 艺术家宫格双列布局适配器
 */
public class ArtistGridAdapter extends RecyclerView.Adapter<ArtistGridAdapter.ViewHolder> {
    
    private Context context;
    private List<Artist> artistList;
    private OnItemClickListener onItemClickListener;
    
    public interface OnItemClickListener {
        void onItemClick(Artist artist, int position);
    }
    
    public ArtistGridAdapter(Context context, List<Artist> artistList) {
        this.context = context;
        this.artistList = artistList;
    }
    
    public void setOnItemClickListener(OnItemClickListener listener) {
        this.onItemClickListener = listener;
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
        
        // 加载艺术家头像
        loadArtistAvatar(holder.binding.ivAvatar, artist);
        
        // 设置点击事件
        holder.itemView.setOnClickListener(v -> {
            if (onItemClickListener != null) {
                onItemClickListener.onItemClick(artist, position);
            }
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
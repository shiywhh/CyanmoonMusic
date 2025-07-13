package com.magicalstory.music.homepage.adapter;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.ViewGroup;
import android.widget.ImageView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.resource.bitmap.RoundedCorners;
import com.bumptech.glide.request.RequestOptions;
import com.magicalstory.music.R;
import com.magicalstory.music.databinding.ItemArtistHorizontalBinding;
import com.magicalstory.music.model.Artist;

import java.util.List;

/**
 * 艺术家横向滑动列表适配器
 */
public class ArtistHorizontalAdapter extends RecyclerView.Adapter<ArtistHorizontalAdapter.ViewHolder> {
    
    private Context context;
    private List<Artist> artistList;
    private OnItemClickListener onItemClickListener;
    
    public interface OnItemClickListener {
        void onItemClick(Artist artist, int position);
    }
    
    public ArtistHorizontalAdapter(Context context, List<Artist> artistList) {
        this.context = context;
        this.artistList = artistList;
    }
    
    public void setOnItemClickListener(OnItemClickListener listener) {
        this.onItemClickListener = listener;
    }
    
    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        ItemArtistHorizontalBinding binding = ItemArtistHorizontalBinding.inflate(
                LayoutInflater.from(context), parent, false);
        return new ViewHolder(binding);
    }
    
    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Artist artist = artistList.get(position);
        
        // 设置艺术家名称
        holder.binding.tvArtistName.setText(artist.getArtistName());
        
        // 设置歌曲数量信息
        String songCountText = artist.getSongCount() + " 首歌曲";
        holder.binding.tvSongCount.setText(songCountText);
        
        // 加载艺术家头像（使用默认图标）
        loadArtistAvatar(holder.binding.ivAvatar);
        
        // 设置点击事件
        holder.itemView.setOnClickListener(v -> {
            if (onItemClickListener != null) {
                onItemClickListener.onItemClick(artist, position);
            }
        });
    }
    
    private void loadArtistAvatar(ImageView imageView) {
        RequestOptions options = new RequestOptions()
                .transform(new RoundedCorners(16))
                .placeholder(R.drawable.place_holder_artist)
                .error(R.drawable.place_holder_artist);
        
        // 艺术家头像使用默认图标
        Glide.with(context)
                .load(R.drawable.place_holder_artist)
                .apply(options)
                .into(imageView);
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
        ItemArtistHorizontalBinding binding;
        
        public ViewHolder(@NonNull ItemArtistHorizontalBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }
    }
}
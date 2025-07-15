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
import com.magicalstory.music.MainActivity;
import com.magicalstory.music.R;
import com.magicalstory.music.databinding.ItemArtistHorizontalBinding;
import com.magicalstory.music.model.Artist;
import com.magicalstory.music.model.Song;

import org.litepal.LitePal;

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
        loadArtistAvatar(holder.binding.ivAvatar, artist);
        
        // 设置点击事件
        holder.itemView.setOnClickListener(v -> {
            if (onItemClickListener != null) {
                onItemClickListener.onItemClick(artist, position);
            }
            
            // 播放艺术家歌曲
            if (context instanceof MainActivity mainActivity) {
                playArtistSongs(mainActivity, artist);
            }
        });
    }
    
    /**
     * 播放艺术家歌曲
     */
    private void playArtistSongs(MainActivity mainActivity, Artist artist) {
        // 根据艺术家名称查询歌曲
        List<Song> artistSongs = LitePal.where("artist = ?", artist.getArtistName())
                .order("album asc, track asc")
                .find(Song.class);
        
        if (artistSongs != null && !artistSongs.isEmpty()) {
            System.out.println("播放艺术家: " + artist.getArtistName() + ", 歌曲数量: " + artistSongs.size());
            mainActivity.setPlaylist(artistSongs);
            mainActivity.playSong(artistSongs.get(0)); // 播放第一首歌曲
        }
    }
    
    private void loadArtistAvatar(ImageView imageView, Artist artist) {
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
        ItemArtistHorizontalBinding binding;
        
        public ViewHolder(@NonNull ItemArtistHorizontalBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }
    }
}
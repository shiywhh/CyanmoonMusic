package com.magicalstory.music.adapter;

import android.content.Context;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.ViewGroup;
import android.widget.ImageView;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.media3.common.util.UnstableApi;
import androidx.navigation.Navigation;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.resource.bitmap.RoundedCorners;
import com.bumptech.glide.request.RequestOptions;
import com.magicalstory.music.R;
import com.magicalstory.music.databinding.ItemArtistHorizontalBinding;
import com.magicalstory.music.model.Artist;
import com.magicalstory.music.model.Song;

import org.litepal.LitePal;

import java.util.List;

/**
 * 艺术家横向滑动列表适配器
 */
@UnstableApi
public class ArtistHorizontalAdapter extends RecyclerView.Adapter<ArtistHorizontalAdapter.ViewHolder> {

    private Context context;
    private List<Artist> artistList;
    private OnItemClickListener onItemClickListener;
    private Fragment fragment;

    public interface OnItemClickListener {
        void onItemClick(Artist artist, int position);
    }

    public ArtistHorizontalAdapter(Context context, List<Artist> artistList) {
        this.context = context;
        this.artistList = artistList;
    }

    public ArtistHorizontalAdapter(Context context, List<Artist> artistList, Fragment fragment) {
        this.context = context;
        this.artistList = artistList;
        this.fragment = fragment;
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
            } else {
                // 默认跳转到歌手详情页面
                navigateToArtistDetail(artist);
            }
        });
    }

    /**
     * 跳转到歌手详情页面
     */
    private void navigateToArtistDetail(Artist artist) {
        if (fragment != null && fragment.getView() != null) {
            Bundle bundle = new Bundle();
            bundle.putString("artist_name", artist.getArtistName());
            
            // 根据当前Fragment类型选择合适的导航动作
            if (fragment instanceof com.magicalstory.music.homepage.HomeFragment) {
                Navigation.findNavController(fragment.requireView()).navigate(R.id.action_home_to_artist_detail, bundle);
            } else if (fragment instanceof com.magicalstory.music.fragment.ArtistListFragment) {
                Navigation.findNavController(fragment.requireView()).navigate(R.id.action_artists_to_artist_detail, bundle);
            } else if (fragment instanceof com.magicalstory.music.fragment.AlbumDetailFragment) {
                Navigation.findNavController(fragment.requireView()).navigate(R.id.action_album_detail_to_artist_detail, bundle);
            } else {
                // 默认情况，尝试全局导航
                Navigation.findNavController(fragment.requireView()).navigate(R.id.nav_artist_detail, bundle);
            }
        }
    }

    private void loadArtistAvatar(ImageView imageView, Artist artist) {
        RequestOptions options = new RequestOptions()
                .transform(new RoundedCorners(16))
                .placeholder(R.drawable.place_holder_artist)
                .error(R.drawable.place_holder_artist);

        String coverUrl = artist.getCoverUrl();
        if (coverUrl != null && !coverUrl.isEmpty()) {
            // 检查是否是歌曲路径（回退封面）
            if (coverUrl.startsWith("/") && coverUrl.contains(".")) {
                // 这是歌曲路径，从歌曲文件中加载封面
                Glide.with(context)
                        .load(coverUrl)
                        .apply(options)
                        .into(imageView);
            } else if (coverUrl.startsWith("http")) {
                // 这是网络URL
                Glide.with(context)
                        .load(coverUrl)
                        .apply(options)
                        .into(imageView);
            } else if (coverUrl.startsWith("content://media/external/audio/albumart/")) {
                // 这是系统专辑封面URI
                Glide.with(context)
                        .load(coverUrl)
                        .apply(options)
                        .into(imageView);
            } else {
                // 其他情况，尝试直接加载
                Glide.with(context)
                        .load(coverUrl)
                        .apply(options)
                        .into(imageView);
            }
        } else {
            // 没有封面时显示默认图标
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
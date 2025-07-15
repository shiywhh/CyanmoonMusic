package com.magicalstory.music.adapter;

import android.content.Context;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.resource.bitmap.RoundedCorners;
import com.bumptech.glide.request.RequestOptions;
import com.magicalstory.music.MainActivity;
import com.magicalstory.music.R;
import com.magicalstory.music.databinding.ItemAlbumHorizontalBinding;
import com.magicalstory.music.model.Album;
import com.magicalstory.music.model.Song;

import org.litepal.LitePal;

import java.util.List;

/**
 * 专辑横向滑动列表适配器
 */
public class AlbumHorizontalAdapter extends RecyclerView.Adapter<AlbumHorizontalAdapter.ViewHolder> {

    private Context context;
    private List<Album> albumList;
    private OnItemClickListener onItemClickListener;

    public interface OnItemClickListener {
        void onItemClick(Album album, int position);
    }

    public AlbumHorizontalAdapter(Context context, List<Album> albumList) {
        this.context = context;
        this.albumList = albumList;
    }

    public void setOnItemClickListener(OnItemClickListener listener) {
        this.onItemClickListener = listener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        ItemAlbumHorizontalBinding binding = ItemAlbumHorizontalBinding.inflate(
                LayoutInflater.from(context), parent, false);
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

        // 设置专辑封面点击事件 - 跳转到专辑详情页面
        holder.itemView.setOnClickListener(v -> {
            if (onItemClickListener != null) {
                onItemClickListener.onItemClick(album, position);
            }
        });

        holder.binding.btnPlay.setOnClickListener(v -> {
            //在这里添加播放专辑音乐
            if (context instanceof MainActivity) {
                MainActivity mainActivity = (MainActivity) context;
                playAlbumSongs(mainActivity, album);
            }
        });
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
            System.out.println("播放专辑: " + album.getAlbumName() + ", 歌曲数量: " + albumSongs.size());
            mainActivity.setPlaylist(albumSongs);
            mainActivity.playSong(albumSongs.get(0)); // 播放第一首歌曲
        }
    }

    private void loadAlbumArt(ImageView imageView, Album album) {
        RequestOptions options = new RequestOptions()
                .transform(new RoundedCorners(16))
                .placeholder(R.drawable.place_holder_album)
                .error(R.drawable.place_holder_album);

        String albumArt = album.getAlbumArt();
        if (!TextUtils.isEmpty(albumArt)) {
            // 检查是否是歌曲路径（回退封面）
            if (albumArt.startsWith("/") && albumArt.contains(".")) {
                // 这是歌曲路径，从歌曲文件中加载封面
                Glide.with(context)
                        .load(albumArt)
                        .apply(options)
                        .into(imageView);
            } else if (albumArt.startsWith("http")) {
                // 这是网络URL
                Glide.with(context)
                        .load(albumArt)
                        .apply(options)
                        .into(imageView);
            } else if (albumArt.startsWith("content://media/external/audio/albumart/")) {
                // 这是系统专辑封面URI
                Glide.with(context)
                        .load(albumArt)
                        .apply(options)
                        .into(imageView);
            } else {
                // 其他情况，尝试直接加载
                Glide.with(context)
                        .load(albumArt)
                        .apply(options)
                        .into(imageView);
            }
        } else {
            // 构建系统专辑封面URI
            String albumArtUri = null;
            if (album.getAlbumId() > 0) {
                albumArtUri = "content://media/external/audio/albumart/" + album.getAlbumId();
            }

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
        ItemAlbumHorizontalBinding binding;

        public ViewHolder(@NonNull ItemAlbumHorizontalBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }
    }
}
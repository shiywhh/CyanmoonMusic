package com.magicalstory.music.adapter;

import android.content.Context;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

import androidx.annotation.NonNull;
import androidx.media3.common.util.UnstableApi;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.resource.bitmap.RoundedCorners;
import com.bumptech.glide.request.RequestOptions;
import com.magicalstory.music.MainActivity;
import com.magicalstory.music.R;
import com.magicalstory.music.databinding.ItemAlbumHorizontalBinding;
import com.magicalstory.music.dialog.AlbumBottomSheetDialogFragment;
import com.magicalstory.music.model.Album;
import com.magicalstory.music.model.Song;

import org.litepal.LitePal;

import java.util.ArrayList;
import java.util.List;

/**
 * 专辑横向滑动列表适配器
 */
@UnstableApi
public class AlbumHorizontalAdapter extends RecyclerView.Adapter<AlbumHorizontalAdapter.ViewHolder> {
    
    private static final String TAG = "AlbumHorizontalAdapter";
    
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
            if (context instanceof MainActivity mainActivity) {
                playAlbumSongs(mainActivity, album);
            }
        });

        // 设置菜单按钮点击事件 - 显示专辑底部弹窗
        holder.binding.btnMenu.setOnClickListener(v -> {
            showAlbumBottomSheet(album);
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
            Log.d(TAG, "播放专辑: " + album.getAlbumName() + ", 歌曲数量: " + albumSongs.size());
            mainActivity.playFromPlaylist(albumSongs, 0); // 播放专辑的第一首歌曲
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

    /**
     * 根据ID列表移除专辑
     * @param albumIds 要移除的专辑ID列表
     */
    public void removeAlbumsByIds(List<Long> albumIds) {
        if (albumIds == null || albumIds.isEmpty() || albumList == null) {
            return;
        }
        
        // 创建要移除的专辑列表
        List<Album> albumsToRemove = new ArrayList<>();
        for (Album album : albumList) {
            if (albumIds.contains(album.getId())) {
                albumsToRemove.add(album);
            }
        }
        
        // 从列表中移除专辑
        albumList.removeAll(albumsToRemove);
        
        // 通知适配器数据已更改
        notifyDataSetChanged();
        
        Log.d(TAG, "移除了 " + albumsToRemove.size() + " 张专辑");
    }

    /**
     * 显示专辑底部弹窗
     */
    private void showAlbumBottomSheet(Album album) {
        if (context instanceof MainActivity) {
            AlbumBottomSheetDialogFragment fragment = AlbumBottomSheetDialogFragment.newInstance(album);
            fragment.show(((MainActivity) context).getSupportFragmentManager(), "AlbumBottomSheet");
            Log.d(TAG, "显示专辑底部弹窗: " + album.getAlbumName());
        }
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        ItemAlbumHorizontalBinding binding;

        public ViewHolder(@NonNull ItemAlbumHorizontalBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }
    }
}
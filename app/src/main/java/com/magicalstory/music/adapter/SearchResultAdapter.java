package com.magicalstory.music.adapter;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import androidx.media3.common.util.UnstableApi;

import com.magicalstory.music.MainActivity;
import com.magicalstory.music.R;
import com.magicalstory.music.databinding.ItemAlbumVerticalBinding;
import com.magicalstory.music.databinding.ItemArtistVerticalBinding;
import com.magicalstory.music.databinding.ItemSearchGroupTitleBinding;
import com.magicalstory.music.databinding.ItemSongVerticalBinding;
import com.magicalstory.music.model.Album;
import com.magicalstory.music.model.Artist;
import com.magicalstory.music.model.Song;
import com.magicalstory.music.utils.glide.Glide2;

import java.util.ArrayList;
import java.util.List;

/**
 * 搜索结果适配器
 * 支持显示歌曲、专辑、艺术家和分组标题
 */
@UnstableApi
public class SearchResultAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

    private static final int TYPE_GROUP_TITLE = 0;
    private static final int TYPE_SONG = 1;
    private static final int TYPE_ALBUM = 2;
    private static final int TYPE_ARTIST = 3;

    private Context context;
    private List<SearchResultItem> items;
    private OnSongClickListener onSongClickListener;
    private OnAlbumClickListener onAlbumClickListener;
    private OnArtistClickListener onArtistClickListener;

    public interface OnSongClickListener {
        void onSongClick(Song song, int position);
    }

    public interface OnAlbumClickListener {
        void onAlbumClick(Album album, int position);
    }

    public interface OnArtistClickListener {
        void onArtistClick(Artist artist, int position);
    }

    public SearchResultAdapter(Context context) {
        this.context = context;
        this.items = new ArrayList<>();
    }

    public void setOnSongClickListener(OnSongClickListener listener) {
        this.onSongClickListener = listener;
    }

    public void setOnAlbumClickListener(OnAlbumClickListener listener) {
        this.onAlbumClickListener = listener;
    }

    public void setOnArtistClickListener(OnArtistClickListener listener) {
        this.onArtistClickListener = listener;
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        LayoutInflater inflater = LayoutInflater.from(context);

        switch (viewType) {
            case TYPE_GROUP_TITLE:
                ItemSearchGroupTitleBinding titleBinding = ItemSearchGroupTitleBinding.inflate(inflater, parent, false);
                return new GroupTitleViewHolder(titleBinding);
            case TYPE_SONG:
                ItemSongVerticalBinding songBinding = ItemSongVerticalBinding.inflate(inflater, parent, false);
                return new SongViewHolder(songBinding);
            case TYPE_ALBUM:
                ItemAlbumVerticalBinding albumBinding = ItemAlbumVerticalBinding.inflate(inflater, parent, false);
                return new AlbumViewHolder(albumBinding);
            case TYPE_ARTIST:
                ItemArtistVerticalBinding artistBinding = ItemArtistVerticalBinding.inflate(inflater, parent, false);
                return new ArtistViewHolder(artistBinding);
            default:
                throw new IllegalArgumentException("Unknown view type: " + viewType);
        }
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        SearchResultItem item = items.get(position);

        switch (holder.getItemViewType()) {
            case TYPE_GROUP_TITLE:
                bindGroupTitle((GroupTitleViewHolder) holder, item);
                break;
            case TYPE_SONG:
                bindSong((SongViewHolder) holder, item, position);
                break;
            case TYPE_ALBUM:
                bindAlbum((AlbumViewHolder) holder, item, position);
                break;
            case TYPE_ARTIST:
                bindArtist((ArtistViewHolder) holder, item, position);
                break;
        }
    }

    private void bindGroupTitle(GroupTitleViewHolder holder, SearchResultItem item) {
        holder.binding.tvGroupTitle.setText(item.getTitle());
    }

    private void bindSong(SongViewHolder holder, SearchResultItem item, int position) {
        Song song = item.getSong();
        holder.binding.getRoot().setPadding(0, 0, 0, 0);
        // 设置歌曲标题
        holder.binding.tvTitle.setText(song.getTitle());

        // 设置艺术家和专辑信息
        String artistAlbum = song.getArtist();
        if (song.getAlbum() != null && !song.getAlbum().isEmpty()) {
            artistAlbum += " • " + formatDuration(song.getDuration());
        }
        holder.binding.tvArtistDur.setText(artistAlbum);

        // 加载专辑封面
        loadAlbumArt(holder.binding.ivCover, song);

        // 设置点击事件
        holder.itemView.setOnClickListener(v -> {
            if (onSongClickListener != null) {
                onSongClickListener.onSongClick(song, position);
            }
        });
    }

    private void bindAlbum(AlbumViewHolder holder, SearchResultItem item, int position) {
        Album album = item.getAlbum();

        // 设置专辑名称
        holder.binding.tvAlbumName.setText(album.getAlbumName());

        // 设置艺术家名称
        holder.binding.tvArtist.setText(album.getArtist());

        // 加载专辑封面
        String albumArtUri = null;
        if (album.getAlbumId() > 0) {
            albumArtUri = "content://media/external/audio/albumart/" + album.getAlbumId();
        }
        Glide2.loadImage(context, holder.binding.ivCover, albumArtUri, R.drawable.place_holder_album);

        // 设置点击事件
        holder.itemView.setOnClickListener(v -> {
            if (onAlbumClickListener != null) {
                onAlbumClickListener.onAlbumClick(album, position);
            }
        });
    }

    private void bindArtist(ArtistViewHolder holder, SearchResultItem item, int position) {
        Artist artist = item.getArtist();

        // 设置艺术家名称
        holder.binding.tvArtistName.setText(artist.getArtistName());

        // 设置歌曲数量
        holder.binding.tvSongCount.setText(artist.getSongCount() + " 首歌曲");

        // 加载艺术家头像
        if (artist.getCoverUrl() != null && !artist.getCoverUrl().isEmpty()) {
            // 如果有封面URL，使用网络图片
            Glide2.loadImage(context, holder.binding.ivAvatar, artist.getCoverUrl(), R.drawable.place_holder_artist);
        } else {
            // 否则使用默认头像
            Glide2.loadImage(context, holder.binding.ivAvatar, null, R.drawable.place_holder_artist);
        }

        // 设置点击事件
        holder.itemView.setOnClickListener(v -> {
            if (onArtistClickListener != null) {
                onArtistClickListener.onArtistClick(artist, position);
            }
        });
    }

    private void loadAlbumArt(android.widget.ImageView imageView, Song song) {
        String albumArtUri = null;
        if (song.getAlbumId() > 0) {
            albumArtUri = "content://media/external/audio/albumart/" + song.getAlbumId();
        }
        Glide2.loadImage(context, imageView, albumArtUri, R.drawable.place_holder_song);
    }

    private String formatDuration(long duration) {
        if (duration <= 0) {
            return "0:00";
        }

        long seconds = duration / 1000;
        long minutes = seconds / 60;
        seconds = seconds % 60;

        return String.format("%d:%02d", minutes, seconds);
    }

    @Override
    public int getItemViewType(int position) {
        return items.get(position).getType();
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    /**
     * 更新搜索结果
     */
    public void updateSearchResults(List<Song> songs, List<Album> albums, List<Artist> artists) {
        items.clear();

        // 添加歌曲分组
        if (!songs.isEmpty()) {
            items.add(new SearchResultItem(TYPE_GROUP_TITLE, songs.size() + "首歌曲", null, null, null));
            for (Song song : songs) {
                items.add(new SearchResultItem(TYPE_SONG, null, song, null, null));
            }
        }

        // 添加专辑分组
        if (!albums.isEmpty()) {
            items.add(new SearchResultItem(TYPE_GROUP_TITLE, albums.size() + "个专辑", null, null, null));
            for (Album album : albums) {
                items.add(new SearchResultItem(TYPE_ALBUM, null, null, album, null));
            }
        }

        // 添加艺术家分组
        if (!artists.isEmpty()) {
            items.add(new SearchResultItem(TYPE_GROUP_TITLE, artists.size() + "个艺术家", null, null, null));
            for (Artist artist : artists) {
                items.add(new SearchResultItem(TYPE_ARTIST, null, null, null, artist));
            }
        }

        notifyDataSetChanged();
    }

    /**
     * 更新单一类型搜索结果
     */
    public void updateSingleTypeResults(String type, List<?> results) {
        items.clear();

        if (!results.isEmpty()) {
            String title = "";
            switch (type) {
                case "songs":
                    title = results.size() + "首歌曲";
                    for (Object result : results) {
                        if (result instanceof Song) {
                            items.add(new SearchResultItem(TYPE_SONG, null, (Song) result, null, null));
                        }
                    }
                    break;
                case "album":
                    title = results.size() + "个专辑";
                    for (Object result : results) {
                        if (result instanceof Album) {
                            items.add(new SearchResultItem(TYPE_ALBUM, null, null, (Album) result, null));
                        }
                    }
                    break;
                case "artist":
                    title = results.size() + "个艺术家";
                    for (Object result : results) {
                        if (result instanceof Artist) {
                            items.add(new SearchResultItem(TYPE_ARTIST, null, null, null, (Artist) result));
                        }
                    }
                    break;
            }

            if (!title.isEmpty()) {
                items.add(0, new SearchResultItem(TYPE_GROUP_TITLE, title, null, null, null));
            }
        }

        notifyDataSetChanged();
    }

    /**
     * 获取所有歌曲列表（用于播放）
     */
    public List<Song> getAllSongs() {
        List<Song> songs = new ArrayList<>();
        for (SearchResultItem item : items) {
            if (item.getType() == TYPE_SONG && item.getSong() != null) {
                songs.add(item.getSong());
            }
        }
        return songs;
    }

    // ViewHolder类
    public static class GroupTitleViewHolder extends RecyclerView.ViewHolder {
        ItemSearchGroupTitleBinding binding;

        public GroupTitleViewHolder(@NonNull ItemSearchGroupTitleBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }
    }

    public static class SongViewHolder extends RecyclerView.ViewHolder {
        ItemSongVerticalBinding binding;

        public SongViewHolder(@NonNull ItemSongVerticalBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }
    }

    public static class AlbumViewHolder extends RecyclerView.ViewHolder {
        ItemAlbumVerticalBinding binding;

        public AlbumViewHolder(@NonNull ItemAlbumVerticalBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }
    }

    public static class ArtistViewHolder extends RecyclerView.ViewHolder {
        ItemArtistVerticalBinding binding;

        public ArtistViewHolder(@NonNull ItemArtistVerticalBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }
    }

    /**
     * 搜索结果项
     */
    public static class SearchResultItem {
        private int type;
        private String title;
        private Song song;
        private Album album;
        private Artist artist;

        public SearchResultItem(int type, String title, Song song, Album album, Artist artist) {
            this.type = type;
            this.title = title;
            this.song = song;
            this.album = album;
            this.artist = artist;
        }

        public int getType() {
            return type;
        }

        public String getTitle() {
            return title;
        }

        public Song getSong() {
            return song;
        }

        public Album getAlbum() {
            return album;
        }

        public Artist getArtist() {
            return artist;
        }
    }
} 
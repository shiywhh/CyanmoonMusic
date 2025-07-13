package com.magicalstory.music.homepage;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.navigation.Navigation;

import com.magicalstory.music.base.BaseFragment;
import com.magicalstory.music.databinding.FragmentRecentSongsBinding;
import com.magicalstory.music.homepage.adapter.SongVerticalAdapter;
import com.magicalstory.music.model.Song;

import org.litepal.LitePal;

import java.util.ArrayList;
import java.util.List;

/**
 * 最近添加的歌曲Fragment
 * 显示所有最近添加的歌曲列表
 */
public class RecentSongsFragment extends BaseFragment<FragmentRecentSongsBinding> {

    private SongVerticalAdapter songAdapter;
    private List<Song> songList;

    @Override
    protected FragmentRecentSongsBinding getViewBinding(LayoutInflater inflater, ViewGroup container) {
        return FragmentRecentSongsBinding.inflate(inflater, container, false);
    }

    @Override
    protected void initView() {
        super.initView();
        
        // 初始化RecyclerView
        initRecyclerView();
        
        // 加载数据
        loadRecentSongs();
    }

    @Override
    protected void initListener() {
        super.initListener();
        
        // 设置返回按钮点击事件
        binding.toolbar.setNavigationOnClickListener(v -> {
            // 使用Navigation组件进行返回，会自动应用返回动画
            Navigation.findNavController(requireView()).popBackStack();
        });
    }

    /**
     * 初始化RecyclerView
     */
    private void initRecyclerView() {
        songList = new ArrayList<>();
        songAdapter = new SongVerticalAdapter(getContext(), songList);
        
        binding.rvRecentSongs.setLayoutManager(new LinearLayoutManager(getContext()));
        binding.rvRecentSongs.setAdapter(songAdapter);
        
        // 设置歌曲点击事件
        songAdapter.setOnItemClickListener((song, position) -> {
            // TODO: 播放歌曲
            // 这里可以添加播放歌曲的逻辑
        });
    }

    /**
     * 加载最近添加的歌曲
     */
    private void loadRecentSongs() {
        // 从数据库查询所有歌曲，按添加时间倒序排列
        List<Song> recentSongs = LitePal.order("dateAdded desc").find(Song.class);
        
        if (recentSongs != null && !recentSongs.isEmpty()) {
            songList.clear();
            songList.addAll(recentSongs);
            songAdapter.notifyDataSetChanged();
            
            // 显示列表，隐藏空状态
            binding.rvRecentSongs.setVisibility(View.VISIBLE);
            binding.layoutEmpty.setVisibility(View.GONE);
        } else {
            // 显示空状态，隐藏列表
            binding.rvRecentSongs.setVisibility(View.GONE);
            binding.layoutEmpty.setVisibility(View.VISIBLE);
        }
    }
}
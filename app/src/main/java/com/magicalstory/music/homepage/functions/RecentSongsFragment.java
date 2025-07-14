package com.magicalstory.music.homepage.functions;

import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.navigation.Navigation;

import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.magicalstory.music.base.BaseFragment;
import com.magicalstory.music.databinding.FragmentRecentSongsBinding;
import com.magicalstory.music.homepage.adapter.SongVerticalAdapter;
import com.magicalstory.music.model.Song;

import org.litepal.LitePal;

import java.util.ArrayList;
import java.util.List;

/**
 * 歌曲列表Fragment
 * 可以显示不同类型的歌曲列表（最近添加、我的收藏、随机推荐）
 */
public class RecentSongsFragment extends BaseFragment<FragmentRecentSongsBinding> {

    // 数据类型常量
    private static final String DATA_TYPE_RECENT = "recent";
    private static final String DATA_TYPE_FAVORITE = "favorite";
    private static final String DATA_TYPE_RANDOM = "random";

    private SongVerticalAdapter songAdapter;
    private List<Song> songList;
    private Handler mainHandler;
    private String dataType;

    @Override
    protected FragmentRecentSongsBinding getViewBinding(LayoutInflater inflater, ViewGroup container) {
        return FragmentRecentSongsBinding.inflate(inflater, container, false);
    }

    @Override
    protected FloatingActionButton getFab() {
        return binding.fab;
    }

    @Override
    protected void initView() {
        super.initView();

        // 获取参数
        Bundle arguments = getArguments();
        if (arguments != null) {
            dataType = arguments.getString("dataType", DATA_TYPE_RECENT);
        } else {
            dataType = DATA_TYPE_RECENT;
        }

        // 根据数据类型设置标题
        setupTitle();

        // 初始化Handler
        mainHandler = new Handler(Looper.getMainLooper());

        // 初始化RecyclerView
        initRecyclerView();

        // 加载数据
        loadSongsByType();
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
     * 根据数据类型设置标题
     */
    private void setupTitle() {
        String title;
        switch (dataType) {
            case DATA_TYPE_FAVORITE:
                title = "我的收藏";
                break;
            case DATA_TYPE_RANDOM:
                title = "随机推荐";
                break;
            case DATA_TYPE_RECENT:
            default:
                title = "最近添加的歌曲";
                break;
        }
        binding.toolbar.setTitle(title);
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
     * 根据数据类型加载歌曲
     */
    private void loadSongsByType() {

        // 创建新线程进行数据查询
        Thread loadThread = new Thread(() -> {
            try {
                List<Song> songs;

                // 根据数据类型查询不同的数据
                switch (dataType) {
                    case DATA_TYPE_FAVORITE:
                        // 我的收藏 - 暂时使用随机歌曲代替，实际应该从收藏表查询
                        songs = LitePal.order("random()").find(Song.class);
                        break;
                    case DATA_TYPE_RANDOM:
                        // 随机推荐
                        songs = LitePal.order("random()").find(Song.class);
                        break;
                    case DATA_TYPE_RECENT:
                    default:
                        // 最近添加的歌曲，按添加时间倒序排列
                        songs = LitePal.order("dateAdded desc").find(Song.class);
                        break;
                }

                // 切换到主线程更新UI
                mainHandler.post(() -> {
                    // 隐藏进度圈
                    binding.progressBar.setVisibility(View.GONE);

                    if (songs != null && !songs.isEmpty()) {
                        songList.clear();
                        songList.addAll(songs);
                        songAdapter.notifyDataSetChanged();

                        // 显示列表，隐藏空状态
                        binding.rvRecentSongs.setVisibility(View.VISIBLE);
                        binding.layoutEmpty.setVisibility(View.GONE);
                    } else {
                        // 显示空状态，隐藏列表
                        binding.rvRecentSongs.setVisibility(View.GONE);
                        binding.layoutEmpty.setVisibility(View.VISIBLE);
                    }
                });

            } catch (Exception e) {
                e.printStackTrace();
                // 发生错误时也要隐藏进度圈
                mainHandler.post(() -> {
                    binding.progressBar.setVisibility(View.GONE);
                    binding.rvRecentSongs.setVisibility(View.GONE);
                    binding.layoutEmpty.setVisibility(View.VISIBLE);
                });
            }
        });

        // 启动线程
        loadThread.start();
    }
}
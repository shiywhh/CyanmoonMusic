package com.magicalstory.music.homepage.functions;

import android.os.Handler;
import android.os.Looper;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.recyclerview.widget.GridLayoutManager;
import androidx.navigation.Navigation;

import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.magicalstory.music.base.BaseFragment;
import com.magicalstory.music.databinding.FragmentArtistBinding;
import com.magicalstory.music.homepage.adapter.ArtistGridAdapter;
import com.magicalstory.music.model.Artist;

import org.litepal.LitePal;

import java.util.ArrayList;
import java.util.List;

/**
 * 最近听过的艺术家Fragment
 * 显示所有艺术家的宫格双列布局
 */
public class RecentArtistFragment extends BaseFragment<FragmentArtistBinding> {

    private ArtistGridAdapter artistAdapter;
    private List<Artist> artistList;
    private Handler mainHandler;

    @Override
    protected FragmentArtistBinding getViewBinding(LayoutInflater inflater, ViewGroup container) {
        return FragmentArtistBinding.inflate(inflater, container, false);
    }

    @Override
    protected FloatingActionButton getFab() {
        return binding.fab;
    }

    @Override
    protected void initView() {
        super.initView();

        // 初始化Handler
        mainHandler = new Handler(Looper.getMainLooper());

        // 初始化RecyclerView
        initRecyclerView();

        // 加载数据
        loadArtists();
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
        artistList = new ArrayList<>();
        artistAdapter = new ArtistGridAdapter(getContext(), artistList);

        // 设置宫格双列布局
        GridLayoutManager gridLayoutManager = new GridLayoutManager(getContext(), 2);
        binding.rvArtists.setLayoutManager(gridLayoutManager);
        binding.rvArtists.setAdapter(artistAdapter);

        // 设置艺术家点击事件
        artistAdapter.setOnItemClickListener((artist, position) -> {
            // TODO: 播放艺术家歌曲
            // 这里可以添加播放艺术家歌曲的逻辑
        });
    }

    /**
     * 加载艺术家数据
     */
    private void loadArtists() {
        // 创建新线程进行数据查询
        Thread loadThread = new Thread(() -> {
            try {
                // 从数据库查询所有艺术家
                List<Artist> artists = LitePal.findAll(Artist.class);

                // 切换到主线程更新UI
                mainHandler.post(() -> {
                    // 隐藏进度圈
                    binding.progressBar.setVisibility(View.GONE);

                    if (artists != null && !artists.isEmpty()) {
                        artistList.clear();
                        artistList.addAll(artists);
                        artistAdapter.notifyDataSetChanged();

                        // 显示列表，隐藏空状态
                        binding.rvArtists.setVisibility(View.VISIBLE);
                        binding.layoutEmpty.setVisibility(View.GONE);
                    } else {
                        // 显示空状态，隐藏列表
                        binding.rvArtists.setVisibility(View.GONE);
                        binding.layoutEmpty.setVisibility(View.VISIBLE);
                    }
                });

            } catch (Exception e) {
                e.printStackTrace();
                // 发生错误时也要隐藏进度圈
                mainHandler.post(() -> {
                    binding.progressBar.setVisibility(View.GONE);
                    binding.rvArtists.setVisibility(View.GONE);
                    binding.layoutEmpty.setVisibility(View.VISIBLE);
                });
            }
        });

        // 启动线程
        loadThread.start();
    }
} 
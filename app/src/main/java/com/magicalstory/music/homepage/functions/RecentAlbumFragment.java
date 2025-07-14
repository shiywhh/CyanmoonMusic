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
import com.magicalstory.music.databinding.FragmentAlbumBinding;
import com.magicalstory.music.homepage.adapter.AlbumGridAdapter;
import com.magicalstory.music.model.Album;

import org.litepal.LitePal;

import java.util.ArrayList;
import java.util.List;

/**
 * 最近播放专辑Fragment
 * 显示所有专辑的宫格双列布局
 */
public class RecentAlbumFragment extends BaseFragment<FragmentAlbumBinding> {

    private AlbumGridAdapter albumAdapter;
    private List<Album> albumList;
    private Handler mainHandler;

    @Override
    protected FragmentAlbumBinding getViewBinding(LayoutInflater inflater, ViewGroup container) {
        return FragmentAlbumBinding.inflate(inflater, container, false);
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
        loadAlbums();
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
        albumList = new ArrayList<>();
        albumAdapter = new AlbumGridAdapter(getContext(), albumList);

        // 设置宫格双列布局
        GridLayoutManager gridLayoutManager = new GridLayoutManager(getContext(), 2);
        binding.rvAlbums.setLayoutManager(gridLayoutManager);
        binding.rvAlbums.setAdapter(albumAdapter);

        // 设置专辑点击事件
        albumAdapter.setOnItemClickListener((album, position) -> {
            // TODO: 播放专辑
            // 这里可以添加播放专辑的逻辑
        });
    }

    /**
     * 加载专辑数据
     */
    private void loadAlbums() {
        // 创建新线程进行数据查询
        Thread loadThread = new Thread(() -> {
            try {
                // 从数据库查询所有专辑
                List<Album> albums = LitePal.findAll(Album.class);

                // 切换到主线程更新UI
                mainHandler.post(() -> {
                    // 隐藏进度圈
                    binding.progressBar.setVisibility(View.GONE);

                    if (albums != null && !albums.isEmpty()) {
                        albumList.clear();
                        albumList.addAll(albums);
                        albumAdapter.notifyDataSetChanged();

                        // 显示列表，隐藏空状态
                        binding.rvAlbums.setVisibility(View.VISIBLE);
                        binding.layoutEmpty.setVisibility(View.GONE);
                    } else {
                        // 显示空状态，隐藏列表
                        binding.rvAlbums.setVisibility(View.GONE);
                        binding.layoutEmpty.setVisibility(View.VISIBLE);
                    }
                });

            } catch (Exception e) {
                e.printStackTrace();
                // 发生错误时也要隐藏进度圈
                mainHandler.post(() -> {
                    binding.progressBar.setVisibility(View.GONE);
                    binding.rvAlbums.setVisibility(View.GONE);
                    binding.layoutEmpty.setVisibility(View.VISIBLE);
                });
            }
        });

        // 启动线程
        loadThread.start();
    }
} 
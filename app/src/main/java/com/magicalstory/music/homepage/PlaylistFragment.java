package com.magicalstory.music.homepage;

import android.view.LayoutInflater;
import android.view.ViewGroup;

import com.magicalstory.music.base.BaseFragment;
import com.magicalstory.music.databinding.FragmentPlaylistBinding;

public class PlaylistFragment extends BaseFragment<FragmentPlaylistBinding> {

    @Override
    protected FragmentPlaylistBinding getViewBinding(LayoutInflater inflater, ViewGroup container) {
        return FragmentPlaylistBinding.inflate(inflater, container, false);
    }
    
    @Override
    protected void initView() {
        super.initView();
        // 初始化视图
        binding.tvTitle.setText("播放列表 - ViewBinding");
    }
    
    @Override
    protected void initData() {
        super.initData();
        // 初始化数据
    }
    
    @Override
    protected void initListener() {
        super.initListener();
        // 初始化监听器
    }

    @Override
    public boolean autoHideBottomNavigation() {
        return false;
    }
}
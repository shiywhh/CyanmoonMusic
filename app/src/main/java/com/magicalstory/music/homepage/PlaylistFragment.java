package com.magicalstory.music.homepage;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.magicalstory.music.R;
import com.magicalstory.music.base.BaseFragment;
import com.magicalstory.music.databinding.FragmentPlaylistBinding;

public class PlaylistFragment extends BaseFragment<FragmentPlaylistBinding> {

    @Override
    protected FragmentPlaylistBinding getViewBinding(LayoutInflater inflater, ViewGroup container) {
        return FragmentPlaylistBinding.inflate(inflater, container, false);
    }

    @Override
    protected boolean usePersistentView() {
        return true;
    }

    @Override
    protected int getLayoutRes() {
        return R.layout.fragment_playlist;
    }

    @Override
    protected FragmentPlaylistBinding bindPersistentView(View view) {
        return FragmentPlaylistBinding.bind(view);
    }
    
    @Override
    protected void initViewForPersistentView() {
        super.initViewForPersistentView();
        // 初始化视图
        binding.tvTitle.setText("播放列表 - ViewBinding");
    }
    
    @Override
    protected void initView() {
        super.initView();
        // 每次视图创建时需要执行的初始化代码
    }
    
    @Override
    protected void initDataForPersistentView() {
        super.initDataForPersistentView();
        // 初始化数据
    }
    
    @Override
    protected void initData() {
        super.initData();
        // 每次视图创建时需要执行的数据初始化代码
    }
    
    @Override
    protected void initListenerForPersistentView() {
        super.initListenerForPersistentView();
        // 初始化监听器
    }
    
    @Override
    protected void initListener() {
        super.initListener();
        // 每次视图创建时需要执行的监听器初始化代码
    }

    @Override
    public boolean autoHideBottomNavigation() {
        return false;
    }
}
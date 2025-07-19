package com.magicalstory.music.homepage;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.magicalstory.music.R;
import com.magicalstory.music.base.BaseFragment;
import com.magicalstory.music.databinding.FragmentLibraryBinding;

public class LibraryFragment extends BaseFragment<FragmentLibraryBinding> {

    @Override
    protected FragmentLibraryBinding getViewBinding(LayoutInflater inflater, ViewGroup container) {
        return FragmentLibraryBinding.inflate(inflater, container, false);
    }

    @Override
    protected boolean usePersistentView() {
        return true;
    }

    @Override
    protected int getLayoutRes() {
        return R.layout.fragment_library;
    }

    @Override
    protected FragmentLibraryBinding bindPersistentView(View view) {
        return FragmentLibraryBinding.bind(view);
    }
    
    @Override
    protected void initViewForPersistentView() {
        super.initViewForPersistentView();
        // 初始化视图
        binding.tvTitle.setText("音乐库 - ViewBinding");
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

    /**
     * 刷新音乐列表
     */
    @Override
    protected void onRefreshMusicList() {
        // 重新加载音乐库数据
        refreshFragmentAsync();
    }

    /**
     * 在后台线程执行刷新操作
     */
    @Override
    protected void performRefreshInBackground() {
        try {
            // 重新加载音乐库数据
            // 这里可以添加具体的刷新逻辑
            
            // 打印原始数据到控制台
            System.out.println("LibraryFragment后台刷新完成");
            
        } catch (Exception e) {
            e.printStackTrace();
            System.out.println("LibraryFragment后台刷新失败: " + e.getMessage());
        }
    }

    /**
     * 在主线程更新UI
     */
    @Override
    protected void updateUIAfterRefresh() {
        try {
            // 更新UI显示
            if (binding != null) {
                // 更新UI组件
                binding.tvTitle.setText("音乐库 - 已刷新");
            }
            
            System.out.println("LibraryFragment UI更新完成");
            
        } catch (Exception e) {
            e.printStackTrace();
            System.out.println("LibraryFragment UI更新失败: " + e.getMessage());
        }
    }
}
package com.magicalstory.music.homepage;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.magicalstory.music.R;
import com.magicalstory.music.base.BaseFragment;
import com.magicalstory.music.databinding.FragmentProfileBinding;

public class ProfileFragment extends BaseFragment<FragmentProfileBinding> {

    @Override
    protected FragmentProfileBinding getViewBinding(LayoutInflater inflater, ViewGroup container) {
        return FragmentProfileBinding.inflate(inflater, container, false);
    }

    @Override
    protected boolean usePersistentView() {
        return true;
    }

    @Override
    protected int getLayoutRes() {
        return R.layout.fragment_profile;
    }

    @Override
    protected FragmentProfileBinding bindPersistentView(View view) {
        return FragmentProfileBinding.bind(view);
    }

    @Override
    protected void initViewForPersistentView() {
        super.initViewForPersistentView();
        // 初始化视图
        binding.tvTitle.setText("个人资料 - ViewBinding");
    }


    @Override
    protected void initDataForPersistentView() {
        super.initDataForPersistentView();
        // 初始化数据
    }


    @Override
    protected void initListenerForPersistentView() {
        super.initListenerForPersistentView();
        // 初始化监听器
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
        // 重新加载个人资料数据
        refreshFragmentAsync();
    }

    /**
     * 在后台线程执行刷新操作
     */
    @Override
    protected void performRefreshInBackground() {
        try {
            // 重新加载个人资料数据
            // 这里可以添加具体的刷新逻辑
            
            // 打印原始数据到控制台
            System.out.println("ProfileFragment后台刷新完成");
            
        } catch (Exception e) {
            e.printStackTrace();
            System.out.println("ProfileFragment后台刷新失败: " + e.getMessage());
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
                binding.tvTitle.setText("个人资料 - 已刷新");
            }
            
            System.out.println("ProfileFragment UI更新完成");
            
        } catch (Exception e) {
            e.printStackTrace();
            System.out.println("ProfileFragment UI更新失败: " + e.getMessage());
        }
    }
}
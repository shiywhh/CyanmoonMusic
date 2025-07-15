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
}
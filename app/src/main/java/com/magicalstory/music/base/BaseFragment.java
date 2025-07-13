package com.magicalstory.music.base;

import android.app.Activity;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.viewbinding.ViewBinding;

/**
 * 基础Fragment类，所有Fragment都应该继承这个类
 * 提供公共方法和功能，支持ViewBinding
 */
public abstract class BaseFragment<VB extends ViewBinding> extends Fragment {

    protected VB binding;
    public Activity context;
    
    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = getViewBinding(inflater, container);
        return binding.getRoot();
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        context = getActivity();
    }


    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        initView();
        initData();
        initListener();
    }
    
    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }

    /**
     * 获取ViewBinding实例
     * @param inflater LayoutInflater
     * @param container ViewGroup
     * @return ViewBinding实例
     */
    protected abstract VB getViewBinding(LayoutInflater inflater, ViewGroup container);

    /**
     * 初始化视图
     */
    protected void initView() {
        // 子类可以重写此方法初始化视图
    }

    /**
     * 初始化数据
     */
    protected void initData() {
        // 子类可以重写此方法初始化数据
    }

    /**
     * 初始化监听器
     */
    protected void initListener() {
        // 子类可以重写此方法初始化监听器
    }

    /**
     * 显示Toast消息
     * @param message 消息内容
     */
    protected void showToast(String message) {
        if (getActivity() != null) {
            Toast.makeText(getActivity(), message, Toast.LENGTH_SHORT).show();
        }
    }
}
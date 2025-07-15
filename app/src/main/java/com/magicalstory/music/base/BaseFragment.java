package com.magicalstory.music.base;

import android.animation.ValueAnimator;
import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.DecelerateInterpolator;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.coordinatorlayout.widget.CoordinatorLayout;
import androidx.fragment.app.Fragment;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;
import androidx.viewbinding.ViewBinding;

import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.magicalstory.music.MainActivity;
import com.magicalstory.music.utils.screen.DensityUtil;

/**
 * 基础Fragment类，所有Fragment都应该继承这个类
 * 提供公共方法和功能，支持ViewBinding
 */
public abstract class BaseFragment<VB extends ViewBinding> extends Fragment {

    // 刷新广播常量
    public static final String ACTION_REFRESH_MUSIC_LIST = "com.magicalstory.music.REFRESH_MUSIC_LIST";

    protected VB binding;
    public Activity context;
    private BroadcastReceiver bottomSheetStateReceiver;
    private BroadcastReceiver refreshReceiver;
    
    // 持久化视图相关变量
    protected boolean hasInitializedRootView = false;
    private View rootView = null;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        // 子类可以重写usePersistentView方法来决定是否使用持久化视图
        if (usePersistentView()) {
            View view = getPersistentView(inflater, container, savedInstanceState, getLayoutRes());
            if (binding == null) {
                binding = bindPersistentView(view);
            }
            return view;
        } else {
            binding = getViewBinding(inflater, container);
            return binding.getRoot();
        }
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        context = getActivity();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        
        // 处理持久化视图的初始化逻辑
        if (!hasInitializedRootView) {
            hasInitializedRootView = true;
            // 首次创建时的初始化
            initViewForPersistentView();
            initDataForPersistentView();
            initListenerForPersistentView();
        }

        // 每次onViewCreated都会执行的初始化
        initView();
        initData();
        initListener();
        // 在初始化完成后注册fab相关的广播
        initFabHandling();
        // 注册刷新广播
        registerRefreshReceiver();
        initNavigationBar();
    }

    private void initNavigationBar() {
        System.out.println("autoHideBottomNavigation() = " + autoHideBottomNavigation());
        if (autoHideBottomNavigation()) {
            if (getActivity() instanceof MainActivity mainActivity) {
                mainActivity.hideBottomNavigation();
            }
        }
    }

    public boolean autoHideBottomNavigation() {
        return true;
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        // 取消注册广播接收器
        if (bottomSheetStateReceiver != null) {
            LocalBroadcastManager.getInstance(requireContext()).unregisterReceiver(bottomSheetStateReceiver);
            bottomSheetStateReceiver = null;
        }
        if (refreshReceiver != null) {
            LocalBroadcastManager.getInstance(requireContext()).unregisterReceiver(refreshReceiver);
            refreshReceiver = null;
        }
        // 对于持久化视图，不清理binding，保持视图状态
        // binding = null;
    }

    /**
     * 获取ViewBinding实例
     * @param inflater LayoutInflater
     * @param container ViewGroup
     * @return ViewBinding实例
     */
    protected abstract VB getViewBinding(LayoutInflater inflater, ViewGroup container);

    /**
     * 是否使用持久化视图
     * 子类可以重写此方法来决定是否使用持久化视图
     * @return true表示使用持久化视图，false表示使用普通视图
     */
    protected boolean usePersistentView() {
        return false;
    }

    /**
     * 获取布局资源ID
     * 当使用持久化视图时，子类需要重写此方法返回对应的布局资源ID
     * @return 布局资源ID
     */
    protected int getLayoutRes() {
        return 0;
    }

    /**
     * 绑定持久化视图
     * 当使用持久化视图时，子类需要重写此方法来绑定已存在的view
     * @param view 已存在的view
     * @return ViewBinding实例
     */
    protected VB bindPersistentView(View view) {
        return null;
    }

    /**
     * 获取持久化视图
     * @param inflater LayoutInflater
     * @param container ViewGroup
     * @param savedInstanceState Bundle
     * @param layout 布局资源ID
     * @return 持久化的View实例
     */
    protected View getPersistentView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState, int layout) {
        if (rootView == null) {
            // 首次创建视图
            rootView = inflater.inflate(layout, container, false);
        } else {
            // 视图已存在，需要从父容器中移除
            // 避免重复添加导致的异常
            ViewGroup parent = (ViewGroup) rootView.getParent();
            if (parent != null) {
                parent.removeView(rootView);
            }
        }
        return rootView;
    }

    /**
     * 获取当前页面的FloatingActionButton
     * 子类如果有fab需要处理，则重写此方法返回fab实例
     * @return FloatingActionButton实例，如果没有则返回null
     */
    protected FloatingActionButton getFab() {
        return null;
    }

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
     * 为持久化视图初始化视图（只在首次创建时执行）
     * 子类可以重写此方法来实现只需要执行一次的视图初始化逻辑
     */
    protected void initViewForPersistentView() {
        // 子类可以重写此方法
    }

    /**
     * 为持久化视图初始化数据（只在首次创建时执行）
     * 子类可以重写此方法来实现只需要执行一次的数据初始化逻辑
     */
    protected void initDataForPersistentView() {
        // 子类可以重写此方法
    }

    /**
     * 为持久化视图初始化监听器（只在首次创建时执行）
     * 子类可以重写此方法来实现只需要执行一次的监听器初始化逻辑
     */
    protected void initListenerForPersistentView() {
        // 子类可以重写此方法
    }

    /**
     * 初始化fab处理
     */
    private void initFabHandling() {
        FloatingActionButton fab = getFab();
        if (fab != null) {
            // 初始化fab的padding
            if (((MainActivity) context).showMiniPlayer()) {
                updateFabPaddingBottom(true);
            }

            // 注册广播接收器
            registerBottomSheetStateReceiver();
        }
    }

    /**
     * 注册底部面板状态变化的广播接收器
     */
    private void registerBottomSheetStateReceiver() {
        bottomSheetStateReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context1, Intent intent) {
                if (MainActivity.ACTION_BOTTOM_SHEET_STATE_CHANGED.equals(intent.getAction())) {
                    // 根据底部面板状态更新fab的paddingBottom
                    updateFabPaddingBottom(((MainActivity) context).showMiniPlayer());
                }
            }
        };

        IntentFilter filter = new IntentFilter(MainActivity.ACTION_BOTTOM_SHEET_STATE_CHANGED);
        LocalBroadcastManager.getInstance(requireContext()).registerReceiver(bottomSheetStateReceiver, filter);
    }

    /**
     * 根据底部面板状态更新fab的marginBottom
     */
    private void updateFabPaddingBottom(boolean isExpanded) {
        FloatingActionButton fab = getFab();
        if (fab == null) return;

        // 计算fab的marginBottom
        int targetMarginBottom = 0;

        if (isExpanded) {
            targetMarginBottom = DensityUtil.dip2px(context, 120);
        } else {
            targetMarginBottom = DensityUtil.dip2px(context, 30);
        }

        // 获取当前的marginBottom
        CoordinatorLayout.LayoutParams params = (CoordinatorLayout.LayoutParams) fab.getLayoutParams();
        int currentMarginBottom = params.bottomMargin;

        // 如果当前margin和目标margin相同，则不需要动画
        if (currentMarginBottom == targetMarginBottom) {
            return;
        }

        // 创建ValueAnimator来实现平滑的margin变化
        ValueAnimator animator = ValueAnimator.ofInt(currentMarginBottom, targetMarginBottom);
        animator.setDuration(300); // 动画持续时间300ms
        animator.setInterpolator(new DecelerateInterpolator()); // 使用减速插值器，让动画更自然

        animator.addUpdateListener(animation -> {
            int animatedValue = (int) animation.getAnimatedValue();
            params.bottomMargin = animatedValue;
            fab.setLayoutParams(params);
        });

        animator.start();
    }

    /**
     * 注册刷新广播接收器
     */
    private void registerRefreshReceiver() {
        refreshReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context1, Intent intent) {
                if (ACTION_REFRESH_MUSIC_LIST.equals(intent.getAction())) {
                    // 调用子类的刷新方法
                    onRefreshMusicList();
                }
            }
        };

        IntentFilter filter = new IntentFilter(ACTION_REFRESH_MUSIC_LIST);
        LocalBroadcastManager.getInstance(requireContext()).registerReceiver(refreshReceiver, filter);
    }

    /**
     * 刷新音乐列表
     * 子类可以重写此方法来实现自己的刷新逻辑
     */
    protected void onRefreshMusicList() {
        // 子类可以重写此方法
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        // 注销广播接收器
        if (bottomSheetStateReceiver != null) {
            LocalBroadcastManager.getInstance(requireContext()).unregisterReceiver(bottomSheetStateReceiver);
        }
        if (refreshReceiver != null) {
            LocalBroadcastManager.getInstance(requireContext()).unregisterReceiver(refreshReceiver);
        }
        // 清理持久化视图相关资源
        if (rootView != null) {
            ViewGroup parent = (ViewGroup) rootView.getParent();
            if (parent != null) {
                parent.removeView(rootView);
            }
            rootView = null;
        }
        binding = null;
        hasInitializedRootView = false;
    }

    /**
     * 清理不再需要的Fragment实例
     * 当Fragment过多时调用此方法
     */
    protected void clearUnusedFragments() {
        if (getActivity() != null) {
            androidx.fragment.app.FragmentManager fragmentManager = getActivity().getSupportFragmentManager();
            // 获取所有Fragment
            java.util.List<androidx.fragment.app.Fragment> fragments = fragmentManager.getFragments();
            
            // 如果Fragment数量超过阈值，清理一些不可见的Fragment
            if (fragments.size() > 10) {
                androidx.fragment.app.FragmentTransaction transaction = fragmentManager.beginTransaction();
                for (androidx.fragment.app.Fragment fragment : fragments) {
                    if (fragment != null && !fragment.isVisible() && !fragment.isAdded()) {
                        transaction.remove(fragment);
                    }
                }
                transaction.commit();
            }
        }
    }

}
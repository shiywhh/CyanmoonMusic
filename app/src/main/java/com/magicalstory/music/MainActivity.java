package com.magicalstory.music;

import android.animation.ValueAnimator;
import android.content.Intent;
import android.content.res.Configuration;
import android.os.Build;
import android.os.Bundle;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;
import androidx.navigation.NavController;
import androidx.navigation.ui.NavigationUI;

import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.bottomsheet.BottomSheetBehavior;
import com.gyf.immersionbar.ImmersionBar;
import com.magicalstory.music.databinding.ActivityMainBinding;
import com.magicalstory.music.homepage.HomeFragment;


public class MainActivity extends AppCompatActivity {

    private ActivityMainBinding binding;
    private BottomSheetBehavior<View> bottomSheetBehavior;
    private View miniPlayerContainer;
    private View fullPlayerContainer;
    private boolean isBottomNavigationVisible = true;
    private int bottomNavHeight;
    private int miniPlayerHeight;
    public boolean VoidHideBottomNavigation = false;

    // 广播常量
    public static final String ACTION_BOTTOM_SHEET_STATE_CHANGED = "com.magicalstory.music.BOTTOM_SHEET_STATE_CHANGED";


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setupStatusBar();
        // 使用ViewBinding
        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        // 初始化底部导航栏高度和mini player高度
        bottomNavHeight = getResources().getDimensionPixelSize(R.dimen.bottom_nav_height);
        miniPlayerHeight = getResources().getDimensionPixelSize(R.dimen.mini_player_height) + 50;

        setupBottomNavigation();
        setupBottomSheet();
        setupBackPressHandler();
    }

    private void setupStatusBar() {
        // 检查当前是否为暗黑模式
        boolean isDarkMode = (getResources().getConfiguration().uiMode & Configuration.UI_MODE_NIGHT_MASK)
                == Configuration.UI_MODE_NIGHT_YES;

        // 根据暗黑模式状态设置状态栏图标颜色
        ImmersionBar.with(this)
                .transparentBar().navigationBarDarkIcon(!isDarkMode)
                .statusBarDarkFont(!isDarkMode) // 暗黑模式下使用浅色图标，正常模式下使用深色图标
                .init();
    }

    private void setupBottomNavigation() {
        try {
            // 获取NavHostFragment
            androidx.fragment.app.Fragment navHostFragment = getSupportFragmentManager().findFragmentById(R.id.nav_host_fragment);
            if (navHostFragment != null) {
                // 从NavHostFragment获取NavController
                NavController navController = androidx.navigation.fragment.NavHostFragment.findNavController(navHostFragment);
                // 设置底部导航与NavController关联
                NavigationUI.setupWithNavController(binding.bottomNavigation, navController);
                // 默认选中主页
                binding.bottomNavigation.setSelectedItemId(R.id.nav_home);
                
                // 添加tab切换监听器
                binding.bottomNavigation.setOnItemSelectedListener(new BottomNavigationView.OnNavigationItemSelectedListener() {
                    @Override
                    public boolean onNavigationItemSelected(@NonNull MenuItem item) {
                        // 在切换fragment时设置VoidHideBottomNavigation=true避免误操作
                        VoidHideBottomNavigation = true;
                        // 调用默认的导航处理
                        return NavigationUI.onNavDestinationSelected(item, navController);
                    }
                });
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void setupBottomSheet() {
        // 获取BottomSheet相关视图
        View bottomSheet = binding.playerBottomSheet;
        bottomSheetBehavior = BottomSheetBehavior.from(bottomSheet);

        // 获取mini player和full player的容器
        miniPlayerContainer = bottomSheet.findViewById(R.id.mini_player_container);
        fullPlayerContainer = bottomSheet.findViewById(R.id.full_player_container);

        // 初始状态设置 - 两个播放器都保持可见，但只有mini player有透明度
        if (miniPlayerContainer != null) {
            miniPlayerContainer.setVisibility(View.VISIBLE);
            miniPlayerContainer.setAlpha(1.0f); // mini player 初始完全显示
        }
        if (fullPlayerContainer != null) {
            fullPlayerContainer.setVisibility(View.VISIBLE);
            fullPlayerContainer.setAlpha(0.0f); // full player 初始完全透明
        }

        // 设置 mini player 点击监听器
        if (miniPlayerContainer != null) {
            // 设置点击背景效果
            miniPlayerContainer.setClickable(true);
            miniPlayerContainer.setFocusable(true);

            miniPlayerContainer.setOnClickListener(v -> {
                // 点击 mini player 时展开 bottomsheet
                if (bottomSheetBehavior.getState() == BottomSheetBehavior.STATE_COLLAPSED) {
                    bottomSheetBehavior.setState(BottomSheetBehavior.STATE_EXPANDED);
                } else if (bottomSheetBehavior.getState() == BottomSheetBehavior.STATE_HIDDEN) {
                    // 如果当前是隐藏状态，先显示为收起状态，然后展开
                    bottomSheetBehavior.setState(BottomSheetBehavior.STATE_COLLAPSED);
                    bottomSheetBehavior.setState(BottomSheetBehavior.STATE_EXPANDED);
                }
            });
        }

        // 设置BottomSheet回调监听器
        bottomSheetBehavior.addBottomSheetCallback(new BottomSheetBehavior.BottomSheetCallback() {
            @Override
            public void onStateChanged(@NonNull View bottomSheet, int newState) {
                // 根据状态变化确保只有一个播放器可见
                if (miniPlayerContainer != null && fullPlayerContainer != null) {
                    switch (newState) {
                        case BottomSheetBehavior.STATE_COLLAPSED:
                            // 完全收起状态 - 只显示 mini player
                            miniPlayerContainer.setVisibility(View.VISIBLE);
                            miniPlayerContainer.setAlpha(1.0f);
                            // 保持 full player 可见但透明，避免布局变化
                            fullPlayerContainer.setVisibility(View.VISIBLE);
                            fullPlayerContainer.setAlpha(0.0f);
                            // 底部导航栏完全可见
                            if (binding != null && binding.bottomNavigation != null) {
                                binding.bottomNavigation.setAlpha(1.0f);
                            }
                            // 发送折叠状态广播
                            sendBottomSheetStateChangedBroadcast(false, true);
                            break;
                        case BottomSheetBehavior.STATE_EXPANDED:
                            // 完全展开状态 - 只显示 full player
                            // 保持 mini player 可见但透明，避免布局变化
                            miniPlayerContainer.setVisibility(View.VISIBLE);
                            miniPlayerContainer.setAlpha(0.0f);
                            fullPlayerContainer.setVisibility(View.VISIBLE);
                            fullPlayerContainer.setAlpha(1.0f);
                            // 底部导航栏完全透明
                            if (binding != null && binding.bottomNavigation != null) {
                                binding.bottomNavigation.setAlpha(0.0f);
                            }
                            // 发送展开状态广播
                            sendBottomSheetStateChangedBroadcast(true, false);
                            break;
                        case BottomSheetBehavior.STATE_HIDDEN:
                            // 隐藏状态 - 播放器被完全隐藏
                            miniPlayerContainer.setVisibility(View.GONE);
                            miniPlayerContainer.setAlpha(0.0f);
                            fullPlayerContainer.setVisibility(View.GONE);
                            fullPlayerContainer.setAlpha(0.0f);
                            // 底部导航栏完全可见
                            if (binding != null && binding.bottomNavigation != null) {
                                binding.bottomNavigation.setAlpha(1.0f);
                            }
                            // 发送隐藏状态广播
                            sendBottomSheetStateChangedBroadcast(false, false);
                            // 可以在这里添加暂停音乐的逻辑
                            break;
                        case BottomSheetBehavior.STATE_DRAGGING:
                        case BottomSheetBehavior.STATE_SETTLING:
                            // 拖拽和稳定状态由 onSlide 处理
                            // 确保两个播放器都保持可见状态
                            miniPlayerContainer.setVisibility(View.VISIBLE);
                            fullPlayerContainer.setVisibility(View.VISIBLE);
                            break;
                    }
                }
            }

            @Override
            public void onSlide(@NonNull View bottomSheet, float slideOffset) {
                // 根据滑动偏移量调整显示状态
                // slideOffset: 负值 = 向下滑动隐藏, 0 = 完全收起(peek状态), 1 = 完全展开

                if (miniPlayerContainer != null && fullPlayerContainer != null) {
                    // 处理向下滑动隐藏的情况
                    if (slideOffset < 0) {
                        // 向下滑动隐藏时，mini player 逐渐消失
                        // slideOffset 从 0 到 -1，透明度从 1 到 0
                        float hideAlpha = 1.0f + slideOffset; // slideOffset是负数，所以用加法
                        miniPlayerContainer.setAlpha(Math.max(0.0f, hideAlpha));
                        fullPlayerContainer.setAlpha(0.0f);
                        // 底部导航栏在隐藏时保持完全可见
                        if (binding != null && binding.bottomNavigation != null) {
                            binding.bottomNavigation.setAlpha(1.0f);
                        }
                        return;
                    }

                    // 在滑动过程中保持两个view都可见，避免突然的visibility变化导致闪烁
                    // 只有在完全静止状态才改变visibility

                    // 设置切换阈值为 0.2，使切换更平滑
                    float threshold = 0.2f;
                    // 设置底部导航栏淡化的阈值，比播放器切换更早开始淡化
                    float navFadeThreshold = 0.1f;

                    // 控制底部导航栏的透明度 - 快速淡化
                    if (binding != null && binding.bottomNavigation != null) {
                        if (slideOffset > navFadeThreshold) {
                            // 当滑动超过导航栏淡化阈值时，快速淡化底部导航栏
                            // 从 navFadeThreshold 到 0.4 的范围内完成淡化，让淡化过程更快
                            float navFadeRange = 0.4f - navFadeThreshold;
                            float navFadeProgress = Math.min(1.0f, (slideOffset - navFadeThreshold) / navFadeRange);
                            float navAlpha = 1.0f - navFadeProgress;
                            binding.bottomNavigation.setAlpha(Math.max(0.0f, navAlpha));
                        } else {
                            // 在阈值以下时，底部导航栏保持完全可见
                            binding.bottomNavigation.setAlpha(1.0f);
                        }
                    }

                    if (slideOffset > threshold) {
                        // 展开超过阈值时，显示 full player，淡出 mini player
                        // Full player 透明度：从 threshold 到 1.0 时，透明度从 0 到 1
                        float fullPlayerAlpha = (slideOffset - threshold) / (1.0f - threshold);
                        fullPlayerContainer.setAlpha(Math.min(1.0f, fullPlayerAlpha));

                        // Mini player 完全透明
                        miniPlayerContainer.setAlpha(0.0f);

                    } else {
                        // 收起低于阈值时，显示 mini player，淡出 full player
                        // Mini player 透明度：从 0 到 threshold 时，透明度从 1 到 0
                        float miniPlayerAlpha = 1.0f - (slideOffset / threshold);
                        miniPlayerContainer.setAlpha(Math.max(0.0f, miniPlayerAlpha));

                        // Full player 完全透明
                        fullPlayerContainer.setAlpha(0.0f);
                    }
                }
            }
        });
    }

    /**
     * 显示底部导航栏
     */
    public void showBottomNavigation() {
        if (VoidHideBottomNavigation) {
            VoidHideBottomNavigation = false;
            return;
        }
        if (!isBottomNavigationVisible) {
            animateBottomNavigation(true);
            // 恢复player_bottom_sheet的peekHeight
            if (bottomSheetBehavior != null) {
                int targetPeekHeight = getResources().getDimensionPixelSize(R.dimen.bottom_nav_mini_player_peek_height);
                animatePeekHeight(targetPeekHeight);
            }
        }
    }

    /**
     * 隐藏底部导航栏
     */
    public void hideBottomNavigation() {
        if (VoidHideBottomNavigation) {
            VoidHideBottomNavigation = false;
            return;
        }
        if (isBottomNavigationVisible) {
            animateBottomNavigation(false);
            // 调整player_bottom_sheet的peekHeight为mini_player_height
            if (bottomSheetBehavior != null) {
                animatePeekHeight(miniPlayerHeight);
            }
        }
    }

    /**
     * 动画显示/隐藏底部导航栏
     * @param show true表示显示，false表示隐藏
     */
    private void animateBottomNavigation(boolean show) {
        if (binding == null) return;

        // 获取当前的translationY值
        float currentTranslationY = binding.bottomNavigation.getTranslationY();
        float targetTranslationY = show ? 0f : bottomNavHeight;

        // 如果已经在目标位置，直接返回
        if (Math.abs(currentTranslationY - targetTranslationY) < 1) {
            return;
        }

        // 创建动画
        ValueAnimator animator = ValueAnimator.ofFloat(currentTranslationY, targetTranslationY);
        animator.setDuration(300); // 动画持续时间300毫秒

        animator.addUpdateListener(animation -> {
            float translationY = (Float) animation.getAnimatedValue();
            binding.bottomNavigation.setTranslationY(translationY);

            // 移除底部边距调整，只保留translationY动画
        });

        animator.start();
        isBottomNavigationVisible = show;
    }

    /**
     * 动画改变BottomSheet的peekHeight
     * @param targetPeekHeight 目标peekHeight值
     */
    private void animatePeekHeight(int targetPeekHeight) {
        if (bottomSheetBehavior == null) return;

        // 获取当前的peekHeight
        int currentPeekHeight = bottomSheetBehavior.getPeekHeight();

        // 如果已经在目标值，直接返回
        if (currentPeekHeight == targetPeekHeight) {
            return;
        }

        // 创建动画
        ValueAnimator animator = ValueAnimator.ofInt(currentPeekHeight, targetPeekHeight);
        animator.setDuration(300); // 动画持续时间300毫秒

        animator.addUpdateListener(animation -> {
            int peekHeight = (Integer) animation.getAnimatedValue();
            bottomSheetBehavior.setPeekHeight(peekHeight);
        });

        animator.start();
    }

    //是否展开mini播放器
    public boolean showMiniPlayer() {
        return bottomSheetBehavior.getState() == BottomSheetBehavior.STATE_COLLAPSED;
    }

    /**
     * 发送底部面板状态变化的广播
     */
    private void sendBottomSheetStateChangedBroadcast(boolean isExpanded, boolean isCollapsed) {
        Intent intent = new Intent(ACTION_BOTTOM_SHEET_STATE_CHANGED);
        LocalBroadcastManager.getInstance(this).sendBroadcast(intent);
    }

    /**
     * 设置返回按键处理器
     */
    private void setupBackPressHandler() {
        // API 33及以上使用新的OnBackInvokedDispatcher
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            getOnBackInvokedDispatcher().registerOnBackInvokedCallback(
                    1000000, // PRIORITY_DEFAULT = 1000000
                    () -> {
                        if (!handleBackPress()) {
                            // 如果没有处理，则调用默认的finish()
                            finish();
                        }
                    }
            );
        }
        // API 33以下使用传统的onBackPressed()方法
    }

    /**
     * 处理返回按键逻辑
     * @return true表示已处理，false表示未处理
     */
    private boolean handleBackPress() {
        // 2. 检查fullplayer是否展开
        if (handleFullPlayerBack()) {
            return true;
        }

        // 1. 检查HomeFragment的搜索框是否展开
        if (handleSearchViewBack()) {
            return true;
        }


        // 3. 执行默认的返回行为
        return false;
    }

    /**
     * 处理搜索框的返回逻辑
     * @return true表示已处理，false表示未处理
     */
    private boolean handleSearchViewBack() {
        // 获取当前显示的Fragment
        androidx.fragment.app.Fragment currentFragment = getSupportFragmentManager().findFragmentById(R.id.nav_host_fragment);
        if (currentFragment instanceof androidx.navigation.fragment.NavHostFragment) {
            androidx.navigation.fragment.NavHostFragment navHostFragment = (androidx.navigation.fragment.NavHostFragment) currentFragment;
            androidx.fragment.app.Fragment primaryFragment = navHostFragment.getChildFragmentManager().getPrimaryNavigationFragment();

            if (primaryFragment instanceof HomeFragment) {
                HomeFragment homeFragment = (HomeFragment) primaryFragment;
                return homeFragment.closeSearchView();
            }
        }
        return false;
    }

    /**
     * 处理fullplayer的返回逻辑
     * @return true表示已处理，false表示未处理
     */
    private boolean handleFullPlayerBack() {
        if (bottomSheetBehavior != null &&
                bottomSheetBehavior.getState() == BottomSheetBehavior.STATE_EXPANDED) {
            bottomSheetBehavior.setState(BottomSheetBehavior.STATE_COLLAPSED);
            return true;
        }
        return false;
    }

    /**
     * 重写onBackPressed方法以支持API 33以下的设备
     */
    @Override
    public void onBackPressed() {
        if (!handleBackPress()) {
            super.onBackPressed();
        }
    }
}
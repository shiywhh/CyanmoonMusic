package com.magicalstory.music;

import android.animation.ValueAnimator;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.res.Configuration;
import android.media.session.PlaybackState;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;

import androidx.core.splashscreen.SplashScreen;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;
import androidx.media3.common.MediaItem;
import androidx.media3.common.PlaybackException;
import androidx.media3.common.Player;
import androidx.media3.common.util.UnstableApi;
import androidx.media3.session.MediaController;
import androidx.media3.session.SessionToken;
import androidx.navigation.NavController;
import androidx.navigation.NavOptions;
import androidx.navigation.ui.NavigationUI;

import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.bottomsheet.BottomSheetBehavior;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.MoreExecutors;
import com.google.gson.reflect.TypeToken;
import com.gyf.immersionbar.ImmersionBar;
import com.hjq.gson.factory.GsonFactory;
import com.magicalstory.music.databinding.ActivityMainBinding;
import com.magicalstory.music.homepage.HomeFragment;
import com.magicalstory.music.model.Song;
import com.magicalstory.music.player.MiniPlayerFragment;
import com.magicalstory.music.player.FullPlayerFragment;
import com.magicalstory.music.player.PlaylistManager;
import com.magicalstory.music.service.MusicService;
import com.magicalstory.music.player.MediaControllerHelper;
import com.magicalstory.music.player.MediaControllerHelper.PlaybackStateListener;
import com.magicalstory.music.utils.app.ToastUtils;
import com.tencent.mmkv.MMKV;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;

/**
 * 主Activity - 完全按照Google Media3框架最佳实践重构
 *
 * 使用MediaController控制播放，而不是直接绑定Service
 * 通过MediaControllerHelper统一管理播放状态监听
 */
@UnstableApi
public class MainActivity extends AppCompatActivity implements PlaybackStateListener {

    private static final String TAG = "MainActivity";

    // 广播常量
    public static final String ACTION_BOTTOM_SHEET_STATE_CHANGED = "com.magicalstory.music.ACTION_BOTTOM_SHEET_STATE_CHANGED";

    private ActivityMainBinding binding;
    private BottomSheetBehavior<View> bottomSheetBehavior;
    private View miniPlayerContainer;
    private View fullPlayerContainer;
    private boolean isBottomNavigationVisible = true;
    private int bottomNavHeight;
    private int miniPlayerHeight;
    public boolean VoidHideBottomNavigation = false;

    // 播放器Fragment
    private MiniPlayerFragment miniPlayerFragment;
    private FullPlayerFragment fullPlayerFragment;

    // Media3 MediaController - 唯一实例
    private MediaController mediaController;
    private ListenableFuture<MediaController> controllerFuture;
    private MediaControllerHelper controllerHelper;
    private ArrayList<Song> songArrayList_lastest = new ArrayList<>();
    private boolean hasLastedPlayList = false;

    // SplashScreen相关
    private boolean isAppReady = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        // 在super.onCreate之前安装SplashScreen
        SplashScreen splashScreen = SplashScreen.installSplashScreen(this);

        super.onCreate(savedInstanceState);
        Log.d(TAG, "MainActivity created");

        setupStatusBar();

        // 使用ViewBinding
        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        // 初始化底部导航栏高度和mini player高度
        bottomNavHeight = getResources().getDimensionPixelSize(R.dimen.bottom_nav_height);
        miniPlayerHeight = getResources().getDimensionPixelSize(R.dimen.mini_player_height) + 50;

        setupBottomNavigation();
        setupBottomSheet();
        setupPlayerFragments();
        setupBackPressHandler();

        // 初始化MediaController
        initializeMediaController();

        // 设置SplashScreen保持显示的条件
        splashScreen.setKeepOnScreenCondition(() -> {
            return !isAppReady;
        });

        resumePlayList();
    }

    //恢复上次播放的列表
    private void resumePlayList() {
        binding.getRoot().postDelayed(() -> {
            if (hasLastedPlayList) {
                long dur = MMKV.defaultMMKV().decodeLong("playPosition", 0);
                System.out.println("dur = " + dur);
                if (dur == 0) {
                    dur = 1000;
                }
                MediaControllerHelper.getInstance().initializePlaylistWithPosition(songArrayList_lastest,
                        PlaylistManager.getInstance().getPlayListIndex(), dur);
            }
        }, 500);

    }

    public void hideSplashScreen() {
        isAppReady = true;
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
                binding.bottomNavigation.setOnItemSelectedListener((BottomNavigationView.OnNavigationItemSelectedListener) item -> {

                    VoidHideBottomNavigation = true;

                    if (bottomSheetBehavior.getState() == BottomSheetBehavior.STATE_EXPANDED) {
                        return false;
                    }

                    // 调用默认的导航处理
                    return NavigationUI.onNavDestinationSelected(item, navController);
                });
            }
        } catch (Exception e) {
            Log.e(TAG, "Error setting up bottom navigation", e);
        }
    }

    private void setupBottomSheet() {
        // 获取BottomSheet相关视图
        View bottomSheet = binding.playerBottomSheet;
        bottomSheetBehavior = BottomSheetBehavior.from(bottomSheet);

        Type listType = new TypeToken<List<Song>>() {
        }.getType();
        songArrayList_lastest = GsonFactory.getSingletonGson().fromJson(PlaylistManager.getInstance().getPlayList(), listType);
        hasLastedPlayList = songArrayList_lastest != null && !songArrayList_lastest.isEmpty();

        // 获取mini player和full player的容器
        miniPlayerContainer = bottomSheet.findViewById(R.id.mini_player_container);
        fullPlayerContainer = bottomSheet.findViewById(R.id.full_player_container);

        // 如果上次没有播放列表，然后初始状态设置为HIDDEN - 默认不显示播放器
        if (!hasLastedPlayList) {
            bottomSheetBehavior.setState(BottomSheetBehavior.STATE_HIDDEN);
        }

        // 初始状态设置 - 两个播放器都隐藏
        if (miniPlayerContainer != null && !hasLastedPlayList) {
            miniPlayerContainer.setVisibility(View.GONE);
            miniPlayerContainer.setAlpha(0.0f);
        }
        if (fullPlayerContainer != null) {
            fullPlayerContainer.setVisibility(View.GONE);
            fullPlayerContainer.setAlpha(0.0f);
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
                            if (binding != null) {
                                binding.bottomNavigation.setAlpha(1.0f);
                            }
                            break;
                        case BottomSheetBehavior.STATE_EXPANDED:
                            // 完全展开状态 - 只显示 full player
                            // 保持 mini player 可见但透明，避免布局变化
                            miniPlayerContainer.setVisibility(View.VISIBLE);
                            miniPlayerContainer.setAlpha(0.0f);
                            fullPlayerContainer.setVisibility(View.VISIBLE);
                            fullPlayerContainer.setAlpha(1.0f);
                            // 底部导航栏完全透明
                            binding.bottomNavigation.setAlpha(0.0f);
                            binding.bottomNavigation.setVisibility(View.INVISIBLE);
                            break;
                        case BottomSheetBehavior.STATE_HIDDEN:
                            // 隐藏状态 - 播放器被完全隐藏
                            miniPlayerContainer.setVisibility(View.GONE);
                            miniPlayerContainer.setAlpha(0.0f);
                            fullPlayerContainer.setVisibility(View.GONE);
                            fullPlayerContainer.setAlpha(0.0f);
                            // 底部导航栏完全可见
                            if (binding != null) {
                                binding.bottomNavigation.setAlpha(1.0f);
                            }
                            // 停止音乐播放
                            stopMusicPlayback();
                            LocalBroadcastManager.getInstance(MainActivity.this).sendBroadcast(new Intent(ACTION_BOTTOM_SHEET_STATE_CHANGED));
                            break;
                        case BottomSheetBehavior.STATE_DRAGGING:
                        case BottomSheetBehavior.STATE_SETTLING:
                            // 拖拽和稳定状态由 onSlide 处理
                            binding.bottomNavigation.setVisibility(View.VISIBLE);
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
     * 设置播放器Fragment
     */
    private void setupPlayerFragments() {
        // 创建播放器Fragment
        miniPlayerFragment = new MiniPlayerFragment();
        fullPlayerFragment = new FullPlayerFragment();

        // 添加到Fragment容器
        getSupportFragmentManager().beginTransaction()
                .replace(R.id.mini_player_container, miniPlayerFragment)
                .replace(R.id.full_player_container, fullPlayerFragment)
                .commit();
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
        return handleSearchViewBack();

        // 3. 执行默认的返回行为
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

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (keyCode == KeyEvent.KEYCODE_BACK) {
            if (handleBackPress()) {
                return true;
            }

            boolean isHomepage = isCurrentDestinationHomepage();
            if (isHomepage) {
                Intent home = new Intent(Intent.ACTION_MAIN);
                home.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                home.addCategory(Intent.CATEGORY_HOME);
                startActivity(home);
                return true;
            }

        }
        return super.onKeyDown(keyCode, event);
    }

    /**
     * 检查当前导航目的地是否为homepage
     * @return true表示当前在homepage，false表示不在
     */
    private boolean isCurrentDestinationHomepage() {
        try {
            // 获取NavHostFragment
            androidx.fragment.app.Fragment navHostFragment = getSupportFragmentManager().findFragmentById(R.id.nav_host_fragment);
            if (navHostFragment != null) {
                // 从NavHostFragment获取NavController
                NavController navController = androidx.navigation.fragment.NavHostFragment.findNavController(navHostFragment);

                // 获取当前目的地
                int currentDestinationId = navController.getCurrentDestination().getId();

                // 检查是否为nav_home
                return currentDestinationId == R.id.nav_home;
            }
        } catch (Exception e) {
            Log.e(TAG, "检查当前导航目的地失败", e);
        }
        return false;
    }

    /**
     * 初始化MediaController
     */
    private void initializeMediaController() {
        Log.d(TAG, "开始初始化MediaController");

        // 创建SessionToken
        SessionToken sessionToken = new SessionToken(this, new ComponentName(this, MusicService.class));
        Log.d(TAG, "SessionToken创建成功: " + sessionToken);

        // 创建MediaController
        controllerFuture = new MediaController.Builder(this, sessionToken).buildAsync();
        Log.d(TAG, "MediaController构建器创建成功");

        // 设置回调监听
        controllerFuture.addListener(() -> {
            try {
                Log.d(TAG, "MediaController连接回调触发");
                mediaController = controllerFuture.get();
                Log.d(TAG, "MediaController获取成功");

                // 检查MediaController状态
                Log.d(TAG, "MediaController状态检查:");
                Log.d(TAG, "- 是否连接: " + mediaController.isConnected());
                Log.d(TAG, "- 播放状态: " + mediaController.getPlaybackState());
                Log.d(TAG, "- 是否播放: " + mediaController.isPlaying());
                Log.d(TAG, "- 媒体项数量: " + mediaController.getMediaItemCount());

                // 初始化辅助类（MediaControllerHelper会自动添加Player.Listener）
                controllerHelper = MediaControllerHelper.getInstance(mediaController, this);
                Log.d(TAG, "MediaControllerHelper初始化成功");

                // 添加MainActivity作为播放状态监听器
                controllerHelper.addPlaybackStateListener(this);
                Log.d(TAG, "MainActivity已添加为播放状态监听器");

                // 添加播放状态监听器到MediaControllerHelper
                Log.d(TAG, "MediaController连接成功");

                // 通知Fragment MediaController已准备好
                notifyFragmentsControllerReady();

            } catch (ExecutionException | InterruptedException e) {
                Log.e(TAG, "MediaController创建失败", e);
                if (e.getCause() != null) {
                    Log.e(TAG, "失败原因: " + e.getCause().getMessage());
                }
            }
        }, MoreExecutors.directExecutor());

        Log.d(TAG, "MediaController初始化设置完成");
    }

    /**
     * 通知Fragment MediaControllerHelper已准备好
     */
    private void notifyFragmentsControllerReady() {
        Log.d(TAG, "通知Fragment MediaControllerHelper已准备好");

        if (miniPlayerFragment != null) {
            Log.d(TAG, "设置MiniPlayerFragment的MediaControllerHelper");
            miniPlayerFragment.setMediaControllerHelper(controllerHelper);
        } else {
            Log.w(TAG, "MiniPlayerFragment为null");
        }

        if (fullPlayerFragment != null) {
            Log.d(TAG, "设置FullPlayerFragment的MediaControllerHelper");
            fullPlayerFragment.setMediaControllerHelper(controllerHelper);
        } else {
            Log.w(TAG, "FullPlayerFragment为null");
        }

        Log.d(TAG, "Fragment通知完成");
    }

    /**
     * 设置播放列表
     */
    public void setPlaylist(List<Song> songs) {
        Log.d(TAG, "设置播放列表请求，歌曲数量: " + songs.size());
        if (controllerHelper != null) {
            controllerHelper.setPlaylist(songs);
        } else {
            Log.w(TAG, "MediaControllerHelper未准备好");
        }
    }

    /**
     * 智能播放列表中的歌曲
     * 优化后的版本，使用延迟加载避免性能问题
     */
    public void playFromPlaylist(List<Song> songs, int position) {
        long startTime = System.currentTimeMillis();
        Log.d(TAG, "播放列表中的歌曲请求: position=" + position);

        if (controllerHelper != null) {
            // 检查是否需要更新播放列表
            long playlistCheckStart = System.currentTimeMillis();
            List<Song> currentPlaylist = controllerHelper.getPlaylist();
            boolean needUpdatePlaylist = false;

            if (currentPlaylist.size() != songs.size()) {
                needUpdatePlaylist = true;
            } else {
                // 比较播放列表内容是否相同（只比较前几首和当前位置附近的歌曲）
                int checkRange = Math.min(10, songs.size());
                for (int i = 0; i < checkRange; i++) {
                    if (!songs.get(i).getPath().equals(currentPlaylist.get(i).getPath())) {
                        needUpdatePlaylist = true;
                        break;
                    }
                }

                // 如果当前位置的歌曲不同，也需要更新
                if (!needUpdatePlaylist && position < songs.size() && position < currentPlaylist.size()) {
                    if (!songs.get(position).getPath().equals(currentPlaylist.get(position).getPath())) {
                        needUpdatePlaylist = true;
                    }
                }
            }

            long playlistCheckEnd = System.currentTimeMillis();
            Log.d(TAG, "播放列表比较耗时: " + (playlistCheckEnd - playlistCheckStart) + "ms");

            if (needUpdatePlaylist) {
                Log.d(TAG, "需要更新播放列表，使用延迟加载");
                long updatePlaylistStart = System.currentTimeMillis();
                controllerHelper.setPlaylist(songs, position);
                long updatePlaylistEnd = System.currentTimeMillis();
                Log.d(TAG, "更新播放列表耗时: " + (updatePlaylistEnd - updatePlaylistStart) + "ms");
            } else {
                Log.d(TAG, "播放列表无需更新，直接播放");
                long playAtIndexStart = System.currentTimeMillis();
                controllerHelper.playAtIndex(position);
                long playAtIndexEnd = System.currentTimeMillis();
                Log.d(TAG, "直接播放耗时: " + (playAtIndexEnd - playAtIndexStart) + "ms");
            }
        } else {
            Log.w(TAG, "MediaControllerHelper未准备好");
        }

        long totalTime = System.currentTimeMillis() - startTime;
        Log.d(TAG, "播放列表请求处理完成，总耗时: " + totalTime + "ms");

        // 如果bottomSheet处于隐藏状态，则显示为收起状态
        Log.d(TAG, "显示播放器");
        showPlayer();

        binding.getRoot().postDelayed(() -> LocalBroadcastManager.getInstance(MainActivity.this).sendBroadcast(new Intent(ACTION_BOTTOM_SHEET_STATE_CHANGED)), 0);
    }

    /**
     * 获取当前播放的歌曲
     */
    @Nullable
    public Song getCurrentSong() {
        if (controllerHelper != null) {
            if (controllerHelper.getPlaybackState() == 1) {
                return null;
            }
            return controllerHelper.getCurrentSong();
        }
        return null;
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

    /**
     * 是否展开mini播放器
     */
    public boolean showMiniPlayer() {
        return bottomSheetBehavior.getState() != BottomSheetBehavior.STATE_HIDDEN;
    }

    /**
     * 显示播放器
     */
    public void showPlayer() {
        if (bottomSheetBehavior != null &&
                bottomSheetBehavior.getState() == BottomSheetBehavior.STATE_HIDDEN) {
            bottomSheetBehavior.setState(BottomSheetBehavior.STATE_COLLAPSED);
        }
    }

    /**
     * 展开BottomSheet到完整播放器
     */
    public void expandBottomSheet() {
        if (bottomSheetBehavior != null) {
            if (bottomSheetBehavior.getState() == BottomSheetBehavior.STATE_COLLAPSED) {
                bottomSheetBehavior.setState(BottomSheetBehavior.STATE_EXPANDED);
            } else if (bottomSheetBehavior.getState() == BottomSheetBehavior.STATE_HIDDEN) {
                // 如果当前是隐藏状态，先显示为收起状态，然后展开
                bottomSheetBehavior.setState(BottomSheetBehavior.STATE_COLLAPSED);
                bottomSheetBehavior.setState(BottomSheetBehavior.STATE_EXPANDED);
            }
        }
    }

    /**
     * 停止音乐播放但保持服务运行
     */
    public void stopMusicPlayback() {
        if (mediaController != null) {
            mediaController.stop();
            PlaylistManager.getInstance().clearPlaylist();
            controllerHelper.notifyStopPlay();
            bottomSheetBehavior.setState(BottomSheetBehavior.STATE_HIDDEN);
        }
    }

    @Override
    protected void onStart() {
        super.onStart();
        Log.d(TAG, "MainActivity started");
    }

    @Override
    protected void onStop() {
        super.onStop();
        Log.d(TAG, "MainActivity stopped");
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        Log.d(TAG, "MainActivity destroyed");

        // 清理辅助类（会自动移除Player.Listener）
        if (controllerHelper != null) {
            // 移除MainActivity的监听器
            controllerHelper.removePlaybackStateListener(this);
            //controllerHelper.removePlaybackStateListener(playbackStateListener);
            controllerHelper.cleanup();
            controllerHelper = null;
        }

        // 释放MediaController
        if (mediaController != null) {
            MediaController.releaseFuture(controllerFuture);
            mediaController = null;
        }
    }



    @Override
    public void onPlaylistEmpty() {
        Log.d(TAG, "播放列表为空，隐藏bottomsheet");
        if (bottomSheetBehavior != null) {
            bottomSheetBehavior.setState(BottomSheetBehavior.STATE_HIDDEN);
        }
    }

    /**
     * 跳转到艺术家详情页面
     * 使用通用的导航方式，适用于从任何页面跳转
     */
    public void navigateToArtistDetail(String artistName) {
        try {
            // 获取NavHostFragment
            androidx.fragment.app.Fragment navHostFragment = getSupportFragmentManager().findFragmentById(R.id.nav_host_fragment);
            if (navHostFragment != null) {
                bottomSheetBehavior.setState(BottomSheetBehavior.STATE_COLLAPSED);

                // 从NavHostFragment获取NavController
                NavController navController = androidx.navigation.fragment.NavHostFragment.findNavController(navHostFragment);

                // 使用通用的导航方式
                Bundle bundle = new Bundle();
                bundle.putString("artist_name", artistName);

                binding.bottomNavigation.postDelayed(() -> {
                    try {
                        // 方法1：使用NavOptions添加动画效果，直接导航到目的地（推荐方式）
                        NavOptions navOptions = new NavOptions.Builder()
                                .setEnterAnim(R.anim.fade_in)
                                .setExitAnim(R.anim.fade_out)
                                .setPopEnterAnim(R.anim.fade_in_pop)
                                .setPopExitAnim(R.anim.fade_out_pop)
                                .build();
                        navController.navigate(R.id.nav_artist_detail, bundle, navOptions);
                        Log.d(TAG, "成功跳转到艺术家详情页面: " + artistName);
                    } catch (Exception e) {
                        Log.e(TAG, "直接导航失败，尝试备用方案", e);
                        ToastUtils.showToast(this, "跳转失败");

                    }
                }, 500);
            } else {
                Log.e(TAG, "NavHostFragment为null，无法执行导航");
            }
        } catch (Exception e) {
            Log.e(TAG, "跳转到艺术家详情页面失败", e);
        }
    }

    /**
     * 跳转到专辑详情页面
     * 使用通用的导航方式，适用于从任何页面跳转
     */
    public void navigateToAlbumDetail(String albumName, String artistName, long albumId) {
        try {
            // 获取NavHostFragment
            androidx.fragment.app.Fragment navHostFragment = getSupportFragmentManager().findFragmentById(R.id.nav_host_fragment);
            if (navHostFragment != null) {
                bottomSheetBehavior.setState(BottomSheetBehavior.STATE_COLLAPSED);

                // 从NavHostFragment获取NavController
                NavController navController = androidx.navigation.fragment.NavHostFragment.findNavController(navHostFragment);

                // 使用通用的导航方式
                Bundle bundle = new Bundle();
                bundle.putString("album_name", albumName);
                bundle.putString("artist_name", artistName);
                bundle.putLong("album_id", albumId);

                binding.bottomNavigation.postDelayed(() -> {
                    try {
                        // 方法1：使用NavOptions添加动画效果，直接导航到目的地（推荐方式）
                        NavOptions navOptions = new NavOptions.Builder()
                                .setEnterAnim(R.anim.fade_in)
                                .setExitAnim(R.anim.fade_out)
                                .setPopEnterAnim(R.anim.fade_in_pop)
                                .setPopExitAnim(R.anim.fade_out_pop)
                                .build();
                        navController.navigate(R.id.nav_album_detail, bundle, navOptions);
                        Log.d(TAG, "成功跳转到专辑详情页面: " + albumName);
                    } catch (Exception e) {
                        Log.e(TAG, "直接导航失败，尝试备用方案", e);
                        ToastUtils.showToast(this, "跳转失败");

                    }
                }, 500);
            } else {
                Log.e(TAG, "NavHostFragment为null，无法执行导航");
            }
        } catch (Exception e) {
            Log.e(TAG, "跳转到专辑详情页面失败", e);
        }
    }

    /**
     * 跳转到歌曲标签编辑器
     * 使用通用的导航方式，适用于从任何页面跳转
     */
    public void navigateToSongTagEditor(Song song) {
        try {
            // 获取NavHostFragment
            androidx.fragment.app.Fragment navHostFragment = getSupportFragmentManager().findFragmentById(R.id.nav_host_fragment);
            if (navHostFragment != null) {
                bottomSheetBehavior.setState(BottomSheetBehavior.STATE_COLLAPSED);

                // 从NavHostFragment获取NavController
                NavController navController = androidx.navigation.fragment.NavHostFragment.findNavController(navHostFragment);

                // 使用通用的导航方式
                Bundle bundle = new Bundle();
                bundle.putSerializable("song", song);

                binding.bottomNavigation.postDelayed(() -> {
                    try {
                        // 方法1：使用NavOptions添加动画效果，直接导航到目的地（推荐方式）
                        NavOptions navOptions = new NavOptions.Builder()
                                .setEnterAnim(R.anim.fade_in)
                                .setExitAnim(R.anim.fade_out)
                                .setPopEnterAnim(R.anim.fade_in_pop)
                                .setPopExitAnim(R.anim.fade_out_pop)
                                .build();
                        navController.navigate(R.id.nav_song_tag_editor, bundle, navOptions);
                        Log.d(TAG, "成功跳转到歌曲标签编辑器: " + song.getTitle());
                    } catch (Exception e) {
                        Log.e(TAG, "直接导航失败，尝试备用方案", e);
                        ToastUtils.showToast(this, "跳转失败");

                    }
                }, 500);
            } else {
                Log.e(TAG, "NavHostFragment为null，无法执行导航");
            }
        } catch (Exception e) {
            Log.e(TAG, "跳转到歌曲标签编辑器失败", e);
        }
    }

}
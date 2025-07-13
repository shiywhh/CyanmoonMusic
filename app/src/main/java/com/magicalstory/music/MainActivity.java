package com.magicalstory.music;

import android.content.res.Configuration;
import android.os.Bundle;
import android.widget.FrameLayout;

import androidx.appcompat.app.AppCompatActivity;
import androidx.navigation.NavController;
import androidx.navigation.ui.NavigationUI;

import com.google.android.material.bottomsheet.BottomSheetBehavior;
import com.gyf.immersionbar.ImmersionBar;
import com.magicalstory.music.databinding.ActivityMainBinding;

public class MainActivity extends AppCompatActivity {

    private ActivityMainBinding binding;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setupStatusBar();
        // 使用ViewBinding
        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        binding.getRoot().post(this::setupBottomNavigation);
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
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    

    // 提供公共方法供Fragment访问binding
    public ActivityMainBinding getBinding() {
        return binding;
    }
}
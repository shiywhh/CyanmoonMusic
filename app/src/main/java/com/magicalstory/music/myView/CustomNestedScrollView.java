package com.magicalstory.music.myView;

import android.content.Context;
import android.util.AttributeSet;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.HorizontalScrollView;
import android.widget.ScrollView;

import androidx.core.widget.NestedScrollView;
import androidx.recyclerview.widget.RecyclerView;

/**
 * 自定义NestedScrollView，解决RecyclerView滑动冲突
 * 只允许垂直滑动，不影响RecyclerView的横向滑动
 */
public class CustomNestedScrollView extends NestedScrollView {

    private float lastX;
    private float lastY;
    private boolean isHorizontalScroll = false;
    private boolean isVerticalScroll = false;
    private static final float HORIZONTAL_SLOP = 10f; // 水平滑动阈值
    private static final float VERTICAL_SLOP = 10f; // 垂直滑动阈值
    private static final boolean DEBUG = false; // 调试开关

    public CustomNestedScrollView(Context context) {
        super(context);
        init();
    }

    public CustomNestedScrollView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public CustomNestedScrollView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }

    private void init() {
        // 设置滚动条样式
        setScrollBarStyle(SCROLLBARS_INSIDE_OVERLAY);
        setOverScrollMode(OVER_SCROLL_NEVER);
    }

    @Override
    public boolean onInterceptTouchEvent(MotionEvent ev) {
        switch (ev.getAction()) {
            case MotionEvent.ACTION_DOWN:
                lastX = ev.getX();
                lastY = ev.getY();
                isHorizontalScroll = false;
                if (DEBUG) Log.d("CustomNestedScrollView", "ACTION_DOWN: x=" + lastX + ", y=" + lastY);
                break;
                
            case MotionEvent.ACTION_MOVE:
                float deltaX = Math.abs(ev.getX() - lastX);
                float deltaY = Math.abs(ev.getY() - lastY);
                
                // 如果水平滑动距离大于阈值，标记为水平滑动
                if (deltaX > HORIZONTAL_SLOP && deltaX > deltaY) {
                    isHorizontalScroll = true;
                    Log.d("CustomNestedScrollView", "检测到水平滑动: deltaX=" + deltaX + ", deltaY=" + deltaY);
                }
                
                // 如果垂直滑动距离大于阈值，标记为垂直滑动
                if (deltaY > VERTICAL_SLOP && deltaY > deltaX) {
                    isVerticalScroll = true;
                    Log.d("CustomNestedScrollView", "检测到垂直滑动: deltaX=" + deltaX + ", deltaY=" + deltaY);
                }
                
                // 如果是水平滑动，不拦截事件
                if (isHorizontalScroll) {
                    Log.d("CustomNestedScrollView", "水平滑动，不拦截事件");
                    return false;
                }
                
                // 如果是垂直滑动，检查是否需要拦截
                if (isVerticalScroll) {
                    // 检查触摸点是否在水平滑动的视图上
                    View child = findChildUnderRecursive(ev.getX(), ev.getY(), this);
                    if (child != null && canScrollHorizontally(child)) {
                        Log.d("CustomNestedScrollView", "触摸点在水平滑动视图上，不拦截事件: " + child.getClass().getSimpleName());
                        return false;
                    }
                }
                break;
                
            case MotionEvent.ACTION_UP:
            case MotionEvent.ACTION_CANCEL:
                isHorizontalScroll = false;
                isVerticalScroll = false;
                Log.d("CustomNestedScrollView", "ACTION_UP/ACTION_CANCEL");
                break;
        }
        
        boolean result = super.onInterceptTouchEvent(ev);
        Log.d("CustomNestedScrollView", "onInterceptTouchEvent result: " + result);
        return result;
    }

    @Override
    public boolean onTouchEvent(MotionEvent ev) {
        // 如果是水平滑动，不处理触摸事件
        if (isHorizontalScroll) {
            return false;
        }
        
        return super.onTouchEvent(ev);
    }

    /**
     * 查找指定坐标下的子视图
     */
    private View findChildUnder(float x, float y) {
        int childCount = getChildCount();
        for (int i = childCount - 1; i >= 0; i--) {
            View child = getChildAt(i);
            if (child.getVisibility() == View.VISIBLE) {
                if (isPointInView(x, y, child)) {
                    return child;
                }
            }
        }
        return null;
    }

    /**
     * 递归查找指定坐标下的子视图
     */
    private View findChildUnderRecursive(float x, float y, ViewGroup parent) {
        if (parent == null) return null;
        
        int childCount = parent.getChildCount();
        for (int i = childCount - 1; i >= 0; i--) {
            View child = parent.getChildAt(i);
            if (child.getVisibility() == View.VISIBLE) {
                if (isPointInView(x, y, child)) {
                    if (child instanceof ViewGroup) {
                        View result = findChildUnderRecursive(x, y, (ViewGroup) child);
                        if (result != null) {
                            return result;
                        }
                    }
                    return child;
                }
            }
        }
        return null;
    }

    /**
     * 检查点是否在视图范围内
     */
    private boolean isPointInView(float x, float y, View view) {
        if (view == null) return false;
        
        int[] location = new int[2];
        view.getLocationInWindow(location);
        
        int left = location[0];
        int top = location[1];
        int right = left + view.getWidth();
        int bottom = top + view.getHeight();
        
        return x >= left && x <= right && y >= top && y <= bottom;
    }

    /**
     * 检查视图是否可以水平滑动
     */
    private boolean canScrollHorizontally(View view) {
        if (view instanceof RecyclerView) {
            RecyclerView recyclerView = (RecyclerView) view;
            return recyclerView.getLayoutManager() != null && 
                   recyclerView.getLayoutManager().canScrollHorizontally();
        } else if (view instanceof HorizontalScrollView) {
            return true;
        } else if (view instanceof ScrollView) {
            return false;
        }
        return false;
    }

    @Override
    public boolean onNestedPreFling(View target, float velocityX, float velocityY) {
        // 如果是水平滑动，不处理嵌套滑动
        if (Math.abs(velocityX) > Math.abs(velocityY)) {
            return false;
        }
        return super.onNestedPreFling(target, velocityX, velocityY);
    }

    @Override
    public boolean onNestedFling(View target, float velocityX, float velocityY, boolean consumed) {
        // 如果是水平滑动，不处理嵌套滑动
        if (Math.abs(velocityX) > Math.abs(velocityY)) {
            return false;
        }
        return super.onNestedFling(target, velocityX, velocityY, consumed);
    }

    @Override
    public void onNestedPreScroll(View target, int dx, int dy, int[] consumed) {
        // 如果是水平滑动，不处理嵌套滑动
        if (Math.abs(dx) > Math.abs(dy)) {
            return;
        }
        super.onNestedPreScroll(target, dx, dy, consumed);
    }

    @Override
    public void onNestedScroll(View target, int dxConsumed, int dyConsumed, int dxUnconsumed, int dyUnconsumed) {
        // 如果是水平滑动，不处理嵌套滑动
        if (Math.abs(dxConsumed) > Math.abs(dyConsumed) || Math.abs(dxUnconsumed) > Math.abs(dyUnconsumed)) {
            return;
        }
        super.onNestedScroll(target, dxConsumed, dyConsumed, dxUnconsumed, dyUnconsumed);
    }

    @Override
    public boolean onStartNestedScroll(View child, View target, int nestedScrollAxes) {
        // 只处理垂直方向的嵌套滑动
        return (nestedScrollAxes & SCROLL_AXIS_VERTICAL) != 0;
    }
} 
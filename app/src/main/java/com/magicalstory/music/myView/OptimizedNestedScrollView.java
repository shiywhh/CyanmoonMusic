package com.magicalstory.music.myView;

/**
 * @Classname: OptimizedNestedScrollView
 * @Auther: Created by 奇谈君 on 2025/7/18.
 * @Description:
 */
import android.content.Context;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.ViewConfiguration;
import androidx.core.view.ViewCompat;
import androidx.core.widget.NestedScrollView;

public class OptimizedNestedScrollView extends NestedScrollView {

    private float startX;
    private float startY;
    private final int touchSlop;

    public OptimizedNestedScrollView(Context context, AttributeSet attrs) {
        super(context, attrs);
        touchSlop = ViewConfiguration.get(context).getScaledTouchSlop();
    }

    @Override
    public boolean onInterceptTouchEvent(MotionEvent ev) {
        int action = ev.getAction();
        switch (action) {
            case MotionEvent.ACTION_DOWN:
                startX = ev.getX();
                startY = ev.getY();
                // 先禁止父容器拦截
                getParent().requestDisallowInterceptTouchEvent(true);
                break;
            case MotionEvent.ACTION_MOVE:
                float dx = Math.abs(ev.getX() - startX);
                float dy = Math.abs(ev.getY() - startY);
                // 横向滑动距离大于阈值时，放行事件给子View
                if (dx > touchSlop && dx > dy) {
                    getParent().requestDisallowInterceptTouchEvent(true);
                    return false;
                }
                break;
        }
        return super.onInterceptTouchEvent(ev);
    }
}
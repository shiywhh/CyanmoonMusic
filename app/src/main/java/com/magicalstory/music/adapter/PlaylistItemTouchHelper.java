package com.magicalstory.music.adapter;

import android.graphics.Color;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.ItemTouchHelper;
import androidx.recyclerview.widget.RecyclerView;

/**
 * 播放列表拖动排序助手
 */
public class PlaylistItemTouchHelper extends ItemTouchHelper.Callback {

    private final PlaylistAdapter adapter;
    private final OnItemMovedListener listener;
    private final OnItemSwipeListener swipeListener;
    private boolean isDragging = false;
    private int dragStartPosition = -1; // 记录拖动开始的位置
    private int lastFromPosition = -1;
    private int lastToPosition = -1;

    public PlaylistItemTouchHelper(PlaylistAdapter adapter, OnItemMovedListener listener) {
        this.adapter = adapter;
        this.listener = listener;
        this.swipeListener = null;
    }

    public PlaylistItemTouchHelper(PlaylistAdapter adapter, OnItemMovedListener listener, OnItemSwipeListener swipeListener) {
        this.adapter = adapter;
        this.listener = listener;
        this.swipeListener = swipeListener;
    }

    @Override
    public int getMovementFlags(@NonNull RecyclerView recyclerView, @NonNull RecyclerView.ViewHolder viewHolder) {
        // 允许垂直拖动
        int dragFlags = ItemTouchHelper.UP | ItemTouchHelper.DOWN;
        // 允许左右滑动删除
        int swipeFlags = ItemTouchHelper.START | ItemTouchHelper.END;
        return makeMovementFlags(dragFlags, swipeFlags);
    }

    @Override
    public boolean onMove(@NonNull RecyclerView recyclerView, @NonNull RecyclerView.ViewHolder viewHolder, @NonNull RecyclerView.ViewHolder target) {
        int fromPosition = viewHolder.getAdapterPosition();
        int toPosition = target.getAdapterPosition();
        
        // 记录最后的移动位置
        lastFromPosition = fromPosition;
        lastToPosition = toPosition;
        
        // 移动适配器中的项
        adapter.moveItem(fromPosition, toPosition);
        
        // 拖动过程中不通知监听器，等拖动完成后再通知
        return true;
    }

    @Override
    public void onSwiped(@NonNull RecyclerView.ViewHolder viewHolder, int direction) {
        int position = viewHolder.getAdapterPosition();
        if (swipeListener != null) {
            swipeListener.onItemSwiped(position);
        }
    }

    @Override
    public boolean isLongPressDragEnabled() {
        // 禁用长按拖动，使用拖动图标
        return false;
    }

    @Override
    public boolean isItemViewSwipeEnabled() {
        // 启用滑动删除
        return true;
    }

    @Override
    public void onSelectedChanged(RecyclerView.ViewHolder viewHolder, int actionState) {
        super.onSelectedChanged(viewHolder, actionState);
        
        if (actionState == ItemTouchHelper.ACTION_STATE_DRAG) {
            // 开始拖动
            isDragging = true;
            dragStartPosition = viewHolder.getAdapterPosition(); // 记录拖动开始的位置
            lastFromPosition = -1;
            lastToPosition = -1;
            
            // 拖动时改变背景色
            viewHolder.itemView.setBackgroundColor(Color.parseColor("#66FFFFFF"));
            viewHolder.itemView.setAlpha(0.8f);
        } else if (actionState == ItemTouchHelper.ACTION_STATE_IDLE && isDragging) {
            // 拖动结束
            isDragging = false;
            
            // 如果有有效的移动，通知监听器
            if (dragStartPosition >= 0 && lastToPosition >= 0 && dragStartPosition != lastToPosition) {
                if (listener != null) {
                    // 使用拖动开始位置和最终位置，而不是每次移动的位置
                    listener.onItemMoved(dragStartPosition, lastToPosition);
                }
            }
        }
    }

    @Override
    public void clearView(@NonNull RecyclerView recyclerView, @NonNull RecyclerView.ViewHolder viewHolder) {
        super.clearView(recyclerView, viewHolder);
        
        // 恢复原始状态
        viewHolder.itemView.setBackgroundColor(Color.TRANSPARENT);
        viewHolder.itemView.setAlpha(1.0f);
    }

    /**
     * 项目移动监听器
     */
    public interface OnItemMovedListener {
        void onItemMoved(int fromPosition, int toPosition);
    }

    /**
     * 项目滑动删除监听器
     */
    public interface OnItemSwipeListener {
        void onItemSwiped(int position);
    }
} 
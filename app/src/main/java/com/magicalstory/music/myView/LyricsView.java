package com.magicalstory.music.myView;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.LinearSmoothScroller;
import androidx.recyclerview.widget.RecyclerView;

import com.magicalstory.music.R;
import com.magicalstory.music.model.LyricLine;

import java.util.ArrayList;
import java.util.List;

/**
 * 自定义歌词View
 */
public class LyricsView extends RecyclerView {

    private static final int AUTO_SCROLL_DELAY = 2000; // 3秒后自动回滚

    private LyricsAdapter adapter;
    private List<LyricLine> lyrics = new ArrayList<>();
    private int currentPlayingIndex = -1; // 当前播放的歌词索引
    private boolean isUserScrolling = false; // 用户是否正在滚动
    private Handler handler = new Handler(Looper.getMainLooper());
    private Runnable autoScrollRunnable;
    private boolean isFristLoad = true;

    // 监听器接口
    public interface OnLyricClickListener {
        void onLyricClick(int position, LyricLine lyricLine);
    }

    private OnLyricClickListener onLyricClickListener;

    public LyricsView(@NonNull Context context) {
        super(context);
        init();
    }

    public LyricsView(@NonNull Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public LyricsView(@NonNull Context context, @Nullable AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        init();
    }

    private void init() {
        // 设置布局管理器
        LinearLayoutManager layoutManager = new LinearLayoutManager(getContext());
        setLayoutManager(layoutManager);

        // 创建适配器
        adapter = new LyricsAdapter();
        setAdapter(adapter);

        // 设置滚动监听器
        addOnScrollListener(new OnScrollListener() {
            @Override
            public void onScrollStateChanged(@NonNull RecyclerView recyclerView, int newState) {
                super.onScrollStateChanged(recyclerView, newState);

                if (newState == SCROLL_STATE_DRAGGING) {
                    // 用户开始滚动
                    isUserScrolling = true;
                    cancelAutoScroll();
                } else if (newState == SCROLL_STATE_IDLE) {
                    // 滚动结束
                    if (isUserScrolling) {
                        // 如果是用户滚动，启动自动回滚定时器
                        startAutoScrollTimer();
                    }
                }
            }
        });
    }

    /**
     * 设置歌词数据
     */
    public void setLyrics(List<LyricLine> lyrics) {
        this.lyrics.clear();
        isFristLoad = true;
        if (lyrics != null) {
            this.lyrics.addAll(lyrics);
        }
        adapter.notifyDataSetChanged();
        post(new Runnable() {
            @Override
            public void run() {
                scrollToPosition(0);
            }
        });
    }

    /**
     * 更新歌词列表
     */
    public void updateLyrics(List<LyricLine> lyrics) {
        setLyrics(lyrics);
    }

    /**
     * 更新当前播放时间
     */
    public void updateCurrentTime(long currentTime) {
        updateCurrentPosition(currentTime);
    }

    /**
     * 更新当前播放的歌词
     */
    public void updateCurrentPosition(long currentPosition) {
        int newCurrentIndex = findCurrentLyricIndex(currentPosition);
        if (newCurrentIndex != currentPlayingIndex) {
            int oldIndex = currentPlayingIndex;
            currentPlayingIndex = newCurrentIndex;

            // 刷新相关项
            if (oldIndex >= 0 && oldIndex < lyrics.size()) {
                adapter.notifyItemChanged(oldIndex);
            }
            if (currentPlayingIndex >= 0 && currentPlayingIndex < lyrics.size()) {
                adapter.notifyItemChanged(currentPlayingIndex);
            }

            // 如果用户没有在滚动，自动滚动到当前播放的歌词下方两个位置
            if (!isUserScrolling) {
                int targetPosition = currentPlayingIndex + 2;
                // 确保不越界
                if (targetPosition >= lyrics.size()) {
                    targetPosition = lyrics.size() - 1;
                }
                int finalTargetPosition = targetPosition;
                if (isFristLoad) {
                    post(() -> scrollToPosition(finalTargetPosition));
                    isFristLoad = false;
                } else {
                    scrollToPosition(finalTargetPosition);

                }
            }
        }
    }

    /**
     * 根据当前播放位置查找对应的歌词索引
     */
    private int findCurrentLyricIndex(long currentPosition) {
        for (int i = lyrics.size() - 1; i >= 0; i--) {
            if (currentPosition >= lyrics.get(i).getStartTime()) {
                return i;
            }
        }
        return -1;
    }

    /**
     * 使用SmoothScroller滚动到当前播放的歌词
     */
    private void smoothScrollToCurrentLyric() {
        if (currentPlayingIndex >= 0 && currentPlayingIndex < lyrics.size()) {
            LinearLayoutManager layoutManager = (LinearLayoutManager) getLayoutManager();
            if (layoutManager != null) {
                // 创建自定义的SmoothScroller
                LinearSmoothScroller smoothScroller = new LinearSmoothScroller(getContext()) {
                    @Override
                    protected int getVerticalSnapPreference() {
                        return 0; // 0表示滚动到开始位置
                    }

                    @Override
                    protected float calculateSpeedPerPixel(android.util.DisplayMetrics displayMetrics) {
                        // 控制滚动速度，数值越小滚动越慢
                        return 0.07f;
                    }

                    @Override
                    public int calculateDtToFit(int viewStart, int viewEnd, int boxStart, int boxEnd, int snapPreference) {
                        // 计算滚动到中心位置需要的距离
                        int viewHeight = viewEnd - viewStart;
                        int boxHeight = boxEnd - boxStart;
                        int centerOffset = (boxHeight - viewHeight) / 2;
                        return boxStart + centerOffset - viewStart;
                    }
                };

                smoothScroller.setTargetPosition(currentPlayingIndex);
                layoutManager.startSmoothScroll(smoothScroller);
            }
        }
    }

    /**
     * 启动自动回滚定时器
     */
    private void startAutoScrollTimer() {
        cancelAutoScroll();
        autoScrollRunnable = () -> {
            isUserScrolling = false;
            smoothScrollToCurrentLyric();
        };
        handler.postDelayed(autoScrollRunnable, AUTO_SCROLL_DELAY);
    }

    /**
     * 取消自动回滚
     */
    private void cancelAutoScroll() {
        if (autoScrollRunnable != null) {
            handler.removeCallbacks(autoScrollRunnable);
            autoScrollRunnable = null;
        }
    }

    /**
     * 设置歌词点击监听器
     */
    public void setOnLyricClickListener(OnLyricClickListener listener) {
        this.onLyricClickListener = listener;
    }

    /**
     * 手动设置当前播放的歌词索引（用于点击歌词时）
     */
    public void setCurrentPlayingIndex(int index) {
        if (index >= 0 && index < lyrics.size()) {
            int oldIndex = currentPlayingIndex;
            currentPlayingIndex = index;

            // 刷新相关项
            if (oldIndex >= 0 && oldIndex < lyrics.size()) {
                adapter.notifyItemChanged(oldIndex);
            }
            if (currentPlayingIndex >= 0 && currentPlayingIndex < lyrics.size()) {
                adapter.notifyItemChanged(currentPlayingIndex);
            }

            // 平滑滚动到选中的歌词
            smoothScrollToCurrentLyric();
        }
    }

    /**
     * 歌词适配器
     */
    private class LyricsAdapter extends RecyclerView.Adapter<LyricsAdapter.LyricViewHolder> {

        @NonNull
        @Override
        public LyricViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_lyric_line, parent, false);
            return new LyricViewHolder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull LyricViewHolder holder, int position) {
            LyricLine lyricLine = lyrics.get(position);
            holder.bind(lyricLine, position == currentPlayingIndex);
        }

        @Override
        public int getItemCount() {
            return lyrics.size();
        }

        class LyricViewHolder extends RecyclerView.ViewHolder {
            private TextView txtLyricContent;

            public LyricViewHolder(@NonNull View itemView) {
                super(itemView);
                txtLyricContent = itemView.findViewById(R.id.txt_lyric_content);

                // 设置点击监听器
                itemView.setOnClickListener(new OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        int position = getAdapterPosition();
                        if (position != RecyclerView.NO_POSITION && onLyricClickListener != null) {
                            // 设置当前播放索引为点击的位置
                            setCurrentPlayingIndex(position);
                            // 调用点击监听器
                            onLyricClickListener.onLyricClick(position, lyrics.get(position));
                        }
                    }
                });
            }

            public void bind(LyricLine lyricLine, boolean isCurrentPlaying) {
                txtLyricContent.setText(lyricLine.getContent());

                // 设置颜色和透明度
                if (isCurrentPlaying) {
                    // 当前播放的歌词使用白色
                    txtLyricContent.setTextColor(getContext().getColor(R.color.white));
                    txtLyricContent.setAlpha(1.0f);
                } else {
                    // 其他歌词使用半透明白色
                    txtLyricContent.setTextColor(getContext().getColor(R.color.white));
                    txtLyricContent.setAlpha(0.5f);
                }
            }
        }
    }

    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        cancelAutoScroll();
    }
} 
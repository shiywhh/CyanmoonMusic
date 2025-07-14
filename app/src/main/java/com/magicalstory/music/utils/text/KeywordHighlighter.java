package com.magicalstory.music.utils.text;

/**
 * @Classname: KeywordHighlighter
 * @Auther: Created by 奇谈君 on 2024/8/4.
 * @Description:关键词高亮
 */
import android.content.Context;
import android.text.SpannableString;
import android.text.Spanned;
import android.text.method.LinkMovementMethod;
import android.text.style.ClickableSpan;
import android.text.style.ForegroundColorSpan;
import android.view.View;
import android.widget.TextView;

public class KeywordHighlighter {

    public static void highlightKeywords(Context context, TextView textView, String fullText, String[] keywords, int highlightColor, OnKeywordClickListener listener) {
        SpannableString spannableString = new SpannableString(fullText);

        for (String keyword : keywords) {
            int startIndex = fullText.indexOf(keyword);
            while (startIndex >= 0) {
                int endIndex = startIndex + keyword.length();
                spannableString.setSpan(new ForegroundColorSpan(highlightColor), startIndex, endIndex, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);

                ClickableSpan clickableSpan = new ClickableSpan() {
                    @Override
                    public void onClick(View widget) {
                        listener.onKeywordClick(keyword);
                    }
                };

                spannableString.setSpan(clickableSpan, startIndex, endIndex, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
                startIndex = fullText.indexOf(keyword, endIndex);
            }
        }

        textView.setText(spannableString);
        textView.setMovementMethod(LinkMovementMethod.getInstance());
    }

    public interface OnKeywordClickListener {
        void onKeywordClick(String keyword);
    }
}

package com.example.attendance.ui;

import android.content.Context;
import android.graphics.Color;
import android.view.Gravity;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.core.content.ContextCompat;

import com.example.attendance.R;

import java.util.ArrayList;
import java.util.List;

/** 把一组可单选的“药丸”按钮塞进一个水平容器，常用于状态筛选。 */
public class FilterBar {

    public interface OnSelected {
        void onSelected(String value);
    }

    private final List<TextView> pills = new ArrayList<>();
    private final List<String> values = new ArrayList<>();
    private int selectedIndex = 0;

    public FilterBar(Context ctx, LinearLayout container, String[] labels, String[] vals, OnSelected cb) {
        for (int i = 0; i < labels.length; i++) {
            TextView pill = new TextView(ctx);
            pill.setText(labels[i]);
            pill.setTextSize(13);
            pill.setGravity(Gravity.CENTER);
            int padH = dp(ctx, 16), padV = dp(ctx, 8);
            pill.setPadding(padH, padV, padH, padV);
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT);
            lp.setMarginEnd(dp(ctx, 8));
            pill.setLayoutParams(lp);
            final int idx = i;
            pill.setOnClickListener(v -> {
                select(idx);
                cb.onSelected(values.get(idx));
            });
            pills.add(pill);
            values.add(vals[i]);
            container.addView(pill);
        }
        applyStyles(ctx);
    }

    private void select(int idx) {
        selectedIndex = idx;
        applyStyles(pills.isEmpty() ? null : pills.get(0).getContext());
    }

    private void applyStyles(Context ctx) {
        for (int i = 0; i < pills.size(); i++) {
            TextView p = pills.get(i);
            if (i == selectedIndex) {
                p.setBackgroundResource(R.drawable.bg_pill_selected);
                p.setTextColor(Color.WHITE);
            } else {
                p.setBackgroundResource(R.drawable.bg_pill_unselected);
                p.setTextColor(ContextCompat.getColor(p.getContext(), R.color.text_secondary));
            }
        }
    }

    public String currentValue() {
        return values.isEmpty() ? null : values.get(selectedIndex);
    }

    private static int dp(Context ctx, int v) {
        return Math.round(ctx.getResources().getDisplayMetrics().density * v);
    }
}

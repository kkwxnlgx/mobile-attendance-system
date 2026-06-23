package com.example.attendance.ui;

import android.view.View;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.example.attendance.R;

/** 提供统一的顶部工具栏装配。 */
public class BaseActivity extends AppCompatActivity {

    /** 装配 view_toolbar：设置标题并让返回键 finish。 */
    protected void setupToolbar(String title) {
        applyStatusBarInset();
        View back = findViewById(R.id.toolbar_back);
        TextView tv = findViewById(R.id.toolbar_title);
        if (tv != null) tv.setText(title);
        if (back != null) back.setOnClickListener(v -> finish());
    }

    /** targetSdk 35+ 强制 edge-to-edge，内容会铺到状态栏下；给 toolbar 顶部补状态栏高度的 padding。 */
    private void applyStatusBarInset() {
        View toolbar = findViewById(R.id.toolbar_root);
        if (toolbar == null) return;
        ViewCompat.setOnApplyWindowInsetsListener(toolbar, (v, insets) -> {
            int top = insets.getInsets(WindowInsetsCompat.Type.systemBars()).top;
            v.setPadding(v.getPaddingLeft(), top, v.getPaddingRight(), v.getPaddingBottom());
            return insets;
        });
    }

    /** 装配带右上角操作按钮的工具栏。 */
    protected void setupToolbar(String title, String actionText, View.OnClickListener action) {
        setupToolbar(title);
        TextView act = findViewById(R.id.toolbar_action);
        if (act != null) {
            act.setText(actionText);
            act.setVisibility(View.VISIBLE);
            act.setOnClickListener(action);
        }
    }
}

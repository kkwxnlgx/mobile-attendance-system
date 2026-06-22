package com.example.attendance.admin;

import android.os.Bundle;

import com.example.attendance.R;
import com.example.attendance.teacher.StatsFragment;
import com.example.attendance.ui.BaseActivity;

/** 管理员复用教师端统计页面。 */
public class StatsHostActivity extends BaseActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_stats_host);
        setupToolbar("考勤统计");
        if (savedInstanceState == null) {
            getSupportFragmentManager().beginTransaction()
                    .replace(R.id.container, new StatsFragment())
                    .commit();
        }
    }
}

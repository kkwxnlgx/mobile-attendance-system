package com.example.attendance.teacher;

import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;

import com.example.attendance.R;
import com.google.android.material.bottomnavigation.BottomNavigationView;

public class TeacherMainActivity extends AppCompatActivity {

    private final Fragment homeFragment = new TeacherHomeFragment();
    private final Fragment approvalFragment = new ApprovalFragment();
    private final Fragment statsFragment = new StatsFragment();
    private final Fragment manageFragment = new TeacherManageFragment();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_teacher_main);

        BottomNavigationView nav = findViewById(R.id.bottom_nav);
        nav.setOnItemSelectedListener(item -> {
            int id = item.getItemId();
            if (id == R.id.nav_home) return switchTo(homeFragment);
            if (id == R.id.nav_approval) return switchTo(approvalFragment);
            if (id == R.id.nav_stats) return switchTo(statsFragment);
            if (id == R.id.nav_manage) return switchTo(manageFragment);
            return false;
        });
        nav.setSelectedItemId(R.id.nav_home);
    }

    private boolean switchTo(Fragment f) {
        getSupportFragmentManager().beginTransaction()
                .replace(R.id.container, f)
                .commit();
        return true;
    }
}

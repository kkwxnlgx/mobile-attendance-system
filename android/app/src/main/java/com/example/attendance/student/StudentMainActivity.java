package com.example.attendance.student;

import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;

import com.example.attendance.R;
import com.google.android.material.bottomnavigation.BottomNavigationView;

public class StudentMainActivity extends AppCompatActivity {

    private final Fragment homeFragment = new StudentHomeFragment();
    private final Fragment recordsFragment = new StudentRecordsFragment();
    private final Fragment profileFragment = new StudentProfileFragment();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_student_main);

        BottomNavigationView nav = findViewById(R.id.bottom_nav);
        nav.setOnItemSelectedListener(item -> {
            int id = item.getItemId();
            if (id == R.id.nav_home) return switchTo(homeFragment);
            if (id == R.id.nav_records) return switchTo(recordsFragment);
            if (id == R.id.nav_profile) return switchTo(profileFragment);
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

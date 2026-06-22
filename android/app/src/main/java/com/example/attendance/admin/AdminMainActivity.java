package com.example.attendance.admin;

import android.content.Intent;
import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;

import com.example.attendance.LoginActivity;
import com.example.attendance.R;
import com.example.attendance.SessionManager;
import com.example.attendance.teacher.ClassManageActivity;
import com.example.attendance.teacher.RecordsQueryActivity;

public class AdminMainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_admin_main);

        findViewById(R.id.m_users).setOnClickListener(v ->
                startActivity(new Intent(this, UserManageActivity.class)));
        findViewById(R.id.m_class).setOnClickListener(v ->
                startActivity(new Intent(this, ClassManageActivity.class)));
        findViewById(R.id.m_stats).setOnClickListener(v ->
                startActivity(new Intent(this, StatsHostActivity.class)));
        findViewById(R.id.m_records).setOnClickListener(v ->
                startActivity(new Intent(this, RecordsQueryActivity.class)));
        findViewById(R.id.btn_logout).setOnClickListener(v -> logout());
    }

    private void logout() {
        SessionManager.logout();
        Intent i = new Intent(this, LoginActivity.class);
        i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(i);
        finish();
    }
}

package com.example.attendance.teacher;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import com.example.attendance.R;
import com.example.attendance.model.Api;
import com.example.attendance.net.ApiCallback;
import com.example.attendance.net.ApiClient;
import com.example.attendance.ui.BaseActivity;
import com.example.attendance.ui.Ui;

import java.util.HashMap;
import java.util.Map;

public class LaunchAttendanceActivity extends BaseActivity {

    private EditText etDuration, etLate, etRadius;
    private Button btnLaunch;
    private long scheduleId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_launch_attendance);
        setupToolbar("发起考勤");

        scheduleId = getIntent().getLongExtra("schedule_id", -1);
        String course = getIntent().getStringExtra("course_name");
        String clazz = getIntent().getStringExtra("class_name");
        String location = getIntent().getStringExtra("location");
        ((TextView) findViewById(R.id.tv_course)).setText(course == null ? "课程" : course);
        ((TextView) findViewById(R.id.tv_class)).setText(
                (clazz == null ? "" : clazz) + (location == null ? "" : "  ·  " + location));

        etDuration = findViewById(R.id.et_duration);
        etLate = findViewById(R.id.et_late);
        etRadius = findViewById(R.id.et_radius);
        btnLaunch = findViewById(R.id.btn_launch);
        btnLaunch.setOnClickListener(v -> launch());
    }

    private int parseInt(EditText et, int def) {
        String s = et.getText().toString().trim();
        if (TextUtils.isEmpty(s)) return def;
        try {
            return Integer.parseInt(s);
        } catch (NumberFormatException e) {
            return def;
        }
    }

    private void launch() {
        if (scheduleId < 0) {
            Ui.toast(this, "课程信息无效");
            return;
        }
        btnLaunch.setEnabled(false);
        Map<String, Object> body = new HashMap<>();
        body.put("schedule_id", scheduleId);
        body.put("duration_min", parseInt(etDuration, 30));
        body.put("late_offset_min", parseInt(etLate, 10));
        body.put("radius_m", parseInt(etRadius, 300));
        ApiClient.api().launchTask(body).enqueue(new ApiCallback<Api.Task>() {
            @Override
            public void onOk(Api.Task t) {
                btnLaunch.setEnabled(true);
                Ui.toast(LaunchAttendanceActivity.this, "考勤已发起，签到码 " + t.code);
                Intent i = new Intent(LaunchAttendanceActivity.this, MonitorActivity.class);
                i.putExtra("task_id", t.id);
                startActivity(i);
                finish();
            }

            @Override
            public void onErr(String message) {
                btnLaunch.setEnabled(true);
                Ui.toast(LaunchAttendanceActivity.this, message);
            }
        });
    }
}

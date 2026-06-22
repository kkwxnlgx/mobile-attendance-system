package com.example.attendance.teacher;

import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TextView;

import androidx.appcompat.app.AlertDialog;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.example.attendance.R;
import com.example.attendance.adapter.ScheduleAdapter;
import com.example.attendance.model.Api;
import com.example.attendance.net.ApiCallback;
import com.example.attendance.net.ApiClient;
import com.example.attendance.ui.BaseActivity;
import com.example.attendance.ui.DateTimePicker;
import com.example.attendance.ui.Ui;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ScheduleManageActivity extends BaseActivity {

    // 演示考勤坐标（与后端种子数据一致），新建课程表默认带上，便于直接定位签到
    private static final double DEMO_LAT = 23.1291;
    private static final double DEMO_LNG = 113.2644;

    private SwipeRefreshLayout swipe;
    private TextView tvEmpty;
    private ScheduleAdapter adapter;
    private final List<Api.Course> courses = new ArrayList<>();
    private final List<Api.Clazz> classes = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_list);
        setupToolbar("课程表设置", "+ 新增", v -> showForm());

        swipe = findViewById(R.id.swipe);
        tvEmpty = findViewById(R.id.tv_empty);
        RecyclerView recycler = findViewById(R.id.recycler);
        adapter = new ScheduleAdapter();
        recycler.setLayoutManager(new LinearLayoutManager(this));
        recycler.setAdapter(adapter);

        swipe.setOnRefreshListener(this::load);
        loadRefData();
        load();
    }

    private void loadRefData() {
        ApiClient.api().listCourses().enqueue(new ApiCallback<List<Api.Course>>() {
            @Override public void onOk(List<Api.Course> data) {
                courses.clear();
                if (data != null) courses.addAll(data);
            }
        });
        ApiClient.api().listClasses().enqueue(new ApiCallback<List<Api.Clazz>>() {
            @Override public void onOk(List<Api.Clazz> data) {
                classes.clear();
                if (data != null) classes.addAll(data);
            }
        });
    }

    private void load() {
        ApiClient.api().listSchedules(null).enqueue(new ApiCallback<List<Api.Schedule>>() {
            @Override
            public void onOk(List<Api.Schedule> data) {
                swipe.setRefreshing(false);
                adapter.setData(data);
                tvEmpty.setVisibility(data == null || data.isEmpty() ? View.VISIBLE : View.GONE);
            }

            @Override
            public void onErr(String message) {
                swipe.setRefreshing(false);
                Ui.toast(ScheduleManageActivity.this, message);
            }
        });
    }

    private final String[] startTimeHolder = new String[1];
    private final String[] endTimeHolder = new String[1];

    private void showForm() {
        if (courses.isEmpty() || classes.isEmpty()) {
            Ui.toast(this, "请先确保已有课程与班级");
            return;
        }
        startTimeHolder[0] = null;
        endTimeHolder[0] = null;

        View form = LayoutInflater.from(this).inflate(R.layout.dialog_schedule, null);
        Spinner spCourse = form.findViewById(R.id.sp_course);
        Spinner spClass = form.findViewById(R.id.sp_class);
        Spinner spWeekday = form.findViewById(R.id.sp_weekday);
        TextView tvStart = form.findViewById(R.id.tv_start);
        TextView tvEnd = form.findViewById(R.id.tv_end);
        EditText etLocation = form.findViewById(R.id.et_location);

        List<String> courseNames = new ArrayList<>();
        for (Api.Course c : courses) courseNames.add(c.name);
        spCourse.setAdapter(new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item, courseNames));

        List<String> classNames = new ArrayList<>();
        for (Api.Clazz c : classes) classNames.add(c.name);
        spClass.setAdapter(new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item, classNames));

        spWeekday.setAdapter(new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item,
                new String[]{"周一", "周二", "周三", "周四", "周五", "周六", "周日"}));

        tvStart.setOnClickListener(v -> DateTimePicker.pickTime(this, (hms, disp) -> {
            startTimeHolder[0] = hms;
            tvStart.setText(disp);
        }));
        tvEnd.setOnClickListener(v -> DateTimePicker.pickTime(this, (hms, disp) -> {
            endTimeHolder[0] = hms;
            tvEnd.setText(disp);
        }));

        new AlertDialog.Builder(this)
                .setTitle("新增课程表")
                .setView(form)
                .setNegativeButton("取消", null)
                .setPositiveButton("保存", (d, w) -> {
                    String location = etLocation.getText().toString().trim();
                    if (startTimeHolder[0] == null || endTimeHolder[0] == null) {
                        Ui.toast(this, "请选择上课时间");
                        return;
                    }
                    if (TextUtils.isEmpty(location)) {
                        Ui.toast(this, "请填写上课地点");
                        return;
                    }
                    Map<String, Object> body = new HashMap<>();
                    body.put("course_id", courses.get(spCourse.getSelectedItemPosition()).id);
                    body.put("class_id", classes.get(spClass.getSelectedItemPosition()).id);
                    body.put("weekday", spWeekday.getSelectedItemPosition() + 1);
                    body.put("start_time", startTimeHolder[0]);
                    body.put("end_time", endTimeHolder[0]);
                    body.put("location", location);
                    body.put("latitude", DEMO_LAT);
                    body.put("longitude", DEMO_LNG);
                    body.put("weeks", "1-16");
                    create(body);
                })
                .show();
    }

    private void create(Map<String, Object> body) {
        ApiClient.api().createSchedule(body).enqueue(new ApiCallback<Api.Schedule>() {
            @Override public void onOk(Api.Schedule data) { Ui.toast(ScheduleManageActivity.this, "已新增"); load(); }
            @Override public void onErr(String message) { Ui.toast(ScheduleManageActivity.this, message); }
        });
    }
}

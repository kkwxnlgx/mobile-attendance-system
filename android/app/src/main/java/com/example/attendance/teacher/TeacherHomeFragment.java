package com.example.attendance.teacher;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.example.attendance.R;
import com.example.attendance.SessionManager;
import com.example.attendance.model.Api;
import com.example.attendance.net.ApiCallback;
import com.example.attendance.net.ApiClient;
import com.example.attendance.ui.Ui;

import java.util.List;

public class TeacherHomeFragment extends Fragment {

    private SwipeRefreshLayout swipe;
    private LinearLayout activeContainer, todayContainer;
    private TextView labelActive, tvTodayEmpty;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_teacher_home, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View v, @Nullable Bundle savedInstanceState) {
        swipe = v.findViewById(R.id.swipe);
        activeContainer = v.findViewById(R.id.active_container);
        todayContainer = v.findViewById(R.id.today_container);
        labelActive = v.findViewById(R.id.label_active);
        tvTodayEmpty = v.findViewById(R.id.tv_today_empty);
        ((TextView) v.findViewById(R.id.tv_greeting)).setText(SessionManager.name() + "，你好");
        swipe.setOnRefreshListener(this::loadAll);
    }

    @Override
    public void onResume() {
        super.onResume();
        loadAll();
    }

    private void loadAll() {
        loadActive();
        loadToday();
    }

    private void loadActive() {
        ApiClient.api().mineActiveTasks().enqueue(new ApiCallback<List<Api.Task>>() {
            @Override
            public void onOk(List<Api.Task> data) {
                activeContainer.removeAllViews();
                if (data == null || data.isEmpty()) {
                    labelActive.setVisibility(View.GONE);
                    return;
                }
                labelActive.setVisibility(View.VISIBLE);
                for (Api.Task t : data) {
                    View row = LayoutInflater.from(getContext())
                            .inflate(R.layout.row_active_task, activeContainer, false);
                    ((TextView) row.findViewById(R.id.tv_course)).setText(t.course_name + " · " + t.class_name);
                    ((TextView) row.findViewById(R.id.tv_info)).setText(
                            "签到码 " + t.code + "  截止 " + Ui.shortDateTime(t.end_time));
                    row.setOnClickListener(x -> openMonitor(t.id));
                    activeContainer.addView(row);
                }
            }

            @Override
            public void onErr(String message) {
                labelActive.setVisibility(View.GONE);
            }
        });
    }

    private void loadToday() {
        ApiClient.api().today().enqueue(new ApiCallback<List<Api.Schedule>>() {
            @Override
            public void onOk(List<Api.Schedule> data) {
                swipe.setRefreshing(false);
                todayContainer.removeAllViews();
                if (data == null || data.isEmpty()) {
                    tvTodayEmpty.setVisibility(View.VISIBLE);
                    return;
                }
                tvTodayEmpty.setVisibility(View.GONE);
                for (Api.Schedule s : data) {
                    View row = LayoutInflater.from(getContext())
                            .inflate(R.layout.row_teach_course, todayContainer, false);
                    ((TextView) row.findViewById(R.id.tv_course)).setText(s.course_name);
                    String info = (s.class_name == null ? "" : s.class_name) + "  "
                            + Ui.hm(s.start_time) + "-" + Ui.hm(s.end_time) + "  "
                            + (s.location == null ? "" : s.location);
                    ((TextView) row.findViewById(R.id.tv_info)).setText(info);
                    row.findViewById(R.id.btn_launch).setOnClickListener(x -> launch(s));
                    todayContainer.addView(row);
                }
            }

            @Override
            public void onErr(String message) {
                swipe.setRefreshing(false);
                Ui.toast(getContext(), message);
            }
        });
    }

    private void launch(Api.Schedule s) {
        Intent i = new Intent(getContext(), LaunchAttendanceActivity.class);
        i.putExtra("schedule_id", s.id);
        i.putExtra("course_name", s.course_name);
        i.putExtra("class_name", s.class_name);
        i.putExtra("location", s.location);
        startActivity(i);
    }

    private void openMonitor(long taskId) {
        Intent i = new Intent(getContext(), MonitorActivity.class);
        i.putExtra("task_id", taskId);
        startActivity(i);
    }
}

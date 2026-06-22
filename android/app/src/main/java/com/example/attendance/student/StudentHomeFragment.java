package com.example.attendance.student;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
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

import android.content.Intent;

public class StudentHomeFragment extends Fragment {

    private SwipeRefreshLayout swipe;
    private View cardActive;
    private TextView tvActiveCourse, tvActiveTime, tvTodayEmpty;
    private LinearLayout todayContainer;
    private Api.Task activeTask;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_student_home, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View v, @Nullable Bundle savedInstanceState) {
        swipe = v.findViewById(R.id.swipe);
        cardActive = v.findViewById(R.id.card_active);
        tvActiveCourse = v.findViewById(R.id.tv_active_course);
        tvActiveTime = v.findViewById(R.id.tv_active_time);
        tvTodayEmpty = v.findViewById(R.id.tv_today_empty);
        todayContainer = v.findViewById(R.id.today_container);

        ((TextView) v.findViewById(R.id.tv_greeting)).setText(SessionManager.name() + "，你好");

        v.findViewById(R.id.btn_checkin).setOnClickListener(x -> openCheckin());
        v.findViewById(R.id.quick_leave).setOnClickListener(x ->
                startActivity(new Intent(getContext(), LeaveRequestActivity.class)));
        v.findViewById(R.id.quick_notice).setOnClickListener(x ->
                startActivity(new Intent(getContext(), NotificationsActivity.class)));
        v.findViewById(R.id.quick_records).setOnClickListener(x -> {
            if (getActivity() instanceof StudentMainActivity) {
                ((com.google.android.material.bottomnavigation.BottomNavigationView)
                        getActivity().findViewById(R.id.bottom_nav)).setSelectedItemId(R.id.nav_records);
            }
        });

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
        ApiClient.api().activeTasks().enqueue(new ApiCallback<List<Api.Task>>() {
            @Override
            public void onOk(List<Api.Task> data) {
                if (data != null && !data.isEmpty()) {
                    activeTask = data.get(0);
                    cardActive.setVisibility(View.VISIBLE);
                    tvActiveCourse.setText(activeTask.course_name + " 正在签到");
                    tvActiveTime.setText("签到截止 " + Ui.shortDateTime(activeTask.end_time));
                } else {
                    activeTask = null;
                    cardActive.setVisibility(View.GONE);
                }
            }

            @Override
            public void onErr(String message) {
                cardActive.setVisibility(View.GONE);
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
                            .inflate(R.layout.row_today_course, todayContainer, false);
                    ((TextView) row.findViewById(R.id.tv_course)).setText(s.course_name);
                    String info = Ui.weekdayLabel(s.weekday) + "  " + Ui.hm(s.start_time)
                            + "-" + Ui.hm(s.end_time) + "  " + (s.location == null ? "" : s.location);
                    ((TextView) row.findViewById(R.id.tv_info)).setText(info);
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

    private void openCheckin() {
        Intent i = new Intent(getContext(), CheckinActivity.class);
        if (activeTask != null) {
            i.putExtra("course_name", activeTask.course_name);
            i.putExtra("code_hint", activeTask.code != null ? "" : "");
        }
        startActivity(i);
    }
}

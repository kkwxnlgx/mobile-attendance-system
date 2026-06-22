package com.example.attendance.teacher;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.example.attendance.R;
import com.example.attendance.model.Api;
import com.example.attendance.net.ApiCallback;
import com.example.attendance.net.ApiClient;
import com.example.attendance.ui.Ui;
import com.google.android.material.progressindicator.LinearProgressIndicator;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class StatsFragment extends Fragment {

    private Spinner spClass;
    private TextView tvRate, cNormal, cLate, cAbsent, cLeave, cLocErr, tvEmpty;
    private LinearProgressIndicator progress;
    private LinearLayout studentContainer;
    private final List<Api.Clazz> classes = new ArrayList<>();

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_stats, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View v, @Nullable Bundle savedInstanceState) {
        spClass = v.findViewById(R.id.sp_class);
        tvRate = v.findViewById(R.id.tv_rate);
        progress = v.findViewById(R.id.progress);
        cNormal = v.findViewById(R.id.c_normal);
        cLate = v.findViewById(R.id.c_late);
        cAbsent = v.findViewById(R.id.c_absent);
        cLeave = v.findViewById(R.id.c_leave);
        cLocErr = v.findViewById(R.id.c_locerr);
        tvEmpty = v.findViewById(R.id.tv_empty);
        studentContainer = v.findViewById(R.id.student_container);

        spClass.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                loadStats();
            }

            @Override public void onNothingSelected(AdapterView<?> parent) {}
        });

        loadClasses();
    }

    private void loadClasses() {
        ApiClient.api().listClasses().enqueue(new ApiCallback<List<Api.Clazz>>() {
            @Override
            public void onOk(List<Api.Clazz> data) {
                classes.clear();
                if (data != null) classes.addAll(data);
                List<String> names = new ArrayList<>();
                for (Api.Clazz c : classes) names.add(c.name);
                ArrayAdapter<String> a = new ArrayAdapter<>(requireContext(),
                        android.R.layout.simple_spinner_dropdown_item, names);
                spClass.setAdapter(a);
                if (!classes.isEmpty()) loadStats();
            }

            @Override
            public void onErr(String message) {
                Ui.toast(getContext(), message);
            }
        });
    }

    private void loadStats() {
        int pos = spClass.getSelectedItemPosition();
        if (pos < 0 || pos >= classes.size()) return;
        long classId = classes.get(pos).id;
        ApiClient.api().stats(classId, null, null, null).enqueue(new ApiCallback<Api.StatSummary>() {
            @Override
            public void onOk(Api.StatSummary s) {
                if (s == null) return;
                tvRate.setText(String.format(Locale.US, "%.1f%%", s.attend_rate));
                progress.setProgress((int) Math.round(s.attend_rate));
                cNormal.setText("正常\n" + s.normal);
                cLate.setText("迟到\n" + s.late);
                cAbsent.setText("缺勤\n" + s.absent);
                cLeave.setText("请假\n" + s.leave);
                cLocErr.setText("异常\n" + s.location_error);
                renderStudents(s.students);
                tvEmpty.setVisibility(s.total == 0 ? View.VISIBLE : View.GONE);
            }

            @Override
            public void onErr(String message) {
                Ui.toast(getContext(), message);
            }
        });
    }

    private void renderStudents(List<Api.StatRow> rows) {
        studentContainer.removeAllViews();
        if (rows == null) return;
        for (Api.StatRow r : rows) {
            View row = LayoutInflater.from(getContext())
                    .inflate(R.layout.row_stat, studentContainer, false);
            ((TextView) row.findViewById(R.id.tv_name)).setText(r.name + "  " + r.username);
            ((TextView) row.findViewById(R.id.tv_detail)).setText(String.format(Locale.US,
                    "正常 %d · 迟到 %d · 缺勤 %d · 请假 %d · 异常 %d",
                    r.normal, r.late, r.absent, r.leave, r.location_error));
            ((TextView) row.findViewById(R.id.tv_rate)).setText(
                    String.format(Locale.US, "%.0f%%", r.attend_rate));
            studentContainer.addView(row);
        }
    }
}

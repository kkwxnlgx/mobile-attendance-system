package com.example.attendance.student;

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
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.example.attendance.R;
import com.example.attendance.adapter.RecordAdapter;
import com.example.attendance.model.Api;
import com.example.attendance.net.ApiCallback;
import com.example.attendance.net.ApiClient;
import com.example.attendance.ui.FilterBar;
import com.example.attendance.ui.Ui;

import java.util.List;

public class StudentRecordsFragment extends Fragment {

    private SwipeRefreshLayout swipe;
    private RecyclerView recycler;
    private TextView tvEmpty;
    private RecordAdapter adapter;
    private String statusFilter = null;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_student_records, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View v, @Nullable Bundle savedInstanceState) {
        swipe = v.findViewById(R.id.swipe);
        recycler = v.findViewById(R.id.recycler);
        tvEmpty = v.findViewById(R.id.tv_empty);

        adapter = new RecordAdapter(false);
        recycler.setLayoutManager(new LinearLayoutManager(getContext()));
        recycler.setAdapter(adapter);

        // 点击异常记录 -> 补卡申请
        adapter.setOnItemClick(r -> {
            if ("absent".equals(r.status) || "location_error".equals(r.status) || "late".equals(r.status)) {
                Intent i = new Intent(getContext(), FixRequestActivity.class);
                i.putExtra("record_id", r.id);
                i.putExtra("course_name", r.course_name);
                i.putExtra("status", r.status);
                startActivity(i);
            }
        });

        LinearLayout filter = v.findViewById(R.id.filter_container);
        new FilterBar(getContext(), filter,
                new String[]{"全部", "正常", "迟到", "缺勤", "请假", "位置异常"},
                new String[]{null, "normal", "late", "absent", "leave", "location_error"},
                value -> {
                    statusFilter = value;
                    load();
                });

        swipe.setOnRefreshListener(this::load);
    }

    @Override
    public void onResume() {
        super.onResume();
        load();
    }

    private void load() {
        ApiClient.api().myRecords(statusFilter, null).enqueue(new ApiCallback<List<Api.Record>>() {
            @Override
            public void onOk(List<Api.Record> data) {
                swipe.setRefreshing(false);
                adapter.setData(data);
                tvEmpty.setVisibility(data == null || data.isEmpty() ? View.VISIBLE : View.GONE);
            }

            @Override
            public void onErr(String message) {
                swipe.setRefreshing(false);
                Ui.toast(getContext(), message);
            }
        });
    }
}

package com.example.attendance.teacher;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.example.attendance.R;
import com.example.attendance.adapter.FixAdapter;
import com.example.attendance.adapter.LeaveAdapter;
import com.example.attendance.model.Api;
import com.example.attendance.net.ApiCallback;
import com.example.attendance.net.ApiClient;
import com.example.attendance.ui.ApproveDialog;
import com.example.attendance.ui.Ui;
import com.google.android.material.tabs.TabLayout;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ApprovalFragment extends Fragment {

    private SwipeRefreshLayout swipe;
    private RecyclerView recycler;
    private TextView tvEmpty;
    private LeaveAdapter leaveAdapter;
    private FixAdapter fixAdapter;
    private int currentTab = 0;   // 0 请假 1 补卡

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_approval, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View v, @Nullable Bundle savedInstanceState) {
        swipe = v.findViewById(R.id.swipe);
        recycler = v.findViewById(R.id.recycler);
        tvEmpty = v.findViewById(R.id.tv_empty);
        recycler.setLayoutManager(new LinearLayoutManager(getContext()));

        leaveAdapter = new LeaveAdapter(true);
        leaveAdapter.setOnApprove((leave, approve) ->
                ApproveDialog.show(getContext(), approve, comment -> doApproveLeave(leave.id, approve, comment)));

        fixAdapter = new FixAdapter(true);
        fixAdapter.setOnApprove((fix, approve) ->
                ApproveDialog.show(getContext(), approve, comment -> doApproveFix(fix.id, approve, comment)));

        TabLayout tabs = v.findViewById(R.id.tabs);
        tabs.addOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
            @Override
            public void onTabSelected(TabLayout.Tab tab) {
                currentTab = tab.getPosition();
                load();
            }

            @Override public void onTabUnselected(TabLayout.Tab tab) {}
            @Override public void onTabReselected(TabLayout.Tab tab) {}
        });

        swipe.setOnRefreshListener(this::load);
    }

    @Override
    public void onResume() {
        super.onResume();
        load();
    }

    private void load() {
        if (currentTab == 0) loadLeaves();
        else loadFixes();
    }

    private void loadLeaves() {
        recycler.setAdapter(leaveAdapter);
        ApiClient.api().pendingLeaves().enqueue(new ApiCallback<List<Api.Leave>>() {
            @Override
            public void onOk(List<Api.Leave> data) {
                swipe.setRefreshing(false);
                leaveAdapter.setData(data);
                tvEmpty.setVisibility(data == null || data.isEmpty() ? View.VISIBLE : View.GONE);
            }

            @Override
            public void onErr(String message) {
                swipe.setRefreshing(false);
                Ui.toast(getContext(), message);
            }
        });
    }

    private void loadFixes() {
        recycler.setAdapter(fixAdapter);
        ApiClient.api().pendingFixes().enqueue(new ApiCallback<List<Api.Fix>>() {
            @Override
            public void onOk(List<Api.Fix> data) {
                swipe.setRefreshing(false);
                fixAdapter.setData(data);
                tvEmpty.setVisibility(data == null || data.isEmpty() ? View.VISIBLE : View.GONE);
            }

            @Override
            public void onErr(String message) {
                swipe.setRefreshing(false);
                Ui.toast(getContext(), message);
            }
        });
    }

    private void doApproveLeave(long id, boolean approve, String comment) {
        Map<String, Object> body = new HashMap<>();
        body.put("approve", approve);
        body.put("comment", comment);
        ApiClient.api().approveLeave(id, body).enqueue(new ApiCallback<Api.Leave>() {
            @Override
            public void onOk(Api.Leave data) {
                Ui.toast(getContext(), approve ? "已通过" : "已驳回");
                loadLeaves();
            }

            @Override
            public void onErr(String message) {
                Ui.toast(getContext(), message);
            }
        });
    }

    private void doApproveFix(long id, boolean approve, String comment) {
        Map<String, Object> body = new HashMap<>();
        body.put("approve", approve);
        body.put("comment", comment);
        ApiClient.api().approveFix(id, body).enqueue(new ApiCallback<Api.Fix>() {
            @Override
            public void onOk(Api.Fix data) {
                Ui.toast(getContext(), approve ? "已通过" : "已驳回");
                loadFixes();
            }

            @Override
            public void onErr(String message) {
                Ui.toast(getContext(), message);
            }
        });
    }
}

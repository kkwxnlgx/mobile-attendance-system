package com.example.attendance.student;

import android.content.Intent;
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

import com.example.attendance.LoginActivity;
import com.example.attendance.R;
import com.example.attendance.SessionManager;
import com.example.attendance.adapter.LeaveAdapter;
import com.example.attendance.model.Api;
import com.example.attendance.net.ApiCallback;
import com.example.attendance.net.ApiClient;
import com.example.attendance.ui.Ui;

import java.util.List;

public class StudentProfileFragment extends Fragment {

    private RecyclerView recyclerLeaves;
    private TextView tvLeaveEmpty;
    private LeaveAdapter adapter;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_student_profile, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View v, @Nullable Bundle savedInstanceState) {
        TextView name = v.findViewById(R.id.tv_name);
        TextView meta = v.findViewById(R.id.tv_meta);
        TextView avatar = v.findViewById(R.id.tv_avatar);
        name.setText(SessionManager.name());
        meta.setText("学号 " + SessionManager.username());
        if (!SessionManager.name().isEmpty()) {
            avatar.setText(SessionManager.name().substring(0, 1));
        }

        recyclerLeaves = v.findViewById(R.id.recycler_leaves);
        tvLeaveEmpty = v.findViewById(R.id.tv_leave_empty);
        adapter = new LeaveAdapter(false);
        recyclerLeaves.setLayoutManager(new LinearLayoutManager(getContext()));
        recyclerLeaves.setAdapter(adapter);

        v.findViewById(R.id.m_password).setOnClickListener(x ->
                startActivity(new Intent(getContext(), com.example.attendance.ChangePasswordActivity.class)));
        v.findViewById(R.id.btn_logout).setOnClickListener(x -> logout());
    }

    @Override
    public void onResume() {
        super.onResume();
        loadLeaves();
    }

    private void loadLeaves() {
        ApiClient.api().myLeaves().enqueue(new ApiCallback<List<Api.Leave>>() {
            @Override
            public void onOk(List<Api.Leave> data) {
                adapter.setData(data);
                tvLeaveEmpty.setVisibility(data == null || data.isEmpty() ? View.VISIBLE : View.GONE);
            }

            @Override
            public void onErr(String message) {
                Ui.toast(getContext(), message);
            }
        });
    }

    private void logout() {
        SessionManager.logout();
        Intent i = new Intent(getContext(), LoginActivity.class);
        i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(i);
        if (getActivity() != null) getActivity().finish();
    }
}

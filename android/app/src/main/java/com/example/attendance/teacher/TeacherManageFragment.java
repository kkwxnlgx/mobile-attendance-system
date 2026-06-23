package com.example.attendance.teacher;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.example.attendance.LoginActivity;
import com.example.attendance.R;
import com.example.attendance.SessionManager;

public class TeacherManageFragment extends Fragment {

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_teacher_manage, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View v, @Nullable Bundle savedInstanceState) {
        ((TextView) v.findViewById(R.id.tv_name)).setText(SessionManager.name() + " 老师");
        v.findViewById(R.id.m_class).setOnClickListener(x ->
                startActivity(new Intent(getContext(), ClassManageActivity.class)));
        v.findViewById(R.id.m_schedule).setOnClickListener(x ->
                startActivity(new Intent(getContext(), ScheduleManageActivity.class)));
        v.findViewById(R.id.m_roster).setOnClickListener(x ->
                startActivity(new Intent(getContext(), RosterActivity.class)));
        v.findViewById(R.id.m_records).setOnClickListener(x ->
                startActivity(new Intent(getContext(), RecordsQueryActivity.class)));
        v.findViewById(R.id.m_password).setOnClickListener(x ->
                startActivity(new Intent(getContext(), com.example.attendance.ChangePasswordActivity.class)));
        v.findViewById(R.id.btn_logout).setOnClickListener(x -> logout());
    }

    private void logout() {
        SessionManager.logout();
        Intent i = new Intent(getContext(), LoginActivity.class);
        i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(i);
        if (getActivity() != null) getActivity().finish();
    }
}

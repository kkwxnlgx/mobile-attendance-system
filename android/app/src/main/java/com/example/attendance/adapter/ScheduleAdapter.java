package com.example.attendance.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.attendance.R;
import com.example.attendance.model.Api;
import com.example.attendance.ui.Ui;

import java.util.ArrayList;
import java.util.List;

public class ScheduleAdapter extends RecyclerView.Adapter<ScheduleAdapter.VH> {

    private final List<Api.Schedule> items = new ArrayList<>();

    public void setData(List<Api.Schedule> data) {
        items.clear();
        if (data != null) items.addAll(data);
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.row_today_course, parent, false);
        return new VH(v);
    }

    @Override
    public void onBindViewHolder(@NonNull VH h, int position) {
        Api.Schedule s = items.get(position);
        h.course.setText(s.course_name);
        String info = (s.class_name == null ? "" : s.class_name) + "  "
                + Ui.weekdayLabel(s.weekday) + " " + Ui.hm(s.start_time) + "-" + Ui.hm(s.end_time)
                + "  " + (s.location == null ? "" : s.location);
        h.info.setText(info);
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    static class VH extends RecyclerView.ViewHolder {
        TextView course, info;

        VH(@NonNull View v) {
            super(v);
            course = v.findViewById(R.id.tv_course);
            info = v.findViewById(R.id.tv_info);
        }
    }
}

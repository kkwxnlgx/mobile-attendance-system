package com.example.attendance.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.attendance.R;
import com.example.attendance.model.Api;

import java.util.ArrayList;
import java.util.List;

/** 课程列表适配器：复用 row_today_course，显示课程名 + 授课教师/学期。 */
public class CourseAdapter extends RecyclerView.Adapter<CourseAdapter.VH> {

    public interface OnItemClick { void onClick(Api.Course course); }

    private final List<Api.Course> items = new ArrayList<>();
    private OnItemClick onItemClick;

    public void setOnItemClick(OnItemClick l) {
        this.onItemClick = l;
    }

    public void setData(List<Api.Course> data) {
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
        Api.Course c = items.get(position);
        h.course.setText(c.name);
        StringBuilder info = new StringBuilder();
        if (c.teacher_name != null && !c.teacher_name.isEmpty()) info.append(c.teacher_name);
        if (c.semester != null && !c.semester.isEmpty()) {
            if (info.length() > 0) info.append("  ·  ");
            info.append(c.semester);
        }
        h.info.setText(info.toString());
        h.itemView.setOnClickListener(v -> { if (onItemClick != null) onItemClick.onClick(c); });
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

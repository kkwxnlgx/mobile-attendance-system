package com.example.attendance.adapter;

import android.graphics.PorterDuff;
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

/** 考勤记录列表适配器（学生个人记录 / 教师查询通用）。 */
public class RecordAdapter extends RecyclerView.Adapter<RecordAdapter.VH> {

    public interface OnItemClick {
        void onClick(Api.Record record);
    }

    private final List<Api.Record> items = new ArrayList<>();
    private final boolean showStudent;   // 教师查询时显示学生姓名
    private OnItemClick onItemClick;

    public RecordAdapter(boolean showStudent) {
        this.showStudent = showStudent;
    }

    public void setOnItemClick(OnItemClick l) {
        this.onItemClick = l;
    }

    public void setData(List<Api.Record> data) {
        items.clear();
        if (data != null) items.addAll(data);
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.row_record, parent, false);
        return new VH(v);
    }

    @Override
    public void onBindViewHolder(@NonNull VH h, int position) {
        Api.Record r = items.get(position);
        String title = r.course_name == null ? "课程" : r.course_name;
        if (showStudent && r.student_name != null) {
            title = r.student_name + " · " + title;
        }
        h.title.setText(title);

        StringBuilder sub = new StringBuilder();
        sub.append(Ui.shortDateTime(r.task_time));
        if (r.checkin_time != null) sub.append("  签到 ").append(Ui.hm(r.checkin_time.length() >= 16 ? r.checkin_time.substring(11) : r.checkin_time));
        if (r.distance_m != null && r.distance_m > 0) sub.append("  ").append(Math.round(r.distance_m)).append("m");
        if (r.remark != null && !r.remark.isEmpty()) sub.append("\n").append(r.remark);
        h.subtitle.setText(sub.toString());

        h.status.setText(Ui.statusLabel(r.status));
        h.status.getBackground().mutate().setColorFilter(
                Ui.statusColor(h.status.getContext(), r.status), PorterDuff.Mode.SRC_IN);

        h.itemView.setOnClickListener(v -> {
            if (onItemClick != null) onItemClick.onClick(r);
        });
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    static class VH extends RecyclerView.ViewHolder {
        TextView title, subtitle, status;

        VH(@NonNull View v) {
            super(v);
            title = v.findViewById(R.id.tv_title);
            subtitle = v.findViewById(R.id.tv_subtitle);
            status = v.findViewById(R.id.tv_status);
        }
    }
}

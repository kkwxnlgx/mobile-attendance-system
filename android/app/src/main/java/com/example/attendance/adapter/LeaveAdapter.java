package com.example.attendance.adapter;

import android.graphics.PorterDuff;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.example.attendance.R;
import com.example.attendance.model.Api;
import com.example.attendance.ui.Ui;

import java.util.ArrayList;
import java.util.List;

/** 请假列表适配器。学生端只展示；教师端 showActions=true 显示同意/驳回。 */
public class LeaveAdapter extends RecyclerView.Adapter<LeaveAdapter.VH> {

    public interface OnApprove {
        void onApprove(Api.Leave leave, boolean approve);
    }

    private final List<Api.Leave> items = new ArrayList<>();
    private final boolean showActions;
    private OnApprove onApprove;

    public LeaveAdapter(boolean showActions) {
        this.showActions = showActions;
    }

    public void setOnApprove(OnApprove l) {
        this.onApprove = l;
    }

    public void setData(List<Api.Leave> data) {
        items.clear();
        if (data != null) items.addAll(data);
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.row_leave, parent, false);
        return new VH(v);
    }

    @Override
    public void onBindViewHolder(@NonNull VH h, int position) {
        Api.Leave lv = items.get(position);
        String title = Ui.leaveTypeLabel(lv.type);
        if (lv.course_name != null) title += " · " + lv.course_name;
        if (showActions && lv.student_name != null) title = lv.student_name + "  " + title;
        h.title.setText(title);
        h.time.setText(Ui.shortDateTime(lv.start_time) + "  至  " + Ui.shortDateTime(lv.end_time));
        h.reason.setText("事由：" + (lv.reason == null ? "无" : lv.reason));

        h.status.setText(Ui.approvalLabel(lv.status));
        h.status.getBackground().mutate().setColorFilter(
                approvalColor(h, lv.status), PorterDuff.Mode.SRC_IN);

        if (showActions && "pending".equals(lv.status)) {
            h.actionBar.setVisibility(View.VISIBLE);
            h.btnApprove.setOnClickListener(v -> {
                if (onApprove != null) onApprove.onApprove(lv, true);
            });
            h.btnReject.setOnClickListener(v -> {
                if (onApprove != null) onApprove.onApprove(lv, false);
            });
        } else {
            h.actionBar.setVisibility(View.GONE);
        }
    }

    private int approvalColor(VH h, String status) {
        int res = R.color.status_pending;
        if ("approved".equals(status)) res = R.color.status_normal;
        else if ("rejected".equals(status)) res = R.color.status_absent;
        return ContextCompat.getColor(h.itemView.getContext(), res);
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    static class VH extends RecyclerView.ViewHolder {
        TextView title, time, reason, status, btnApprove, btnReject;
        View actionBar;

        VH(@NonNull View v) {
            super(v);
            title = v.findViewById(R.id.tv_title);
            time = v.findViewById(R.id.tv_time);
            reason = v.findViewById(R.id.tv_reason);
            status = v.findViewById(R.id.tv_status);
            actionBar = v.findViewById(R.id.action_bar);
            btnApprove = v.findViewById(R.id.btn_approve);
            btnReject = v.findViewById(R.id.btn_reject);
        }
    }
}

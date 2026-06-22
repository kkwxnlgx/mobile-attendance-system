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

/** 补卡列表适配器。学生端只展示；教师端 showActions=true 显示同意/驳回。 */
public class FixAdapter extends RecyclerView.Adapter<FixAdapter.VH> {

    public interface OnApprove {
        void onApprove(Api.Fix fix, boolean approve);
    }

    private final List<Api.Fix> items = new ArrayList<>();
    private final boolean showActions;
    private OnApprove onApprove;

    public FixAdapter(boolean showActions) {
        this.showActions = showActions;
    }

    public void setOnApprove(OnApprove l) {
        this.onApprove = l;
    }

    public void setData(List<Api.Fix> data) {
        items.clear();
        if (data != null) items.addAll(data);
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.row_fix, parent, false);
        return new VH(v);
    }

    @Override
    public void onBindViewHolder(@NonNull VH h, int position) {
        Api.Fix f = items.get(position);
        String title = (showActions && f.student_name != null ? f.student_name + " · " : "")
                + (f.course_name == null ? "课程" : f.course_name);
        h.title.setText(title);
        h.reason.setText(Ui.fixReasonLabel(f.reason_type) + "：" + (f.description == null ? "" : f.description));

        h.status.setText(Ui.approvalLabel(f.status));
        int res = R.color.status_pending;
        if ("approved".equals(f.status)) res = R.color.status_normal;
        else if ("rejected".equals(f.status)) res = R.color.status_absent;
        h.status.getBackground().mutate().setColorFilter(
                ContextCompat.getColor(h.itemView.getContext(), res), PorterDuff.Mode.SRC_IN);

        if (showActions && "pending".equals(f.status)) {
            h.actionBar.setVisibility(View.VISIBLE);
            h.btnApprove.setOnClickListener(v -> {
                if (onApprove != null) onApprove.onApprove(f, true);
            });
            h.btnReject.setOnClickListener(v -> {
                if (onApprove != null) onApprove.onApprove(f, false);
            });
        } else {
            h.actionBar.setVisibility(View.GONE);
        }
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    static class VH extends RecyclerView.ViewHolder {
        TextView title, reason, status, btnApprove, btnReject;
        View actionBar;

        VH(@NonNull View v) {
            super(v);
            title = v.findViewById(R.id.tv_title);
            reason = v.findViewById(R.id.tv_reason);
            status = v.findViewById(R.id.tv_status);
            actionBar = v.findViewById(R.id.action_bar);
            btnApprove = v.findViewById(R.id.btn_approve);
            btnReject = v.findViewById(R.id.btn_reject);
        }
    }
}

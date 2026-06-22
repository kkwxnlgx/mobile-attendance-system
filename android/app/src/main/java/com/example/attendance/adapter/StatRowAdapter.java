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
import java.util.Locale;

public class StatRowAdapter extends RecyclerView.Adapter<StatRowAdapter.VH> {

    private final List<Api.StatRow> items = new ArrayList<>();

    public void setData(List<Api.StatRow> data) {
        items.clear();
        if (data != null) items.addAll(data);
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.row_stat, parent, false);
        return new VH(v);
    }

    @Override
    public void onBindViewHolder(@NonNull VH h, int position) {
        Api.StatRow r = items.get(position);
        h.name.setText(r.name + "  " + r.username);
        h.detail.setText(String.format(Locale.US,
                "正常 %d · 迟到 %d · 缺勤 %d · 请假 %d · 异常 %d",
                r.normal, r.late, r.absent, r.leave, r.location_error));
        h.rate.setText(String.format(Locale.US, "%.0f%%", r.attend_rate));
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    static class VH extends RecyclerView.ViewHolder {
        TextView name, detail, rate;

        VH(@NonNull View v) {
            super(v);
            name = v.findViewById(R.id.tv_name);
            detail = v.findViewById(R.id.tv_detail);
            rate = v.findViewById(R.id.tv_rate);
        }
    }
}

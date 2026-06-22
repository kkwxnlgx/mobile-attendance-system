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

public class NotificationAdapter extends RecyclerView.Adapter<NotificationAdapter.VH> {

    public interface OnItemClick {
        void onClick(Api.Notification n);
    }

    private final List<Api.Notification> items = new ArrayList<>();
    private OnItemClick onItemClick;

    public void setOnItemClick(OnItemClick l) {
        this.onItemClick = l;
    }

    public void setData(List<Api.Notification> data) {
        items.clear();
        if (data != null) items.addAll(data);
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.row_notification, parent, false);
        return new VH(v);
    }

    @Override
    public void onBindViewHolder(@NonNull VH h, int position) {
        Api.Notification n = items.get(position);
        h.title.setText(n.title);
        h.content.setText(n.content);
        h.time.setText(Ui.shortDateTime(n.created_at));
        h.dot.setVisibility(n.is_read == 0 ? View.VISIBLE : View.INVISIBLE);
        h.itemView.setOnClickListener(v -> {
            if (onItemClick != null) onItemClick.onClick(n);
        });
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    static class VH extends RecyclerView.ViewHolder {
        TextView title, content, time;
        View dot;

        VH(@NonNull View v) {
            super(v);
            title = v.findViewById(R.id.tv_title);
            content = v.findViewById(R.id.tv_content);
            time = v.findViewById(R.id.tv_time);
            dot = v.findViewById(R.id.dot);
        }
    }
}

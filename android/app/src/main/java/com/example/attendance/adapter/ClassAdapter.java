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

import java.util.ArrayList;
import java.util.List;

public class ClassAdapter extends RecyclerView.Adapter<ClassAdapter.VH> {

    public interface OnItemClick {
        void onClick(Api.Clazz clazz);
    }

    private final List<Api.Clazz> items = new ArrayList<>();
    private OnItemClick onItemClick;

    public void setOnItemClick(OnItemClick l) {
        this.onItemClick = l;
    }

    public void setData(List<Api.Clazz> data) {
        items.clear();
        if (data != null) items.addAll(data);
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.row_class, parent, false);
        return new VH(v);
    }

    @Override
    public void onBindViewHolder(@NonNull VH h, int position) {
        Api.Clazz c = items.get(position);
        h.name.setText(c.name);
        StringBuilder meta = new StringBuilder();
        if (c.major != null) meta.append(c.major).append(" · ");
        if (c.grade != null) meta.append(c.grade).append(" · ");
        meta.append(c.student_count).append(" 人");
        h.meta.setText(meta.toString());

        boolean active = c.status == 1;
        h.status.setText(active ? "正常" : "已停用");
        int color = active ? R.color.status_normal : R.color.status_pending;
        h.status.getBackground().mutate().setColorFilter(
                ContextCompat.getColor(h.itemView.getContext(), color), PorterDuff.Mode.SRC_IN);

        h.itemView.setOnClickListener(v -> {
            if (onItemClick != null) onItemClick.onClick(c);
        });
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    static class VH extends RecyclerView.ViewHolder {
        TextView name, meta, status;

        VH(@NonNull View v) {
            super(v);
            name = v.findViewById(R.id.tv_name);
            meta = v.findViewById(R.id.tv_meta);
            status = v.findViewById(R.id.tv_status);
        }
    }
}

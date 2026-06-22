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

/** 用户列表适配器。名单管理 showStatus=false；管理员用户管理 showStatus=true 显示启用/禁用。 */
public class UserAdapter extends RecyclerView.Adapter<UserAdapter.VH> {

    public interface OnItemClick {
        void onClick(Api.User user);
    }

    private final List<Api.User> items = new ArrayList<>();
    private final boolean showStatus;
    private OnItemClick onItemClick;

    public UserAdapter(boolean showStatus) {
        this.showStatus = showStatus;
    }

    public void setOnItemClick(OnItemClick l) {
        this.onItemClick = l;
    }

    public void setData(List<Api.User> data) {
        items.clear();
        if (data != null) items.addAll(data);
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.row_user, parent, false);
        return new VH(v);
    }

    @Override
    public void onBindViewHolder(@NonNull VH h, int position) {
        Api.User u = items.get(position);
        h.name.setText(u.name);
        if (!u.name.isEmpty()) h.avatar.setText(u.name.substring(0, 1));

        String meta = u.username;
        if (showStatus) meta += "  ·  " + roleLabel(u.role);
        if (u.phone != null && !u.phone.isEmpty()) meta += "  ·  " + u.phone;
        h.meta.setText(meta);

        if (showStatus) {
            h.status.setVisibility(View.VISIBLE);
            boolean enabled = u.status == 1;
            h.status.setText(enabled ? "启用" : "已禁用");
            int color = enabled ? R.color.status_normal : R.color.status_absent;
            h.status.getBackground().mutate().setColorFilter(
                    ContextCompat.getColor(h.itemView.getContext(), color), PorterDuff.Mode.SRC_IN);
        } else {
            h.status.setVisibility(View.GONE);
        }

        h.itemView.setOnClickListener(v -> {
            if (onItemClick != null) onItemClick.onClick(u);
        });
    }

    private String roleLabel(String role) {
        if ("teacher".equals(role)) return "教师";
        if ("admin".equals(role)) return "管理员";
        return "学生";
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    static class VH extends RecyclerView.ViewHolder {
        TextView avatar, name, meta, status;

        VH(@NonNull View v) {
            super(v);
            avatar = v.findViewById(R.id.tv_avatar);
            name = v.findViewById(R.id.tv_name);
            meta = v.findViewById(R.id.tv_meta);
            status = v.findViewById(R.id.tv_status);
        }
    }
}

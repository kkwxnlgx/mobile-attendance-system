package com.example.attendance.admin;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.appcompat.app.AlertDialog;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.example.attendance.R;
import com.example.attendance.adapter.UserAdapter;
import com.example.attendance.model.Api;
import com.example.attendance.net.ApiCallback;
import com.example.attendance.net.ApiClient;
import com.example.attendance.ui.BaseActivity;
import com.example.attendance.ui.FilterBar;
import com.example.attendance.ui.Ui;

import java.util.List;

public class UserManageActivity extends BaseActivity {

    private SwipeRefreshLayout swipe;
    private TextView tvEmpty;
    private UserAdapter adapter;
    private String roleFilter = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_list);
        setupToolbar("用户管理");

        FrameLayout headerSlot = findViewById(R.id.header_slot);
        View header = LayoutInflater.from(this).inflate(R.layout.view_filter_only, headerSlot, false);
        headerSlot.addView(header);
        LinearLayout filter = header.findViewById(R.id.filter_container);

        swipe = findViewById(R.id.swipe);
        tvEmpty = findViewById(R.id.tv_empty);
        RecyclerView recycler = findViewById(R.id.recycler);
        adapter = new UserAdapter(true);
        recycler.setLayoutManager(new LinearLayoutManager(this));
        recycler.setAdapter(adapter);
        adapter.setOnItemClick(this::confirmToggle);

        new FilterBar(this, filter,
                new String[]{"全部", "学生", "教师", "管理员"},
                new String[]{null, "student", "teacher", "admin"},
                value -> {
                    roleFilter = value;
                    load();
                });

        swipe.setOnRefreshListener(this::load);
        load();
    }

    private void load() {
        ApiClient.api().adminUsers(roleFilter, "").enqueue(new ApiCallback<List<Api.User>>() {
            @Override
            public void onOk(List<Api.User> data) {
                swipe.setRefreshing(false);
                adapter.setData(data);
                tvEmpty.setVisibility(data == null || data.isEmpty() ? View.VISIBLE : View.GONE);
            }

            @Override
            public void onErr(String message) {
                swipe.setRefreshing(false);
                Ui.toast(UserManageActivity.this, message);
            }
        });
    }

    private void confirmToggle(Api.User u) {
        boolean enabled = u.status == 1;
        new AlertDialog.Builder(this)
                .setTitle(u.name + "（" + u.username + "）")
                .setMessage(enabled ? "确认禁用该账号？禁用后将无法登录。" : "确认启用该账号？")
                .setNegativeButton("取消", null)
                .setPositiveButton(enabled ? "禁用" : "启用", (d, w) ->
                        ApiClient.api().toggleUser(u.id).enqueue(new ApiCallback<Api.User>() {
                            @Override public void onOk(Api.User data) { Ui.toast(UserManageActivity.this, "已更新"); load(); }
                            @Override public void onErr(String message) { Ui.toast(UserManageActivity.this, message); }
                        }))
                .show();
    }
}

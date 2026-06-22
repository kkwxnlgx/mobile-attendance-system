package com.example.attendance.student;

import android.os.Bundle;
import android.view.View;
import android.widget.TextView;

import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.example.attendance.R;
import com.example.attendance.adapter.NotificationAdapter;
import com.example.attendance.model.Api;
import com.example.attendance.net.ApiCallback;
import com.example.attendance.net.ApiClient;
import com.example.attendance.ui.BaseActivity;
import com.example.attendance.ui.Ui;

import java.util.List;

public class NotificationsActivity extends BaseActivity {

    private SwipeRefreshLayout swipe;
    private TextView tvEmpty;
    private NotificationAdapter adapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_list);
        setupToolbar("通知消息");

        swipe = findViewById(R.id.swipe);
        tvEmpty = findViewById(R.id.tv_empty);
        RecyclerView recycler = findViewById(R.id.recycler);
        adapter = new NotificationAdapter();
        recycler.setLayoutManager(new LinearLayoutManager(this));
        recycler.setAdapter(adapter);

        adapter.setOnItemClick(n -> {
            if (n.is_read == 0) {
                ApiClient.api().readNotification(n.id).enqueue(new ApiCallback<Api.Msg>() {
                    @Override public void onOk(Api.Msg data) { load(); }
                });
            }
        });

        swipe.setOnRefreshListener(this::load);
        load();
    }

    private void load() {
        ApiClient.api().notifications().enqueue(new ApiCallback<List<Api.Notification>>() {
            @Override
            public void onOk(List<Api.Notification> data) {
                swipe.setRefreshing(false);
                adapter.setData(data);
                tvEmpty.setVisibility(data == null || data.isEmpty() ? View.VISIBLE : View.GONE);
            }

            @Override
            public void onErr(String message) {
                swipe.setRefreshing(false);
                Ui.toast(NotificationsActivity.this, message);
            }
        });
    }
}

package com.example.attendance.teacher;

import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.Spinner;
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
import com.example.attendance.ui.Ui;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class RosterActivity extends BaseActivity {

    private SwipeRefreshLayout swipe;
    private TextView tvEmpty;
    private Spinner spClass;
    private UserAdapter adapter;
    private final List<Api.Clazz> classes = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_list);
        setupToolbar("学生名单管理", "+ 添加", v -> showAddDialog());

        FrameLayout headerSlot = findViewById(R.id.header_slot);
        View header = LayoutInflater.from(this).inflate(R.layout.view_class_spinner, headerSlot, false);
        headerSlot.addView(header);
        spClass = header.findViewById(R.id.sp_class);

        swipe = findViewById(R.id.swipe);
        tvEmpty = findViewById(R.id.tv_empty);
        RecyclerView recycler = findViewById(R.id.recycler);
        adapter = new UserAdapter(false);
        recycler.setLayoutManager(new LinearLayoutManager(this));
        recycler.setAdapter(adapter);
        adapter.setOnItemClick(this::confirmRemove);

        spClass.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                load();
            }

            @Override public void onNothingSelected(AdapterView<?> parent) {}
        });

        swipe.setOnRefreshListener(this::load);
        loadClasses();
    }

    private long currentClassId() {
        int pos = spClass.getSelectedItemPosition();
        if (pos < 0 || pos >= classes.size()) return -1;
        return classes.get(pos).id;
    }

    private void loadClasses() {
        ApiClient.api().listClasses().enqueue(new ApiCallback<List<Api.Clazz>>() {
            @Override
            public void onOk(List<Api.Clazz> data) {
                classes.clear();
                if (data != null) classes.addAll(data);
                List<String> names = new ArrayList<>();
                for (Api.Clazz c : classes) names.add(c.name);
                spClass.setAdapter(new ArrayAdapter<>(RosterActivity.this,
                        android.R.layout.simple_spinner_dropdown_item, names));
                if (!classes.isEmpty()) load();
            }

            @Override
            public void onErr(String message) {
                Ui.toast(RosterActivity.this, message);
            }
        });
    }

    private void load() {
        long cid = currentClassId();
        if (cid < 0) {
            swipe.setRefreshing(false);
            return;
        }
        ApiClient.api().roster(cid, "").enqueue(new ApiCallback<List<Api.User>>() {
            @Override
            public void onOk(List<Api.User> data) {
                swipe.setRefreshing(false);
                adapter.setData(data);
                tvEmpty.setVisibility(data == null || data.isEmpty() ? View.VISIBLE : View.GONE);
            }

            @Override
            public void onErr(String message) {
                swipe.setRefreshing(false);
                Ui.toast(RosterActivity.this, message);
            }
        });
    }

    private void showAddDialog() {
        long cid = currentClassId();
        if (cid < 0) {
            Ui.toast(this, "请先选择班级");
            return;
        }
        EditText input = new EditText(this);
        input.setHint("学生学号");
        int pad = Math.round(getResources().getDisplayMetrics().density * 16);
        input.setPadding(pad, pad, pad, pad);
        new AlertDialog.Builder(this)
                .setTitle("添加学生到名单")
                .setView(input)
                .setNegativeButton("取消", null)
                .setPositiveButton("添加", (d, w) -> {
                    String username = input.getText().toString().trim();
                    if (TextUtils.isEmpty(username)) {
                        Ui.toast(this, "请输入学号");
                        return;
                    }
                    Map<String, Object> body = new HashMap<>();
                    body.put("username", username);
                    ApiClient.api().addStudent(cid, body).enqueue(new ApiCallback<Api.User>() {
                        @Override public void onOk(Api.User data) { Ui.toast(RosterActivity.this, "已添加"); load(); }
                        @Override public void onErr(String message) { Ui.toast(RosterActivity.this, message); }
                    });
                })
                .show();
    }

    private void confirmRemove(Api.User u) {
        long cid = currentClassId();
        new AlertDialog.Builder(this)
                .setTitle("移出名单")
                .setMessage("确认将 " + u.name + " 移出本班名单？")
                .setNegativeButton("取消", null)
                .setPositiveButton("移出", (d, w) ->
                        ApiClient.api().removeStudent(cid, u.id).enqueue(new ApiCallback<Api.Msg>() {
                            @Override public void onOk(Api.Msg data) { Ui.toast(RosterActivity.this, "已移出"); load(); }
                            @Override public void onErr(String message) { Ui.toast(RosterActivity.this, message); }
                        }))
                .show();
    }
}

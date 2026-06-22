package com.example.attendance.teacher;

import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;

import androidx.appcompat.app.AlertDialog;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.example.attendance.R;
import com.example.attendance.adapter.ClassAdapter;
import com.example.attendance.model.Api;
import com.example.attendance.net.ApiCallback;
import com.example.attendance.net.ApiClient;
import com.example.attendance.ui.BaseActivity;
import com.example.attendance.ui.Ui;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ClassManageActivity extends BaseActivity {

    private SwipeRefreshLayout swipe;
    private TextView tvEmpty;
    private ClassAdapter adapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_list);
        setupToolbar("班级管理", "+ 新增", v -> showForm(null));

        swipe = findViewById(R.id.swipe);
        tvEmpty = findViewById(R.id.tv_empty);
        RecyclerView recycler = findViewById(R.id.recycler);
        adapter = new ClassAdapter();
        recycler.setLayoutManager(new LinearLayoutManager(this));
        recycler.setAdapter(adapter);
        adapter.setOnItemClick(this::showActions);

        swipe.setOnRefreshListener(this::load);
        load();
    }

    private void load() {
        ApiClient.api().listClasses().enqueue(new ApiCallback<List<Api.Clazz>>() {
            @Override
            public void onOk(List<Api.Clazz> data) {
                swipe.setRefreshing(false);
                adapter.setData(data);
                tvEmpty.setVisibility(data == null || data.isEmpty() ? View.VISIBLE : View.GONE);
            }

            @Override
            public void onErr(String message) {
                swipe.setRefreshing(false);
                Ui.toast(ClassManageActivity.this, message);
            }
        });
    }

    private void showActions(Api.Clazz c) {
        new AlertDialog.Builder(this)
                .setTitle(c.name)
                .setItems(new String[]{"编辑", c.status == 1 ? "停用" : "启用"}, (d, which) -> {
                    if (which == 0) showForm(c);
                    else toggle(c);
                })
                .show();
    }

    private void toggle(Api.Clazz c) {
        ApiClient.api().toggleClass(c.id).enqueue(new ApiCallback<Api.Clazz>() {
            @Override public void onOk(Api.Clazz data) { load(); }
            @Override public void onErr(String message) { Ui.toast(ClassManageActivity.this, message); }
        });
    }

    private void showForm(Api.Clazz existing) {
        View form = LayoutInflater.from(this).inflate(R.layout.dialog_class, null);
        EditText etName = form.findViewById(R.id.et_name);
        EditText etGrade = form.findViewById(R.id.et_grade);
        EditText etMajor = form.findViewById(R.id.et_major);
        EditText etRemark = form.findViewById(R.id.et_remark);
        if (existing != null) {
            etName.setText(existing.name);
            etGrade.setText(existing.grade);
            etMajor.setText(existing.major);
            etRemark.setText(existing.remark);
        }
        new AlertDialog.Builder(this)
                .setTitle(existing == null ? "新增班级" : "编辑班级")
                .setView(form)
                .setNegativeButton("取消", null)
                .setPositiveButton("保存", (d, w) -> {
                    String name = etName.getText().toString().trim();
                    if (TextUtils.isEmpty(name)) {
                        Ui.toast(this, "请输入班级名称");
                        return;
                    }
                    Map<String, Object> body = new HashMap<>();
                    body.put("name", name);
                    body.put("grade", etGrade.getText().toString().trim());
                    body.put("major", etMajor.getText().toString().trim());
                    body.put("remark", etRemark.getText().toString().trim());
                    if (existing == null) create(body);
                    else update(existing.id, body);
                })
                .show();
    }

    private void create(Map<String, Object> body) {
        ApiClient.api().createClass(body).enqueue(new ApiCallback<Api.Clazz>() {
            @Override public void onOk(Api.Clazz data) { Ui.toast(ClassManageActivity.this, "已新增"); load(); }
            @Override public void onErr(String message) { Ui.toast(ClassManageActivity.this, message); }
        });
    }

    private void update(long id, Map<String, Object> body) {
        ApiClient.api().updateClass(id, body).enqueue(new ApiCallback<Api.Clazz>() {
            @Override public void onOk(Api.Clazz data) { Ui.toast(ClassManageActivity.this, "已保存"); load(); }
            @Override public void onErr(String message) { Ui.toast(ClassManageActivity.this, message); }
        });
    }
}

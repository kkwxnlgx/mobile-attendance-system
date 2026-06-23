package com.example.attendance.admin;

import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
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
import com.example.attendance.ui.FilterBar;
import com.example.attendance.ui.Ui;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class UserManageActivity extends BaseActivity {

    private SwipeRefreshLayout swipe;
    private TextView tvEmpty;
    private UserAdapter adapter;
    private String roleFilter = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_list);
        setupToolbar("用户管理", "+ 新增", v -> showForm(null));

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
        adapter.setOnItemClick(this::showActions);

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

    private void showActions(Api.User u) {
        boolean enabled = u.status == 1;
        new AlertDialog.Builder(this)
                .setTitle(u.name + "（" + u.username + "）")
                .setItems(new String[]{"编辑", enabled ? "禁用" : "启用", "删除"}, (d, which) -> {
                    if (which == 0) showForm(u);
                    else if (which == 1) toggle(u, enabled);
                    else confirmDelete(u);
                })
                .show();
    }

    private void toggle(Api.User u, boolean enabled) {
        new AlertDialog.Builder(this)
                .setMessage(enabled ? "确认禁用该账号？禁用后将无法登录。" : "确认启用该账号？")
                .setNegativeButton("取消", null)
                .setPositiveButton(enabled ? "禁用" : "启用", (d, w) ->
                        ApiClient.api().toggleUser(u.id).enqueue(new ApiCallback<Api.User>() {
                            @Override public void onOk(Api.User data) { Ui.toast(UserManageActivity.this, "已更新"); load(); }
                            @Override public void onErr(String message) { Ui.toast(UserManageActivity.this, message); }
                        }))
                .show();
    }

    private void confirmDelete(Api.User u) {
        new AlertDialog.Builder(this)
                .setTitle("删除用户")
                .setMessage("确认删除「" + u.name + "」？不可恢复。若该用户已有考勤/课程等数据将无法删除，请改用禁用。")
                .setNegativeButton("取消", null)
                .setPositiveButton("删除", (d, w) ->
                        ApiClient.api().deleteUser(u.id).enqueue(new ApiCallback<Api.Msg>() {
                            @Override public void onOk(Api.Msg data) { Ui.toast(UserManageActivity.this, "已删除"); load(); }
                            @Override public void onErr(String message) { Ui.toast(UserManageActivity.this, message); }
                        }))
                .show();
    }

    // 角色下拉的显示文案与提交值一一对应
    private static final String[] ROLE_LABELS = {"学生", "教师"};
    private static final String[] ROLE_VALUES = {"student", "teacher"};

    private void showForm(final Api.User editing) {
        View form = LayoutInflater.from(this).inflate(R.layout.dialog_user, null);
        Spinner spRole = form.findViewById(R.id.sp_role);
        EditText etUsername = form.findViewById(R.id.et_username);
        EditText etName = form.findViewById(R.id.et_name);
        EditText etPassword = form.findViewById(R.id.et_password);
        EditText etPhone = form.findViewById(R.id.et_phone);

        spRole.setAdapter(new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item, ROLE_LABELS));

        if (editing != null) {
            spRole.setSelection("teacher".equals(editing.role) ? 1 : 0);
            spRole.setEnabled(false);              // 角色不可改
            etUsername.setText(editing.username);
            etUsername.setEnabled(false);          // 账号不可改
            etName.setText(editing.name);
            etPhone.setText(editing.phone == null ? "" : editing.phone);
            etPassword.setHint("新密码（留空不修改）");
        }

        new AlertDialog.Builder(this)
                .setTitle(editing == null ? "新增用户" : "编辑用户")
                .setView(form)
                .setNegativeButton("取消", null)
                .setPositiveButton("保存", (d, w) -> {
                    String name = etName.getText().toString().trim();
                    String password = etPassword.getText().toString();
                    String phone = etPhone.getText().toString().trim();
                    if (TextUtils.isEmpty(name)) {
                        Ui.toast(this, "请填写姓名");
                        return;
                    }
                    if (editing == null) {
                        String username = etUsername.getText().toString().trim();
                        if (TextUtils.isEmpty(username)) {
                            Ui.toast(this, "请填写账号");
                            return;
                        }
                        if (password.length() < 6) {
                            Ui.toast(this, "初始密码至少 6 位");
                            return;
                        }
                        Map<String, Object> body = new HashMap<>();
                        body.put("role", ROLE_VALUES[spRole.getSelectedItemPosition()]);
                        body.put("username", username);
                        body.put("name", name);
                        body.put("password", password);
                        if (!TextUtils.isEmpty(phone)) body.put("phone", phone);
                        create(body);
                    } else {
                        if (!password.isEmpty() && password.length() < 6) {
                            Ui.toast(this, "新密码至少 6 位");
                            return;
                        }
                        Map<String, Object> body = new HashMap<>();
                        body.put("name", name);
                        body.put("phone", phone);              // 允许清空
                        if (!password.isEmpty()) body.put("password", password);
                        update(editing.id, body);
                    }
                })
                .show();
    }

    private void create(Map<String, Object> body) {
        ApiClient.api().createUser(body).enqueue(new ApiCallback<Api.User>() {
            @Override public void onOk(Api.User data) { Ui.toast(UserManageActivity.this, "已新增"); load(); }
            @Override public void onErr(String message) { Ui.toast(UserManageActivity.this, message); }
        });
    }

    private void update(long id, Map<String, Object> body) {
        ApiClient.api().updateUser(id, body).enqueue(new ApiCallback<Api.User>() {
            @Override public void onOk(Api.User data) { Ui.toast(UserManageActivity.this, "已保存"); load(); }
            @Override public void onErr(String message) { Ui.toast(UserManageActivity.this, message); }
        });
    }
}

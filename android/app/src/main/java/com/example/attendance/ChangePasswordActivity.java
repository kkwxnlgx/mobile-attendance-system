package com.example.attendance;

import android.os.Bundle;
import android.text.TextUtils;
import android.widget.Button;
import android.widget.EditText;

import com.example.attendance.model.Api;
import com.example.attendance.net.ApiCallback;
import com.example.attendance.net.ApiClient;
import com.example.attendance.ui.BaseActivity;
import com.example.attendance.ui.Ui;

import java.util.HashMap;
import java.util.Map;

/** 三类角色通用的自助修改密码界面。 */
public class ChangePasswordActivity extends BaseActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_change_password);
        setupToolbar("修改密码");

        EditText etOld = findViewById(R.id.et_old);
        EditText etNew = findViewById(R.id.et_new);
        EditText etConfirm = findViewById(R.id.et_confirm);
        Button btnSave = findViewById(R.id.btn_save);

        btnSave.setOnClickListener(v -> {
            String oldPwd = etOld.getText().toString();
            String newPwd = etNew.getText().toString();
            String confirm = etConfirm.getText().toString();
            if (TextUtils.isEmpty(oldPwd)) {
                Ui.toast(this, "请输入原密码");
                return;
            }
            if (newPwd.length() < 6) {
                Ui.toast(this, "新密码至少 6 位");
                return;
            }
            if (!newPwd.equals(confirm)) {
                Ui.toast(this, "两次新密码不一致");
                return;
            }
            Map<String, Object> body = new HashMap<>();
            body.put("old_password", oldPwd);
            body.put("new_password", newPwd);
            ApiClient.api().changePassword(body).enqueue(new ApiCallback<Api.Msg>() {
                @Override public void onOk(Api.Msg data) { Ui.toast(ChangePasswordActivity.this, "密码已修改"); finish(); }
                @Override public void onErr(String message) { Ui.toast(ChangePasswordActivity.this, message); }
            });
        });
    }
}

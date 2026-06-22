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

public class RegisterActivity extends BaseActivity {

    private EditText etUsername, etName, etPhone, etPassword, etPassword2;
    private Button btnRegister;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register);
        setupToolbar("学生注册");

        etUsername = findViewById(R.id.et_username);
        etName = findViewById(R.id.et_name);
        etPhone = findViewById(R.id.et_phone);
        etPassword = findViewById(R.id.et_password);
        etPassword2 = findViewById(R.id.et_password2);
        btnRegister = findViewById(R.id.btn_register);

        btnRegister.setOnClickListener(v -> doRegister());
    }

    private void doRegister() {
        String username = etUsername.getText().toString().trim();
        String name = etName.getText().toString().trim();
        String phone = etPhone.getText().toString().trim();
        String pwd = etPassword.getText().toString();
        String pwd2 = etPassword2.getText().toString();

        if (TextUtils.isEmpty(username) || TextUtils.isEmpty(name)) {
            Ui.toast(this, "请填写学号和姓名");
            return;
        }
        if (pwd.length() < 6) {
            Ui.toast(this, "密码不少于6位");
            return;
        }
        if (!pwd.equals(pwd2)) {
            Ui.toast(this, "两次密码不一致");
            return;
        }
        btnRegister.setEnabled(false);
        Map<String, Object> body = new HashMap<>();
        body.put("username", username);
        body.put("name", name);
        body.put("password", pwd);
        if (!TextUtils.isEmpty(phone)) body.put("phone", phone);

        ApiClient.api().register(body).enqueue(new ApiCallback<Api.User>() {
            @Override
            public void onOk(Api.User data) {
                btnRegister.setEnabled(true);
                Ui.toast(RegisterActivity.this, "注册成功，请登录");
                finish();
            }

            @Override
            public void onErr(String message) {
                btnRegister.setEnabled(true);
                Ui.toast(RegisterActivity.this, message);
            }
        });
    }
}

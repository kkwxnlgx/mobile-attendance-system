package com.example.attendance;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.example.attendance.model.Api;
import com.example.attendance.net.ApiCallback;
import com.example.attendance.net.ApiClient;
import com.example.attendance.ui.Ui;

import java.util.HashMap;
import java.util.Map;

public class LoginActivity extends AppCompatActivity {

    private EditText etUsername, etPassword;
    private Button btnLogin;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // 已登录则直接进入对应主页（杀进程重开免登录）
        if (SessionManager.isLoggedIn()) {
            goHome();
            return;
        }

        setContentView(R.layout.activity_login);
        etUsername = findViewById(R.id.et_username);
        etPassword = findViewById(R.id.et_password);
        btnLogin = findViewById(R.id.btn_login);

        btnLogin.setOnClickListener(v -> doLogin());
        findViewById(R.id.tv_register).setOnClickListener(v ->
                startActivity(new Intent(this, RegisterActivity.class)));
    }

    private void doLogin() {
        String username = etUsername.getText().toString().trim();
        String password = etPassword.getText().toString();
        if (TextUtils.isEmpty(username) || TextUtils.isEmpty(password)) {
            Ui.toast(this, "请输入账号和密码");
            return;
        }
        btnLogin.setEnabled(false);
        Map<String, Object> body = new HashMap<>();
        body.put("username", username);
        body.put("password", password);
        ApiClient.api().login(body).enqueue(new ApiCallback<Api.LoginResp>() {
            @Override
            public void onOk(Api.LoginResp r) {
                btnLogin.setEnabled(true);
                if (r == null) { Ui.toast(LoginActivity.this, "登录失败"); return; }
                SessionManager.save(r.token, r.user_id, r.username, r.name, r.role, r.class_id);
                goHome();
            }

            @Override
            public void onErr(String message) {
                btnLogin.setEnabled(true);
                Ui.toast(LoginActivity.this, message);
            }
        });
    }

    private void goHome() {
        Intent i = SessionManager.homeIntent(this);
        i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(i);
        finish();
    }
}

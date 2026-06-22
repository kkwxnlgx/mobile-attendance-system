package com.example.attendance.student;

import android.Manifest;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.text.TextUtils;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.example.attendance.R;
import com.example.attendance.model.Api;
import com.example.attendance.net.ApiCallback;
import com.example.attendance.net.ApiClient;
import com.example.attendance.ui.BaseActivity;
import com.example.attendance.ui.Ui;

import java.util.HashMap;
import java.util.Map;

/** 学生定位签到。使用 LocationManager 监听 GPS/网络定位，提交后由服务端判定状态。 */
public class CheckinActivity extends BaseActivity implements LocationListener {

    private static final int REQ_LOCATION = 1001;

    private EditText etCode;
    private TextView tvLocStatus, tvLocDetail, tvCourse, tvCourseSub;
    private Button btnSubmit;

    private LocationManager locationManager;
    private Double curLat, curLng;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_checkin);
        setupToolbar("定位签到");

        etCode = findViewById(R.id.et_code);
        tvLocStatus = findViewById(R.id.tv_loc_status);
        tvLocDetail = findViewById(R.id.tv_loc_detail);
        tvCourse = findViewById(R.id.tv_course);
        tvCourseSub = findViewById(R.id.tv_course_sub);
        btnSubmit = findViewById(R.id.btn_submit);

        String courseName = getIntent().getStringExtra("course_name");
        if (!TextUtils.isEmpty(courseName)) tvCourse.setText(courseName);

        locationManager = (LocationManager) getSystemService(LOCATION_SERVICE);
        findViewById(R.id.btn_relocate).setOnClickListener(v -> startLocation());
        btnSubmit.setOnClickListener(v -> submit());

        ensurePermissionAndLocate();
    }

    private void ensurePermissionAndLocate() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.ACCESS_FINE_LOCATION,
                            Manifest.permission.ACCESS_COARSE_LOCATION}, REQ_LOCATION);
        } else {
            startLocation();
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQ_LOCATION) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                startLocation();
            } else {
                tvLocStatus.setText("未授权定位权限");
                tvLocDetail.setText("无法获取位置，请在系统设置中授权");
            }
        }
    }

    private void startLocation() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {
            ensurePermissionAndLocate();
            return;
        }
        tvLocStatus.setText("正在获取定位…");
        tvLocDetail.setText("请稍候");
        try {
            // 同时监听 GPS 与网络定位，模拟器 Extended Controls 注入的坐标走 GPS。
            if (locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
                locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 1000, 0, this);
            }
            if (locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER)) {
                locationManager.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, 1000, 0, this);
            }
            // 先尝试 lastKnownLocation 兜底
            Location last = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER);
            if (last == null) {
                last = locationManager.getLastKnownLocation(LocationManager.NETWORK_PROVIDER);
            }
            if (last != null) onLocationChanged(last);
        } catch (SecurityException e) {
            tvLocStatus.setText("定位失败");
            tvLocDetail.setText(e.getMessage());
        }
    }

    @Override
    public void onLocationChanged(@NonNull Location location) {
        curLat = location.getLatitude();
        curLng = location.getLongitude();
        tvLocStatus.setText("定位成功");
        tvLocDetail.setText(String.format("当前坐标 %.5f, %.5f", curLat, curLng));
    }

    private void submit() {
        String code = etCode.getText().toString().trim();
        if (TextUtils.isEmpty(code)) {
            Ui.toast(this, "请输入签到码");
            return;
        }
        if (curLat == null || curLng == null) {
            Ui.toast(this, "定位尚未就绪，请点击重新定位");
            return;
        }
        btnSubmit.setEnabled(false);
        Map<String, Object> body = new HashMap<>();
        body.put("code", code);
        body.put("latitude", curLat);
        body.put("longitude", curLng);
        ApiClient.api().checkin(body).enqueue(new ApiCallback<Api.CheckinResp>() {
            @Override
            public void onOk(Api.CheckinResp r) {
                btnSubmit.setEnabled(true);
                showResult(r);
            }

            @Override
            public void onErr(String message) {
                btnSubmit.setEnabled(true);
                new androidx.appcompat.app.AlertDialog.Builder(CheckinActivity.this)
                        .setTitle("签到失败")
                        .setMessage(message)
                        .setPositiveButton("知道了", null)
                        .show();
            }
        });
    }

    private void showResult(Api.CheckinResp r) {
        String title;
        switch (r.status) {
            case "normal": title = "✅ 签到成功（正常）"; break;
            case "late": title = "⚠️ 签到成功（迟到）"; break;
            case "location_error": title = "❌ 位置异常"; break;
            default: title = "签到结果"; break;
        }
        new androidx.appcompat.app.AlertDialog.Builder(this)
                .setTitle(title)
                .setMessage(r.message)
                .setPositiveButton("完成", (d, w) -> finish())
                .setCancelable(false)
                .show();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        try {
            locationManager.removeUpdates(this);
        } catch (SecurityException ignored) {
        }
    }
}

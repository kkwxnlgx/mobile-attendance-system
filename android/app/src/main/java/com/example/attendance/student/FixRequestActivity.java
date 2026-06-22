package com.example.attendance.student;

import android.os.Bundle;
import android.text.TextUtils;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TextView;

import com.example.attendance.R;
import com.example.attendance.model.Api;
import com.example.attendance.net.ApiCallback;
import com.example.attendance.net.ApiClient;
import com.example.attendance.ui.BaseActivity;
import com.example.attendance.ui.Ui;

import java.util.HashMap;
import java.util.Map;

public class FixRequestActivity extends BaseActivity {

    private Spinner spReason;
    private EditText etDesc;
    private Button btnSubmit;
    private long recordId;
    private final String[] reasonValues = {"forgot", "gps_fail", "other"};

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_fix_request);
        setupToolbar("补卡申请");

        recordId = getIntent().getLongExtra("record_id", -1);
        String course = getIntent().getStringExtra("course_name");
        String status = getIntent().getStringExtra("status");
        ((TextView) findViewById(R.id.tv_record_info)).setText(
                "针对记录：" + (course == null ? "课程" : course) + " · " + Ui.statusLabel(status));

        spReason = findViewById(R.id.sp_reason);
        etDesc = findViewById(R.id.et_desc);
        btnSubmit = findViewById(R.id.btn_submit);

        spReason.setAdapter(new ArrayAdapter<>(this,
                android.R.layout.simple_spinner_dropdown_item,
                new String[]{"忘记签到", "定位失败", "其他"}));

        btnSubmit.setOnClickListener(v -> submit());
    }

    private void submit() {
        if (recordId < 0) {
            Ui.toast(this, "记录无效");
            return;
        }
        String desc = etDesc.getText().toString().trim();
        if (TextUtils.isEmpty(desc)) {
            Ui.toast(this, "请填写情况说明");
            return;
        }
        btnSubmit.setEnabled(false);
        Map<String, Object> body = new HashMap<>();
        body.put("record_id", recordId);
        body.put("reason_type", reasonValues[spReason.getSelectedItemPosition()]);
        body.put("description", desc);
        ApiClient.api().createFix(body).enqueue(new ApiCallback<Api.Fix>() {
            @Override
            public void onOk(Api.Fix data) {
                btnSubmit.setEnabled(true);
                Ui.toast(FixRequestActivity.this, "补卡申请已提交");
                finish();
            }

            @Override
            public void onErr(String message) {
                btnSubmit.setEnabled(true);
                Ui.toast(FixRequestActivity.this, message);
            }
        });
    }
}

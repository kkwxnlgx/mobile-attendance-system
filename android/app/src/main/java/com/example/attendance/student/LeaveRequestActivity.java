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
import com.example.attendance.ui.DateTimePicker;
import com.example.attendance.ui.Ui;

import java.util.HashMap;
import java.util.Map;

public class LeaveRequestActivity extends BaseActivity {

    private Spinner spType;
    private TextView tvStart, tvEnd;
    private EditText etReason;
    private Button btnSubmit;

    private String startIso, endIso;
    private final String[] typeValues = {"sick", "personal", "other"};

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_leave_request);
        setupToolbar("请假申请");

        spType = findViewById(R.id.sp_type);
        tvStart = findViewById(R.id.tv_start);
        tvEnd = findViewById(R.id.tv_end);
        etReason = findViewById(R.id.et_reason);
        btnSubmit = findViewById(R.id.btn_submit);

        ArrayAdapter<String> a = new ArrayAdapter<>(this,
                android.R.layout.simple_spinner_dropdown_item,
                new String[]{"病假", "事假", "其他"});
        spType.setAdapter(a);

        tvStart.setOnClickListener(v -> DateTimePicker.pick(this, (iso, disp) -> {
            startIso = iso;
            tvStart.setText(disp);
        }));
        tvEnd.setOnClickListener(v -> DateTimePicker.pick(this, (iso, disp) -> {
            endIso = iso;
            tvEnd.setText(disp);
        }));
        btnSubmit.setOnClickListener(v -> submit());
    }

    private void submit() {
        if (startIso == null || endIso == null) {
            Ui.toast(this, "请选择开始和结束时间");
            return;
        }
        if (endIso.compareTo(startIso) < 0) {
            Ui.toast(this, "结束时间不能早于开始时间");
            return;
        }
        String reason = etReason.getText().toString().trim();
        if (TextUtils.isEmpty(reason)) {
            Ui.toast(this, "请填写请假原因");
            return;
        }
        btnSubmit.setEnabled(false);
        Map<String, Object> body = new HashMap<>();
        body.put("type", typeValues[spType.getSelectedItemPosition()]);
        body.put("start_time", startIso);
        body.put("end_time", endIso);
        body.put("reason", reason);
        ApiClient.api().createLeave(body).enqueue(new ApiCallback<Api.Leave>() {
            @Override
            public void onOk(Api.Leave data) {
                btnSubmit.setEnabled(true);
                Ui.toast(LeaveRequestActivity.this, "请假申请已提交");
                finish();
            }

            @Override
            public void onErr(String message) {
                btnSubmit.setEnabled(true);
                Ui.toast(LeaveRequestActivity.this, message);
            }
        });
    }
}

package com.example.attendance.teacher;

import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.widget.Button;
import android.widget.TextView;

import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.attendance.R;
import com.example.attendance.adapter.RecordAdapter;
import com.example.attendance.model.Api;
import com.example.attendance.net.ApiCallback;
import com.example.attendance.net.ApiClient;
import com.example.attendance.ui.BaseActivity;
import com.example.attendance.ui.Ui;

/** 考勤监控：大字签到码 + 实时计数 + 名单，每 5 秒轮询刷新。 */
public class MonitorActivity extends BaseActivity {

    private TextView tvCode, tvCourse, tvArrived, cNormal, cLate, cAbsent, cLeave, cLocErr;
    private RecordAdapter adapter;
    private Button btnFinish;
    private long taskId;
    private boolean finished = false;

    private final Handler handler = new Handler(Looper.getMainLooper());
    private final Runnable poll = new Runnable() {
        @Override
        public void run() {
            load();
            if (!finished) handler.postDelayed(this, 5000);
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_monitor);
        setupToolbar("考勤监控");

        taskId = getIntent().getLongExtra("task_id", -1);
        tvCode = findViewById(R.id.tv_code);
        tvCourse = findViewById(R.id.tv_course);
        tvArrived = findViewById(R.id.tv_arrived);
        cNormal = findViewById(R.id.c_normal);
        cLate = findViewById(R.id.c_late);
        cAbsent = findViewById(R.id.c_absent);
        cLeave = findViewById(R.id.c_leave);
        cLocErr = findViewById(R.id.c_locerr);
        btnFinish = findViewById(R.id.btn_finish);

        adapter = new RecordAdapter(true);
        RecyclerView recycler = findViewById(R.id.recycler);
        recycler.setLayoutManager(new LinearLayoutManager(this));
        recycler.setAdapter(adapter);

        btnFinish.setOnClickListener(v -> confirmFinish());
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (!finished) handler.post(poll);
    }

    @Override
    protected void onPause() {
        super.onPause();
        handler.removeCallbacks(poll);
    }

    private void load() {
        if (taskId < 0) return;
        ApiClient.api().monitor(taskId).enqueue(new ApiCallback<Api.Monitor>() {
            @Override
            public void onOk(Api.Monitor m) {
                if (m == null) return;
                tvCode.setText(m.task.code);
                tvCourse.setText(m.task.course_name + " · " + m.task.class_name);
                int arrived = m.normal + m.late;
                tvArrived.setText("已到 " + arrived + " / 应到 " + m.total);
                cNormal.setText("正常\n" + m.normal);
                cLate.setText("迟到\n" + m.late);
                cAbsent.setText("缺勤\n" + m.absent);
                cLeave.setText("请假\n" + m.leave);
                cLocErr.setText("异常\n" + m.location_error);
                adapter.setData(m.records);
                if ("finished".equals(m.task.status)) {
                    finished = true;
                    btnFinish.setText("考勤已结束");
                    btnFinish.setEnabled(false);
                }
            }

            @Override
            public void onErr(String message) {
                Ui.toast(MonitorActivity.this, message);
            }
        });
    }

    private void confirmFinish() {
        new androidx.appcompat.app.AlertDialog.Builder(this)
                .setTitle("结束考勤")
                .setMessage("结束后学生将无法继续签到，未签到者记为缺勤。确认结束？")
                .setNegativeButton("取消", null)
                .setPositiveButton("确认结束", (d, w) -> doFinish())
                .show();
    }

    private void doFinish() {
        ApiClient.api().finishTask(taskId).enqueue(new ApiCallback<Api.Task>() {
            @Override
            public void onOk(Api.Task t) {
                finished = true;
                handler.removeCallbacks(poll);
                btnFinish.setText("考勤已结束");
                btnFinish.setEnabled(false);
                Ui.toast(MonitorActivity.this, "考勤已结束");
                load();
            }

            @Override
            public void onErr(String message) {
                Ui.toast(MonitorActivity.this, message);
            }
        });
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        handler.removeCallbacks(poll);
    }
}

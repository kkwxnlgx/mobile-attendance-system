package com.example.attendance.teacher;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.TextView;

import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.example.attendance.R;
import com.example.attendance.adapter.RecordAdapter;
import com.example.attendance.model.Api;
import com.example.attendance.net.ApiCallback;
import com.example.attendance.net.ApiClient;
import com.example.attendance.ui.BaseActivity;
import com.example.attendance.ui.FilterBar;
import com.example.attendance.ui.Ui;

import java.util.ArrayList;
import java.util.List;

public class RecordsQueryActivity extends BaseActivity {

    private SwipeRefreshLayout swipe;
    private TextView tvEmpty;
    private Spinner spClass;
    private RecordAdapter adapter;
    private String statusFilter = null;
    private final List<Api.Clazz> classes = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_list);
        setupToolbar("考勤记录查询");

        FrameLayout headerSlot = findViewById(R.id.header_slot);
        View header = LayoutInflater.from(this).inflate(R.layout.view_records_header, headerSlot, false);
        headerSlot.addView(header);
        spClass = header.findViewById(R.id.sp_class);
        LinearLayout filter = header.findViewById(R.id.filter_container);

        swipe = findViewById(R.id.swipe);
        tvEmpty = findViewById(R.id.tv_empty);
        RecyclerView recycler = findViewById(R.id.recycler);
        adapter = new RecordAdapter(true);
        recycler.setLayoutManager(new LinearLayoutManager(this));
        recycler.setAdapter(adapter);

        new FilterBar(this, filter,
                new String[]{"全部", "正常", "迟到", "缺勤", "请假", "位置异常"},
                new String[]{null, "normal", "late", "absent", "leave", "location_error"},
                value -> {
                    statusFilter = value;
                    load();
                });

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
                spClass.setAdapter(new ArrayAdapter<>(RecordsQueryActivity.this,
                        android.R.layout.simple_spinner_dropdown_item, names));
                if (!classes.isEmpty()) load();
            }

            @Override
            public void onErr(String message) {
                Ui.toast(RecordsQueryActivity.this, message);
            }
        });
    }

    private void load() {
        long cid = currentClassId();
        if (cid < 0) {
            swipe.setRefreshing(false);
            return;
        }
        ApiClient.api().queryRecords(cid, null, statusFilter).enqueue(new ApiCallback<List<Api.Record>>() {
            @Override
            public void onOk(List<Api.Record> data) {
                swipe.setRefreshing(false);
                adapter.setData(data);
                tvEmpty.setVisibility(data == null || data.isEmpty() ? View.VISIBLE : View.GONE);
            }

            @Override
            public void onErr(String message) {
                swipe.setRefreshing(false);
                Ui.toast(RecordsQueryActivity.this, message);
            }
        });
    }
}

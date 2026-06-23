package com.example.attendance.admin;

import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TextView;

import androidx.appcompat.app.AlertDialog;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.example.attendance.R;
import com.example.attendance.adapter.CourseAdapter;
import com.example.attendance.model.Api;
import com.example.attendance.net.ApiCallback;
import com.example.attendance.net.ApiClient;
import com.example.attendance.ui.BaseActivity;
import com.example.attendance.ui.Ui;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/** 管理员课程管理：列出全部课程，新增时为课程指定授课教师。 */
public class CourseManageActivity extends BaseActivity {

    private SwipeRefreshLayout swipe;
    private TextView tvEmpty;
    private CourseAdapter adapter;
    private final List<Api.User> teachers = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_list);
        setupToolbar("课程管理", "+ 新增", v -> showForm(null));

        swipe = findViewById(R.id.swipe);
        tvEmpty = findViewById(R.id.tv_empty);
        RecyclerView recycler = findViewById(R.id.recycler);
        adapter = new CourseAdapter();
        adapter.setOnItemClick(this::showActions);
        recycler.setLayoutManager(new LinearLayoutManager(this));
        recycler.setAdapter(adapter);

        swipe.setOnRefreshListener(this::load);
        loadTeachers();
        load();
    }

    private void loadTeachers() {
        ApiClient.api().adminUsers("teacher", "").enqueue(new ApiCallback<List<Api.User>>() {
            @Override public void onOk(List<Api.User> data) {
                teachers.clear();
                if (data != null) teachers.addAll(data);
            }
        });
    }

    private void load() {
        ApiClient.api().listCourses().enqueue(new ApiCallback<List<Api.Course>>() {
            @Override
            public void onOk(List<Api.Course> data) {
                swipe.setRefreshing(false);
                adapter.setData(data);
                tvEmpty.setVisibility(data == null || data.isEmpty() ? View.VISIBLE : View.GONE);
            }

            @Override
            public void onErr(String message) {
                swipe.setRefreshing(false);
                Ui.toast(CourseManageActivity.this, message);
            }
        });
    }

    private void showActions(Api.Course c) {
        new AlertDialog.Builder(this)
                .setTitle(c.name)
                .setItems(new String[]{"编辑", "删除"}, (d, which) -> {
                    if (which == 0) showForm(c);
                    else confirmDelete(c);
                })
                .show();
    }

    private void showForm(final Api.Course editing) {
        if (teachers.isEmpty()) {
            Ui.toast(this, "请先新增教师，再创建课程");
            return;
        }
        View form = LayoutInflater.from(this).inflate(R.layout.dialog_course, null);
        EditText etName = form.findViewById(R.id.et_name);
        EditText etSemester = form.findViewById(R.id.et_semester);
        Spinner spTeacher = form.findViewById(R.id.sp_teacher);

        List<String> teacherNames = new ArrayList<>();
        for (Api.User t : teachers) teacherNames.add(t.name + "（" + t.username + "）");
        spTeacher.setAdapter(new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item, teacherNames));

        if (editing != null) {
            etName.setText(editing.name);
            etSemester.setText(editing.semester == null ? "" : editing.semester);
            spTeacher.setSelection(indexOfTeacher(editing.teacher_id));
        }

        new AlertDialog.Builder(this)
                .setTitle(editing == null ? "新增课程" : "编辑课程")
                .setView(form)
                .setNegativeButton("取消", null)
                .setPositiveButton("保存", (d, w) -> {
                    String name = etName.getText().toString().trim();
                    if (TextUtils.isEmpty(name)) {
                        Ui.toast(this, "请输入课程名称");
                        return;
                    }
                    Map<String, Object> body = new HashMap<>();
                    body.put("name", name);
                    body.put("teacher_id", teachers.get(spTeacher.getSelectedItemPosition()).id);
                    String semester = etSemester.getText().toString().trim();
                    if (!TextUtils.isEmpty(semester)) body.put("semester", semester);
                    if (editing == null) create(body);
                    else update(editing.id, body);
                })
                .show();
    }

    private int indexOfTeacher(long teacherId) {
        for (int i = 0; i < teachers.size(); i++) if (teachers.get(i).id == teacherId) return i;
        return 0;
    }

    private void confirmDelete(Api.Course c) {
        new AlertDialog.Builder(this)
                .setTitle("删除课程")
                .setMessage("确认删除「" + c.name + "」？不可恢复。若该课程已有排课或考勤将无法删除。")
                .setNegativeButton("取消", null)
                .setPositiveButton("删除", (d, w) ->
                        ApiClient.api().deleteCourse(c.id).enqueue(new ApiCallback<Api.Msg>() {
                            @Override public void onOk(Api.Msg data) { Ui.toast(CourseManageActivity.this, "已删除"); load(); }
                            @Override public void onErr(String message) { Ui.toast(CourseManageActivity.this, message); }
                        }))
                .show();
    }

    private void create(Map<String, Object> body) {
        ApiClient.api().createCourse(body).enqueue(new ApiCallback<Api.Course>() {
            @Override public void onOk(Api.Course data) { Ui.toast(CourseManageActivity.this, "已新增"); load(); }
            @Override public void onErr(String message) { Ui.toast(CourseManageActivity.this, message); }
        });
    }

    private void update(long id, Map<String, Object> body) {
        ApiClient.api().updateCourse(id, body).enqueue(new ApiCallback<Api.Course>() {
            @Override public void onOk(Api.Course data) { Ui.toast(CourseManageActivity.this, "已保存"); load(); }
            @Override public void onErr(String message) { Ui.toast(CourseManageActivity.this, message); }
        });
    }
}

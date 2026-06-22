package com.example.attendance;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;

import com.example.attendance.student.StudentMainActivity;
import com.example.attendance.teacher.TeacherMainActivity;
import com.example.attendance.admin.AdminMainActivity;

/** 登录状态持久化（SharedPreferences）。 */
public class SessionManager {
    private static final String PREF = "attendance_session";
    private static final String K_TOKEN = "token";
    private static final String K_UID = "user_id";
    private static final String K_USERNAME = "username";
    private static final String K_NAME = "name";
    private static final String K_ROLE = "role";
    private static final String K_CLASS = "class_id";

    private static SharedPreferences sp() {
        return App.get().getSharedPreferences(PREF, Context.MODE_PRIVATE);
    }

    public static void save(String token, long uid, String username, String name, String role, Long classId) {
        sp().edit()
                .putString(K_TOKEN, token)
                .putLong(K_UID, uid)
                .putString(K_USERNAME, username)
                .putString(K_NAME, name)
                .putString(K_ROLE, role)
                .putLong(K_CLASS, classId == null ? -1 : classId)
                .apply();
    }

    public static String token() { return sp().getString(K_TOKEN, null); }
    public static long userId() { return sp().getLong(K_UID, -1); }
    public static String username() { return sp().getString(K_USERNAME, ""); }
    public static String name() { return sp().getString(K_NAME, ""); }
    public static String role() { return sp().getString(K_ROLE, ""); }
    public static long classId() { return sp().getLong(K_CLASS, -1); }

    public static boolean isLoggedIn() {
        return token() != null;
    }

    public static void logout() {
        sp().edit().clear().apply();
    }

    /** 根据角色返回对应主页 Activity。 */
    public static Class<?> homeFor(String role) {
        if ("teacher".equals(role)) return TeacherMainActivity.class;
        if ("admin".equals(role)) return AdminMainActivity.class;
        return StudentMainActivity.class;
    }

    public static Intent homeIntent(Context ctx) {
        return new Intent(ctx, homeFor(role()));
    }
}

package com.example.attendance.net;

import androidx.annotation.NonNull;

import org.json.JSONObject;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

/**
 * 统一回调：成功走 onOk(T)，失败走 onErr(message)。
 * 失败时优先解析 FastAPI 的 {"detail": "..."} 错误信息。
 * Retrofit 在 Android 上默认在主线程回调，可直接更新 UI。
 */
public abstract class ApiCallback<T> implements Callback<T> {

    public abstract void onOk(T data);

    public void onErr(String message) {
        // 默认空实现，子类可覆盖
    }

    @Override
    public void onResponse(@NonNull Call<T> call, @NonNull Response<T> response) {
        if (response.isSuccessful()) {
            onOk(response.body());
        } else {
            onErr(parseError(response));
        }
    }

    @Override
    public void onFailure(@NonNull Call<T> call, @NonNull Throwable t) {
        onErr("网络异常：" + (t.getMessage() == null ? "无法连接服务器" : t.getMessage()));
    }

    private String parseError(Response<T> response) {
        try {
            if (response.errorBody() != null) {
                String body = response.errorBody().string();
                JSONObject obj = new JSONObject(body);
                if (obj.has("detail")) {
                    Object d = obj.get("detail");
                    return d.toString();
                }
            }
        } catch (Exception ignored) {
        }
        return "请求失败（" + response.code() + "）";
    }
}

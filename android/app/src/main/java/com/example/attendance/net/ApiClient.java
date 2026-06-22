package com.example.attendance.net;

import com.example.attendance.SessionManager;

import java.util.concurrent.TimeUnit;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

/** Retrofit 单例。模拟器通过 10.0.2.2 访问宿主机回环地址上的后端。 */
public class ApiClient {

    // Android 模拟器中 10.0.2.2 指向宿主机 127.0.0.1
    public static final String BASE_URL = "http://10.0.2.2:8000/";

    private static ApiService service;

    public static ApiService api() {
        if (service == null) {
            OkHttpClient client = new OkHttpClient.Builder()
                    .connectTimeout(15, TimeUnit.SECONDS)
                    .readTimeout(20, TimeUnit.SECONDS)
                    .addInterceptor(chain -> {
                        Request.Builder b = chain.request().newBuilder();
                        String token = SessionManager.token();
                        if (token != null) {
                            b.header("Authorization", "Bearer " + token);
                        }
                        return chain.proceed(b.build());
                    })
                    .build();

            Retrofit retrofit = new Retrofit.Builder()
                    .baseUrl(BASE_URL)
                    .client(client)
                    .addConverterFactory(GsonConverterFactory.create())
                    .build();
            service = retrofit.create(ApiService.class);
        }
        return service;
    }
}

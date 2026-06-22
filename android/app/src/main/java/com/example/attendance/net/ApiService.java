package com.example.attendance.net;

import com.example.attendance.model.Api;

import java.util.List;
import java.util.Map;

import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.DELETE;
import retrofit2.http.GET;
import retrofit2.http.POST;
import retrofit2.http.PUT;
import retrofit2.http.Path;
import retrofit2.http.Query;

/** 全部后端接口定义。请求体统一用 Map，响应用 model/Api 中的 DTO。 */
public interface ApiService {

    // ---- 认证 ----
    @POST("api/auth/register")
    Call<Api.User> register(@Body Map<String, Object> body);

    @POST("api/auth/login")
    Call<Api.LoginResp> login(@Body Map<String, Object> body);

    @GET("api/auth/me")
    Call<Api.User> me();

    // ---- 班级 / 名单 ----
    @GET("api/classes")
    Call<List<Api.Clazz>> listClasses();

    @POST("api/classes")
    Call<Api.Clazz> createClass(@Body Map<String, Object> body);

    @PUT("api/classes/{id}")
    Call<Api.Clazz> updateClass(@Path("id") long id, @Body Map<String, Object> body);

    @PUT("api/classes/{id}/status")
    Call<Api.Clazz> toggleClass(@Path("id") long id);

    @GET("api/classes/{id}/students")
    Call<List<Api.User>> roster(@Path("id") long id, @Query("keyword") String keyword);

    @POST("api/classes/{id}/students")
    Call<Api.User> addStudent(@Path("id") long id, @Body Map<String, Object> body);

    @DELETE("api/classes/{id}/students/{sid}")
    Call<Api.Msg> removeStudent(@Path("id") long id, @Path("sid") long sid);

    // ---- 课程 / 课程表 ----
    @GET("api/courses")
    Call<List<Api.Course>> listCourses();

    @POST("api/courses")
    Call<Api.Course> createCourse(@Body Map<String, Object> body);

    @GET("api/schedules")
    Call<List<Api.Schedule>> listSchedules(@Query("class_id") Long classId);

    @POST("api/schedules")
    Call<Api.Schedule> createSchedule(@Body Map<String, Object> body);

    @PUT("api/schedules/{id}")
    Call<Api.Schedule> updateSchedule(@Path("id") long id, @Body Map<String, Object> body);

    @GET("api/schedules/today")
    Call<List<Api.Schedule>> today();

    // ---- 考勤 ----
    @POST("api/attendance/tasks")
    Call<Api.Task> launchTask(@Body Map<String, Object> body);

    @GET("api/attendance/tasks/active")
    Call<List<Api.Task>> activeTasks();

    @GET("api/attendance/tasks/mine_active")
    Call<List<Api.Task>> mineActiveTasks();

    @GET("api/attendance/tasks/{id}/monitor")
    Call<Api.Monitor> monitor(@Path("id") long id);

    @POST("api/attendance/tasks/{id}/finish")
    Call<Api.Task> finishTask(@Path("id") long id);

    @POST("api/attendance/checkin")
    Call<Api.CheckinResp> checkin(@Body Map<String, Object> body);

    @GET("api/attendance/records/my")
    Call<List<Api.Record>> myRecords(@Query("status") String status, @Query("course_id") Long courseId);

    @GET("api/attendance/records")
    Call<List<Api.Record>> queryRecords(@Query("class_id") Long classId,
                                        @Query("course_id") Long courseId,
                                        @Query("status") String status);

    // ---- 请假 ----
    @POST("api/leaves")
    Call<Api.Leave> createLeave(@Body Map<String, Object> body);

    @GET("api/leaves/my")
    Call<List<Api.Leave>> myLeaves();

    @GET("api/leaves/pending")
    Call<List<Api.Leave>> pendingLeaves();

    @POST("api/leaves/{id}/approve")
    Call<Api.Leave> approveLeave(@Path("id") long id, @Body Map<String, Object> body);

    // ---- 补卡 ----
    @POST("api/fixes")
    Call<Api.Fix> createFix(@Body Map<String, Object> body);

    @GET("api/fixes/my")
    Call<List<Api.Fix>> myFixes();

    @GET("api/fixes/pending")
    Call<List<Api.Fix>> pendingFixes();

    @POST("api/fixes/{id}/approve")
    Call<Api.Fix> approveFix(@Path("id") long id, @Body Map<String, Object> body);

    // ---- 统计 ----
    @GET("api/stats/summary")
    Call<Api.StatSummary> stats(@Query("class_id") Long classId,
                                @Query("course_id") Long courseId,
                                @Query("start_date") String startDate,
                                @Query("end_date") String endDate);

    // ---- 通知 ----
    @GET("api/notifications")
    Call<List<Api.Notification>> notifications();

    @POST("api/notifications/{id}/read")
    Call<Api.Msg> readNotification(@Path("id") long id);

    // ---- 管理员 ----
    @GET("api/admin/users")
    Call<List<Api.User>> adminUsers(@Query("role") String role, @Query("keyword") String keyword);

    @PUT("api/admin/users/{id}/status")
    Call<Api.User> toggleUser(@Path("id") long id);
}

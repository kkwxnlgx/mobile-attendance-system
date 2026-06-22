package com.example.attendance.model;

import java.util.List;

/**
 * 与后端 FastAPI schemas 对齐的数据传输对象。
 * 字段名采用 snake_case 直接匹配 JSON，Gson 无需额外注解。
 */
public class Api {

    public static class LoginResp {
        public String token;
        public long user_id;
        public String username;
        public String name;
        public String role;
        public Long class_id;
    }

    public static class User {
        public long id;
        public String username;
        public String name;
        public String role;
        public String phone;
        public Long class_id;
        public int status;
    }

    public static class Clazz {
        public long id;
        public String name;
        public String grade;
        public String major;
        public String remark;
        public int status;
        public int student_count;
    }

    public static class Course {
        public long id;
        public String name;
        public long teacher_id;
        public String teacher_name;
        public String semester;
    }

    public static class Schedule {
        public long id;
        public long course_id;
        public String course_name;
        public long class_id;
        public String class_name;
        public int weekday;
        public Integer start_section;
        public Integer end_section;
        public String start_time;   // "HH:MM:SS"
        public String end_time;
        public String weeks;
        public String location;
        public Double latitude;
        public Double longitude;
    }

    public static class Task {
        public long id;
        public long course_id;
        public String course_name;
        public long class_id;
        public String class_name;
        public String code;
        public String start_time;
        public String end_time;
        public int late_offset_min;
        public Double latitude;
        public Double longitude;
        public int radius_m;
        public String status;
    }

    public static class CheckinResp {
        public String status;
        public Double distance_m;
        public Integer radius_m;
        public String message;
    }

    public static class Record {
        public long id;
        public long task_id;
        public long student_id;
        public String student_name;
        public String student_username;
        public String course_name;
        public String class_name;
        public String checkin_time;
        public Double distance_m;
        public String status;
        public String remark;
        public String task_time;
    }

    public static class Monitor {
        public Task task;
        public int total;
        public int normal;
        public int late;
        public int absent;
        public int leave;
        public int location_error;
        public List<Record> records;
    }

    public static class Leave {
        public long id;
        public long student_id;
        public String student_name;
        public Long course_id;
        public String course_name;
        public String type;
        public String start_time;
        public String end_time;
        public String reason;
        public String status;
        public String approve_comment;
        public String approve_time;
    }

    public static class Fix {
        public long id;
        public long record_id;
        public long student_id;
        public String student_name;
        public String course_name;
        public String reason_type;
        public String description;
        public String status;
        public String approve_comment;
        public String task_time;
    }

    public static class StatRow {
        public long student_id;
        public String username;
        public String name;
        public int normal;
        public int late;
        public int absent;
        public int leave;
        public int location_error;
        public int total;
        public double attend_rate;
    }

    public static class StatSummary {
        public int total;
        public int normal;
        public int late;
        public int absent;
        public int leave;
        public int location_error;
        public double attend_rate;
        public List<StatRow> students;
    }

    public static class Notification {
        public long id;
        public String title;
        public String content;
        public String type;
        public int is_read;
        public String created_at;
    }

    public static class Msg {
        public String message;
    }
}

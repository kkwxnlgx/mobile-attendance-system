package com.example.attendance.ui;

import android.content.Context;
import android.widget.Toast;

import androidx.core.content.ContextCompat;

import com.example.attendance.R;

/** UI 通用工具：状态文案/颜色、类型文案、日期格式化。 */
public class Ui {

    public static void toast(Context ctx, String msg) {
        if (ctx != null && msg != null) Toast.makeText(ctx, msg, Toast.LENGTH_SHORT).show();
    }

    /** 考勤状态中文。 */
    public static String statusLabel(String status) {
        if (status == null) return "未知";
        switch (status) {
            case "normal": return "正常";
            case "late": return "迟到";
            case "absent": return "缺勤";
            case "leave": return "请假";
            case "location_error": return "位置异常";
            default: return status;
        }
    }

    public static int statusColor(Context ctx, String status) {
        int res = R.color.status_pending;
        if (status != null) {
            switch (status) {
                case "normal": res = R.color.status_normal; break;
                case "late": res = R.color.status_late; break;
                case "absent": res = R.color.status_absent; break;
                case "leave": res = R.color.status_leave; break;
                case "location_error": res = R.color.status_location_error; break;
            }
        }
        return ContextCompat.getColor(ctx, res);
    }

    /** 审批状态中文。 */
    public static String approvalLabel(String status) {
        if (status == null) return "";
        switch (status) {
            case "pending": return "待审批";
            case "approved": return "已通过";
            case "rejected": return "已驳回";
            default: return status;
        }
    }

    public static String leaveTypeLabel(String type) {
        if (type == null) return "";
        switch (type) {
            case "sick": return "病假";
            case "personal": return "事假";
            default: return "其他";
        }
    }

    public static String fixReasonLabel(String t) {
        if (t == null) return "";
        switch (t) {
            case "forgot": return "忘记签到";
            case "gps_fail": return "定位失败";
            default: return "其他";
        }
    }

    public static String weekdayLabel(int wd) {
        String[] arr = {"", "周一", "周二", "周三", "周四", "周五", "周六", "周日"};
        return (wd >= 1 && wd <= 7) ? arr[wd] : "";
    }

    /** ISO 时间字符串截取为 "MM-dd HH:mm"。后端返回如 2026-06-11T14:23:21。 */
    public static String shortDateTime(String iso) {
        if (iso == null || iso.length() < 16) return iso == null ? "" : iso;
        return iso.substring(5, 10) + " " + iso.substring(11, 16);
    }

    /** 截取 "HH:mm"。 */
    public static String hm(String t) {
        if (t == null || t.length() < 5) return t == null ? "" : t;
        return t.substring(0, 5);
    }

    public static String dateOnly(String iso) {
        if (iso == null || iso.length() < 10) return iso == null ? "" : iso;
        return iso.substring(0, 10);
    }
}

package com.example.attendance.ui;

import android.app.DatePickerDialog;
import android.app.TimePickerDialog;
import android.content.Context;

import java.util.Calendar;
import java.util.Locale;

/** 先选日期再选时间，回调返回 ISO 字符串和用于显示的文本。 */
public class DateTimePicker {

    public interface Callback {
        void onPicked(String iso, String display);
    }

    public interface TimeCallback {
        void onPicked(String hms, String display);
    }

    /** 仅选时间，返回 "HH:mm:00"。 */
    public static void pickTime(Context ctx, TimeCallback cb) {
        final Calendar c = Calendar.getInstance();
        new TimePickerDialog(ctx, (tp, hour, minute) -> {
            String hms = String.format(Locale.US, "%02d:%02d:00", hour, minute);
            String display = String.format(Locale.US, "%02d:%02d", hour, minute);
            cb.onPicked(hms, display);
        }, c.get(Calendar.HOUR_OF_DAY), c.get(Calendar.MINUTE), true).show();
    }

    public static void pick(Context ctx, Callback cb) {
        final Calendar c = Calendar.getInstance();
        new DatePickerDialog(ctx, (dp, year, month, day) ->
                new TimePickerDialog(ctx, (tp, hour, minute) -> {
                    String iso = String.format(Locale.US, "%04d-%02d-%02dT%02d:%02d:00",
                            year, month + 1, day, hour, minute);
                    String display = String.format(Locale.US, "%04d-%02d-%02d %02d:%02d",
                            year, month + 1, day, hour, minute);
                    cb.onPicked(iso, display);
                }, c.get(Calendar.HOUR_OF_DAY), c.get(Calendar.MINUTE), true).show(),
                c.get(Calendar.YEAR), c.get(Calendar.MONTH), c.get(Calendar.DAY_OF_MONTH)).show();
    }
}

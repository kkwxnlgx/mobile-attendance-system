package com.example.attendance.ui;

import android.content.Context;
import android.text.TextUtils;
import android.widget.EditText;

import androidx.appcompat.app.AlertDialog;

/** 审批对话框：同意时意见可选，驳回时意见必填。 */
public class ApproveDialog {

    public interface Callback {
        void onConfirm(String comment);
    }

    public static void show(Context ctx, boolean approve, Callback cb) {
        EditText input = new EditText(ctx);
        input.setHint(approve ? "审批意见（可选）" : "请填写驳回理由");
        int pad = Math.round(ctx.getResources().getDisplayMetrics().density * 16);
        input.setPadding(pad, pad, pad, pad);

        new AlertDialog.Builder(ctx)
                .setTitle(approve ? "同意申请" : "驳回申请")
                .setView(input)
                .setNegativeButton("取消", null)
                .setPositiveButton("确定", (d, w) -> {
                    String comment = input.getText().toString().trim();
                    if (!approve && TextUtils.isEmpty(comment)) {
                        Ui.toast(ctx, "驳回时必须填写理由");
                        return;
                    }
                    cb.onConfirm(comment);
                })
                .show();
    }
}

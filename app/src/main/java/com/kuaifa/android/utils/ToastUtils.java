package com.kuaifa.android.utils;

import android.content.Context;
import android.widget.Toast;

public class ToastUtils {
    public static void showMessage(Context context, String sMsg) {
        Toast.makeText(context, sMsg, Toast.LENGTH_SHORT).show();
    }
}

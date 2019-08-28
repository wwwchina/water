package com.kuaifa.android.utils;

import android.text.TextUtils;
import android.widget.ImageView;

import com.kuaifa.android.MyApplication;
import com.squareup.picasso.Callback;
import com.squareup.picasso.MemoryPolicy;
import com.squareup.picasso.NetworkPolicy;
import com.squareup.picasso.Picasso;

/**
 * 毕加索加载网络图片
 */
public class DisplayImage {


    public static void displayImage(String __url, ImageView __image) {
        Picasso.with(MyApplication.getInstance()).load(__url).into(__image);
    }


    public static void displayImage(String __url, ImageView __image, int default_icon, int error_default_icon) {
        if (!TextUtils.isEmpty(__url))
            Picasso.with(MyApplication.getInstance()).load(__url).placeholder(default_icon).error(error_default_icon).into(__image);
        else
            __image.setImageResource(error_default_icon);
    }

    public static void displayImage2(String __url, ImageView __image, int default_icon, int error_default_icon) {
        if (!TextUtils.isEmpty(__url))
            Picasso.with(MyApplication.getInstance())
                    .load(__url).placeholder(default_icon)
                    .memoryPolicy(MemoryPolicy.NO_CACHE)
                    .networkPolicy(NetworkPolicy.NO_CACHE)
                    .error(error_default_icon).into(__image);
        else
            __image.setImageResource(error_default_icon);
    }


    public static void displayImage(String __url, ImageView __image, int error_default_icon, Callback callback) {
        if (!TextUtils.isEmpty(__url))
            Picasso.with(MyApplication.getInstance()).load(__url).error(error_default_icon).into(__image, callback);
        else
            __image.setImageResource(error_default_icon);
    }
}

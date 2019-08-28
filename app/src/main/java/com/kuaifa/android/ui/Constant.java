package com.kuaifa.android.ui;

import android.os.Environment;

public class Constant {
    public static final String ORDERURL = "http://second.bczhiyinshui.com/";

    public static String WORK_DIR = Environment.getExternalStorageDirectory().getAbsolutePath() + "/kuaifa/";
    public static String WORK_DIR_APK = WORK_DIR + "apk/";
    public static String WORK_DIR_RES = WORK_DIR + "res/";
    public static String WORK_DIR_LOG = WORK_DIR + "log/";
    public static String WORK_DIR_LOG_MAC_FILE = WORK_DIR + "mac.txt";
    public static String WORK_DIR_XML_FILE = WORK_DIR_RES + "aaa.xml";
    public static String WORK_DIR_LOG_FILE = WORK_DIR_LOG+"superservice.log";

}

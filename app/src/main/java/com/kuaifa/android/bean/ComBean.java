package com.kuaifa.android.bean;

import android.annotation.SuppressLint;

import java.text.SimpleDateFormat;

/**
 * Created by silencefun on 2018/1/20.
 */

public class ComBean {

    public byte[] bRec=null;
    public String sRecTime="";
    public String sComPort="";
    public ComBean(String sPort, byte[] buffer, int size){
        sComPort=sPort;
        bRec=new byte[size];
        for (int i = 0; i < size; i++) {
            bRec[i]=buffer[i];
        }
        sRecTime = new SimpleDateFormat("hh:mm:ss").format(new java.util.Date());
    }
}

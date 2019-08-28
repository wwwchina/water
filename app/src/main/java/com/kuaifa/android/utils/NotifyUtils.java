package com.kuaifa.android.utils;

import com.kuaifa.android.listener.NotifyListener;

public class NotifyUtils {

    public static NotifyListener notifyListener;

    public static void setListener(NotifyListener listener){
        if (listener != null){
            notifyListener = listener;
        }
    }

    public static void setPause(){
       if (notifyListener != null){
           notifyListener.notifyPause();
       }
    }
    public static void setStop(){
        if (notifyListener!= null){
            notifyListener.notifyStop();
        }
    }
    public static void setRestart(){
        if (notifyListener!= null){
            notifyListener.notifyReStart();
        }
    }
}

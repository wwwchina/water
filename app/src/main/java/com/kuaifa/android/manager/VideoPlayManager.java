package com.kuaifa.android.manager;

import android.annotation.SuppressLint;
import android.os.Handler;
import android.os.Message;
import android.util.Log;

import com.kuaifa.android.utils.PlayListener;

public class VideoPlayManager {
    private static long lastTime = 0;//最后一次点击的时间
    private static  int PLAY_TIME =  35 * 1000;//2m没有触发就播放视频
//    private static final int PLAY_TIME = 10 * 1000;//10s钟没有触发就播放视频
    private static final int SEND_TIME = 1000;//每1秒钟发送一次信息
    private static PlayListener playlistener;
    public static void setPlayListener(PlayListener listener){
        playlistener = listener;
    }

    public static void startMonitor() {
        isStop=false;

        handler.sendEmptyMessageDelayed(0, SEND_TIME);//每1秒钟发送一次信息
        Log.e("-------------", "开始了 = " + SEND_TIME);
    }

    /**
     * 设置当前时间
     */
    public static void eliminateEvent() {
        lastTime = System.currentTimeMillis();
        Log.e("-------------", "lastTime = " + lastTime);
    }
    public static void setTenSecond(){
        isStop=true;
        handler.removeMessages(0);
        startMonitor();
        eliminateEvent();
//        lastTime = System.currentTimeMillis()-10*1001;
        PLAY_TIME = 10 * 1000;
    }
    public static void stop(){
        isStop=true;
        handler.removeMessages(0);
    }
   static boolean isStop=false;
    @SuppressLint("HandlerLeak")
    private static Handler handler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
            long idleTime = System.currentTimeMillis() - lastTime;
            Log.e("-------------", idleTime+"");
            if (idleTime >= PLAY_TIME) {
                if (playlistener != null){
                    Log.d("-----------","playlistener.onPlay();");
                    playlistener.onPlay();
                }
                PLAY_TIME = 35 * 1000;
                Log.e("-------------", "结束了");
            } else {
                if(!isStop)
                handler.sendEmptyMessageDelayed(0, SEND_TIME);
                Log.e("时间差",PLAY_TIME+"");
            }
        }
    };
}

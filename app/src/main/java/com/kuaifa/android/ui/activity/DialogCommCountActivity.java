package com.kuaifa.android.ui.activity;

import android.annotation.SuppressLint;
import android.content.Context;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import androidx.appcompat.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.kuaifa.android.R;
import com.kuaifa.android.bean.ComBean;
import com.kuaifa.android.manager.VideoPlayManager;
import com.kuaifa.android.utils.SerialHelper;
import com.kuaifa.android.utils.SharedPreferencesUtil;
import com.kuaifa.android.utils.ToastUtils;

import java.io.IOException;
import java.security.InvalidParameterException;


public class DialogCommCountActivity extends AppCompatActivity implements View.OnClickListener{

    private int waterNumber = 0;
    private int waterTime = 0; //水量转换成毫秒---冷水和热水转换方式不同
    public SerialControl serial;//4个串口
    private MyThread thread;
    private MyHandler handler = new MyHandler();
    private ImageView stop;//停止按钮
    private ImageView pause;//暂停按钮
    private Context context;
    private static final String saveIndex = "SP_SAVE_INDEX";
    public boolean isPause= false;
    public int delay = 10;//延迟时间==10ms
    private TextView countdown;

    public class MyHandler extends Handler {
        @SuppressLint("SetTextI18n")
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what){
                case 10000://接水完成
                    notifyStop();
                    DialogCommCountActivity.this.finish();
                    break;
                case 10001:
                    updateReceivedData();//记录已经接水的时间
                    break;
                case 10002://暂停倒计时
                    countdown.setText("点击暂停10s后退出程序   " + msg.arg1);
                    break;
            }
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.dialog_comm_count_activity);
        initView();
        initData();
        writeSer();
    }

    public void initView(){
        setFinishOnTouchOutside(false);
        context = DialogCommCountActivity.this;
        stop = findViewById(R.id.stop);
        pause = findViewById(R.id.pause);
        countdown = findViewById(R.id.countdown);
        stop.setOnClickListener(this);
        pause.setOnClickListener(this);
    }

    public void initData(){
        try {
            String sd = getIntent().getStringExtra("waterNumber");
            double d = Double.parseDouble(sd);
            waterNumber = (int) (d * 1000);
            waterTime = waterNumber * 12;//冷水量转换成毫秒  1ml== 12ms
        } catch (Exception e) {
            e.printStackTrace();
            waterTime = 0;
        }
        initCom();
    }

    //初始化Com
    public void initCom(){
        serial = new SerialControl();
        serial.setPort("/dev/ttyS0");
        serial.setN81("8", "1", "'N'");
        serial.setBaudRate("9600");
        OpenComPort(serial);
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()){
            case R.id.stop:
                notifyStop();
                DialogCommCountActivity.this.finish();
                break;
            case R.id.pause:
                //暂停逻辑
                /**
                 * 1.首先点击暂停，调用停止方法
                 * 2，将暂停时间-开始时间，拿到当前已经接水的时间
                 * 3，将接水的总时间-已经接水的时间，拿到还需要接水的时间长度
                 * 4，执行10s倒计时：倒计时时间内，点击接水按钮打开继电器开始接水。倒计时结束，关闭接水页面。
                 */

                isPause = !isPause;
                if (isPause){//暂停
                    pause.setImageResource(R.mipmap.img_start);
                    if (thread != null) {
                        thread.pauseThread();
                    }
                    Log.d("----------->","pause--->waterTime="+waterTime);
                    notifyStop();//停止
                    SharedPreferencesUtil.getInstance(this).putSP(saveIndex,index+"");
                }else {
                    pause.setImageResource(R.mipmap.img_onpause);
                    if (thread !=null) {
                        thread.resumeThread();
                    }
                    Log.d("----------->","restart--->waterTime="+waterTime);
                    index = Integer.parseInt(SharedPreferencesUtil.getInstance(getApplicationContext()).getSP(saveIndex));
                    notifyStart();
                }
                break;
        }
    }

    private void OpenComPort(SerialHelper ComPort) {
        try {
            ComPort.open();
        } catch (SecurityException e) {
            ToastUtils.showMessage(context,"打开串口失败:没有串口读/写权限!");
        } catch (IOException e) {
            ToastUtils.showMessage(context,"打开串口失败:未知错误!");
        } catch (InvalidParameterException e) {
            ToastUtils.showMessage(context,"打开串口失败:参数错误!");
        }
    }

    private void writeSer() {
        if(waterNumber <=0){
            Toast.makeText(this, "请设置取水量", Toast.LENGTH_SHORT).show();
            DialogCommCountActivity.this.finish();
            return;
        }
        thread = new MyThread();
        thread.start();
    }

    /**
     * 计算循环的次数
     * 如果500ms循环一次，
     *      index * 500 >= waterTime 则接水结束
     *      index * 500 <  waterTime 则继续接水
     */
    private int index = 0;

    /**
     * 需要在子线程中调用
     */
    private void updateReceivedData() {
        if (waterTime == 0 ) {
            Toast.makeText(getApplicationContext(), "接水时间为0ms", Toast.LENGTH_SHORT).show();
            Log.e("----------------","接水时间为0ms");
            thread.interrupt();
        }
        if (index == 0){ //第一次进入，打开接水
            notifyStart();
        }
        index = index + 1;
        if (index * delay >= waterTime ) { //接水时间用完
            Message message = Message.obtain();
            message.what = 10000;
            handler.sendMessage(message);
        }
        VideoPlayManager.eliminateEvent();//循环中设置屏保不显示
    }

    @Override
    public void onDestroy() {
        waterNumber=0;
        if (serial != null) {
            try {
                if (serial.isOpen()) {
                    serial.sendHex("AFFF0102DF");//继电器1 断开
                    serial.sendHex("AFFF0202DF");//继电器2 断开
                } else {
                    OpenComPort(serial);
                    serial.sendHex("AFFF0102DF");//继电器1 断开
                    serial.sendHex("AFFF0202DF");//继电器2 断开
                }
            }catch (Exception e){
                e.printStackTrace();
            }
            serial.stopSend();
            serial.close();
        }
        if (thread != null) {
            thread.interrupt();
            thread = null;
        }

        if (handler != null){
            handler.removeCallbacksAndMessages(null);
        }
        super.onDestroy();
    }

    //自定义子线程
    class MyThread extends Thread {
        private final Object lock = new Object();
        private boolean pause = false;

        /**
         * 调用这个方法实现暂停线程
         */
        void pauseThread() {
            pause = true;
        }

        /**
         * 调用这个方法实现恢复线程的运行
         */
        void resumeThread() {
            pause = false;
            synchronized (lock) {
                lock.notifyAll();
            }
        }

        /**
         * 注意：这个方法只能在run方法里调用，不然会阻塞主线程，导致页面无响应
         */
        void onPause() {
            synchronized (lock) {
                try {
                    lock.wait();
                    Log.d("------------","lock.wait()");
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }

        @Override
        public void run() {
            int pauseIndex = 0;//记录倒计时
            while (!isInterrupted()) {
                while (pause) {
                    VideoPlayManager.eliminateEvent();
//                    onPause();

                    //点击暂停后，进入内层循环
                    try {
                        sleep(1000);
                        pauseIndex = pauseIndex + 1;
                        Log.d("------pauseIndex>>>>","pauseIndex="+pauseIndex);
                        int countDown = 10 - pauseIndex;
                        if (countDown<=0){ //倒计时结束
                            handler.sendEmptyMessage(10000);
                            pauseIndex = 0;
                            onPause();

                            return;
                        }

                        Message message = Message.obtain();
                        message.arg1 = countDown;
                        message.what = 10002;
                        Log.d("------------countdown","countdown = "+countDown);
                        handler.sendMessage(message);

                    } catch (InterruptedException e) {
                        break;
                    }
                }
                synchronized (this) {
                    try {
                        pauseIndex = 0;
                        if(index * delay < waterTime){ //轮循器所用的时间 < 需要接水的时间，说明接水还未完成
                            Log.d("--------------restart","用掉时间="+index*delay+"-------------waterTime="+waterTime);
                            sleep(delay);
                            Message msg = Message.obtain();
                            msg.what = 10001;
                            handler.sendMessage(msg);
                        }else {
                            handler.sendEmptyMessage(10000);
                            Thread.interrupted();
                        }
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                        Thread.interrupted();
                        if ( !pause ) {//非暂停情况下
                            break;
                        }
                    }
                }
            }
        }
    }

    //串口读取数据
    class SerialControl extends SerialHelper {
        @Override
        protected void onDataReceived(ComBean ComRecData) {
            Log.d("------------str",ComRecData.bRec.length+"");
        }
    }

    /**
     * 关闭继电器，停止出水
     */
    public void notifyStop(){
        if( null== serial ){
            serial = new SerialControl();
            serial.setPort("/dev/ttyS0");
            serial.setN81("8", "1", "'N'");
            serial.setBaudRate("9600");
            OpenComPort(serial);
        }else if(!serial.isOpen()) {
            OpenComPort(serial);
        }

        serial.sendHex("AFFF0202DF");//继电器 断开
        try {
            waterNumber = 0;
            thread.interrupt();
            serial.close();
            serial = null;
        } catch (Exception e){
            serial.sendHex("AFFF0202DF");//继电器 断开
        }
    }

    //连接继电器，还是出水
    public void notifyStart(){
        if( null== serial ){
            serial = new SerialControl();
            serial.setPort("/dev/ttyS0");
            serial.setN81("8", "1", "'N'");
            serial.setBaudRate("9600");
            OpenComPort(serial);
        }else if(!serial.isOpen()) {
            OpenComPort(serial);
        }
        serial.sendHex("AFFF0101DF");//继电器 打开
    }

}

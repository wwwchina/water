package com.kuaifa.android.zoowaterui.activity;

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


public class DialogZooCountActivity extends AppCompatActivity implements View.OnClickListener{
    /**
     * 二路串口指令“AF FF EC 01 DF”
     * AF（12）：开头
     * FF（34）：地址值，可随意设定，范围00-FF
     * EC（56）：波特率值，(0X01：波特率=4800bps)(0X02：波特率=9600bps)(0X03：波特率=19200bps)(0X04：波特率=38400bps)(其他值：波特率=9600bps)(默认值：0X02->波特率=9600bp)
     * 01（78）：
     */
    private int waterNumber = 0;
    private int waterTime = 0; //水量转换成毫秒---冷水和热水转换方式不同
    private int type = 1; //1 冷水 2热水
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
    private ImageView coolwater;
    private ImageView hotwater;

    public class MyHandler extends Handler {
        @SuppressLint("SetTextI18n")
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what){
                case 10000://接水完成
                    notifyStop();
                    DialogZooCountActivity.this.finish();
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
        setContentView(R.layout.dialog_zoo_count_activity);
        initView();
        initData();
        writeSer();
    }

    public void initView(){
        setFinishOnTouchOutside(false);
        context = DialogZooCountActivity.this;
        stop = findViewById(R.id.stop);
        pause = findViewById(R.id.pause);
        countdown = findViewById(R.id.countdown);

        coolwater = findViewById(R.id.coolwater);
        hotwater = findViewById(R.id.hotwater);

        coolwater.setOnClickListener(this);
        hotwater.setOnClickListener(this);
        stop.setOnClickListener(this);
        pause.setOnClickListener(this);
    }

    public void initData(){
        try {
            String sd = getIntent().getStringExtra("waterNumber");
            double d = Double.parseDouble(sd);
            waterNumber = (int) (d * 1000);
            type = getIntent().getIntExtra("type",1);
            if (type ==1){
                //冷水
                coolwater.setImageResource(R.mipmap.img_coolwater_checked);
                hotwater.setImageResource(R.mipmap.img_hotwater_unchecked);
            }else {
                //热水
                coolwater.setImageResource(R.mipmap.img_coolwater_unchecked);
                hotwater.setImageResource(R.mipmap.img_hotwater_checked);
            }
            waterToTime(type);
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
            case R.id.coolwater://切换冷水
                ToastUtils.showMessage(getApplicationContext(),"切换冷水");
                if (type==2){
                    type = 1;
                    coolwater.setImageResource(R.mipmap.img_coolwater_checked);
                    hotwater.setImageResource(R.mipmap.img_hotwater_unchecked);
                }
                break;
            case R.id.hotwater://切换热水
                ToastUtils.showMessage(getApplicationContext(),"切换热水");
                if (type == 1){
                    type = 2;
                    hotwater.setImageResource(R.mipmap.img_hotwater_checked);
                    coolwater.setImageResource(R.mipmap.img_coolwater_unchecked);
                }
                break;
            case R.id.stop:
                notifyStop();
                DialogZooCountActivity.this.finish();
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
                    coolwater.setVisibility(View.VISIBLE);
                    hotwater.setVisibility(View.VISIBLE);
                    pause.setImageResource(R.mipmap.img_start);
                    if (thread != null) {
                        thread.pauseThread();
                    }
                    Log.d("----------->","pause--->waterTime="+waterTime);
                    notifyStop();//停止
                    SharedPreferencesUtil.getInstance(this).putSP(saveIndex,index+"");
                }else {
                    coolwater.setVisibility(View.INVISIBLE);
                    hotwater.setVisibility(View.INVISIBLE);
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
            DialogZooCountActivity.this.finish();
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
        if (type ==1) {
            serial.sendHex("AFFF0102DF");//继电器 1 断开
            Log.d("----------","notifyStop-----继电器1断开");
        }else {
            serial.sendHex("AFFF0202DF");//继电器 2 断开
            Log.d("----------","notifyStop-----继电器2断开");
        }
        try {
            waterNumber = 0;
            thread.interrupt();
            serial.close();
            serial = null;
        } catch (Exception e){
            if (type==1) {
                serial.sendHex("AFFF0102DF");//继电器1 断开
            }else {
                serial.sendHex("AFFF0202DF");//继电器2 断开
            }
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

        if (type ==1) { //type ==1 冷水    type==2 热水
            serial.sendHex("AFFF0101DF");//继电器1 打开
            Log.d("---------->","notifyStart-----继电器1 冷水打开");
        }else {
            serial.sendHex("AFFF0201DF");//继电器2 打开
            Log.d("---------->","notifyStart-----继电器2 热水打开");
        }
//        serial.sendHex("FDFDFD");//TDS指令
    }

    /**
     * 接水量转换成时间
     * 因为水压原因，冷水和热水的水流量不同，造成接水量存在偏差
     * 此处分别判断热水和冷水实际流量
     * type==1 冷水   type==2热水
     */
    public void waterToTime(int type){
        if (type == 1) {
            waterTime = waterNumber * 25;//冷水量转换成毫秒  1ml== 25ms
        }else {
            waterTime = waterNumber * 20;//热水量转换成毫秒  1ml== 25ms
        }
    }

}

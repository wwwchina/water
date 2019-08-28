package com.kuaifa.android.ui.activity;

import android.annotation.SuppressLint;
import android.content.Context;
import android.hardware.usb.UsbDeviceConnection;
import android.hardware.usb.UsbManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import com.kuaifa.android.R;
import com.kuaifa.android.bean.ComBean;
import com.kuaifa.android.driver.UsbSerialDriver;
import com.kuaifa.android.driver.UsbSerialPort;
import com.kuaifa.android.driver.UsbSerialProber;
import com.kuaifa.android.manager.VideoPlayManager;
import com.kuaifa.android.utils.HexDump;
import com.kuaifa.android.utils.SerialHelper;
import com.kuaifa.android.utils.SerialInputOutputManager;
import com.kuaifa.android.utils.ToastUtils;

import java.io.IOException;
import java.lang.ref.WeakReference;
import java.security.InvalidParameterException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class DialogActivity extends AppCompatActivity implements View.OnClickListener{

private UsbManager mUsbManager;
    private int oldNumber = -1;//旧水量获取不到，-1
    private int waterNumber = 0;//传过来的接水量
    public UsbSerialPort sPort = null;
    public SerialControl ComA;//4个串口
    private Thread thread;
    private ExecutorService mExecutor = Executors.newSingleThreadExecutor();
    private SerialInputOutputManager mSerialIoManager;
    private MyHandler ttsHandler = new MyHandler(this);
    private TextView stop;
    private Context context;

    public class MyHandler extends Handler {
        private WeakReference<DialogActivity> mActivity;

        MyHandler(DialogActivity activity) {
            mActivity = new WeakReference<>(activity);
        }

        @Override
        public void handleMessage(Message msg) {
            DialogActivity theActivity = mActivity.get();
            theActivity.sendHex();
        }
    }

    /**
     * 串口输入输出管理器 回调监听接口
     */
    private final SerialInputOutputManager.Listener mListener = new SerialInputOutputManager.Listener() {

        @Override
        public void onRunError(Exception e) {
            notifyStop();
        }

        @Override
        public void onNewData(final byte[] data) {
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    //UI线程中更新水流量信息
                    DialogActivity.this.updateReceivedData(data);
                }
            });
        }
    };

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.dialog_activity);
        initView();
        initData();
        initSerial();

        ComA = new SerialControl();
        ComA.setPort("/dev/ttyS0");
        ComA.setN81("8", "1", "'N'");
        ComA.setBaudRate("9600");
        OpenComPort(ComA);
        //链接串口
        connectionSerial();
        //写入取水码
        writeSer();
    }

    public void initView(){
        setFinishOnTouchOutside(false);
        context = DialogActivity.this;
        stop = findViewById(R.id.stop);//关闭接水按钮
        stop.setOnClickListener(this);
    }

    public void initData(){

        //获取水量 L转换成mL
        try {
            String sd = getIntent().getStringExtra("waterNumber");
            double d = Double.parseDouble(sd);
            waterNumber = (int) (d * 1000);
        } catch (Exception e) {
            e.printStackTrace();
            waterNumber = 0;
        }
    }

    //初始化串口
    public void initSerial(){
        mUsbManager = (UsbManager) getSystemService(Context.USB_SERVICE);
        final List<UsbSerialDriver> drivers = UsbSerialProber.getDefaultProber().findAllDrivers(mUsbManager);//获取所有串口驱动
        final List<UsbSerialPort> result = new ArrayList<>();
        for (final UsbSerialDriver driver : drivers) {//遍历所有驱动，获取串口添加到集合中
            final List<UsbSerialPort> ports = driver.getPorts();
            result.addAll(ports);
            sPort = result.get(0);

        }
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()){
            case R.id.stop:
                notifyStop();
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

    //链接串口
    private void connectionSerial() {
        if (sPort == null) {
            VideoPlayManager.eliminateEvent();
            Toast.makeText(this, "串口为空，不能使用，请检查设备", Toast.LENGTH_LONG).show();
            return;
        }
        final UsbManager usbManager = (UsbManager) getSystemService(Context.USB_SERVICE);
        UsbDeviceConnection connection = usbManager.openDevice(sPort.getDriver().getDevice());
        if (connection == null) {
            Toast.makeText(this, "打开设备失败，请检查设备", Toast.LENGTH_LONG).show();
            return;
        }
        try {
            sPort.open(connection);
            sPort.setParameters(4800, UsbSerialPort.DATABITS_8, UsbSerialPort.STOPBITS_1, UsbSerialPort.PARITY_NONE);
        } catch (Exception e) {
            Toast.makeText(this, "设置串口失败", Toast.LENGTH_LONG).show();
            try {
                sPort.close();
            } catch (IOException e2) {
                // Ignore.
            }
            sPort = null;
            return;
        }
        onDeviceStateChange();
    }

    private void onDeviceStateChange() {
        stopIoManager();
        startIoManager();
    }

    private void writeSer() {
        if(waterNumber <=0){
            Toast.makeText(this, "请设置取水量", Toast.LENGTH_SHORT).show();
            DialogActivity.this.finish();
        }
        thread =  new MyThread();
        thread.start();
    }

    private void sendHex() {
        String s = "010300080004";
        String strs = getCRC2(hexStringToBytes(s));
        try {
            if (sPort == null) {
                mUsbManager = (UsbManager) getSystemService(Context.USB_SERVICE);
                final List<UsbSerialDriver> drivers = UsbSerialProber.getDefaultProber().findAllDrivers(mUsbManager);
                final List<UsbSerialPort> result = new ArrayList<UsbSerialPort>();
                for (final UsbSerialDriver driver : drivers) {
                    final List<UsbSerialPort> ports = driver.getPorts();
                    result.addAll(ports);
                    sPort = result.get(0);
                }
                connectionSerial();
            }
            sPort.write(hexStringToBytes(s + strs), 50);
            Log.i("lbc", "--11---" + hexStringToBytes(s + strs));

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static byte[] date_d = new byte[13];
    private static int flag = 0;

    @SuppressLint("SetTextI18n")
    private void updateReceivedData(byte[] data) {
        byte[] b = data;
        if (null == b) {
            return;
        }
        final String message;
        if (b.length != 13) {
            date_d[flag]=b[0];
            flag++;
            if( flag >0&&date_d[0] !=1){
                flag =0;
                return;
            }
            if(flag >1 && date_d[1] !=3){
                flag = 0;
                return;
            }
            if(flag >2 && date_d[2] !=8){
                flag = 0;
                return;
            }
            if(flag >12){
                b = new byte[13];
                System.arraycopy(date_d, 0, b, 0, 13);
                flag = 0;
            }

        }
        message = HexDump.toHexString(b);
        Log.d("message-----------", message);
        if (waterNumber == 0) {
            return;
        }
        int newNumber = 0;
        if (oldNumber == -1) {
            oldNumber = getWaterNumber(message);
            if(oldNumber ==0){
                oldNumber =-1;
                return;
            }
            ToastUtils.showMessage(context,"开始放水" + oldNumber);
            if( null== ComA ){
                ComA = new SerialControl();
                ComA.setPort("/dev/ttyS0");
                ComA.setN81("8", "1", "'N'");
                ComA.setBaudRate("9600");
                OpenComPort(ComA);
            }else if(!ComA.isOpen()) {
                OpenComPort(ComA);
            }
            ComA.sendHex("afff0101df");
        } else {
            newNumber = getWaterNumber(message);
        }
        int flag = newNumber - oldNumber;//实际的接水量 = 新的总出水量 - 旧的总出水量
        if (flag >= waterNumber) {  //waterNumber:此次待接的出水量
            ToastUtils.showMessage(context,"关闭");
            notifyStop();
        }

        VideoPlayManager.eliminateEvent();

    }

    private int getWaterNumber(String str) {
        String s1 = splitStr(str, 0, 22);
        if (s1 == null) {
            return 0;
        }
        String s2 = splitStr(str, 22, 26);
        String c = getCRC2(hexStringToBytes(s1));
        if (!s2.equals(c)) {
            return 0;
        }

        String low = splitStr(str, 6, 14);
        String hight = splitStr(str, 14, 22);
        int hightL = Integer.parseInt(hight, 16);//16进制转换为10进制
        int lowL = Integer.parseInt(low, 16);
        double dd = hightL * 1000000 + lowL / 1000d;
        Log.d("hightL--------",hightL+"");
        Log.d("lowL--------",lowL+"");
        return (int) (dd * 1000);
    }

    private String splitStr(String str, int start, int end) {
        try {
            if (str.length() != 26) {
                return null;
            }
            return str.substring(start, end);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    //crc校验码
    public String getCRC2(byte[] bytes) {
        int CRC = 0x0000ffff;
        int POLYNOMIAL = 0x0000a001;
        int i, j;
        for (i = 0; i < bytes.length; i++) {
            CRC ^= (int) bytes[i] & 0x000000ff;
            for (j = 0; j < 8; j++) {
                if ((CRC & 0x00000001) == 1) {
                    CRC >>= 1;
                    CRC ^= POLYNOMIAL;
                } else {
                    CRC >>= 1;
                }
            }
        }
        //高低位转换，看情况使用（譬如本人这次对led彩屏的通讯开发就规定校验码高位在前低位在后，也就不需要转换高低位)
        CRC = ((CRC & 0x0000FF00) >> 8) | ((CRC & 0x000000FF) << 8);
        return String.format("%04X", CRC);
    }

    @Override
    public void onDestroy() {
        waterNumber=0;
        if (ComA != null) {
            try {
                if (ComA.isOpen()) {
                    ComA.sendHex("afff0202df");
                } else {
                    OpenComPort(ComA);
                    ComA.sendHex("afff0202df");
                }
                ComA.sendHex("afff0202df");
            }catch (Exception e){
                ComA.sendHex("afff0202df");
            }
            ComA.stopSend();
            ComA.close();
        }
        if (sPort != null) {
            try {
                sPort.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
            sPort = null;
        }
        if (thread != null) {
            thread.interrupt();
            thread = null;
        }
        if(ttsHandler != null){
            ttsHandler.removeCallbacksAndMessages(null);
            ttsHandler = null;
        }
        super.onDestroy();
    }


    class MyThread extends Thread {
        @Override
        public void run() {
            while (!isInterrupted()) {
                synchronized (this) {
                    try {
                        if(waterNumber !=0){
                            sleep(500);//每隔500毫秒执行一次
                            Message msg = Message.obtain();
                            msg.what = 1;
                            ttsHandler.sendMessage(msg);
                        }
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                        interrupted();
                    }
                }
            }
        }
    }

    private void stopIoManager() {
        if (mSerialIoManager != null) {
            mSerialIoManager.stop();
            mSerialIoManager = null;
        }
    }

    private void startIoManager() {
        if (sPort != null) {
            mSerialIoManager = new SerialInputOutputManager(sPort, mListener);
            mExecutor.submit(mSerialIoManager);
        }
    }


    class SerialControl extends SerialHelper {
        public SerialControl() {
        }
        @Override
        protected void onDataReceived( ComBean ComRecData) {
        }
    }

    //16位转bytes
    public static byte[] hexStringToBytes(String hexString) {
        if (hexString == null || hexString.equals("")) {
            return null;
        }
        hexString = hexString.toUpperCase();
        int length = hexString.length() / 2;
        char[] hexChars = hexString.toCharArray();
        byte[] d = new byte[length];
        for (int i = 0; i < length; i++) {
            int pos = i * 2;
            d[i] = (byte) (charToByte(hexChars[pos]) << 4 | charToByte(hexChars[pos + 1]));
        }
        return d;
    }

    public static byte charToByte(char c) {
        return (byte) "0123456789ABCDEF".indexOf(c);
    }


    /**
     * 停止出水
     */
    public void notifyStop(){
        if( null== ComA ){
            ComA = new SerialControl();
            ComA.setPort("/dev/ttyS0");
            ComA.setN81("8", "1", "'N'");
            ComA.setBaudRate("9600");
            OpenComPort(ComA);
        }else if(!ComA.isOpen()) {
            OpenComPort(ComA);
        }
        ComA.sendHex("afff0202df");
        try {
            waterNumber = 0;
            thread.interrupt();
            thread=null;
            ComA.close();
            ComA = null;
            sPort.close();
            sPort = null;
            mUsbManager=null;
        } catch (IOException e) {
            e.printStackTrace();
        }catch (Exception e){
            if( null== ComA ){
                ComA = new SerialControl();
                ComA.setPort("/dev/ttyS0");
                ComA.setN81("8", "1", "'N'");
                ComA.setBaudRate("9600");
                OpenComPort(ComA);
            }else if(!ComA.isOpen()) {
                OpenComPort(ComA);
            }
            ComA.sendHex("afff0202df");
        }
        DialogActivity.this.finish();
    }

}

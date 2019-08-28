package com.kuaifa.android.utils;


import android.util.Log;

import com.kuaifa.android.bean.ComBean;
import com.kuaifa.android.serialport.SerialPort;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.security.InvalidParameterException;

/**
 * @author benjaminwan  串口辅助工具类
 */
public abstract class SerialHelper {
    private SerialPort mSerialPort;
    public OutputStream mOutputStream;
    public InputStream mInputStream;
    private ReadThread mReadThread;
    private SendThread mSendThread;
    private String sPort = "/dev/ttyS0";
    private int iBaudRate = 9600;
    private boolean _isOpen = false;
    private byte[] _bLoopData = new byte[]{0x30};
    private int iDelay = 500;

    //默认 N-8-1
    private char parity = 'N';//char校验类型 取值N ,E, O,,S
    private int dataBits = 8;//dataBits 类型 int数据位 取值 位7或8
    private int stopBits = 1;//stopBits 类型 int 停止位 取值1 或者 2

    /**
     *设定校验位等
     */
    public void setN81(String idataBits, String istopBits, String cparity) {
        this.parity =cparity.charAt(0);
        this.dataBits = Integer.parseInt(idataBits);
        this.stopBits =  Integer.parseInt(istopBits);
    }

    public SerialHelper(String sPort, int iBaudRate) {
        this.sPort = sPort;
        this.iBaudRate = iBaudRate;
    }

    public SerialHelper() {
        this("/dev/s3c2410_serial0", 9600);
    }

    public void open() throws SecurityException, IOException, InvalidParameterException {
        File device = new File(sPort);
        //检查访问权限，如果没有读写权限，进行文件操作，修改文件访问权限
        if (!device.canRead() || !device.canWrite()) {
            try {
                //通过挂在到linux的方式，修改文件的操作权限
                Process su = Runtime.getRuntime().exec("/system/bin/su");
                //一般的都是/system/bin/su路径，有的也是/system/xbin/su
                String cmd = "chmod 777 " + device.getAbsolutePath() + "\n" + "exit\n";
                su.getOutputStream().write(cmd.getBytes());

                if ((su.waitFor() != 0) || !device.canRead() || !device.canWrite()) {
                    throw new SecurityException();
                }
            } catch (Exception e) {
                e.printStackTrace();
                throw new SecurityException();
            }
        }

        Log.e("before open--->",sPort+iBaudRate+dataBits+stopBits+parity);
        mSerialPort =new SerialPort(new File(sPort), iBaudRate, dataBits, stopBits, parity);
        mOutputStream = mSerialPort.getOutputStream();
        mInputStream = mSerialPort.getInputStream();
        mReadThread = new ReadThread();
        mReadThread.start();
        mSendThread = new SendThread();
        mSendThread.setSuspendFlag();
        mSendThread.start();
        _isOpen = true;
    }

    public void close() {
        if (mReadThread != null) {
            mReadThread.interrupt();
            mReadThread =null;
        }
        if(mSendThread!=null){
            mSendThread.interrupt();
            mSendThread = null;
        }
        if (mSerialPort != null) {
            mSerialPort.close();
            mSerialPort = null;
        }
        if(mOutputStream!= null){
            try {
                mOutputStream.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
            mOutputStream=null;
        }
        if(mInputStream != null) {
            try {
                mInputStream.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        _isOpen = false;
    }

    public void send(byte[] bOutArray) {
        try {
            if(mOutputStream!=null) {
                mOutputStream.write(bOutArray);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void sendHex(String sHex) {
        byte[] bOutArray = MyFunc.HexToByteArr(sHex);
        send(bOutArray);
    }

    private class ReadThread extends Thread {
        @Override
        public void run() {
            super.run();
            while (!isInterrupted()) {
                synchronized (this) {
                    try {
                        if (mInputStream == null) return;
                        byte[] buffer = new byte[512];
                        int size = mInputStream.read(buffer);
                        if (size > 0) {
                            Log.d("--------------byte-size",size+"");
                            ComBean ComRecData = new ComBean(sPort, buffer, size);
                            onDataReceived(ComRecData);
//                            byte[] bRec = new byte[size];
//                            for (int i = 0; i < size; i++) {
//                                bRec[i]=buffer[i];
//                            }
//                            String s = MyFunc.ByteArrToHex(bRec);
//                            onDataReceived(s);
//                            Log.d("-----------------byte",s);
                        }
                        try {
                            sleep(300);//延时50ms
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                            interrupted();
                        }
                    } catch (Throwable e) {
                        e.printStackTrace();
                        return;
                    }
                }
            }
        }
    }

    private class SendThread extends Thread {
        public boolean suspendFlag = true;// 控制线程的执行

        @Override
        public void run() {
            super.run();
            while (!isInterrupted()) {
                synchronized (this) {
                    while (suspendFlag) {
                        try {
                            wait();
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                            interrupted();
                        }
                    }
                }
                send(getbLoopData());
                try {
                    sleep(iDelay);//设置计时器间隔时间500ms
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }

        //线程暂停
        public void setSuspendFlag() {
            this.suspendFlag = true;
        }

        //唤醒线程
        public synchronized void setResume() {
            this.suspendFlag = false;
            notify();
        }
    }

    public boolean setBaudRate(int iBaud) {
        if (_isOpen) {
            return false;
        } else {
            iBaudRate = iBaud;
            return true;
        }
    }

    public boolean setBaudRate(String sBaud) {
        int iBaud = Integer.parseInt(sBaud);
        return setBaudRate(iBaud);
    }

    public boolean setPort(String sPort) {
        if (_isOpen) {
            return false;
        } else {
            this.sPort = sPort;
            return true;
        }
    }

    public boolean isOpen() {
        return _isOpen;
    }

    public byte[] getbLoopData() {
        return _bLoopData;
    }

    public void stopSend() {
        if (mSendThread != null) {
            mSendThread.setSuspendFlag();
        }
    }

    protected abstract void onDataReceived(ComBean ComRecData);
//    protected abstract void onDataReceived(String ComRecData);


    public static String Bytes2HexString(byte[] b, int size){
//size==b.length,toUpperCase()将字符串小写字符转换为大写,toLowerCase()将字符串大写字符转换为小写
        String ret = "";
        for (int i = 0; i < size; i++) {
            String hex = Integer.toHexString(b[i] & 0xFF);
            if (hex.length() == 1) {
                hex = "0" + hex;
            }
            ret += hex.toUpperCase();
        }
        return ret;
    }

}
package com.kuaifa.android.ui.manager;

import android.os.SystemClock;
import android.text.TextUtils;
import android.util.Log;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;

/**
 * 读串口线程
 */
public class SerialReadThread extends Thread {

    private static final String TAG = "SerialReadThread";

    private BufferedInputStream mInputStream;

    private int shuizhi=0;

    public SerialReadThread(InputStream is) {
        mInputStream = new BufferedInputStream(is);
    }

    public int getShuizhi(){
        return shuizhi;
    }

    @Override
    public void run() {
        byte[] received = new byte[5];
        int size;

        while (true) {

            if (Thread.currentThread().isInterrupted()) {
                break;
            }
            try {

                int available = mInputStream.available();

                if (available > 0) {
                    size = mInputStream.read(received);
                    if (size > 0) {
                        int sz = received[1]+received[2]*256;
                        String mes = "shuizhi:"+sz+" size:"+size;
                        shuizhi= sz;
                        Log.e(TAG, mes);
                        onDataReceive(received, size);

                    }
                } else {
                    // 暂停一点时间，免得一直循环造成CPU占用率过高
                    SystemClock.sleep(1);
                }
            } catch (IOException e) {
            }
            //Thread.yield();
        }

    }

    /**
     * 处理获取到的数据
     *
     * @param received
     * @param size
     */
    private void onDataReceive(byte[] received, int size) {
        String hexStr = ByteUtil.bytes2HexStr(received, 0, size);
        int sz = received[1]+received[2]*256;
        Log.e("water>>>>>>>>>>>>>>>>>>","hexStr="+hexStr+" shuizhi:" +sz+" size:"+size);

    }

    /**
     * 停止读线程
     */
    public void close() {

        try {
            mInputStream.close();
        } catch (IOException e) {
        } finally {
            super.interrupt();
        }
    }
}

package com.kuaifa.android.ui.activity;

import android.Manifest;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Handler;
import androidx.core.app.ActivityCompat;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentTransaction;
import android.view.MotionEvent;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.kuaifa.android.R;
import com.kuaifa.android.listener.PreventFastOnClickListener;
import com.kuaifa.android.manager.VideoPlayManager;
import com.kuaifa.android.ui.Constant;
import com.kuaifa.android.ui.fragment.StaffFragment;
import com.kuaifa.android.ui.fragment.RechargeFragment;
import com.kuaifa.android.ui.fragment.MainFragment;
import com.kuaifa.android.ui.fragment.PersonalFragment;
import com.kuaifa.android.ui.manager.Device;
import com.kuaifa.android.ui.manager.SerialPortManager;
import com.kuaifa.android.utils.LoadingDialog;
import com.kuaifa.android.utils.PlayListener;

import java.io.File;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import tv.kuaifa.superplayer.business.PlayerEngine;
import tv.kuaifa.superplayer.config.PlayerEngineOptions;
import tv.kuaifa.superplayer.util.CommonHelper;

public class MainActivity extends BaseActivity implements PlayListener{
//    public static ExecutorService fixedThreadPool = Executors.newFixedThreadPool(3);
    public static ExecutorService fixedThreadPool = Executors.newCachedThreadPool();
    private RechargeFragment companyRechargeFragment;
    private PersonalFragment personalFragment;
    private StaffFragment companyFragment;
    private MainFragment mainFragment;
    private Fragment mFragment;
    private TextView personal, number, company, guide;
    private LinearLayout video;
    private PlayerEngine playerEngine;
    public static final int START_TYPE_REAL_MAC = 0;
    public static final int START_TYPE_REAL_SN = 1;
    public static final int START_TYPE_VIRTUAL_MAC = 2;
    public static final int START_TYPE_VIRTUAL_SN = 3;
    int strStartType = START_TYPE_REAL_MAC;
    String strAppId, strAppKey, strVirtualSN, strVirtualMac;

    @Override
    public int getLayout() {
        return R.layout.activity_main;
    }

    /**
     *  购水接口新加上 mac = MacUtil.getMac(MyApplication.getInstance());
     */
    @Override
    public void initView(){
        video = findViewById(R.id.video);//屏保
        personal =  findViewById(R.id.radio_button0);//个人购水
        number = findViewById(R.id.radio_button1);//员工接水
        company = findViewById(R.id.radio_button2);//公司充值
        guide = findViewById(R.id.radio_button4);//直饮水引导
        personal.setOnClickListener(new GoodsDetailPreventFastClick());
        number.setOnClickListener(new GoodsDetailPreventFastClick());
        company.setOnClickListener(new GoodsDetailPreventFastClick());
        guide.setOnClickListener(new GoodsDetailPreventFastClick());
        VideoPlayManager.setPlayListener(this);
        setTabIndex(3);

        //TDS打开串口
        Device mDevice = new Device("/dev/ttyS1","9600");
        SerialPortManager.instance().open(mDevice);

    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        //SerialPortManager.instance().close();
        if (playerEngine != null) {
            playerEngine.stopPlay();
            playerEngine = null;
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        //TDS串口发送指令
        SerialPortManager.instance().sendCommand("FDFDFD");
    }

    @Override
    public void initData() {
        CommonHelper.checkWorkDir();//创建目录
        //checkWatchApp();//安装守护程序
        initPlayerEngine();
    }


    @Override
    public void onPlay() {
        if (playerEngine != null){
            playerEngine.startPlay();
            video.setVisibility(View.VISIBLE);
        }else {
            initPlayerEngine();
        }
    }

    //初始化播放器
    public void initPlayerEngine(){
        PlayerEngineOptions.Builder builder = new PlayerEngineOptions.Builder();
        builder.setControlScreenBrightness(false);//设置ssp不控制亮度
        builder.setControlVolume(false); //设置ssp不控制音量
        if (playerEngine == null) {
            switch (strStartType) {
                case START_TYPE_VIRTUAL_MAC:
                    builder.setId(strAppId, strAppKey, strVirtualMac);
                    break;
                case START_TYPE_VIRTUAL_SN:
                    //替换默认背景
                    builder.setDefaultBgPath("file:///android_asset/port.png", "file:///android_asset/land.png");
                    builder.setId(strAppId, strAppKey, strVirtualSN);
                    break;
                case START_TYPE_REAL_SN:
                    if (ActivityCompat.checkSelfPermission(this, Manifest.permission.READ_PHONE_STATE) != PackageManager.PERMISSION_GRANTED) {
                        return;
                    }
                    if (Build.VERSION.SDK_INT >= 26) {
                        builder.setId(strAppId, strAppKey, Build.getSerial());
                    } else {
                        builder.setId(strAppId, strAppKey, Build.SERIAL);
                    }

                    break;
                case START_TYPE_REAL_MAC:
                default:
                    break;
            }
            playerEngine = new PlayerEngine(this, video, builder.build());
        }

        playerEngine.startPlay();


    }

    private class GoodsDetailPreventFastClick extends PreventFastOnClickListener {
        @Override
        public void OnClickLis(View v) {
            if (v.getId() == personal.getId()) {
                setTabIndex(0);
            } else if (v.getId() == number.getId()) {
                setTabIndex(1);
            } else if (v.getId() == company.getId()) {
                setTabIndex(2);
            }else if(v.getId() == guide.getId()){
                setTabIndex(3);
            }
        }
    }
    public void setTabIndex(int index) {
        if (index >= 0 && index < 4) {
            switch (index) {
                case 0:/**个人购水*/
                    personal.setSelected(true);
                    number.setSelected(false);
                    company.setSelected(false);
                    guide.setSelected(false);
                    break;
                case 1:/**员工接水*/
                    personal.setSelected(false);
                    number.setSelected(true);
                    company.setSelected(false);
                    guide.setSelected(false);
                    break;
                case 2:/**公司购水*/
                    personal.setSelected(false);
                    number.setSelected(false);
                    company.setSelected(true);
                    guide.setSelected(false);
                    break;
                case 3:/**直饮水引导*/
                    personal.setSelected(false);
                    number.setSelected(false);
                    company.setSelected(false);
                    guide.setSelected(true);
                    break;
            }
            onCheckedChanged(index);
        }
    }

    private void switchContent(Fragment from, Fragment to) {
        if (null == from) {
            mFragment = to;
            FragmentTransaction transaction = getSupportFragmentManager().beginTransaction();
            if (!to.isAdded()) { // 先判断是否被add过
                transaction.add(R.id.realContent, to).commitAllowingStateLoss(); // 隐藏当前的fragment，add下一个到Activity中
            } else {
                transaction.show(to).commitAllowingStateLoss(); // 隐藏当前的fragment，显示下一个
            }
        } else if (mFragment != to) {
            mFragment = to;
            FragmentTransaction transaction = getSupportFragmentManager().beginTransaction();
            if (!to.isAdded()) {
                transaction.hide(from).add(R.id.realContent, to).commitAllowingStateLoss();
            } else {
                transaction.hide(from).show(to).commitAllowingStateLoss();
            }
        }
    }

    public void onCheckedChanged(int i) {
        switch (i) {
            case 0:// 个人购水
                if (null == personalFragment) {
                    personalFragment = new PersonalFragment();
                }
                switchContent(mFragment, personalFragment);
                personalFragment.refresh();
                break;

            case 1:// 员工接水
                if (null == companyFragment) {
                    companyFragment = new StaffFragment();
                }
                switchContent(mFragment, companyFragment);
                companyFragment.refresh();
                break;
            case 2:// 公司充值
                if (null == companyRechargeFragment) {
                    companyRechargeFragment = new RechargeFragment();
                }
                switchContent(mFragment, companyRechargeFragment);
                break;
            case 3:// 直饮水指导
                if (null == mainFragment) {
                    mainFragment = new MainFragment();
                }
                switchContent(mFragment, mainFragment);
                break;
        }

    }

    @Override
    public boolean dispatchTouchEvent(MotionEvent ev) {
        if (ev.getAction() == MotionEvent.ACTION_UP) {
            VideoPlayManager.startMonitor();
            VideoPlayManager.eliminateEvent();//设置手指离开屏幕的时间
            video.setVisibility(View.GONE);
            if (playerEngine!= null) {
                playerEngine.stopPlay();
            }
        }
        return super.dispatchTouchEvent(ev);
    }

    /**
     * 守护程序安装
     */
    private void checkWatchApp() {
        // 如果APK目录下已经存在该文件，说明已经安装过了
        File file = new File(Constant.WORK_DIR_APK + "DrinkCommSuperService.apk");
        if (!file.exists()) {
            CommonHelper.installSystemApk(getApplicationContext(), "DrinkCommSuperService.apk", "DrinkCommSuperService.apk", true);
        }
    }


}

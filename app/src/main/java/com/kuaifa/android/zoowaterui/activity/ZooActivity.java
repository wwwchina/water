package com.kuaifa.android.zoowaterui.activity;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;

import androidx.annotation.Nullable;
import androidx.core.app.ActivityCompat;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import android.util.Log;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.Toast;

import com.kuaifa.android.MyApplication;
import com.kuaifa.android.R;
import com.kuaifa.android.manager.VideoPlayManager;
import com.kuaifa.android.ui.Constant;
import com.kuaifa.android.ui.activity.BaseActivity;
import com.kuaifa.android.utils.LoadingDialog;
import com.kuaifa.android.utils.PlayListener;
import com.kuaifa.android.zoowaterui.fragment.SettingZooFragment;
import com.kuaifa.android.zoowaterui.fragment.WaterZooFragment;

import java.io.File;

import tv.kuaifa.superplayer.business.PlayerEngine;
import tv.kuaifa.superplayer.config.PlayerEngineOptions;
import tv.kuaifa.superplayer.util.CommonHelper;

//动物园主Activity
public class ZooActivity extends BaseActivity implements View.OnClickListener, PlayListener {

    private FrameLayout container;
    public FragmentManager fragmentManager;
    private WaterZooFragment waterZooFragment;
    private SettingZooFragment settingZooFragment;
    private RelativeLayout button_bar;
    private ImageView cool_water;
    private ImageView hot_water;
    private ImageView help;
    private ImageView back;
    private LinearLayout video;

    private PlayerEngine playerEngine;
    public static final int START_TYPE_REAL_MAC = 0;
    public static final int START_TYPE_REAL_SN = 1;
    public static final int START_TYPE_VIRTUAL_MAC = 2;
    public static final int START_TYPE_VIRTUAL_SN = 3;
    private int strStartType = START_TYPE_REAL_MAC;
    private String strAppId, strAppKey, strVirtualSN, strVirtualMac;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        VideoPlayManager.startMonitor();
        initView();
        initData();
        checkWatchApp();
    }

    @Override
    public int getLayout() {
        return R.layout.activity_zoo;
    }

    //初始化view
    public void initView(){
        container = findViewById(R.id.container);
        button_bar = findViewById(R.id.button_bar);
        cool_water = findViewById(R.id.cool_water);
        hot_water = findViewById(R.id.hot_water);
        help = findViewById(R.id.help);
        back = findViewById(R.id.back);
        video = findViewById(R.id.video);
        cool_water.setOnClickListener(this);
        hot_water.setOnClickListener(this);
        help.setOnClickListener(this);
        back.setOnClickListener(this);
    }

    //初始化资源
    public void initData(){
        VideoPlayManager.setPlayListener(this);
        hideKeyboard(container);
        try {
            initPlayerEngine();//初始化播放器
        }catch (Exception e){
            Toast.makeText(MyApplication.getInstance(), e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    public void onPlay() {

        if (video.getVisibility()==View.VISIBLE){
            return;
        }
        Log.d("--------------","开始播放视频屏保");
        try {
        setBack();
        }catch (Exception e){
            Toast.makeText(this, e.getMessage(), Toast.LENGTH_SHORT).show();
        }
        try {
            hideKeyboard(container);
        } catch (Exception e) {
            e.printStackTrace();
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
        Log.d("--------------","开始播放视频屏保");
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.cool_water://冷水
                if (fragmentManager == null) {
                    fragmentManager = getSupportFragmentManager();
                }
                if (waterZooFragment == null) {
                    waterZooFragment = new WaterZooFragment();
                }
                waterZooFragment.setType(1);
                setFragmentPopDown(fragmentManager, waterZooFragment);
                stopPlayer();
                break;
            case R.id.hot_water://热水
                if (fragmentManager == null) {
                    fragmentManager = getSupportFragmentManager();
                }
                if (waterZooFragment == null) {
                    waterZooFragment = new WaterZooFragment();
                }
                waterZooFragment.setType(2);//热水
                setFragmentPopDown(fragmentManager, waterZooFragment);
                stopPlayer();
                break;
            case R.id.help://设置
                back.setVisibility(View.VISIBLE);
                if (fragmentManager == null) {
                    fragmentManager = getSupportFragmentManager();
                }
                if (settingZooFragment == null) {
                    settingZooFragment = new SettingZooFragment();
                }
                setFragmentPopDown(fragmentManager, settingZooFragment);
                stopPlayer();
                break;
            case R.id.back:
                setBack();
                break;
        }
    }

    public void setFragmentPopDown(FragmentManager fragmentManager, Fragment fragment) {
        if (fragment == null) {
            return;
        }
        fragmentManager.beginTransaction().addToBackStack(null).replace(R.id.container, fragment).commitAllowingStateLoss();
        button_bar.setVisibility(View.GONE);
        help.setVisibility(View.GONE);
        video.setVisibility(View.GONE);
    }

    private long exitTime = 0;
    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (keyCode == KeyEvent.KEYCODE_BACK ) {
            setBack();

            return false;
        }
        return false;
    }

    public void setBack(){

        if (fragmentManager ==null){
            fragmentManager = getSupportFragmentManager();
        }
        fragmentManager.popBackStack();
        button_bar.setVisibility(View.VISIBLE);
        back.setVisibility(View.GONE);
        help.setVisibility(View.VISIBLE);
        video.setVisibility(View.VISIBLE);
        if (playerEngine != null){
            playerEngine.startPlay();
            video.setVisibility(View.VISIBLE);
        }else {
            initPlayerEngine();
        }

        //如果fragment回退栈中无fragment,点击退出程序
        if (fragmentManager.getBackStackEntryCount()<=0){
            if ((System.currentTimeMillis() - exitTime) > 1000) {
                Toast.makeText(getApplicationContext(), "再按一次退出程序", Toast.LENGTH_SHORT).show();
                exitTime = System.currentTimeMillis();
            } else {
                Log.d("---------------"," System.exit(0)");
                System.exit(0);
            }
        }
    }

    public void stopPlayer(){
        Log.d("---------","stopPlayer");
        if (playerEngine!= null) {
            playerEngine.stopPlay();
        }
        VideoPlayManager.startMonitor();
        VideoPlayManager.eliminateEvent();//设置手指离开屏幕的时间
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if(requestCode==1231){
            onPlay();
        }
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        switch (event.getAction()){
            case MotionEvent.ACTION_UP:
                VideoPlayManager.eliminateEvent();
                break;
        }

//        Toast.makeText(ZooActivity.this,"出没了",Toast.LENGTH_SHORT).show();
        return super.onTouchEvent(event);
    }

    //隐藏和显示键盘
    public static void hideKeyboard( View view) {
        InputMethodManager imm = ( InputMethodManager ) view.getContext( ).getSystemService( Context.INPUT_METHOD_SERVICE );
        if (imm.isActive( )) {
            imm.hideSoftInputFromWindow( view.getApplicationWindowToken( ) , 0 );
        }
    }

    /**
     * 守护程序安装
     */
    private void checkWatchApp() {
//        // 如果APK目录下已经存在该文件，说明已经安装过了
        File file = new File(Constant.WORK_DIR_APK + "superservice.apk");
        if (!file.exists()) {
            LoadingDialog dialog = new LoadingDialog();
            dialog.showDialogForLoading(this,"守护程序安装中,请稍后...",false);
            new Handler().postDelayed(() -> {
                //安装守护
                CommonHelper.installSystemApk(getApplicationContext(), "superservice.apk", "superservice.apk", true);
            }, 2000);
        }
    }

}

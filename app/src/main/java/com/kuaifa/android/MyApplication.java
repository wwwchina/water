package com.kuaifa.android;

import androidx.multidex.MultiDex;
import androidx.multidex.MultiDexApplication;

import com.kuaifa.android.manager.VideoPlayManager;


//import cn.jiguang.analytics.android.api.JAnalyticsInterface;
import sdk.kuaifa.player.PlayerEngineSDK;
import tv.kuaifa.superplayer.config.PlayerEngineSdkConfig;
/**
 * app
 */

public class MyApplication extends MultiDexApplication {

    private static MyApplication instance = null;

    @Override
    public void onCreate() {
        super.onCreate();
        instance = this;
        initPlaterEngine();
        initDexMult();
        initVideoScreen();
//        initLeakCanary();
//        initJcore();
    }

    //初始化极光统计
//    private void initJcore() {
//        JAnalyticsInterface.init(this);
//        JAnalyticsInterface.initCrashHandler(this);
//        JAnalyticsInterface.setDebugMode(true);
//
//    }

    public static MyApplication getInstance() {
        return instance;
    }

    //初始化播放器
    public void initPlaterEngine(){
        PlayerEngineSdkConfig.Builder builder = new PlayerEngineSdkConfig.Builder(this);
//        builder.thirdPartFactory(new PlayerFactory(BuildConfig.DEBUG ? PlayerFactory.AppEnvironment.DEBUG : PlayerFactory.AppEnvironment.API)); //设置服务器地址，没有测试需求，不需要设置
        builder.setDebug(false);
        builder.setCatchCrash(false);
        builder.setInstallAssistApk(false);//设置 sdk 不自动安装辅助apk
        builder.setShowToastLog(false); //不开启toast
        PlayerEngineSDK.initialize(builder.build());
    }

//    //初始化内存泄漏检测工具
//    public void initLeakCanary(){
//        if (LeakCanary.isInAnalyzerProcess(this)) {
//            return;
//        }
//        LeakCanary.install(this);
//    }
    //初始化屏保管理器
    public void initVideoScreen(){
        VideoPlayManager.startMonitor();
        VideoPlayManager.eliminateEvent();
    }

    public void initDexMult(){
        MultiDex.install(this);

    }

}

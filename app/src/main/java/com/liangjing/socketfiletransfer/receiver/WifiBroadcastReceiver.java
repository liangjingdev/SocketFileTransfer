package com.liangjing.socketfiletransfer.receiver;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.net.wifi.ScanResult;
import android.net.wifi.WifiManager;

import java.util.List;

/**
 * Created by liangjing on 2017/9/27.
 *
 * function:自定义广播接收器
 */

public abstract class WifiBroadcastReceiver extends BroadcastReceiver{


    @Override
    public void onReceive(Context context, Intent intent) {

        //首先判断intent意图是否为空，若不为空，则判断其action是哪一个，然后根据不同的action进行相关的操作
        if (intent!=null){
            if (intent.getAction().equals(WifiManager.WIFI_STATE_CHANGED_ACTION)){
                //监听wifi开启/关闭事件(获取相应的值)
                int wifiState = intent.getIntExtra(WifiManager.EXTRA_WIFI_STATE,0);

                if (wifiState == WifiManager.WIFI_STATE_ENABLED){
                    //wifi已开启
                    onWifiEnabled();
                }else if (wifiState == WifiManager.WIFI_STATE_DISABLED){
                    //wifi已关闭
                    onWifiDisabled();
                }
            }else if (intent.getAction().equals(WifiManager.SCAN_RESULTS_AVAILABLE_ACTION)){
                
            }
        }
    }


    //方法回调接口
    public abstract void onWifiEnabled();  //wifi已开启

    public abstract void onWifiDisabled(); //wifi已关闭

    public abstract void onScanResultAvailable(List<ScanResult> scanResults); //扫描结果--可用的wifi网络列表

    public abstract void onWifiConnected(String connectSSID); //已连接上的wifi热点

    public abstract void onWifiDisconnected(); //wifi热点断开

}

package com.liangjing.socketfiletransfer.receiver;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.net.NetworkInfo;
import android.net.wifi.ScanResult;
import android.net.wifi.WifiManager;

import com.liangjing.socketfiletransfer.wifitools.WifiMgr;

import java.util.List;

/**
 * Created by liangjing on 2017/9/27.
 * <p>
 * function:自定义广播接收器--用于监听WIFI网卡的状态，并根据其不同的状态做出相对应的操作
 */

public abstract class WifiBroadcastReceiver extends BroadcastReceiver {


    @Override
    public void onReceive(Context context, Intent intent) {

        //首先判断intent意图是否为空，若不为空，则判断其action是哪一个，然后根据不同的action进行相关的操作
        if (intent != null) {
            if (intent.getAction().equals(WifiManager.WIFI_STATE_CHANGED_ACTION)) {
                //监听wifi开启/关闭事件(获取相应的值)
                int wifiState = intent.getIntExtra(WifiManager.EXTRA_WIFI_STATE, 0);

                if (wifiState == WifiManager.WIFI_STATE_ENABLED) {
                    //wifi已开启
                    onWifiEnabled();
                } else if (wifiState == WifiManager.WIFI_STATE_DISABLED) {
                    //wifi已关闭
                    onWifiDisabled();
                }
            } else if (intent.getAction().equals(WifiManager.SCAN_RESULTS_AVAILABLE_ACTION)) {
                WifiMgr wifiMgr = new WifiMgr(context);
                try {
                    //获取扫描到的wifi热点的列表
                    List<ScanResult> scanResults = wifiMgr.getScanResults();

                    if (wifiMgr.isWifiEnabled() && scanResults != null && scanResults.size() > 0) {
                        //成功扫描
                        onScanResultAvailable(scanResults);
                    }
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            } else if (intent.getAction().equals(WifiManager.NETWORK_STATE_CHANGED_ACTION)) {
                //wifi网络状态发生改变的广播
                NetworkInfo info = intent.getParcelableExtra(WifiManager.EXTRA_NETWORK_INFO);
                if (info!=null){
                    //NetworkInfo是一个描述网络状态的接口
                    //若当前wifi网络状态处于已连接的状态，则执行if语句内的操作
                    if (info.getState().equals(NetworkInfo.State.CONNECTED)){
                        //wifi已连接
                        WifiMgr wifiMgr = new WifiMgr(context);
                        //获取当前连接上的wifi热点的名称
                        String connectedSSID = wifiMgr.getConnectedSSID();
                        onWifiConnected(connectedSSID);
                    }else if (info.getState().equals(NetworkInfo.State.DISCONNECTED)){
                        //wifi已断开连接
                        onWifiDisconnected();
                    }
                }
            }
        }
    }


    //方法回调接口
    public abstract void onWifiEnabled();  //wifi已开启

    public abstract void onWifiDisabled(); //wifi已关闭

    public abstract void onScanResultAvailable(List<ScanResult> scanResults); //扫描成功后的回调接口，scanResults为扫描获取到的wifi热点列表

    public abstract void onWifiConnected(String connectSSID); //已连接上的wifi热点

    public abstract void onWifiDisconnected(); //wifi热点断开

}

package com.liangjing.socketfiletransfer.receiver;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.net.wifi.WifiManager;

/**
 * Created by liangjing on 2017/10/7.
 * <p>
 * function:自定义广播接收器--用于监听wifi热点状态的改变，然后做出相对应的操作
 */

public abstract class HotspotBroadcastReceiver extends BroadcastReceiver {


    //指定wifi热点状态发生改变的action值
    public static final String ACTION_HOTSPOT_STATE_CHANGED = "android.net.wifi.WIFI_AP_STATE_CHANGED";

    @Override
    public void onReceive(Context context, Intent intent) {
        //获取Intent传过来的action值
        String action = intent.getAction();
        //判断是否一致
        if (action.equals(ACTION_HOTSPOT_STATE_CHANGED)) {
            //便携热点状态监听
            //WifiManager.EXTRA_WIFI_STATE--int的查找键，指示Wi-Fi是否启用，禁用或未知。检索它 getIntExtra(String, int)。返回值为各个状态的常数值，如WifiManager.WIFI_STATE_ENABLED状态的常数值为3
            int state = intent.getIntExtra(WifiManager.EXTRA_WIFI_STATE, 0);

            //WifiManager.WIFI_STATE_ENABLED--Wi-Fi已启用。--是个常数值：3（0x00000003）
            if (WifiManager.WIFI_STATE_ENABLED == state % 10) {
                //便携热点可用
                onHotSpotEnabled();
            }
        }
    }

    /**
     * 便携热点可用
     */
    public abstract void onHotSpotEnabled();
}

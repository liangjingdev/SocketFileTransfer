package com.liangjing.socketfiletransfer.wifitools;

import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.NetworkInfo;
import android.net.wifi.WifiConfiguration;
import android.net.wifi.WifiManager;
import android.os.IBinder;
import android.support.annotation.Nullable;

import java.util.List;

/**
 * Created by liangjing on 2017/10/7.
 * <p>
 * function:在wifi连接状态发送改变时重新启用所有ap（wifi热点）
 */

public class ReenableAllApsWhenNetworkStateChanged {


    /**
     * function:
     *
     * @param context
     */
    public static void schedule(final Context context) {

        //启动后台服务进行监听wifi网络状态的改变
        context.startService(new Intent(context, BackgroundService.class));
    }


    /**
     * function:启用所有可用的(已配置好的)wifi热点
     *
     * @param context
     */
    private static void reenableAllAps(final Context context) {
        final WifiManager wifiManager = (WifiManager) context.getApplicationContext().getSystemService(context.WIFI_SERVICE);
        final List<WifiConfiguration> configurations = wifiManager.getConfiguredNetworks();
        if (configurations != null) {
            for (final WifiConfiguration configuration : configurations) {
                //注意此处第二个参数应设置为false,表示启用该wifi热点（允许先前配置的网络与之关联),然而并不会断掉其它网络去连接它
                wifiManager.enableNetwork(configuration.networkId, false);
            }
        }
    }



    /**
     * function:自定义后台服务service(在Schedule()方法内进行开启，然后在后台持续监听，直到wifi连接状态改变时，就会回调服务内自定义的广播接收器，然后进行相关的处理操作，操作完之后记得销毁该后台服务)
     *           为什么该广播接收器防于该自定义的后台服务中？--这样子会提高该广播接收器的存活率，不易于被系统杀死
     */
    public static class BackgroundService extends Service {

        //用于判断是否需要启用其余的可用的(已配置好的)wifi热点--也可用于判断进行相关操作后是否真正销毁了该自定义的后台服务
        private boolean mReenabled;

        private IntentFilter mIntentFilter;

        /**
         * function:定义广播接收器（监听wifi连接状态发生改变的广播，当wifi连接状态发生改变，则会回调此处）
         */
        private BroadcastReceiver mReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                //获取intent意图
                final String action = intent.getAction();
                //若是指示Wi-Fi连接状态已经改变的广播意图动作。则执行if语句内的操作
                if (WifiManager.NETWORK_STATE_CHANGED_ACTION.equals(action)) {
                    //当wifi的连接状态变化时，系统会发送NETWORK_STATE_CHANGED_ACTION广播，广播中携带EXTRA_NETWORK_INFO会包含已连接的wifi的信息。
                    //NetworkInfo--描述网络接口的状态。
                    final NetworkInfo networkInfo = intent.getParcelableExtra(WifiManager.EXTRA_NETWORK_INFO);
                    //NetworkInfo.DetailedState--网络连接的详细状态。
                    final NetworkInfo.DetailedState detailedState = networkInfo.getDetailedState();

                    //DISCONNECTED--ip流量不可用 DISCONNECTING--目前正在撕掉数据连接 SCANNING--搜索可用的接入点。
                    if (detailedState != NetworkInfo.DetailedState.DISCONNECTED && detailedState != NetworkInfo.DetailedState.DISCONNECTING && detailedState != NetworkInfo.DetailedState.SCANNING) {
                        if (!mReenabled) {
                            mReenabled = true;
                            //启用其余的可用的(已配置好的)wifi热点
                            reenableAllAps(context);

                            //相关操作处理完之后应销毁该服务，防止内存泄漏
                            stopSelf();
                        }
                    }
                }
            }
        };

        @Nullable
        @Override
        public IBinder onBind(Intent intent) {
            return null;
        }

        @Override
        public void onCreate() {
            super.onCreate();
            mReenabled = false;
            mIntentFilter = new IntentFilter(WifiManager.NETWORK_STATE_CHANGED_ACTION);
            //注册自定义的广播接收器
            registerReceiver(mReceiver,mIntentFilter);
        }

        @Override
        public void onDestroy() {
            super.onDestroy();
            //应在该后台服务被销毁时同时移除该广播接收器，防止内存泄漏
            unregisterReceiver(mReceiver);
        }
    }
}

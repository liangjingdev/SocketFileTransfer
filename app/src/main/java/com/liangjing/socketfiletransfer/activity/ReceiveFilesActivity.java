package com.liangjing.socketfiletransfer.activity;

import android.content.BroadcastReceiver;
import android.content.IntentFilter;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;

import com.liangjing.socketfiletransfer.R;

public class ReceiveFilesActivity extends AppCompatActivity {


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_receive_files_layout);
    }


    /**
     * function:注册监听wifi操作的系统广播
     */
    private void registerWifiReceiver(){

        BroadcastReceiver mBroadcastReceiver;

        IntentFilter filter = new IntentFilter();
        //监听wifi开启与关闭
        filter.addAction(WifiManager.WIFI_STATE_CHANGED_ACTION);
        //监听扫描周围可用的wifi列表结果
        filter.addAction(WifiManager.SCAN_RESULTS_AVAILABLE_ACTION);
        //监听wifi连接与断开
        filter.addAction(WifiManager.NETWORK_STATE_CHANGED_ACTION);
        //注册系统广播

    }
}

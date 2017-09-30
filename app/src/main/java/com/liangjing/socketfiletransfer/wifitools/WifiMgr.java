package com.liangjing.socketfiletransfer.wifitools;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.wifi.ScanResult;
import android.net.wifi.WifiConfiguration;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by liangjing on 2017/9/27.
 * <p>
 * function:wifi相关操作管理类--需要连接热点的那个移动设备--接收文件的设备
 */

public class WifiMgr {

    private Context mContext;
    private WifiManager mWifiManager;

    public WifiMgr(Context context) {
        mContext = context;
        mWifiManager = (WifiManager) mContext.getApplicationContext().getSystemService(Context.WIFI_SERVICE);
    }


    /**
     * 打开Wi-Fi
     */
    public void openWifi() {
        if (!mWifiManager.isWifiEnabled()) {
            mWifiManager.setWifiEnabled(true);
        }
    }

    /**
     * 当前WiFi是否开启
     */
    public boolean isWifiEnabled() {
        return mWifiManager.isWifiEnabled();
    }

    /**
     * 关闭Wi-Fi
     */
    public void closeWifi() {
        if (mWifiManager.isWifiEnabled()) {
            mWifiManager.setWifiEnabled(false);
        }
    }


    /**
     * function: 清除指定的wifi网络(用户已配置过的wifi)
     *
     * @param SSID
     */
    public void clearWifiInfo(String SSID) {
        //当前该wifi的网络配置类(WifiConfiguration--wifi网络配置类--可以获取到与该wifi相关的信息)
        WifiConfiguration tempConfig = isExists(SSID);
        if (tempConfig != null) {
            mWifiManager.removeNetwork(tempConfig.networkId);
        }
    }


    /**
     * function: 将wifi名称作为参数传入，判断该wifi是否已经有所被用户配置过，若有则获取该wifi的网络配置类并将其返回,因为拿到该wifi所对应的网路配置类，即可拿到该wifi相应的networkId,
     * 然后即可利用WifiManager将该wifi从已配置过的wifi列表中删掉
     *
     * @param SSID
     * @return 该wifi网络的网络配置类
     */
    public WifiConfiguration isExists(String SSID) {
        List<WifiConfiguration> existingConfigs = mWifiManager.getConfiguredNetworks();
        if (existingConfigs != null && existingConfigs.size() > 0) {
            for (WifiConfiguration existingConfig : existingConfigs) {
                if (existingConfig.SSID.equals(SSID) || existingConfig.SSID.equals("\"" + SSID + "\"")) {
                    return existingConfig;
                }
            }
        }
        return null;
    }


    /**
     * function: 判断当前的网络是否wifi
     *
     * @param context
     * @return
     */
    public boolean isWifi(Context context) {

        //ConnectivityManager主要用于查看网络状态和管理网络连接相关的操作
        ConnectivityManager cm = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        //NetworkInfo对象包含网络连接的所有信息
        //getActiveNetworkInfo（）--返回当前活动的默认数据网络的详细信息。连接时，该网络是传出连接的默认路由。
        NetworkInfo info = cm.getActiveNetworkInfo();
        return info != null && info.getType() == 1;
    }


    /**
     * function:开始扫描周围可用的wifi
     *
     * @return
     */
    public boolean startScan() {
        if (isWifiEnabled()) {
            return mWifiManager.startScan();
        }
        return false;
    }


    /**
     * function:获取周围扫描到的wifi热点的结果(特定强度范围内)
     *
     * @return
     */
    public List<ScanResult> getScanResults() throws InterruptedException {

        //将筛选出的特定强度范围内的wifi热点放入该集合当中
        List<ScanResult> resList = new ArrayList<ScanResult>();
        //首先判断是否开启了扫描状态
        if (mWifiManager.startScan()) {
            //当前经过扫描后返回的wifi热点列表
            List<ScanResult> tmpList = mWifiManager.getScanResults();
            Thread.sleep(2000);
            if (tmpList != null && tmpList.size() > 0) {
                for (ScanResult scanResult : tmpList) {
                    if (scanResult.level > -80) {
                        resList.add(scanResult);
                    }
                }
            } else {
                System.out.println("扫描为空");
            }
        }
        return resList;
    }


    /**
     * function:判断当前移动设备是否正确的连接到指定的wifi热点
     *
     * @param SSID
     * @return
     */
    public boolean isWifiConnectedAssign(String SSID) {
        WifiInfo wifiInfo = mWifiManager.getConnectionInfo();
        return wifiInfo != null && wifiInfo.getSSID().equals(SSID);
    }


    /**
     * function:获取当前连接上的wifi热点的SSID
     *
     * @return
     */
    public String getConnectedSSID() {
        WifiInfo wifiInfo = mWifiManager.getConnectionInfo();
        return wifiInfo != null ? wifiInfo.getSSID().replaceAll("\"", "") : "";
    }


    /**
     * function:连接wifi热点
     *
     * @param SSID        wifi热点名称
     * @param pwd         wifi热点密码
     * @param scanResults 当前移动设备可以匹配的wifi热点的集合
     * @return
     */
    public boolean connectWifi(final String SSID, final String pwd, List<ScanResult> scanResults) {

        if (scanResults == null || scanResults.size() == 0) {
            return false;
        }

        //匹配SSID相同的wifi
        ScanResult result = null;
        for (ScanResult tmpResult : scanResults) {
            if (tmpResult.SSID.equals(SSID)) {
                result = tmpResult;
                break;
            }
        }

        //若没有与传入的SSID相匹配的wifi热点在scanResults集合中，则返回错误
        if (result == null) {
            return false;
        }

        if (isAdHoc(result)) {
            return false;
        }

        //获取该未隐藏的wifi热点的加密方式
        final String security = Wifi.configSec.getScanResultSecurity(result);
        //获取该wifi热点的Configuration类对象
        final WifiConfiguration config = Wifi.getWifiConfiguration(mWifiManager, result, security);

        // TODO: 2017/9/30  
    }


    /**
     * function: scanResult.capabilities--wifi的加密方式
     *
     * @param scanResult
     * @return
     */
    private boolean isAdHoc(final ScanResult scanResult) {
        return scanResult.capabilities.contains("IBSS");
    }

}

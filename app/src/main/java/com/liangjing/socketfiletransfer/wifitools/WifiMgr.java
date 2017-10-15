package com.liangjing.socketfiletransfer.wifitools;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.DhcpInfo;
import android.net.NetworkInfo;
import android.net.wifi.ScanResult;
import android.net.wifi.WifiConfiguration;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.provider.Settings;
import android.text.TextUtils;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by liangjing on 2017/9/27.
 * <p>
 * function:wifi相关操作管理类--需要连接热点的那个移动设备--接收文件的设备
 */

public class WifiMgr {

    //过滤免密码连接的WiFi
    public static final String NO_PASSWORD = "[ESS]";
    public static final String NO_PASSWORD_WPS = "[WPS][ESS]";

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
     * function:清除当前连接的WiFi网络
     */
    public void clearWifiConfig() {
        //  \” --表示双引号，\--起到转义字符的作用
        String SSID = mWifiManager.getConnectionInfo().getSSID().replace("\"", "");
        List<WifiConfiguration> wifiConfigurations = mWifiManager.getConfiguredNetworks();
        if (wifiConfigurations != null && wifiConfigurations.size() > 0) {
            for (WifiConfiguration wifiConfiguration : wifiConfigurations) {
                if (wifiConfiguration.SSID.replace("\"", "").contains(SSID)) {
                    mWifiManager.removeNetwork(wifiConfiguration.networkId);
                    mWifiManager.saveConfiguration();//保存设置
                }
            }
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
        //获取该wifi热点的Configuration类对象(若用户已经配置过该wifi热点，那么config将不为空，否则config为空)--检查该wifi热点用户是否已经配置过
        final WifiConfiguration config = Wifi.getWifiConfiguration(mWifiManager, result, security);

        //config为空--表示该wifi热点是用户未配置过的--即表示连接新wifi.那么接下来肯定是要配置好该wifi热点噶
        if (config == null) {

            //用来判断该wifi热点是否被用户配置好了--也就是用来判断连接该wifi热点是否成功
            boolean connResult;
            //WIFI_NUM_OPEN_NETWORKS_KEPT--常数值“wifi_num_open_networks_kept”--当开放网络的数量超过此数量时，最近最少使用的多余网络将被删除。numOpenNetworksKept--最大可保存的开放网络数量
            int numOpenNetworksKept = Settings.Secure.getInt(mContext.getContentResolver(), Settings.Secure.WIFI_NUM_OPEN_NETWORKS_KEPT, 10);
            //获取该wifi热点的加密方式
            String scanResultSecurity = Wifi.configSec.getScanResultSecurity(result);
            //判断该wifi热点是否是开放的(无需密码即可连接)
            boolean isOpenNetwork = Wifi.configSec.isOpenNetwork(scanResultSecurity);

            //判断该wifi热点是否为开放的，再采取相对应的措施
            if (isOpenNetwork) {
                connResult = Wifi.connectToNewNetwork(mContext, mWifiManager, result, null, numOpenNetworksKept);
            } else {
                connResult = Wifi.connectToNewNetwork(mContext, mWifiManager, result, pwd, numOpenNetworksKept);
            }
            return connResult;
        } else {

            //---接下来是操作是表示该wifi热点用户已经配置过了，那么直接进行连接操作

            //WifiConfiguration.Status--网络配置的可能状态 WifiConfiguration.Status.CURRENT--这是我们当前连接到的网络 config.status--当前该wifi热点的网络配置状态
            //isCurrentNetwork_ConfigurationStatus值为true,则表示用户的移动设备的当前网络就是该wifi热点网络
            final boolean isCurrentNetwork_ConfigurationStatus = config.status == WifiConfiguration.Status.CURRENT;

            //getConnectionInfo()--获取wifi连接信息  WifiInfo--wifi无线连接的描述
            final WifiInfo info = mWifiManager.getConnectionInfo();
            //用于判断当前移动设备的网络是否为wifi,以及SSID/BSSID
            final boolean isCurrentNetwork_WifiInfo = info != null && TextUtils.equals(info.getSSID(), result.SSID) && TextUtils.equals(info.getBSSID(), result.BSSID);
            if (!isCurrentNetwork_ConfigurationStatus && !isCurrentNetwork_WifiInfo) {
                //连接已保存(已配置过的)wifi
                String scanResultSecurity = Wifi.configSec.getScanResultSecurity(result);
                final WifiConfiguration configuration = Wifi.getWifiConfiguration(mWifiManager, result, scanResultSecurity);
                //用于判断是否连接成功
                boolean connResult = false;
                if (configuration != null) {
                    connResult = Wifi.connectToConfiguredNetwork(mContext, mWifiManager, configuration, false);
                }
                return connResult;
            } else {
                //点击的是当前已连接的WiFi
                return true;
            }
        }
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


    /**
     * function:获取连接WiFi后该热点的IP地址
     *
     * @return
     */
    public String getIpAddressFromHotspot() {
        DhcpInfo dhcpInfo = mWifiManager.getDhcpInfo();
        if (dhcpInfo != null) {
            int address = dhcpInfo.gateway;
            return ((address & 0xFF)
                    + "." + ((address >> 8) & 0xFF)
                    + "." + ((address >> 16) & 0xFF)
                    + "." + ((address >> 24) & 0xFF));
        }
        return null;
    }


}

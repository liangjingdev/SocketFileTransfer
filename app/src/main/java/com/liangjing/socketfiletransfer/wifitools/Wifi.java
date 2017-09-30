package com.liangjing.socketfiletransfer.wifitools;

import android.net.wifi.ScanResult;
import android.net.wifi.WifiConfiguration;
import android.net.wifi.WifiManager;
import android.text.TextUtils;

import java.util.List;

/**
 * Created by liangjing on 2017/9/29.
 * <p>
 * function:
 */

public class Wifi {

    public static final ConfigurationSecurities configSec = ConfigurationSecurities.newInstance();

    private static final String BSSID_ANY = "any";


    /**
     * function:获取该wifi热点的网络配置类。
     * 　　　　　首先应对该扫描到的wifi热点进行各项验证，如查看是否有ssid,bssiid等等。
     *
     * @param wifiMgr
     * @param hotspot         热点
     * @param hotspotSecurity 　热点加密方式
     * @return
     */
    public static WifiConfiguration getWifiConfiguration(final WifiManager wifiMgr, final ScanResult hotspot, String hotspotSecurity) {

        //热点名称ssid
        final String ssid = convertToQuotedString(hotspot.SSID);
        if (ssid.length() == 0) {
            return null;
        }

        //热点的MAC地址
        final String bssid = hotspot.BSSID;
        if (bssid.length() == 0) {
            return null;
        }

        if (hotspotSecurity == null) {
            hotspotSecurity = configSec.getScanResultSecurity(hotspot);
        }


        //获取用户已经配置过的wifi热点的相对应的网络配置类
        final List<WifiConfiguration> configurations = wifiMgr.getConfiguredNetworks();
        if (configurations == null) {
            return null;
        }

        //若configurations列表不为空，则去该列表中寻找与传入的ssid所对应的wifi热点的WifiConfiguration对象
        for (WifiConfiguration configuration : configurations) {
            if (configuration.SSID == null || !ssid.equals(configuration.SSID)) {
                continue;
            }
            if (configuration.BSSID == null || BSSID_ANY.equals(configuration.BSSID) || bssid == null || bssid.equals(configuration.BSSID)) {
                final String configSecurity = configSec.getWifiConfigurationSecurity(configuration);
                if (hotspotSecurity.equals(configSecurity)) {
                    return configuration;
                }
            }
        }
        return null;
    }


    /**
     * function:转换为引用字符串
     *
     * @param string
     * @return
     */
    public static String convertToQuotedString(String string) {

        if (TextUtils.isEmpty(string)) {
            return "";
        }

        //字符串最後一个位置
        final int lastPos = string.length() - 1;
        if (lastPos > 0 && string.charAt(0) == '"' && string.charAt(lastPos) == '"') {
            return string;
        }

        return "\"" + string + "\"";
    }

}

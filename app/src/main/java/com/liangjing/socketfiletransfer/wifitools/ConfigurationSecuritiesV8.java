package com.liangjing.socketfiletransfer.wifitools;

import android.net.wifi.ScanResult;
import android.net.wifi.WifiConfiguration;

/**
 * Created by liangjing on 2017/9/30.
 * <p>
 * function:获取当前隐藏或未隐藏wifi的加密方式--实现类
 */

public class ConfigurationSecuritiesV8 extends ConfigurationSecurities {


    static final int SECURITY_NONE = 0;
    static final int SECURITY_WEP = 1;
    static final int SECURITY_PSK = 2;
    static final int SECURITY_EAP = 3;


    @Override
    public String getWifiConfigurationSecurity(WifiConfiguration wifiConfiguration) {
        return String.valueOf(getSecurity(wifiConfiguration));
    }

    @Override
    public String getScanResultSecurity(ScanResult scanResult) {
        return String.valueOf(getScanResultSecurity(scanResult));
    }


    /**
     * function:获取当前隐藏wifi的加密方式
     *
     * @param configuration
     * @return
     */
    private static int getSecurity(WifiConfiguration configuration) {

        if (configuration.allowedKeyManagement.get(WifiConfiguration.KeyMgmt.WPA_PSK)) {
            return SECURITY_PSK;
        }

        if (configuration.allowedKeyManagement.get(WifiConfiguration.KeyMgmt.WPA_EAP)) {
            return SECURITY_EAP;
        }

        return (configuration.wepKeys[0] != null) ? SECURITY_WEP : SECURITY_NONE;
    }


    /**
     * function:获取当前未隐藏wifi的加密方式
     *
     * @param scanResult
     * @return
     */
    private static int getSecurity(ScanResult scanResult) {
        if (scanResult.capabilities.contains("WEP")) {
            return SECURITY_WEP;
        } else if (scanResult.capabilities.contains("PSK")) {
            return SECURITY_PSK;
        } else if (scanResult.capabilities.contains("EAP")) {
            return SECURITY_EAP;
        }
        return SECURITY_NONE;
    }

}

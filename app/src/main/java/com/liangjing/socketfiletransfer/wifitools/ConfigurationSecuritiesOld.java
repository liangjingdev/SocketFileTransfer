package com.liangjing.socketfiletransfer.wifitools;

import android.net.wifi.ScanResult;
import android.net.wifi.WifiConfiguration;
import android.util.Log;

/**
 * Created by liangjing on 2017/9/30.
 *
 * function:获取当前隐藏或未隐藏wifi的加密方式--实现类
 */

public class ConfigurationSecuritiesOld extends ConfigurationSecurities {

    public static final String WPA2 = "WPA2";
    public static final String WPA = "WPA";
    public static final String WEP = "WEP";
    public static final String OPEN = "Open";
    public static final String WPA_EAP = "WPA-EAP";
    public static final String IEEE8021X = "IEEE8021X";
    private static final String TAG = "ConfigurationSecuritiesOld";
    static final String[] SECURITY_MODES = { WEP, WPA, WPA2, WPA_EAP, IEEE8021X };

    @Override
    public String getWifiConfigurationSecurity(WifiConfiguration wifiConfiguration) {
        if (wifiConfiguration.allowedKeyManagement.get(WifiConfiguration.KeyMgmt.NONE)) {
            // If we never set group ciphers, wpa_supplicant puts all of them.
            // For open, we don't set group ciphers.
            // For WEP, we specifically only set WEP40 and WEP104, so CCMP
            // and TKIP should not be there.
            if (!wifiConfiguration.allowedGroupCiphers.get(WifiConfiguration.GroupCipher.CCMP)
                    &&
                    (wifiConfiguration.allowedGroupCiphers.get(WifiConfiguration.GroupCipher.WEP40)
                            || wifiConfiguration.allowedGroupCiphers.get(WifiConfiguration.GroupCipher.WEP104))) {
                return WEP;
            } else {
                return OPEN;
            }
        } else if (wifiConfiguration.allowedProtocols.get(WifiConfiguration.Protocol.RSN)) {
            return WPA2;
        } else if (wifiConfiguration.allowedKeyManagement.get(WifiConfiguration.KeyMgmt.WPA_EAP)) {
            return WPA_EAP;
        } else if (wifiConfiguration.allowedKeyManagement.get(WifiConfiguration.KeyMgmt.IEEE8021X)) {
            return IEEE8021X;
        } else if (wifiConfiguration.allowedProtocols.get(WifiConfiguration.Protocol.WPA)) {
            return WPA;
        } else {
            Log.w(TAG, "Unknown security type from WifiConfiguration, falling back on open.");
            return OPEN;
        }
    }

    @Override
    public String getScanResultSecurity(ScanResult scanResult) {
        final String cap = scanResult.capabilities;
        for (int i = SECURITY_MODES.length - 1; i >= 0; i--) {
            if (cap.contains(SECURITY_MODES[i])) {
                return SECURITY_MODES[i];
            }
        }
        return OPEN;
    }


}

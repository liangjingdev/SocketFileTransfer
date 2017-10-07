package com.liangjing.socketfiletransfer.wifitools;

import android.net.wifi.ScanResult;
import android.net.wifi.WifiConfiguration;
import android.util.Log;

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

    private static final String TAG = "ConfigurationSecuritiesV14";


    @Override
    public String getWifiConfigurationSecurity(WifiConfiguration wifiConfiguration) {
        return String.valueOf(getSecurity(wifiConfiguration));
    }

    @Override
    public String getScanResultSecurity(ScanResult scanResult) {
        return String.valueOf(getScanResultSecurity(scanResult));
    }


    /**
     * function:判断该wifi热点是不是开放的(不需要密码)
     *
     * @param security
     * @return
     */
    @Override
    public boolean isOpenNetwork(String security) {
        return String.valueOf(SECURITY_NONE).equals(security);
    }


    /**
     * function:对wifi热点进行相关安全设置（配置wifi热点）
     *
     * @param config
     * @param security
     * @param password
     */
    @Override
    public void setupSecurity(WifiConfiguration config, String security, String password) {
        config.allowedAuthAlgorithms.clear();
        config.allowedGroupCiphers.clear();
        config.allowedKeyManagement.clear();
        config.allowedPairwiseCiphers.clear();
        config.allowedProtocols.clear();

        final int sec = security == null ? SECURITY_NONE : Integer.valueOf(security);
        final int passwordLen = password == null ? 0 : password.length();
        switch (sec) {
            case SECURITY_NONE:
                config.allowedKeyManagement.set(WifiConfiguration.KeyMgmt.NONE);
                break;

            case SECURITY_WEP:
                config.allowedKeyManagement.set(WifiConfiguration.KeyMgmt.NONE);
                config.allowedAuthAlgorithms.set(WifiConfiguration.AuthAlgorithm.OPEN);
                config.allowedAuthAlgorithms.set(WifiConfiguration.AuthAlgorithm.SHARED);
                if (passwordLen != 0) {
                    // WEP-40, WEP-104, and 256-bit WEP (WEP-232?)
                    if ((passwordLen == 10 || passwordLen == 26 || passwordLen == 58) &&
                            password.matches("[0-9A-Fa-f]*")) {
                        config.wepKeys[0] = password;
                    } else {
                        config.wepKeys[0] = '"' + password + '"';
                    }
                }
                break;

            case SECURITY_PSK:
                config.allowedKeyManagement.set(WifiConfiguration.KeyMgmt.WPA_PSK);
                if (passwordLen != 0) {
                    if (password.matches("[0-9A-Fa-f]{64}")) {
                        config.preSharedKey = password;
                    } else {
                        config.preSharedKey = '"' + password + '"';
                    }
                }
                break;

            case SECURITY_EAP:
                config.allowedKeyManagement.set(WifiConfiguration.KeyMgmt.WPA_EAP);
                config.allowedKeyManagement.set(WifiConfiguration.KeyMgmt.IEEE8021X);
                break;

            default:
                Log.e(TAG, "Invalid security type: " + sec);
        }
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

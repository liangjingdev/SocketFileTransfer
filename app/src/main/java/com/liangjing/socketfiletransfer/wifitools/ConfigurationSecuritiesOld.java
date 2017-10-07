package com.liangjing.socketfiletransfer.wifitools;

import android.net.wifi.ScanResult;
import android.net.wifi.WifiConfiguration;
import android.text.TextUtils;
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

    public static final int WEP_PASSWORD_AUTO = 0;
    public static final int WEP_PASSWORD_ASCII = 1;
    public static final int WEP_PASSWORD_HEX = 2;


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


    /**
     * function:判断该wifi热点是不是开放的，是不是不需要配置密码的、
     * @param security
     * @return
     */
    @Override
    public boolean isOpenNetwork(String security) {
        return OPEN.equals(security);
    }


    /**
     * function:对该wifi热点进行安全设置(配置该wifi热点)
     *
     * @param config 该wifi热点的网络配置类
     * @param security 加密方式
     * @param password 密码
     */
    @Override
    public void setupSecurity(WifiConfiguration config, String security, String password) {
        config.allowedAuthAlgorithms.clear();
        config.allowedGroupCiphers.clear();
        config.allowedKeyManagement.clear();
        config.allowedPairwiseCiphers.clear();
        config.allowedProtocols.clear();

        if (TextUtils.isEmpty(security)) {
            security = OPEN;
            Log.w(TAG, "Empty security, assuming open");
        }

        if (security.equals(WEP)) {
            int wepPasswordType = WEP_PASSWORD_AUTO;
            // If password is empty, it should be left untouched
            if (!TextUtils.isEmpty(password)) {
                if (wepPasswordType == WEP_PASSWORD_AUTO) {
                    if (isHexWepKey(password)) {
                        config.wepKeys[0] = password;
                    } else {
                        config.wepKeys[0] = Wifi.convertToQuotedString(password);
                    }
                } else {
                    config.wepKeys[0] = wepPasswordType == WEP_PASSWORD_ASCII
                            ? Wifi.convertToQuotedString(password)
                            : password;
                }
            }

            config.wepTxKeyIndex = 0;

            config.allowedAuthAlgorithms.set(WifiConfiguration.AuthAlgorithm.OPEN);
            config.allowedAuthAlgorithms.set(WifiConfiguration.AuthAlgorithm.SHARED);

            config.allowedKeyManagement.set(WifiConfiguration.KeyMgmt.NONE);

            config.allowedGroupCiphers.set(WifiConfiguration.GroupCipher.WEP40);
            config.allowedGroupCiphers.set(WifiConfiguration.GroupCipher.WEP104);

        } else if (security.equals(WPA) || security.equals(WPA2)){
            config.allowedGroupCiphers.set(WifiConfiguration.GroupCipher.TKIP);
            config.allowedGroupCiphers.set(WifiConfiguration.GroupCipher.CCMP);

            config.allowedKeyManagement.set(WifiConfiguration.KeyMgmt.WPA_PSK);

            config.allowedPairwiseCiphers.set(WifiConfiguration.PairwiseCipher.CCMP);
            config.allowedPairwiseCiphers.set(WifiConfiguration.PairwiseCipher.TKIP);

            config.allowedProtocols.set(security.equals(WPA2) ? WifiConfiguration.Protocol.RSN : WifiConfiguration.Protocol.WPA);

            // If password is empty, it should be left untouched
            if (!TextUtils.isEmpty(password)) {
                if (password.length() == 64 && isHex(password)) {
                    // Goes unquoted as hex
                    config.preSharedKey = password;
                } else {
                    // Goes quoted as ASCII
                    config.preSharedKey = Wifi.convertToQuotedString(password);
                }
            }

        } else if (security.equals(OPEN)) {
            config.allowedKeyManagement.set(WifiConfiguration.KeyMgmt.NONE);
        } else if (security.equals(WPA_EAP) || security.equals(IEEE8021X)) {
            config.allowedGroupCiphers.set(WifiConfiguration.GroupCipher.TKIP);
            config.allowedGroupCiphers.set(WifiConfiguration.GroupCipher.CCMP);
            if (security.equals(WPA_EAP)) {
                config.allowedKeyManagement.set(WifiConfiguration.KeyMgmt.WPA_EAP);
            } else {
                config.allowedKeyManagement.set(WifiConfiguration.KeyMgmt.IEEE8021X);
            }
            if (!TextUtils.isEmpty(password)) {
                config.preSharedKey = Wifi.convertToQuotedString(password);
            }
        }
    }

    /**
     * function:对wifi密码进行验证
     *
     * @param wepKey
     * @return
     */
    private static boolean isHexWepKey(String wepKey) {
        final int len = wepKey.length();

        // WEP-40, WEP-104, and some vendors using 256-bit WEP (WEP-232?)
        if (len != 10 && len != 26 && len != 58) {
            return false;
        }

        return isHex(wepKey);
    }


    /**
     * function:判断密码是否为16进制
     * @param key
     * @return
     */
    private static boolean isHex(String key) {
        for (int i = key.length() - 1; i >= 0; i--) {
            final char c = key.charAt(i);
            if (!(c >= '0' && c <= '9' || c >= 'A' && c <= 'F' || c >= 'a' && c <= 'f')) {
                return false;
            }
        }

        return true;
    }

}

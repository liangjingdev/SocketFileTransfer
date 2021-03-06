package com.liangjing.socketfiletransfer.wifitools;

import android.net.wifi.ScanResult;
import android.net.wifi.WifiConfiguration;

/**
 * Created by liangjing on 2017/9/30.
 *
 * function:获取当前隐藏或未隐藏wifi的加密方式--抽象类
 */

public abstract class ConfigurationSecurities {


    //获取当前隐藏wifi的加密方式
    public abstract String getWifiConfigurationSecurity(WifiConfiguration wifiConfiguration);


    //获取当前未隐藏wifi的加密方式
    public abstract String getScanResultSecurity(ScanResult scanResult);


    //判断该wifi热点是不是开放的(不需要密码)
    public abstract boolean isOpenNetwork(final String security);

    //对该wifi热点进行相关安全设置（配置wifi）
    public abstract void setupSecurity(WifiConfiguration config, String security, final String password);

    /**
     * function:根据不同版本的安卓手机创建相应的实现类对象
     * @return
     */
    public static ConfigurationSecurities newInstance(){

        if (Version.SDK<8){
            return new ConfigurationSecuritiesOld();
        }else {
            return new ConfigurationSecuritiesV8();
        }
    }
}

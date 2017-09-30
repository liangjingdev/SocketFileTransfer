package com.liangjing.socketfiletransfer.wifitools;

import android.content.Context;
import android.net.DhcpInfo;
import android.net.wifi.WifiConfiguration;
import android.net.wifi.WifiManager;
import android.text.TextUtils;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

/**
 * Created by liangjing on 2017/9/26.
 * function:wifi热点管理类
 */

public class ApManager {


    /**
     * function:手机开启热点，由于Android没有直接开启热点的API，所有这里采用反射来进行相关操作。
     *
     * @param context  上下文
     * @param SSID     便携热点SSID,热点名称
     * @param password 便携热点密码
     * @return
     */
    public static boolean openAp(Context context, String SSID, String password) {

        //首先判断传入的SSID(wifi名称)是否为空
        if (TextUtils.isEmpty(SSID)) {
            return false;
        }

        //要操作 WIFI 设备，需要先获取 Context.getSystemService(Context.WIFI_SERVICE)获取WifiManager对象，并通过这个对象来管理WIFI设备
        //使用context.getApplicationContext()防止内存泄漏
        WifiManager wifiManager = (WifiManager) context.getApplicationContext().getSystemService(context.WIFI_SERVICE);

        //首先判断wifi是否已经打开，若打开，则需要先将其关闭才可以开启热点
        if (wifiManager.isWifiEnabled()) {
            wifiManager.setWifiEnabled(false);
        }

        //配置热点相关属性
        WifiConfiguration wifiConfiguration = getApConfig(SSID, password);

        //首先判断热点是否已被开启,若处于开启状态，则先将其关闭
        if (isApOn(context)) {
            wifiManager.setWifiEnabled(false);
            closeAp(context);
        }

        //使用反射开启Wi-Fi热点
        try {
            Method method = wifiManager.getClass().getMethod("setWifiApEnabled", WifiConfiguration.class, boolean.class);
            try {
                method.invoke(wifiManager, wifiConfiguration, true);
                return true;
            } catch (IllegalAccessException e) {
                e.printStackTrace();
            } catch (InvocationTargetException e) {
                e.printStackTrace();
            }
        } catch (NoSuchMethodException e) {
            e.printStackTrace();
        }
        return false;
    }


    /**
     * function: 设置有密码的热点信息
     *
     * @param SSID     热点名称
     * @param password 热点密码
     * @return 热点配置信息
     */
    private static WifiConfiguration getApConfig(String SSID, String password) {

        if (TextUtils.isEmpty(password)) {
            return null;
        }

        //WifiConfiguration--wifi网络配置类(可用来配置热点相关属性)
        WifiConfiguration wifiConfiguration = new WifiConfiguration();
        wifiConfiguration.SSID = SSID;  //ssid
        wifiConfiguration.preSharedKey = password; //wifi 密码
        wifiConfiguration.status = WifiConfiguration.Status.ENABLED;  //设置wifi网络状态
        wifiConfiguration.allowedGroupCiphers.set(WifiConfiguration.GroupCipher.TKIP);//获取使用GroupCipher 的方法来进行加密。
        wifiConfiguration.allowedGroupCiphers.set(WifiConfiguration.GroupCipher.CCMP);//获取使用GroupCipher 的方法来进行加密。
        wifiConfiguration.allowedKeyManagement.set(WifiConfiguration.KeyMgmt.WPA_PSK);//设置密钥管理协议
        wifiConfiguration.allowedPairwiseCiphers.set(WifiConfiguration.PairwiseCipher.TKIP);//获取使用WPA 方式的加密。
        wifiConfiguration.allowedPairwiseCiphers.set(WifiConfiguration.PairwiseCipher.CCMP);//获取使用WPA 方式的加密。
        wifiConfiguration.allowedProtocols.set(WifiConfiguration.Protocol.RSN);//设置安全协议
        return wifiConfiguration;
    }


    /**
     * function:判断便携热点是否开启
     *
     * @param context
     * @return
     */
    public static boolean isApOn(Context context) {
        WifiManager wifiManager = (WifiManager) context.getApplicationContext().getSystemService(Context.WIFI_SERVICE);
        try {

            //先通过反射拿到'判断热点是否开启'的方法，然后再将当前的wifiManager作为参数传递进去来判断当前移动设备是否开启热点
            Method method = wifiManager.getClass().getDeclaredMethod("isWifiApEnable"); //获取该方法，不问访问权限
            method.setAccessible(true);//获取访问权，并不是修改方法实际权限
            return (Boolean) method.invoke(wifiManager);
        } catch (NoSuchMethodException e) {
            e.printStackTrace();
        } catch (InvocationTargetException e) {
            e.printStackTrace();
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        }
        return false;
    }


    /**
     * function: 关闭wifi
     *
     * @param context 上下文
     */
    public static void closeWifi(Context context) {

        WifiManager wifiManager = (WifiManager) context.getApplicationContext().getSystemService(Context.WIFI_SERVICE);
        if (wifiManager.isWifiEnabled()) {
            wifiManager.setWifiEnabled(false);
        }
    }


    /***
     * function:获取开启便携热点后自身热点的ip地址
     *          dhcpInfo.serverAddress：所连接的wifi热点对应的ip地址
     *          dhcpInfo.ipAddress：本机的ip地址
     * @param context
     * @return
     */
    public static String getHotspotLocalIpAddress(Context context) {

        WifiManager wifiManager = (WifiManager) context.getApplicationContext().getSystemService(Context.WIFI_SERVICE);

        // 获取当前连接的信息
        DhcpInfo dhcpInfo = wifiManager.getDhcpInfo();
        if (dhcpInfo != null) {
            int address = dhcpInfo.serverAddress;
            return ((address & 0xFF)
                    + "." + ((address >> 8) & 0xFF)
                    + "." + ((address >> 16) & 0xFF)
                    + "." + ((address >> 24) & 0xFF));
        }
        return null;
    }

    /**
     * 关闭便携热点
     *
     * @param context 上下文
     */
    public static void closeAp(Context context) {
        WifiManager wifimanager = (WifiManager) context.getSystemService(context.WIFI_SERVICE);
        try {
            Method method = wifimanager.getClass().getMethod("setWifiApEnabled", WifiConfiguration.class, boolean.class);
            method.invoke(wifimanager, null, false);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}

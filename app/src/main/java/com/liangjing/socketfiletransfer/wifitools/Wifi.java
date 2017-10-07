package com.liangjing.socketfiletransfer.wifitools;

import android.content.Context;
import android.net.wifi.ScanResult;
import android.net.wifi.WifiConfiguration;
import android.net.wifi.WifiManager;
import android.text.TextUtils;
import android.util.Log;

import java.util.Collections;
import java.util.Comparator;
import java.util.List;

/**
 * Created by liangjing on 2017/9/29.
 * <p>
 * function:wifi功能相关操作--发送文件设备和接收文件设备的一些共同性操作
 */

public class Wifi {

    public static final ConfigurationSecurities configSec = ConfigurationSecurities.newInstance();

    private static final String BSSID_ANY = "any";

    private static final String TAG = "Wifi Connecter";

    /**
     * function:首先在用户已经配置过的wifi热点的集合中检查传进来的wifi热点是否有已经被用户配置过，若有则返回该wifi热点的网络配置类。
     * 其实也就是检查该wifi热点是否被用户配置过.
     * 　　　　　 首先应对该扫描到的wifi热点进行各项验证，如查看是否有ssid,bssiid等等。
     *
     * @param wifiMgr
     * @param hotspot         热点
     * @param hotspotSecurity 　热点的加密方式
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
     * function:首先在用户已经配置过的wifi热点的集合中检查传进来的wifi热点是否有已经被用户配置过，若有则返回该wifi热点的网络配置类。
     * 其实也就是检查该wifi热点是否被用户配置过.
     * 　　　　　 首先应对该扫描到的wifi热点进行各项验证，如查看是否有ssid,bssiid等等。
     *
     * @param wifiMnaager
     * @param wifiConfiguration
     * @param security
     * @return
     */
    public static WifiConfiguration getWifiConfiguration(final WifiManager wifiMnaager, final WifiConfiguration wifiConfiguration, String security) {
        final String ssid = wifiConfiguration.SSID;
        if (ssid.length() == 0) {
            return null;
        }

        final String bssid = wifiConfiguration.BSSID;


        if (security == null) {
            security = configSec.getWifiConfigurationSecurity(wifiConfiguration);
        }

        final List<WifiConfiguration> configurations = wifiMnaager.getConfiguredNetworks();

        for (final WifiConfiguration config : configurations) {
            if (config.SSID == null || !ssid.equals(config.SSID)) {
                continue;
            }
            if (config.BSSID == null || BSSID_ANY.equals(config.BSSID) || bssid == null || bssid.equals(config.BSSID)) {
                final String configSecurity = configSec.getWifiConfigurationSecurity(config);
                if (security.equals(configSecurity)) {
                    return config;
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


    /**
     * function:配置一个新的wifi热点(即对该未配置过的wifi热点进行配置),然后连接它
     *
     * @param context             上下文
     * @param wifiManager         WifiManager类
     * @param scanResult          该wifi热点
     * @param password            wifi密码
     * @param numOpenNetworksKept 最大可保存的开放网络数量
     * @return
     */
    public static boolean connectToNewNetwork(final Context context, final WifiManager wifiManager, final ScanResult scanResult, final String password, final int numOpenNetworksKept) {

        final String security = configSec.getScanResultSecurity(scanResult);

        //首先判断该wifi热点是否是开放的
        if (configSec.isOpenNetwork(security)) {
            //如果该wifi热点是开放的，那么将其保存下来
            checkForExcessOpenNetworkAndSave(wifiManager, numOpenNetworksKept);
        }

        //---若该wifi热点不是开放的，则执行以下操作

        //网络配置类
        WifiConfiguration config = new WifiConfiguration();
        config.SSID = convertToQuotedString(scanResult.SSID);
        config.BSSID = scanResult.BSSID;
        configSec.setupSecurity(config, security, password);

        //当前wifi热点所相对应的networkId(若id赋值成功，则该wifi热点存在于wifiManager.getConfiguredNetworks()列表中)
        int id = -1;
        try {
            //若是未连接过的wifi网络，networkId可以通过wifimanager的addNetwork方法获得，addNetwork方法的参数是一个wifi网络配置WifiConfiguration类
            id = wifiManager.addNetwork(config);
        } catch (NullPointerException e) {
            Log.e(TAG, "Weird!! Really!! What's wrong??", e);
        }

        if (id == -1) {
            return false;
        }

        if (!wifiManager.saveConfiguration()) {
            return false;
        }

        //检查该wifi热点是否已经被配置好噶
        config = getWifiConfiguration(wifiManager, config, security);
        if (config == null) {
            return false;
        }

        //连接该wifi热点(前提:用户已经配置好该wifi热点)
        return connectToConfiguredNetwork(context, wifiManager, config, true);
    }


    /**
     * function:连接已经配置好的wifi热点
     *
     * @param context
     * @param wifiManager
     * @param config
     * @param reassociate 即便连接没有准备好，是否也要连通
     * @return
     */
    public static boolean connectToConfiguredNetwork(final Context context, final WifiManager wifiManager, WifiConfiguration config, boolean reassociate) {

        if (Version.SDK >= 23) {
            return connectToConfiguredNetworkV23(context, wifiManager, config, reassociate);
        }

        //Android6.0版本以下的操作
        final String security = configSec.getWifiConfigurationSecurity(config);
        //该wifi热点的优先级(强度值)
        int oldPri = config.priority;
        //newPri为当前最大优先级(在已经配置好的wifi热点列表中)--目的是让当前连接上的wifi热点在已经配置好的wifi列表中的优先级最大
        int newPri = getMaxPriority(wifiManager) + 1;

        //如果NewPri的值超过了MAX_PRIORITY,那么将重新改变已经配置好的wifi列表中的各个wifi热点的优先级，并给newPri赋上新的值
        if (newPri > MAX_PRIORITY) {
            newPri = shiftPriorityAndSave(wifiManager);
            //检查该wifi热点是否配置好(是否存在于已经配置好的wifi列表中)
            config = getWifiConfiguration(wifiManager, config, security);
            if (config == null) {
                return false;
            }
        }


        // Set highest priority to this configured network
        config.priority = newPri;
        //更新现有配置网络的网络描述(若改变了某个wifi的WifiConfiguration(网络配置类)中的某个值，一定要记得调用updateNetwork()方法来进行更新当前网络配置描述)
        int networkId = wifiManager.updateNetwork(config);
        //如果返回的networkId值为-1,则表示出现错误
        if (networkId == -1) {
            return false;
        }


        //如果连接该wifi热点失败，那么将该wifi热点的优先级(强度值)恢复到原来的样子
        if (!wifiManager.enableNetwork(networkId, true)) {
            config.priority = oldPri;
            return false;
        }

        //如果保存当前wifi网络配置信息失败，那么将该wifi热点的优先级(强度值)恢复到原来的样子
        if (!wifiManager.saveConfiguration()) {
            config.priority = oldPri;
            return false;
        }

        // We have to retrieve the WifiConfiguration after save.(在保存网络配置信息后(saveConfiguration())，我们需要去检索该wifi的网络配置类是否存在)
        config = getWifiConfiguration(wifiManager, config, security);
        if (config == null) {
            return false;
        }

        //在wifi连接状态发送改变同时启用其余可用的(已配置好的)wifi热点,注意：启用不代表连接
        ReenableAllApsWhenNetworkStateChanged.schedule(context);

        // Disable others, but do not save.
        // Just to force the WifiManager to connect to it.
        if (!wifiManager.enableNetwork(config.networkId, true)) {
            return false;
        }

        final boolean connect = reassociate ? wifiManager.reassociate() : wifiManager.reconnect();
        //通过判断返回值，来检验操作是否成功。若成功，则connect的值为true
        if (!connect) {
            return false;
        }
        return true;
    }


    private static final int MAX_PRIORITY = 99999;

    /**
     * function:改变优先级并保存
     *
     * @param wifiManager
     * @return
     */
    private static int shiftPriorityAndSave(final WifiManager wifiManager) {
        final List<WifiConfiguration> configurations = wifiManager.getConfiguredNetworks();
        sortByPriority(configurations);
        final int size = configurations.size();
        for (int i = 0; i < size; i++) {
            final WifiConfiguration config = configurations.get(i);
            config.priority = i;
            //更新现有配置网络的网络描述。
            wifiManager.updateNetwork(config);
        }
        //保存配置信息
        wifiManager.saveConfiguration();
        return size;
    }

    /**
     * function:连接已经配置好的wifi热点(Android6.0以上版本)
     *
     * @param context
     * @param wifiManager
     * @param config
     * @param reassociate 即便连接没有准备好，是否也要连通
     * @return
     */
    private static boolean connectToConfiguredNetworkV23(final Context context, final WifiManager wifiManager, WifiConfiguration config, boolean reassociate) {

        //连接wifi需要调用wifimanager的enableNetwork方法，这个方法的第一个参数是需要连接wifi网络的networkId，第二个参数是指连接当前wifi网络是否需要断开其他网络
        if (!wifiManager.enableNetwork(config.networkId, true)) {
            return false;
        }

        //reassociate() 即便连接没有准备好，也要连通
        //reconnect() 如果连接准备好了，连通
        return reassociate ? wifiManager.reassociate() : wifiManager.reconnect();
    }


    /**
     * function:检查多余的开放网络并保存,确保没有超过numOpenNetworksKept
     *
     * @param wifiManager
     * @param numOpenNetworksKept
     * @return
     */
    private static boolean checkForExcessOpenNetworkAndSave(final WifiManager wifiManager, final int numOpenNetworksKept) {

        //获取已经配置过的wifi热点列表
        final List<WifiConfiguration> configurations = wifiManager.getConfiguredNetworks();
        //对热点信号强度进行排序
        sortByPriority(configurations);

        //检查开放热点数是否超过numOpenNetworksKept
        boolean modified = false;
        //统计已经配置过（已连接过）的wifi热点中有多少个是开放的(没有密码的)
        int tempCount = 0;

        for (int i = configurations.size() - 1; i >= 0; i--) {
            final WifiConfiguration config = configurations.get(i);
            if (configSec.isOpenNetwork(configSec.getWifiConfigurationSecurity(config))) {
                tempCount++;

                //此时若开放热点数已超过numOpenNetworksKept,则将该开放热点从configurations集合中移除
                if (tempCount >= numOpenNetworksKept) {
                    modified = true;
                    //从configurations集合中移除
                    wifiManager.removeNetwork(config.networkId);
                }
            }
        }
        if (modified) {
            //若modified为true,则表示已经配置过的wifi热点的列表有所改变，则需要调用WifiManager的saveConfiguration()方法来持久保存当前最新的wifi网络列表--note--保存网络配置信息
            return wifiManager.saveConfiguration();
        }
        return true;
    }


    /**
     * function:对热点信号强度进行排序
     *
     * @param configurations 已经配置过的wifi热点集合
     */
    private static void sortByPriority(final List<WifiConfiguration> configurations) {

        Collections.sort(configurations, new Comparator<WifiConfiguration>() {
            @Override
            public int compare(WifiConfiguration object1, WifiConfiguration object2) {
                return object1.priority - object2.priority;
            }
        });
    }


    /**
     * function:在已经配置好的wifi热点列表中查找出最高强度的wifi，并将其强度值赋给pri并返回
     *
     * @param wifiManager
     * @return
     */
    private static int getMaxPriority(final WifiManager wifiManager) {

        final List<WifiConfiguration> configurations = wifiManager.getConfiguredNetworks();
        int pri = 0;
        for (final WifiConfiguration wifiConfiguration : configurations) {
            if (wifiConfiguration.priority > pri) {
                pri = wifiConfiguration.priority;
            }
        }
        return pri;
    }
}

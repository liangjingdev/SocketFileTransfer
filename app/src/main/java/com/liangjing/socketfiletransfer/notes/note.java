package com.liangjing.socketfiletransfer.notes;

/**
 * Created by liangjing on 2017/10/6.
 *
 * function:笔记
 */

public class note {





    /*

    ---Settings.Secure类

    getInt

    添加在API 3级
    int getInt（ContentResolver cr，
    String name，
    int def）
    检索单个安全设置值作为整数的便利功能。请注意，内部设置值始终作为字符串存储; 此函数将字符串转换为您的整数。如果设置未定义或不是整数，则返回默认值。

    参数
    cr	ContentResolver：要访问的ContentResolver。
    name	String：要检索的设置的名称。
    def	int：如果未定义设置，则返回值。
    返回
    int	该设置的当前值，如果未定义，则为'def'或不为有效整数。*/


    /*

    ---WifiManager类

    saveConfiguration

            在API级别1中添加
    boolean saveConfiguration（）
    告诉请求者持久保存当前配置的网络列表。

    注意：此方法可能会更改现有网络的网络ID。调用此方法后，您应该假设网络ID可能不同。

    返回
    boolean	true 如果操作成功*/

   /*

   ---WifiManager类

   updateNetwork

            在API级别1中添加
    int updateNetwork（WifiConfiguration config）
    更新现有配置网络的网络描述。

    参数
    config	WifiConfiguration：描述配置的一组变量，包含在一个WifiConfiguration对象中。它可能是稀疏的，所以只有正在改变的项目是非null。该networkId字段必须设置为正在更新的现有网络的ID。
    返回
    int	返回成功networkId提供的 WifiConfiguration。 在故障时
    返回-1，包括当该networkId 字段WifiConfiguration不指代现有网络时。*/


    /*

    --WifiManager类

    enableNetwork

            在API级别1中添加
    boolean enableNetwork（int netId，
    boolean disableOthers）
    允许先前配置的网络与之关联。如果 disableOthers为真，则所有其他配置的网络都被禁用，并且尝试连接到所选网络。这可能导致状态更改事件的异步传递。

    注意：如果应用程序的目标SDK版本 LOLLIPOP或更新版本，即使Wi-Fi连接，网络通信也可能不使用Wi-Fi; 业务可以替代地通过另一个网络发送，例如蜂窝数据，蓝牙链接或以太网。例如，如果提供提供因特网接入的另一个网络（例如蜂窝数据）可用，流量将永远不会使用不提供因特网接入的Wi-Fi网络（例如，无线打印机）。需要确保他们的网络流量使用Wi-Fi应该使用的API，例如应用程序 bindSocket(java.net.Socket)， openConnection(java.net.URL)或者 bindProcessToNetwork(Network)这样做。

    参数
    netId	int：配置网络列表中网络的ID
    disableOthers	boolean：如果为true，请禁用所有其他网络。选择要连接的特定网络的方式是指定true 此参数。
    返回
    boolean	true 如果操作成功*/

}

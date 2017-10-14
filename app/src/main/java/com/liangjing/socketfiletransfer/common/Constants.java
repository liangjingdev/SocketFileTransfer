package com.liangjing.socketfiletransfer.common;

import com.liangjing.socketfiletransfer.bean.FileInfo;

import java.util.Comparator;
import java.util.Map;

/**
 * Created by liangjing on 2017/10/14.
 * <p>
 * function:公用常量数据、全局方法、静态变量、、、
 */

public class Constants {

    public static final String KEY_EXIT = "exit"; //退出
    public static final String KEY_IPPORT_INFO = "ipport_info"; //ip端口信息

    //最大尝试次数
    public static final int DEFAULT_TRY_COUNT = 10;

    //wifi连接成功时未分配的默认ip地址(若当前移动设备的ip地址显示为这个的话，则是不能够正常上网的)
    public static final String DEFAULT_UNKNOWN_IP = "0.0.0.0";

    //UDP通信服务端的默认端口号(Udp Socket通信--服务端所监听的端口号)
    public static final int DEFAULT_SERVER_UDP_PORT = 8204;

    //文件接收端监听的默认端口号(Tcp Socket通信--服务端所监听的端口号)
    public static final int DEFAULT_FILE_RECEIVE_SERVER_PORT = 8284;

    //UDP通知:发送端可以开始发送文件了
    public static final String MSG_START_SEND = "MSG_START_SEND";

    /**
     * UDP通知：文件接收端初始化完毕
     */
    public static final String MSG_FILE_RECEIVER_INIT_SUCCESS = "MSG_FILE_RECEIVER_INIT_SUCCESS";


    /**
     * function:将文件通过比较position值进行排序
     */
    public static final Comparator<Map.Entry<String, FileInfo>> DEFAULT_COMPARATOR = new Comparator<Map.Entry<String, FileInfo>>() {
        @Override
        public int compare(Map.Entry<String, FileInfo> o1, Map.Entry<String, FileInfo> o2) {
            if (o1.getValue().getPosition() > o2.getValue().getPosition()) {
                return 1;
            } else if (o1.getValue().getPosition() < o2.getValue().getPosition()) {
                return -1;
            } else {
                return 0;
            }
        }
    };

}

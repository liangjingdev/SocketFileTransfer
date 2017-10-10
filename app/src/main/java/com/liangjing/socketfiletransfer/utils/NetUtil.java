package com.liangjing.socketfiletransfer.utils;

import java.io.IOException;

/**
 * Created by liangjing on 2017/10/10.
 * <p>
 * function:网络工具类
 */

public class NetUtil {


    /***
     * function:判断是否可以ping通指定的ip地址
     *
     * waitFor()方法
     * 使当前线程等待，如果需要，直到该Process对象表示的进程已终止。如果子进程已经终止，此方法立即返回。如果子进程尚未终止，则调用线程将被阻止，直到子进程退出。
     * 返回 int--由该Process对象表示的子进程的退出值 。按照惯例，返回值0表示正常终止--则说明可以ping通该传入的ip地址。
     * @param ipAddress
     * @return
     */
    public static boolean pingIpAddress(String ipAddress) {
        try {
            Process mProcess = Runtime.getRuntime().exec("/system/bin/ping -c 1 -w 100" + ipAddress);
            int status = mProcess.waitFor();
            return status == 0;
        } catch (IOException e) {
            e.printStackTrace();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        return false;
    }
}

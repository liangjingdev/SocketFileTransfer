package com.liangjing.socketfiletransfer.base;

import android.app.Application;

import com.liangjing.socketfiletransfer.bean.FileInfo;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

/**
 * Created by liangjing on 2017/10/9.
 * <p>
 * function:自定义Application类(采用单例模式创建--每个Android App运行时，会首先自动创建Application 类并实例化 Application 对象，且只有一个),
 * 作用:统一管理全局资源。
 */

public class AppContext extends Application {


    //App全局上下文
    private static AppContext mInstance;

    //主线程池
    public static Executor MAIN_EXECUTOR = Executors.newFixedThreadPool(5);

    //文件发送端单线程
    public static Executor FILE_SENDER_EXECUTOR = Executors.newSingleThreadExecutor();

    //待发送的文件数据(该Map集合中的key为该文件在发送端中的存储路径)--接收端所需要接收到的文件数据
    public Map<String, FileInfo> mSendFileInfoMap = new HashMap<>();

    //接收到的文件数据
    public Map<String, FileInfo> mReceivedFileInfoMap = new HashMap<>();

    @Override
    public void onCreate() {
        super.onCreate();
        mInstance = this;
    }


    /**
     * function:获取Application全局变量
     *
     * @return
     */
    public static AppContext getAppContext() {
        return mInstance;
    }


    /**************************************************************************************
     ********************************************发送端************************************
     **************************************************************************************/

    /**
     * function:删除待发送的文件map(所有文件数据)
     */
    public void clearSendFileInfoMap() {
        mSendFileInfoMap.clear();
    }

    /**
     * function:获取待发送的文件Map
     * Map.Entry<String,FileInfo>--其中的一个条目
     * mSendFileInfoMap.entrySet()--该map集合的所有条目
     *
     * @return
     */
    public List<Map.Entry<String, FileInfo>> getSendFileInfoMap() {
        List<Map.Entry<String, FileInfo>> fileInfoMapList = new ArrayList<>(mSendFileInfoMap.entrySet());
        return fileInfoMapList;
    }

    /**
     * function:获取待发送文件的总长度(大小)
     *
     * @return
     */
    public long getAllSendFileInfoSize() {
        long totalSize = 0;
        for (FileInfo fileInfo : mSendFileInfoMap.values()) {
            if (fileInfo != null) {
                totalSize += fileInfo.getSize();
            }
        }
        return totalSize;
    }


    /**
     * function:添加FileInfo到待发送的文件数据列表中--mSendFileInfoMap
     *
     * @param fileInfo
     */
    public void addSendFileInfo(FileInfo fileInfo) {
        if (!mSendFileInfoMap.containsKey(fileInfo.getFilePath())) {
            mSendFileInfoMap.put(fileInfo.getFilePath(), fileInfo);
        }
    }


    /**
     * function:更新FileInfo(当有一个文件发送成功或失败时就会回调该方法)
     *
     * @param fileInfo
     */
    public void updateSendFileInfo(FileInfo fileInfo) {
        mSendFileInfoMap.put(fileInfo.getFilePath(), fileInfo);
    }


    /**************************************************************************************
     ********************************************接收端************************************
     **************************************************************************************/

    /**
     * function:删除接收到的文件map(所有文件数据)--清空文件列表
     */
    public void clearReceivedFileInfoMap() {
        mReceivedFileInfoMap.clear();
    }

    /**
     * function:获取接收到的文件map(所有文件数据)
     *
     * @return
     */
    public List<Map.Entry<String, FileInfo>> getReceivedFileInfoMap() {
        List<Map.Entry<String, FileInfo>> fileInfoList = new ArrayList<>(mReceivedFileInfoMap.entrySet());
        return fileInfoList;
    }

    /**
     * function:获取接收到的文件的总长度
     *
     * @return
     */
    public long getAllReceivedFileInfoSize() {
        long totalSize = 0;
        for (FileInfo fileInfo : mReceivedFileInfoMap.values()) {
            if (fileInfo != null) {
                totalSize += fileInfo.getSize();
            }
        }
        return totalSize;
    }

    /**
     * function:添加FileInfo到待接收的文件数据列表中
     *
     * @param fileInfo
     */
    public void addReceivedFileInfo(FileInfo fileInfo) {
        if (!mReceivedFileInfoMap.containsKey(fileInfo.getFilePath())) {
            mReceivedFileInfoMap.put(fileInfo.getFilePath(), fileInfo);
        }
    }


    /**
     * function:更新FileInfo
     *
     * @param fileInfo
     */
    public void updateReceivedFileInfo(FileInfo fileInfo) {
        mReceivedFileInfoMap.put(fileInfo.getFilePath(), fileInfo);
    }

}

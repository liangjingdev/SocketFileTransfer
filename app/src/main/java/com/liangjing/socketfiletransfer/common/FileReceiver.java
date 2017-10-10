package com.liangjing.socketfiletransfer.common;

import com.liangjing.socketfiletransfer.base.BaseTransfer;
import com.liangjing.socketfiletransfer.bean.FileInfo;
import com.liangjing.socketfiletransfer.utils.FileUtil;
import com.liangjing.socketfiletransfer.utils.LogUtil;

import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;

/**
 * Created by liangjing on 2017/10/10.
 * <p>
 * function:文件接收端相关文件传输操作--实现了Runnable接口--即使实现了Runnable接口，那也无法启动线程，必须依托其他类。--所以实现Runnable接口后，还需要通过使用Thread类来启动
 */

public class FileReceiver extends BaseTransfer implements Runnable {


    //接收文件的Socket以及Socket输入流
    private Socket mSocket;
    private InputStream mInputStream;

    //待接收的文件数据
    private FileInfo mFileInfo;

    /**
     * 用来控制线程暂停、恢复
     */
    private final Object LOCK = new Object();
    private boolean mIsPaused = false;

    /**
     * 设置未执行线程的不执行标识
     */
    private boolean mIsStop;

    /**
     * 该线程是否执行完毕
     */
    private boolean mIsFinish;

    /**
     * 文件接收监听事件
     */
    private OnReceiveListener mOnReceiveListener;


    public FileReceiver(Socket socket, FileInfo fileInfo) {
        mSocket = socket;
        mFileInfo = fileInfo;
    }

    /**
     * 设置接收监听事件
     *
     * @param onReceiveListener
     */
    public void setOnReceiveListener(OnReceiveListener onReceiveListener) {
        mOnReceiveListener = onReceiveListener;
    }

    @Override
    public void init() throws Exception {
        if (mSocket != null) {
            //获取Socket输入流
            mInputStream = mSocket.getInputStream();
        }
    }

    @Override
    public void parseBody() throws Exception {

        if (mFileInfo == null) {
            return;
        }

        long fileSize = mFileInfo.getSize();
        //FileUtil.generateLocalFile(mFileInfo.getFilePath())--生成一个File对象--具体看FileUtil类,将Socket输入流中的字节数据通过该输出流写入到指定的文件中
        OutputStream outputStream = new FileOutputStream(FileUtil.generateLocalFile(mFileInfo.getFilePath()));

        byte[] bytes = new byte[BYTE_SIZE_DATA];
        long total = 0;
        int len = 0;

        long sTime = System.currentTimeMillis();
        long eTime = 0;
        while ((len = mInputStream.read(bytes)) != -1) {
            synchronized (LOCK) {
                if (mIsPaused) {
                    try {
                        LOCK.wait();
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }

                //将字节数据写入到文件中
                outputStream.write(bytes, 0, len);
                total = total + len;

                //每隔200毫秒返回一次进度
                eTime = System.currentTimeMillis();
                if (eTime - sTime > 200) {
                    sTime = eTime;
                    if (mOnReceiveListener != null) {
                        mOnReceiveListener.onProgress(mFileInfo, total, fileSize);
                    }
                }
            }
        }


        //文件接收成功
        if (mOnReceiveListener != null) {
            mOnReceiveListener.onSuccess(mFileInfo);
        }
        mIsFinish = true;
    }

    @Override
    public void finishTransfer() throws Exception {
        if (mInputStream != null) {
            try {
                mInputStream.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        if (mSocket != null && mSocket.isConnected()) {
            try {
                mSocket.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    @Override
    public void run() {
        if (mIsStop) {
            return;
        }

        //初始化
        try {
            if (mOnReceiveListener != null) {
                mOnReceiveListener.onStart();
            }
            init();
        } catch (Exception e) {
            e.printStackTrace();
            LogUtil.i("FileReceiver init() ------->>> occur expection");
            if (mOnReceiveListener != null) {
                mOnReceiveListener.onFailure(e, mFileInfo);
            }
        }

        //发送文件实体数据
        try {
            parseBody();
        } catch (Exception e) {
            e.printStackTrace();
            LogUtil.i("FileReceiver parseBody() ------->>> occur expection");
            if (mOnReceiveListener != null) {
                mOnReceiveListener.onFailure(e, mFileInfo);
            }
        }

        //文件传输完毕
        try {
            finishTransfer();
        } catch (Exception e) {
            e.printStackTrace();
            LogUtil.i("FileReceiver finishTransfer() ------->>> occur expection");
            if (mOnReceiveListener != null) {
                mOnReceiveListener.onFailure(e, mFileInfo);
            }
        }
    }


    /**
     * 暂停接收线程
     */
    public void pause() {
        synchronized (LOCK) {
            mIsPaused = true;
            LOCK.notifyAll();
        }
    }

    /**
     * 恢复接收线程
     */
    public void resume() {
        synchronized (LOCK) {
            mIsPaused = false;
            LOCK.notifyAll();
        }
    }

    /**
     * 设置当前的接收任务不执行
     */
    public void stop() {
        mIsStop = true;
    }

    /**
     * 文件是否在接收中
     *
     * @return
     */
    public boolean isRunning() {
        return !mIsFinish;
    }


    /**
     * 文件接收监听事件
     */
    public interface OnReceiveListener {
        void onStart();

        void onProgress(FileInfo fileInfo, long progress, long total);

        void onSuccess(FileInfo fileInfo);

        void onFailure(Throwable throwable, FileInfo fileInfo);
    }
}

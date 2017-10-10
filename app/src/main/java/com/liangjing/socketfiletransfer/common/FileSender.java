package com.liangjing.socketfiletransfer.common;

import android.content.Context;

import com.liangjing.socketfiletransfer.base.BaseTransfer;
import com.liangjing.socketfiletransfer.bean.FileInfo;
import com.liangjing.socketfiletransfer.utils.LogUtil;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;

/**
 * Created by liangjing on 2017/10/10.
 * <p>
 * function:文件发送端相关文件传输操作--实现了Runnable接口--即使实现了Runnable接口，那也无法启动线程，必须依托其他类。--所以实现Runnable接口后，还需要通过使用Thread类来启动
 */

public class FileSender extends BaseTransfer implements Runnable {

    //上下文
    private Context mContext;

    //待发送的文件数据
    private FileInfo mFileInfo;

    //传送文件的Socket以及Socket的输出流以及IO流
    private Socket mSocket;
    private OutputStream os;
    private OutputStream mOutputStream;

    //用来控制线程暂停、恢复
    private final Object LOCK = new Object();
    private boolean mIsPause;

    //判断该线程是否执行完毕
    private boolean mIsFinish;

    //设置未执行线程的不执行标识
    private boolean mIsStop;

    //文件传送监听事件
    private OnSendListener mOnSendListener;


    public FileSender(Context context, FileInfo fileInfo, Socket socket) {
        mContext = context;
        mFileInfo = fileInfo;
        mSocket = socket;
    }

    /**
     * function:设置发送文件监听事件
     *
     * @param onSendListener
     */
    public void setOnSendListener(OnSendListener onSendListener) {
        mOnSendListener = onSendListener;
    }


    /**
     * function:初始化操作
     *
     * @throws Exception
     */
    @Override
    public void init() throws Exception {
        //超时设置
        mSocket.setSoTimeout(30 * 1000);
        //获取Socket对象的输出流(发送文件数据)
        os = mSocket.getOutputStream();
        //得到缓冲输出流
        mOutputStream = new BufferedOutputStream(os);
    }


    /**
     * function:发送文件实体数据
     *
     * @throws Exception
     */
    @Override
    public void parseBody() throws Exception {
        //文件大小
        long fileSize = mFileInfo.getSize();
        //通过文件路径生成File对象
        File file = new File(mFileInfo.getFilePath());
        //拿到File对象之后，可通过FileInputStream读取该文件，返回与该文件相对应的输入流对象
        InputStream inputStream = new FileInputStream(file);

        int len = 0;
        long total = 0;  //文件当前被读取到的字节数(也就是发送端当前发送出去的字节数)
        byte[] bytes = new byte[BYTE_SIZE_DATA];

        long sTime = System.currentTimeMillis();
        long eTime = 0;

        while ((len = inputStream.read(bytes)) != -1) {
            synchronized (LOCK) {
                if (mIsPause) {
                    try {
                        //让当前该线程进入堵塞状态,等待其它操作完成之后再进行通路
                        LOCK.wait();
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }

                //将文件数据逐字节写入到Socket输出流,进行传送
                mOutputStream.write(bytes, 0, len);
                total += len;

                //每隔200毫秒返回一次进度
                eTime = System.currentTimeMillis();
                if (eTime - sTime > 200) {
                    sTime = eTime;
                    if (mOnSendListener != null) {
                        mOnSendListener.onProgress(total, fileSize);
                    }
                }

            }
        }


        // 关闭Socket输出流
        mSocket.shutdownOutput();
        os.close();


        //关闭IO流--输出流
        mOutputStream.flush();
        mOutputStream.close();

        //关闭Socket
        mSocket.close();

        //文件发送成功
        if (mOnSendListener != null) {
            mOnSendListener.onSuccess(mFileInfo);
        }
        mIsFinish = true;
    }


    /**
     * function:文件传输完毕(发送完毕)
     * 检查Socket输出流、IO流以及Socket是否已经关闭
     *
     * @throws Exception
     */
    @Override
    public void finishTransfer() throws Exception {

        if (os != null) {
            try {
                os.close();
            } catch (IOException e) {
                e.printStackTrace();
            }


            if (mOutputStream != null) {
                try {
                    mOutputStream.close();
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
    }

    @Override
    public void run() {
        if (mIsStop) {
            return;
        }

        //初始化操作回调接口
        if (mOnSendListener != null) {
            mOnSendListener.onStart();
        }

        try {
            //初始化Socket以及IO流
            init();
        } catch (Exception e) {
            e.printStackTrace();
            LogUtil.i("FileSender init() ------->>> occur expection");
            if (mOnSendListener != null) {
                mOnSendListener.onFailure(e, mFileInfo);
            }
        }

        //发送文件实体数据
        try {
            parseBody();
        } catch (Exception e) {
            e.printStackTrace();
            LogUtil.i("FileSender parseBody() ------->>> occur expection");
            if (mOnSendListener != null) {
                mOnSendListener.onFailure(e, mFileInfo);
            }
        }

        //文件传输完毕
        try {
            finishTransfer();
        } catch (Exception e) {
            e.printStackTrace();
            LogUtil.i("FileSender finishTransfer() ------->>> occur expection");
            if (mOnSendListener != null) {
                mOnSendListener.onFailure(e, mFileInfo);
            }
        }
    }


    /**
     * function:使该发送文件的线程暂停
     */
    public void pause() {
        synchronized (LOCK) {
            mIsPause = true;
            LOCK.notifyAll();
        }
    }

    /**
     * function:恢复该发送文件的线程
     */
    public void resume() {
        synchronized (LOCK) {
            mIsPause = false;
            LOCK.notifyAll();
        }
    }


    /**
     * function:设置当前的发送任务不执行
     */
    public void Stop() {
        mIsStop = true;
    }

    /**
     * function:判断当前文件是否在发送当中
     *
     * @return
     */
    public boolean isRunning() {
        return !mIsFinish;
    }

    //回调接口
    public interface OnSendListener {
        void onStart();

        void onProgress(long progress, long total);

        void onSuccess(FileInfo fileInfo);

        void onFailure(Throwable throwable, FileInfo fileInfo);
    }


}

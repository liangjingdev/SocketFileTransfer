package com.liangjing.socketfiletransfer.activity;

import android.content.DialogInterface;
import android.content.IntentFilter;
import android.os.Build;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.support.v7.widget.DividerItemDecoration;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.View;
import android.view.ViewStub;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.liangjing.socketfiletransfer.R;
import com.liangjing.socketfiletransfer.base.AppContext;
import com.liangjing.socketfiletransfer.base.BaseActivity;
import com.liangjing.socketfiletransfer.bean.FileInfo;
import com.liangjing.socketfiletransfer.common.Constants;
import com.liangjing.socketfiletransfer.common.FileSender;
import com.liangjing.socketfiletransfer.receiver.HotspotBroadcastReceiver;
import com.liangjing.socketfiletransfer.utils.FileUtil;
import com.liangjing.socketfiletransfer.utils.LogUtil;
import com.liangjing.socketfiletransfer.wifitools.ApManager;
import com.zhy.adapter.recyclerview.CommonAdapter;
import com.zhy.adapter.recyclerview.base.ViewHolder;

import java.io.File;
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import butterknife.BindView;

/**
 * function:发送文件activity(利用Udp Socket通信来发送文件列表以及一些指令，再利用Tcp Socket通信来发送接收端需要接收到的那些文件)
 * <p>
 * 发送端首先将所有能够发送出去的文件的文件列表传送给接收端，然后接收端经过筛选后将其想要接收的那些文件的文件列表告知给发送端，然后发送端开始向接收端
 * 发送这些它需要的文件。
 * <p>
 * 注意：结束该activity时一定要记得回收掉相关资源，避免内存泄漏.
 */
public class SendFilesActivity extends BaseActivity {


    //更新进度条
    public static final int MSG_UPDATE_PROGRESS = 0x661;

    //接收端初始化成功
    public static final int MSG_FILE_RECEIVER_INIT_SUCCESS = 0x663;

    //设置当前处于什么状态(如：正在等待连接/成功发送文件列表等。)
    public static final int MSG_SET_STATUS = 0x664;

    /**
     * 更新列表适配器
     */
    public static final int MSG_UPDATE_ADAPTER = 0x662;

    @BindView(R.id.tv_send_files_status)
    TextView tvStatus;

    @BindView(R.id.vs_send_files_open_hotspot)
    ViewStub vsOpenHotspot;

    @BindView(R.id.rv_send_files)
    RecyclerView mSendFileRecyclerView;

    //热点名称以及密码
    private EditText etSSID;
    private EditText etPassword;
    //开启热点按钮
    private Button btnOpenHotspot;

    //便携热点状态接收器
    private HotspotBroadcastReceiver mHotspotBroadcastReceiver;

    //UDP Socket
    private DatagramSocket mDatagramSocket;

    //文件发送Runnable接口--在子线程中去执行
    private SenderServerRunnable mSenderServerRunnable;

    //发送端所有可发送的文件的列表
    private List<FileInfo> mAllFileInfos = new ArrayList<>();

    //发送文件线程列表数据（mFileSenderList集合中包含了许多待执行的Runnable接口--FileSender,FileSender--里面包含了发送某个文件的详细操作）
    private List<FileSender> mFileSenderList = new ArrayList<>();

    //判断获取权限是否成功
    private boolean mIsPermissionGranted;

    //判断是否初始化成功
    private boolean mIsInitialized;

    /**
     * 发送文件列表适配器
     */
    private CommonAdapter<Map.Entry<String, FileInfo>> mSendFileAdapter;

    @Override
    protected int getLayoutId() {
        return R.layout.activity_send_files_layout;
    }

    @Override
    protected String getTitleText() {
        return "发送文件";
    }

    /**
     * function:初始化数据
     */
    @Override
    protected void initData() {

        //假装添加文件
        String file1 = Environment.getExternalStorageDirectory() + File.separator + "2.rar";
        String file2 = Environment.getExternalStorageDirectory() + File.separator + "test.jpg";

        try {
            //首先通过文件路径来获得相对应的文件实体类。
            FileInfo fileInfo1 = new FileInfo(1, file1, FileUtil.getFileSize(new File(file1)));
            FileInfo fileInfo2 = new FileInfo(2, file2, FileUtil.getFileSize(new File(file2)));
            //然后将这些将要发送的文件实体类加入到待发送的文件列表中
            mAllFileInfos.add(fileInfo1);
            mAllFileInfos.add(fileInfo2);
        } catch (Exception e) {
            e.printStackTrace();
        }

        //请求权限，开启热点
        requestPermission(PERMISSION_CREATE_HOSPOT, PERMISSION_REQ_CREATE_HOTSPOT);
        //初始化-开启热点的视图
        initOpenHotspotLayout();
    }


    /**
     * function:初始化-开启热点的视图
     */
    private void initOpenHotspotLayout() {
        View view = vsOpenHotspot.inflate();
        etSSID = (EditText) view.findViewById(R.id.et_open_hotspot_ssid);
        etPassword = (EditText) view.findViewById(R.id.et_open_hotspot_password);
        btnOpenHotspot = (Button) view.findViewById(R.id.btn_open_hotspot);

        //按钮监听事件--开启热点
        btnOpenHotspot.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                openHotspot();
            }
        });
    }


    /**
     * function:开启便携热点
     */
    private void openHotspot() {
        String ssid = etSSID.getText().toString().trim();
        String password = etPassword.getText().toString().trim();
        if (isEmptyString(ssid)) {
            //若ssid输入框为空，则以手机型号为ssid
            ssid = Build.MODEL;
        }

        //判断是否有权限
        if (mIsPermissionGranted) {
            //注意：开启热点前，先关闭wifi,如有其它热点已开启，先关闭
            ApManager.closeWifi(getContext());
            if (ApManager.isApOn(getContext())) {
                ApManager.closeAp(getContext());
            }

            //注册便携热点状态接收器
            registerHotspotReceiver();

            boolean isSuccess = ApManager.openAp(getContext(), ssid, password);
            if (!isSuccess) {
                setStatus("创建热点失败");
            }
        } else {
            showTipsDialog("获取权限失败，开启热点", new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    finish();
                }
            });
        }
    }


    /**
     * function:文件发送Runnable接口--在某个子线程中去执行
     */
    private class SenderServerRunnable implements Runnable {

        //Tcp Socket 通信 -- 发送文件给接收端
        private ServerSocket mServerSocket;

        @Override
        public void run() {
            try {
                //获取待发送的文件列表数据，按position索引排序
                List<Map.Entry<String, FileInfo>> fileInfoList = AppContext.getAppContext().getSendFileInfoMap();
                Collections.sort(fileInfoList, Constants.DEFAULT_COMPARATOR);
                mServerSocket = new ServerSocket(Constants.DEFAULT_FILE_RECEIVE_SERVER_PORT); //(Tcp Socket通信--服务端所监听的端口号)
                //逐个文件进行发送
                for (final Map.Entry<String, FileInfo> fileInfoMap : fileInfoList) {
                    final FileInfo fileInfo = fileInfoMap.getValue();
                    Socket socket = mServerSocket.accept();
                    //首先获取到该文件对应的FileSender对象,FileSender--进行文件发送操作
                    FileSender fileSender = new FileSender(getContext(), fileInfo, socket);
                    //添加监听器
                    fileSender.setOnSendListener(new FileSender.OnSendListener() {
                        @Override
                        public void onStart() {
                            mHandler.obtainMessage(MSG_SET_STATUS, "开始发送" + FileUtil.getFileName(fileInfo.getFilePath())).sendToTarget();
                        }

                        @Override
                        public void onProgress(long progress, long total) {
                            //更新发送进度视图
                            int i_progress = (int) (progress * 100 / total);
                            LogUtil.e("正在发送：" + fileInfo.getFilePath() + "\n当前进度：" + i_progress);

                            Message msg = new Message();
                            msg.what = MSG_UPDATE_PROGRESS;
                            msg.arg1 = fileInfo.getPosition();
                            msg.arg2 = i_progress;
                            mHandler.sendMessage(msg);
                        }

                        @Override
                        public void onSuccess(FileInfo fileInfo) {
                            //发送成功
                            mHandler.obtainMessage(MSG_SET_STATUS, "文件：" + FileUtil.getFileName(fileInfo.getFilePath()) + "发送成功").sendToTarget();
                            fileInfo.setResult(FileInfo.FLAG_SUCCESS);
                            AppContext.getAppContext().updateSendFileInfo(fileInfo);

                            Message msg = new Message();
                            msg.what = MSG_UPDATE_PROGRESS;
                            msg.arg1 = fileInfo.getPosition();
                            msg.arg2 = 100;
                            mHandler.sendMessage(msg);
                        }

                        @Override
                        public void onFailure(Throwable throwable, FileInfo fileInfo) {
                            //发送失败
                            mHandler.obtainMessage(MSG_SET_STATUS, "文件：" + FileUtil.getFileName(fileInfo.getFilePath()) + "发送失败").sendToTarget();
                            fileInfo.setResult(FileInfo.FLAG_FAILURE);
                            AppContext.getAppContext().updateSendFileInfo(fileInfo);

                            Message msg = new Message();
                            msg.what = MSG_UPDATE_PROGRESS;
                            msg.arg1 = fileInfo.getPosition();
                            msg.arg2 = -1;//表示文件发送失败
                            mHandler.sendMessage(msg);
                        }
                    });
                    //然后将该FileSender对象(Runnable接口)添加到线程池中去执行
                    mFileSenderList.add(fileSender);
                    AppContext.FILE_SENDER_EXECUTOR.execute(fileSender);
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        /**
         * function:关闭socket连接
         */
        public void closeServerSocket() {
            if (mServerSocket != null) {
                try {
                    mServerSocket.close();
                    mServerSocket = null;
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }


    /**
     * function:权限请求成功
     *
     * @param requestCode
     */
    @Override
    protected void permissionSuccess(int requestCode) {
        super.permissionSuccess(requestCode);

        //通过requestCode来判断是连接wifi的权限还是开启热点的权限
        if (requestCode == PERMISSION_REQ_CREATE_HOTSPOT) {
            //获取创建便携热点权限成功
            mIsPermissionGranted = true;
        }
    }

    /**
     * function:权限请求失败
     *
     * @param requestCode
     */
    @Override
    protected void permissionFail(int requestCode) {
        super.permissionFail(requestCode);

        if (requestCode == PERMISSION_REQ_CREATE_HOTSPOT) {
            //获取创建便携热点权限失败
            mIsPermissionGranted = false;
        }
    }

    @Override
    protected void onPause() {
        super.onPause();

        if (mHotspotBroadcastReceiver != null) {
            //反注册便携热点状态接收器
            unregisterHotSpotReceiver();
        }
    }

    /**
     * function:反注册便携热点状态接收器
     */
    private void unregisterHotSpotReceiver() {
        if (mHotspotBroadcastReceiver != null) {
            unregisterReceiver(mHotspotBroadcastReceiver);
            mHotspotBroadcastReceiver = null; //将对象置为空
        }
    }

    @Override
    protected void onResume() {
        super.onResume();

        if (mIsPermissionGranted && mHotspotBroadcastReceiver == null) {
            //注册便携热点状态接收器
            registerHotspotReceiver();
        }
    }


    /**
     * function:注册便携热点状态和接收器
     */
    private void registerHotspotReceiver() {
        if (mHotspotBroadcastReceiver == null) {
            mHotspotBroadcastReceiver = new HotspotBroadcastReceiver() {
                @Override
                public void onHotSpotEnabled() {
                    //热点开启成功
                    if (!mIsInitialized) {
                        //表示初始化成功
                        mIsInitialized = true;
                        setStatus("成功开启热点...");

                        //设置定时器，改变状态表示--设置为:正在等待连接...
                        tvStatus.postDelayed(new Runnable() {
                            @Override
                            public void run() {
                                setStatus("正在等待连接...");

                                //等待接收端连接--获取Runnable接口
                                Runnable mUdpServerRunnable = receiveInitSuccessOrderRunnable();
                                //让该Runnable接口的内容在主线程池中执行
                                AppContext.MAIN_EXECUTOR.execute(mUdpServerRunnable);
                            }
                        }, 2000);
                    }
                }
            };

            //注册系统广播接收器
            IntentFilter filter = new IntentFilter(HotspotBroadcastReceiver.ACTION_HOTSPOT_STATE_CHANGED);
            registerReceiver(mHotspotBroadcastReceiver, filter);
        }
    }


    /**
     * function:等待接收端发送初始化完成指令的线程
     *
     * @return
     */
    private Runnable receiveInitSuccessOrderRunnable() {
        return new Runnable() {
            @Override
            public void run() {

                try {
                    //开始接收接收端发来的指令
                    receiveInitSuccessOrder(Constants.DEFAULT_SERVER_UDP_PORT);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        };
    }

    /**
     * function:等待接收端发送初始化完成的指令，然后再向其发送文件列表(Udp Socket通信)--实现
     * <p>
     * 参数：serverPort:UDP通信服务端默认端口号
     */
    private void receiveInitSuccessOrder(int serverPort) throws Exception {
        //确保wifi连接后获取到正确的ip地址
        int tryCount = 0; // 重试次数
        String localIpAddress = ApManager.getHotspotLocalIpAddress(getContext());
        while (localIpAddress.equals(Constants.DEFAULT_UNKNOWN_IP) && tryCount < Constants.DEFAULT_TRY_COUNT) {
            Thread.sleep(1000);
            localIpAddress = ApManager.getHotspotLocalIpAddress(getContext());
            tryCount++;
        }

        //这里使用UDP发送和接收指令
        mDatagramSocket = new DatagramSocket(serverPort);
        //while(true)--目的：使得服务端能够持续监听该端口
        while (true) {
            byte[] receiveData = new byte[1024];
            DatagramPacket receivePacket = new DatagramPacket(receiveData, receiveData.length); //接收数据包
            String response = new String(receivePacket.getData()).trim(); //转化为字符串形式
            if (isNotEmptyString(response)) {
                LogUtil.e("接收到的消息 -------->>>" + response);
                if (response.equals(Constants.MSG_FILE_RECEIVER_INIT_SUCCESS)) {
                    //利用Handler发送初始化成功指令，后续交由handleMessage()来进行处理 (异步通信机制)。
                    mHandler.sendEmptyMessage(MSG_FILE_RECEIVER_INIT_SUCCESS);
                    //既然接收端初始化完成了，那么接下来向其发送文件列表(文件列表--显示接收端能够进行接收的那些文件)
                    InetAddress inetAddress = receivePacket.getAddress(); // 获取Udp Socket通信中--接收端(客户端)的ip地址
                    int port = receivePacket.getPort(); //获取Udp Socket通信中--接收端的端口号(客户端监听的端口号)
                    //向接收端发送文件列表
                    sendFileInfoListToReceiverWithUDP(inetAddress, port);
                } else if (response.equals(Constants.MSG_START_SEND)) {
                    //发送端接收到接收端发过来的'开始发送指令'--则发送端开始向接收端发送其需要的文件
                    initSenderServer();
                } else {
                    //接收端发过来的待发送文件列表(即接收端想要接收到的那些文件)
                    parseFileInfo(response);
                }
            }
        }
    }


    /**
     * function:将json字符串解析成FileInfo(该json字符串是一个数组形式，表示的是接收端需要接收的那些文件)
     */
    private void parseFileInfo(String jsonStr) {
        if (isNotEmptyString(jsonStr)) {
            List<FileInfo> fileInfoList = FileInfo.toObjectList(jsonStr);
            if (!isEmptyList(fileInfoList)) {
                for (FileInfo fileInfo : fileInfoList) {
                    if (fileInfo != null && isNotEmptyString(fileInfo.getFilePath())) {
                        //设置该文件在列表中的位置
                        fileInfo.setPosition(fileInfoList.indexOf(fileInfo));
                        //添加FileInfo到待发送的文件数据列表中--mSendFileInfoMap
                        AppContext.getAppContext().addSendFileInfo(fileInfo);
                        mHandler.sendEmptyMessage(MSG_UPDATE_ADAPTER); //更新列表适配器
                    }
                }
            }
        }
    }


    /**
     * function:初始化发送端的发送服务，开始发送文件给服务端
     */
    private void initSenderServer() {
        mSenderServerRunnable = new SenderServerRunnable();
        //在一个新的线程中去执行该Runnable中的内容
        new Thread(mSenderServerRunnable).start();
    }


    /**
     * function:向接收端发送文件列表(Udp Socket通信)
     *
     * @param ipAddress  IP地址
     * @param serverPort 端口号
     */
    private void sendFileInfoListToReceiverWithUDP(InetAddress ipAddress, int serverPort) {
        if (!isEmptyList(mAllFileInfos)) {
            String jsonStr = FileInfo.toJsonStr(mAllFileInfos); //首先转化为json字符串，然后将json字符串转化为对应的字节数组来进行发送出去
            DatagramPacket sendFileInfoPacket = new DatagramPacket(jsonStr.getBytes(), jsonStr.getBytes().length, ipAddress, serverPort);
            try {
                //发送文件列表
                mDatagramSocket.send(sendFileInfoPacket);
                LogUtil.i("发送消息 --------->>>" + jsonStr + "=== Success!");
                mHandler.obtainMessage(MSG_SET_STATUS, "成功发送文件列表...").sendToTarget(); //获得返回的Message对象并设置其what字段和obj字段，然后将该消息发送出去处理
            } catch (IOException e) {
                e.printStackTrace();
                LogUtil.i("发送消息 --------->>>" + jsonStr + "=== 失败！");
            }
        }
    }


    /**
     * function:Handler对象--处理消息
     */
    private Handler mHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);

            if (msg.what == MSG_UPDATE_PROGRESS) {
                //更新文件发送进度
                int position = msg.arg1; //该文件在发送列表中的索引值
                int progress = msg.arg2; //该文件发送进度值
                if (position >= 0 && position < mSendFileAdapter.getItemCount()) {
                    updateProgress(position, progress);
                }
            } else if (msg.what == MSG_UPDATE_ADAPTER) {
                //更新列表适配器
                initSendFilesLayout();
            } else if (msg.what == MSG_SET_STATUS) {
                //设置当前状态(msg.obj字段的值转化为字符串形式再展示出来)
                setStatus(msg.obj.toString());
            } else if (msg.what == MSG_FILE_RECEIVER_INIT_SUCCESS) {
                //接收端初始化完毕
                setStatus("接收端初始化成功...");
                //显示发送文件视图
                initSendFilesLayout();
            }
        }
    };

    /**
     * function:初始化发送文件视图
     */
    private void initSendFilesLayout() {
        vsOpenHotspot.setVisibility(View.GONE);//设置开启热点视图为不可见状态
        mSendFileRecyclerView.setVisibility(View.VISIBLE);

        //设置适配器
        List<Map.Entry<String, FileInfo>> fileInfos = AppContext.getAppContext().getSendFileInfoMap();
        //将文件进行排序(通过比较文件的position值)
        Collections.sort(fileInfos, Constants.DEFAULT_COMPARATOR);
        mSendFileAdapter = new CommonAdapter<Map.Entry<String, FileInfo>>(getContext(), R.layout.item_file_transfer, fileInfos) {
            @Override
            protected void convert(ViewHolder holder, Map.Entry<String, FileInfo> fileInfoMap, int position) {
                FileInfo fileInfo = fileInfoMap.getValue();
                //文件路径
                holder.setText(R.id.tv_item_file_transfer_file_path, fileInfo.getFilePath());
                //文件大小
                holder.setText(R.id.tv_item_file_transfer_size, FileUtil.FormatFileSize(fileInfo.getSize()));
                //文件发送状态
                if (fileInfo.getProgress() >= 100) {
                    holder.setText(R.id.tv_item_file_transfer_status, "发送完毕");
                } else if (fileInfo.getProgress() == 0) {
                    holder.setText(R.id.tv_item_file_transfer_status, "准备发送");
                } else if (fileInfo.getProgress() < 100) {
                    holder.setText(R.id.tv_item_file_transfer_status, "正在发送");
                } else {
                    holder.setText(R.id.tv_item_file_transfer_status, "发送失败");
                }
                //文件发送进度
                ProgressBar progressBar = holder.getView(R.id.pb_item_file_transfer);
                progressBar.setProgress(fileInfo.getProgress());
            }
        };
        mSendFileRecyclerView.setAdapter(mSendFileAdapter);
        //设置ListView样式
        mSendFileRecyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        //分割线
        mSendFileRecyclerView.addItemDecoration(new DividerItemDecoration(getContext(), DividerItemDecoration.VERTICAL));
    }


    /**
     * function:更新文件发送进度值
     *
     * @param position 文件索引
     * @param progress 接收进度
     */
    private void updateProgress(int position, int progress) {
        //首先判断索引值是否正确
        if (position < 0 || position >= mSendFileAdapter.getItemCount()) {
            return;
        }

        FileInfo fileInfo = mSendFileAdapter.getDatas().get(position).getValue();
        fileInfo.setProgress(progress); //设置进度值
        mSendFileAdapter.notifyItemChanged(position);//此时就会回调适配器中设置数据的那些方法，以更新item

        //若刚好是在传送最后一个文件且已传送完毕，则弹出提示--传送所有文件完毕
        if (position == AppContext.getAppContext().getSendFileInfoMap().size() - 1 && progress == 100) {
            toast("所有文件发送完毕");
            LogUtil.e("所有文件发送完毕");
        }
    }


    /**
     * 6
     * function:设置状态表示
     *
     * @param status
     */
    private void setStatus(String status) {
        tvStatus.setText(status);
        LogUtil.e(status);
    }

    /**
     * function:监听返回按钮
     */
    @Override
    public void onBackPressed() {
        // 需要将这句代码注释掉，才能够将点击事件在这里消费掉 super.onBackPressed();
        if (hasFileSending()) {
            showTipsDialog("文件正在发送，是否退出？", "是", new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    finishActivity();
                }
            }, "否", null);
        } else {
            finishActivity();
        }
    }


    /**
     * function:判断是否还有文件在发送
     *
     * @return
     */
    private boolean hasFileSending() {
        for (FileSender fileSender : mFileSenderList) {
            if (fileSender != null && fileSender.isRunning()) {
                //fileSender.isRunning()--该Runnable接口正在执行
                return true;
            }
        }
        return false;
    }

    /**
     * function:关闭此Activity
     * <p>
     * 注意：一定要记得回收掉相关资源，避免内存泄漏.
     */
    private void finishActivity() {
        //关闭UDP Socket连接
        closeUdpSocket();

        //停止所有文件发送任务
        stopAllFileSendingTask();

        //关闭发送端Socket
        if (mSenderServerRunnable != null) {
            mSenderServerRunnable.closeServerSocket();
            mSenderServerRunnable = null;
        }

        //关闭便携热点
        ApManager.closeAp(getContext());

        //清除待发送的文件列表
        AppContext.getAppContext().clearSendFileInfoMap();

        finish();
    }

    /**
     * function:停止所有文件发送任务
     */
    private void stopAllFileSendingTask() {
        for (FileSender fileSender : mFileSenderList) {
            if (fileSender != null) {
                //将对应的runnable接口stop掉
                fileSender.Stop();
            }
        }
    }

    /**
     * function:关闭UDP Socket
     */
    private void closeUdpSocket() {
        if (mDatagramSocket != null) {
            mDatagramSocket.disconnect();
            mDatagramSocket.close();
            mDatagramSocket = null;
        }
    }

}

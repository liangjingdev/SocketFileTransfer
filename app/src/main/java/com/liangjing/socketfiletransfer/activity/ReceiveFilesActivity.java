package com.liangjing.socketfiletransfer.activity;

import android.content.DialogInterface;
import android.content.IntentFilter;
import android.net.wifi.ScanResult;
import android.net.wifi.WifiManager;
import android.os.Handler;
import android.os.Message;
import android.support.v7.app.AlertDialog;
import android.support.v7.widget.DividerItemDecoration;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.liangjing.socketfiletransfer.R;
import com.liangjing.socketfiletransfer.base.AppContext;
import com.liangjing.socketfiletransfer.base.BaseActivity;
import com.liangjing.socketfiletransfer.base.BaseTransfer;
import com.liangjing.socketfiletransfer.bean.FileInfo;
import com.liangjing.socketfiletransfer.common.Constants;
import com.liangjing.socketfiletransfer.common.FileReceiver;
import com.liangjing.socketfiletransfer.common.SpaceItemDecoration;
import com.liangjing.socketfiletransfer.receiver.WifiBroadcastReceiver;
import com.liangjing.socketfiletransfer.utils.FileUtil;
import com.liangjing.socketfiletransfer.utils.LogUtil;
import com.liangjing.socketfiletransfer.utils.NetUtil;
import com.liangjing.socketfiletransfer.wifitools.WifiMgr;
import com.zhy.adapter.recyclerview.CommonAdapter;
import com.zhy.adapter.recyclerview.MultiItemTypeAdapter;
import com.zhy.adapter.recyclerview.base.ViewHolder;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import butterknife.BindView;
import butterknife.OnClick;


/**
 * function:接收端activity(接收文件)
 */
public class ReceiveFilesActivity extends BaseActivity implements MultiItemTypeAdapter.OnItemClickListener {

    /**
     * 接收端初始化完毕
     */
    public static final int MSG_FILE_RECEIVER_INIT_SUCCESS = 0x661;

    /**
     * 更新适配器
     */
    public static final int MSG_UPDATE_ADAPTER = 0x662;

    /**
     * 发送选中要接收的文件列表
     */
    public static final int MSG_SEND_RECEIVE_FILE_LIST = 0x663;

    /**
     * 添加接收文件
     */
    public static final int MSG_ADD_FILEINFO = 0x664;

    /**
     * 更新进度条
     */
    public static final int MSG_UPDATE_PROGRESS = 0x665;

    /**
     * 设置当前状态
     */
    public static final int MSG_SET_STATUS = 0x666;

    @BindView(R.id.tv_receive_files_status)
    TextView tvStatus;

    @BindView(R.id.btn_receive_files)
    Button btnSendFileList;

    @BindView(R.id.rv_receive_files_choose_hotspot)
    RecyclerView mChooseHotspotRecyclerView;

    //wifi列表适配器(扫描到的wifi列表)
    private CommonAdapter<ScanResult> mChooseHotspotAdapter;

    @BindView(R.id.rv_receive_files)
    RecyclerView mReceiveFilesRecyclerView;

    //文件列表适配器
    private CommonAdapter<Map.Entry<String, FileInfo>> mReceiveFilesAdapter;

    /**
     * 所选中的待发送的文件的集合--即这些文件是接收端要求发送端发送过来的，接收端所需要接收到的
     */
    private List<FileInfo> mSendFileInfos = new ArrayList<>();

    /**
     * 接收文件线程列表数据（mFileReceiverList集合中包含了许多待执行的Runnable接口--FileReceiver,FileReceiver--里面包含了接收某个文件的详细操作）
     */
    private List<FileReceiver> mFileReceiverList = new ArrayList<>();

    /**
     * WiFi工具类
     */
    private WifiMgr mWifiMgr;

    /**
     * 扫描到的可用WiFi列表
     */
    private List<ScanResult> mScanResults = new ArrayList<>();

    /**
     * 用来接收文件的Socket--Tcp Socket通信
     */
    private Socket mClientSocket;

    /**
     * UDP Socket通信
     */
    private DatagramSocket mDatagramSocket;

    /**
     * 接收文件线程
     */
    private ReceiveServerRunnable mReceiveServerRunnable;

    /**
     * 判断是否已发送初始化指令
     */
    private boolean mIsSendInitOrder;

    /**
     * 判断获取权限是否成功
     */
    private boolean mIsPermissionGranted;

    /**
     * 当前所选WiFi的SSID
     */
    private String mSelectedSSID;


    @Override
    protected int getLayoutId() {
        return R.layout.activity_receive_files_layout;
    }

    @Override
    protected String getTitleText() {
        return "接收文件";
    }

    @Override
    protected void initData() {
        //请求权限
        requestPermission(PERMISSION_CONNECT_WIFI, PERMISSION_REQ_CONNECT_WIFI);
    }

    /**
     * function:利用Handler--异步操作
     */
    private Handler mHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
            if (msg.what == MSG_FILE_RECEIVER_INIT_SUCCESS) {
                ///告知发送端，接收端初始化完毕
                sendInitSuccessToFileSender();
            } else if (msg.what == MSG_UPDATE_ADAPTER) {
                //设置接收文件列表适配器
                setupReceiveFilesAdapter();
            } else if (msg.what == MSG_SEND_RECEIVE_FILE_LIST) {
                //发送选中要接收的文件列表
                sendFileListToFileSender();
            } else if (msg.what == MSG_ADD_FILEINFO) {
                //添加接收文件
                mReceiveFilesAdapter.notifyDataSetChanged();
            } else if (msg.what == MSG_UPDATE_PROGRESS) {
                //更新进度条
                int position = msg.arg1;
                int progress = msg.arg2;
                if (position >= 0 && position < mReceiveFilesAdapter.getItemCount()) {
                    updateProgress(position, progress);
                }
            } else if (msg.what == MSG_SET_STATUS) {
                //设置当前状态
                setStatus(msg.obj.toString());
            }
        }
    };

    /**
     * function:更新文件接收进度
     *
     * @param position
     * @param progress
     */
    private void updateProgress(int position, int progress) {
        FileInfo fileInfo = mReceiveFilesAdapter.getDatas().get(position).getValue();
        fileInfo.setProgress(progress);
        mReceiveFilesAdapter.notifyItemChanged(position);//此时就会回调适配器中设置数据的那些方法，以更新item

        if (position == AppContext.getAppContext().getReceivedFileInfoMap().size() - 1 && progress == 100) {
            toast("所有文件接收完毕");
            LogUtil.e("所有文件接收完毕");
        }
    }

    /**
     * function:发送选中要接收的文件列表给发送端，然后由发送端发送过来
     */
    private void sendFileListToFileSender() {
        new Thread() {
            @Override
            public void run() {
                try {
                    //确保WiFi连接后获取正确IP地址
                    String serverIp = mWifiMgr.getIpAddressFromHotspot();
                    if (mDatagramSocket == null) {
                        //解决：java.net.BindException: bind failed: EADDRINUSE (Address already in use)
                        mDatagramSocket = new DatagramSocket(null);
                        mDatagramSocket.setReuseAddress(true);
                        mDatagramSocket.bind(new InetSocketAddress(Constants.DEFAULT_SERVER_UDP_PORT));
                    }

                    //发送选中的文件列表给发送端
                    InetAddress ipAddress = InetAddress.getByName(serverIp);
                    String jsonStr = FileInfo.toJsonStr(mSendFileInfos);//转化为json字符串
                    DatagramPacket sendPacket = new DatagramPacket(jsonStr.getBytes(), jsonStr.getBytes().length, ipAddress, Constants.DEFAULT_SERVER_UDP_PORT);
                    mDatagramSocket.send(sendPacket);
                    LogUtil.i("Send Msg To FileSender ------->>>" + jsonStr);

                    //发送开始发送文件指令给发送端
                    byte[] sendData = Constants.MSG_START_SEND.getBytes(BaseTransfer.UTF_8);
                    DatagramPacket sendPacket2 = new DatagramPacket(sendData, sendData.length, ipAddress, Constants.DEFAULT_SERVER_UDP_PORT);
                    mDatagramSocket.send(sendPacket2);
                    LogUtil.i("Send Msg To FileSender ------->>>" + sendData);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }.start();
    }


    /**
     * function:设置接收文件列表适配器
     */
    private void setupReceiveFilesAdapter() {
        //待接收的文件数据列表
        List<Map.Entry<String, FileInfo>> fileInfos = AppContext.getAppContext().getReceivedFileInfoMap();
        //排序
        Collections.sort(fileInfos, Constants.DEFAULT_COMPARATOR);
        //设置文件列表适配器
        mReceiveFilesAdapter = new CommonAdapter<Map.Entry<String, FileInfo>>(getContext(), R.layout.item_files_selector, fileInfos) {
            @Override
            protected void convert(ViewHolder holder, Map.Entry<String, FileInfo> fileInfoMap, int position) {
                final FileInfo fileInfo = fileInfoMap.getValue();
                //文件路径
                holder.setText(R.id.tv_item_files_selector_file_path, fileInfo.getFilePath());
                //文件大小
                holder.setText(R.id.tv_item_files_selector_size, FileUtil.FormatFileSize(fileInfo.getSize()));
                //文件接收状态
                if (fileInfo.getProgress() >= 100) {
                    holder.setText(R.id.tv_item_files_selector_status, "接收完毕");
                } else if (fileInfo.getProgress() == 0) {
                    holder.setText(R.id.tv_item_files_selector_status, "准备接收");
                } else if (fileInfo.getProgress() < 100) {
                    holder.setText(R.id.tv_item_files_selector_status, "正在接收");
                } else {
                    holder.setText(R.id.tv_item_files_selector_status, "接收失败");
                }

                //文件接收进度
                ProgressBar progressBar = holder.getView(R.id.pb_item_files_selector);
                progressBar.setProgress(fileInfo.getProgress());

                //选中文件
                CheckBox checkBox = holder.getView(R.id.cb_item_files_selector);
                checkBox.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
                    @Override
                    public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                        if (isChecked) {
                            mSendFileInfos.add(fileInfo); //添加进该集合，表示接收端需要发送端发送该文件过来
                        } else {
                            mSendFileInfos.remove(fileInfo);
                        }

                        //设置选中的文件个数大于零才可点击底部按钮
                        btnSendFileList.setEnabled(mSendFileInfos.size() > 0);
                    }
                });
            }
        };
        mReceiveFilesRecyclerView.setAdapter(mReceiveFilesAdapter);
        //设置ListView样式
        mReceiveFilesRecyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        //分割线
        mReceiveFilesRecyclerView.addItemDecoration(new DividerItemDecoration(getContext(), DividerItemDecoration.VERTICAL));
    }

    /**
     * function:告知发送端初始化完毕.接收端--客户端
     */
    private void sendInitSuccessToFileSender() {
        new Thread() {
            @Override
            public void run() {
                try {
                    //确保wifi连接后获取正确ip地址
                    int tryCount = 0;
                    String serverIp = mWifiMgr.getIpAddressFromHotspot(); //服务端ip地址
                    while (serverIp.equals(Constants.DEFAULT_UNKNOWN_IP) && tryCount < Constants.DEFAULT_TRY_COUNT) {
                        Thread.sleep(1000);
                        serverIp = mWifiMgr.getIpAddressFromHotspot();
                        tryCount++;
                    }

                    //是否可以ping通指定ip地址
                    tryCount = 0;
                    while (!NetUtil.pingIpAddress(serverIp) && tryCount < Constants.DEFAULT_TRY_COUNT) {
                        Thread.sleep(500);
                        LogUtil.i("Try to ping ------" + serverIp + " - " + tryCount);
                        tryCount++;
                    }

                    //创建UDP通信
                    if (mDatagramSocket == null) {
                        //解决：java.net.BindException: bind failed: EADDRINUSE (Address already in use)
                        mDatagramSocket = new DatagramSocket(null);//构造数据报套接字并将其绑定到本地主机上的任何可用端口--在该端口处进行监听，接收外来的数据包
                        //设置重用地址(启用/禁用SO_REUSEADDR套接字选项。)
                        mDatagramSocket.setReuseAddress(true);
                        mDatagramSocket.bind(new InetSocketAddress(Constants.DEFAULT_SERVER_UDP_PORT));//将此DatagramSocket绑定到特定的地址和端口--可向该特定的地址的端口处发送数据包
                    }

                    //发送初始化完毕指令
                    InetAddress ipAddress = InetAddress.getByName(serverIp);//首先获得服务端ip地址所对应的InetAddress对象--需要作为参数传入DatagramPacket构造方法中
                    byte[] sendData = Constants.MSG_FILE_RECEIVER_INIT_SUCCESS.getBytes(BaseTransfer.UTF_8);//参数--设置字符编码
                    DatagramPacket sendPacket = new DatagramPacket(sendData, sendData.length, ipAddress, Constants.DEFAULT_SERVER_UDP_PORT);//发送数据包
                    mDatagramSocket.send(sendPacket);
                    LogUtil.i("发送消息 ------->>>" + Constants.MSG_FILE_RECEIVER_INIT_SUCCESS);

                    //接收文件列表--(接收端可接收的那些文件)--while(true):始终监听该端口
                    while (true) {
                        byte[] receiveData = new byte[1024];
                        DatagramPacket receivePacket = new DatagramPacket(receiveData, receiveData.length);
                        mDatagramSocket.receive(receivePacket);
                        String response = new String(receivePacket.getData()).trim();
                        if (isNotEmptyString(response)) {
                            //发送端发来的文件列表
                            LogUtil.e("接收到的消息 -------->>>" + response);
                            parseFileInfoList(response);
                        }
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }.start();
    }


    /**
     * function:将字符串解析成FileInfo列表
     *
     * @param jsonStr
     */
    private void parseFileInfoList(String jsonStr) {
        if (isNotEmptyString(jsonStr)) {
            List<FileInfo> fileInfos = FileInfo.toObjectList(jsonStr);
            if (!isEmptyList(fileInfos)) {
                for (FileInfo fileInfo : fileInfos) {
                    if (fileInfo != null && isNotEmptyString(fileInfo.getFilePath())) {
                        //添加FileInfo到待接收的文件数据列表中
                        AppContext.getAppContext().addReceivedFileInfo(fileInfo);
                    }
                }
                //更新适配器
                mHandler.sendEmptyMessage(MSG_UPDATE_ADAPTER);
            }
        }
    }

    /**
     * function:注册监听wifil(连接)操作的系统广播
     */
    private void registerWifiReceiver() {

        IntentFilter filter = new IntentFilter();
        //监听wifi开启与关闭
        filter.addAction(WifiManager.WIFI_STATE_CHANGED_ACTION);
        //监听扫描周围可用的wifi列表结果
        filter.addAction(WifiManager.SCAN_RESULTS_AVAILABLE_ACTION);
        //监听wifi连接与断开
        filter.addAction(WifiManager.NETWORK_STATE_CHANGED_ACTION);
        //注册系统广播
        registerReceiver(mWifiBroadcastReceiver, filter); // 注册系统广播接收器
    }


    @Override
    protected void onResume() {
        super.onResume();
        if (mIsPermissionGranted && mWifiBroadcastReceiver == null) {
            registerWifiReceiver();
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (mWifiBroadcastReceiver != null) {
            unregisterWifiReceiver();
        }
    }

    /**
     * function:反注册wifi相关操作的系统广播
     */
    private void unregisterWifiReceiver() {
        if (mWifiBroadcastReceiver != null) {
            unregisterReceiver(mWifiBroadcastReceiver);
            mWifiBroadcastReceiver = null;
        }
    }


    /**
     * function:监听wifi列表点击事件
     *
     * @param view
     * @param holder
     * @param position
     */
    @Override
    public void onItemClick(View view, RecyclerView.ViewHolder holder, int position) {
        if (position < mChooseHotspotAdapter.getItemCount() && position >= 0) {
            //获取当前点击的wifi的ssid
            ScanResult scanResult = mChooseHotspotAdapter.getDatas().get(position);
            mSelectedSSID = scanResult.SSID;

            if ((scanResult.capabilities != null && !scanResult.capabilities.equals(WifiMgr.NO_PASSWORD)) || (scanResult.capabilities != null && !scanResult.capabilities.equals(WifiMgr.NO_PASSWORD_WPS))) {
                //弹出密码输入框
                showDialogWithEditText(mSelectedSSID, new OnWifiPasswordConfirmListener() {
                    @Override
                    public void onConfirm(String password) {
                        //使用密码连接WiFi
                        if (isNotEmptyString(password)) {
                            setStatus("正在连接Wifi...");
                            mWifiMgr.connectWifi(mSelectedSSID, password, mScanResults);
                        } else {
                            toast("密码不能为空");
                        }
                    }
                });
            } else {
                //连接免密码WiFi
                setStatus("正在连接Wifi...");
                mWifiMgr.connectWifi(mSelectedSSID, "", mScanResults);
            }
        }
    }

    @Override
    public boolean onItemLongClick(View view, RecyclerView.ViewHolder holder, int position) {
        return false;
    }


    /**
     * function:显示WiFi密码输入框
     *
     * @param title
     * @param listener
     */
    protected void showDialogWithEditText(String title, final OnWifiPasswordConfirmListener listener) {
        //输入框的view视图
        View dialogView = LayoutInflater.from(this).inflate(R.layout.layout_dialog_with_edittext, null);
        final EditText etPassword = (EditText) dialogView.findViewById(R.id.et_dialog_with_edittext);

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(title);
        builder.setView(dialogView); //设置视图
        builder.setPositiveButton(getString(R.string.confirm), new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                if (listener != null) {
                    //方法回调
                    listener.onConfirm(etPassword.getText().toString().trim());
                }
            }
        });
        builder.setNegativeButton(getString(R.string.cancel), null);
        builder.create().show();//创建对话框并显示
    }


    /**
     * function:监听返回键操作
     */
    @Override
    public void onBackPressed() {
        if (hasFileReceiving()) {
            showTipsDialog("文件正在接收，是否退出？", "是", new DialogInterface.OnClickListener() {
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
     * 关闭此Activity
     */
    private void finishActivity() {
        //断开UDP Socket
        closeUdpSocket();

        //停止所有文件接收任务
        stopAllFileReceivingTask();

        //断开接收文件的Socket
        closeClientSocket();

        //清除当前WiFi网络
        mWifiMgr.clearWifiConfig();

        //清空接收文件列表
        AppContext.getAppContext().clearReceivedFileInfoMap();

        finish();
    }

    /**
     * 关闭UDP Socket
     */
    private void closeUdpSocket() {
        if (mDatagramSocket != null) {
            mDatagramSocket.disconnect();
            mDatagramSocket.close();
            mDatagramSocket = null;
        }
    }

    /**
     * 断开接收文件的Tcp Socket通信
     */
    private void closeClientSocket() {
        if (mClientSocket != null) {
            try {
                mClientSocket.close();
                mClientSocket = null;
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }


    /**
     * 停止所有文件发送任务
     */
    private void stopAllFileReceivingTask() {
        for (FileReceiver fileReceiver : mFileReceiverList) {
            if (fileReceiver != null) {
                //让该runnable接口stop，即停止运行run里面的操作内容
                fileReceiver.stop();
            }
        }
    }


    /**
     * 是否还有文件在接收
     *
     * @return
     */
    private boolean hasFileReceiving() {
        for (FileReceiver fileReceiver : mFileReceiverList) {
            if (fileReceiver != null && fileReceiver.isRunning()) {
                return true;
            }
        }
        return false;
    }

    /**
     * function:授权成功
     *
     * @param requestCode
     */
    @Override
    protected void permissionSuccess(int requestCode) {
        super.permissionSuccess(requestCode);
        if (requestCode == PERMISSION_REQ_CONNECT_WIFI) {
            //权限请求成功
            mIsPermissionGranted = true;

            //开启wifi,监听wifi广播
            registerWifiReceiver();
            //wifi相关操作工具类
            mWifiMgr = new WifiMgr(getContext());
            //首先判断wifi功能是否可用(wifi是否打开)
            if (mWifiMgr.isWifiEnabled()) {
                setStatus("正在扫描可用wifi...");
                mWifiMgr.startScan();//开始扫描wifi
            } else {
                mWifiMgr.openWifi();
            }
        }
    }


    /**
     * function:设置当前状态
     */
    private void setStatus(String status) {
        tvStatus.setText(status);
        LogUtil.e(status);
    }

    /**
     * function:权限请求失败
     *
     * @param requestCode
     */
    @Override
    protected void permissionFail(int requestCode) {
        super.permissionFail(requestCode);
        if (requestCode == PERMISSION_REQ_CONNECT_WIFI) {
            //权限请求失败
            mIsPermissionGranted = false;
            showTipsDialog("wifi权限获取失败", new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    onBackPressed();
                }
            });
        }
    }


    /**
     * function:监听'发送'按钮事件--将接收端所需要接收到的文件告知发送端，让发送端发送过来。
     */
    @OnClick(R.id.btn_receive_files)
    public void sendReceiveFileListToFileSender() {
        //将选择的文件列表发给发送端，然后开始接收文件
        AppContext.getAppContext().clearReceivedFileInfoMap();//首先清空原来的文件列表
        for (FileInfo fileInfo : mSendFileInfos) {
            fileInfo.setPosition(mSendFileInfos.indexOf(fileInfo));
            AppContext.getAppContext().addReceivedFileInfo(fileInfo);//然后将接收端所需要接收到的文件添加到文件列表中
        }
        setupReceiveFilesAdapter();//更新文件列表适配器
        initReceiverServer();//初始化文件接收服务
    }

    /**
     * function:开启文件接收服务
     */
    private void initReceiverServer() {
        mReceiveServerRunnable = new ReceiveServerRunnable();
        new Thread(mReceiveServerRunnable).start();
    }


    /**
     * function:wifi相关系统广播接收器,WifiBroadcastReceiver--抽象类,定义了若干回调接口方法
     */
    private WifiBroadcastReceiver mWifiBroadcastReceiver = new WifiBroadcastReceiver() {


        @Override
        public void onWifiEnabled() {
            //wifi可用（wifi已开启）,开始扫描可用wifi
            setStatus("正在扫描可用wifi...");
            mWifiMgr.startScan();
        }

        @Override
        public void onWifiDisabled() {
            //wifi不可用(wifi已关闭)，清除可用wifi列表
            mSelectedSSID = "";
            mScanResults.clear();
            setUpWifiAdapter();//设置wifi列表适配器
        }

        @Override
        public void onScanResultAvailable(List<ScanResult> scanResults) {
            //扫描周围可用WiFi成功，设置可用WiFi列表
            mScanResults.clear();
            mScanResults.addAll(scanResults);
            setUpWifiAdapter();
        }

        @Override
        public void onWifiConnected(String connectSSID) {
            //判断指定WiFi是否连接成功
            if (connectSSID.equals(mSelectedSSID) && !mIsSendInitOrder) {
                //连接成功
                setStatus("连接成功...");
                //显示发送文件列表，隐藏wifi选择列表
                mChooseHotspotRecyclerView.setVisibility(View.INVISIBLE);
                mReceiveFilesRecyclerView.setVisibility(View.VISIBLE);

                //告知发送端，接收端已初始化完毕,发送端此时就可以向接收端传送其可以接收的所有文件的一个列表--Udp Socket通信
                mHandler.sendEmptyMessage(MSG_FILE_RECEIVER_INIT_SUCCESS);
                mIsSendInitOrder = true;
            } else {
                //连接成功的不是设备WiFi，清除该WiFi，重新扫描周围WiFi
//                LogUtils.e("连接到错误WiFi，正在断开重连...");
//                mWifiMgr.disconnectWifi(connectedSSID);
//                mWifiMgr.startScan();
            }
        }

        @Override
        public void onWifiDisconnected() {
            //wifi断开连接
        }
    };

    /**
     * function:设置wifi列表适配器(显示扫描到的wifi)
     * mScanResults--扫描到的可用wifi
     * R.layout.item_choose_hotspot--wifi Item视图
     */
    private void setUpWifiAdapter() {
        if (mChooseHotspotAdapter == null) {
            mChooseHotspotAdapter = new CommonAdapter<ScanResult>(getContext(), R.layout.item_choose_hotspot, mScanResults) {
                @Override
                protected void convert(ViewHolder holder, ScanResult scanResult, int position) {
                    //设置数据
                    holder.setText(R.id.tv_item_choose_hotspot_ssid, scanResult.SSID);
                    holder.setText(R.id.tv_item_choose_hotspot_level, String.format(getString(R.string.item_level), scanResult.level));
                }
            };
            //设置点击事件
            mChooseHotspotAdapter.setOnItemClickListener(this);
            //设置适配器
            mChooseHotspotRecyclerView.setAdapter(mChooseHotspotAdapter);
            //设置间隔
            mChooseHotspotRecyclerView.addItemDecoration(new SpaceItemDecoration(10));
            mChooseHotspotRecyclerView.setVisibility(View.VISIBLE);//设置列表可见
        } else {
            //通知数据集变化
            mChooseHotspotAdapter.notifyDataSetChanged();
        }
    }


    /**
     * function:密码框输入监听器
     */
    private interface OnWifiPasswordConfirmListener {
        void onConfirm(String password);
    }


    /**
     * function:Tcp Socket通信启动线程(Runnable)
     */
    private class ReceiveServerRunnable implements Runnable {

        @Override
        public void run() {
            try {
                //发送选择接收的文件列表给发送端
                mHandler.sendEmptyMessage(MSG_SEND_RECEIVE_FILE_LIST);

                Thread.sleep(3000);
                //开始接收文件
                String serverIp = mWifiMgr.getIpAddressFromHotspot();
                List<Map.Entry<String, FileInfo>> fileInfoList = AppContext.getAppContext().getReceivedFileInfoMap();
                Collections.sort(fileInfoList, Constants.DEFAULT_COMPARATOR);
                for (final Map.Entry<String, FileInfo> fileInfoMap : fileInfoList) {
                    //连接发送端，逐个文件进行接收
                    final int position = fileInfoList.indexOf(fileInfoMap);
                    mClientSocket = new Socket(serverIp, Constants.DEFAULT_FILE_RECEIVE_SERVER_PORT);
                    FileReceiver fileReceiver = new FileReceiver(mClientSocket, fileInfoMap.getValue());//获取该文件所对应的FileReceiver对象，进行文件接收操作
                    fileReceiver.setOnReceiveListener(new FileReceiver.OnReceiveListener() {
                        @Override
                        public void onStart() {
                            mHandler.obtainMessage(MSG_SET_STATUS, "开始接收" + FileUtil.getFileName(fileInfoMap.getValue().getFilePath())).sendToTarget();
                        }

                        @Override
                        public void onProgress(FileInfo fileInfo, long progress, long total) {
                            //更新接收进度视图
                            int i_progress = (int) (progress * 100 / total);
                            LogUtil.e("正在接收：" + fileInfo.getFilePath() + "\n当前进度：" + i_progress);

                            Message msg = new Message();
                            msg.what = MSG_UPDATE_PROGRESS;
                            msg.arg1 = position;
                            msg.arg2 = i_progress;
                            mHandler.sendMessage(msg);
                        }

                        @Override
                        public void onSuccess(FileInfo fileInfo) {
                            //接收成功
                            mHandler.obtainMessage(MSG_SET_STATUS, "文件：" + FileUtil.getFileName(fileInfo.getFilePath()) + "接收成功").sendToTarget();
                            fileInfo.setResult(FileInfo.FLAG_SUCCESS);
                            AppContext.getAppContext().updateReceivedFileInfo(fileInfo);

                            Message msg = new Message();
                            msg.what = MSG_UPDATE_PROGRESS;
                            msg.arg1 = position;
                            msg.arg2 = 100;
                            mHandler.sendMessage(msg);
                        }

                        @Override
                        public void onFailure(Throwable throwable, FileInfo fileInfo) {
                            if (fileInfo != null) {
                                //接收失败
                                mHandler.obtainMessage(MSG_SET_STATUS, "文件：" + FileUtil.getFileName(fileInfo.getFilePath()) + "接收失败").sendToTarget();
                                fileInfo.setResult(FileInfo.FLAG_FAILURE);
                                AppContext.getAppContext().updateReceivedFileInfo(fileInfo);

                                Message msg = new Message();
                                msg.what = MSG_UPDATE_PROGRESS;
                                msg.arg1 = position;
                                msg.arg2 = -1;
                                mHandler.sendMessage(msg);
                            }
                        }
                    });

                    //加入线程池中执行
                    mFileReceiverList.add(fileReceiver);
                    AppContext.MAIN_EXECUTOR.execute(fileReceiver);
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

}

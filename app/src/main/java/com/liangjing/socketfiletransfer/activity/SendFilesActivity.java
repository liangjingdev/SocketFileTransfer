package com.liangjing.socketfiletransfer.activity;

import android.os.Environment;
import android.view.View;
import android.view.ViewStub;
import android.widget.EditText;
import android.widget.TextView;

import com.liangjing.socketfiletransfer.R;
import com.liangjing.socketfiletransfer.base.BaseActivity;
import com.liangjing.socketfiletransfer.bean.FileInfo;
import com.liangjing.socketfiletransfer.common.FileSender;
import com.liangjing.socketfiletransfer.receiver.HotspotBroadcastReceiver;
import com.liangjing.socketfiletransfer.utils.FileUtil;
import com.liangjing.unirecyclerviewlib.recyclerview.OptionRecyclerView;

import java.io.File;
import java.net.DatagramPacket;
import java.util.ArrayList;
import java.util.List;

import butterknife.BindView;

/**
 * function:发送文件activity
 */
public class SendFilesActivity extends BaseActivity {


    //更新进度条
    public static final int MSG_UPDATE_PROGRESS = 0x661;

    //接收端初始化成功
    public static final int MSG_FILE_RECEIVER_INIT_SUCCESS = 0x663;

    //设置当前处于什么状态(如：正在等待连接/成功发送文件列表等。)
    public static final int ET_STATUS = 0x664;

    @BindView(R.id.tv_send_files_status)
    TextView tvStatus;

    @BindView(R.id.vs_send_files_open_hotspot)
    ViewStub vsOpenHotspot;

    @BindView(R.id.rv_send_files)
    OptionRecyclerView mSendFileRecyclerView;

    //热点名称以及密码
    private EditText etSSID;
    private EditText etPassword;

    //便携热点状态接收器
    private HotspotBroadcastReceiver mHotspotBroadcastReceiver;

    //UDP Socket
    private DatagramPacket mDatagramPacket;

    //文件发送线程
    private SenderServerRunnable mSendServerRunnable;

    //发送端所有待发送的文件的列表
    private List<FileInfo> mAllFileInfos = new ArrayList<>();

    //发送文件线程列表数据
    private List<FileSender> mFileSenderList = new ArrayList<>();

    //判断获取权限是否成功
    private boolean mIsPermissionGranted;

    //判断是否初始化成功
    private boolean mIsInitialized;


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
    }


    /**
     * function:文件发送线程
     */
    private class SenderServerRunnable implements Runnable {

        @Override
        public void run() {

        }
    }
}

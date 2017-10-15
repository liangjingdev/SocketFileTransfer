package com.liangjing.socketfiletransfer.activity;

import android.view.View;

import com.liangjing.socketfiletransfer.R;
import com.liangjing.socketfiletransfer.base.BaseActivity;

public class MainActivity extends BaseActivity {

    @Override
    protected int getLayoutId() {
        return R.layout.activity_main_layout;
    }

    @Override
    protected String getTitleText() {
        return "首页";
    }

    @Override
    protected void initData() {
        setToolbarLeftIcon(0);
    }


    /**
     * function:监听"发送文件"按钮
     *
     * @param view
     */
    public void sendFiles(View view) {
        pushActivity(SendFilesActivity.class);
    }

    /**
     * function:监听"接收文件"按钮
     *
     * @param view
     */
    public void receiveFiles(View view) {
        pushActivity(ReceiveFilesActivity.class);
    }

}

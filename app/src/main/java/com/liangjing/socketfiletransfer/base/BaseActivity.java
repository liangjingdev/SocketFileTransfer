package com.liangjing.socketfiletransfer.base;

import android.Manifest;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.text.TextUtils;
import android.view.MenuItem;
import android.view.ViewStub;
import android.widget.TextView;
import android.widget.Toast;

import com.liangjing.socketfiletransfer.R;
import com.liangjing.socketfiletransfer.utils.LogUtil;

import java.util.ArrayList;
import java.util.List;

import butterknife.ButterKnife;

/**
 * Created by liangjing on 2017/10/8.
 * <p>
 * function:activity基类
 */

public abstract class BaseActivity extends AppCompatActivity {

    //wifi热点连接和创建权限请求码
    protected static final int PERMISSION_REQ_CONNECT_WIFI = 3020;

    //创建便携热点权限请求码
    protected static final int PERMISSION_REQ_CREATE_HOTSPOT = 3021;

    //连接wifi所需权限
    protected static final String[] PERMISSION_CONNECT_WIFI = new String[]{
            Manifest.permission.ACCESS_WIFI_STATE,
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_COARSE_LOCATION,
            Manifest.permission.WRITE_EXTERNAL_STORAGE
    };

    //创建便携热点所需权限
    protected static final String[] PERMISSION_CREATE_HOSPOT = new String[]{
            Manifest.permission.WRITE_SETTINGS,
            Manifest.permission.WRITE_EXTERNAL_STORAGE
    };


    //标题栏相关控件
    private Toolbar mToolbar;
    private TextView tvTitle;

    //内容视图控件
    private ViewStub mViewStub;

    //当前Activity的上下文
    private Context mContext;

    //权限请求码
    private int mRequestCode;

    //获取当前Activity视图ID
    protected abstract int getLayoutId();

    //获取当前Activity标题
    protected abstract String getTitleText();

    //初始化数据
    protected abstract void initData();

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mContext = this;

        setContentView(R.layout.activity_base_layout);

        mToolbar = (Toolbar) findViewById(R.id.base_toolbar);
        tvTitle = (TextView) findViewById(R.id.tv_toolbar_title);
        mViewStub = (ViewStub) findViewById(R.id.base_viewStub);

        mToolbar.setTitle("");
        setSupportActionBar(mToolbar);

        //设置视图(为ViewStub填充视图)
        int layoutId = getLayoutId();
        if (layoutId > 0) {
            mViewStub.setLayoutResource(getLayoutId());
            mViewStub.inflate();
        }

        //设置标题
        String titleText = getTitleText();
        if (isNotEmptyString(titleText)) {
            tvTitle.setText(getTitleText());
        }

        //绑定注解类
        ButterKnife.bind(this);

        //初始化数据
        initData();
    }


    /**
     * function:若用户点击了主屏幕按钮键，则表示返回(功能)
     *
     * @param item
     * @return
     */
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.home:
                onBackPressed();
                break;
        }
        return true;
    }


    /**
     * function；获取当前Activity的上下文
     *
     * @return
     */
    protected Context getContext() {
        return mContext;
    }


    /**
     * function:设置标题栏左边icon
     *
     * @param resId
     */
    protected void setToolbarLeftIcon(int resId) {
        if (resId > 0) {
            mToolbar.setNavigationIcon(resId);
            //启用或禁用操作栏角落中的“主页”按钮。（请注意，这是操作栏上的应用程序回家/启动能力，而不是系统的home按钮。）
            getSupportActionBar().setHomeButtonEnabled(true);
            //布尔值为ture表示用户选择home将返回一级，而不是应用程序的顶级
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        } else {
            mToolbar.setNavigationIcon(null);
            getSupportActionBar().setHomeButtonEnabled(false);
            getSupportActionBar().setDisplayHomeAsUpEnabled(false);
        }
    }


    /**
     * function:请求权限
     * <p>
     * ActivityCompat.requestPermissions()--向用户发起请求，请求所需权限
     *
     * @param permissions 所需要的权限的列表
     * @param requestCode 请求码
     */
    protected void requestPermission(String[] permissions, int requestCode) {
        this.mRequestCode = requestCode;
        if (checkPermissions(permissions)) {
            permissionSuccess(requestCode);
        } else {
            List<String> needPermissions = getDeniedPermissions(permissions);
            //注：需要将List集合转化为数组的形式
            ActivityCompat.requestPermissions(this, needPermissions.toArray(new String[needPermissions.size()]), requestCode);
        }
    }


    /**
     * function:检查所需的权限是否都已授权
     * ContextCompat--帮助您访问Context API 4级后引入的功能，以向后兼容的方式。
     * checkSelfPermission--目的是确定您是否被授予某特定权限--note--即判断是否拥有该权限噶
     *
     * @param permissions
     * @return
     */
    private boolean checkPermissions(String[] permissions) {

        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) {
            return true;
        }

        for (String permission : permissions) {
            if (ContextCompat.checkSelfPermission(this, permission) != PackageManager.PERMISSION_GRANTED) {
                return false;
            }
        }
        return true;
    }

    /**
     * function:获取所需权限列表中需要申请权限的列表
     * 若checkPermissions()方法返回值不为PackageManager.PERMISSION_GRANTED，
     * 则表示有些需要的权限没有被授权，则就会调用该方法收集那些还没有被授权的权限
     * <p>
     * ActivityCompat.shouldShowRequestPermissionRationale()--获取是否应该显示UI以获得请求权限的理由。
     * 如果应用之前请求过此权限但用户拒绝了请求，此方法将返回 true。
     * <p>
     * 注：如果用户在过去拒绝了权限请求，并在权限请求系统对话框中选择了 Don’t ask again 选项，此方法将返回 false。
     * 如果设备规范禁止应用具有该权限，此方法也会返回 false。
     * <p>
     * 所以，如果用户选择了拒绝并且不再提醒，那么这个方法会返回false，通过这一点，就可以在适当的时候展开一个对话框，告诉用户到底发生了什么，需要怎么做。
     * 实际测试中发现，这个时候如果直接调用requestPermissions()也没用，因为刚才说了，已经选择不再提醒了。所以，需要告诉用户怎么打开权限：
     * 在app信息界面可以选择并控制所有的权限。
     *
     * @param permissions
     * @return
     */
    private List<String> getDeniedPermissions(String[] permissions) {
        List<String> needRequestPermissionList = new ArrayList<>();
        for (String permission : permissions) {
            if (ContextCompat.checkSelfPermission(this, permission) != PackageManager.PERMISSION_GRANTED ||
                    ActivityCompat.shouldShowRequestPermissionRationale(this, permission)) {
                needRequestPermissionList.add(permission);
            }
        }
        return needRequestPermissionList;
    }


    /**
     * function:请求权限的回调接口
     *
     * @param requestCode  请求码
     * @param permissions  所需请求权限的列表
     * @param grantResults 请求权限的结果码，一个权限对应一个结果码，结果码为PackageManager.PERMISSION_GRANTED则表示授权成功
     */
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        //系统请求权限回调,通过请求码来进行识别
        if (requestCode == mRequestCode) {
            if (verifyPermissions(grantResults)) {
                permissionSuccess(requestCode);
            } else {
                permissionFail(requestCode);
                //显示权限对话框
                showPermissionTipsDialog();
            }
        }
    }


    /**
     * function:确认所需权限是否都已经授权成功(如果此时还有某个权限没有授权成功的话，代表用户拒绝该权限之后还可能勾选了不再提醒选项，那么此时接下来应该弹出一个提示对话框告诉用户应该怎样去解决这个问题)
     *
     * @param grantResults
     * @return
     */
    private boolean verifyPermissions(int[] grantResults) {
        for (int grantResult : grantResults) {
            if (grantResult != PackageManager.PERMISSION_GRANTED) {
                return false;
            }
        }
        return true;
    }


    /**
     * 弹出Toast提示
     *
     * @param text 提示内容
     */
    protected void toast(String text) {
        if (this.isFinishing()) {
            return;
        }

        if (!TextUtils.isEmpty(text)) {
            Toast.makeText(mContext, text, Toast.LENGTH_SHORT).show();
        }
    }

    /**
     * function:判断字符串是否不为空
     *
     * @param text
     * @return
     */
    protected boolean isNotEmptyString(String text) {
        return !TextUtils.isEmpty(text) && !text.equals("");
    }

    /**
     * 权限请求成功
     *
     * @param requestCode
     */
    protected void permissionSuccess(int requestCode) {
        LogUtil.e("获取权限成功：" + requestCode);
    }


    /**
     * 权限请求失败
     *
     * @param requestCode
     */
    protected void permissionFail(int requestCode) {
        LogUtil.e("获取权限失败：" + requestCode);
    }


    /**
     * function:显示权限提示对话框
     */
    private void showPermissionTipsDialog() {
        showTipsDialogWithTitle("提示", "当前应用缺少必要权限，该功能暂时无法使用。如若需要，请点击【确定】按钮前往设置中心进行权限授权", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                //打开该app的设置页面
                startAppSettings();
            }
        }, null);
    }


    /**
     * function: 启动当前应用设置页面(利用Intent)
     */
    private void startAppSettings() {
        Intent intent = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
        intent.setData(Uri.parse("package:" + getPackageName()));
        startActivity(intent);
    }


    /**
     * function:显示提示对话框
     *
     * @param content         内容
     * @param confirmListener 确认按钮点击事件
     */
    protected void showTipsDialog(String content, DialogInterface.OnClickListener confirmListener) {
        showTipsDialogWithTitle(null, content, getString(R.string.confirm), confirmListener, null, null);
    }


    /**
     * 显示提示对话框
     *
     * @param content         内容
     * @param confirmText     确定按钮文字
     * @param confirmListener 确定按钮点击事件
     * @param cancelText      取消按钮文字
     * @param cancelListener  取消按钮点击事件
     */
    protected void showTipsDialog(String content, String confirmText, DialogInterface.OnClickListener confirmListener, String cancelText, DialogInterface.OnClickListener cancelListener) {
        showTipsDialogWithTitle("", content, confirmText, confirmListener, cancelText, cancelListener);
    }


    /**
     * 显示提示对话框（带标题）
     *
     * @param title           标题
     * @param content         内容
     * @param confirmListener 确定按钮点击事件
     * @param cancelListener  取消按钮点击事件
     */
    protected void showTipsDialogWithTitle(String title, String content, DialogInterface.OnClickListener confirmListener, DialogInterface.OnClickListener cancelListener) {
        showTipsDialogWithTitle(title, content, getString(R.string.confirm), confirmListener, getString(R.string.cancel), cancelListener);
    }

    /**
     * function:显示提示对话框（带标题）
     *
     * @param title           标题
     * @param content         内容
     * @param confirmText     确认按钮文字
     * @param confirmListener 确认按钮点击事件
     * @param cancleText      取消按钮文字
     * @param cancleListener  取消按钮点击事件
     */
    protected void showTipsDialogWithTitle(String title, String content, String confirmText, DialogInterface.OnClickListener confirmListener,
                                           String cancleText, DialogInterface.OnClickListener cancleListener) {

        //创建对话框
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        if (isNotEmptyString(title)) {
            builder.setTitle(title);
        }
        builder.setMessage(content);
        builder.setPositiveButton(confirmText, confirmListener);
        if (isNotEmptyString(cancleText)) {
            builder.setNegativeButton(cancleText, cancleListener);
        }
        builder.create().show();
    }


    /**
     * function:跳转到另外一个activity
     *
     * @param mClass
     */
    protected void pushActivity(Class<?> mClass) {
        startActivity(new Intent(mContext, mClass));
    }

    /**
     * 判断字符串是否为空
     *
     * @param text
     * @return
     */
    protected boolean isEmptyString(String text) {
        return TextUtils.isEmpty(text) || text.equals("null");
    }

    /**
     * 判断列表是否为空
     *
     * @param list
     * @return
     */
    protected boolean isEmptyList(List<?> list) {
        return list == null || list.size() <= 0;
    }
}

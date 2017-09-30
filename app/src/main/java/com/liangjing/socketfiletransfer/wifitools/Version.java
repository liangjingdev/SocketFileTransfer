package com.liangjing.socketfiletransfer.wifitools;

import android.os.Build;

import java.lang.reflect.Field;

/**
 * Created by liangjing on 2017/9/30.
 *
 * function:获取当前移动设备的安卓版本
 */

public class Version {

    public final static int SDK = get();

    private static int get() {

        final Class<Build.VERSION> versionClass = Build.VERSION.class;
        try {
            // First try to read the recommended field android.os.Build.VERSION.SDK_INT.
            final Field sdkIntField = versionClass.getField("SDK_INT");
            return sdkIntField.getInt(null);
        }catch (NoSuchFieldException e) {
            // If SDK_INT does not exist, read the deprecated field SDK.
            return Integer.valueOf(Build.VERSION.SDK);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}

package com.liangjing.socketfiletransfer.utils;

import android.os.Environment;
import android.text.TextUtils;

import java.io.File;
import java.io.FileInputStream;
import java.text.DecimalFormat;

/**
 * Created by liangjing on 2017/10/9.
 * <p>
 * function:文件工具类
 */

public class FileUtil {


    //外部存储
    public static final String ROOT_PATH = Environment.getExternalStorageDirectory() + File.separator + "socketFileTransfer/";

    /**
     * function:根据文件路径获取文件名称
     *
     * @param filePath
     * @return
     */
    public static String getFileName(String filePath) {
        if (TextUtils.isEmpty(filePath)) {
            return "";
        }
        return filePath.substring(filePath.lastIndexOf(File.separator) + 1);
    }


    /**
     * function:生成本地文件路径(接收端)
     *
     * @param filePath
     * @return
     */
    public static File generateLocalFile(String filePath) {
        String fileName = getFileName(filePath);
        File dirFile = new File(ROOT_PATH);
        if (!dirFile.exists()) {
            //创建目录
            dirFile.mkdirs();
        }
        return new File(dirFile, fileName);
    }


    /**
     * function:转换文件大小(单位)
     * 1024--B
     * 1024*1024=1048576--K
     * 1048576*1048576=1073741824--M
     *
     * @param fileSize
     * @return
     */
    public static String FormatFileSize(long fileSize) {
        if (fileSize <= 0) {
            return "0KB";
        }

        DecimalFormat df = new DecimalFormat("#.00");
        String fileSizeString = "";
        if (fileSize < 1024) {
            fileSizeString = df.format((double) fileSize) + "B";
        } else if (fileSize < 1048576) {
            fileSizeString = df.format((double) fileSize / 1024) + "K";
        } else if (fileSize < 1073741824) {
            fileSizeString = df.format((double) fileSize / 1048576) + "M";
        } else {
            fileSizeString = df.format((double) fileSize / 1073741824) + "G";
        }
        return fileSizeString;
    }


    /**
     * function:获取文件大小(即字节数)
     *          FileInputStream的available()方法--作用：返回该文件的实际可读字节数，也就是总大小
     *
     * @param file
     * @return
     */
    @SuppressWarnings("resource")
    public static long getFileSize(File file) throws Exception {
        long size = 0;
        if (file.exists()) {
            FileInputStream inputStream = null;
            inputStream = new FileInputStream(file);
            size = inputStream.available();
        } else {
            //创建文件
            file.createNewFile();
        }
        return size;
    }
}

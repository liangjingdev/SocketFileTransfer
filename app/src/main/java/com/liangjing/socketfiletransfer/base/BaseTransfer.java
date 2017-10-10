package com.liangjing.socketfiletransfer.base;

/**
 * Created by liangjing on 2017/10/10.
 * <p>
 * function:定义一些Transferable接口当中的方法实现时需要用到的数据
 */

public abstract class BaseTransfer implements Transferable {


    //字节数组长度
    public static final int BYTE_SIZE_HEADER = 1024 * 10;
    public static final int BYTE_SIZE_DATA = 1024 * 4;

    //传输字节类型
    public static final String UTF_8 = "UTF-8";
}
